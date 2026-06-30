# PB Monorepo — Microservices Spring Boot

## Integrantes

- Nathan Rodrigues Vieira
- Natan de Andrade Robers
- Marcos Isaac Santana do Carmo
- Isabella Mayer Rocha e Silva
- Eduardo de Assis Araujo
- Lucas Silva de Souza

## Descrição do Projeto

Monorepo de uma plataforma de colecionáveis digitais (figurinhas) com microserviços Java/Spring Boot, API Gateway Kong, frontend React, comunicação assíncrona via Kafka e observabilidade com Prometheus/Grafana/Jaeger.

## Decisões de Arquitetura & Trade-offs

## Estrutura de pastas

```text
pb-monorepo/
├── .github/                        # Configurações do GitHub e CI/CD
│   ├── workflows/                  # Pipelines de CI (build, test, deploy)
│   └── CODEOWNERS
├── docker/                         # Infraestrutura local
│   ├── kong/
│   │   └── kong.yaml               # Rotas e plugins do API Gateway (DB-less)
│   ├── postgres/
│   │   └── init-databases.sh       # Criação automática dos bancos
│   ├── prometheus/
│   │   └── prometheus.yaml         # Configuração de métricas
│   └── docker-compose.yaml         # Orquestração de todos os containers
├── docs/                           # Documentações do projeto
│   ├── clean-architecture/
│   ├── eureka/
│   ├── kafka/
│   ├── kong/
│   ├── semantic-commits/
│   ├── swagger/
│   ├── toxiproxy/
│   └── postman/                    # Postman collection (JSON v2.1)
├── frontend/                       # Aplicação React
├── postman/                        # Collection versionada (YAML por endpoint)
│   ├── collections/
│   │   └── PB Microservices/
│   │       ├── API Gateway/        # Endpoints via Kong (porta 8000)
│   │       └── Microserviços isolados/  # Endpoints diretos
│   └── environments/
├── services/                       # Microserviços Java
│   ├── adm-dashboard/              # Dashboard administrativo
│   ├── community/                  # Fórum/posts e comentários
│   ├── eureka-server/              # Service discovery (Netflix Eureka)
│   ├── geolocalization/            # Geolocalização
│   ├── payments/                   # Processamento de pagamentos
│   ├── shared/                     # Módulo compartilhado (security, jwt, utils)
│   ├── stickers/                   # Catálogo e coleção de figurinhas
│   ├── store/                      # Loja / marketplace
│   └── users/                      # Autenticação e perfil de usuários
├── pom.xml                         # POM pai do monorepo (opcional)
├── lefthook.yml                    # Hooks de Git (frontend apenas)
└── README.md
```

## Microservices

Todos os microservices são dockerizados com `Dockerfile` próprio e orquestrados pelo `docker-compose.yaml`. Cada serviço expõe uma porta e possui seu próprio banco PostgreSQL.

| Serviço         | Porta | Banco           | Descrição                                                                                                   |
| --------------- | ----- | --------------- | ----------------------------------------------------------------------------------------------------------- |
| adm-dashboard   | 8081  | adm_dashboard   | Painel administrativo (em desenvolvimento)                                                                  |
| users           | 8082  | auth            | Cadastro, login, logout e perfil de usuários. Suporta roles USER e SELLER                                   |
| community       | 8083  | community       | Fórum com posts e comentários. Posts criados por ADMIN/SELLER; comentários por qualquer usuário autenticado |
| geolocalization | 8084  | geolocalization | Consulta de dados geográficos (em desenvolvimento)                                                          |
| payments        | 8085  | payments        | Processamento de pagamentos com eventos assíncronos via Kafka (PENDING → APPROVED/FAILED)                   |
| stickers        | 8087  | stickers        | Catálogo de figurinhas e gerenciamento da coleção pessoal do usuário + match de trocas                      |
| store           | 8088  | store           | Loja / marketplace (em desenvolvimento)                                                                     |

### Discovery Server

O Eureka Server permite que os microserviços se descubram dinamicamente sem endereços fixos.

| Nome          | Responsabilidade  | Porta |
| ------------- | ----------------- | ----- |
| Eureka Server | Service Discovery | 8761  |

### API Gateway

O Kong API Gateway centraliza o acesso aos microservices. Sobe em modo DB-less com configuração declarativa em `docker/kong/kong.yaml`. Interface administrativa em <http://localhost:8002>, Admin API em <http://localhost:8001> e proxy HTTP em <http://localhost:8000>.

O Kong gerencia autenticação via plugin JWT (RS256). Rotas públicas não exigem token; rotas privadas exigem JWT válido. O consumer `frontend` possui a chave pública para validação.

#### Endpoints — API Gateway (`http://localhost:8000`)

| Serviço       | Método | Endpoint                                                | Auth   | Descrição                                     |
| ------------- | ------ | ------------------------------------------------------- | ------ | --------------------------------------------- |
| **Users**     | POST   | `/auth/register`                                        | ❌     | Registro de novo usuário                      |
| **Users**     | POST   | `/auth/register/seller`                                 | ❌     | Registro de novo vendedor                     |
| **Users**     | POST   | `/auth/login`                                           | ❌     | Login, retorna JWT + refresh_token            |
| **Users**     | POST   | `/auth/logout`                                          | Bearer | Invalida refresh_token                        |
| **Users**     | GET    | `/me`                                                   | Bearer | Perfil do usuário logado                      |
| **Community** | POST   | `/posts`                                                | Bearer | Criar post (ADMIN/SELLER)                     |
| **Community** | PUT    | `/posts/{postId}`                                       | Bearer | Atualizar post                                |
| **Community** | DELETE | `/posts/{postId}`                                       | Bearer | Deletar post                                  |
| **Community** | GET    | `/posts`                                                | ❌     | Feed paginado de posts                        |
| **Community** | GET    | `/posts/{postId}`                                       | ❌     | Post com comentários                          |
| **Community** | POST   | `/comments`                                             | Bearer | Criar comentário                              |
| **Community** | DELETE | `/comments/{commentId}`                                 | Bearer | Deletar comentário                            |
| **Payments**  | POST   | `/payments`                                             | Bearer | Criar pagamento                               |
| **Payments**  | GET    | `/payments/{id}`                                        | Bearer | Buscar pagamento por ID                       |
| **Payments**  | GET    | `/payments/my`                                          | Bearer | Listar pagamentos do usuário                  |
| **Payments**  | PATCH  | `/payments/{id}/cancel`                                 | Bearer | Cancelar pagamento                            |
| **Stickers**  | GET    | `/stickers`                                             | Bearer | Listar todas as figurinhas                    |
| **Stickers**  | GET    | `/stickers/{id}`                                        | Bearer | Buscar figurinha por ID                       |
| **Stickers**  | GET    | `/stickers/code/{stickerCode}`                          | Bearer | Buscar figurinha por código                   |
| **Stickers**  | GET    | `/stickers/search?team=&type=&stickerCode=&playerName=` | Bearer | Buscar figurinhas com filtros                 |
| **Stickers**  | POST   | `/stickers`                                             | Bearer | Criar figurinha                               |
| **Stickers**  | PUT    | `/stickers/{id}`                                        | Bearer | Atualizar figurinha                           |
| **Stickers**  | DELETE | `/stickers/{id}`                                        | Bearer | Deletar figurinha                             |
| **Stickers**  | GET    | `/collections/album`                                    | Bearer | Coleção do usuário logado                     |
| **Stickers**  | GET    | `/collections/album/{stickerId}`                        | Bearer | Figurinha específica na coleção               |
| **Stickers**  | GET    | `/collections/album/{stickerId}/available-quantity`     | Bearer | Quantidade disponível para troca              |
| **Stickers**  | POST   | `/collections/album`                                    | Bearer | Adicionar figurinha à coleção                 |
| **Stickers**  | DELETE | `/collections/album/delete/{stickerId}?quantity=1`      | Bearer | Remover figurinha da coleção                  |
| **Stickers**  | GET    | `/collections/progress`                                 | Bearer | Progresso do álbum (% completo)               |
| **Stickers**  | GET    | `/collections/missing`                                  | Bearer | Figurinhas faltantes                          |
| **Stickers**  | GET    | `/collections/repeated`                                 | Bearer | Figurinhas repetidas                          |
| **Stickers**  | GET    | `/collections/matches`                                  | ❌     | Matches de troca entre usuários               |
| **Adm**       | —      | `/adm/*`                                                | —      | Dashboard administrativo (em desenvolvimento) |
| **Geo**       | —      | `/geolocalization/*`                                    | —      | Geolocalização (em desenvolvimento)           |
| **Store**     | —      | `/store/*`                                              | —      | Loja (em desenvolvimento)                     |

> Rotas `*/docs` servem a documentação Swagger de cada serviço em <http://localhost:8000/docs>.

#### Fluxo de autenticação via Kong

```txt
FRONTEND
   │
   │ POST /rota (ex: /users)
   │ Authorization: Bearer <JWT>
   │
   ▼
KONG :8000
   │
   │ plugin jwt valida assinatura RSA256
   │
   ├── inválido ──────────────────────> 401 ──> FRONTEND
   │
   └── válido ──> repassa o mesmo JWT p/ QUALQUER microservice
                                                              │
                                                              ▼
                                                        MICROSSERVIÇO DESTINO
                                                              │
                                                              │ Spring Security decodifica JWT
                                                              │ popula SecurityContext
                                                              │
                                                              ▼
                                                        CONTROLLER
                                                              │
                                                              │ @AuthenticationPrincipal Jwt jwt
                                                              │ jwt.getSubject(), jwt.getClaim("name"), etc.
                                                              │
                                                              ▼
                                                        RESPOSTA HTTP
                                                              │
                                                              ▼
                                                        FRONTEND
```

**O papel de cada camada:**

| Camada                       | Responsabilidade                                                                                                                                                                                                                                                                 |
| ---------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Kong** (proxy)             | Ao identificar que a rota é privada, ativa o plugin `jwt`. Valida assinatura RSA256 contra a chave pública do consumer `frontend` e checa expiração (`exp`). Se inválido → `401`. Se válido → repassa requisição ao microsserviço com o header `Authorization` original intacto. |
| **Spring Security** (filtro) | Configurado via `oauth2ResourceServer().jwt()`, intercepta o header `Authorization`, decodifica o JWT usando `JwtDecoder` (chave pública RSA) e popula o `SecurityContext` com o objeto `Jwt` autenticado.                                                                       |
| **Controller** (método)      | Recebe o JWT decodificado via `@AuthenticationPrincipal Jwt jwt` e acessa `jwt.getSubject()` (UUID), `jwt.getClaim("name")`, `jwt.getClaim("role")`.                                                                                                                             |

**Fluxo resumido:**

1. **Registro** → `POST /auth/register` — rota pública sem JWT, cria usuário no banco.
2. **Login** → `POST /auth/login` — valida credenciais, gera JWT (issuer `frontend`, 5 min) + `refresh_token`.
3. **Rota privada** → Ex: `GET /me` — Kong valida JWT, Spring Security decodifica, controller recebe via `@AuthenticationPrincipal`.
4. **Logout** → `POST /auth/logout` — extrai `sub` do JWT e invalida o `refresh_token`.

### Kafka

Comunicação assíncrona entre microserviços via tópicos. Cada consumer usa **ack manual** com retry exponencial (1s → 2s → 4s → 8s, max 30s) e Dead Letter Topic (DLT) após esgotar tentativas.

| Tópico                    | Publisher                 | Consumer(s) | Quando é publicado / consumido                                             |
| ------------------------- | ------------------------- | ----------- | -------------------------------------------------------------------------- |
| `payment.initiated`       | payments                  | —           | Pagamento criado com status `PENDING`                                      |
| `payment.approved`        | payments                  | —           | Pagamento processado e aprovado (`amount` < R$ 2.000,00)                   |
| `payment.failed`          | payments                  | —           | Pagamento processado e recusado (`amount` >= R$ 2.000,00)                  |
| `order.payment.requested` | _(store — a implementar)_ | payments    | Pedido criado na loja → payments cria o pagamento automaticamente          |
| `collection-transfer`     | _(trade — a implementar)_ | stickers    | Proposta de troca aceita → stickers transfere as figurinhas entre coleções |
| `collection-transfer.DLT` | stickers                  | —           | Mensagens que falharam após retries no `collection-transfer`               |

**Tratamento de falhas (stickers):**

- Retry exponencial com backoff (1s → 2s → 4s → 8s, max 30s)
- `DeserializationException` não tentam novamente — vão direto para a DLT
- Após esgotar tentativas, a mensagem é publicada em `collection-transfer.DLT` para análise manual

### Observabilidade

Prometheus, Grafana e Jaeger configurados no `docker-compose.yaml`. Cada microservice expõe endpoints Actuator com métricas coletadas pelo Micrometer e expostas em `/actuator/prometheus`.

| Nome       | Responsabilidade | Porta | Usuário | Senha |
| ---------- | ---------------- | ----- | ------- | ----- |
| Prometheus | Métricas         | 9090  |         |       |
| Grafana    | Dashboards       | 3002  | admin   | admin |
| Jaeger     | Tracing          | 16686 | —       | —     |

## Como executar

**Pré-requisitos:** Java 25 e Docker & Docker Compose

```bash
# Clone o repositório
git clone https://github.com/Projeto-Microservices-Spring-Boot/pb-monorepo.git

# Acessa pasta Docker
cd docker

# Sobe tudo (infra + serviços + frontend)
docker compose up --build
```

## Bancos de dados

### PostgreSQL

Um único container PostgreSQL (`pg:5432`, `postgres:18.3-alpine`) atende todos os microserviços que usam banco relacional. O script `docker/postgres/init-databases.sh` cria automaticamente cada banco listado em `POSTGRES_EXTRA_DATABASES`.

| Banco             | Serviço         | Possui seed?                                                                                                                                      |
| ----------------- | --------------- | ------------------------------------------------------------------------------------------------------------------------------------------------- |
| `users`           | users           | Sim — `database/seed/01-seed-db.sql` (12 usuários com roles ADMIN, BUYER, SELLER)                                                                 |
| `stickers`        | stickers        | Sim — `database-sticker.seed/01-seed-db-sticker.sql` (~400 figurinhas Panini 2026) + `02-seed-db-collections.sql` (coleções mock para 4 usuários) |
| `payments`        | payments        | Não                                                                                                                                               |
| `geolocalization` | geolocalization | Não                                                                                                                                               |
| `adm_dashboard`   | adm-dashboard   | Não                                                                                                                                               |
| `store`           | store           | Não                                                                                                                                               |

A seed é carregada automaticamente na primeira inicialização via `spring.sql.init.mode=always` + `data-locations`. O schema é gerenciado pelo Hibernate com `ddl-auto: update`.

### MongoDB

O serviço `community` usa **MongoDB 7** (`mongodb:27017`, banco `community`) em um container separado. Possui duas coleções: `posts` e `comments`. Não há seed automatizado.

![fluxograma](image-1.png)
