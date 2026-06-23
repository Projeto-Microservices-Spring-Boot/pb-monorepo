# Configuração da Chave Pública RSA

O community-service usa a **mesma chave pública RSA** que todos os outros
microserviços do projeto para validar os JWTs emitidos pelo users-service.

## Como obter a chave correta

A chave pública já está no `kong.yml` do seu projeto:

```yaml
consumers:
  - username: frontend
    jwt_secrets:
      - rsa_public_key: |
          -----BEGIN PUBLIC KEY-----
          MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA3Ga1...
          -----END PUBLIC KEY-----
```



## Configuração por ambiente

### Docker / docker-compose
A chave está no arquivo `keys/public.pem` dentro do JAR — já configurado.

### Railway (produção)

```
JWT_PUBLIC_KEY=-----BEGIN PUBLIC KEY-----\nMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA3Ga1...\n-----END PUBLIC KEY-----
```

> **Atenção**: No Railway o valor deve ser uma única linha com `\n` literal
> separando as linhas do PEM, ou use um arquivo montado como volume.

### Variável de ambiente com arquivo externo


## Geração de novo par de chaves (se necessário)

```bash
# Gera chave privada RSA 2048 bits
openssl genrsa -out private.pem 2048

# Extrai a chave pública
openssl rsa -in private.pem -pubout -out public.pem
```
A chave pública vai em TODOS os microserviços e no kong.yml.
