# Apache Kafka

O Apache Kafka é uma plataforma distribuída de streaming de eventos open-source, desenvolvida originalmente pelo LinkedIn e posteriormente aberta ao público em 2011. Hoje é mantida pela Apache Software Foundation e amplamente adotada por empresas que precisam processar grandes volumes de dados em tempo real. O Kafka atua como um sistema de mensageria de alta performance, mas vai além, oferecendo recursos completos de processamento de streams.

## Visão Geral

O Kafka é projetado para lidar com fluxos de dados em tempo real, permitindo que aplicações publiquem, consumam e processem streams de registros (mensagens) de forma durável e confiável. Ele combina a simplicidade de uma fila de mensagens com recursos avançados de armazenamento e processamento distribuído.

## Principais Características

- **Alto Throughput**: Projetado para processar milhões de mensagens por segundo com baixa latência, superando sistemas de mensageria tradicionais.
- **Escalabilidade Horizontal**: Arquitetura distribuída que permite adicionar novos brokers (servidores) ao cluster para aumentar a capacidade de processamento e armazenamento.
- **Persistência de Dados**: Mensagens são armazenadas em disco de forma durável e podem ser configuradas para retenção por um período de tempo ou tamanho específico.
- **Replicação**: Dados são replicados entre múltiplos brokers, garantindo tolerância a falhas e alta disponibilidade.
- **Modelo Publish-Subscribe**: Suporta padrões de mensageria assíncrona onde produtores publicam mensagens em tópicos e consumidores as leem.
- **Processamento de Streams**: Oferece a Kafka Streams API para processamento de dados em tempo real diretamente no Kafka.
- **Consumer Groups**: Permite que múltiplos consumidores compartilhem o processamento de um tópico, com cada consumer recebendo uma partição diferente.

## Conceitos Fundamentais

### Broker

Um servidor Kafka que faz parte de um cluster. Os brokers são responsáveis por receber mensagens dos produtores, armazená-las e servi-las aos consumidores. Um cluster Kafka consiste em um ou mais brokers.

### Topic

Uma categoria ou feed de mensagens. Os dados no Kafka são organizados em tópicos, que são identificados por um nome. Tópicos são particionados e replicados para escalabilidade e tolerância a falhas.

### Partition

Cada tópico pode ser dividido em múltiplas partições, que são a unidade básica de paralelismo no Kafka. Cada partição é uma sequência ordenada e imutável de mensagens que é continuamente anexada. As mensagens em uma partição recebem um ID sequencial chamado offset.

### Producer (Produtor)

Aplicações que enviam (publicam) mensagens para tópicos Kafka. Os produtores podem escolher qual partição enviar a mensagem (baseada em uma chave ou algoritmo de round-robin).

### Consumer (Consumidor)

Aplicações que leem (consomem) mensagens de tópicos Kafka. Os consumidores leem mensagens em ordem dentro de uma partição, usando o offset para rastrear seu progresso.

### Consumer Group

Um grupo de consumidores que trabalham juntos para consumir um ou mais tópicos. Cada partição em um tópico é consumida por exatamente um consumidor no grupo, permitindo processamento paralelo. Se houver mais consumidores que partições, alguns consumidores ficarão ociosos.

### Offset

Um identificador único para cada mensagem dentro de uma partição. Os consumidores rastreiam qual offset eles consumiram para saber de onde continuar em caso de reinicialização.

### ZooKeeper / KRaft

Historicamente, o Kafka usava o Apache ZooKeeper para gerenciar metadados do cluster, eleição de broker líder e configurações. Recentemente, o Kafka introduziu o KRaft (Kafka Raft) para eliminar a dependência do ZooKeeper, tornando o Kafka auto-gerenciado.

## Arquitetura

### Cluster Kafka

Um cluster Kafka é composto por múltiplos brokers que cooperam para armazenar e servir mensagens. O cluster gerencia a atribuição de partições, replicação e eleição de líderes.

### Replicação

Cada partição pode ter múltiplas réplicas em diferentes brokers. Entre as réplicas, uma é designada como líder (leader) e as outras são seguidoras (followers). O líder lida com todas as operações de leitura e escrita para a partição, enquanto os seguidores apenas replicam os dados.

### Retenção de Mensagens

O Kafka retém mensagens por um período configurável (padrão de 7 dias) ou até que um tamanho limite seja atingido. Diferente de filas de mensagens tradicionais, as mensagens não são apagadas após serem consumidas, permitindo que múltiplos consumidores leiam os mesmos dados independentemente.

## APIs do Kafka

### Producer API

Permite que aplicações publiquem streams de registros em um ou mais tópicos Kafka.

### Consumer API

Permite que aplicações assinem um ou mais tópicos e processem o stream de registros produzidos.

### Streams API

Permite que aplicações atuem como processadores de stream, transformando streams de entrada em streams de saída. Ideal para processamento de dados em tempo real.

### Connect API

Permite construir e executar connectors que trazem dados de sistemas externos para o Kafka ou levam dados do Kafka para sistemas externos (bancos de dados, sistemas de arquivos, etc.).

### Admin API

Permite gerenciar e inspecionar tópicos, brokers e outras entidades Kafka programaticamente.

## Casos de Uso Comuns

- **Pipeline de Dados em Tempo Real**: Coleta e processamento de dados de sensores IoT, logs de aplicações, etc.
- **Stream Processing**: Processamento de eventos em tempo real para análises, detecção de fraudes, recomendações, etc.
- **Integração de Microsserviços**: Comunicação assíncrona entre serviços usando eventos.
- **Log Aggregation**: Coleta centralizada de logs de múltiplos serviços.
- **Commit Logs**: Replicação de dados entre sistemas.

## Quando Utilizar

- Quando há necessidade de processar grandes volumes de dados em tempo real
- Para construir pipelines de dados escaláveis e tolerantes a falhas
- Quando múltiplos consumidores precisam ler os mesmos dados independentemente
- Para desacoplar microsserviços através de eventos assíncronos
- Quando a durabilidade e persistência de mensagens são requisitos importantes
