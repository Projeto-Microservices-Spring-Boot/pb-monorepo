# Resumo Executivo

**Status atual:** O Payments tem o fluxo de negócio completo funcionando: criação, processamento (simulação determinística de gateway), consulta, listagem e cancelamento, tudo protegido por JWT. Toda transição de status publica eventos no Kafka, agora com retry e fallback em caso de falha de publicação. As pendências restantes são testes automatizados e polimento de infraestrutura/integração.

**Etapas concluídas:**
- Etapa 1 — Processamento de pagamento (simulação de gateway)
- Etapa 2 — Integração Kafka (eventos de pagamento)
- Etapa 3 — Resiliência Kafka (Retry com Resilience4j)

**Próximas etapas:**
- Etapa 4 — Testes
- Etapa 5 — Infraestrutura e integração
- Etapa 6 — Validação completa do ecossistema

Consulte `DOCUMENTACAO.md` para a explicação técnica detalhada (arquivo por arquivo, método por método) do que já está implementado.

---

# Etapa 1 — Processamento de Pagamento (concluída)

**Objetivo:** dar ao Payments uma decisão própria de aprovação/reprovação, sem depender de gateway externo, já que o projeto é acadêmico.

**O que foi implementado:**
- `PaymentService.process(Payment payment)`: regra determinística — `amount < 1000.00` → `APPROVED`; `amount >= 1000.00` → `FAILED`
- `PaymentService.create()` passou a persistir o pagamento como `PENDING`, processá-lo de forma síncrona e persistir o status final na mesma chamada
- Decisão tomada: **não há webhook nem callback externo** — a aprovação/reprovação acontece inteiramente dentro do próprio `payments`, já que não há integração com gateways reais (Mercado Pago, Stripe, etc.)
- Decisão tomada: o campo `failureReason` foi **descartado por enquanto** — o foco era fazer o fluxo `PENDING → APPROVED/FAILED` funcionar antes de detalhar motivos de falha

**Limitação conhecida e aceita:** como o processamento é síncrono, todo pagamento criado já sai do banco como `APPROVED` ou `FAILED` — o endpoint de cancelamento (que exige `PENDING`) ficou praticamente inalcançável no fluxo real. Ver `DOCUMENTACAO.md`, Seção 8.

---

# Etapa 2 — Integração Kafka (concluída)

**Objetivo:** publicar eventos de domínio a cada transição de status, para que outros serviços (ex: `store`, `adm-dashboard`) possam reagir no futuro.

**O que foi implementado:**
- `config/KafkaConfig.java` — declara os 3 tópicos via beans `NewTopic` (`payment.initiated`, `payment.approved`, `payment.failed`), necessário porque `KAFKA_AUTO_CREATE_TOPICS_ENABLE` está `false`
- `dto/PaymentEvent.java` — DTO do evento Kafka, desacoplado do `PaymentResponse`
- `producer/PaymentProducer.java` — decide o tópico a partir do `status` do evento e publica via `KafkaTemplate`, de forma assíncrona, com log de sucesso/erro via `.whenComplete()`
- `PaymentService.create()` publica dois eventos: um para o estado `PENDING` (`payment.initiated`) e outro para o estado final (`payment.approved` ou `payment.failed`)
- `application.yaml` — `spring.kafka.bootstrap-servers: kafka:29092` configurado

**Bugs pré-existentes corrigidos durante a validação** (não causados por esta etapa, mas expostos por ela ao reconstruir a imagem Docker):
- `jwt.public.key` / `jwt.private.key` sem valor no `payments/application.yaml` — causava `PlaceholderResolutionException` na subida do serviço
- `SecurityConfig` (módulo `shared`) sem `.anyRequest().authenticated()` — requisições com JWT válido recebiam `403 Forbidden` em rotas não listadas explicitamente

**Débito técnico identificado, ainda não corrigido:** o campo `timestamp` do `PaymentEvent` serializa como array Jackson (`[2026,6,16,23,30,26,267482000]`) em vez de String ISO-8601. Precisa ser corrigido antes que outros serviços comecem a consumir esses tópicos (ver Etapa 5).

---

# Etapa 3 — Resiliência Kafka: Retry com Resilience4j (concluída)

**Objetivo:** evitar que uma falha temporária na publicação de eventos Kafka derrube a requisição ou deixe o pagamento em estado inconsistente, usando retry automático com fallback controlado.

**O que foi implementado:**
- `pom.xml` — dependências `resilience4j-spring-boot3` e `spring-boot-starter-aop` (a segunda é exigida porque o Resilience4j intercepta o método anotado via proxy AOP)
- `application.yaml` — `resilience4j.retry.instances.kafka-publisher` com `max-attempts: 3` e `wait-duration: 500ms`
- `producer/PaymentProducer.java`:
  - `publish()` passou a aguardar a confirmação do Kafka de forma síncrona, via `.get(2, TimeUnit.SECONDS)`, em vez do `.whenComplete()` assíncrono usado na Etapa 2 — sem essa mudança o Retry não detectava falha, porque o método retornava sucesso antes do Kafka confirmar o recebimento
  - anotação `@Retry(name = "kafka-publisher", fallbackMethod = "publishFallback")` no método `publish()`
  - novo método `publishFallback(PaymentEvent event, Exception ex)` — loga a falha definitiva (após as 3 tentativas) sem propagar exceção, para que a criação do pagamento não seja revertida só porque o evento não foi publicado

**Achado crítico durante a validação manual — `max.block.ms`:** o `.get(2, TimeUnit.SECONDS)` só limita o tempo de espera pela confirmação do broker depois que `kafkaTemplate.send()` já devolveu um `Future`. Quando o Kafka está totalmente inacessível, o próprio `send()` trava antes disso, internamente, tentando resolver a conexão com o broker — esse bloqueio é controlado por uma propriedade do cliente Kafka chamada `max.block.ms`, que não estava configurada e por padrão vale 60000ms (60s). Como o Retry faz 3 tentativas e `create()` publica 2 eventos por pagamento, uma falha de Kafka chegou a travar uma única requisição por aproximadamente 6 minutos antes da correção.

**Correção aplicada:** adicionado `spring.kafka.producer.properties.max.block.ms: 2000` no `application.yaml`, alinhando o tempo de bloqueio interno do `send()` ao mesmo teto de 2 segundos usado no `.get()`.

**Testes manuais realizados** (Kafka parado e religado via `docker compose stop kafka` / `docker compose start kafka`, sem alterar nenhum outro serviço):

| Cenário | Resultado |
|---|---|
| Fluxo normal, Kafka disponível | Pagamento criado, os 2 eventos publicados com sucesso, log de confirmação para ambos |
| App reiniciada com Kafka já fora do ar (producer nunca chegou a conectar) | Falha rápida, cerca de 2,5 segundos, na própria construção do producer idempotente (mensagem "Failed to construct kafka producer") |
| Kafka cai com a aplicação já rodando e o producer já conectado (cenário mais realista de queda em produção) | 3 tentativas de aproximadamente 2 segundos cada, com log de falha por tentativa, seguidas do fallback final — cerca de 14 segundos no total, pagamento criado mesmo assim com status final correto |
| Kafka religado, sem reiniciar a aplicação | Próximo pagamento processado em cerca de 0,1 segundo, eventos publicados normalmente — recuperação automática confirmada, sem intervenção manual |

**Limitação conhecida e aceita:** no cenário em que a falha ocorre durante a construção do producer (segunda linha da tabela acima), o log granular `"Falha ao publicar evento no tópico..."` não aparece, porque a exceção lançada nesse caso é `org.apache.kafka.common.errors.TimeoutException`, de um tipo diferente do que o bloco `catch` de `publish()` trata (`java.util.concurrent.TimeoutException`). O fallback ainda é acionado corretamente nesse cenário pelo Resilience4j — apenas o log por tentativa fica mudo. Não corrigido por enquanto; pode ser ajustado numa etapa futura caso esse nível de detalhe no log passe a ser necessário.

---

# Etapa 4 — Testes

**Objetivo:** garantir, por meio de testes automatizados, que a lógica de processamento e a publicação de eventos Kafka continuam corretas à medida que o projeto evolui.

**Por que vem agora:** a lógica de negócio (Etapa 1), a integração Kafka (Etapa 2) e o retry com fallback (Etapa 3) já estão implementados e validados manualmente — faz sentido travar esse comportamento com testes antes de mexer em infraestrutura (Etapa 5) ou validar a integração completa do ecossistema (Etapa 6).

## Passo 1 — Testes unitários do `PaymentService`

**Escopo:** mockar `PaymentRepository` e `PaymentProducer` (com Mockito), testar a lógica isoladamente, sem subir Spring context nem Kafka real.

**Classe de teste:** `PaymentServiceTest.java`

| Cenário | Método testado |
|---|---|
| Criar pagamento com `amount < 1000.00` → status final `APPROVED` | `create()` / `process()` |
| Criar pagamento com `amount >= 1000.00` → status final `FAILED` | `create()` / `process()` |
| `create()` publica evento `PENDING` e depois evento do status final (2 chamadas a `producer.publish`) | `create()` |
| Buscar por ID existente e dono correto → retorna | `findById()` |
| Buscar por ID inexistente → 404 | `findById()` |
| Buscar por ID de outro usuário → 403 | `findById()` |
| Listar pagamentos de usuário com registros → retorna lista | `findByUser()` |
| Listar pagamentos de usuário sem registros → lista vazia | `findByUser()` |
| Cancelar pagamento `PENDING` do próprio usuário → `CANCELLED` | `cancel()` |
| Cancelar pagamento `APPROVED`/`FAILED`/`CANCELLED` → `422` | `cancel()` |
| Cancelar pagamento de outro usuário → `403` | `cancel()` |
| Cancelar pagamento inexistente → `404` | `cancel()` |

## Passo 2 — Testes unitários do `PaymentProducer`

**Escopo:** validar a lógica de roteamento de tópicos, mockando `KafkaTemplate`.

| Cenário |
|---|
| Evento com status `PENDING` → publica em `payment.initiated` |
| Evento com status `APPROVED` → publica em `payment.approved` |
| Evento com status `FAILED` → publica em `payment.failed` |
| Evento com status `CANCELLED` (ou outro não mapeado) → não publica, apenas loga aviso |

## Passo 3 — Testes do `GlobalExceptionHandler`

| Cenário |
|---|
| `ResponseStatusException` → JSON com `timestamp`, `status` e `message` |
| `MethodArgumentNotValidException` → `400` com campo inválido |
| `Exception` genérica → `500` |

## Passo 4 — Decisão pendente: testes de integração com Kafka

Ainda não decidido se o escopo da Etapa 4 inclui testes de integração com `EmbeddedKafka` (validando que a mensagem realmente chega ao tópico, com payload correto) ou se isso fica para depois. Avaliar após concluir os Passos 1–3.

**Critério de conclusão da Etapa 4:** `PaymentServiceTest`, `PaymentProducerTest` (ou equivalente) e testes do `GlobalExceptionHandler` implementados e passando; decisão tomada sobre o escopo de testes de integração.

---

# Etapa 5 — Infraestrutura e Integração

**Objetivo:** resolver os débitos técnicos identificados e preparar o serviço para ser consumido por outros microsserviços via Kong e Kafka.

**Itens (ordem não bloqueante entre si):**

1. **Correção da serialização do `timestamp` no `PaymentEvent`** — configurar Jackson para serializar `LocalDateTime` como String ISO-8601 em vez de array. Necessário antes de outros serviços consumirem os tópicos de pagamento.
2. **Proteção JWT no Kong para as rotas `/payments`** — hoje a proteção existe apenas no nível da aplicação; adicionar plugin JWT no `kong.yaml` para as rotas `/payments`.
3. **Flyway para migrações de banco** — substituir `ddl-auto: update` por controle de migração versionado (`V1__...sql`).
4. **Índice em `user_id`** — adicionar `@Index` na entidade `Payment` ou via migration Flyway, para evitar full scan em `findByUserId` com volume alto de dados.
5. **Paginação em `GET /payments/my`** — aceitar `?page=0&size=10`, retornar `Page<PaymentResponse>`.

**Critério de conclusão:** itens 1 e 2 resolvidos (bloqueiam o consumo por outros serviços); itens 3–5 podem ser tratados como melhorias incrementais.

---

# Etapa 6 — Validação Completa do Ecossistema

**Objetivo:** validar o fluxo ponta a ponta do Payments integrado aos demais serviços do projeto.

**Checklist de validação:**

```
[ ] POST /payments com amount < 1000.00 retorna 201 com status APPROVED
[ ] POST /payments com amount >= 1000.00 retorna 201 com status FAILED
[ ] POST /payments sem JWT retorna 401
[ ] POST /payments com amount = 0 retorna 400
[ ] POST /payments com method inválido retorna 400
[ ] GET /payments/{id} retorna 200 para o dono
[ ] GET /payments/{id} retorna 403 para outro usuário
[ ] GET /payments/{id} retorna 404 para ID inexistente
[ ] GET /payments/my retorna lista do usuário autenticado
[ ] GET /payments/my retorna [] para usuário sem pagamentos
[ ] PATCH /payments/{id}/cancel em status APPROVED/FAILED retorna 422
[ ] PATCH /payments/{id}/cancel de outro usuário retorna 403
[ ] Evento publicado em payment.initiated ao criar pagamento
[ ] Evento publicado em payment.approved ao aprovar pagamento
[ ] Evento publicado em payment.failed ao reprovar pagamento
[ ] timestamp do evento Kafka serializado como ISO-8601 (não array)
[x] Falha de publicação no Kafka aciona retry (3 tentativas) e depois o fallback, sem derrubar a requisição
[x] Pagamento criado com sucesso mesmo quando o Kafka está indisponível
[x] Aplicação volta a publicar normalmente assim que o Kafka é religado, sem reiniciar o serviço
[ ] Rota /payments no Kong exige JWT (401 sem token pelo gateway)
[ ] /actuator/health retorna UP
[ ] /actuator/prometheus retorna métricas
[ ] Serviço registrado no Eureka após startup
[ ] Traces visíveis no Jaeger após requisições
[ ] PaymentServiceTest e PaymentProducerTest passando
[ ] Outro serviço (ex: store) consegue consumir um tópico de pagamento de teste
```

**Pré-condição:** Etapas 4 e 5 concluídas; todos os containers rodando (postgres, kafka, users, payments, kong).

**Critério de conclusão:** todos os itens do checklist validados manualmente ou via teste automatizado.
