# Nome do Projeto ( A fazer)

## Integrantes

- Nathan Rodrigues Vieira
- Natan de Andrade Robers
- Marcos Isaac Santana do Carmo
- Isabella Mayer Rocha e Silva
- Eduardo de Assis Araujo
- Lucas Silva de Souza

## Descrição do Projeto ( A fazer)

## Decisões de Arquitetura & Trade-offs ( A fazer)

## Bancos de dados

Ao subir o serviço `pg` no `docker-compose.yaml`, o container executa o script `./docker/postgres/init-databases.sh` e cria automaticamente os bancos adicionais informados em `POSTGRES_EXTRA_DATABASES`. Assim, além do banco padrão definido em `POSTGRES_DB`, o ambiente local já sobe com todos os bancos necessários para os microservices.

### Estrutura de pastas

```text
pb-monorepo/
├── .github/                        # Configurações do GitHub e CI/CD
│   ├── PULL_REQUEST_TEMPLATE/
│   └── CODEOWNERS
├── docker/                         # Infraestrutura local
│   ├── kong/kong.yaml              # Rotas e plugins do API Gateway
│   ├── postgres/init-databases.sh  # Criação dos bancos
│   └── prometheus/prometheus.yaml  # Configuração de métricas
├── docs/                           # Documentações do projeto
│   ├── clean-architecture/
│   ├── eureka/
│   ├── kafka/
│   ├── kong/
│   ├── postman/                    # Collection para testar os endpoints
│   ├── semantic-commits/
│   ├── swagger/
│   └── toxiproxy/
├── frontend/                       # Frontend da aplicação
├── services/                       # Microserviços
│   ├── adm-dashboard/
│   ├── auth/
│   ├── community/
│   ├── eureka-server/              # Discovery Server
│   ├── geolocalization/
│   ├── payments/
│   ├── profile/
│   ├── stickers/
│   └── store/
├── docker-compose.yaml             # Orquestração dos containers
├── lefthook.yml                    # Hooks de Git (Frontend apenas)
└── README.md
```

### Microservices

Todos os microservices deste monorepo são dockerizados em arquivos `Dockerfile` e sobem localmente pelo `docker-compose.yaml`, cada um em sua própria imagem/container e com a porta exposta definida na tabela abaixo.

| Serviço         | Responsabilidade | Porta | Banco           |
| --------------- | ---------------- | ----- | --------------- |
| adm-dashboard   |                  | 8081  | adm_dashboard   |
| auth            |                  | 8082  | auth            |
| community       |                  | 8083  | community       |
| geolocalization |                  | 8084  | geolocalization |
| payments        |                  | 8085  | payments        |
| profile         |                  | 8086  | profile         |
| stickers        |                  | 8087  | stickers        |
| store           |                  | 8088  | store           |

### Discovery Server

O Eureka é utilizado para permitir que os diversos microsserviços se conectem e se comuniquem dinamicamente sem a necessidade de informar os endereços IP e as portas de cada um individualmente, cada serviço se conecta ao Eureka Server que está acessível em: <http://localhost:8761> facilitando a escalabilidade.

| Nome          | Responsabilidade | Porta |
| ------------- | ---------------- | ----- |
| Eureka Server | Discovery Server | 8761  |

### API Gateway

O Kong API Gateway centraliza o acesso aos microservices. No `docker-compose.yaml`, ele sobe em modo DB-less, carregando a configuração declarativa definida em `docker/kong/kong.yaml`. A interface administrativa fica em <http://localhost:8002>, a Admin API em <http://localhost:8001> e o proxy HTTP em <http://localhost:8000>. Para acessar um microservice, use a rota correspondente no proxy, por exemplo `http://localhost:8000/auth`, `http://localhost:8000/store` ou `http://localhost:8000/payments`.

O Kong também gerencia a autenticação via plugin JWT: rotas públicas (ex: `/users/public`) não exigem token, enquanto rotas privadas (ex: `/users/`) exigem um JWT válido assinado com RSA256. O consumer `frontend` possui a chave pública para validação. O fluxo completo de autenticação (registro, login, logout) está detalhado em [Fluxo de autenticação via Kong](#fluxo-de-autenticação-via-kong).

As portas expostas são:

| Nome | Responsabilidade | Porta            |
| ---- | ---------------- | ---------------- |
| Kong | API Gateway      | 8000 (Proxy)     |
| Kong | API Gateway      | 8001 (Admin API) |
| Kong | API Gateway      | 8002 (Admin UI)  |

#### Fluxo de autenticação via Kong

Abaixo, a jornada de uma requisição autenticada desde o frontend até o controller, passando pelo Kong e pelo Spring Security:

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
   └── válido ──> repassa 0 mesmo JWT p/ QUALQUER microservice
                                                              │
                                                              │
                                                              │
                                                              │
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

| Camada                       | Responsabilidade                                                                                                                                                                                                                                                                                                                                                                                       |
| ---------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| **Kong** (proxy)             | Ao identificar que a rota é `/users/` (privada), ativa o plugin `jwt`. Lê o header `Authorization: Bearer <token>`, valida a assinatura RSA256 contra a chave pública do consumer `frontend` e checa a expiração (`exp`). Se inválido → `401`. Se válido → **repassa a requisição ao microsserviço com o header `Authorization` original intacto**. Kong não decodifica nem modifica o payload do JWT. |
| **Spring Security** (filtro) | Configurado via `oauth2ResourceServer().jwt()`, intercepta o header `Authorization` na chegada ao microsserviço, **decodifica** o JWT usando a `JwtDecoder` (chave pública RSA) e popula o `SecurityContext` com o objeto `Jwt` autenticado.                                                                                                                                                           |
| **Controller** (método)      | Recebe o JWT já decodificado via `@AuthenticationPrincipal Jwt jwt`. Acessa `jwt.getSubject()` (UUID do usuário), `jwt.getClaim("name")`, `jwt.getClaim("role")` sem precisar extrair ou decodificar nada manualmente.                                                                                                                                                                                 |

**Fluxo resumido:**

1. **Registro** → `POST /users/public/auth/register` — rota pública (sem plugin jwt), cria o usuário no banco.
2. **Login** → `POST /users/public/auth/login` — valida credenciais, gera um JWT (issuer `frontend`, 5 min) e um `refresh_token`, retorna ambos.
3. **Rota privada** → `GET /users/perfil` — Kong valida o JWT, repassa ao microsserviço. Spring Security decodifica. Controller recebe via `@AuthenticationPrincipal`.
4. **Logout** → `POST /auth/logout` — controller extrai o `sub` do JWT e invalida o `refresh_token` no banco.

### Observabilidade

O Prometheus, Grafana e o Jaeger estão configurados no `docker-compose.yaml` para dar visibilidade ao ambiente local. Cada microservice expõe endpoints do Spring Boot Actuator que fornecem informações de saúde, métricas e diagnóstico. As métricas são coletadas pelo Micrometer (registry Prometheus) e expostas no endpoint `/actuator/prometheus`, que o Prometheus faz scrape de acordo com `./docker/prometheus/prometheus.yml`. O Grafana consome esses dados para exibir dashboards prontos e facilitar a análise de saúde, desempenho e comportamento dos serviços.

Como usar (local / desenvolvimento):

- Endpoints principais:
- `/actuator/health` — estado de saúde do serviço.
- `/actuator/metrics` — métricas agregadas pelo Micrometer.
- `/actuator/prometheus` — ponto de integração com Prometheus (formato de scraping).

A forma mais direta de consultar

- Healthcheck: `http://localhost:8082/actuator/health`
- Métricas: `http://localhost:8082/actuator/prometheus`

As portas expostas são:

| Nome       | Responsabilidade | Porta | Usuário | Senha |
| ---------- | ---------------- | ----- | ------- | ----- |
| Prometheus | Métricas         | 9090  |         |       |
| Grafana    | Dashboards       | 3002  | admin   | admin |
| Jaeger     | Tracing          | 16686 | ------- | ----- |

## Como executar

**Pré-requisitos:** **Java 25** e **Docker & Docker Compose**

```bash
# clone o repositório do projeto
git clone https://github.com/Projeto-Microservices-Spring-Boot/pb-monorepo.git

# Sobe tudo (infra + serviços + frontend)
docker compose up --build
```
