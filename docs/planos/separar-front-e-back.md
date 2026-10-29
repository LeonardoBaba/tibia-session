# Separar o tibia-session em dois repositórios (front e back)

Plano escrito em 2026-10-29 para ser executado por outra sessão do Claude, uma etapa por sessão, nesta ordem.
Faça este plano antes do `docs/planos/refatoracao-solid.md`: ele é curto e deixa o CI rodando os testes em
qualquer branch, o que ajuda a validar cada etapa da refatoração.

Ao terminar uma etapa, marque-a aqui (data e branch) e faça commit e push deste arquivo direto na `master`.
**Até a etapa 3 chegar na `master`, todo push na `master` faz deploy em produção.** Por isso, commit que só
mexe em documentação leva `[skip ci]` na mensagem.

## Objetivo

Hoje o repositório `LeonardoBaba/tibia-session` (pasta `~/projetos/analyzer` na VPS) tem o backend na raiz e
o frontend Angular em `frontend/`. Ao final:

- o `tibia-session` tem só o backend (API REST e bot do Discord);
- o `tibia-session-front` (repo novo) tem só o frontend, com o histórico de commits dele;
- cada repo testa o próprio código, publica a própria imagem e faz o próprio deploy na VPS;
- nada muda para quem usa o site ou o bot: mesmos domínios, containers, variáveis e login.

## Como está hoje (2026-10-29)

- **Back:** Spring Boot 3.4.3, Java 21, Maven (`pom.xml` na raiz), Postgres com Flyway e bot do Discord (JDA).
  `./mvnw -B test -Dtest='!TibiaAnalyzerApplicationTests'` passa com 51 testes. O
  `TibiaAnalyzerApplicationTests` sobe o contexto inteiro e falha sem um Postgres.
- **Front:** Angular 21, Tailwind CSS 4 e Vitest, em `frontend/`. `npm ci`, `npx ng test --watch=false`
  (1 teste) e `npm run build` passam. A imagem é um nginx com o build (`frontend/Dockerfile`,
  `frontend/nginx.conf`).
- **Produção:** fica na VPS, atrás de um Caddy compartilhado. O Caddy não está em nenhum repo e alcança os
  containers pela rede Docker `proxy-net`.
  - `https://huntanalyzer.lbaba.com.br` → container `analyzer-frontend` (serviço `frontend`, imagem
    `${DOCKER_USERNAME}/analyzer-frontend:latest`).
  - `https://api.huntanalyzer.lbaba.com.br` → container `analyzer-app` (porta 15600, imagem
    `${DOCKER_USERNAME}/analyzer:latest`), que usa o banco `analyzer-db`.
  - Os três containers vêm de um único `docker-compose.yml`, copiado para `/root/analyzer/`. O projeto
    compose se chama `analyzer`.
- **CI/CD:** `.github/workflows/deploy.yml` roda a cada push na `master`. Ele gera as duas imagens, publica no
  Docker Hub e roda `docker compose pull && docker compose up -d` na VPS por SSH. Não roda testes (o
  `Dockerfile` usa `-DskipTests`). Secrets do repo: `DOCKER_USERNAME`, `DOCKER_PASSWORD`, `VPS_HOST`,
  `VPS_SSH_KEY`, `DB_USER`, `DB_PASSWORD`, `DISCORD_TOKEN`, `DISCORD_CLIENT_ID` e `DISCORD_CLIENT_SECRET`.

Em produção, front e back já funcionam separados: domínios diferentes, CORS e cookie no domínio
`lbaba.com.br`. O que os une é só o repositório, o workflow e o `docker-compose.yml`.

## Pontos de contato entre front e back

A separação não muda nenhum deles. Depois dela, cada lado fica num repo, e nada avisa quando um quebra o
outro. Esta tabela diz onde olhar.

| O quê | No back | No front |
|---|---|---|
| Endereço da API | domínio `api.huntanalyzer.lbaba.com.br`, porta 15600 | `src/environments/environment.ts` (`apiUrl`). Em dev, `proxy.conf.json` repassa `/api` para `http://localhost:15600`, e `src/app/core/auth/auth.service.ts` (`resolveApiOrigin`) usa esse endereço no login |
| Endereço do front | `ANALYZER_FRONTEND_URL` (`docker-compose.yml`), usado no redirect depois do login (`api/config/SecurityConfig.java`: `/` e `/?login=failed`) e no link do embed do Discord (`discord/embed/PartyHuntEmbedFactory.java`: `/hunts/{id}`) | `src/app/app.routes.ts`: a rota `hunts/:id` tem que continuar existindo |
| CORS | `api/config/WebConfig.java`, origens fixas no código: `http://localhost:4200` e `https://huntanalyzer.lbaba.com.br` | — |
| Cookie de sessão | `application.properties` e `ANALYZER_COOKIE_DOMAIN=lbaba.com.br`, com `SameSite=lax` e `Secure` | `src/app/core/auth/auth.interceptor.ts` (`withCredentials`). Front e API precisam continuar sob `lbaba.com.br` |
| Login com Discord | `/oauth2/authorization/discord`, com retorno em `/login/oauth2/code/discord` (endereço cadastrado no Discord Developer Portal) | `AuthService.login()` |
| Formato do JSON | `api/dto/*.java` e `api/exception/ApiExceptionHandler.java` | `src/app/core/models/session.model.ts`, `src/app/core/auth/user.model.ts` e `src/app/core/services/session-api.service.ts` |
| Link "View on GitHub" | — | `src/app/layout/app-shell/app-shell.html` aponta para o `tibia-session` e continua assim |

Os caminhos do back são relativos a `src/main/java/br/com/baba/tibia_analyzer/`. Os do front são relativos à
raiz do front: hoje a pasta `frontend/`, depois da separação a raiz do repo novo.

## Decisões

1. **O repo atual vira o back.** O `tibia-session` fica com o backend, o histórico, os secrets, a imagem
   `analyzer` e a pasta `/root/analyzer` da VPS. Como o back já está na raiz, nenhum caminho muda.
2. **O front vai para o repo novo `LeonardoBaba/tibia-session-front`, público.** O `tibia-session` é
   público, e esse código já está publicado nele. A regra do `~/projetos/CLAUDE.md` de criar repo privado
   vale para projeto novo, e aqui o código só muda de lugar. Se o Leonardo preferir privado, troque
   `--public` por `--private` na etapa 1.
3. **O histórico do front vai junto**, com `git subtree split --prefix=frontend`. Testado em 2026-10-29:
   gera 20 commits em menos de 1 s, com o conteúdo de `frontend/` na raiz.
4. **Em produção, só muda quem faz o deploy do front.** Ficam iguais o nome da imagem
   (`analyzer-frontend:latest`), o nome do serviço (`frontend`), o nome do container (`analyzer-frontend`) e
   a rede (`proxy-net`). Assim o Caddy não precisa mudar, aponte ele para o nome do container ou para o do
   serviço.
5. **Na virada, primeiro o back e depois o front.** O back deixa de declarar o serviço `frontend`. O
   container antigo continua no ar, mas órfão: nenhum compose cuida mais dele. No primeiro deploy, o front
   remove esse container e cria o seu. **Não use `--remove-orphans` no deploy do back durante a transição**,
   porque isso derrubaria o site.
6. **O CI dos dois repos segue o modelo do daily-report.** Os testes rodam em qualquer branch. A publicação
   da imagem e o deploy só acontecem na branch principal (`master` no back, `main` no front). Commits que só
   mexem em `docs/**` ou em `*.md` não disparam o workflow.
7. **Renomear o `tibia-session` para `tibia-session-back` é opcional** e fica para o fim (etapa 5).

## Etapas

Os comandos supõem a VPS (`~/projetos`). Se `java` ou `node` não forem encontrados, rode antes
`source ~/.sdkman/bin/sdkman-init.sh` e `source ~/.nvm/nvm.sh`. Antes de mexer em qualquer repo, `git pull`.

### 1. Criar o repo do front com o histórico (sem deploy)

Nada vai para produção nesta etapa.

1. Atualize o repo atual e extraia o histórico do front para uma branch local:
   ```bash
   cd ~/projetos/analyzer && git checkout master && git pull
   git subtree split --prefix=frontend -b front-split
   ```
2. Crie o repo local novo a partir dela. Depois apague a branch temporária, que nunca vai para o GitHub:
   ```bash
   mkdir ~/projetos/tibia-session-front && cd ~/projetos/tibia-session-front
   git init -b main && git pull ~/projetos/analyzer front-split
   git -C ~/projetos/analyzer branch -D front-split
   ```
3. Acrescente na raiz do repo novo:
   - `README.md` (em inglês) e `README.pt-BR.md`, no mesmo formato dos READMEs do `tibia-session`, com:
     - o que é e o link do Live Demo;
     - o link para o repo do back (API e bot);
     - a stack: Angular 21, Tailwind CSS 4 e Vitest;
     - como rodar local: Node 24; suba o back do `tibia-session` na porta 15600; depois `npm ci` e
       `npm start` → http://localhost:4200 (o `proxy.conf.json` repassa `/api`);
     - testes (`npx ng test --watch=false`) e build (`npm run build` → `dist/frontend/browser`);
     - deploy: "por enquanto, feito pelo repo tibia-session".
   - `.github/workflows/ci.yml` com o conteúdo abaixo. Ele não usa secrets.
4. Não renomeie o projeto Angular `frontend` (em `angular.json` e `package.json`): o `Dockerfile` copia de
   `dist/frontend/browser`.
5. Valide local: `npm ci && npx ng test --watch=false && npm run build`.
6. Faça o commit e crie o repo no GitHub:
   ```bash
   git add -A && git commit -m "chore: set up standalone frontend repository"
   gh repo create LeonardoBaba/tibia-session-front --public --source . --push \
     --description "Web UI (Angular) of Tibia Session" --homepage https://huntanalyzer.lbaba.com.br
   ```
7. Na VPS, acrescente o projeto na lista do `~/projetos/CLAUDE.md`: "`tibia-session-front/` → GitHub
   `LeonardoBaba/tibia-session-front` (Angular; frontend do tibia-session, separado em AAAA-MM-DD)". Na linha
   do `analyzer/`, avise que a partir daqui mudanças no front vão para o repo novo e que o `frontend/` dali
   vai ser apagado na etapa 4.

`.github/workflows/ci.yml` do repo novo:

```yaml
name: CI/CD

# Build e testes em qualquer branch. A publicação da imagem e o deploy entram na etapa 3 do
# docs/planos/separar-front-e-back.md do tibia-session.
on:
  push:
    paths-ignore:
      - 'docs/**'
      - '**.md'
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout do código
        uses: actions/checkout@v4

      - name: Configurar Node 24
        uses: actions/setup-node@v4
        with:
          node-version: '24'
          cache: npm

      - name: Dependências
        run: npm ci

      - name: Testes
        run: npx ng test --watch=false

      - name: Build de produção
        run: npm run build

  image:
    needs: build
    runs-on: ubuntu-latest
    steps:
      - name: Checkout do código
        uses: actions/checkout@v4

      - name: Build da imagem (sem publicar)
        uses: docker/build-push-action@v5
        with:
          context: .
          push: false
```

**Como verificar**
- `git log --oneline | tail -1` no repo novo mostra `feat: scaffold Angular frontend with Hunt History card
  listing`, e `ls` mostra `package.json`, `src/`, `Dockerfile` e `docs/planos/` na raiz.
- `gh run list -R LeonardoBaba/tibia-session-front --limit 1` termina com `success`, com os jobs `build` e
  `image` verdes.
- Produção não mudou.

### 2. Secrets do repo novo (Leonardo, manual)

O Claude não consegue fazer esta etapa: o GitHub não mostra o valor dos secrets, então não dá para copiá-los
de outro repo.

No GitHub, em `tibia-session-front` → Settings → Secrets and variables → Actions, crie estes secrets com os
mesmos valores do `tibia-session`:

- `DOCKER_USERNAME`;
- `DOCKER_PASSWORD`: é um token do Docker Hub, e dá para gerar um novo em Account settings → Personal access
  tokens;
- `VPS_HOST`;
- `VPS_SSH_KEY`: a mesma chave que o `tibia-session` e o `daily-report` usam.

No PC também dá para criar com `gh secret set NOME -R LeonardoBaba/tibia-session-front`, que pede o valor.

**Como verificar:** `gh secret list -R LeonardoBaba/tibia-session-front` lista os quatro nomes.

### 3. Virada do deploy

Pré-requisito: `gh secret list -R LeonardoBaba/tibia-session-front` mostra os quatro secrets. Se não
mostrar, pare e peça ao Leonardo a etapa 2.

A sessão prepara uma branch em cada repo. Quem faz o merge é o Leonardo, **na ordem descrita abaixo**.

**3a. Back: branch `claude/separar-front` no `tibia-session`**

- `docker-compose.yml`: apague o serviço `frontend`. Mantenha `app`, `db`, os volumes e as redes.
- `.github/workflows/deploy.yml`:
  - troque o bloco `on:` por:
    ```yaml
    # Testes em qualquer branch; imagem e deploy na VPS só a partir da master.
    # Commits que só mexem em docs/ ou em arquivos .md não disparam o workflow.
    on:
      push:
        paths-ignore:
          - 'docs/**'
          - '**.md'
      workflow_dispatch:
    ```
  - acrescente este job no começo de `jobs:`:
    ```yaml
      test:
        runs-on: ubuntu-latest
        steps:
          - name: Checkout do código
            uses: actions/checkout@v4

          - name: Configurar Java 21
            uses: actions/setup-java@v4
            with:
              java-version: '21'
              distribution: 'temurin'
              cache: maven

          - name: Dar permissão de execução ao Maven Wrapper
            run: chmod +x mvnw

          # TibiaAnalyzerApplicationTests sobe o contexto inteiro e precisa de Postgres.
          - name: Testes
            run: ./mvnw -B test -Dtest='!TibiaAnalyzerApplicationTests'
    ```
  - no job `build-and-push-backend`, acrescente `needs: test` e `if: github.ref == 'refs/heads/master'`;
  - apague o job `build-and-push-frontend`;
  - no job `deploy`, troque o `needs` por `build-and-push-backend` e acrescente o mesmo `if`. O resto do job
    fica como está, e continua sem `--remove-orphans`.
- **Como verificar:** `./mvnw -B test -Dtest='!TibiaAnalyzerApplicationTests'` passa com 51 testes. Depois
  do push da branch, o job `test` fica verde no GitHub, e `build-and-push-backend` e `deploy` aparecem como
  pulados.

**3b. Front: branch `claude/deploy` no `tibia-session-front`**

- `docker-compose.yml` na raiz, com o conteúdo abaixo.
- `.github/workflows/ci.yml`: troque pelo conteúdo abaixo. Ele publica a imagem e faz deploy só na `main`, e
  o primeiro deploy remove o container antigo.
- Nos dois READMEs, a seção de deploy: push na `main` → testes → imagem `analyzer-frontend:latest` no
  Docker Hub → `/root/tibia-session-front` na VPS, rede `proxy-net`, atrás do Caddy.
- **Como verificar:** depois do push da branch, `build` e `image` ficam verdes (a imagem é gerada sem
  publicar) e `deploy` aparece como pulado.

`docker-compose.yml`:

```yaml
# Fica na VPS em /root/tibia-session-front. O deploy do GitHub Actions copia este arquivo e cria o .env.
# O Caddy (rede proxy-net) encaminha huntanalyzer.lbaba.com.br para este container.
# Não mude o nome do serviço nem o container_name: a configuração do Caddy depende deles.
name: tibia-session-front

services:
  frontend:
    image: ${DOCKER_USERNAME}/analyzer-frontend:latest
    container_name: analyzer-frontend
    restart: always
    networks:
      - proxy-net

networks:
  proxy-net:
    external: true
```

`.github/workflows/ci.yml`:

```yaml
name: CI/CD

# Build e testes em qualquer branch; publicação da imagem e deploy na VPS só a partir da main.
# Commits que só mexem em docs/ ou em arquivos .md não disparam o workflow.
on:
  push:
    paths-ignore:
      - 'docs/**'
      - '**.md'
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout do código
        uses: actions/checkout@v4

      - name: Configurar Node 24
        uses: actions/setup-node@v4
        with:
          node-version: '24'
          cache: npm

      - name: Dependências
        run: npm ci

      - name: Testes
        run: npx ng test --watch=false

      - name: Build de produção
        run: npm run build

  image:
    needs: build
    runs-on: ubuntu-latest
    steps:
      - name: Checkout do código
        uses: actions/checkout@v4

      - name: Login no Docker Hub
        if: github.ref == 'refs/heads/main'
        uses: docker/login-action@v3
        with:
          username: ${{ secrets.DOCKER_USERNAME }}
          password: ${{ secrets.DOCKER_PASSWORD }}

      - name: Build da imagem (publica só na main)
        uses: docker/build-push-action@v5
        with:
          context: .
          push: ${{ github.ref == 'refs/heads/main' }}
          tags: ${{ secrets.DOCKER_USERNAME }}/analyzer-frontend:latest

  deploy:
    needs: image
    if: github.ref == 'refs/heads/main'
    runs-on: ubuntu-latest
    steps:
      - name: Checkout do código
        uses: actions/checkout@v4

      - name: Copia o docker-compose.yml para a VPS
        uses: appleboy/scp-action@v0.1.7
        with:
          host: ${{ secrets.VPS_HOST }}
          username: root
          key: ${{ secrets.VPS_SSH_KEY }}
          source: docker-compose.yml
          target: /root/tibia-session-front/
          overwrite: true

      - name: Sobe o container
        uses: appleboy/ssh-action@v1.0.3
        with:
          host: ${{ secrets.VPS_HOST }}
          username: root
          key: ${{ secrets.VPS_SSH_KEY }}
          script: |
            set -e
            cd /root/tibia-session-front
            echo "DOCKER_USERNAME=${{ secrets.DOCKER_USERNAME }}" > .env
            docker compose pull
            # Só age no primeiro deploy: o analyzer-frontend antigo foi criado pelo compose do
            # tibia-session (projeto "analyzer") e precisa sair para este projeto criar o seu.
            dono=$(docker inspect -f '{{ index .Config.Labels "com.docker.compose.project" }}' analyzer-frontend 2>/dev/null || true)
            if [ -n "$dono" ] && [ "$dono" != "tibia-session-front" ]; then docker rm -f analyzer-frontend; fi
            docker compose up -d
            docker compose ps
```

**Merge (Leonardo), nesta ordem**

1. `claude/separar-front` na `master` do `tibia-session`. Espere o workflow terminar verde. O back é
   redeployado e o site continua no ar com o container antigo. O log do deploy avisa que há um container
   órfão, e isso é esperado.
2. `claude/deploy` na `main` do `tibia-session-front`. O workflow publica a imagem e troca o container
   antigo pelo novo. O site fica fora do ar por alguns segundos.

**Como verificar, depois dos dois merges**

```bash
gh run list -R LeonardoBaba/tibia-session --limit 1          # completed  success
gh run list -R LeonardoBaba/tibia-session-front --limit 1    # completed  success
gh run view <id do run do front> -R LeonardoBaba/tibia-session-front --log | grep analyzer-frontend
#   → linha do "docker compose ps" com o container Up
curl -s -o /dev/null -w '%{http_code}\n' https://huntanalyzer.lbaba.com.br/                            # 200
curl -s https://huntanalyzer.lbaba.com.br/hunts/teste | grep -c '<app-root'                            # 1
curl -s -o /dev/null -w '%{http_code}\n' 'https://api.huntanalyzer.lbaba.com.br/api/sessions?size=1'   # 200
curl -s -o /dev/null -w '%{http_code}\n' https://api.huntanalyzer.lbaba.com.br/api/auth/me             # 401
```

O segundo `curl` confere que o nginx devolve o app para qualquer rota. Depois, o Leonardo confere no
navegador: abrir o site, entrar com o Discord, abrir uma hunt, renomear e sair. No Discord, usar `/loot` com
um analyzer e abrir o link do embed, que tem que levar para a hunt.

**Se der errado**

- Se o deploy do front falhar antes de remover o container antigo, o site continua no ar. Corrija e rode de
  novo (`gh run rerun <id> -R LeonardoBaba/tibia-session-front`). Se o `scp` reclamar que a pasta não
  existe, acrescente antes dele um passo de SSH com `mkdir -p /root/tibia-session-front`.
- Se o site cair depois do merge 2 e a correção não for rápida, o Leonardo roda na VPS, como root:
  ```bash
  cd /root/tibia-session-front && docker compose down
  ```
  Depois ele reverte o merge do `tibia-session`, pelo botão Revert do PR ou com `git revert -m 1 <commit do
  merge>` na `master`. O workflow antigo volta a gerar o front e a subi-lo pelo projeto `analyzer`.

### 4. Tirar o `frontend/` do back

Branch `claude/remover-frontend` no `tibia-session`.

- Rode `git rm -r frontend` e depois `rm -rf frontend`. O segundo apaga o que sobrar: `node_modules`, `dist`
  e `.angular`, que o git ignora.
- Nos READMEs (`README.md` e `README.pt-BR.md`):
  - diga que este repo é a API e o bot, e que o front está em
    https://github.com/LeonardoBaba/tibia-session-front;
  - tire o `frontend/` da estrutura do projeto;
  - na tabela de variáveis, acrescente as que faltam hoje: `ANALYZER_FRONTEND_URL` (padrão
    `http://localhost:4200`), `ANALYZER_COOKIE_SECURE` (padrão `false`) e `ANALYZER_COOKIE_DOMAIN` (padrão
    vazio);
  - deixe a seção de deploy só com o back.
- Crie `docs/contrato-api.md` com o contrato de que o front depende:
  - cada endpoint, com método, caminho, se exige login, parâmetros e status de resposta;
  - o JSON de cada DTO e o formato de erro;
  - a paginação padrão (`size=20`, `sort=startTime,desc`);
  - o cookie e o CORS;
  - as rotas do front que o back usa em links: `/hunts/{id}`, `/` e `/?login=failed`.

  As fontes são `api/dto/`, `api/controller/SessionController.java`, `api/auth/AuthController.java`,
  `api/config/`, `api/exception/ApiExceptionHandler.java` e `discord/embed/PartyHuntEmbedFactory.java`.
  Termine o arquivo com: "Mudou algo aqui? Atualize `src/app/core/models/` no tibia-session-front."
- **Como verificar:**
  - `./mvnw -B test -Dtest='!TibiaAnalyzerApplicationTests'` passa;
  - `git grep -n "frontend/"` só acha as menções esperadas (READMEs e contrato);
  - o CI da branch fica verde.
- Depois do merge, o Leonardo faz no PC: `git pull` no `tibia-session`, apaga a pasta `frontend/` que
  sobrar (só com arquivos ignorados) e clona o `tibia-session-front` ao lado. Na VPS, a sessão faz o mesmo
  no `~/projetos/analyzer`.

### 5. (Opcional) Renomear o `tibia-session` para `tibia-session-back`

Só se o Leonardo pedir.

- Rode `gh repo rename tibia-session-back -R LeonardoBaba/tibia-session`. O GitHub redireciona os endereços
  antigos, mas o redirecionamento para de funcionar se um dia alguém criar outro repo chamado
  `tibia-session`.
- Atualize:
  - o `origin` dos clones: `git remote set-url origin https://github.com/LeonardoBaba/tibia-session-back.git`;
  - os links nos READMEs dos dois repos;
  - o link "View on GitHub" do front, em `src/app/layout/app-shell/app-shell.html` (numa branch no front);
  - o `~/projetos/CLAUDE.md`.
- Docker Hub, pasta da VPS e containers continuam com os nomes `analyzer*`. Não precisa mexer neles.

## Fora do escopo

- Tornar configuráveis as origens do CORS e o endereço da API: os domínios não mudam com a separação. O
  CORS está no `docs/planos/refatoracao-solid.md` (etapa B7) e o endereço da API, no plano do front (F2).
- Fazer o `TibiaAnalyzerApplicationTests` rodar no CI, o que exige um Postgres no CI.
- Atualizar as versões das actions, do Spring Boot ou do Angular.
