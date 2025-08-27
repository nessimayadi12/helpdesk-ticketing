package com.finconnect.helpdeskback.controllers;

import com.finconnect.helpdeskback.entities.User;
import com.finconnect.helpdeskback.entities.UserRole;
import com.finconnect.helpdeskback.repositories.UserRepository;
import com.finconnect.helpdeskback.security.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import com.finconnect.helpdeskback.entities.PasswordResetToken;
import com.finconnect.helpdeskback.repositories.PasswordResetTokenRepository;
import com.finconnect.helpdeskback.services.MailService;
import org.springframework.beans.factory.annotation.Value;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetTokenRepository resetRepo;
    private final MailService mailService;
    @Value("${app.frontend.base-url:http://localhost:4200}")
    private String frontendBaseUrl;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtService jwtService,
                          UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          PasswordResetTokenRepository resetRepo,
                          MailService mailService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.resetRepo = resetRepo;
        this.mailService = mailService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");
        try {
            // Support login by email or username
            User lookup = userRepository.findByUsername(username);
            if (lookup == null) {
                // If not found by username, try by email
                lookup = userRepository.findByEmail(username);
                if (lookup != null) {
                    username = lookup.getUsername();
                }
            }
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            User user = userRepository.findByUsername(username);
            Map<String, Object> claims = new HashMap<>();
            // Safeguard role null
            String role = (user.getRole() != null) ? user.getRole().name() : "CUSTOMER";
            claims.put("role", role);
            claims.put("email", user.getEmail());
            String token = jwtService.generateToken(username, claims);

            Map<String, Object> body = new HashMap<>();
            body.put("token", token);
            body.put("username", username);
            body.put("role", role);
            return ResponseEntity.ok(body);
        } catch (BadCredentialsException ex) {
            return ResponseEntity.status(401).body(Map.of("message", "Identifiants invalides"));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        if (user.getUsername() == null || user.getUsername().isBlank() ||
            user.getPassword() == null || user.getPassword().isBlank() ||
            user.getEmail() == null || user.getEmail().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Champs requis manquants"));
        }
        if (userRepository.findByUsername(user.getUsername()) != null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Username existe déjà"));
        }
        if (user.getRole() == null) {
            user.setRole(UserRole.CUSTOMER);
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "Utilisateur créé"));
    }

    // Request password reset: accept username or email
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgot(@RequestBody Map<String, String> req) {
        String id = req.get("identifier");
        if (id == null || id.isBlank()) return ResponseEntity.badRequest().body(Map.of("message", "Identifiant requis"));
        User user = userRepository.findByUsername(id);
        if (user == null) user = userRepository.findByEmail(id);
        if (user == null) return ResponseEntity.ok(Map.of("message", "Si un compte existe, un email a été envoyé"));
        PasswordResetToken prt = new PasswordResetToken();
        prt.setUser(user);
        prt.setToken(UUID.randomUUID().toString().replace("-", ""));
        prt.setExpiresAt(Instant.now().plus(30, ChronoUnit.MINUTES));
        resetRepo.save(prt);
        try {
            String link = buildResetLink(prt.getToken());
            String html = "<div style='font-family:Arial,sans-serif;max-width:680px;margin:0 auto'>"+
                          "<h2 style='color:#6c2bd9;margin:16px 0'>Réinitialisation de mot de passe</h2>"+
                          "<p>Bonjour "+ (user.getUsername()!=null?user.getUsername():"") +",</p>"+
                          "<p>Vous avez demandé à réinitialiser votre mot de passe. Ce lien est valable 30 minutes.</p>"+
                          "<p><a href='"+link+"' style='display:inline-block;padding:10px 14px;background:#6c2bd9;color:#fff;text-decoration:none;border-radius:6px'>Réinitialiser mon mot de passe</a></p>"+
                          "<p style='color:#666'>Si vous n'êtes pas à l'origine de cette demande, ignorez cet email.</p>"+
                          "</div>";
            mailService.sendGeneric(user.getEmail(), "Réinitialisation de mot de passe", html);
        } catch (Exception ignored) {}
        return ResponseEntity.ok(Map.of("message", "Si un compte existe, un email a été envoyé"));
    }

    // Confirm reset: token + newPassword
    @PostMapping("/reset-password")
    public ResponseEntity<?> reset(@RequestBody Map<String, String> req) {
        String token = req.get("token");
        String newPwd = req.get("newPassword");
        if (token == null || token.isBlank() || newPwd == null || newPwd.isBlank())
            return ResponseEntity.badRequest().body(Map.of("message", "Paramètres manquants"));
        PasswordResetToken prt = resetRepo.findByToken(token);
        if (prt == null || prt.isUsed() || prt.getExpiresAt() == null || prt.getExpiresAt().isBefore(Instant.now()))
            return ResponseEntity.status(400).body(Map.of("message", "Lien invalide ou expiré"));
        User u = prt.getUser();
        if (u == null) return ResponseEntity.status(400).body(Map.of("message", "Token invalide"));
        u.setPassword(passwordEncoder.encode(newPwd));
        userRepository.save(u);
        prt.setUsed(true);
        resetRepo.save(prt);
        return ResponseEntity.ok(Map.of("message", "Mot de passe mis à jour"));
    }

    private String buildResetLink(String token) {
        String base = frontendBaseUrl != null && !frontendBaseUrl.isBlank() ? frontendBaseUrl.trim() : "http://localhost:4200";
        if (base.endsWith("/")) base = base.substring(0, base.length()-1);
        return base + "/guest/reset-password?token=" + token;
    }

    // email sending delegated to MailService.sendGeneric
}
