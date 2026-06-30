# Toxiproxy

Ferramenta de chaos engineering para testar resiliência de serviços (latência, falhas, bandwidth).

## O que é?

Proxy TCP que fica entre sua aplicação e o serviço (Redis, PostgreSQL, etc). Permite injetar falhas dinamicamente sem mudar código.

---

## Referência Rápida

```bash
# Criar proxy
docker exec -it toxiproxy /toxiproxy-cli create -l 0.0.0.0:13333 -u backend:3333 backend-proxy

# Listar proxies
docker exec -it toxiproxy /toxiproxy-cli list

# Ver detalhes
docker exec -it toxiproxy /toxiproxy-cli inspect backend-proxy

# Adicionar latência
docker exec -it toxiproxy /toxiproxy-cli toxic add -t latency -n redis_latency -a latency=2000 backend-proxy

# Remover latência
docker exec -it toxiproxy /toxiproxy-cli toxic remove -n redis_latency backend-proxy

# Deletar proxy
docker exec -it toxiproxy /toxiproxy-cli delete backend-proxy
```

## Tipos de Toxic

| Tipo         | Descrição         | Parâmetro      |
| ------------ | ----------------- | -------------- |
| `latency`    | Atraso            | `latency=ms`   |
| `timeout`    | Corta conexão     | `timeout=ms`   |
| `bandwidth`  | Limita velocidade | `rate=bytes/s` |
| `slow_close` | Atrasa fechamento | `delay=ms`     |
| `slicer`     | Corta dados       | `average=ms`   |
| `limit_data` | Limita tamanho    | `bytes=n`      |

---

## Docker

Todos os comandos usam `docker exec` para executar dentro do container:

```bash
docker exec -it toxiproxy /toxiproxy-cli <comando> [opções]
```
