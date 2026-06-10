# Documentação Técnica — Microsserviço `payments`

> Documento de referência para estudo, manutenção e onboarding.
> Baseado no código efetivamente implementado em junho de 2026.

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
| Chave RSA utilizada | `app.key` (privada) + `app.key.pub` (pública) | `app.key.pub` (pública apenas) |
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
5. payments intercepta a requisição via Spring Security
6. JwtDecoder verifica a assinatura do token usando app.key.pub
7. Se válido: extrai sub → UUID do usuário → processa a operação
8. Se inválido ou expirado: retorna 401 automaticamente
```

---

## 2. Arquitetura

### Estrutura de pastas

```
services/payments/
├── src/
│   ├── main/
│   │   ├── java/com/edu/infnet/pb/payments/
│   │   │   ├── PaymentsApplication.java       ← ponto de entrada
│   │   │   ├── config/
│   │   │   │   └── SecurityConfig.java        ← configuração de segurança JWT
│   │   │   ├── controller/
│   │   │   │   └── PaymentController.java     ← endpoints REST
│   │   │   ├── dto/
│   │   │   │   ├── PaymentRequest.java        ← dados de entrada da API
│   │   │   │   └── PaymentResponse.java       ← dados de saída da API
│   │   │   ├── entity/
│   │   │   │   └── Payment.java               ← entidade JPA (tabela payments)
│   │   │   ├── enums/
│   │   │   │   ├── PaymentMethod.java         ← métodos de pagamento disponíveis
│   │   │   │   └── PaymentStatus.java         ← estados possíveis de um pagamento
│   │   │   ├── exception/
│   │   │   │   └── GlobalExceptionHandler.java ← tratamento centralizado de erros
│   │   │   ├── repository/
│   │   │   │   └── PaymentRepository.java     ← acesso ao banco de dados
│   │   │   └── service/
│   │   │       └── PaymentService.java        ← regras de negócio
│   │   └── resources/
│   │       ├── app.key.pub                    ← chave pública RSA (valida JWT)
│   │       └── application.yaml              ← configurações da aplicação
│   └── test/
│       └── java/com/edu/infnet/pb/payments/
│           └── PaymentsApplicationTests.java  ← teste de contexto
├── Dockerfile                                 ← build multi-stage
├── pom.xml                                    ← dependências Maven
└── mvnw                                       ← Maven wrapper
```

### Responsabilidade de cada camada

| Camada | Pacote | Responsabilidade |
|---|---|---|
| **Controller** | `controller/` | Receber requisições HTTP, extrair dados do JWT, delegar ao service, retornar resposta |
| **Service** | `service/` | Implementar regras de negócio, validar ownership, lançar exceções de negócio |
| **Repository** | `repository/` | Executar queries no banco de dados via Spring Data JPA |
| **Entity** | `entity/` | Representar a tabela `payments` no banco — nunca exposta diretamente na API |
| **DTO** | `dto/` | Contratos de entrada (Request) e saída (Response) da API |
| **Enums** | `enums/` | Valores válidos para status e método de pagamento |
| **Config** | `config/` | Configurações de infraestrutura (segurança, beans do Spring) |
| **Exception** | `exception/` | Interceptar e formatar erros de forma padronizada |

### Fluxo completo de uma requisição

```
Cliente (Postman / Frontend)
  │
  │  Authorization: Bearer <jwt>
  │  POST /payments  { amount: 100.00, method: "PIX" }
  ▼
Spring Security Filter Chain  (SecurityConfig)
  │  1. Intercepta a requisição
  │  2. Extrai o token do header Authorization
  │  3. JwtDecoder verifica assinatura com app.key.pub
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

**Por que existe:** Todo projeto Spring Boot precisa de exatamente uma classe anotada com `@SpringBootApplication`. Ela dispara o component scan a partir do pacote `com.edu.infnet.pb.payments`, encontrando automaticamente todos os `@Service`, `@Repository`, `@RestController` e `@Configuration` do projeto.

**Quem utiliza:** É o ponto de entrada do Docker — o `Dockerfile` executa esta classe via `java -jar`.

**Nenhum outro arquivo depende diretamente dela.**

---

### `config/SecurityConfig.java`

**Localização:** `com/edu/infnet/pb/payments/config/SecurityConfig.java`

**Responsabilidade:** Define as regras de segurança HTTP e registra o componente que valida tokens JWT.

**Por que existe:** Com `spring-boot-starter-security` no classpath, o Spring Security está ativo e bloqueia tudo por padrão. Esta classe sobrescreve esse comportamento padrão com as regras específicas do `payments`: libera `/actuator/**` e exige JWT em todo o resto.

**Quem utiliza:** O próprio Spring Security — lê esta configuração ao inicializar. Nenhum outro código chama esta classe diretamente.

**Como participa do fluxo:** Toda requisição HTTP passa pelo `SecurityFilterChain` definido aqui antes de chegar em qualquer controller. É a primeira barreira de segurança da aplicação.

**Dependências:**
- `app.key.pub` — lido via `@Value("${jwt.public.key}")`, convertido para `RSAPublicKey`
- `application.yaml` — propriedade `jwt.public.key` que aponta para o arquivo

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

**Responsabilidade:** Implementar todas as regras de negócio do domínio de pagamentos. É a camada central da aplicação.

**Por que existe:** Separa a lógica de negócio do protocolo HTTP (controller) e do acesso a dados (repository). Se amanhã a API mudar de REST para gRPC, o service não muda.

**Quem utiliza:** `PaymentController` — é o único componente que deve chamar o service diretamente.

**Depende de:** `PaymentRepository`, `Payment`, `PaymentRequest`, `PaymentResponse`, `PaymentStatus`.

---

### `resources/app.key.pub`

**Localização:** `src/main/resources/app.key.pub`

**Responsabilidade:** Armazenar a chave pública RSA usada para verificar a assinatura dos tokens JWT.

**Por que existe:** O `users` assina os tokens com sua chave privada (`app.key`). Para verificar que um token é legítimo — que foi realmente emitido pelo `users` e não foi adulterado — o `payments` precisa da chave pública correspondente. É matematicamente impossível verificar sem ela.

**Como é carregado:** O Spring lê o arquivo via `jwt.public.key: classpath:app.key.pub` no `application.yaml` e converte automaticamente para `RSAPublicKey` quando injetado com `@Value` no `SecurityConfig`.

**Cuidado:** Este arquivo deve ser **idêntico** ao `app.key.pub` do microsserviço `users`. Se o `users` trocar seu par de chaves RSA, o `app.key.pub` do `payments` deve ser atualizado junto — caso contrário todos os tokens emitidos pelo `users` passarão a ser rejeitados pelo `payments`.

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

  jpa:
    hibernate:
      ddl-auto: update       # Hibernate cria/atualiza tabelas automaticamente
    show-sql: true           # loga todas as queries SQL no console

jwt:
  public:
    key: classpath:app.key.pub  # caminho da chave pública RSA

server:
  port: 8085                 # porta do serviço

eureka:                      # registro e descoberta de serviços
  client:
    service-url:
      defaultZone: http://eureka:8761/eureka/

logging:
  pattern:
    correlation: "[payments,traceId,spanId]"  # correlaciona logs com traces

management:
  tracing:
    sampling:
      probability: 1.0       # 100% das requisições são rastreadas no Jaeger
  endpoints:
    web:
      exposure:
        include: [health, prometheus, info]  # endpoints do Actuator expostos

otel:
  exporter:
    otlp:
      endpoint: "http://jaeger:4318"  # envia traces para o Jaeger
```

---

### `pom.xml`

**Responsabilidade:** Declara todas as dependências e plugins de build do projeto.

**Dependências principais e seus papéis:**

| Dependência | Papel |
|---|---|
| `spring-boot-starter-web` | Servidor HTTP, controllers REST, Jackson (JSON) |
| `spring-boot-starter-data-jpa` | Hibernate, Spring Data, queries automáticas |
| `spring-boot-starter-security` | Framework de segurança, filtros HTTP |
| `spring-boot-starter-oauth2-resource-server` | Suporte a JWT, `NimbusJwtDecoder` |
| `spring-boot-starter-validation` | Bean Validation (`@NotNull`, `@DecimalMin`) |
| `spring-boot-starter-actuator` | Endpoints `/actuator/health` e `/actuator/prometheus` |
| `postgresql` | Driver JDBC para PostgreSQL |
| `spring-cloud-starter-netflix-eureka-client` | Registro do serviço no Eureka |
| `spring-kafka` | Dependência presente, Kafka não implementado ainda |
| `spring-cloud-starter-circuitbreaker-resilience4j` | Circuit Breaker, não configurado ainda |
| `micrometer-registry-prometheus` | Expõe métricas no formato Prometheus |
| `opentelemetry-spring-boot-starter` | Rastreamento distribuído enviado ao Jaeger |
| `lombok` | Geração de código boilerplate (`@Builder`, `@Getter`, etc.) |
| `spring-boot-starter-log4j2` | Sistema de logging |

---

## 4. Explicação Método por Método

### `PaymentService.create(PaymentRequest request, UUID userId)`

**Objetivo:** Criar um novo pagamento no banco de dados.

**Parâmetros:**
- `request` — body da requisição com `amount` e `method`, já validados pelo `@Valid` no controller
- `userId` — UUID do usuário autenticado, extraído do JWT pelo controller

**Fluxo interno:**
1. Constrói uma entidade `Payment` via Builder com os dados do request + userId + status `PENDING`
2. Persiste no banco via `repository.save()`
3. O Hibernate preenche `id`, `createdAt` e `updatedAt` automaticamente
4. Converte a entidade salva para `PaymentResponse` via `toResponse()`
5. Retorna o DTO

**Retorno:** `PaymentResponse` com todos os campos preenchidos, incluindo o UUID gerado.

**Exceções:** Nenhuma lançada diretamente. Erros de banco (ex: violação de constraint) seriam capturados pelo `GlobalExceptionHandler` como 500.

**Exemplo prático:**
```
Entrada: { amount: 150.00, method: "PIX" }, userId: "uuid-abc"
Saída:   { id: "uuid-xyz", userId: "uuid-abc", amount: 150.00,
           method: "PIX", status: "PENDING",
           createdAt: "2026-06-09T21:00:00", updatedAt: "2026-06-09T21:00:00" }
```

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

---

### `PaymentService.toResponse(Payment payment)` (privado)

**Objetivo:** Converter uma entidade JPA `Payment` no DTO `PaymentResponse`.

**Por que existe:** Centraliza a conversão em um único lugar. Se o `PaymentResponse` mudar (ex: adicionar um campo), a alteração é feita apenas aqui.

**Utilizado por:** `create`, `findById`, `findByUser`, `cancel` — todos os métodos públicos do service chamam este método antes de retornar.

---

### `SecurityConfig.securityFilterChain(HttpSecurity http)`

**Objetivo:** Definir as regras de autorização HTTP da aplicação.

**Regras configuradas:**
- `/actuator/**` → livre, sem autenticação (para Docker healthcheck e Prometheus)
- Qualquer outra rota → exige JWT válido no header `Authorization`
- CSRF desabilitado (não aplicável em APIs REST stateless)
- Sessão: STATELESS (nunca cria HttpSession)
- Modo OAuth2 Resource Server com JWT

---

### `SecurityConfig.jwtDecoder()`

**Objetivo:** Registrar o componente que valida e decodifica tokens JWT.

**Como funciona:** Cria um `NimbusJwtDecoder` configurado com a chave pública RSA. Quando uma requisição chega com `Authorization: Bearer <token>`, o Spring usa este decoder para:
1. Decodificar o Base64 do token
2. Verificar a assinatura RSA com `app.key.pub`
3. Verificar se o token está expirado
4. Disponibilizar os claims via `@AuthenticationPrincipal Jwt`

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
- Quem verifica usa a **chave pública** — o `payments` tem

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
│  VALIDADOR (payments)                                           │
│                                                                 │
│  1. Cliente envia: Authorization: Bearer eyJ...                 │
│  2. SecurityFilterChain intercepta a requisição                 │
│  3. JwtDecoder extrai e verifica o token:                       │
│     a. Decodifica Base64                                        │
│     b. Verifica assinatura com app.key.pub                      │
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
  "status": "PENDING",
  "createdAt": "2026-06-09T21:00:00",
  "updatedAt": "2026-06-09T21:00:00"
}
```

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
  "status": "PENDING",
  "createdAt": "2026-06-09T21:00:00",
  "updatedAt": "2026-06-09T21:00:00"
}
```

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
    "status": "PENDING",
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

- Todo pagamento nasce com status `PENDING`
- O `userId` vem **exclusivamente** do JWT — nunca do body
- O `amount` deve ser maior que `0.01`
- O `method` deve ser um dos valores válidos do enum `PaymentMethod`
- Não há limite de pagamentos por usuário

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
SecurityFilterChain
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
  repository.save(payment)
  │
  ▼
PaymentRepository → PostgreSQL
  INSERT INTO payments (id, user_id, amount, method, status, created_at, updated_at)
  VALUES (gen_uuid, 'uuid-abc', 200.00, 'CREDIT_CARD', 'PENDING', now(), now())
  │
  ▼
PaymentService
  toResponse(savedPayment) → PaymentResponse
  │
  ▼
Cliente recebe: 201 Created
{ "id": "uuid-xyz", "status": "PENDING", ... }
```

---

### Fluxo 2 — Listagem de pagamentos

```
Cliente
  GET /payments/my
  Authorization: Bearer eyJ...
  │
  ▼
SecurityFilterChain → token válido
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
SecurityFilterChain → token de B é válido → userId = "uuid-B"
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
SecurityFilterChain → válido → userId = "uuid-abc"
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
SecurityFilterChain
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
- [x] Consulta de pagamento por ID (com verificação de ownership)
- [x] Listagem de pagamentos do usuário autenticado
- [x] Cancelamento de pagamento (apenas se `PENDING`)
- [x] Autenticação via JWT RS256
- [x] Extração segura do `userId` pelo claim `sub` do token
- [x] Tratamento centralizado de erros com respostas padronizadas
- [x] Validação de entrada com Bean Validation
- [x] Registro no Eureka
- [x] Rastreamento distribuído via OpenTelemetry → Jaeger
- [x] Métricas expostas para Prometheus via `/actuator/prometheus`
- [x] Health check em `/actuator/health`

### Funcionalidades pendentes

- [ ] Processamento de pagamento (simulação de gateway)
- [ ] Máquina de estado (transições `PENDING → APPROVED/FAILED`)
- [ ] Eventos Kafka (`payment.initiated`, `payment.approved`, `payment.failed`)
- [ ] Endpoint de webhook (`POST /payments/webhook`)
- [ ] Proteção JWT no Kong para as rotas de `/payments`
- [ ] Flyway para controle de migrações de banco
- [ ] Métricas customizadas com Micrometer (contadores por status)
- [ ] Paginação na listagem de pagamentos

### Limitações atuais

- Todo pagamento criado fica eternamente em `PENDING` — não há mecanismo de processamento implementado
- Nenhum evento é publicado no Kafka quando o status muda
- As rotas `/payments` no Kong não exigem JWT — a proteção existe apenas no nível da aplicação
- `ddl-auto: update` — adequado para desenvolvimento, inadequado para produção

### Débitos técnicos identificados

| Débito | Impacto | Solução recomendada |
|---|---|---|
| `ddl-auto: update` | Migrações silenciosas podem corromper dados em produção | Flyway com scripts `V1__...sql` |
| Sem paginação na listagem | Um usuário com muitos pagamentos pode sobrecarregar a memória | `Pageable` no repository + `Page<PaymentResponse>` no controller |
| Sem índice em `user_id` | Queries lentas com volume alto de dados | `@Index` na entidade ou migration Flyway |
| Rotas sem JWT no Kong | Qualquer cliente externo pode chamar sem token se passar direto pelo Kong | Adicionar plugin JWT no `kong.yaml` |
| `app.key.pub` no repositório | Má prática de segurança — chaves não deveriam estar no código | Secrets do Docker/Kubernetes ou variável de ambiente |

---

## 11. Próximos Passos

### Etapa 3 — Máquina de estado e simulação de gateway

Implementar a lógica que processa o pagamento após sua criação. O pagamento nasce `PENDING` e deve transitar para `APPROVED` ou `FAILED` com base em uma simulação de gateway.

**O que implementar:**
- Método de simulação no `PaymentService` que altera o status após a criação
- Validação das transições: `PENDING → APPROVED`, `PENDING → FAILED`, `APPROVED/FAILED → CANCELLED` é proibido
- O cancelamento já está implementado — só falta garantir que `APPROVED` e `FAILED` não possam ser cancelados (já está, pelo check de status no `cancel`)

**Onde mexer:** `PaymentService.java` — adicionar lógica de processamento no `create` ou em um método separado `process(UUID id)`.

---

### Etapa 4 — Eventos Kafka

Publicar eventos no Kafka quando o status de um pagamento muda, permitindo que outros serviços (como `store`) reajam de forma assíncrona.

**O que implementar:**
- `KafkaProducerConfig.java` em `config/`
- `PaymentEventProducer.java` em `service/` ou `infrastructure/`
- Criação dos tópicos: `payment.initiated`, `payment.approved`, `payment.failed`
- Publicação nos pontos de transição de status no `PaymentService`

**Dependência:** Kafka está rodando no Docker mas com `KAFKA_AUTO_CREATE_TOPICS_ENABLE: false` — os tópicos precisam ser criados explicitamente, seja via código (`NewTopic` bean) ou via CLI do Kafka.

---

### Etapa 5 — Kong e webhook

**Kong:** Atualizar `docker/kong/kong.yaml` para adicionar proteção JWT nas rotas de `/payments`, seguindo o padrão já existente para o `users-service`.

**Webhook:** Implementar `POST /payments/webhook` como rota pública para receber callbacks de um gateway externo simulado, com validação de um secret no header.

---

### Etapa 6 — Flyway e métricas customizadas

**Flyway:** Adicionar dependência `flyway-core` no `pom.xml`, criar `src/main/resources/db/migration/V1__create_payments_table.sql`, remover `ddl-auto: update`.

**Métricas:** Usar `MeterRegistry` do Micrometer no `PaymentService` para registrar contadores de pagamentos por status — visíveis no Grafana.

---

## 12. Guia para Evolução

### Onde adicionar novos endpoints

1. Crie o método no `PaymentService` com a lógica de negócio
2. Crie o método no `PaymentController` com a anotação HTTP adequada
3. O método do controller deve:
   - Extrair o `userId` do JWT via `UUID.fromString(jwt.getSubject())`
   - Delegar toda lógica ao service
   - Nunca conter `if`, validações de negócio ou acesso ao repository diretamente

**Exemplo de padrão a seguir:**
```java
// Controller — apenas recebe, extrai userId, delega
@GetMapping("/example")
public ExampleResponse example(@AuthenticationPrincipal Jwt jwt) {
    UUID userId = UUID.fromString(jwt.getSubject());
    return service.example(userId);
}

// Service — regra de negócio aqui
public ExampleResponse example(UUID userId) {
    // lógica, validações, acesso ao repository
}
```

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

### Padrões já utilizados que devem ser mantidos

| Padrão | Como está implementado | Por que manter |
|---|---|---|
| Injeção por construtor | `@RequiredArgsConstructor` + `final` | Imutabilidade, facilita testes |
| DTO de entrada e saída separados | `PaymentRequest` / `PaymentResponse` | Desacopla API da entidade interna |
| `record` para DTOs | `public record PaymentRequest(...)` | Imutável, sem boilerplate, idiomático no Java moderno |
| Extração de `userId` do JWT | `UUID.fromString(jwt.getSubject())` | Segurança — nunca confiar no body |
| `ResponseStatusException` | Lançada diretamente no service | Simples, sem necessidade de exceptions customizadas por ora |
| `toResponse()` privado no service | Método centralizado de conversão | Manutenção em um único lugar |

---

## 13. Resumo Final

O microsserviço `payments` é um **Resource Server OAuth2** que gerencia o ciclo de vida básico de pagamentos dentro da plataforma PB Mono Repo.

**Stack:** Java 25 + Spring Boot 3.5.14 + Spring Security + OAuth2 Resource Server + Spring Data JPA + PostgreSQL.

**Segurança:** Valida tokens JWT RS256 emitidos pelo microsserviço `users` usando a chave pública RSA. O `userId` é sempre extraído do campo `sub` do token — nunca aceito do corpo da requisição.

**Endpoints disponíveis:**

| Método | Rota | O que faz |
|---|---|---|
| `POST` | `/payments` | Cria pagamento com status `PENDING` |
| `GET` | `/payments/{id}` | Consulta pagamento (apenas o dono) |
| `GET` | `/payments/my` | Lista todos os pagamentos do usuário autenticado |
| `PATCH` | `/payments/{id}/cancel` | Cancela pagamento se estiver em `PENDING` |

**Regras centrais:**
- Todo pagamento nasce `PENDING`
- Apenas o dono do pagamento pode consultá-lo ou cancelá-lo
- Cancelamento só é permitido no status `PENDING`
- Erros retornam sempre JSON com `timestamp`, `status` e `message`

**O que ainda não está implementado:** processamento real (o pagamento nunca sai de `PENDING`), eventos Kafka, proteção JWT no Kong, Flyway para migrações.

**Onde mexer para cada tarefa:**

| Tarefa | Arquivo |
|---|---|
| Novo endpoint | `PaymentController` + `PaymentService` |
| Nova regra de negócio | `PaymentService` |
| Nova consulta ao banco | `PaymentRepository` |
| Nova validação de entrada | `PaymentRequest` |
| Novo tipo de erro tratado | `GlobalExceptionHandler` |
| Nova configuração de segurança | `SecurityConfig` |
| Nova configuração da aplicação | `application.yaml` |
