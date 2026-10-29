# Refatoração SOLID do frontend

Plano escrito em 2026-10-29 para ser executado por outra sessão do Claude, uma etapa por sessão, nesta
ordem. É a trilha do frontend de um plano em duas partes. A trilha do backend, o diagnóstico geral e as
regras comuns estão em `docs/planos/refatoracao-solid.md` do repo `tibia-session`.

**Onde fica o código:** até a separação dos repos (plano `docs/planos/separar-front-e-back.md` do
`tibia-session`), na pasta `frontend/` do `tibia-session`; depois dela, na raiz do `tibia-session-front`. Os
caminhos abaixo são relativos à raiz do front (`src/app/...`).

## Objetivo

Componentes menores e lógica testável, sem mudar o que aparece na tela nem o que é enviado para a API. Hoje
há um único teste (`app.spec.ts`), e quase toda a lógica está dentro de componentes. Por isso, cada etapa
tira a lógica para funções puras ou serviços e escreve testes para eles.

## Como está (2026-10-29)

- Angular 21 com standalone components, signals e `OnPush`; Tailwind CSS 4; testes com Vitest pelo
  `ng test`.
- `npm ci`, `npx ng test --watch=false` (1 teste) e `npm run build` passam. Na VPS, se `node` não for
  encontrado, rode antes `source ~/.nvm/nvm.sh`.
- A VPS não tem navegador (nem Chromium), então não dá para ver as telas lá. A conferência visual fica para
  o Claude do PC do Leonardo, que tem navegador, ou para o próprio Leonardo.

## Diagnóstico

As linhas são de 2026-10-29 e podem ter mudado.

**S: responsabilidade única**
- `features/compare/compare.page.ts` tem 581 linhas e faz tudo:
  - carrega as sessões;
  - guarda o estado do pareamento de personagens (linhas 81-197 e 400-462);
  - trata o drag and drop (linhas 270-324);
  - monta as classes CSS (linhas 344-396);
  - calcula a tabela de métricas (linhas 464-549).
- `features/stamina/stamina.page.ts` tem 573 linhas e junta:
  - o simulador de stamina, com as regras do jogo (linhas 77-95 e 190-276);
  - a formatação de hora e duração (linhas 127-176);
  - a montagem da timeline (linhas 300-412);
  - os handlers do formulário.
- `features/hunt-detail/hunt-detail.page.ts` e `features/import/import.page.ts` repetem o relatório da
  sessão: o mesmo `buildEntries` (linhas 62-75 e 103-116), os mesmos três rankings e os mesmos componentes
  no template.

**O: aberto/fechado**
- A timeline da stamina é uma cadeia de `if`, um por tipo de evento (`stamina.page.ts`, linhas 310-403). Um
  tipo novo obriga a mexer no meio dessa função.

**L e I: contratos dos tipos**
- Em `core/models/session.model.ts`, `SessionDetail extends SessionSummary`, então o detalhe herda
  `memberCount`. Só que a API não manda esse campo no detalhe (`SessionDetailDTO` do back). O tipo promete
  um campo que não existe: um `SessionDetail` usado onde se espera `SessionSummary` (o `HuntCard` mostra
  `memberCount`) mostraria `undefined`. E quem só precisa dos campos comuns é obrigado a carregar os
  específicos de cada lado.

**D: inversão de dependência**
- `core/http/api-base-url.interceptor.ts` e `core/auth/auth.service.ts` importam o `environment`
  diretamente. O `AuthService` ainda tem `http://localhost:15600` fixo no código (linha 54). A configuração
  deveria entrar por injeção (`InjectionToken`), definida num lugar só, o `app.config.ts`.
- `features/import/import.page.ts` importa componentes internos de outra feature
  (`features/hunt-detail/components/...`, linhas 8-11).

**Duplicações e sobras** (cada uma é resolvida numa das etapas abaixo)
- A regra de cor de lucro e prejuízo está copiada em cinco lugares: `hunt-card.ts` (linhas 21-26),
  `party-members.ts` (32-36), `hunt-summary.ts` (43-53), `ranking-card.ts` (40-60) e `compare.page.ts`
  (215-219).
- `features/hunt-history/services/hunt-history.service.ts` é uma cópia do `SessionApiService.list`, e
  ninguém usa.
- `core/auth/auth.interceptor.ts` liga o `withCredentials` em qualquer URL que comece com `http`, não só na
  da API. Se o app um dia chamar outra API pública, essa chamada falha no CORS: o navegador recusa
  `Access-Control-Allow-Origin: *` em requisição com credenciais.
- `shared/components/editable-text/editable-text.ts` tem um `ngAfterViewInit` vazio (linha 53).

## Regras para todas as etapas

- **Não mude o que aparece na tela, as rotas nem as chamadas à API.** As rotas são `/hunts`, `/hunts/:id`,
  `/compare?ids=...`, `/stamina` e `/import`. O back gera links para `/hunts/{id}`.
- **Use uma branch por etapa,** `claude/solid-f<N>-<tema>`, criada a partir da branch principal atualizada
  (`main` no repo do front separado; `master` enquanto o código estiver no `tibia-session`).
- **Lógica nova vai para funções puras ou serviços, com testes** (`*.spec.ts` ao lado do arquivo).
  Componente de apresentação só recebe `input()` e emite `output()`.
- **A etapa está pronta quando:**
  - `npx ng test --watch=false` e `npm run build` passam;
  - o CI da branch está verde (depois da separação dos repos);
  - este arquivo marca a etapa como "pronta na branch X, falta merge";
  - o Leonardo recebeu o nome da branch, como testar e o que conferir na tela.
- **Atualize este arquivo direto na branch principal.** Use `[skip ci]` na mensagem do commit enquanto o
  workflow não ignorar `docs/**`.

## Onde o frontend vai chegar

```
core/
  config/api-config.ts       API_CONFIG (InjectionToken): o único lugar que lê o environment
  models/session.model.ts    SessionBase, SessionSummary e SessionDetail
  services/                  SessionApiService
  utils/session-duration.ts  (como hoje)
shared/
  utils/                     balance-tone, api-error e time-format
  session-report/            SessionReport, hunt-summary, ranking-card, party-members,
                             payment-distribution e ranking-entries
features/
  compare/                   ComparePage (carrega e compõe), PairingStore, compare-metrics,
                             PairingBoard e MetricTable
  stamina/                   StaminaPage (estado e composição), stamina-rules, stamina-simulator,
                             timeline-rows e os componentes do formulário e da timeline
  hunt-detail/, hunt-history/, import/   páginas finas
```

## Etapas

### F1. Rede de segurança: testes do que já existe

Só acrescenta testes.

- Teste o que já é puro: `core/utils/session-duration.ts` (`parseDurationToHours` e `perHour`) e os pipes
  `compact-number` e `signed-number`.
- Teste o `SessionApiService` com `HttpTestingController`: método, URL e parâmetros de cada chamada (a
  listagem não manda filtro vazio).
- Teste o `apiBaseUrlInterceptor` e o `authInterceptor` com o comportamento de hoje.
- **Pronto quando:** os testes passam.

### F2. Configuração da API por injeção

Princípio: D.

- Crie `core/config/api-config.ts` com `interface ApiConfig { baseUrl: string; origin: string }` e
  `API_CONFIG = new InjectionToken<ApiConfig>('API_CONFIG')`. Forneça o token no `app.config.ts`, a partir
  do `environment`.
- Nos `environments`, acrescente `apiOrigin`: `https://api.huntanalyzer.lbaba.com.br` em produção e
  `http://localhost:15600` em dev. O `apiUrl` continua como está.
- `apiBaseUrlInterceptor`, `authInterceptor` e `AuthService` passam a usar `inject(API_CONFIG)`.
- No `AuthService`, apague o `resolveApiOrigin()` e o `localhost` fixo. Para dar para testar, separe a URL
  de login num método `loginUrl()`; o `login()` só navega para ela.
- No `authInterceptor`, ligue o `withCredentials` só para chamadas da API: caminho que começa com `/api`
  (antes da troca de URL) ou URL que começa com o `baseUrl`. Hoje o app só chama a API, então nada muda na
  prática.
- **Pronto quando:** `grep -rn "environments/environment" src/app` só acha o `app.config.ts`; os testes da
  F1 passam, ajustados para fornecer o `API_CONFIG`; e um teste novo mostra que uma URL de outro domínio sai
  sem credenciais.

### F3. Modelos certos e utilitários sem duplicação

Princípios: L, I e S.

- Em `session.model.ts`, crie `SessionBase` com os campos comuns. `SessionSummary extends SessionBase`
  acrescenta `memberCount`; `SessionDetail extends SessionBase` acrescenta `members` e `transfers`.
- Apague `features/hunt-history/services/hunt-history.service.ts`, que ninguém usa.
- Crie `shared/utils/balance-tone.ts`, com `balanceTone(valor)`, que devolve `'profit'`, `'loss'` ou
  `'neutral'`, e as classes de texto e de barra de cada tom. Use nos cinco lugares listados no diagnóstico.
  **As classes CSS que saem têm que ser as mesmas de hoje.** Atenção: o tom neutro usa
  `--color-text-secondary` em alguns lugares e `--color-text-primary` em outros (`hunt-summary` e
  `ranking-card`). Mantenha cada lugar como está, por exemplo passando a classe do neutro como parâmetro.
- Crie `shared/utils/api-error.ts` com `apiErrorMessage(err, fallback)`, que lê o `message` do corpo de erro
  da API. Ele substitui o bloco com `@ts-expect-error` do `import.page.ts` (linhas 92-101).
- Apague o `ngAfterViewInit` vazio do `EditableText`.
- Escreva testes de `balanceTone` e `apiErrorMessage`.
- **Pronto quando:** os testes e o build passam (o `strictTemplates` acusa qualquer uso de `memberCount` no
  detalhe) e os componentes não têm mais cópias próprias do `if (balance > 0)`.

### F4. Relatório da sessão compartilhado

Princípios: S e D (uma feature deixa de importar componentes internos de outra).

- Mova `hunt-summary`, `ranking-card`, `party-members` e `payment-distribution` de
  `features/hunt-detail/components/` para `shared/session-report/`.
- Crie `shared/session-report/ranking-entries.ts` com a função pura `rankingEntries(session, metric, perHour)`.
  Ela substitui os dois `buildEntries` copiados e mantém o comportamento de hoje:
  - só entram membros com valor maior que zero;
  - no modo por hora, o valor é arredondado;
  - sem duração válida, fica o valor bruto.
- Crie o componente `SessionReport` (`<app-session-report [session] [(perHour)]>`). Ele mostra o resumo, os
  três rankings (Damage, Healing e Waste, com "/ h" no modo por hora), os membros e as transferências. As
  notas existem só no detalhe e ficam entre o resumo e os rankings; elas entram por `<ng-content>`.
- `HuntDetailPage` e `ImportPage` passam a usar o `SessionReport` e perdem os `computed` repetidos.
- Escreva testes de `rankingEntries`.
- **Pronto quando:** os testes e o build passam e `grep -rn "hunt-detail/components" src/app` não acha
  nada. Conferir na tela, iguais a antes:
  - `/hunts/<id>` logado como dono e sem login, com o "per hour" ligado e desligado;
  - `/import` no modo preview, sem login.

### F5. Comparação: lógica pura e estado fora do componente

Princípio: S. O template não muda nesta etapa.

- Crie `features/compare/compare-metrics.ts` com funções puras feitas a partir do `buildRows` e do
  `makeRow` de hoje (linhas 464-549). Elas recebem as sessões, os grupos, as atribuições e a opção "por
  hora", e devolvem as linhas da tabela.
- Crie `features/compare/pairing.store.ts`, um `@Injectable()` fornecido nos `providers` do `ComparePage`.
  Ele guarda os signals do pareamento (`groups`, `assign`, seleção e arrasto) e tem:
  - as operações `seed(sessions, auto)`, `assignChar`, `unassignChar`, `addGroup` e `removeGroup`;
  - os `computed` `trays`, `board` e `unassignedCount`.
- O `ComparePage` fica com o carregamento das sessões, o `showPerHour`, o `boardCollapsed` e a ligação
  entre o template e o store.
- Testes de caracterização:
  - no pareamento: o auto-match casa nomes sem diferenciar maiúsculas; personagem sem par vai para a
    bandeja; atribuir numa célula ocupada troca o personagem; remover um grupo devolve os personagens;
  - nas métricas: a melhor célula (maior valor, ou menor no custo de supplies), a escala por hora e as
    linhas "Hunt #N only".
- **Pronto quando:** os testes e o build passam. Conferir na tela `/compare?ids=<id1>,<id2>`: arrastar e
  soltar, clicar para atribuir, "auto match", "clear" e "per hour".

### F6. Comparação: componentes de apresentação

Princípio: S.

- Crie o `PairingBoard`, com as bandejas e o quadro de pareamento, o drag and drop e as classes
  `trayZoneClass`, `chipClass` e `slotClass`. Ele usa o `PairingStore` injetado pelo pai.
- Crie a `MetricTable`, uma tabela por métrica (damage, healing e supplies), com o `cellTextClass`.
- O `ComparePage` fica só com o carregamento e a composição.
- **Pronto quando:** os testes e o build passam. Conferir na tela como na F5, inclusive no celular, onde o
  quadro rola na horizontal.

### F7. Stamina: simulador puro e formatação

Princípio: S. O template não muda nesta etapa.

- Crie `features/stamina/stamina-rules.ts` com as constantes do jogo: `MAX` (42:00), `BONUS` (39:00), `LOW`
  (14:00), a regeneração de 1/3 e de 1/6 por minuto e a extensão de +1 h.
- Crie `features/stamina/stamina-simulator.ts` com a função pura
  `simulateStamina({ startMs, staminaMin, hunts, extensions })`, que devolve o `SimResult`. Ela vem do
  `computed` `sim` de hoje (linhas 190-276), junto com o `huntBonusMin`.
- Crie `shared/utils/time-format.ts` com `hhmm`, `durationLabel`, `clockLabel`, `toLocalInput` e
  `roundToMinute` (linhas 127-176). O `clockLabel` recebe por parâmetro se o relógio é de 24 h.
- Testes:
  - começar em 42:00 sem caçada termina com `fullT = 0`;
  - começar em 38:00 sem caçada: registre o `fullT` que o código de hoje dá;
  - uma caçada de 2 h que começa em 42:00 tem 120 min de bônus;
  - uma extensão soma 60 min sem passar de 42:00;
  - os formatadores.
- **Pronto quando:** os testes e o build passam. Conferir `/stamina` na tela.

### F8. Stamina: timeline extensível e componentes

Princípios: O e S.

- Crie `features/stamina/timeline-rows.ts` com um `Record<SimEventType, (evento, contexto) => TimelineRow>`
  no lugar da cadeia de `if` (linhas 310-403). Faça o filtro por tipo (linhas 405-411) também por tabela.
  Com o `Record`, o TypeScript não compila se faltar um tipo de evento, e um tipo novo vira só uma entrada
  nova.
- Crie componentes de apresentação para a entrada de stamina, a lista de caçadas, a lista de extensões e a
  timeline. O `StaminaPage` fica com o estado e a composição.
- Escreva um teste por tipo de evento, com a tag, os rótulos e as cores de hoje.
- **Pronto quando:** os testes e o build passam. Conferir `/stamina` na tela, com os filtros da timeline.

## Fora do escopo (anotado para depois)

- Gerar os tipos do front a partir de um OpenAPI do back (springdoc). Isso evitaria erros como o do
  `memberCount`, mas acrescenta uma dependência no back.
- Testes de tela com Playwright, quando houver navegador onde a sessão roda.
