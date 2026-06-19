# Documentação Técnica — Microsserviço `payments`

> Documento de referência para estudo, manutenção e onboarding.
> Baseado no código efetivamente implementado em junho de 2026 — inclui processamento de pagamento (simulação determinística) e integração com Kafka.

---

## Índice

1. [Visão Geral](#1-visão-geral)
2. [Arquitetura](#2-arquitetura)
3. [Explicação Arquivo por Arquivo](#3-explicação-arquivo-por-arquivo)
4. [Explicação Método por Método](#4-explicação-método-por-método)
5. [Segurança e JWT](#5-segurança-e-jwt)
6. [Endpoints](#6-endpoints)
7. [Banco de Dados](#7-banco-de-dados)
8. [Regras de Negócio](#8-regras-de-negócio)
9. [Fluxos Reais](#9-fluxos-reais)
10. [Estado Atual do Microsserviço](#10-estado-atual-do-microsserviço)
11. [Próximos Passos](#11-próximos-passos)
12. [Guia para Evolução](#12-guia-para-evolução)
13. [Resumo Final](#13-resumo-final)

---

## 1. Visão Geral

### Responsabilidade

O microsserviço `payments` é responsável por **criar, consultar, listar e cancelar pagamentos** dentro da plataforma PB. Ele persiste as transações financeiras no banco de dados e garante que cada operação só possa ser realizada pelo próprio usuário dono do pagamento.

### Problema que resolve

Em uma arquitetura de microsserviços, nenhum serviço deve ser responsável por tudo. O `payments` isola completamente o domínio financeiro: outros serviços (como `store`) não precisam saber como um pagamento é criado ou cancelado — apenas consomem eventos futuros de confirmação. Isso evita acoplamento e permite que o `payments` evolua de forma independente.

### Papel dentro da arquitetura

O `payments` é um **Resource Server OAuth2**: ele não emite tokens JWT, apenas os valida. Toda requisição que chega precisa de um token emitido pelo microsserviço `users`. O `payments` verifica a assinatura do token, extrai a identidade do usuário e usa essa identidade para todas as operações.

```
Frontend / Postman
      │
      ▼
Kong API Gateway (porta 8000)
      │  roteia /payments → payments:8085
      ▼
payments (porta 8085)
      │  valida JWT → consulta PostgreSQL
      ▼
Banco de dados payments (PostgreSQL)
```

### Relação com o microsserviço `users`

| Aspecto | `users` | `payments` |
|---|---|---|
| Papel no JWT | Emissor (assina com chave privada) | Validador (verifica com chave pública) |
| Chave RSA utilizada | `app.key` (privada) + `app.key.pub` (pública) | `app.key.pub` via módulo `shared` |
| Porta | 8082 | 8085 |
| Banco de dados | `users` | `payments` |

O `users` emite um token JWT quando o usuário faz login. Esse token contém o UUID do usuário no campo `sub` (subject). O `payments` lê esse `sub` para saber quem está fazendo a requisição — sem precisar consultar o banco do `users`.

### Fluxo de autenticação entre `users` e `payments`

```
1. Usuário faz POST /auth/login no users
2. users valida credenciais e emite JWT assinado com chave privada RSA
3. JWT contém: sub = UUID do usuário, exp = +5 minutos, issuer = "users-service"
4. Frontend armazena o token e o envia em todas as requisições ao payments:
   Header: Authorization: Bearer <token>
5. payments intercepta a requisição via Spring Security (SecurityConfig do shared)
6. JwtDecoder verifica a assinatura do token usando app.key.pub (do shared)
7. Se válido: extrai sub → UUID do usuário → processa a operação
8. Se inválido ou expirado: retorna 401 automaticamente
```

---

## 2. Arquitetura

### Módulo `shared`

O `payments` depende do módulo `shared` (`services/shared/`), que é um JAR Maven instalado localmente durante o build. O `shared` fornece os componentes de segurança reutilizados por todos os microserviços:

- **`SecurityConfig.java`** (`com.edu.infnet.pb.config`) — define o `SecurityFilterChain`, o `JwtDecoder` (verifica tokens) e o `JwtEncoder` (assina tokens, usado apenas pelo `users`)
- **`app.key.pub`** (`src/main/resources/`) — chave pública RSA usada para verificar tokens JWT emitidos pelo `users`
- **`app.key`** (`src/main/resources/`) — chave privada RSA, necessária porque o `SecurityConfig` do `shared` declara um bean `JwtEncoder` compartilhado por todos os serviços, mesmo que o `payments` nunca o utilize

O `PaymentsApplication` usa `scanBasePackages = "com.edu.infnet.pb"`, que inclui o pacote do `shared`, fazendo o Spring carregar automaticamente o `SecurityConfig` de lá.

**Importante:** diferente de versões anteriores deste documento, o `SecurityConfig` do `shared` **não tem mais valor default** para `jwt.public.key` e `jwt.private.key`. O `payments/application.yaml` precisa declarar explicitamente os dois caminhos `classpath:`, mesmo não usando o `JwtEncoder`.

### Estrutura de pastas

```
services/payments/
├── src/
│   ├── main/
│   │   ├── java/com/edu/infnet/pb/payments/
│   │   │   ├── PaymentsApplication.java       ← ponto de entrada
│   │   │   ├── config/
│   │   │   │   ├── OpenAPIConfig.java         ← configuração do Swagger/OpenAPI
│   │   │   │   └── KafkaConfig.java           ← declara os tópicos Kafka (NewTopic beans)
│   │   │   ├── controller/
│   │   │   │   └── PaymentController.java     ← endpoints REST
│   │   │   ├── dto/
│   │   │   │   ├── PaymentRequest.java        ← dados de entrada da API
│   │   │   │   ├── PaymentResponse.java       ← dados de saída da API
│   │   │   │   └── PaymentEvent.java          ← payload publicado no Kafka
│   │   │   ├── entity/
│   │   │   │   └── Payment.java               ← entidade JPA (tabela payments)
│   │   │   ├── enums/
│   │   │   │   ├── PaymentMethod.java         ← métodos de pagamento disponíveis
│   │   │   │   └── PaymentStatus.java         ← estados possíveis de um pagamento
│   │   │   ├── exception/
│   │   │   │   └── GlobalExceptionHandler.java ← tratamento centralizado de erros
│   │   │   ├── producer/
│   │   │   │   └── PaymentProducer.java       ← publica eventos no Kafka
│   │   │   ├── repository/
│   │   │   │   └── PaymentRepository.java     ← acesso ao banco de dados
│   │   │   └── service/
│   │   │       └── PaymentService.java        ← regras de negócio + processamento + disparo de eventos
│   │   └── resources/
│   │       └── application.yaml              ← configurações da aplicação
│   └── test/
│       └── java/com/edu/infnet/pb/payments/
│           └── PaymentsApplicationTests.java  ← teste de contexto
├── Dockerfile                                 ← build multi-stage
├── pom.xml                                    ← dependências Maven
└── mvnw                                       ← Maven wrapper
```

> `SecurityConfig.java` e `app.key.pub` não existem neste módulo — são fornecidos pelo `shared`.

### Responsabilidade de cada camada

| Camada | Pacote | Responsabilidade |
|---|---|---|
| **Controller** | `controller/` | Receber requisições HTTP, extrair dados do JWT, delegar ao service, retornar resposta |
| **Service** | `service/` | Implementar regras de negócio, validar ownership, lançar exceções de negócio |
| **Repository** | `repository/` | Executar queries no banco de dados via Spring Data JPA |
| **Entity** | `entity/` | Representar a tabela `payments` no banco — nunca exposta diretamente na API |
| **DTO** | `dto/` | Contratos de entrada (Request) e saída (Response) da API |
| **Enums** | `enums/` | Valores válidos para status e método de pagamento |
| **Config** | `config/` | Configuração do Swagger/OpenAPI (segurança vem do `shared`) |
| **Exception** | `exception/` | Interceptar e formatar erros de forma padronizada |

### Fluxo completo de uma requisição

```
Cliente (Postman / Frontend)
  │
  │  Authorization: Bearer <jwt>
  │  POST /payments  { amount: 100.00, method: "PIX" }
  ▼
Spring Security Filter Chain  (SecurityConfig do shared)
  │  1. Intercepta a requisição
  │  2. Extrai o token do header Authorization
  │  3. JwtDecoder verifica assinatura com app.key.pub (do shared)
  │  4. Se inválido → 401 (nunca chega ao controller)
  │  5. Se válido → injeta o Jwt no SecurityContext
  ▼
PaymentController.create()
  │  1. Recebe @RequestBody PaymentRequest (valida com @Valid)
  │  2. Extrai userId: UUID.fromString(jwt.getSubject())
  │  3. Chama service.create(request, userId)
  ▼
PaymentService.create()
  │  1. Constrói entidade Payment com status PENDING
  │  2. Chama repository.save(payment)
  │  3. Converte entidade para PaymentResponse via toResponse()
  │  4. Retorna PaymentResponse
  ▼
PaymentRepository.save()
  │  1. Persiste no PostgreSQL (tabela payments)
  │  2. Hibernate gera UUID e timestamps automaticamente
  │  3. Retorna entidade com id e timestamps preenchidos
  ▼
PaymentController
  │  Retorna PaymentResponse serializado como JSON
  │  HTTP 201 Created
  ▼
Cliente recebe resposta JSON
```

---

## 3. Explicação Arquivo por Arquivo

### `PaymentsApplication.java`

**Localização:** `com/edu/infnet/pb/payments/PaymentsApplication.java`

**Responsabilidade:** Ponto de entrada da aplicação Spring Boot. Contém o método `main` que inicializa o contexto do Spring, sobe o servidor Tomcat embarcado e registra o serviço no Eureka.

**Por que existe:** Todo projeto Spring Boot precisa de exatamente uma classe anotada com `@SpringBootApplication`. A anotação usa `scanBasePackages = "com.edu.infnet.pb"` — pacote raiz que inclui tanto o pacote `payments` quanto o pacote `shared`. Isso faz o Spring encontrar automaticamente o `SecurityConfig` do módulo `shared`, além de todos os `@Service`, `@Repository`, `@RestController` e `@Configuration` do próprio `payments`.

**Quem utiliza:** É o ponto de entrada do Docker — o `Dockerfile` executa esta classe via `java -jar`.

**Nenhum outro arquivo depende diretamente dela.**

---

### `config/OpenAPIConfig.java`

**Localização:** `com/edu/infnet/pb/payments/config/OpenAPIConfig.java`

**Responsabilidade:** Configura os metadados da documentação OpenAPI (Swagger) do serviço.

**Por que existe:** Personaliza o título, descrição, versão e contato exibidos no Swagger UI. Sem ela, o Swagger UI mostraria informações genéricas geradas automaticamente pelo Springdoc.

**Quem utiliza:** O Springdoc lê esta configuração ao gerar a spec em `/v3/api-docs`. O Swagger UI centralizado (`localhost:8089`) consome essa spec via Kong (`/docs/payments/v3/api-docs`).

**Nenhum outro código Java depende desta classe.**

---

### `SecurityConfig` (módulo `shared`)

**Localização:** `services/shared/src/main/java/com/edu/infnet/pb/config/SecurityConfig.java`

**Responsabilidade:** Define as regras de segurança HTTP e registra o componente que valida tokens JWT. Este arquivo não existe dentro do `payments` — é fornecido pelo módulo `shared` e carregado automaticamente graças ao `scanBasePackages = "com.edu.infnet.pb"` no `PaymentsApplication`.

**Regras configuradas:**
- `/actuator/**` → livre, sem autenticação (para Docker healthcheck e Prometheus)
- `POST /auth/register`, `POST /auth/login` → livres (rotas do `users`, ignoradas pelo `payments`)
- Qualquer outra rota → exige JWT válido no header `Authorization`, via `.anyRequest().authenticated()`
- CSRF desabilitado (não aplicável em APIs REST stateless)
- Sessão: STATELESS (nunca cria HttpSession)
- Modo OAuth2 Resource Server com JWT

**Dependências:**
- `app.key.pub` — lido via `@Value("${jwt.public.key}")`, convertido para `RSAPublicKey`. **Sem valor default.**
- `app.key` — lido via `@Value("${jwt.private.key}")`, convertido para `RSAPrivateKey`. Necessário porque o `SecurityConfig` também declara o bean `JwtEncoder` (usado apenas pelo `users`, mas presente em todos os serviços que importam o `shared`).
- O `payments/application.yaml` **precisa declarar explicitamente** as duas propriedades `jwt.public.key` e `jwt.private.key` apontando para `classpath:`. Sem isso, o contexto Spring falha na inicialização com `PlaceholderResolutionException`.

> **Atenção — regra crítica de manutenção:** se `.anyRequest().authenticated()` for removido do `SecurityConfig` do `shared`, todo JWT válido passa a receber `403 Forbidden` em qualquer rota não listada explicitamente — mesmo estando corretamente autenticado. Esse foi um bug real encontrado durante a validação da integração com Kafka: a ausência dessa regra fazia requisições autenticadas falharem silenciosamente com 403, sem nenhuma mensagem indicando a causa.

---

### `app.key.pub` (módulo `shared`)

**Localização:** `services/shared/src/main/resources/app.key.pub`

**Responsabilidade:** Armazenar a chave pública RSA usada para verificar a assinatura dos tokens JWT.

**Por que existe:** O `users` assina os tokens com sua chave privada (`app.key`). Para verificar que um token é legítimo — que foi realmente emitido pelo `users` e não foi adulterado — o `payments` precisa da chave pública correspondente. É matematicamente impossível verificar sem ela.

**Como é carregado:** O arquivo está em `src/main/resources/` do módulo `shared`. Quando o `shared` é compilado como JAR, o arquivo entra no classpath. O `payments/application.yaml` declara `jwt.public.key: classpath:app.key.pub`, e o `SecurityConfig` do `shared` lê essa propriedade via `@Value("${jwt.public.key}")`, convertendo para `RSAPublicKey`.

**Cuidado:** Este arquivo deve ser **idêntico** ao `app.key.pub` do microsserviço `users`. Se o `users` trocar seu par de chaves RSA, o `app.key.pub` do `shared` deve ser atualizado junto — caso contrário todos os tokens emitidos pelo `users` passarão a ser rejeitados pelo `payments` (e por qualquer outro serviço que use o `shared`).

---

### `app.key` (módulo `shared`)

**Localização:** `services/shared/src/main/resources/app.key`

**Responsabilidade:** Armazenar a chave privada RSA usada para assinar tokens JWT.

**Por que o `payments` precisa dela:** O `payments` nunca assina tokens — apenas o `users` faz login e emite JWT. Porém, o `SecurityConfig` do `shared` é compartilhado por todos os serviços e declara um bean `JwtEncoder`, que exige a chave privada para ser construído. Como o `payments` carrega esse `SecurityConfig` via `scanBasePackages`, ele precisa fornecer a propriedade `jwt.private.key`, mesmo nunca chamando o `JwtEncoder`.

**Como é carregado:** Igual ao `app.key.pub` — via `jwt.private.key: classpath:app.key` no `application.yaml`, convertido para `RSAPrivateKey` pelo Spring.

---

### `controller/PaymentController.java`

**Localização:** `com/edu/infnet/pb/payments/controller/PaymentController.java`

**Responsabilidade:** Expor os endpoints HTTP REST do serviço de pagamentos. Receber requisições, extrair o `userId` do JWT e delegar toda a lógica ao `PaymentService`.

**Por que existe:** Separação de responsabilidades — o controller não tem lógica de negócio. Ele é a fronteira entre o protocolo HTTP e o domínio da aplicação.

**Quem utiliza:** Clientes externos (Postman, frontend, Kong).

**Depende de:** `PaymentService`, `PaymentRequest`, `PaymentResponse`.

**Como participa do fluxo:** É o primeiro ponto de código Java que uma requisição atinge após passar pelo filtro de segurança. Extrai o `userId` do token via `jwt.getSubject()` e passa para o service — garantindo que a identidade do usuário nunca vem do body da requisição.

---

### `dto/PaymentRequest.java`

**Localização:** `com/edu/infnet/pb/payments/dto/PaymentRequest.java`

**Responsabilidade:** Representar os dados que o cliente envia no body ao criar um pagamento.

**Por que existe:** Isola o contrato da API da entidade interna. Se a entidade `Payment` mudar internamente, o contrato com o cliente permanece estável.

**Quem utiliza:** `PaymentController` (recebe do cliente via `@RequestBody`) e `PaymentService` (recebe como parâmetro).

**Campos:**

| Campo | Tipo | Validação |
|---|---|---|
| `amount` | `BigDecimal` | `@NotNull`, `@DecimalMin("0.01")` |
| `method` | `PaymentMethod` | `@NotNull` |

**Nota importante:** `userId` e `status` não estão neste DTO intencionalmente. O `userId` vem do JWT. O `status` inicial é sempre `PENDING` — definido pelo service, não pelo cliente.

---

### `dto/PaymentResponse.java`

**Localização:** `com/edu/infnet/pb/payments/dto/PaymentResponse.java`

**Responsabilidade:** Representar os dados que a API retorna ao cliente após qualquer operação bem-sucedida.

**Por que existe:** Nunca se expõe uma entidade JPA diretamente na API — ela pode conter dados internos, relacionamentos lazy que causam erros de serialização, ou campos que não devem ser públicos. O DTO de resposta é o contrato explícito do que o cliente pode ver.

**Quem utiliza:** Retornado por todos os métodos do `PaymentController`. Gerado pelo método `toResponse()` do `PaymentService`.

**Campos:**

| Campo | Tipo | Descrição |
|---|---|---|
| `id` | `UUID` | Identificador único do pagamento |
| `userId` | `UUID` | UUID do usuário dono do pagamento |
| `amount` | `BigDecimal` | Valor em BRL |
| `method` | `PaymentMethod` | Método de pagamento utilizado |
| `status` | `PaymentStatus` | Estado atual do pagamento |
| `createdAt` | `LocalDateTime` | Data e hora de criação |
| `updatedAt` | `LocalDateTime` | Data e hora da última atualização |

---

### `dto/PaymentEvent.java`

**Localização:** `com/edu/infnet/pb/payments/dto/PaymentEvent.java`

**Responsabilidade:** Representar o payload publicado no Kafka a cada mudança de estado de um pagamento.

**Por que existe:** É o contrato dos eventos do `payments` para o restante do ecossistema. Diferente do `PaymentResponse` (contrato HTTP), este é o contrato assíncrono — consumidores Kafka de outros serviços dependem desta estrutura.

**Quem utiliza:** `PaymentService` (constrói via `toEvent()`), `PaymentProducer` (recebe pronto e publica).

**Campos:**

| Campo | Tipo | Descrição |
|---|---|---|
| `paymentId` | `UUID` | Identifica o pagamento que originou o evento |
| `userId` | `UUID` | Dono do pagamento |
| `amount` | `BigDecimal` | Valor do pagamento |
| `method` | `PaymentMethod` | Método utilizado |
| `status` | `PaymentStatus` | Estado do pagamento **no momento do evento** — é o dado central da mensagem |
| `timestamp` | `LocalDateTime` | Quando o evento ocorreu (usa o `updatedAt` do pagamento no instante da publicação) |

**Nota sobre serialização:** o `JsonSerializer` do Kafka serializa `LocalDateTime` como array (`[2026,6,16,23,30,26,267482000]`), não como string ISO-8601. Funcional, mas não é o formato mais amigável para consumidores — ajuste pendente antes de outros serviços passarem a consumir estes tópicos.

---

### `config/KafkaConfig.java`

**Localização:** `com/edu/infnet/pb/payments/config/KafkaConfig.java`

**Responsabilidade:** Declarar os tópicos Kafka usados pelo `payments` como beans `NewTopic`.

**Por que existe:** O Kafka do ambiente roda com `KAFKA_AUTO_CREATE_TOPICS_ENABLE: "false"` (configurado no `docker-compose.yaml`). Sem essa classe, qualquer tentativa de publicar em um tópico inexistente falha. O Spring Kafka detecta automaticamente beans `NewTopic` no contexto e cria os tópicos correspondentes no broker durante o startup.

**Tópicos declarados:**

| Tópico | Partições | Replicação | Quando é usado |
|---|---|---|---|
| `payment.initiated` | 1 | 1 | Publicado logo após a criação do pagamento (status `PENDING`) |
| `payment.approved` | 1 | 1 | Publicado quando o processamento aprova o pagamento |
| `payment.failed` | 1 | 1 | Publicado quando o processamento reprova o pagamento |

**Quem utiliza:** Nenhum código chama esta classe diretamente — o Spring Kafka a descobre via `@Configuration` e processa os beans `NewTopic` automaticamente no startup.

---

### `producer/PaymentProducer.java`

**Localização:** `com/edu/infnet/pb/payments/producer/PaymentProducer.java`

**Responsabilidade:** Publicar um `PaymentEvent` no tópico Kafka correto, decidido a partir do `status` do evento.

**Por que existe:** Centraliza a lógica de "qual tópico usar" em um único lugar. O `PaymentService` não decide nomes de tópico — apenas informa o que aconteceu, e o producer decide onde publicar.

**Quem utiliza:** `PaymentService`, injetado via construtor (`@RequiredArgsConstructor`).

**Depende de:** `KafkaTemplate<String, PaymentEvent>` (auto-configurado pelo Spring Boot a partir das propriedades `spring.kafka.*` do `application.yaml` — nenhuma configuração manual de `KafkaTemplate` é necessária).

**Lógica de roteamento:**
```text
PENDING  → payment.initiated
APPROVED → payment.approved
FAILED   → payment.failed
CANCELLED (ou qualquer outro) → nenhum tópico, apenas log de aviso
```

**Chave da mensagem:** o `paymentId` (como String). Isso garante que todos os eventos de um mesmo pagamento caiam na mesma partição, preservando a ordem entre eles.

**Tratamento de erro:** `kafkaTemplate.send()` é assíncrono e retorna um `CompletableFuture`. O producer usa `.whenComplete()` para logar sucesso ou falha sem bloquear o fluxo principal — uma falha de publicação no Kafka **não impede** a resposta HTTP ao cliente nem derruba o serviço.

---

### `entity/Payment.java`

**Localização:** `com/edu/infnet/pb/payments/entity/Payment.java`

**Responsabilidade:** Representar a tabela `payments` no banco de dados PostgreSQL. É o modelo interno da aplicação — nunca exposto diretamente na API.

**Por que existe:** O JPA (Hibernate) usa esta classe para gerar e executar as queries SQL. Cada instância desta classe corresponde a uma linha na tabela `payments`.

**Quem utiliza:** `PaymentRepository` (persiste e consulta), `PaymentService` (cria e manipula).

**Depende de:** `PaymentMethod`, `PaymentStatus`.

**Como participa do fluxo:** O service constrói instâncias desta classe usando o padrão Builder (`Payment.builder()...build()`) e as passa para o repository. Nunca sai do service sem ser convertida para `PaymentResponse`.

---

### `enums/PaymentStatus.java`

**Localização:** `com/edu/infnet/pb/payments/enums/PaymentStatus.java`

**Responsabilidade:** Definir os estados válidos de um pagamento.

**Por que existe:** Usar um enum garante que o banco nunca terá um status inválido como "CANCELADO" (com acento) ou "approved" (minúsculo). O Hibernate persiste o nome do enum como String (`@Enumerated(EnumType.STRING)`).

**Valores:**

| Valor | Significado |
|---|---|
| `PENDING` | Pagamento criado, aguardando processamento |
| `APPROVED` | Pagamento aprovado com sucesso |
| `FAILED` | Pagamento falhou no processamento |
| `CANCELLED` | Pagamento cancelado pelo usuário |

**Quem utiliza:** `Payment` (campo `status`), `PaymentService` (verifica e muda status), `PaymentResponse` (exposto na API).

---

### `enums/PaymentMethod.java`

**Localização:** `com/edu/infnet/pb/payments/enums/PaymentMethod.java`

**Responsabilidade:** Definir os métodos de pagamento aceitos pela plataforma.

**Por que existe:** Mesmo motivo do `PaymentStatus` — evita valores arbitrários no banco e na API. O cliente só pode enviar um dos valores definidos aqui; qualquer outro resulta em erro 400.

**Valores:**

| Valor | Significado |
|---|---|
| `PIX` | Pagamento via PIX |
| `CREDIT_CARD` | Cartão de crédito |
| `BOLETO` | Boleto bancário |

**Quem utiliza:** `Payment` (campo `method`), `PaymentRequest` (entrada da API), `PaymentResponse` (saída da API).

---

### `exception/GlobalExceptionHandler.java`

**Localização:** `com/edu/infnet/pb/payments/exception/GlobalExceptionHandler.java`

**Responsabilidade:** Interceptar exceções lançadas em qualquer controller e transformá-las em respostas JSON padronizadas.

**Por que existe:** Sem ele, erros retornam respostas inconsistentes — às vezes HTML, às vezes JSON sem estrutura definida. Com ele, toda resposta de erro segue o mesmo formato, independentemente da origem do erro.

**Quem utiliza:** Nenhum código chama esta classe diretamente. O Spring a descobre via `@RestControllerAdvice` e a registra automaticamente no pipeline de tratamento de erros.

**Formato de resposta de erro:**
```json
{
  "timestamp": "2026-06-09T21:00:00",
  "status": 404,
  "message": "Pagamento não encontrado"
}
```

**Exceções tratadas:**

| Exceção | Status | Origem |
|---|---|---|
| `ResponseStatusException` | Variável (404, 403, 422) | `PaymentService` |
| `MethodArgumentNotValidException` | 400 | Bean Validation no controller |
| `Exception` (genérica) | 500 | Qualquer erro inesperado |

---

### `repository/PaymentRepository.java`

**Localização:** `com/edu/infnet/pb/payments/repository/PaymentRepository.java`

**Responsabilidade:** Executar operações de leitura e escrita no banco de dados para a entidade `Payment`.

**Por que existe:** O Spring Data JPA gera automaticamente a implementação desta interface. Você declara o que quer consultar e o Spring escreve o SQL por você.

**Quem utiliza:** `PaymentService` — é o único componente que deve acessar o repository diretamente.

**Métodos disponíveis:**

| Método | Origem | SQL gerado |
|---|---|---|
| `save(Payment)` | Herdado de `JpaRepository` | `INSERT` ou `UPDATE` |
| `findById(UUID)` | Herdado de `JpaRepository` | `SELECT ... WHERE id = ?` |
| `findByUserId(UUID)` | Declarado na interface | `SELECT ... WHERE user_id = ?` |
| `findAll()` | Herdado de `JpaRepository` | `SELECT * FROM payments` |

---

### `service/PaymentService.java`

**Localização:** `com/edu/infnet/pb/payments/service/PaymentService.java`

**Responsabilidade:** Implementar todas as regras de negócio do domínio de pagamentos — incluindo a simulação de processamento e o disparo de eventos Kafka. É a camada central da aplicação.

**Por que existe:** Separa a lógica de negócio do protocolo HTTP (controller), do acesso a dados (repository) e da mensageria (producer). Se amanhã a API mudar de REST para gRPC, ou o Kafka for trocado por outro broker, o service não muda.

**Quem utiliza:** `PaymentController` — é o único componente que deve chamar o service diretamente.

**Depende de:** `PaymentRepository`, `PaymentProducer`, `Payment`, `PaymentRequest`, `PaymentResponse`, `PaymentEvent`, `PaymentStatus`.

---

### `resources/application.yaml`

**Localização:** `src/main/resources/application.yaml`

**Responsabilidade:** Centralizar todas as configurações da aplicação.

**Seções e seus papéis:**

```yaml
spring:
  application:
    name: payments           # nome usado no Eureka e nos logs

  datasource:
    url: jdbc:postgresql://pg:5432/payments  # banco exclusivo do payments
    username: admin
    password: admin
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: update       # Hibernate cria/atualiza tabelas automaticamente
    show-sql: true           # loga todas as queries SQL no console

  kafka:
    bootstrap-servers: kafka:29092   # endereço do broker na rede interna do Docker
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer

server:
  port: 8085                 # porta do serviço

jwt:
  public:
    key: classpath:app.key.pub   # obrigatório — sem default no SecurityConfig do shared
  private:
    key: classpath:app.key       # obrigatório — exigido pelo bean JwtEncoder do shared, não usado pelo payments

eureka:                      # registro e descoberta de serviços
  instance:
    instance-id: ${spring.application.name}:${random.value}
  client:
    service-url:
      defaultZone: http://eureka:8761/eureka/
    register-with-eureka: true
    fetch-registry: true

logging:
  level:
    "[com.edu.infnet.pb]": TRACE
  pattern:
    correlation: "[${spring.application.name:},%X{traceId:-},%X{spanId:-}] "

management:
  tracing:
    sampling:
      probability: 1.0       # 100% das requisições são rastreadas no Jaeger
  endpoints:
    web:
      exposure:
        include:
          - health
          - prometheus
          - info

otel:
  exporter:
    otlp:
      endpoint: "http://jaeger:4318"  # envia traces para o Jaeger
  traces:
    exporter: otlp
  metrics:
    exporter: none
  logs:
    exporter: none
```

> As propriedades `jwt.public.key` e `jwt.private.key` são declaradas explicitamente aqui — o `SecurityConfig` do módulo `shared` não tem mais valor default. A propriedade `spring.kafka.bootstrap-servers` aponta para `kafka:29092`, o nome DNS do broker na rede interna do Docker Compose.

---

### `pom.xml`

**Responsabilidade:** Declara todas as dependências e plugins de build do projeto.

**Dependências principais e seus papéis:**

| Dependência | Papel |
|---|---|
| `spring-boot-starter-web` | Servidor HTTP, controllers REST, Jackson (JSON) |
| `spring-boot-starter-data-jpa` | Hibernate, Spring Data, queries automáticas |
| `spring-boot-starter-validation` | Bean Validation (`@NotNull`, `@DecimalMin`) |
| `spring-boot-starter-actuator` | Endpoints `/actuator/health` e `/actuator/prometheus` |
| `postgresql` | Driver JDBC para PostgreSQL |
| `shared` (módulo local) | Fornece `SecurityConfig`, `app.key.pub`, `spring-boot-starter-security` e `spring-boot-starter-oauth2-resource-server` |
| `spring-cloud-starter-netflix-eureka-client` | Registro do serviço no Eureka |
| `spring-kafka` | Publicação de eventos de pagamento via `PaymentProducer` e `KafkaTemplate` (ver `config/KafkaConfig.java`, `producer/PaymentProducer.java`) |
| `spring-cloud-starter-circuitbreaker-resilience4j` | Circuit Breaker, não configurado ainda |
| `micrometer-registry-prometheus` | Expõe métricas no formato Prometheus |
| `opentelemetry-spring-boot-starter` | Rastreamento distribuído enviado ao Jaeger |
| `lombok` | Geração de código boilerplate (`@Builder`, `@Getter`, etc.) |
| `spring-boot-starter-log4j2` | Sistema de logging |
| `springdoc-openapi-starter-webmvc-ui` | Swagger UI e geração de spec OpenAPI |

> `spring-boot-starter-security` e `spring-boot-starter-oauth2-resource-server` não são dependências diretas do `payments` — chegam transitivamente via o módulo `shared`.

---

## 4. Explicação Método por Método

### `PaymentService.create(PaymentRequest request, UUID userId)`

**Objetivo:** Criar um novo pagamento, processá-lo (simulação de gateway) e publicar os eventos correspondentes no Kafka.

**Parâmetros:**
- `request` — body da requisição com `amount` e `method`, já validados pelo `@Valid` no controller
- `userId` — UUID do usuário autenticado, extraído do JWT pelo controller

**Fluxo interno:**
1. Constrói uma entidade `Payment` via Builder com os dados do request + userId + status `PENDING`
2. Persiste no banco via `repository.save()` — esta é a primeira gravação, com status `PENDING`
3. Publica no Kafka o evento referente a esse estado `PENDING` (tópico `payment.initiated`) via `producer.publish(toEvent(saved))`
4. Chama `process(saved)`, que decide o status final (`APPROVED` ou `FAILED`) e salva novamente no banco
5. Publica no Kafka o evento referente ao estado final (tópico `payment.approved` ou `payment.failed`)
6. Converte a entidade processada para `PaymentResponse` via `toResponse()`
7. Retorna o DTO já com o status final

**Retorno:** `PaymentResponse` com todos os campos preenchidos, incluindo o UUID gerado e o status final (`APPROVED` ou `FAILED` — nunca `PENDING`, pois o processamento acontece de forma síncrona dentro da mesma chamada).

**Exceções:** Nenhuma lançada diretamente. Erros de banco (ex: violação de constraint) seriam capturados pelo `GlobalExceptionHandler` como 500. Falhas ao publicar no Kafka são tratadas dentro de `PaymentProducer` (log de erro) e não interrompem o fluxo nem alteram a resposta HTTP.

**Exemplo prático:**
```
Entrada: { amount: 150.00, method: "PIX" }, userId: "uuid-abc"
Saída:   { id: "uuid-xyz", userId: "uuid-abc", amount: 150.00,
           method: "PIX", status: "APPROVED",
           createdAt: "2026-06-09T21:00:00", updatedAt: "2026-06-09T21:00:00" }
```

---

### `PaymentService.process(Payment payment)` (privado)

**Objetivo:** Simular a decisão de um gateway de pagamento, definindo se o pagamento é aprovado ou falha.

**Regra aplicada:** valor menor que `1000.00` → `APPROVED`; valor maior ou igual a `1000.00` → `FAILED`. É uma regra determinística e arbitrária, documentada como simulação acadêmica — não existe integração com gateway real (ver Seção 8, "Processamento de pagamento").

**Fluxo interno:**
1. Compara `payment.getAmount()` com `BigDecimal("1000.00")` via `compareTo`
2. Define o novo status na própria entidade (`payment.setStatus(status)`)
3. Persiste a alteração via `repository.save(payment)` — segunda gravação no banco para o mesmo pagamento
4. Retorna a entidade atualizada

**Retorno:** `Payment` com o status final já persistido.

**Utilizado por:** `create`, logo após a primeira gravação como `PENDING`.

---

### `PaymentService.toEvent(Payment payment)` (privado)

**Objetivo:** Converter uma entidade `Payment` no DTO `PaymentEvent`, usado exclusivamente para publicação no Kafka.

**Por que existe:** Mantém o formato do evento Kafka desacoplado do formato do `PaymentResponse` (DTO de resposta HTTP). Embora hoje os dois tenham campos parecidos, representam contratos diferentes — um é consumido por outros serviços via Kafka, o outro é consumido pelo cliente HTTP.

**Utilizado por:** `create`, duas vezes — uma vez para o estado `PENDING` e outra vez para o estado final (`APPROVED`/`FAILED`).

---

### `PaymentService.findById(UUID id, UUID userId)`

**Objetivo:** Consultar um pagamento específico, garantindo que apenas o dono possa acessá-lo.

**Parâmetros:**
- `id` — UUID do pagamento a consultar (vem da URL)
- `userId` — UUID do usuário autenticado (vem do JWT)

**Fluxo interno:**
1. Busca o pagamento no banco via `repository.findById(id)`
2. Se não encontrar → lança `ResponseStatusException(404, "Pagamento não encontrado")`
3. Compara `payment.getUserId()` com o `userId` recebido
4. Se diferentes → lança `ResponseStatusException(403, "Acesso negado")`
5. Converte para `PaymentResponse` e retorna

**Retorno:** `PaymentResponse` do pagamento encontrado.

**Exceções:**
- `404` — pagamento não existe no banco
- `403` — pagamento existe mas pertence a outro usuário

**Exemplo prático:**
```
Usuário A consulta seu pagamento   → 200 OK com os dados
Usuário A consulta pagamento de B  → 403 Forbidden
Qualquer usuário consulta id fake  → 404 Not Found
```

---

### `PaymentService.findByUser(UUID userId)`

**Objetivo:** Listar todos os pagamentos do usuário autenticado.

**Parâmetros:**
- `userId` — UUID do usuário autenticado (vem do JWT)

**Fluxo interno:**
1. Chama `repository.findByUserId(userId)`
2. Converte cada `Payment` para `PaymentResponse` via stream
3. Retorna a lista (pode ser vazia se o usuário não tiver pagamentos)

**Retorno:** `List<PaymentResponse>` — nunca null, pode ser lista vazia `[]`.

**Exceções:** Nenhuma. Lista vazia é um resultado válido.

**Exemplo prático:**
```
Usuário sem pagamentos → []
Usuário com 3 pagamentos → [ {...}, {...}, {...} ]
```

---

### `PaymentService.cancel(UUID id, UUID userId)`

**Objetivo:** Cancelar um pagamento, desde que ele pertença ao usuário e esteja em status `PENDING`.

**Parâmetros:**
- `id` — UUID do pagamento a cancelar (vem da URL)
- `userId` — UUID do usuário autenticado (vem do JWT)

**Fluxo interno:**
1. Busca o pagamento no banco via `repository.findById(id)`
2. Se não encontrar → `404`
3. Compara ownership → `403` se não for o dono
4. Verifica se status é `PENDING` → `422` se for qualquer outro status
5. Muda o status para `CANCELLED`
6. Persiste via `repository.save(payment)`
7. Converte e retorna o pagamento atualizado

**Retorno:** `PaymentResponse` com `status: "CANCELLED"` e `updatedAt` atualizado.

**Exceções:**
- `404` — pagamento não existe
- `403` — pagamento pertence a outro usuário
- `422 Unprocessable Entity` — pagamento não está em `PENDING`

**Exemplo prático:**
```
Cancela pagamento PENDING próprio   → 200 OK, status: "CANCELLED"
Cancela pagamento APPROVED próprio  → 422 Unprocessable Entity
Cancela pagamento de outro usuário  → 403 Forbidden
```

> **Atenção — limitação prática introduzida pelo processamento síncrono:** desde que `create()` passou a chamar `process()` de forma síncrona, todo pagamento criado já sai do banco como `APPROVED` ou `FAILED` — nunca permanece em `PENDING`. Como `cancel()` só aceita pagamentos em `PENDING`, esse método se tornou praticamente inalcançável no fluxo atual (só seria possível inserindo um registro `PENDING` diretamente no banco). O método e a regra continuam corretos e foram mantidos, mas essa condição deve ser reavaliada caso o processamento se torne assíncrono no futuro.

---

### `PaymentService.toResponse(Payment payment)` (privado)

**Objetivo:** Converter uma entidade JPA `Payment` no DTO `PaymentResponse`.

**Por que existe:** Centraliza a conversão em um único lugar. Se o `PaymentResponse` mudar (ex: adicionar um campo), a alteração é feita apenas aqui.

**Utilizado por:** `create`, `findById`, `findByUser`, `cancel` — todos os métodos públicos do service chamam este método antes de retornar.

---

### `SecurityConfig.securityFilterChain` e `SecurityConfig.jwtDecoder` (módulo `shared`)

**Objetivo:** Definir as regras de autorização HTTP e registrar o decoder JWT. Descritos aqui pois impactam diretamente o comportamento do `payments`.

**`securityFilterChain`** — regras:
- `/actuator/**` → livre (Docker healthcheck e Prometheus)
- `POST /auth/register`, `POST /auth/login` → livre (rotas públicas do `users`, ignoradas pelo `payments`)
- Qualquer outra rota → exige JWT válido no header `Authorization`, via `.anyRequest().authenticated()`
- CSRF desabilitado; sessão STATELESS

> Não existe regra liberando `/swagger-ui/**` ou `/v3/api-docs/**`. Com `.anyRequest().authenticated()` ativo, essas rotas também exigem JWT válido.

**`jwtDecoder`** — cria um `NimbusJwtDecoder` com a chave pública RSA (`app.key.pub`). Para cada requisição:
1. Decodifica o Base64 do token
2. Verifica a assinatura RSA
3. Verifica se o token está expirado
4. Disponibiliza os claims via `@AuthenticationPrincipal Jwt`

---

## 5. Segurança e JWT

### O que é JWT

JWT (JSON Web Token) é um padrão para transmitir informações de forma segura entre partes. Um token JWT tem três partes separadas por ponto:

```
header.payload.signature
```

- **Header:** algoritmo usado (RS256)
- **Payload:** claims — dados do usuário (sub, exp, iss)
- **Signature:** hash criptográfico que garante que o token não foi adulterado

### O que é RS256

RS256 é o algoritmo de assinatura digital usado neste projeto. Usa **criptografia assimétrica RSA**:

- Quem assina usa a **chave privada** — somente o `users` tem
- Quem verifica usa a **chave pública** — o `shared` tem, fornecendo ao `payments`

Isso significa que qualquer serviço pode verificar se um token é legítimo, mas **somente o `users` pode emitir tokens**. Se alguém tentar criar um token falso sem a chave privada, a verificação falhará.

### Fluxo completo de autenticação

```
┌─────────────────────────────────────────────────────────────────┐
│  EMISSOR (users)                                                 │
│                                                                 │
│  1. Usuário faz login com email + senha                         │
│  2. users valida no banco de dados                              │
│  3. Cria claims: sub = UUID, iss = "users-service", exp = +5min │
│  4. Assina com chave privada RSA (app.key)                      │
│  5. Retorna: { token: "eyJ...", expiresIn: 300 }                │
└─────────────────────────────────────────────────────────────────┘
                          │
                          │  token armazenado pelo cliente
                          ▼
┌─────────────────────────────────────────────────────────────────┐
│  VALIDADOR (payments via shared)                                │
│                                                                 │
│  1. Cliente envia: Authorization: Bearer eyJ...                 │
│  2. SecurityFilterChain (do shared) intercepta a requisição     │
│  3. JwtDecoder extrai e verifica o token:                       │
│     a. Decodifica Base64                                        │
│     b. Verifica assinatura com app.key.pub (do shared)          │
│     c. Verifica se exp > agora                                  │
│  4. Se inválido → 401 Unauthorized (sem chegar ao controller)   │
│  5. Se válido → Jwt disponível via @AuthenticationPrincipal     │
│  6. Controller extrai: UUID.fromString(jwt.getSubject())        │
└─────────────────────────────────────────────────────────────────┘
```

### Por que o `userId` nunca vem do body da requisição

Esta é uma regra crítica de segurança. Se o `userId` viesse do body:

```json
{ "userId": "uuid-de-outro-usuario", "amount": 100.00 }
```

Qualquer usuário poderia criar pagamentos em nome de outro — bastaria colocar o UUID de outra pessoa no campo `userId`. Não há como confiar em dados que o cliente mesmo enviou.

O token JWT, ao contrário, foi emitido pelo `users` após autenticação real e assinado criptograficamente. Se o `payments` conseguiu validar o token, é porque o `users` realmente emitiu aquele token para aquele usuário. O `sub` do token é confiável; o body da requisição não é.

### `@AuthenticationPrincipal Jwt jwt`

Esta anotação no parâmetro do método do controller pede ao Spring para injetar o objeto `Jwt` já decodificado que está no `SecurityContext`. É a forma idiomática do Spring Security de acessar os dados do usuário autenticado sem acoplamento com detalhes de implementação.

```java
// Extração do userId em todos os endpoints:
UUID userId = UUID.fromString(jwt.getSubject());
// jwt.getSubject() retorna o campo "sub" do payload do token
// que o users preencheu com user.getId().toString()
```

---

## 6. Endpoints

### `POST /payments` — Criar pagamento

| Atributo | Valor |
|---|---|
| Método | `POST` |
| URL | `/payments` |
| Autenticação | JWT obrigatório |
| HTTP Success | `201 Created` |

**Request body:**
```json
{
  "amount": 150.00,
  "method": "PIX"
}
```

| Campo | Tipo | Obrigatório | Validação |
|---|---|---|---|
| `amount` | `number` | Sim | Maior que 0.01 |
| `method` | `string` | Sim | `PIX`, `CREDIT_CARD` ou `BOLETO` |

**Response (201):**
```json
{
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "userId": "1a2b3c4d-5e6f-7890-abcd-ef1234567890",
  "amount": 150.00,
  "method": "PIX",
  "status": "APPROVED",
  "createdAt": "2026-06-09T21:00:00",
  "updatedAt": "2026-06-09T21:00:00"
}
```

> O `status` retornado nunca é `PENDING` — o processamento (simulação de gateway) acontece de forma síncrona dentro da própria chamada a `create()`, então a resposta já reflete o resultado final: `APPROVED` (amount < 1000.00) ou `FAILED` (amount >= 1000.00). Ver Seção 8, "Processamento de pagamento".

**Possíveis erros:**

| Status | Motivo |
|---|---|
| `400 Bad Request` | `amount` nulo, negativo ou zero; `method` inválido ou nulo |
| `401 Unauthorized` | Token ausente, expirado ou inválido |

---

### `GET /payments/{id}` — Consultar pagamento por ID

| Atributo | Valor |
|---|---|
| Método | `GET` |
| URL | `/payments/{id}` |
| Autenticação | JWT obrigatório |
| HTTP Success | `200 OK` |

**Path parameter:**
- `id` — UUID do pagamento

**Response (200):**
```json
{
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "userId": "1a2b3c4d-5e6f-7890-abcd-ef1234567890",
  "amount": 150.00,
  "method": "PIX",
  "status": "APPROVED",
  "createdAt": "2026-06-09T21:00:00",
  "updatedAt": "2026-06-09T21:00:00"
}
```

> O `status` aqui pode ser `APPROVED`, `FAILED` ou `CANCELLED` — na prática não é mais possível observar `PENDING`, já que o processamento ocorre de forma síncrona no momento da criação (ver Seção 8).

**Possíveis erros:**

| Status | Motivo |
|---|---|
| `401 Unauthorized` | Token ausente, expirado ou inválido |
| `403 Forbidden` | Pagamento existe mas pertence a outro usuário |
| `404 Not Found` | Nenhum pagamento com este ID no banco |

---

### `GET /payments/my` — Listar meus pagamentos

| Atributo | Valor |
|---|---|
| Método | `GET` |
| URL | `/payments/my` |
| Autenticação | JWT obrigatório |
| HTTP Success | `200 OK` |

**Response (200):**
```json
[
  {
    "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "userId": "1a2b3c4d-5e6f-7890-abcd-ef1234567890",
    "amount": 150.00,
    "method": "PIX",
    "status": "APPROVED",
    "createdAt": "2026-06-09T21:00:00",
    "updatedAt": "2026-06-09T21:00:00"
  }
]
```

Retorna `[]` (array vazio) se o usuário não tiver pagamentos — nunca 404.

**Possíveis erros:**

| Status | Motivo |
|---|---|
| `401 Unauthorized` | Token ausente, expirado ou inválido |

---

### `PATCH /payments/{id}/cancel` — Cancelar pagamento

| Atributo | Valor |
|---|---|
| Método | `PATCH` |
| URL | `/payments/{id}/cancel` |
| Autenticação | JWT obrigatório |
| HTTP Success | `200 OK` |

**Path parameter:**
- `id` — UUID do pagamento a cancelar

**Sem body.** A operação não requer dados adicionais.

**Response (200):**
```json
{
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "userId": "1a2b3c4d-5e6f-7890-abcd-ef1234567890",
  "amount": 150.00,
  "method": "PIX",
  "status": "CANCELLED",
  "createdAt": "2026-06-09T21:00:00",
  "updatedAt": "2026-06-09T21:05:00"
}
```

**Possíveis erros:**

| Status | Motivo |
|---|---|
| `401 Unauthorized` | Token ausente, expirado ou inválido |
| `403 Forbidden` | Pagamento pertence a outro usuário |
| `404 Not Found` | Pagamento não existe |
| `422 Unprocessable Entity` | Status não é `PENDING` (já aprovado, falhou ou foi cancelado) |

---

## 7. Banco de Dados

### Entidade e tabela

A classe `Payment` é mapeada para a tabela `payments` no banco `payments` do PostgreSQL. O Hibernate cria e atualiza a tabela automaticamente via `ddl-auto: update`.

### Estrutura da tabela

| Coluna | Tipo SQL | Constraints | Descrição |
|---|---|---|---|
| `id` | `UUID` | PK, NOT NULL | Gerado automaticamente pelo Hibernate |
| `user_id` | `UUID` | NOT NULL | UUID do usuário dono do pagamento |
| `amount` | `NUMERIC(19,2)` | NOT NULL | Valor com precisão para dinheiro |
| `method` | `VARCHAR(255)` | NOT NULL | Nome do enum: `PIX`, `CREDIT_CARD`, `BOLETO` |
| `status` | `VARCHAR(255)` | NOT NULL | Nome do enum: `PENDING`, `APPROVED`, etc. |
| `created_at` | `TIMESTAMP` | NOT NULL | Preenchido automaticamente na criação |
| `updated_at` | `TIMESTAMP` | NOT NULL | Atualizado automaticamente a cada save |

### Por que `NUMERIC(19,2)` para amount

`double` e `float` usam representação binária de ponto flutuante, que introduz erros de arredondamento em valores decimais. `0.1 + 0.2 = 0.30000000000000004` em ponto flutuante. Para dinheiro, isso é inaceitável. `NUMERIC(19,2)` é exato: 19 dígitos significativos com 2 casas decimais.

### Por que os enums são persistidos como String

`@Enumerated(EnumType.STRING)` persiste `PENDING`, `APPROVED`, etc. em vez de `0`, `1`, `2`. Isso torna os dados legíveis diretamente no banco e evita que a reordenação dos valores no enum Java quebre os dados históricos.

### Relação com usuários

O `userId` na tabela `payments` é apenas um UUID — não há chave estrangeira para a tabela `users`. Isso é intencional na arquitetura de microsserviços: cada serviço tem seu próprio banco e **não deve ter dependências de banco cruzadas**. A consistência é garantida pela lógica da aplicação (o JWT válido garante que o userId existe no `users`).

### Conexão

```yaml
url: jdbc:postgresql://pg:5432/payments
username: admin
password: admin
```

`pg` é o nome DNS do container PostgreSQL dentro da rede Docker. Fora do Docker (desenvolvimento local sem Docker), seria necessário usar `localhost:5432`.

---

## 8. Regras de Negócio

### Criação de pagamento

- Todo pagamento nasce com status `PENDING` e é persistido nesse estado antes de qualquer processamento
- O `userId` vem **exclusivamente** do JWT — nunca do body
- O `amount` deve ser maior que `0.01`
- O `method` deve ser um dos valores válidos do enum `PaymentMethod`
- Não há limite de pagamentos por usuário
- Logo após a criação, o pagamento é processado de forma síncrona (ver "Processamento de pagamento" abaixo) — a resposta da requisição já reflete o status final

### Processamento de pagamento

- Não há integração com gateway de pagamento real (Mercado Pago, Stripe, PagSeguro, Cielo, etc.) — o objetivo do microsserviço é acadêmico, e a decisão de aprovação/reprovação é simulada internamente
- Regra determinística aplicada em `PaymentService.process()`: `amount < 1000.00` → `APPROVED`; `amount >= 1000.00` → `FAILED`
- O processamento ocorre de forma síncrona, dentro da mesma chamada a `create()` — não existe fila, delay ou callback assíncrono
- Cada mudança de status gera um evento publicado no Kafka (ver "Eventos Kafka" abaixo e Seção 3, `producer/PaymentProducer.java`)

### Eventos Kafka

- Toda criação de pagamento publica dois eventos: um para o estado `PENDING` (tópico `payment.initiated`) e outro para o estado final (`payment.approved` ou `payment.failed`)
- A chave de cada mensagem é o `paymentId` (como String), garantindo que eventos do mesmo pagamento fiquem na mesma partição
- A publicação é assíncrona e não bloqueia nem altera a resposta HTTP — falhas de publicação são apenas logadas (ver `PaymentProducer.publish()`)
- Os tópicos são criados explicitamente via `KafkaConfig` (`KAFKA_AUTO_CREATE_TOPICS_ENABLE` está desabilitado no cluster)

### Consulta de pagamento

- Qualquer pagamento pode ser consultado pelo seu dono
- Um usuário nunca pode ver pagamento de outro usuário → `403`
- Um ID inexistente retorna `404` (não `403`, para não revelar se o recurso existe)

### Listagem de pagamentos

- Retorna apenas os pagamentos do usuário autenticado
- Sem filtros, paginação ou ordenação na versão atual
- Lista vazia é retorno válido — nunca gera erro

### Cancelamento

- Só é possível cancelar pagamentos em status `PENDING`
- Tentar cancelar `APPROVED`, `FAILED` ou `CANCELLED` → `422 Unprocessable Entity`
- O dono do pagamento é verificado antes do status — `403` tem precedência sobre `422`
- Após cancelamento, o status não pode mais ser alterado (não há endpoint para tal)

> **Limitação prática atual:** como o processamento (acima) é síncrono, todo pagamento criado já sai do banco como `APPROVED` ou `FAILED` — nunca permanece em `PENDING`. Na prática, não há mais como atingir o endpoint de cancelamento com um pagamento elegível, a menos que um registro `PENDING` seja inserido manualmente no banco. A regra de negócio em si permanece válida e implementada.

### Códigos HTTP utilizados

| Código | Nome | Quando ocorre |
|---|---|---|
| `200` | OK | Consulta, listagem e cancelamento bem-sucedidos |
| `201` | Created | Pagamento criado com sucesso |
| `400` | Bad Request | Dados de entrada inválidos (Bean Validation) |
| `401` | Unauthorized | Token JWT ausente, expirado ou com assinatura inválida |
| `403` | Forbidden | Usuário autenticado tentando acessar recurso de outro usuário |
| `404` | Not Found | Pagamento não encontrado no banco |
| `422` | Unprocessable Entity | Operação inválida dado o estado atual do pagamento |
| `500` | Internal Server Error | Erro inesperado não tratado |

---

## 9. Fluxos Reais

### Fluxo 1 — Criação de pagamento (caminho feliz)

```
Cliente
  POST /payments
  Authorization: Bearer eyJ...
  Body: { "amount": 200.00, "method": "CREDIT_CARD" }
  │
  ▼
SecurityFilterChain (shared)
  Extrai token do header → JwtDecoder verifica assinatura → válido
  │
  ▼
PaymentController.create()
  @Valid valida o body → amount > 0 e method válido → ok
  userId = UUID.fromString(jwt.getSubject()) → "uuid-abc"
  service.create(request, userId)
  │
  ▼
PaymentService.create()
  Payment.builder()
    .userId("uuid-abc")
    .amount(200.00)
    .method(CREDIT_CARD)
    .status(PENDING)
    .build()
  saved = repository.save(payment)
  │
  ▼
PaymentRepository → PostgreSQL (1ª gravação)
  INSERT INTO payments (id, user_id, amount, method, status, created_at, updated_at)
  VALUES (gen_uuid, 'uuid-abc', 200.00, 'CREDIT_CARD', 'PENDING', now(), now())
  │
  ▼
PaymentProducer.publish(toEvent(saved))
  status = PENDING → tópico "payment.initiated"
  kafkaTemplate.send("payment.initiated", paymentId, event)  [assíncrono]
  │
  ▼
PaymentService.process(saved)
  amount (200.00) < 1000.00 → status = APPROVED
  processed = repository.save(payment)
  │
  ▼
PaymentRepository → PostgreSQL (2ª gravação)
  UPDATE payments SET status = 'APPROVED', updated_at = now() WHERE id = gen_uuid
  │
  ▼
PaymentProducer.publish(toEvent(processed))
  status = APPROVED → tópico "payment.approved"
  kafkaTemplate.send("payment.approved", paymentId, event)  [assíncrono]
  │
  ▼
PaymentService
  toResponse(processed) → PaymentResponse
  │
  ▼
Cliente recebe: 201 Created
{ "id": "uuid-xyz", "status": "APPROVED", ... }
```

> Se `amount` fosse `1500.00` (>= 1000.00), o fluxo seria idêntico até `process()`, que definiria `status = FAILED` e o evento publicado no segundo passo iria para o tópico `payment.failed` em vez de `payment.approved`. A resposta HTTP continua `201 Created` — falha no processamento simulado não é um erro HTTP, é um resultado de negócio válido.

---

### Fluxo 2 — Listagem de pagamentos

```
Cliente
  GET /payments/my
  Authorization: Bearer eyJ...
  │
  ▼
SecurityFilterChain (shared) → token válido
  │
  ▼
PaymentController.findMyPayments()
  userId = UUID.fromString(jwt.getSubject())
  service.findByUser(userId)
  │
  ▼
PaymentService.findByUser()
  repository.findByUserId("uuid-abc")
  │
  ▼
PostgreSQL
  SELECT * FROM payments WHERE user_id = 'uuid-abc'
  Retorna lista (pode ser vazia)
  │
  ▼
PaymentService
  .stream().map(toResponse).toList()
  │
  ▼
Cliente recebe: 200 OK  [ {...}, {...} ]
```

---

### Fluxo 3 — Tentativa de acesso indevido (403)

```
Cliente (usuário B, token de B)
  GET /payments/uuid-do-pagamento-de-A
  Authorization: Bearer eyJ...token-de-B...
  │
  ▼
SecurityFilterChain (shared) → token de B é válido → userId = "uuid-B"
  │
  ▼
PaymentController.findById()
  service.findById("uuid-do-pagamento-de-A", "uuid-B")
  │
  ▼
PaymentService.findById()
  repository.findById("uuid-do-pagamento-de-A") → encontrou (pertence ao usuário A)
  payment.getUserId() = "uuid-A"
  "uuid-A".equals("uuid-B") → false
  throw new ResponseStatusException(403, "Acesso negado")
  │
  ▼
GlobalExceptionHandler.handleResponseStatus()
  │
  ▼
Cliente recebe: 403 Forbidden
{ "timestamp": "...", "status": 403, "message": "Acesso negado" }
```

---

### Fluxo 4 — Cancelamento válido

```
Cliente
  PATCH /payments/uuid-xyz/cancel
  Authorization: Bearer eyJ...
  │
  ▼
SecurityFilterChain (shared) → válido → userId = "uuid-abc"
  │
  ▼
PaymentController.cancel()
  service.cancel("uuid-xyz", "uuid-abc")
  │
  ▼
PaymentService.cancel()
  repository.findById("uuid-xyz") → encontrou, status = PENDING
  payment.getUserId() = "uuid-abc" = userId → é o dono
  payment.getStatus() = PENDING → cancelamento permitido
  payment.setStatus(CANCELLED)
  repository.save(payment)
  │
  ▼
PostgreSQL
  UPDATE payments SET status = 'CANCELLED', updated_at = now()
  WHERE id = 'uuid-xyz'
  │
  ▼
Cliente recebe: 200 OK
{ "id": "uuid-xyz", "status": "CANCELLED", "updatedAt": "...", ... }
```

---

### Fluxo 5 — Cancelamento inválido (422)

```
Cliente tenta cancelar um pagamento já APPROVED
  PATCH /payments/uuid-xyz/cancel
  │
  ▼
PaymentService.cancel()
  repository.findById("uuid-xyz") → encontrou, status = APPROVED
  payment.getUserId() = userId → é o dono
  payment.getStatus() = APPROVED ≠ PENDING
  throw new ResponseStatusException(422, "Apenas pagamentos com status PENDING podem ser cancelados")
  │
  ▼
Cliente recebe: 422 Unprocessable Entity
{ "timestamp": "...", "status": 422, "message": "Apenas pagamentos com status PENDING podem ser cancelados" }
```

---

### Fluxo 6 — Requisição sem token (401)

```
Cliente
  GET /payments/my
  (sem header Authorization)
  │
  ▼
SecurityFilterChain (shared)
  Nenhum token encontrado
  Retorna 401 imediatamente
  O controller nunca é chamado
  │
  ▼
Cliente recebe: 401 Unauthorized
```

---

## 10. Estado Atual do Microsserviço

### Funcionalidades implementadas

- [x] Criação de pagamento com status `PENDING`
- [x] Processamento de pagamento (simulação de gateway, regra determinística por valor)
- [x] Transições de status `PENDING → APPROVED` ou `PENDING → FAILED`
- [x] Eventos Kafka (`payment.initiated`, `payment.approved`, `payment.failed`)
- [x] Consulta de pagamento por ID (com verificação de ownership)
- [x] Listagem de pagamentos do usuário autenticado
- [x] Cancelamento de pagamento (apenas se `PENDING` — ver limitação prática na Seção 8)
- [x] Autenticação via JWT RS256 (via módulo `shared`)
- [x] Extração segura do `userId` pelo claim `sub` do token
- [x] Tratamento centralizado de erros com respostas padronizadas
- [x] Validação de entrada com Bean Validation
- [x] Registro no Eureka
- [x] Rastreamento distribuído via OpenTelemetry → Jaeger
- [x] Métricas expostas para Prometheus via `/actuator/prometheus`
- [x] Health check em `/actuator/health`
- [x] Documentação Swagger em `/swagger-ui.html` e `/v3/api-docs`

### Funcionalidades pendentes

- [ ] Testes unitários e de integração (PaymentService, Kafka via EmbeddedKafka)
- [ ] Serialização de `timestamp` como ISO-8601 no `PaymentEvent` (atualmente serializa como array Jackson — ver Limitações atuais)
- [ ] Endpoint de webhook (`POST /payments/webhook`)
- [ ] Proteção JWT no Kong para as rotas de `/payments`
- [ ] Flyway para controle de migrações de banco
- [ ] Métricas customizadas com Micrometer (contadores por status)
- [ ] Paginação na listagem de pagamentos

### Limitações atuais

- O campo `timestamp` do `PaymentEvent` serializa como array Jackson (`[2026,6,16,23,30,26,267482000]`) em vez de String ISO-8601 — isso precisa ser corrigido antes que outros serviços comecem a consumir esses tópicos, pois exige um parser específico no lado do consumidor
- Com o processamento síncrono, todo pagamento criado já sai como `APPROVED` ou `FAILED` — o cancelamento (`PENDING` obrigatório) ficou praticamente inalcançável no fluxo real (ver Seção 8)
- As rotas `/payments` no Kong não exigem JWT — a proteção existe apenas no nível da aplicação
- `ddl-auto: update` — adequado para desenvolvimento, inadequado para produção

### Débitos técnicos identificados

| Débito | Impacto | Solução recomendada |
|---|---|---|
| `ddl-auto: update` | Migrações silenciosas podem corromper dados em produção | Flyway com scripts `V1__...sql` |
| Sem paginação na listagem | Um usuário com muitos pagamentos pode sobrecarregar a memória | `Pageable` no repository + `Page<PaymentResponse>` no controller |
| Sem índice em `user_id` | Queries lentas com volume alto de dados | `@Index` na entidade ou migration Flyway |
| Rotas sem JWT no Kong | Qualquer cliente externo pode chamar sem token se passar direto pelo Kong | Adicionar plugin JWT no `kong.yaml` |
| `app.key.pub` no repositório (via shared) | Má prática de segurança — chaves não deveriam estar no código | Secrets do Docker/Kubernetes ou variável de ambiente |
| `timestamp` serializado como array no `PaymentEvent` | Consumidores Kafka precisam de parser específico em vez de ISO-8601 padrão | Configurar Jackson para serializar `LocalDateTime` como String (`WRITE_DATES_AS_TIMESTAMPS = false`) |

---

## 11. Próximos Passos

Consulte o arquivo `IMPLEMENTACAO.md` na raiz deste módulo para o plano detalhado de implementação com etapas, critérios de conclusão e checklist de validação.

Resumo das próximas etapas em ordem:

1. ~~**Etapa 1 — Processamento de pagamento**~~ — concluída (simulação de gateway, transições de status)
2. ~~**Etapa 2 — Kafka**~~ — concluída (tópicos, producer, eventos `payment.initiated`/`payment.approved`/`payment.failed`)
3. **Etapa 3 — Testes** — unitários (`PaymentService`), integração (banco), Kafka (`EmbeddedKafka`)
4. **Etapa 4 — Infraestrutura e integração** — Kong (proteção JWT nas rotas `/payments`), Flyway, correção da serialização do `timestamp`
5. **Etapa 5 — Validação completa do ecossistema** — fluxo ponta a ponta com `users` e demais serviços consumindo os eventos do Kafka

---

## 12. Guia para Evolução

### Onde adicionar novos endpoints

1. Crie o método no `PaymentService` com a lógica de negócio
2. Crie o método no `PaymentController` com a anotação HTTP adequada
3. O método do controller deve:
   - Extrair o `userId` do JWT via `UUID.fromString(jwt.getSubject())`
   - Delegar toda lógica ao service
   - Nunca conter `if`, validações de negócio ou acesso ao repository diretamente

### Onde implementar novas regras de negócio

**Sempre no `PaymentService`.** Nunca no controller, nunca no repository.

### Onde adicionar novas consultas ao banco

**No `PaymentRepository`**, usando derived queries do Spring Data JPA:

```java
// Exemplos de queries que podem ser necessárias no futuro:
List<Payment> findByUserIdAndStatus(UUID userId, PaymentStatus status);
List<Payment> findByUserIdOrderByCreatedAtDesc(UUID userId);
Page<Payment> findByUserId(UUID userId, Pageable pageable);
```

### Onde adicionar novas validações

- **Validações de entrada** (campos obrigatórios, formatos): no `PaymentRequest.java` via anotações Bean Validation
- **Validações de negócio** (regras do domínio): no `PaymentService`, lançando `ResponseStatusException` com o status adequado
- **Nunca** no controller ou no repository

### Cuidados arquiteturais importantes

1. **Nunca expor a entidade `Payment` diretamente na API** — sempre converter para `PaymentResponse` via `toResponse()`
2. **O `userId` sempre vem do JWT** — nunca aceitar userId do body da requisição
3. **Verificar ownership antes de qualquer operação** — sempre checar se o pagamento pertence ao usuário antes de retornar ou modificar
4. **Lançar `ResponseStatusException` no service** — o `GlobalExceptionHandler` já trata automaticamente
5. **Não colocar lógica de negócio no controller** — o controller é apenas tradutor entre HTTP e o service
6. **Não criar `SecurityConfig` local** — a segurança vem do módulo `shared`; qualquer alteração nas regras de segurança deve ser feita em `services/shared/src/main/java/com/edu/infnet/pb/config/SecurityConfig.java`

### Padrões já utilizados que devem ser mantidos

| Padrão | Como está implementado | Por que manter |
|---|---|---|
| Injeção por construtor | `@RequiredArgsConstructor` + `final` | Imutabilidade, facilita testes |
| DTO de entrada e saída separados | `PaymentRequest` / `PaymentResponse` | Desacopla API da entidade interna |
| `record` para DTOs | `public record PaymentRequest(...)` | Imutável, sem boilerplate, idiomático no Java moderno |
| Extração de `userId` do JWT | `UUID.fromString(jwt.getSubject())` | Segurança — nunca confiar no body |
| `ResponseStatusException` | Lançada diretamente no service | Simples, sem necessidade de exceptions customizadas por ora |
| `toResponse()` privado no service | Método centralizado de conversão | Manutenção em um único lugar |
| Segurança via `shared` | `scanBasePackages = "com.edu.infnet.pb"` | Centraliza config de segurança em um único lugar para todos os serviços |

---

## 13. Resumo Final

O microsserviço `payments` é um **Resource Server OAuth2** que gerencia o ciclo de vida básico de pagamentos dentro da plataforma PB Mono Repo.

**Stack:** Java 25 + Spring Boot 3.5.14 + Spring Security (via `shared`) + OAuth2 Resource Server (via `shared`) + Spring Data JPA + PostgreSQL.

**Segurança:** Valida tokens JWT RS256 emitidos pelo microsserviço `users` usando a chave pública RSA fornecida pelo módulo `shared`. O `userId` é sempre extraído do campo `sub` do token — nunca aceito do corpo da requisição. O `SecurityConfig` e o `app.key.pub` vivem em `services/shared/`, não neste módulo.

**Endpoints disponíveis:**

| Método | Rota | O que faz |
|---|---|---|
| `POST` | `/payments` | Cria pagamento, processa (simulação de gateway) e publica eventos no Kafka; retorna `APPROVED` ou `FAILED` |
| `GET` | `/payments/{id}` | Consulta pagamento (apenas o dono) |
| `GET` | `/payments/my` | Lista todos os pagamentos do usuário autenticado |
| `PATCH` | `/payments/{id}/cancel` | Cancela pagamento se estiver em `PENDING` (ver limitação prática na Seção 8) |

**Regras centrais:**
- Todo pagamento nasce `PENDING`, é persistido nesse estado e em seguida processado de forma síncrona (`amount < 1000.00` → `APPROVED`; `amount >= 1000.00` → `FAILED`)
- Cada transição de status publica um evento Kafka (`payment.initiated`, `payment.approved` ou `payment.failed`)
- Apenas o dono do pagamento pode consultá-lo ou cancelá-lo
- Cancelamento só é permitido no status `PENDING`
- Erros retornam sempre JSON com `timestamp`, `status` e `message`

**O que ainda não está implementado:** testes (unitários e de integração), correção da serialização do `timestamp` no evento Kafka (atualmente array Jackson, não ISO-8601), proteção JWT no Kong, Flyway para migrações.

**Onde mexer para cada tarefa:**

| Tarefa | Arquivo |
|---|---|
| Novo endpoint | `PaymentController` + `PaymentService` |
| Nova regra de negócio | `PaymentService` |
| Nova consulta ao banco | `PaymentRepository` |
| Nova validação de entrada | `PaymentRequest` |
| Novo tipo de erro tratado | `GlobalExceptionHandler` |
| Configuração de segurança | `services/shared/.../SecurityConfig.java` |
| Nova configuração da aplicação | `application.yaml` |
| Documentação da API | `OpenAPIConfig` |
