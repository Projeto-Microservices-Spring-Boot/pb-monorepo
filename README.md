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

## Bancos de dados (A fazer)

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
├── services/                       # Microserviços
│   └── eureka-server/              # Discovery Server
├── frontend/                       # Frontend da aplicação
├── docker-compose.yaml             # Orquestração dos containers
├── lefthook.yml                    # Hooks de Git (Frontend apenas)
└── README.md
```

### Microservices

| Serviço | Responsabilidade | Porta | Banco |
| ------- | ---------------- | ----- | ----- |
|         |                  |       |       |

### Discovery Server

O Eureka é utilizado para permitir que os diversos microsserviços se conectem e se comuniquem dinamicamente sem a necessidade de informar os endereços IP e as portas de cada um individualmente, cada serviço se conecta ao Eureka Server que está acessível em: <http://localhost:8761> facilitando a escalabilidade.

| Nome          | Responsabilidade | Porta |
| ------------- | ---------------- | ----- |
| Eureka Server | Discovery Server | 8761  |

### API Gateway

O Kong API Gateway centraliza o acesso aos microserviços. No `docker-compose.yaml`, ele sobe em modo DB-less, carregando a configuração declarativa definida em `docker/kong/kong.yaml`.

As portas expostas são:

| Nome | Responsabilidade | Porta     |
| ---- | ---------------- | --------- |
| Kong | API Gateway      | 8002 (UI) |

### Observabilidade

O Prometheus e Grafana estão configurados no `docker-compose.yaml`. O Prometheus recebe as métricas e o Grafana as exibe em dashboards. O Prometheus está configurado em `./docker/prometheus/prometheus.yml` e cada microservices envia métricas via o endpoint /actuator/prometheus.

As portas expostas são:

| Nome       | Responsabilidade | Porta | Usuário | Senha |
| ---------- | ---------------- | ----- | ------- | ----- |
| Prometheus | Métricas         | 9090  | admin   | admin |
| Grafana    | Dashboards       | 3002  | ------- | ----- |

## Como executar

**Pré-requisitos:** **Java 25** e **Docker & Docker Compose**

```bash
# clone o repositório do projeto
git clone https://github.com/Projeto-Microservices-Spring-Boot/pb-monorepo.git

# Sobe tudo (infra + serviços + frontend)
docker compose up
```

## Exemplos de requisições ( A fazer)
