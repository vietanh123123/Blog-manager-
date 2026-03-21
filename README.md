# BlogWritter2

This project is a full-stack blog management application built with a **plain Java backend** (no Spring), **PostgreSQL**, and a **vanilla HTML/CSS/JS frontend**. It provides user authentication with JWT, secure password hashing with BCrypt, and complete CRUD operations for articles scoped per user account.

## Visit The Site

Feel free to check out the live project here:

- Frontend: [https://blogmanagersaar.netlify.app](https://blogmanagersaar.netlify.app)
- Backend API: [https://blog-manager-production-6034.up.railway.app/api/articles](https://blog-manager-production-6034.up.railway.app/api/articles)

## Features

- **PostgreSQL Database:** Stores users and articles, with ownership enforced by `user_id`.
- **Plain Java Backend (JDK HttpServer + JDBC):** Lightweight REST API without Spring Boot.
- **JWT Authentication:** Login returns a token used as `Authorization: Bearer <token>`.
- **Secure Password Hashing:** Passwords are hashed with BCrypt before storage.
- **Article Management:** Create, read, update, and delete blog articles.
- **Per-User Data Isolation:** Each user only accesses their own content.
- **Docker + Railway Ready:** Includes `Dockerfile`, `nixpacks.toml`, and `railway.toml`.

## Project Structure

- `src/com/blog/server` — HTTP server bootstrap and router
- `src/com/blog/controller` — API route handlers
- `src/com/blog/service` — business logic
- `src/com/blog/repository` — JDBC database access
- `src/com/blog/model` — domain models (`Article`, `User`)
- `src/com/blog/util` — JSON helpers, JWT, password hashing
- `assets` + `index.html` — frontend UI
- `run.sh` — local compile/run script

## Prerequisites

Before running this project locally, ensure you have:

- Java Development Kit (JDK) **21+**
- PostgreSQL database
- Bash shell (Linux/macOS) for `run.sh` (or run equivalent Java commands manually)
- Internet access once to download required JAR files into `lib/`

Required JARs in `lib/`:

- `postgresql-42.7.3.jar`
- `bcrypt-0.10.2.jar`
- `bytes-1.6.1.jar`

## Installation

### 1) Clone the repository

Clone and open this project in your preferred IDE.

### 2) Configure environment variables

Set these environment variables before running:

- `DB_URL` (or `DATABASE_URL`)
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET`
- optional: `PORT` (default is `8080`)
- optional: `CORS_ALLOWED_ORIGINS` (comma-separated origins)

`src/db.properties` already reads from env variables:

- `db.url=${DB_URL}`
- `db.username=${DB_USERNAME}`
- `db.password=${DB_PASSWORD}`

### 3) Run backend locally

On Linux/macOS:

1. Make the script executable (first time only): `chmod +x run.sh`
2. Start the server: `./run.sh`

The script compiles all Java files to `out/`, copies `db.properties`, and starts:

- `com.blog.server.BlogServer`

### 4) Run frontend locally

This frontend is static and does not require npm.

1. Open `index.html` with a local static server (recommended at `localhost:5500`).
2. If backend is local, the frontend auto-targets `http://localhost:8080/api`.

## Usage

Base API URL (local): `http://localhost:8080`

### Auth Endpoints

- `POST /api/auth/register` — create account
- `POST /api/auth/login` — login and receive JWT token

### Article Endpoints (require Bearer token)

- `GET /api/articles/published` — list published articles for current user
- `GET /api/articles` — list all user articles
- `GET /api/articles/{id}` — get one article
- `POST /api/articles` — create article
- `PUT /api/articles/{id}` — update article
- `DELETE /api/articles/{id}` — delete article

## Deployment

- `Dockerfile` builds and runs the Java backend.
- `railway.toml` and `nixpacks.toml` provide Railway/Nixpacks build and start settings.

## Notes

- Database schema is initialized automatically at startup in `DatabaseManager.initializeSchema()`.
- JWT token expiry is currently set to **10 minutes**.
- The backend uses Java virtual threads (`Executors.newVirtualThreadPerTaskExecutor()`).

## Contributing

Contributions are welcome. If you’d like to improve this project, feel free to open an issue or submit a pull request.