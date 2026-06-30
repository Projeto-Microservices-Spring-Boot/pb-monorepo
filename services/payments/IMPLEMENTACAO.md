# Resumo Executivo

**Status atual:** O Payments está funcional como CRUD básico com autenticação JWT. O fluxo HTTP completo funciona (criação, consulta, listagem, cancelamento). No entanto, nenhuma lógica de processamento de pagamento existe — todos os pagamentos ficam eternamente em `PENDING`. Kafka, testes e proteção JWT no Kong são zero.

**Percentual de conclusão:** ~35%

**Principais pendências:**
- Processamento de pagamento (máquina de estado / simulação de gateway)
- Kafka: produtor, tópicos, configuração
- Testes: zero cobertura de negócio
- Kong: rotas `/payments` sem JWT
- `application.yaml` sem `spring.kafka.bootstrap-servers`
- `docker-compose.yaml`: payments não depende do kafka

---

# Fase 1 — Correções Bloqueantes

**1. `spring.kafka.bootstrap-servers` ausente no `application.yaml`**
- O que falta: adicionar `spring.kafka.bootstrap-servers: kafka:29092` no `application.yaml`
- Arquivo: `services/payments/src/main/resources/application.yaml`
- Sem isso, qualquer tentativa de usar Kafka via `KafkaTemplate` lançará exceção de configuração em runtime.
- Ordem: 1º

**2. `depends_on: kafka` ausente no `docker-compose.yaml`**
- O que falta: adicionar `kafka: condition: service_healthy` no bloco `depends_on` do serviço `payments`
- Arquivo: `docker/docker-compose.yaml`
- Sem isso, o payments pode subir antes do Kafka estar pronto e falhar ao tentar produzir eventos.
- Ordem: 2º (junto com o item 1)

**3. `OpenAPIConfig` com URL hardcoded**
- O que falta: a URL `http://localhost:8085` no `OpenAPIConfig.java` não funciona quando o acesso é via Kong (`localhost:8000/payments`). O Swagger UI centralizado (`localhost:8089`) vai tentar fazer chamadas diretas na porta 8085 que não é acessível de fora do Docker.
- Arquivo: `services/payments/src/main/java/com/edu/infnet/pb/payments/config/OpenAPIConfig.java`
- Impacto: Swagger UI centralizado não funciona para payments.
- Ordem: 3º

**4. Ausência de Log4j2 configuration**
- O que falta: `log4j2.xml` ou `log4j2.yaml` em `src/main/resources/`
- O `pom.xml` exclui `spring-boot-starter-logging` e usa `spring-boot-starter-log4j2`, mas nenhum arquivo de configuração existe. O Spring Boot usa configuração padrão do Log4j2, o que pode gerar comportamento inconsistente com os outros serviços.
- Arquivo: ausente em `services/payments/src/main/resources/`
- Ordem: 4º (baixo impacto, não bloqueia startup)

---

# Fase 2 — Implementação do Fluxo de Negócio

**1. Simulação de processamento de gateway**
- Objetivo: após criação do pagamento, simular o processamento assíncrono (PENDING → APPROVED ou FAILED)
- Arquivos envolvidos: `PaymentService.java`, opcionalmente um `PaymentProcessor.java` separado
- Critério de conclusão: pagamento criado via `POST /payments` muda de status para `APPROVED` ou `FAILED` após processamento simulado; `PENDING` deixa de ser estado terminal

**2. Validação de transições de status**
- Objetivo: garantir que as transições sejam unidirecionais — não é possível aprovar um pagamento cancelado, por exemplo
- Arquivo: `PaymentService.java`
- Critério de conclusão: qualquer tentativa de mudar status de forma inválida retorna `422 Unprocessable Entity`; o método `cancel` já implementa isso para PENDING, mas a lógica de processamento ainda não existe

**3. Endpoint de webhook (`POST /payments/webhook`)**
- Objetivo: simular callback de gateway externo notificando resultado de pagamento
- Arquivos envolvidos: `PaymentController.java`, `PaymentService.java`, novo DTO `WebhookRequest.java`
- Critério de conclusão: `POST /payments/webhook` com payload `{paymentId, status, secret}` atualiza o status do pagamento correspondente; rota pública (sem JWT) mas validada por secret no header ou body; `GlobalExceptionHandler.java` cobre novos casos de erro

**4. Proteção JWT no Kong para rotas de Payments**
- Objetivo: `/payments` só deve ser acessível por usuários autenticados a nível de API Gateway, não apenas a nível de aplicação
- Arquivo: `docker/kong/kong.yaml`
- Critério de conclusão: rota `/payments` tem plugin JWT com `key_claim_name: iss` e `claims_to_verify: [exp]`; rota `/payments/webhook` permanece pública (sem plugin JWT); consumer `frontend` já existe e está configurado com a chave pública correta

**5. Paginação na listagem de pagamentos**
- Objetivo: `GET /payments/my` não pode retornar todos os registros sem limite
- Arquivos: `PaymentRepository.java`, `PaymentController.java`, `PaymentService.java`
- Critério de conclusão: endpoint aceita parâmetros `?page=0&size=10`, retorna `Page<PaymentResponse>` com metadados de paginação

---

# Fase 3 — Kafka e Integração entre Microserviços

## O que já existe
- Dependência `spring-kafka` declarada no `pom.xml` (linha 89–92)
- Kafka rodando no docker-compose na rede interna em `kafka:29092`
- Kafka UI em `localhost:8080`
- `KAFKA_AUTO_CREATE_TOPICS_ENABLE: false` (correto, exige criação explícita)

## O que falta

**Configuração:**
- `spring.kafka.bootstrap-servers: kafka:29092` — ausente em `application.yaml`
- `KafkaProducerConfig.java` — inexistente em `services/payments/src/main/java/com/edu/infnet/pb/payments/config/`
- Beans `NewTopic` para criação declarativa dos tópicos — inexistente

**Produtor:**
- Classe `PaymentEventProducer.java` — inexistente
- `KafkaTemplate<String, Object>` não instanciado
- Sem serialização JSON configurada (Jackson para Kafka)

**Tópicos necessários:**

| Tópico | Produzido por | Consumido por | Quando |
|---|---|---|---|
| `payment.initiated` | payments | adm-dashboard | Ao criar pagamento |
| `payment.approved` | payments | store, adm-dashboard | Status → APPROVED |
| `payment.failed` | payments | store, adm-dashboard | Status → FAILED |

**DTOs de evento:**
- Nenhum DTO de evento Kafka existe — precisa de classes como `PaymentInitiatedEvent`, `PaymentApprovedEvent`, `PaymentFailedEvent` em novo pacote `event/`

**O que precisa ser corrigido:**
- O docker-compose `payments` não tem `kafka` em `depends_on` — pagamento vai subir antes do Kafka e falhar
- `KAFKA_AUTO_CREATE_TOPICS_ENABLE: false`: os tópicos precisam ser criados via beans `NewTopic` no `KafkaProducerConfig.java` ou manualmente antes do start

## Fluxo esperado

```
POST /payments → PaymentService.create()
    → publica PaymentInitiatedEvent em payment.initiated
    → simula processamento de gateway
    → se aprovado: status = APPROVED → publica PaymentApprovedEvent em payment.approved
    → se falhou:   status = FAILED  → publica PaymentFailedEvent em payment.failed

POST /payments/webhook (callback externo)
    → recebe {paymentId, status}
    → PaymentService atualiza status
    → publica evento correspondente no Kafka

store (futuro, fora do escopo)
    → consome payment.approved → libera pedido
    → consome payment.failed  → reverte pedido
```

**Tratamento de erros Kafka (a implementar):**
- Retry: configurar `spring.kafka.producer.retries` no `application.yaml`
- Dead Letter Queue: não há DLQ configurada nem prevista; para o escopo atual (apenas producer), DLQ seria relevante apenas quando houver consumers
- Idempotência do producer: configurar `spring.kafka.producer.properties.enable.idempotence: true`

---

# Fase 4 — Persistência e Banco de Dados

## O que está correto
- Entidade `Payment.java` mapeada para tabela `payments`
- `@GeneratedValue(strategy = GenerationType.UUID)` — UUID gerado pelo Hibernate
- `@Enumerated(EnumType.STRING)` em `method` e `status` — correto, persistência por nome
- `@CreationTimestamp` / `@UpdateTimestamp` — gestão automática de timestamps
- `BigDecimal` com `precision = 19, scale = 2` — correto para valores monetários
- `userId` como UUID sem FK cruzada — correto para arquitetura de microserviços
- Banco `payments` criado pelo script `docker/postgres/01-init-databases.sh`
- `findByUserId(UUID userId)` no repository — query derivada correta

## O que está faltando
- **Índices**: sem `@Index` em `user_id` no `Payment.java` — queries `findByUserId` farão full scan em volume alto
- **Flyway**: sem migration versionada; usando `ddl-auto: update` que é inadequado para produção
- **Campo `orderId`**: a documentação menciona `orderId` como campo da entidade para integração com `store`, mas a entidade atual não tem esse campo
- **Campo `externalReference`**: mencionado na documentação como ID retornado pelo gateway, ausente na entidade atual
- **Campo `failureReason`**: mencionado na documentação para motivo de falha, ausente na entidade atual

## O que precisa ser ajustado
- `ddl-auto: update` → substituir por `ddl-auto: validate` (com Flyway) ou manter como `none` após migration
- Adicionar os campos ausentes (`orderId`, `externalReference`, `failureReason`) na entidade `Payment.java` e no `PaymentResponse.java` antes de criar a migration Flyway — se adicionados depois haverá migration adicional
- Adicionar `@Index` ou incluir índices na migration Flyway para `user_id` e futuramente `order_id`

---

# Fase 5 — Testes

## Testes Unitários

**`PaymentService` — cobertura obrigatória:**

| Cenário | Método |
|---|---|
| Criar pagamento com dados válidos → status PENDING | `create()` |
| Criar pagamento com userId extraído corretamente | `create()` |
| Buscar por ID existente e dono correto → retorna | `findById()` |
| Buscar por ID inexistente → 404 | `findById()` |
| Buscar por ID de outro usuário → 403 | `findById()` |
| Listar pagamentos de usuário com registros → retorna lista | `findByUser()` |
| Listar pagamentos de usuário sem registros → lista vazia | `findByUser()` |
| Cancelar pagamento PENDING do próprio usuário → CANCELLED | `cancel()` |
| Cancelar pagamento APPROVED → 422 | `cancel()` |
| Cancelar pagamento FAILED → 422 | `cancel()` |
| Cancelar pagamento CANCELLED → 422 | `cancel()` |
| Cancelar pagamento de outro usuário → 403 | `cancel()` |
| Cancelar pagamento inexistente → 404 | `cancel()` |

**Classe de teste:** `PaymentServiceTest.java` — usar Mockito para mockar `PaymentRepository`

**`GlobalExceptionHandler` — cobertura obrigatória:**

| Cenário |
|---|
| `ResponseStatusException` → JSON com status e message |
| `MethodArgumentNotValidException` → 400 com campo inválido |
| `Exception` genérica → 500 |

## Testes de Integração

**Com banco real (PostgreSQL via Testcontainers):**
- Classe: `PaymentRepositoryIT.java`
- Cenários: `save`, `findById`, `findByUserId` com dados reais no banco
- Dependência a adicionar no `pom.xml`: `testcontainers` + `testcontainers-postgresql`

**Com banco em memória (H2):**
- Opção mais simples para CI sem Docker
- Dependência a adicionar: `com.h2database:h2` em scope `test`
- Configuração: `application-test.yaml` com `url: jdbc:h2:mem:testdb` e `ddl-auto: create-drop`
- Cuidado: H2 não suporta `GenerationType.UUID` do PostgreSQL da mesma forma — pode exigir ajuste na entidade para testes

**Com Testcontainers (recomendado sobre H2 para fidelidade):**
- Arquivo: `services/payments/src/test/java/com/edu/infnet/pb/payments/PaymentsApplicationTests.java` — atualmente só faz `contextLoads()`, o que falha se PostgreSQL não estiver disponível

## Testes Kafka

**Validação de producers:**
- Usar `EmbeddedKafka` do `spring-kafka-test` (já disponível via `spring-boot-starter-test` que inclui `spring-kafka`)
- Anotação: `@EmbeddedKafka(partitions = 1, topics = {"payment.initiated", "payment.approved", "payment.failed"})`
- Validar que após `PaymentService.create()` uma mensagem foi publicada em `payment.initiated`
- Validar payload do evento (campos obrigatórios, tipos corretos)

**Validação de consumers (fase futura):**
- Não aplicável ao Payments no escopo atual — o Payments é apenas produtor

**Validação de processamento de eventos:**
- Ao implementar webhook, validar que `POST /payments/webhook` com status `APPROVED` dispara evento em `payment.approved`
- Usar `KafkaTestUtils.getRecords()` para ler mensagens do broker embarcado

**Validação de falhas e retries:**
- Simular falha do broker usando `EmbeddedKafka` pausado
- Validar que o producer realmente retria conforme configuração

## Testes End-to-End

**Fluxo completo a testar:**

```
1. POST /auth/login no users → obtém JWT
2. POST /payments com JWT → pagamento criado em PENDING
3. Simulação de gateway processa → status atualiza para APPROVED
4. Verificar evento publicado em payment.approved no Kafka
5. GET /payments/{id} → status é APPROVED
6. Tentar cancelar pagamento APPROVED → 422
```

**Pré-condição:** todos os containers rodando (postgres, kafka, users, payments)

**Ferramentas:** RestAssured ou MockMvc com `@SpringBootTest(webEnvironment = RANDOM_PORT)` + Testcontainers para infra

---

# Fase 6 — Validação Final

```
[ ] POST /payments retorna 201 com status PENDING
[ ] POST /payments sem JWT retorna 401
[ ] POST /payments com amount = 0 retorna 400
[ ] POST /payments com method inválido retorna 400
[ ] GET /payments/{id} retorna 200 para o dono
[ ] GET /payments/{id} retorna 403 para outro usuário
[ ] GET /payments/{id} retorna 404 para ID inexistente
[ ] GET /payments/my retorna lista do usuário autenticado
[ ] GET /payments/my retorna [] para usuário sem pagamentos
[ ] PATCH /payments/{id}/cancel retorna 200 com status CANCELLED
[ ] PATCH /payments/{id}/cancel em status APPROVED retorna 422
[ ] PATCH /payments/{id}/cancel de outro usuário retorna 403
[ ] Pagamento processado transita de PENDING para APPROVED ou FAILED
[ ] Evento publicado em payment.initiated ao criar pagamento
[ ] Evento publicado em payment.approved ao aprovar pagamento
[ ] Evento publicado em payment.failed ao reprovar pagamento
[ ] POST /payments/webhook atualiza status e publica evento
[ ] Rota /payments no Kong exige JWT (401 sem token pelo gateway)
[ ] Rota /payments/webhook no Kong é pública (sem JWT)
[ ] /actuator/health retorna UP
[ ] /actuator/prometheus retorna métricas
[ ] Serviço registrado no Eureka após startup
[ ] Traces visíveis no Jaeger após requisições
[ ] Tabela payments criada no banco com todas as colunas esperadas
[ ] Flyway migration executada sem erros (se implementado)
[ ] PaymentServiceTest cobre todos os cenários de negócio
[ ] Testes de integração com banco passando
[ ] Testes Kafka validando producers passando
[ ] contextLoads() passa sem banco/kafka disponíveis (usar mocks ou profiles de teste)
```
