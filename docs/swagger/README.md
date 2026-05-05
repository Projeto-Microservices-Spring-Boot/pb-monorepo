# Swagger (OpenAPI)

O Swagger (atualmente conhecido como OpenAPI Specification) é um padrão para documentar APIs RESTful. Permite descrever endpoints, parâmetros, schemas de requisição/resposta e autenticação de forma legível para humanos e máquinas.

## O que é

Um arquivo de especificação (JSON ou YAML) que descreve sua API: endpoints, métodos HTTP, parâmetros, respostas e modelos de dados.

## Principais Características

- **Documentação Visual**: Interface Swagger UI para testar APIs diretamente no navegador
- **Validação**: Valida requisições e respostas contra o contrato definido
- **Padrão da Indústria**: OpenAPI 3.x é o padrão mais adotado para documentação de APIs REST

Acesse a documentação em: `http://localhost:8080/docs`

## Quando Utilizar

- Para documentar APIs RESTful de forma padronizada
- Quando precisar gerar clientes automaticamente (usando Orval, swagger-codegen, etc.)
- Para permitir que frontend e backend trabalhem em paralelo baseados no contrato da API
- Como fonte da verdade do contrato da API
