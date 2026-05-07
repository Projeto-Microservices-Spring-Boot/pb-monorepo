# Clean Architecture

Clean Architecture é um padrão de arquitetura de software proposto por Robert C. Martin (Uncle Bob) que estabelece um conjunto de práticas para criar sistemas que sejam independentes de frameworks, banco de dados, interfaces de usuário e qualquer agente externo. O objetivo principal é facilitar a manutenção, testabilidade e evolução do software ao longo do tempo.

## Princípios Fundamentais

- **Independência de Frameworks**: A arquitetura não depende de bibliotecas ou frameworks externos. Essas ferramentas são tratadas como plugins que podem ser substituídos sem afetar a lógica de negócio.
- **Testabilidade**: As regras de negócio podem ser testadas sem depender de UI, banco de dados, servidor web ou qualquer elemento externo.
- **Independência de UI**: A interface do usuário pode ser alterada facilmente sem que isso afete o restante do sistema. Por exemplo, uma interface web pode ser substituída por uma interface de linha de comando sem mudanças nas regras de negócio.
- **Independência de Banco de Dados**: O sistema não depende de um banco de dados específico. Os dados podem ser armazenados em SQL, NoSQL, ou até mesmo em arquivos locais, sem que a lógica de negócio seja afetada.
- **Independência de Agentes Externos**: As regras de negócio não conhecem nada sobre o mundo exterior. Elas não sabem nada sobre HTTP, APIs externas ou interfaces de usuário.

## As Camadas da Clean Architecture

A arquitetura é organizada em camadas concêntricas, onde as camadas internas contêm as regras de negócio de mais alto nível e as camadas externas contêm detalhes de implementação.

### 1. Entities (Camada de Entidades)

A camada mais interna, contendo as regras de negócio enterprise da aplicação. As entidades encapsulam as regras de negócio mais gerais e críticas da empresa. Elas são independentes de qualquer framework ou banco de dados e não devem ser afetadas por mudanças em camadas externas.

### 2. Use Cases (Casos de Uso)

Esta camada contém as regras de negócio específicas da aplicação. Os casos de uso orquestram o fluxo de dados para e das entidades, e direcionam esses dados para as camadas de apresentação ou persistência. Mudanças na interface do usuário ou no banco de dados não devem afetar esta camada.

### 3. Interface Adapters (Adaptadores de Interface)

Esta camada converte dados do formato mais conveniente para as camadas internas (use cases e entities) para o formato mais conveniente para as camadas externas (banco de dados, web, etc.). Exemplos incluem controllers, gateways e presenters.

### 4. Frameworks & Drivers (Frameworks e Drivers)

A camada mais externa, composta por frameworks e ferramentas como banco de dados, web frameworks (Spring, Express, etc.), e a interface do usuário. Esta camada contém o código que faz a comunicação com o mundo externo.

## Regra de Dependência

A regra mais importante na Clean Architecture é a **Regra de Dependência**: as dependências só podem apontar para dentro. Nenhuma dependência de uma camada externa deve apontar para uma camada interna. Isso significa que as camadas internas não têm conhecimento sobre as camadas externas.

## Benefícios

- **Facilidade de Manutenção**: Mudanças em frameworks ou banco de dados têm impacto mínimo no sistema.
- **Testabilidade Superior**: As regras de negócio podem ser testadas isoladamente.
- **Flexibilidade**: O sistema pode evoluir independentemente das tecnologias utilizadas.
- **Reutilização**: As regras de negócio podem ser reutilizadas em diferentes contextos (web, mobile, desktop).

## Quando Utilizar

A Clean Architecture é especialmente útil em projetos de médio a grande porte, onde a manutenção a longo prazo e a evolução do sistema são prioridades. Para projetos pequenos ou protótipos, a complexidade adicional pode não ser justificada.
