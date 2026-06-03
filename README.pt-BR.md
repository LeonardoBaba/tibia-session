> 🇬🇧 [Read in English](README.md)

# Tibia Session

Um projeto desenvolvido para processar, gerenciar e analisar sessões do *Party Hunt Analyzer* do jogo Tibia. O sistema integra um Bot de Discord com uma aplicação Web.

**Live Demo:** [https://huntanalyzer.lbaba.com.br/](https://huntanalyzer.lbaba.com.br/)

**Fluxo de interação no Discord:** O usuário executa o *slash command* `/loot` → Preenche o modal com o texto do *analyzer* → O bot responde com um *embed* detalhando a sessão.

## Tecnologias

**Frontend:**
* Angular 21
* Tailwind CSS

**Backend:**
* Java 21
* Spring Boot 3.4 (Data JPA, Security)
* JDA (Java Discord API) 5.3.1
* PostgreSQL
* Flyway

**Infraestrutura & CI/CD:**
* Docker
* Docker Compose
* GitHub Actions

## Como Rodar

Para executar a aplicação localmente, basta clonar o repositório e configurar as variáveis de ambiente necessárias.

### Pré-requisitos

- Java 21
- PostgreSQL
- Token de bot do Discord (crie um no [Discord Developer Portal](https://discord.com/developers/applications))
- Docker instalado

### Clonar o Repositório

```bash
git clone https://github.com/LeonardoBaba/tibia-session.git
cd tibia-session
```

### Variáveis de ambiente
Crie um arquivo `.env` na raiz do projeto e preencha com as variáveis abaixo:

| Variável | Exemplo |
|---|---|
| `ANALYZER_DATABASE_CONNECTION` | `jdbc:postgresql://localhost:5432/analyzer` |
| `ANALYZER_DATABASE_USER` | `postgres` |
| `ANALYZER_DATABASE_PASSWORD` | `postgres` |
| `ANALYZER_SERVER_PORT` | `15600` |
| `ANALYZER_DISCORD_TOKEN` | seu token de bot do Discord |
| `ANALYZER_DISCORD_CLIENT_ID` | client ID OAuth2 da sua aplicação Discord |
| `ANALYZER_DISCORD_CLIENT_SECRET` | client secret OAuth2 da sua aplicação Discord |

### Executar
Com o arquivo `.env` configurado, inicie os contêineres utilizando o Docker Compose:
```bash
docker compose up
```

## Estrutura do projeto

```
tibia-session/
├── src/
│   ├── main/java/br/com/baba/tibia_analyzer/
│   │   ├── core/        # domínio: parsing, splitter, persistência, DTOs
│   │   ├── discord/     # bot do Discord (JDA): comandos, modais, embeds
│   │   └── api/         # API REST: controllers, auth (OAuth2), mappers
│   └── main/resources/
│       └── db/migration # migrations SQL do Flyway
├── frontend/            # interface web em Angular
├── .github/workflows/   # CI/CD (deploy.yml)
├── docker-compose.yml
├── Dockerfile
└── pom.xml
```

## Deploy

O processo de deploy é automatizado por CI/CD. Qualquer push realizado na branch `master` dispara a action `.github/workflows/deploy.yml`, que executa o fluxo: build da aplicação → geração e envio da imagem para o Docker Hub → deploy na VPS via SSH.
