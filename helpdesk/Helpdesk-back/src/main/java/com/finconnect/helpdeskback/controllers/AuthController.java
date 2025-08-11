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

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"http://localhost:4200"})
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtService jwtService,
                          UserRepository userRepository,
                          PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
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
}
