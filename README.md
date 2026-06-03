> 🇧🇷 [Leia em português](README.pt-BR.md)

# Tibia Session

A project built to process, manage and analyze *Party Hunt Analyzer* sessions from the game Tibia. The system integrates a Discord Bot with a Web application.

**Live Demo:** [https://huntanalyzer.lbaba.com.br/](https://huntanalyzer.lbaba.com.br/)

**Discord interaction flow:** The user runs the `/loot` slash command → fills in the modal with the *analyzer* text → the bot responds with an *embed* detailing the session.

## Tech Stack

**Frontend:**
* Angular 21
* Tailwind CSS

**Backend:**
* Java 21
* Spring Boot 3.4 (Data JPA, Security)
* JDA (Java Discord API) 5.3.1
* PostgreSQL
* Flyway

**Infrastructure & CI/CD:**
* Docker
* Docker Compose
* GitHub Actions

## Getting Started

To run the application locally, just clone the repository and configure the required environment variables.

### Prerequisites

- Java 21
- PostgreSQL
- A Discord bot token (create one at the [Discord Developer Portal](https://discord.com/developers/applications))
- Docker installed

### Clone the Repository

```bash
git clone https://github.com/LeonardoBaba/tibia-session.git
cd tibia-session
```

### Environment Variables
Create a `.env` file at the project root and fill it with the variables below:

| Variable | Example |
|---|---|
| `ANALYZER_DATABASE_CONNECTION` | `jdbc:postgresql://localhost:5432/analyzer` |
| `ANALYZER_DATABASE_USER` | `postgres` |
| `ANALYZER_DATABASE_PASSWORD` | `postgres` |
| `ANALYZER_SERVER_PORT` | `15600` |
| `ANALYZER_DISCORD_TOKEN` | your Discord bot token |
| `ANALYZER_DISCORD_CLIENT_ID` | OAuth2 client ID of your Discord application |
| `ANALYZER_DISCORD_CLIENT_SECRET` | OAuth2 client secret of your Discord application |

### Run
With the `.env` file configured, start the containers using Docker Compose:
```bash
docker compose up
```

## Project Structure

```
tibia-session/
├── src/
│   ├── main/java/br/com/baba/tibia_analyzer/
│   │   ├── core/        # domain: parsing, splitter, persistence, DTOs
│   │   ├── discord/     # Discord bot (JDA): commands, modals, embeds
│   │   └── api/         # REST API: controllers, auth (OAuth2), mappers
│   └── main/resources/
│       └── db/migration # Flyway SQL migrations
├── frontend/            # Angular web UI
├── .github/workflows/   # CI/CD (deploy.yml)
├── docker-compose.yml
├── Dockerfile
└── pom.xml
```

## Deploy

The deploy process is automated via CI/CD. Any push to the `master` branch triggers the `.github/workflows/deploy.yml` action, which runs the flow: build the application → generate and push the image to Docker Hub → deploy on the VPS via SSH.
