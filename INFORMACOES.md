# PARTE 1 — Entendimento do Projeto e da Infraestrutura

---

## Arquitetura Geral

### Estrutura do Monorepo

```
pb-monorepo/
├── docker/                 → toda a infraestrutura (compose, Kong, Postgres, Prometheus, Grafana)
├── docs/                   → guias de arquitetura, Kafka, Kong, Eureka, Swagger
├── frontend/               → aplicação Next.js (React 19, TypeScript, Bun)
├── services/               → 8 microsserviços Java Spring Boot
│   ├── eureka-server       → servidor de descoberta (porta 8761)
│   ├── users               → autenticação e gestão de usuários (porta 8082)
│   ├── adm-dashboard       → painel administrativo (porta 8081)
│   ├── community           → funcionalidades de comunidade (porta 8083)
│   ├── geolocalization     → serviços de localização (porta 8084)
│   ├── payments            → pagamentos (porta 8085) — esqueleto
│   ├── stickers            → gestão de stickers (porta 8087)
│   └── store               → gestão de loja (porta 8088)
└── postman/                → coleções de testes de API
```

### Responsabilidade de Cada Microsserviço

| Serviço | Responsabilidade | Estado |
|---|---|---|
| `eureka-server` | Registro e descoberta de serviços | Completo |
| `users` | Autenticação (register/login), geração de JWT RS256 | Parcialmente implementado |
| `adm-dashboard` | Painel administrativo do sistema | Esqueleto |
| `community` | Funcionalidades sociais/comunidade | Esqueleto |
| `geolocalization` | Serviços de geolocalização | Esqueleto |
| `payments` | Processamento de pagamentos | Esqueleto mínimo |
| `stickers` | Gerenciamento de stickers | Esqueleto |
| `store` | Gerenciamento de loja/produtos | Esqueleto |

### Como os Microsserviços se Comunicam

Existem **duas camadas de comunicação** previstas na arquitetura:

1. **Síncrona via Kong (HTTP):** o frontend chama `localhost:8000` → Kong roteia para o microsserviço correto via nome DNS interno do Docker (ex: `http://payments:8085`). O microsserviço também pode chamar outro microsserviço diretamente via nome DNS interno, embora isso não esteja implementado ainda.

2. **Assíncrona via Kafka:** previsto para comunicação orientada a eventos entre serviços (ex: `payments` publica evento `payment.confirmed` → `store` consome e atualiza estoque). A dependência `spring-kafka` está no `pom.xml` de todos os serviços, mas nenhuma lógica de produtor/consumidor foi implementada.

### Fluxo Completo de uma Requisição

```
Frontend (localhost:3000)
    ↓ HTTP
Kong API Gateway (localhost:8000)
    ↓ Verifica rota e plugins (JWT se rota privada, CORS, Prometheus)
    ↓ Encaminha para o microsserviço correto (via rede Docker interna)
Microsserviço (ex: payments:8085)
    ↓ Processa requisição
    ↓ Persiste no PostgreSQL (payments DB)
    ↓ (futuro) Publica evento no Kafka
    ↓ Retorna resposta
Kong
    ↓ Repassa resposta ao frontend
```

Paralelamente, a cada requisição:
- OpenTelemetry envia trace ao **Jaeger** (porta 4318)
- `/actuator/prometheus` é raspado pelo **Prometheus** a cada 5 segundos

---

## Infraestrutura

### Eureka

**Problema que resolve:** em ambientes com múltiplos microsserviços e escalabilidade dinâmica, endereços IP e portas mudam. O Eureka elimina a necessidade de endereços hard-coded: cada serviço se registra com seu nome e o sistema descobre seus endereços em tempo de execução.

**Como está sendo utilizado:** está rodando como servidor standalone (`register-with-eureka: false`, `fetch-registry: false`). Todos os 7 outros microsserviços se registram nele usando `instance-id: ${spring.application.name}:${random.value}`, o que garante instâncias únicas mesmo com múltiplos pods.

**Conexão com outros componentes:** os microsserviços apontam para `http://eureka:8761/eureka/`. O Prometheus coleta métricas do Eureka via `/actuator/prometheus`. Na prática atual, como Kong usa URLs fixas de DNS Docker (ex: `http://payments:8085`), o Eureka está presente mas pouco explorado para roteamento efetivo.

---

### Kong

**Problema que resolve:** centraliza o ponto de entrada da API, eliminando a necessidade de o frontend conhecer endereços individuais de cada microsserviço. Oferece autenticação, CORS, logging e métricas num único lugar.

**Como está sendo utilizado:** modo DB-less com configuração declarativa em `kong.yaml`. Define 7 serviços com suas rotas. Apenas o `users-service` tem diferenciação entre rota pública (`/users/public`, sem JWT) e privada (`/users/private`, com plugin JWT RS256). Os demais serviços, incluindo `payments`, não têm proteção por JWT.

**Conexão com outros componentes:** Kong recebe requisições do frontend (porta 8000), encaminha para os microsserviços via rede Docker interna, expõe métricas para o Prometheus na porta 8001 (`/metrics`), e valida JWTs usando a chave pública RSA configurada no `consumers` block.

---

### Kafka

**Problema que resolve:** desacopla serviços que precisam se comunicar sem espera de resposta imediata. Garante que eventos não se percam mesmo se o consumidor estiver temporariamente indisponível.

**Como está sendo utilizado:** Kafka 7.8.0 em modo KRaft (sem Zookeeper), single-node, com `KAFKA_AUTO_CREATE_TOPICS_ENABLE: false` (configuração adequada para produção). Kafka UI (kafbat) disponível na porta 8080 para visualização. A dependência `spring-kafka` está no `pom.xml` de todos os serviços, mas **nenhum produtor ou consumidor foi implementado** em qualquer microsserviço. Kafka está completamente ocioso no estado atual.

**Conexão com outros componentes:** microsserviços se conectariam via `kafka:29092` (rede interna Docker). O Kafka UI aponta para `kafka:29092`.

---

### PostgreSQL

**Problema que resolve:** persistência de dados relacionais com isolamento por serviço (database-per-service pattern).

**Como está sendo utilizado:** uma única instância PostgreSQL 18.3 hospeda 8 bancos de dados separados — um por microsserviço. A inicialização é feita pelo script `01-init-databases.sh`, que cria os bancos via variável de ambiente `POSTGRES_EXTRA_DATABASES`. Cada serviço aponta para seu próprio banco (ex: `jdbc:postgresql://pg:5432/payments`). O serviço `users` possui seeds com 12 usuários de teste. Os demais bancos estão vazios.

**Conexão com outros componentes:** todos os microsserviços dependem do healthcheck do PostgreSQL antes de iniciar (`condition: service_healthy`).

---

### Redis

**Problema que resolve:** cache de alta performance, armazenamento de sessões, filas simples.

**Como está sendo utilizado:** Redis 7.4 está rodando com healthcheck configurado e volume persistente. **Porém nenhum microsserviço tem a dependência `spring-data-redis` no `pom.xml`, e nenhum código de cache foi implementado.** Redis está configurado e rodando, mas completamente não utilizado pela aplicação.

**Conexão com outros componentes:** nenhuma conexão ativa no momento.

---

### Prometheus

**Problema que resolve:** coleta e armazena séries temporais de métricas dos serviços para análise de performance, alertas e dashboards.

**Como está sendo utilizado:** Prometheus v3.11.3 com scrape interval de 5 segundos. Coleta métricas de 9 targets: Kong (`/metrics` na porta 8001) e 8 microsserviços (`/actuator/prometheus`). Todos os serviços expõem o endpoint prometheus via `management.endpoints.web.exposure.include: [health, prometheus, info]`.

**Conexão com outros componentes:** Prometheus puxa dados dos microsserviços via rede Docker. Grafana consome os dados do Prometheus como fonte de dados.

---

### Grafana

**Problema que resolve:** visualização de métricas do Prometheus em dashboards interativos.

**Como está sendo utilizado:** Grafana 11.5.10 acessível na porta 3002 (mapeada internamente para 3000). Credenciais admin/admin. Volume `grafana_data` para persistência. O arquivo `grafana.ini` existe mas **nenhum dashboard foi configurado** — o datasource do Prometheus precisa ser adicionado manualmente pela UI ou via arquivo de provisionamento.

**Conexão com outros componentes:** Grafana consultaria o Prometheus em `http://prometheus:9090`.

---

### Jaeger

**Problema que resolve:** rastreamento distribuído de requisições que atravessam múltiplos microsserviços, permitindo visualizar latência e gargalos end-to-end.

**Como está sendo utilizado:** Jaeger all-in-one 1.57 com OTLP habilitado. Todos os microsserviços enviam traces via HTTP OTLP para `http://jaeger:4318`. O `management.tracing.sampling.probability: 1.0` garante que 100% das requisições são rastreadas. UI acessível em `localhost:16686`. O padrão de log `[app,traceId,spanId]` permite correlacionar logs com traces.

**Conexão com outros componentes:** microsserviços → Jaeger (push via OTLP HTTP). Independente dos demais componentes.

---

### Toxiproxy

**Problema que resolve:** simula condições adversas de rede (latência, perda de pacotes, conexões lentas, timeouts) para testar resiliência dos serviços.

**Como está sendo utilizado:** a imagem do Shopify está rodando com a API de controle na porta 8474. **Nenhum proxy foi configurado** e nenhum teste de resiliência está definido. O Circuit Breaker (Resilience4j) está no `pom.xml` de todos os serviços, mas sem configuração específica (sem `application.yaml` com `resilience4j.*`). A combinação Toxiproxy + Resilience4j sugere intenção de testar resiliência, mas nenhum dos dois está efetivamente em uso.

**Conexão com outros componentes:** nenhuma conexão ativa. Funcionaria como proxy entre microsserviços para injetar falhas.

---

### Docker Compose

**Problema que resolve:** orquestra todos os containers do ambiente de desenvolvimento local com uma única configuração, garantindo ordem de inicialização, rede compartilhada e volumes persistentes.

**Como está sendo utilizado:** arquivo único em `docker/docker-compose.yaml` com 16 serviços. Usa healthchecks para garantir ordem de inicialização (microsserviços só sobem após PostgreSQL e Kong estarem saudáveis). Define 4 volumes nomeados (`pg_data`, `redis_data`, `kafka_data`, `grafana_data`). Todos os microsserviços são buildados localmente via Dockerfile multi-stage.

---

## Validação da Infraestrutura

### O que está funcionando corretamente

- **Estrutura Docker Compose** bem organizada, com healthchecks e `depends_on` corretos
- **PostgreSQL** com inicialização automática de múltiplos bancos
- **Eureka Server** configurado corretamente como servidor standalone
- **Kong DB-less** com roteamento declarativo funcionando
- **Jaeger + OpenTelemetry** integrado em todos os serviços de forma consistente
- **Prometheus** com scrape de todos os 9 targets definidos
- **Kafka KRaft** configurado sem Zookeeper (moderno e correto)
- **Padrão de log** com correlação de traceId/spanId em todos os serviços
- **JWT RS256** com chave assimétrica (mais seguro que HS256)
- **Dockerfile multi-stage** em todos os serviços (builds otimizados)

### O que parece incompleto

- **Redis** rodando mas sem nenhuma integração nos microsserviços
- **Kafka** rodando mas sem nenhum tópico, produtor ou consumidor implementado
- **Toxiproxy** sem proxies configurados
- **Grafana** sem datasource e sem dashboards configurados
- **Resilience4j** como dependência sem configuração de circuit breaker em nenhum serviço
- **Payments, community, geolocalization, stickers, store, adm-dashboard** são apenas esqueletos sem lógica de negócio
- **Kong** sem JWT em rotas de `payments`, `store`, `community` — todas acessíveis sem autenticação
- **Orval** configurado no frontend para geração de cliente OpenAPI, mas nenhuma spec OpenAPI (`openapi.yaml` ou Swagger) existe nos serviços

### O que parece configurado mas não utilizado

- **Redis** — container rodando, sem uso
- **Toxiproxy** — container rodando, sem configuração
- **`secret: segredo-super-secreto`** no Kong (desnecessário para RS256, e está em plain text no repositório)
- **`POSTGRES_EXTRA_DATABASES`** — variável de ambiente customizada que só funciona porque o script `01-init-databases.sh` foi criado para lê-la (não é padrão do Postgres)
- **Porta 14268 do Jaeger** (HTTP legado) — exportada, mas os serviços usam OTLP (4318), tornando a 14268 desnecessária

### Possíveis Problemas Arquiteturais

1. **`ddl-auto: update` em todos os serviços** — perigoso em produção. Pode causar perda de dados ou migrações silenciosas incorretas. O correto seria usar Flyway ou Liquibase para migrações versionadas.

2. **Banco de dados compartilhado (instância única):** o padrão database-per-service está correto em termos de bancos separados, mas usar uma única instância PostgreSQL cria acoplamento de infraestrutura — uma falha derruba todos os serviços.

3. **Ausência de autenticação nas rotas de payments:** qualquer cliente pode chamar `/payments` sem JWT. Para um serviço de pagamentos, isso é um problema crítico de segurança.

4. **Chave privada RSA no classpath** (`app.key` em `src/main/resources`) — está dentro do repositório, o que é uma má prática de segurança para produção.

5. **Eureka pouco integrado ao Kong:** Kong usa URLs fixas de DNS Docker, não descoberta dinâmica via Eureka. Se houver escalonamento horizontal, Kong não seria capaz de balancear carga automaticamente com a configuração atual.

### Possíveis Problemas de Observabilidade

1. **Grafana sem datasource configurado automaticamente** — nenhum arquivo de provisionamento (`/etc/grafana/provisioning/datasources/`) foi definido, exigindo configuração manual a cada novo ambiente.
2. **Nenhum dashboard criado** — Prometheus coleta dados mas não há forma de visualizá-los sem criar dashboards do zero.
3. **Sem alertas configurados** no Prometheus (nenhum `alerting_rules.yml`).
4. **Logs não centralizados** — cada container loga no stdout mas não há stack de log centralizada (ex: Loki + Grafana).

### Possíveis Problemas de Comunicação Entre Serviços

1. **Sem Kafka topics definidos** — `KAFKA_AUTO_CREATE_TOPICS_ENABLE: false` é correto para produção mas exige criação explícita dos tópicos, que não existe.
2. **Sem comunicação inter-serviço implementada** — nenhum serviço chama outro via RestTemplate, WebClient ou Kafka.
3. **Sem rate limiting no Kong** — o próprio `kong.yaml` tem um `TODO` para isso.
4. **Microsserviços dependem apenas de Kong e PostgreSQL**, mas não do Kafka nem do Eureka no healthcheck do Docker Compose — se Kafka estiver indisponível, o serviço vai subir mas falhar ao tentar publicar eventos.

### Possíveis Melhorias

- Adicionar Flyway/Liquibase para controle de migrações
- Configurar provisioning automático do Grafana (datasource + dashboards via arquivos YAML)
- Adicionar rate limiting no Kong para as rotas críticas
- Criar arquivo `.env` ou usar secrets para credenciais (não hard-coded no compose)
- Adicionar Loki para centralização de logs (stack PLG: Prometheus + Loki + Grafana)
- Configurar `spring.kafka.bootstrap-servers` explicitamente no `application.yaml` de cada serviço
- Adicionar JWT ao Kong nas rotas de `payments` (rota privada)

---

## Microsserviço Payments — Estado Atual

### Estrutura de Pastas

```
services/payments/
├── src/
│   ├── main/
│   │   ├── java/com/edu/infnet/pb/payments/
│   │   │   └── PaymentsApplication.java       ← único arquivo Java existente
│   │   └── resources/
│   │       └── application.yaml               ← configuração completa
│   └── test/
│       └── java/com/edu/infnet/pb/payments/
│           └── PaymentsApplicationTests.java  ← teste de contexto vazio
├── Dockerfile                                 ← multi-stage build pronto
├── pom.xml                                    ← dependências completas
└── mvnw                                       ← Maven wrapper
```

### Controllers
Nenhum controller existe. Não há endpoints implementados.

### Services
Nenhum service existe. Não há lógica de negócio implementada.

### DTOs
Nenhum DTO existe.

### Entities
Nenhuma entity JPA existe. O banco `payments` foi criado no PostgreSQL mas está completamente vazio — nenhuma tabela foi criada pelo Hibernate (pois não há entidades).

### Repositories
Nenhum repository existe.

### Configurações (`application.yaml`)

| Configuração | Valor |
|---|---|
| Nome da aplicação | `payments` |
| Porta | `8085` |
| Banco de dados | `jdbc:postgresql://pg:5432/payments` |
| Credenciais | `admin / admin` |
| DDL Auto | `update` (problemático) |
| Show SQL | `true` (dev) |
| Eureka | registra em `http://eureka:8761/eureka/` |
| Tracing | 100% sampling → Jaeger `http://jaeger:4318` |
| Actuator | expõe `health`, `prometheus`, `info` |
| Log | com correlação `[payments,traceId,spanId]` |

### Integrações (declaradas no pom.xml, não implementadas)

| Dependência | Status |
|---|---|
| `spring-boot-starter-web` | Disponível, sem controllers |
| `spring-boot-starter-data-jpa` | Disponível, sem entities |
| `spring-kafka` | Disponível, sem producer/consumer |
| `spring-cloud-starter-netflix-eureka-client` | Funcional — registra no Eureka |
| `spring-cloud-starter-circuitbreaker-resilience4j` | Disponível, sem configuração |
| `micrometer-registry-prometheus` | Funcional — expõe `/actuator/prometheus` |
| `opentelemetry-spring-boot-starter` | Funcional — envia traces ao Jaeger |
| `lombok` | Disponível |
| `postgresql` | Disponível, banco criado |

### Endpoints
Nenhum endpoint implementado. O serviço sobe, registra-se no Eureka, expõe `/actuator/health` e `/actuator/prometheus`, mas não responde a nenhuma requisição de negócio.

### Eventos
Nenhum evento Kafka definido.

---

# PARTE 2 — Como Transformar o Payments em um Microsserviço de Pagamento Completo

---

## O que deve existir obrigatoriamente

### Entidades

**`Payment`** — entidade central, representa uma transação de pagamento.

| Campo | Tipo | Descrição |
|---|---|---|
| `id` | UUID | PK gerada automaticamente |
| `userId` | UUID | ID do usuário que paga (vem do JWT) |
| `orderId` | UUID | ID do pedido associado (referência ao `store`) |
| `amount` | BigDecimal | Valor do pagamento (nunca `double` para dinheiro) |
| `currency` | String | `BRL` fixo (ou enum) |
| `status` | Enum | `PENDING`, `PROCESSING`, `CONFIRMED`, `FAILED`, `CANCELLED` |
| `method` | Enum | `CREDIT_CARD`, `PIX`, `BOLETO` |
| `externalReference` | String | ID retornado pelo "gateway" simulado |
| `failureReason` | String | Motivo da falha, se houver |
| `createdAt` | LocalDateTime | Data de criação |
| `updatedAt` | LocalDateTime | Última atualização |

**`PaymentMethod`** (opcional, fase 2) — dados de meio de pagamento salvos pelo usuário.

### Endpoints

```
POST   /payments             → Iniciar um pagamento (autenticado)
GET    /payments/{id}        → Consultar status de um pagamento (autenticado)
GET    /payments/my          → Listar pagamentos do usuário autenticado (autenticado)
PATCH  /payments/{id}/cancel → Cancelar um pagamento PENDING (autenticado)
POST   /payments/webhook     → Receber callback do gateway simulado (público, com secret)
```

O endpoint `webhook` simula o callback de um gateway real (ex: Stripe, PagSeguro) notificando que o pagamento foi processado.

### Regras de Negócio

1. **Apenas o dono do pagamento pode consultá-lo ou cancelá-lo** — o `userId` é extraído do JWT, nunca do request body.
2. **Pagamento só pode ser cancelado se estiver em `PENDING`** — qualquer outro status retorna erro.
3. **`amount` deve ser maior que zero** — validação no DTO.
4. **Transição de status é unidirecional:** `PENDING → PROCESSING → CONFIRMED` ou `PENDING → PROCESSING → FAILED`. Não existe reversão de `CONFIRMED`.
5. **O processamento real é assíncrono** — após criar o pagamento, o serviço simula o envio para um gateway e publica um evento no Kafka quando recebe a confirmação.

### Persistência

- Tabela `payments` gerenciada pelo Hibernate (com Flyway para migrações)
- Índices em `userId` e `orderId` para queries frequentes
- `BigDecimal` mapeado como `NUMERIC(19,2)` no banco

### Eventos Kafka

| Tópico | Produtor | Consumidor | Descrição |
|---|---|---|---|
| `payment.initiated` | `payments` | `adm-dashboard` | Pagamento criado |
| `payment.confirmed` | `payments` | `store`, `adm-dashboard` | Pagamento aprovado → liberar pedido |
| `payment.failed` | `payments` | `store`, `adm-dashboard` | Pagamento falhou → reverter pedido |

O `store` consumindo `payment.confirmed` é o caso de uso mais importante e demonstra o valor do Kafka — desacoplamento assíncrono entre dois domínios distintos.

### Tratamento de Erros

Usar um `@ControllerAdvice` global com respostas padronizadas:

| Cenário | HTTP Status |
|---|---|
| Pagamento não encontrado | `404 Not Found` |
| Usuário não é dono do pagamento | `403 Forbidden` |
| Cancelamento de pagamento em status inválido | `422 Unprocessable Entity` |
| Dados inválidos no request | `400 Bad Request` |
| Erro interno inesperado | `500 Internal Server Error` |

### Observabilidade

- **Métricas customizadas** com Micrometer: contador de pagamentos por status, histograma de valor de pagamento, taxa de falhas
- **Traces** automáticos via OpenTelemetry já configurado — cada endpoint aparece no Jaeger
- **Logs estruturados** com `traceId` e `spanId` já configurados no `application.yaml`
- **`/actuator/health`** já exposto — adicionar health check customizado para verificar conectividade com Kafka

---

## O que NÃO vale a pena implementar

| Funcionalidade | Por quê não |
|---|---|
| Integração com gateway real (Stripe, PagSeguro, Mercado Pago) | Exige conta, keys de API, certificados — complexidade operacional desnecessária para portfólio |
| PCI DSS e tokenização de cartão | Compliance de nível enterprise, fora do escopo educacional |
| Estorno / chargeback | Lógica financeira complexa, requer casos de uso adicionais |
| Multi-currency | Conversão de moedas com taxas dinâmicas foge do escopo |
| Retry automático de pagamentos falhos | Requer scheduler, backoff exponencial, lógica de idempotência robusta |
| Fraud detection | Requer ML ou integração com serviço externo |
| Relatórios financeiros / reconciliação | Domínio contábil separado |
| Pagamentos recorrentes (assinatura) | Estado e lógica de ciclo de vida muito mais complexos |
| Split de pagamento | Requer múltiplos beneficiários, lógica de distribuição |
| Refresh token no JWT | Já está previsto no `users` service, não é responsabilidade do payments |

---

## Roadmap

### Etapa 1 — Base do Domínio

**Objetivo:** criar as entidades, repositório, DTOs e a estrutura Clean Architecture sem nenhuma lógica de negócio ainda.

**O que fazer:** criar `Payment.java` (entity), `PaymentStatus.java` (enum), `PaymentMethod.java` (enum), `PaymentRepository.java`, `CreatePaymentRequestDto.java`, `PaymentResponseDto.java`, e a estrutura de pacotes `domain/`, `application/`, `infrastructure/`, `presentation/`.

**Benefício:** fundação sólida e alinhada com o padrão já estabelecido no `users` service. O banco `payments` cria as tabelas automaticamente ao subir.

**Complexidade:** baixa.

**Dependências:** nenhuma.

---

### Etapa 2 — CRUD Básico com Autenticação

**Objetivo:** implementar `POST /payments` e `GET /payments/{id}` com extração do `userId` do JWT.

**O que fazer:** criar `PaymentController.java`, `PaymentService.java` com lógica de criação, configurar `SecurityConfig.java` para aceitar JWT (OAuth2 resource server, igual ao `users`), extrair `userId` do `SecurityContext`, validar `@RequestBody` com Bean Validation.

**Benefício:** serviço funcional com primeiro endpoint testável via Postman. Demonstra autenticação correta via JWT RS256.

**Complexidade:** média — requer entender o fluxo de JWT já implementado no `users`.

**Dependências:** Etapa 1 completa; serviço `users` rodando para gerar tokens válidos.

---

### Etapa 3 — Máquina de Estado e Simulação de Gateway

**Objetivo:** implementar o ciclo de vida completo do pagamento com transições de status.

**O que fazer:** criar `PaymentStateMachine` (ou método de validação de transição), implementar o processamento simulado (método que retorna `CONFIRMED` ou `FAILED` com probabilidade configurável), implementar `PATCH /payments/{id}/cancel`, implementar `GET /payments/my`.

**Benefício:** serviço com regras de negócio reais. Interessante de explicar em entrevista — máquina de estado é um padrão clássico.

**Complexidade:** média.

**Dependências:** Etapas 1 e 2 completas.

---

### Etapa 4 — Eventos Kafka

**Objetivo:** publicar eventos no Kafka quando o status do pagamento muda.

**O que fazer:** criar `PaymentEventProducer.java`, configurar `KafkaProducerConfig.java`, criar os tópicos `payment.initiated`, `payment.confirmed`, `payment.failed` manualmente (ou via `NewTopic` bean), publicar eventos nas transições de status.

**Benefício:** demonstra comunicação assíncrona entre microsserviços — um dos diferenciais mais valorizados do projeto para portfólio e entrevistas.

**Complexidade:** média — Kafka está configurado na infra, mas a integração no código requer atenção à serialização (JSON via Jackson) e configuração do producer.

**Dependências:** Etapas 1–3 completas; Kafka rodando.

---

### Etapa 5 — Proteção no Kong e Webhook

**Objetivo:** adicionar JWT ao Kong para as rotas de payments e implementar o endpoint de webhook.

**O que fazer:** atualizar `kong.yaml` para adicionar rota pública (`/payments/webhook`) e rota privada (`/payments` com plugin JWT), implementar `POST /payments/webhook` com validação de secret header.

**Benefício:** segurança real na API gateway, simulação completa do fluxo de gateway de pagamento.

**Complexidade:** baixa (Kong) a média (webhook com validação de assinatura).

**Dependências:** Etapas 1–4 completas.

---

### Etapa 6 — Observabilidade Customizada e Migração Flyway

**Objetivo:** adicionar métricas customizadas com Micrometer e substituir `ddl-auto: update` por Flyway.

**O que fazer:** adicionar dependência `flyway-core`, criar `V1__create_payments_table.sql`, adicionar `MeterRegistry` no service para contadores de status, criar health indicator customizado para Kafka.

**Benefício:** profissionalismo técnico real. `ddl-auto: update` é considerado má prática e Flyway aparece muito em entrevistas. Métricas customizadas enriquecem o Grafana.

**Complexidade:** baixa a média.

**Dependências:** todas as etapas anteriores.

---

### Visão Geral do Roadmap

```
Etapa 1 → Entidades e estrutura
Etapa 2 → Endpoints básicos + JWT
Etapa 3 → Máquina de estado + simulação
Etapa 4 → Eventos Kafka
Etapa 5 → Kong protegido + webhook
Etapa 6 → Flyway + métricas customizadas
```

Cada etapa entrega um serviço funcional e demonstrável, incrementando complexidade de forma controlada. O resultado final é um microsserviço de pagamentos simples, com autenticação, persistência, eventos assíncronos, observabilidade e segurança — exatamente o nível esperado para um portfólio técnico bem avaliado em entrevistas.
