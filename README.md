<div align="center">

<img src="berry-free-angular-admin-template/src/assets/images/logo-fan.png" alt="Helpdesk Ticketing" height="96" />

# Helpdesk Ticketing

Gestion de tickets moderne (Angular 17 + Spring Boot 3) avec authentification JWT, commentaires en temps réel (SSE), tableau de bord statistiques et notifications email.

[![Made with Angular](https://img.shields.io/badge/Angular-17-DD0031?logo=angular&logoColor=white)](./berry-free-angular-admin-template)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3-6DB33F?logo=springboot&logoColor=white)](./helpdesk/Helpdesk-back)
[![TypeScript](https://img.shields.io/badge/TypeScript-5-3178C6?logo=typescript&logoColor=white)](./berry-free-angular-admin-template)
[![Java](https://img.shields.io/badge/Java-17-007396?logo=openjdk&logoColor=white)](./helpdesk/Helpdesk-back)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](berry-free-angular-admin-template/LICENSE)
[![Issues](https://img.shields.io/github/issues/nessimayadi12/helpdesk-ticketing.svg)](https://github.com/nessimayadi12/helpdesk-ticketing/issues)
[![Stars](https://img.shields.io/github/stars/nessimayadi12/helpdesk-ticketing.svg?style=social)](https://github.com/nessimayadi12/helpdesk-ticketing/stargazers)

</div>

## Aperçu

Application de helpdesk permettant de créer, suivre et commenter des tickets. Elle propose des notifications email, un flux de commentaires en temps réel via SSE, un tableau de bord riche en statistiques, et une sécurité fine (JWT, rôles, propriété des tickets, reset mot de passe).

<p align="center">
  <img src="berry-free-angular-admin-template/src/assets/images/authentication/auth2-login.svg" alt="Écran de connexion" height="220" />
  <img src="berry-free-angular-admin-template/src/assets/images/logo-dark.svg" alt="Logo" height="80" />
</p>

## Fonctionnalités clés

- Authentification JWT (login, registre) et réinitialisation du mot de passe par email
- Tickets: création, lecture, mise à jour, suppression (droits admin ou propriétaire)
- Commentaires avec diffusion en temps réel (SSE) et indicateur de non‑lu
- Recherche plein‑texte et filtres par statut dans la liste des tickets
- Dashboard statistiques (admin et non‑admin): totaux, par statut, dernières activités, top commentés
- Emails HTML à la création et à la mise à jour de ticket (français)
- Locale française pour les dates et l’interface

## Architecture

```
helpdesk-ticketing/
├─ berry-free-angular-admin-template/     # Frontend Angular 17 (UI, services, pages)
│  └─ src/
│     ├─ app/                             # Pages, services (tickets, stats, auth)
│     ├─ assets/                          # Images, icônes
│     └─ environments/                    # environment.ts (apiUrl)
└─ helpdesk/
   └─ Helpdesk-back/                      # Backend Spring Boot 3
      ├─ src/main/java                    # API REST, sécurité, services
      └─ src/main/resources               # application.properties
```

Flux technique:

- Sécurité: Spring Security stateless + JWT; CORS ouvert aux origines front; endpoints publics pour auth et reset.
- SSE: SseEmitter côté back (/api/sse/comments), EventSource côté front (token transmis en query) pour updates temps réel.
- Données: JPA/Hibernate (tickets, commentaires, stats agrégées), DTO/records côté API.

## Captures d’écran

<img width="671" height="877" alt="image" src="https://github.com/user-attachments/assets/c1593d3c-b017-4d9e-ba4d-a3f73cbebfd7" />

- Connexion: `assets/images/authentication/auth2-login.svg`
- Logo clair: `assets/images/logo-dark.svg`
- Avatar exemple: `assets/images/user/avatar-1.jpg`

## Démarrage rapide

Prérequis: Node.js 18+, Java 17+, Maven 3.9+, npm.

### Backend (Spring Boot)

Répertoire: `helpdesk/Helpdesk-back`

1) Configurer `src/main/resources/application.properties` (datasource, SMTP, options JWT). Exemples de propriétés utiles:
   - `server.port=8084`
   - `spring.mail.host`, `spring.mail.username`, `spring.mail.password`
   - `app.frontend.base-url=http://localhost:4200`

2) Lancer localement:

```
# Optionnel (documentation):
mvn -q -DskipTests spring-boot:run
```

API par défaut: `http://localhost:8084`

### Frontend (Angular)

Répertoire: `berry-free-angular-admin-template`

1) Vérifier `src/environments/environment.ts` → `apiUrl: 'http://localhost:8084'`

2) Installer et démarrer:

```
# Optionnel (documentation):
npm ci
npm run start
```

UI par défaut: `http://localhost:4200`

## Points d’API (extraits)

- Auth: `POST /api/auth/login`, `POST /api/auth/register`, `POST /api/auth/forgot-password`, `POST /api/auth/reset-password`
- Tickets: `GET /api/tickets` (admin), `GET /api/tickets/my` (user), `POST /api/tickets`, `PUT/DELETE /api/tickets/{id}`
- Commentaires: `GET/POST /api/tickets/{ticketId}/comments`, stats: `GET /api/comments/stats`
- SSE: `GET /api/sse/comments?token=...`
- Stats: `GET /api/stats/admin`, `GET /api/stats/me`

## Sécurité et droits

- JWT dans l’en‑tête Authorization: `Bearer <token>` (ou en query pour SSE)
- Accès admin requis pour la liste globale des tickets; édition/suppression: admin ou propriétaire du ticket
- CORS configuré; CSRF désactivé (stateless)

## Configuration email (SMTP)

Active l’envoi d’emails HTML pour la création/mise à jour de tickets et pour “Mot de passe oublié”.

Propriétés typiques:

```
# Optionnel (documentation):
spring.mail.host=smtp.example.com
spring.mail.port=587
spring.mail.username=...
spring.mail.password=...
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

## Scripts utiles

- Génération PDF du journal de stage (local): fichiers dans `c:/Stage/journal/` (HTML/MD → PDF via Edge headless)
- Lint/format (front): `npm run lint` / `npm run format`

## Contribution

Les contributions sont les bienvenues: issues, forks, pull requests.

1. Forkez le repo, créez une branche feature.
2. Ajoutez tests/validations si nécessaire.
3. Ouvrez une PR claire avec captures/description.

## Licence

Sous licence MIT. Voir [LICENSE](berry-free-angular-admin-template/LICENSE).

## Remerciements

- Template UI basé sur “Berry Free Angular Admin”.
- Icônes et assets du dossier `src/assets` (crédits respectifs).

---

<div align="center">
  <sub>Made with ❤ using Angular & Spring Boot — temps réel, sécurité et statistiques.</sub>
</div>
