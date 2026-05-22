# CLAUDE.md

Guia para o Claude Code trabalhar neste repositório.

## Visão geral

Bot de Discord que recebe o texto do **Party Hunt Analyzer** do jogo Tibia,
faz o parsing da sessão e divide o lucro/prejuízo entre os participantes,
calculando quem deve transferir quanto para quem. As sessões processadas são
persistidas no PostgreSQL.

Objetivos futuros (ver [Próximos passos](#próximos-passos)):
- API REST para um front-end Angular.
- Comparação entre hunts.
- Estatísticas (dano total, dano por hora, etc.).

## Stack

- **Java 21** + **Spring Boot 3.4.3** (Maven, via wrapper `mvnw`)
- **JDA 5.3.1** — integração com o Discord
- **PostgreSQL** + **Flyway** (migrations)
- **Spring Data JPA** / **Lombok**
- `spring-boot-starter-web` já presente (ainda sem controllers REST)

## Como rodar localmente

O Postgres roda local (ou em container) e a aplicação é executada pela IDE.
A app **exige** estas variáveis de ambiente — não há valores default:

| Variável | Exemplo |
|---|---|
| `ANALYZER_DATABASE_CONNECTION` | `jdbc:postgresql://localhost:5432/analyzer` |
| `ANALYZER_DATABASE_USER` | `postgres` |
| `ANALYZER_DATABASE_PASSWORD` | `postgres` |
| `ANALYZER_SERVER_PORT` | `15600` |
| `ANALYZER_DISCORD_TOKEN` | token do bot no Discord |

O Flyway aplica as migrations de `src/main/resources/db/migration` no startup.

## Comandos

```bash
./mvnw clean package      # build (gera o jar em target/)
./mvnw test               # roda os testes
./mvnw spring-boot:run    # roda a aplicação
```

## Arquitetura

Pacote raiz: `br.com.baba.tibia_analyzer`. Dividido em dois domínios:

### `core` — regra de negócio
- `util/PartyAnalyzerConverter` — parseia o texto do analyzer via regex para `PartyHuntAnalyzerDTO`.
- `util/PartyHuntSplitter` — calcula a divisão e as estatísticas: soma loot/supplies, define a parte igual de cada jogador, gera as transferências por um algoritmo credor/devedor e os rankings de dano/cura. Devolve um `SessionResultDTO` (dados estruturados, não texto).
- `service/PartyHuntService` — orquestra: converter → splitter → persiste via `PartySessionDAO`; retorna o `SessionResultDTO`.
- `model/` — entidades JPA (`PartySession`, `PartyMember`, `PartyTransfer`).
- `dto/` — records de transporte (`PartyHuntAnalyzerDTO`, `SessionResultDTO`, `PlayerStatDTO`, etc.).
- `dao/` — repositórios Spring Data.

### `discord` — camada de interação
Fluxo de uma interação:
1. `BotInitializer` sobe o JDA e registra os slash commands.
2. `SlashCommandHandler` (listener) recebe os eventos.
3. Slash command `/loot` → `LootCommandHandler` abre um modal.
4. Submissão do modal → `PartyHuntHandler` → chama `PartyHuntService` e
   renderiza o resultado num embed via `embed/PartyHuntEmbedFactory`.

Comandos e modais são registrados em enums (`CommandEnum`, `ModalEnum`,
`InputEnum`); as factories resolvem o handler via `ApplicationContext`.
**Para adicionar um comando/modal novo:** crie o handler e registre-o no enum
correspondente — as factories e o listener cuidam do resto.

## Banco de dados

- Tabelas: `party_session` (1) → `party_member` (N) e `party_transfer` (N).
- Migrations Flyway em `src/main/resources/db/migration`, nomeadas
  `V{n}__descricao.sql`. **Nunca edite uma migration já aplicada** — crie uma nova.

## Convenções

- **Código** em inglês (identificadores, nomes de classes).
- **Commits**: conventional commits em inglês (`feat:`, `fix:`, `refactor:`, `test:`).
- **Testes**: sempre criar/atualizar testes JUnit ao adicionar ou alterar
  lógica — especialmente no pacote `core` (converter, splitter, service).
  Há exemplos em `src/test/java/.../core`.
- Comentários só quando o "porquê" não for óbvio.

## Próximos passos

- **API REST** para o front Angular: criar controllers (pacote sugerido
  `api` ou `web`) expondo as sessões, comparação de hunts e estatísticas.
  Reaproveitar `PartyHuntService` e os DTOs do `core`.
- O front Angular ficará em `frontend/` (hoje só com scaffolding vazio).

## Deploy

`git push` na branch `master` dispara o workflow `.github/workflows/deploy.yml`:
build → imagem Docker no Docker Hub → deploy na VPS via SSH (`docker compose`).
Não force push em `master`.
