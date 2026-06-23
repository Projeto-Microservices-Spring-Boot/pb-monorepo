# AWS Production Setup

## SNS Topic

```bash
TOPIC_ARN=$(aws sns create-topic --name community-events-topic --query TopicArn --output text)
```

## SQS Queues with DLQs

```bash
# Moderation DLQ
MOD_DLQ_ARN=$(aws sqs create-queue --queue-name community-moderation-queue-dlq \
  --query Attributes.QueueArn --output text)

# Moderation Queue
aws sqs create-queue --queue-name community-moderation-queue \
  --attributes "{\"VisibilityTimeout\":\"30\",\"MessageRetentionPeriod\":\"345600\",
    \"RedrivePolicy\":\"{\\\"deadLetterTargetArn\\\":\\\"$MOD_DLQ_ARN\\\",\\\"maxReceiveCount\\\":\\\"5\\\"}\"}"

# Notification DLQ
NOTIF_DLQ_ARN=$(aws sqs create-queue --queue-name community-notification-queue-dlq \
  --query Attributes.QueueArn --output text)

# Notification Queue
aws sqs create-queue --queue-name community-notification-queue \
  --attributes "{\"VisibilityTimeout\":\"30\",\"MessageRetentionPeriod\":\"345600\",
    \"RedrivePolicy\":\"{\\\"deadLetterTargetArn\\\":\\\"$NOTIF_DLQ_ARN\\\",\\\"maxReceiveCount\\\":\\\"5\\\"}\"}"
```

## SNS Subscriptions with Filter Policies

```bash
MOD_QUEUE_ARN=$(aws sqs get-queue-attributes \
  --queue-url $(aws sqs get-queue-url --queue-name community-moderation-queue --query QueueUrl --output text) \
  --attribute-names QueueArn --query Attributes.QueueArn --output text)

NOTIF_QUEUE_ARN=$(aws sqs get-queue-attributes \
  --queue-url $(aws sqs get-queue-url --queue-name community-notification-queue --query QueueUrl --output text) \
  --attribute-names QueueArn --query Attributes.QueueArn --output text)

# Moderation: receives PostCreated and CommentCreated
aws sns subscribe --topic-arn $TOPIC_ARN --protocol sqs \
  --notification-endpoint $MOD_QUEUE_ARN \
  --attributes '{"FilterPolicy":"{\"eventType\":[\"PostCreated\",\"CommentCreated\"]}"}'

# Notification: receives CommentCreated only
aws sns subscribe --topic-arn $TOPIC_ARN --protocol sqs \
  --notification-endpoint $NOTIF_QUEUE_ARN \
  --attributes '{"FilterPolicy":"{\"eventType\":[\"CommentCreated\"]}"}'
```

## SQS Queue Policy (allow SNS to deliver)

Apply this policy to both queues, replacing `$TOPIC_ARN` and `$QUEUE_ARN`:

```json
{
  "Version": "2012-10-17",
  "Statement": [{
    "Effect": "Allow",
    "Principal": { "Service": "sns.amazonaws.com" },
    "Action": "sqs:SendMessage",
    "Resource": "$QUEUE_ARN",
    "Condition": { "ArnEquals": { "aws:SourceArn": "$TOPIC_ARN" } }
  }]
}
```

## Required Environment Variables

| Variable | Description |
|---|---|
| `MONGODB_URI` | MongoDB connection string |
| `AWS_REGION` | AWS region (e.g. `us-east-1`) |
| `AWS_ACCESS_KEY_ID` | IAM access key |
| `AWS_SECRET_ACCESS_KEY` | IAM secret key |
| `SNS_TOPIC_COMMUNITY_EVENTS_ARN` | ARN of the community-events SNS topic |
| `SQS_QUEUE_MODERATION` | Name of the moderation SQS queue |
| `SQS_QUEUE_NOTIFICATION` | Name of the notification SQS queue |
| `JWT_ISSUER` | Expected JWT issuer (`users-service`) |
| `JWT_PUBLIC_KEY` | Chave pública RSA (PEM) usada para validar a assinatura do JWT — a mesma chave configurada no `kong.yml` |
