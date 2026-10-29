# Refatoração SOLID do tibia-session

Plano escrito em 2026-10-29 para ser executado por outra sessão do Claude, uma etapa por sessão. Tem duas
trilhas que não dependem uma da outra; dentro de cada trilha, siga a ordem.

- **Backend:** este arquivo, etapas B1 a B8, no repo `tibia-session`.
- **Frontend:** `frontend/docs/planos/refatoracao-solid.md`, etapas F1 a F8. Depois da separação dos repos,
  esse arquivo fica em `docs/planos/refatoracao-solid.md` do `tibia-session-front`. Ele mora junto do código
  do front para ir com ele na separação.

Faça antes o `docs/planos/separar-front-e-back.md`. Ele é curto e deixa o CI rodando os testes em qualquer
branch, o que ajuda a validar cada etapa daqui.

## Objetivo

Deixar o código mais fácil de mudar e de testar, aplicando SOLID onde há ganho real e sem mudar o
comportamento: mesma API (caminhos, status e JSON), mesmos comandos e mensagens do Discord e mesmas telas.
As únicas mudanças de comportamento são correções, e cada etapa que tem uma avisa:

- entrada inválida passa a dar 400 em vez de 500;
- o bot passa a responder a uma entrada inválida em vez de ficar "pensando...";
- uma falha ao salvar o usuário no login leva para `/?login=failed`, em vez de dar erro 500.

## Como está (2026-10-29)

- São cerca de 1.700 linhas de Java em `src/main/java/br/com/baba/tibia_analyzer/`, em três pacotes:
  - `core`: domínio, parser do texto do analyzer, divisão do loot e JPA;
  - `api`: REST e login com Discord;
  - `discord`: o bot (JDA).

  A divisão em pacotes faz sentido. Os problemas estão na direção das dependências e em algumas classes com
  responsabilidade demais.
- Testes: `./mvnw -B test -Dtest='!TibiaAnalyzerApplicationTests'` passa com 51 testes. O
  `TibiaAnalyzerApplicationTests` sobe o contexto inteiro e precisa de Postgres, por isso fica de fora.
- Na VPS: `cd ~/projetos/analyzer`. Se `java` não for encontrado, rode antes
  `source ~/.sdkman/bin/sdkman-init.sh`.

## Diagnóstico do backend

Caminhos relativos a `src/main/java/br/com/baba/tibia_analyzer/`. As linhas são de 2026-10-29 e podem ter
mudado.

**S: responsabilidade única**
- `core/service/PartyHuntService.java` faz cinco coisas: roda o parser e a divisão, cria, consulta, edita e
  apaga sessões. Tem sobrecargas de `processSession` (linhas 31-40) que só os testes usam e um record
  interno, `ProcessedSession` (linha 94), que o bot também usa.
- `core/util/PartyHuntSplitter.java` soma os totais, interpreta a duração `"01:29h"` (linhas 41-53), monta
  os rankings e calcula as transferências (linhas 67-97).
- As entidades `core/model/PartySession.java` (linhas 46-65), `PartyMember.java` (29-36) e
  `PartyTransfer.java` (26-30) se montam a partir dos DTOs do parser. Ou seja, o modelo do banco depende do
  formato da análise.
- `api/controller/SessionController.java` decide quem é o dono da sessão (`isOwner`, linhas 127-131) e
  monta as respostas 403 e 404 em cada método (linhas 82-107). "Só o dono edita ou apaga" é regra de
  negócio, mas está na camada web: outro ponto de entrada, como um comando novo no bot, passaria por cima
  dela.
- `discord/interactions/modals/PartyHuntHandler.java` lê o modal, registra o usuário, processa e salva a
  sessão, monta o arquivo e o embed e responde.

**O: aberto/fechado**
- Para criar um comando no bot, além do handler, é preciso mexer em `discord/enums/CommandEnum.java` (ou
  `ModalEnum.java`). As fábricas `discord/service/*HandlerFactory.java` buscam o handler no
  `ApplicationContext` pela classe guardada no enum.
- `api/config/WebConfig.java` tem as origens do CORS fixas no código (linhas 14-17).
- O status de erro é decidido em cada método do controller. Um tipo novo de erro na API obriga a mexer em
  todos eles.

**L: substituição de Liskov (contratos)**
- O contrato implícito do parser é: entrada inválida gera `ConverterException`. É isso que
  `api/exception/ApiExceptionHandler.java` e `discord/listener/SlashCommandHandler.java` esperam. Mas nada
  lança `ConverterException`. Na prática, reproduzido em 2026-10-29:
  - texto qualquer gera `NumberFormatException`, que vira 400 por acaso (o handler também trata essa
    exceção);
  - data inválida gera `DateTimeParseException` e a API devolve **500**;
  - cabeçalho sem nenhum jogador gera `ArithmeticException: / by zero` no `PartyHuntSplitter` (linha 30) e
    a API devolve **500**;
  - no Discord, o `PartyHuntHandler` já chamou `deferReply()` (linha 33) e o listener só captura
    `ConverterException`. Com qualquer um desses erros, o usuário fica vendo "pensando..." para sempre.
- `api/auth/DiscordOAuth2UserService.java` estende `DefaultOAuth2UserService`, cujo contrato só permite
  lançar `OAuth2AuthenticationException`. Se salvar o usuário falhar, sai outra exceção, e o login dá erro
  500 em vez de voltar para `/?login=failed`.

**I: segregação de interfaces**
- Quem usa o `PartyHuntService` usa só uma parte dele: o bot, só `processAndPersist`; o preview, só
  `previewSession`.
- `api/dto/CreateSessionRequest.java` tem o campo `ownerDiscordId`, que o servidor ignora de propósito (por
  segurança). O mesmo DTO é usado no `/preview`, que só lê o `input`.

**D: inversão de dependência**
- `core` depende de `api`: `core/dao/PartySessionSpecifications.java` (linha 3) e
  `core/service/PartyHuntService.java` (linha 3) importam `api.dto.SessionFilter`.
- Dez classes recebem dependências por `@Autowired` em atributo: `PartyHuntService`, `UserService`,
  `SessionController`, `AuthController`, `DiscordOAuth2UserService`, `SecurityConfig`,
  `SlashCommandHandler`, `CommandHandlerFactory`, `ModalHandlerFactory` e `PartyHuntHandler`. Assim as
  dependências ficam escondidas e não podem ser `final`.
- Os controllers conhecem o `OAuth2User` do Spring Security e o nome do atributo do Discord
  (`principal.getAttribute("id")`): `SessionController`, linhas 46 e 129, e `AuthController`, linha 30.
- As fábricas do bot usam o `ApplicationContext` para achar os handlers (service locator).
- O construtor de `PartySession` chama `new Date()`, então a hora não pode ser controlada nos testes.
- O app depende do Discord para subir: `discord/BotInitializer.java` sempre cria o JDA.

## Regras para todas as etapas

- **Não mude o comportamento externo**, a não ser onde a etapa disser. Isso inclui:
  - os caminhos, os status e os nomes e tipos dos campos do JSON. O front depende deles, em
    `src/app/core/models/` (`frontend/src/app/core/models/` enquanto os repos não forem separados);
  - os textos e comandos do Discord;
  - as tabelas e colunas do banco. Nenhuma etapa precisa de migration.
- **Crie interface só onde há variação ou fronteira de verdade.** Serviço do Spring com uma implementação
  só não ganha interface. Este plano cria uma única interface (`PartyHuntParser`, na B5) e explica por quê.
- **Use uma branch por etapa,** `claude/solid-b<N>-<tema>`, criada a partir da `master` atualizada. Se a
  etapa anterior ainda não foi mergeada, crie a partir da branch dela e avise o Leonardo.
- **A etapa está pronta quando:**
  - `./mvnw -B test -Dtest='!TibiaAnalyzerApplicationTests'` passa. Os testes de hoje continuam passando,
    ajustados só onde a etapa muda uma assinatura;
  - o CI da branch está verde (depois do plano de separação);
  - este arquivo marca a etapa como "pronta na branch X, falta merge";
  - o Leonardo recebeu o nome da branch e como testar.
- **Atualize este arquivo direto na `master`.** Use `[skip ci]` na mensagem do commit enquanto o workflow
  não ignorar `docs/**` (isso muda na etapa 3 do plano de separação).
- **O bot só pode ser testado em produção,** porque precisa do token. Quando a etapa mexer no bot, diga ao
  Leonardo o que testar no Discord depois do deploy.

## Onde o backend vai chegar

```
core/
  analyzer/  PartyHuntParser (interface) e TibiaPartyHuntParser (o antigo util/PartyAnalyzerConverter),
             PartyHuntSplitter (antes em util/), SessionDuration, TransferPlanner,
             InvalidAnalyzerInputException (a antiga exception/ConverterException)
  dto/       records da análise, como hoje
  model/     entidades sem dependência de DTO
  dao/       DAOs, Specifications e SessionSearchCriteria (o antigo api/dto/SessionFilter)
  service/   PartySessionFactory, SessionAnalysisService, SessionCommandService, SessionQueryService,
             UserService e as exceções de sessão
api/         controllers finos, DTOs, CurrentUser e o resolver dele, ApiExceptionHandler
discord/     DiscordInteractionListener, DiscordInteractionRegistry, handlers, PartyHuntModal, embed
```

As dependências seguem uma direção só: `api` e `discord` dependem de `core`; `core` não depende de nenhum
dos dois; e `api` e `discord` não dependem um do outro. O teste de arquitetura da B3 garante isso.

## Etapas do backend

### B1. Rede de segurança: testes de contrato e de caracterização

Prepara as outras etapas. Só acrescenta testes; nenhum código de produção muda.

- **Contrato do JSON.** Confira o conjunto exato de campos de cada resposta: leia o corpo com `ObjectMapper`
  como `Map` e compare as chaves. Pode ser em `SessionControllerTest` e `AuthControllerTest` ou numa classe
  nova `@WebMvcTest`, como as que já existem. Respostas a cobrir:
  - o detalhe da sessão (`GET /api/sessions/{id}`, `POST /api/sessions/preview` e `POST /api/sessions`);
  - o item da lista e o envelope da paginação (`GET /api/sessions`);
  - o membro e a transferência;
  - `GET /api/auth/me`;
  - o corpo do erro 400 (`timestamp`, `status`, `error` e `message`).

  Os campos esperados estão nos modelos do front (`session.model.ts` e `user.model.ts`). Exceção:
  `memberCount` existe só no item da lista, e o front declara isso errado (ver a F3 do plano do front).
- **Parser.** Em `src/test/resources/`, acrescente mais dois exemplos reais, no formato do `analyzer.txt`:
  um com três ou mais jogadores e o `(Leader)` num jogador do meio, outro com balance negativo. Teste os
  dois em `PartyAnalyzerConverterTest`.
- **Divisão.** Em `PartyHuntSplitterTest`, acrescente dois casos:
  - um jogador só: nenhuma transferência;
  - quatro jogadores: depois das transferências, todos ficam com `individualBalance`, a menos do resto da
    divisão inteira.
- Não escreva testes que travem os bugs (entrada sem jogador, data inválida): esse comportamento muda na B5.
- **Pronto quando:** os testes novos passam com o código de hoje.

### B2. Injeção por construtor e logger

Princípio: D.

- Troque todo `@Autowired` em atributo por atributo `private final` mais construtor. O construtor pode ser
  explícito ou gerado pelo `@RequiredArgsConstructor` do Lombok, que o projeto já usa. No `BotInitializer`,
  receba o token pelo construtor também (`@Value` no parâmetro).
- No `SlashCommandHandler`, troque `System.out.println(e.getMessage())` (linha 36) por um `Logger` do SLF4J
  que registre a exceção inteira: `log.error("...", e)`.
- Os testes com `@InjectMocks` continuam funcionando com construtor.
- **Pronto quando:** `grep -rn "@Autowired" src/main/java` não acha nada e os testes passam.

### B3. `core` sem depender de `api`, com teste de arquitetura

Princípio: D.

- Mova `api/dto/SessionFilter.java` para `core/dao/SessionSearchCriteria.java`, com os mesmos campos. O
  `SessionController` monta o critério a partir dos `@RequestParam`, como hoje.
- Acrescente `com.tngtech.archunit:archunit-junit5` com escopo `test` (a última versão em 2026-10-29 era a
  1.5.1). Crie `src/test/java/br/com/baba/tibia_analyzer/ArchitectureTest.java` com três regras:
  1. classes de `..core..` não dependem de `..api..` nem de `..discord..`;
  2. `..api..` não depende de `..discord..`, e `..discord..` não depende de `..api..`;
  3. nenhum atributo tem `@Autowired`, o que trava a B2.
- **Pronto quando:** os testes passam, incluindo o `ArchitectureTest`. Para conferir que ele funciona,
  ponha de volta um `import` de `api` no `core`, veja o teste falhar e desfaça.

### B4. Entidades sem DTO e hora injetável

Princípios: S e D.

- Crie `core/service/PartySessionFactory` (`@Component`) com o que hoje está nos construtores de
  `PartySession`, `PartyMember` e `PartyTransfer` que recebem DTO. A data de processamento passa a vir de um
  `Clock` injetado. Crie o bean (`Clock.systemDefaultZone()`) numa classe `@Configuration` do `core`, por
  exemplo `core/config/ClockConfig.java`.
- Apague esses construtores das entidades. Dê a `PartySession` três métodos, que as próximas etapas vão
  usar: `rename(String)`, `changeComment(String)` e `isOwnedBy(String discordId)`.
- Nas quatro entidades (inclusive `User`), troque `@Data` por `@Getter`, `@Setter` e `@NoArgsConstructor`.
  O `equals`, o `hashCode` e o `toString` que o `@Data` gera usam campos mutáveis e coleções lazy, e isso
  causa bugs conhecidos com JPA. Em 2026-10-29, nenhum teste comparava entidades com `assertEquals`, mas
  confira de novo.
- Ajuste quem constrói sessões:
  - no `PartyHuntService`, `processAndPersist` e `previewSession`;
  - o `PartySessionTest` vira `PartySessionFactoryTest`, com `Clock.fixed`;
  - o `buildSession()` do `SessionControllerTest` passa a usar a fábrica ou setters.
- **Pronto quando:** nenhum arquivo de `core/model/` importa de `core/dto/` e os testes passam.

### B5. Contrato do parser e divisão em partes testáveis (corrige os erros 500)

Princípios: L, D e S. **Muda o comportamento de propósito:** entrada inválida passa a dar 400 na API e uma
mensagem de erro no Discord.

- Crie `core/analyzer/` e leve para lá o que hoje está em `core/util/`:
  - **`PartyHuntParser`** (interface), com `PartyHuntAnalyzerDTO parse(String rawInput)`. O Javadoc fixa o
    contrato: se o texto não for um Party Hunt Analyzer válido, inclusive se não tiver nenhum jogador, o
    método lança `InvalidAnalyzerInputException`, com uma mensagem que dá para mostrar ao usuário, e nunca
    outra exceção. É a única interface nova do plano, por dois motivos: o formato do texto é do jogo e
    pode mudar com o cliente do Tibia, e o resto do sistema não precisa saber como o texto é lido.
  - **`TibiaPartyHuntParser implements PartyHuntParser`** é o `PartyAnalyzerConverter` renomeado, com três
    mudanças:
    - os `Pattern` viram constantes (hoje o `extractValue` compila a expressão a cada chamada);
    - `NumberFormatException` e `DateTimeParseException` viram a exceção do contrato;
    - análise sem nenhum jogador é recusada.
  - **`InvalidAnalyzerInputException`** é a `ConverterException` renomeada (hoje ninguém a lança).
  - **`PartyHuntSplitter`** vai para `core/analyzer/`, sem virar interface: é lógica pura, com um algoritmo
    só. Extraia dele:
    - `SessionDuration`, um value object com `static Optional<SessionDuration> parse(String)` e
      `double hours()`, usado no loot por hora;
    - `TransferPlanner`, com o algoritmo das transferências (linhas 67-97).

    Proteja a divisão por zero, mesmo com o parser garantindo pelo menos um jogador.
- O `PartyHuntService` passa a depender de `PartyHuntParser`, não da implementação.
- No `ApiExceptionHandler`, mapeie `InvalidAnalyzerInputException` para 400, no formato de corpo de hoje.
  Deixe as outras exceções como estão; a B7 revisa.
- No bot:
  - no `PartyHuntHandler`, capture `InvalidAnalyzerInputException` e responda pelo `event.getHook()` com
    `"Error processing your request!\n"` mais a mensagem. Tem que ser pelo hook porque o `deferReply()` já
    foi chamado;
  - no listener, troque o `catch (ConverterException)` por um `catch` que registre o erro no log e
    responda ao usuário: pelo hook se a interação já foi reconhecida (`event.isAcknowledged()`), senão com
    `event.reply(...).setEphemeral(true)`.
- Testes:
  - o parser lança a exceção do contrato para texto qualquer, data inválida e cabeçalho sem jogadores;
  - testes de `SessionDuration` e de `TransferPlanner`;
  - um teste `@WebMvcTest` em que o serviço lança `InvalidAnalyzerInputException` e a API devolve 400 com a
    mensagem.
- **Pronto quando:** os testes passam e, nos três casos de entrada inválida acima, o parser de verdade lança
  `InvalidAnalyzerInputException`. Dá para conferir com `jshell --class-path target/classes`. Depois do
  deploy, o Leonardo testa no Discord: `/loot` com texto inválido mostra a mensagem de erro, sem ficar
  "pensando...".

### B6. Serviços de sessão separados por responsabilidade, com a regra do dono no core

Princípios: S e I.

- Troque o `PartyHuntService` por três serviços em `core/service/`:
  - **`SessionAnalysisService`**: `analyze(String input)` devolve o record
    `SessionAnalysis(PartyHuntAnalyzerDTO parsed, SessionResultDTO result)`. Roda o parser e a divisão e não
    salva nada.
  - **`SessionCommandService`** (`@Transactional`):
    - `create(input, name, comment, ownerDiscordId)` devolve
      `CreatedSession(PartySession session, SessionResultDTO result)`, que substitui o `ProcessedSession`;
    - `updateMetadata(id, name, comment, requesterDiscordId)` devolve a `PartySession`;
    - `delete(id, requesterDiscordId)`.

    Sessão inexistente lança `SessionNotFoundException`, e pedido de quem não é o dono lança
    `NotSessionOwnerException`. A checagem usa o `PartySession.isOwnedBy` da B4 e sai do controller.
  - **`SessionQueryService`** (`@Transactional(readOnly = true)`): `findById(id)` devolve
    `Optional<PartySession>`, e `search(SessionSearchCriteria, Pageable)` devolve `Page<PartySession>`.
  - O preview usa `SessionAnalysisService` e `PartySessionFactory` para montar uma sessão que não é salva,
    como hoje.
  - Apague as sobrecargas `processSession`, que só os testes usavam.
- O `SessionController` fica fino: chama os serviços e não decide mais 403 nem 404. O `ApiExceptionHandler`
  mapeia `SessionNotFoundException` para 404 e `NotSessionOwnerException` para 403, **com corpo vazio, como
  hoje**. Mantenha a ordem atual das checagens: sessão inexistente dá 404 mesmo para quem não é o dono.
- O `PartyHuntHandler` (bot) passa a depender só de `SessionCommandService` e `UserService`. Opcional: com
  a análise separada, ele pode analisar o texto antes do `deferReply()` e mostrar o erro de entrada numa
  mensagem efêmera, que só quem enviou vê. Analisar duas vezes não pesa: o parser leva microssegundos.
- Testes:
  - o `PartyHuntServiceTest` vira um teste por serviço. O `SessionCommandServiceTest` cobre os casos de
    dono, de quem não é dono e de sessão inexistente;
  - no `SessionControllerTest`, mude só os mocks. As expectativas de status dos 21 testes atuais ficam
    iguais: são elas que provam que nada mudou para o front.
- **Pronto quando:** os testes passam e o `SessionController` não tem mais `isOwner` nem
  `ResponseEntity.status(403)`.

### B7. Camada web: usuário atual, DTOs de entrada e configuração

Princípios: D, I, O e L. **Muda o comportamento em dois casos raros:** uma falha ao salvar o usuário no
login passa a levar para `/?login=failed`, e um `IllegalArgumentException` vindo de outro lugar passa de 400
para 500, que é o certo para um bug.

- **Usuário atual.** Crie `api/auth/CurrentUser`, um record com `discordId`, e um
  `HandlerMethodArgumentResolver` registrado no `WebConfig`. O resolver lê o `OAuth2User` autenticado e
  devolve `null` para quem não está logado. Os controllers recebem `CurrentUser` como parâmetro. Só o
  resolver e o `DiscordOAuth2UserService` sabem que o id do Discord está no atributo `"id"`. Acrescente ao
  `ArchitectureTest`: classes com `@RestController` não dependem de `OAuth2User`. Os testes que usam
  `oauth2Login().attributes(...)` continuam valendo.
- **DTOs de entrada.** Tire `ownerDiscordId` do `CreateSessionRequest` e crie
  `PreviewSessionRequest(String input)` para o `/preview`. O Jackson do Spring Boot ignora campos
  desconhecidos, então quem ainda mandar `ownerDiscordId` continua funcionando. O teste
  `shouldCreateSessionUsingAuthenticatedDiscordId` já manda esse campo e tem que continuar passando.
- **Erros.** Tire `NumberFormatException` e `IllegalArgumentException` do `ApiExceptionHandler`. Depois da
  B5, o parser não lança mais nenhuma das duas, e tratar como erro do usuário um
  `IllegalArgumentException` de qualquer origem esconde bug. Ajuste o `shouldReturn400WhenParserFails` para
  lançar `InvalidAnalyzerInputException`.
- **CORS.** As origens passam a vir da propriedade `analyzer.cors.allowed-origins`, uma lista separada por
  vírgula. No `application.properties`:
  `analyzer.cors.allowed-origins=${ANALYZER_CORS_ALLOWED_ORIGINS:http://localhost:4200,https://huntanalyzer.lbaba.com.br}`.
  Os dois testes de CORS continuam passando sem mudança.
- **Login (L).** No `DiscordOAuth2UserService`, troque a herança por composição: a classe passa a
  implementar `OAuth2UserService<OAuth2UserRequest, OAuth2User>` e usa um `DefaultOAuth2UserService` como
  delegate. Se salvar o usuário falhar, lance `OAuth2AuthenticationException`, com a causa, para o Spring
  Security mandar o usuário para `/?login=failed`. Escreva um teste unitário com o delegate mockado.
- **Pronto quando:** os testes passam, incluindo as regras novas do `ArchitectureTest`. Depois do deploy, o
  Leonardo testa: entrar e sair, editar e apagar uma hunt dele.

### B8. Bot aberto para comandos novos

Princípios: O, D e S. O comportamento do bot não muda.

- `CommandHandler` ganha `String name()` e `SlashCommandData definition()`; `ModalHandler` ganha
  `String modalId()`.
- Crie `discord/DiscordInteractionRegistry` (`@Component`). Ele recebe `List<CommandHandler>` e
  `List<ModalHandler>` pelo construtor, monta os mapas por nome e falha ao subir se houver nome repetido.
  Expõe `command(name)`, `modal(id)` e `definitions()`.
- Apague `CommandEnum`, `ModalEnum`, `CommandHandlerFactory` e `ModalHandlerFactory`. A partir daqui, um
  comando novo é só uma classe `@Component` nova.
- Crie `PartyHuntModal`, que monta o modal com ou sem os campos de nome e detalhes. Ele acaba com a
  duplicação entre `LootCommandHandler` e `LootDetailsCommandHandler` (linhas 16-20 de cada) e guarda os
  ids do modal e dos campos. O `InputEnum` pode sumir.
- Renomeie `listener/SlashCommandHandler` para `DiscordInteractionListener`, porque ele trata comandos e
  modais, e faça-o usar o registro.
- O `BotInitializer` registra `registry.definitions()` e só sobe com `discord.bot.enabled=true`:
  `@ConditionalOnProperty(name = "discord.bot.enabled", havingValue = "true", matchIfMissing = true)`.
  Assim a API pode rodar localmente sem o bot.
- **Tem que ficar igual:**
  - nomes e descrições dos comandos (`loot` e `loot-details`);
  - id e título do modal (`partyHunt` e `Party Hunt Session`);
  - ids dos campos (`sessionInput`, `nameInput` e `commentInput`);
  - tamanhos e obrigatoriedade dos campos (texto da sessão de 150 a 4000 caracteres, obrigatório; nome até
    100 e detalhes até 1000, opcionais);
  - a mensagem de comando desconhecido.
- Testes:
  - `DiscordInteractionRegistryTest`: acha os handlers; `definitions()` tem os dois comandos com as
    descrições de hoje; nome repetido falha;
  - `PartyHuntModalTest`: ids e tamanhos;
  - um teste do listener com eventos mockados: comando desconhecido recebe a resposta efêmera.
- **Pronto quando:** os testes passam e
  `grep -rn "ApplicationContext" src/main/java/br/com/baba/tibia_analyzer/discord` não acha nada. Depois do
  deploy, o Leonardo testa no Discord: `/loot` e `/loot-details` funcionam como antes.

## Fora do escopo (anotado para depois)

- Fazer o `TibiaAnalyzerApplicationTests` rodar no CI com Postgres, por um service container do GitHub
  Actions ou Testcontainers (na VPS não há Docker). Depois da B8, dá para subir o contexto com
  `discord.bot.enabled=false`.
- `PartySession.processDate` é `java.util.Date` e `sessionDuration` é texto. Mudar exige migration e mexe
  no JSON.
- O `@MockBean` está obsoleto no Spring Boot 3.4 (o substituto é o `@MockitoBean`). Trocar junto com uma
  atualização de versão.
- Validar a entrada com Bean Validation (`@NotBlank` no `input`) exige a dependência
  `spring-boot-starter-validation`.
- Divisão do loot com outras regras (excluir um membro, usar pesos): quando aparecer, vira uma estratégia no
  `PartyHuntSplitter`. Antes disso, seria uma abstração sem uso.
