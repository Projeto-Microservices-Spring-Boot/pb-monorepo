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

As portas expostas são:

| Nome | Responsabilidade | Porta        |
| ---- | ---------------- | ------------ |
| Kong | API Gateway      | 8001 (UI)    |
| Kong | API Gateway      | 8002 (ADMIN) |
| Kong | API Gateway      | 8000 (Proxy) |

### Observabilidade

O Prometheus e o Grafana estão configurados no `docker-compose.yaml` para dar visibilidade ao ambiente local. Cada microservice expõe seus endpoints do Spring Boot Actuator, incluindo `health` para verificação de disponibilidade e `prometheus` para coleta de métricas. O Prometheus realiza o scrape dessas métricas usando a configuração de `./docker/prometheus/prometheus.yml`, e o Grafana consome esses dados para exibir dashboards e facilitar a análise de saúde, desempenho e comportamento dos serviços.

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
docker compose up --build
```

## Exemplos de requisições ( A fazer)
