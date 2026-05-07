# Netflix Eureka

O Eureka é um serviço de registro e descoberta de serviços desenvolvido pela Netflix como parte do ecossistema Netflix OSS. Ele é amplamente utilizado em arquiteturas de microsserviços para permitir que serviços se localizem e se comuniquem entre si sem hardcoding de endereços de rede. O Eureka faz parte do Spring Cloud Netflix, facilitando sua integração com aplicações Java/Spring.

## Visão Geral

Em uma arquitetura de microsserviços, o número de serviços pode crescer rapidamente, e cada serviço pode ter múltiplas instâncias rodando em diferentes portas e servidores. Manter a configuração manual desses endereços é impraticável. O Eureka resolve esse problema fornecendo um registry centralizado onde os serviços podem se registrar e descobrir outros serviços dinamicamente.

## Principais Características

- **Service Discovery**: Permite que serviços se registrem e descubram outros serviços dinamicamente através de um registry central.
- **Alta Disponibilidade**: Suporta configuração em cluster (peer-aware) onde múltiplas instâncias do Eureka Server se comunicam para evitar ponto único de falha.
- **Registro Dinâmico**: Serviços registram-se automaticamente no Eureka Server ao iniciar, fornecendo metadados como host, porta, status de saúde, entre outros.
- **Renovação de Registro (Heartbeats)**: Serviços enviam heartbeats periódicos (a cada 30 segundos por padrão) para o Eureka Server para manter seu registro ativo.
- **Eviction (Eliminação)**: Se o Eureka Server não receber heartbeats de um serviço por um período configurável (90 segundos por padrão), ele remove o serviço do registry.
- **Client-Side Load Balancing**: Integrado com Ribbon (ou Spring Cloud LoadBalancer), permite balanceamento de carga no lado do cliente.
- **Integração com Spring Cloud**: Amplamente utilizado com Spring Cloud Netflix, facilitando a configuração via anotações e propriedades.

## Componentes Principais

### Eureka Server

O servidor de registro onde todos os serviços se registram. Ele mantém um registro de todas as instâncias de serviço disponíveis e suas informações de rede. Em produção, é recomendado executar múltiplas instâncias do Eureka Server em configuração de peer-to-peer para garantir alta disponibilidade.

**Funcionalidades do Server:**

- Mantém um registry de todos os serviços registrados
- Recebe heartbeats dos clientes para manter registros ativos
- Remove serviços que não enviam heartbeats dentro do tempo configurado
- Fornece a lista de serviços disponíveis para os clientes

### Eureka Client

Aplicações que se registram no Eureka Server e podem descobrir outros serviços registrados. O cliente se comunica com o servidor para registro, renovação e obtenção de informações de outros serviços.

**Funcionalidades do Client:**

- Registra a aplicação no Eureka Server no startup
- Envia heartbeats periódicos para manter o registro
- Consulta o servidor para descobrir outros serviços
- Cache local de informações de serviços para resiliência

## Configuração do Eureka Server

Para criar um Eureka Server com Spring Boot:

1. Adicionar a dependência `spring-cloud-starter-netflix-eureka-server` no pom.xml ou build.gradle
2. Anotar a classe principal com `@EnableEurekaServer`
3. Configurar as propriedades no `application.yml` ou `application.properties`

```yaml
server:
  port: 8761

eureka:
  client:
    register-with-eureka: false
    fetch-registry: false
  server:
    enable-self-preservation: false
```

## Configuração do Eureka Client

Para registrar um microsserviço como cliente Eureka:

1. Adicionar a dependência `spring-cloud-starter-netflix-eureka-client`
2. Anotar a classe principal com `@EnableEurekaClient` ou `@EnableDiscoveryClient`
3. Configurar a URL do Eureka Server:

```yaml
spring:
  application:
    name: meu-microsservico

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
```

## Recursos Avançados

### Self-Preservation Mode

O Eureka possui um mecanismo de auto-preservação que entra em ação quando recebe muitos heartbeats perdidos em um curto período. Em vez de remover todos os serviços que pararam de enviar heartbeats, ele tenta preservá-los, assumindo que pode ser um problema de rede. Em desenvolvimento, esse modo pode ser desabilitado.

### Zones e Regions

O Eureka suporta conceitos de regiões (regions) e zonas (zones) para ambientes distribuídos geograficamente, permitindo que serviços prefiram se comunicar com outros serviços na mesma zona.

### Metadados Personalizados

Serviços podem registrar metadados adicionais (como versão, ambiente, etc.) que podem ser usados por outros serviços para tomar decisões de roteamento.

## Uso no pb-monorepo

O Eureka é o serviço de descoberta utilizado por todos os microsserviços do pb-monorepo:

- Cada microsserviço registra-se no Eureka Server no startup
- O Kong API Gateway consulta o Eureka para rotear requisições para os serviços corretos
- Health checks são realizados automaticamente para garantir que apenas serviços saudáveis recebam tráfego
- Em desenvolvimento, o Eureka Server roda na porta `8761`
