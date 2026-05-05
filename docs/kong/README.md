# Kong API Gateway

O Kong é uma plataforma open-source de API Gateway e microserviços construída sobre o NGINX e OpenResty. Ele atua como uma camada intermediária entre os clientes e os serviços backend, fornecendo funcionalidades essenciais como autenticação, rate limiting, caching, logging e muito mais através de um sistema de plugins extensível. O Kong é amplamente utilizado por empresas que precisam gerenciar, proteger e monitorar APIs em escala.

## Visão Geral

Em arquiteturas de microsserviços modernas, ter um API Gateway se torna essencial para gerenciar o tráfego entre clientes e serviços. O Kong atua como um ponto de entrada único (single entry point) para todas as APIs, centralizando preocupações transversais (cross-cutting concerns) como segurança, monitoramento e roteamento.

> **Nota para desenvolvedores**: Este projeto utiliza o Kong no **modo DB-less** (sem banco de dados). Toda a configuração é feita via arquivo declarativo `kong.yml`, eliminando a necessidade de PostgreSQL e tornando o deploy mais simples e imutável.

## Principais Características

- **Alto Desempenho**: Construído sobre o NGINX e OpenResty (LuaJIT), o Kong oferece performance excepcional com baixa latência e alta taxa de transferência.
- **Extensibilidade via Plugins**: Sistema robusto de plugins que permite adicionar funcionalidades sem modificar o código dos serviços backend.
- **Load Balancing**: Distribuição inteligente de tráfego entre múltiplas instâncias de serviços usando algoritmos como round-robin e least-connections.
- **Autenticação e Autorização**: Suporte nativo a múltiplos mecanismos de autenticação (JWT, OAuth2, API Keys, Basic Auth, LDAP, etc.).
- **Rate Limiting**: Controle granular de taxa de requisições por consumidor, IP ou outros critérios para proteger serviços contra abuso.
- **Logging e Monitoramento**: Integração com ferramentas de observabilidade como Prometheus, StatsD, Datadog, e suporte a logging em arquivos, HTTP, TCP, etc.
- **Transformação de Requisições/Respostas**: Modificação de headers, bodies e parâmetros de requisições e respostas em trânsito.
- **Cache**: Caching de respostas para melhorar o desempenho e reduzir a carga nos serviços backend.
- **Circuit Breaker**: Proteção contra falhas em cascata quando serviços backend estão indisponíveis.
- **Modo DB-less**: Configuração declarativa via arquivo YAML, sem necessidade de banco de dados (usado neste projeto).

## Componentes Principais

### Kong Server (Proxy)

O núcleo do Kong que processa as requisições HTTP/HTTPS dos clientes. Ele roteia as requisições para os serviços backend apropriados, aplicando plugins configurados durante o caminho. Opera nas portas 8000 (HTTP) e 8443 (HTTPS) por padrão.

### Kong Plugins

Extensões que adicionam funcionalidades ao Kong. Os plugins podem ser aplicados globalmente, por serviço, por rota ou por consumidor. O Kong possui dezenas de plugins oficiais e a comunidade contribui com muitos outros.

**Categorias de Plugins:**

- **Autenticação**: JWT, OAuth2, API Key, Basic Auth, LDAP, etc.
- **Segurança**: CORS, IP Restriction, Bot Detection, etc.
- **Tráfego**: Rate Limiting, Request Size Limiting, Proxy Cache, etc.
- **Transformação**: Request Transformer, Response Transformer, etc.
- **Logging**: HTTP Log, TCP Log, File Log, StatsD, Prometheus, etc.
- **Analytics & Monitoring**: Datadog, Runscope, etc.

### Admin API

Uma interface REST para configuração e gerenciamento do Kong. Tradicionalmente acessada na porta 8001 (HTTP) ou 8444 (HTTPS). **No modo DB-less, a Admin API opera em modo somente leitura** — alterações devem ser feitas no arquivo de configuração declarativa.

### Modo DB-less (Usado neste Projeto)

No modo DB-less, o Kong não usa banco de dados. A configuração é carregada de um arquivo YAML/JSON declarativo na inicialização. Este modo é ideal para:

- Ambientes containerizados (Docker, Kubernetes)
- Deployments imutáveis
- CI/CD pipelines
- Redução de dependências externas

A configuração é definida no arquivo `kong.yml` e recarregada reiniciando o Kong ou enviando um SIGHUP.

## Conceitos Fundamentais

### Services (Serviços)

Representam um serviço backend upstream (a aplicação real que responde às requisições). Um serviço é identificado por um URL que aponta para o serviço backend.

### Routes (Rotas)

Define como as requisições são roteadas para os serviços. Uma rota especifica critérios de correspondência (como paths, hosts, métodos HTTP) e está associada a um serviço.

### Consumers (Consumidores)

Representam um consumidor da API (usuário ou aplicação). Consumidores são usados para autenticação, rate limiting e controle de acesso granular.

### Plugins

Configurações que adicionam comportamentos específicos ao Kong. Podem ser aplicados em diferentes níveis: global, serviço, rota ou consumidor.

## Portas Locais do Kong

Ao subir o Kong em `localhost`, estas são as portas mais importantes deste projeto:

- `http://localhost:8000`: porta do **proxy HTTP**. É por onde os clientes entram e as requisições são encaminhadas para os serviços backend configurados no `kong.yml`.
- `http://localhost:8002`: porta da **UI**. É usada para consultar as rotas, plugins etc em uma interface interativa.
- `http://localhost:8001`: porta da **Admin API**. É usada para consultar o status do Kong e inspecionar serviços, rotas e plugins. No modo DB-less, ela fica somente leitura.

## Recursos Avançados

### Load Balancing

O Kong suporta load balancing com health checks. Você pode definir múltiplos upstream targets para um serviço e o Kong distribuirá as requisições.

### Circuit Breaker

Protege serviços backend detectando falhas e impedindo que requisições sejam enviadas para serviços que estão com problemas.

### Request/Response Transformation

Modifique headers, parâmetros de query, corpo da requisição ou resposta em trânsito sem alterar o código do serviço backend.

### OAuth2 and OIDC

Suporte completo para fluxos OAuth2 e integração com OpenID Connect para autenticação moderna.

### Estrutura de Arquivos

```md
docs/kong/
└── README.md       # Esta documentação

kong/
└── kong.yml        # Configuração declarativa dos serviços, rotas e plugins
```

## Quando Utilizar

- Para centralizar a segurança e monitoramento de múltiplas APIs
- Quando há necessidade de autenticação e autorização consistentes em todos os serviços
- Para aplicar rate limiting e proteção contra abuso em APIs públicas
- Quando se deseja desacoplar preocupações transversais dos serviços de negócio
- Para transformar, cachear ou modificar requisições/respostas sem alterar os serviços backend
- Em arquiteturas de microsserviços que necessitam de um ponto de entrada unificado
