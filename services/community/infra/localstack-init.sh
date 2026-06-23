#!/usr/bin/env bash
# LocalStack initialization script.
# Creates the SNS topic, SQS queues (with DLQs), and fan-out subscriptions
# with eventType message filter policies (Requirements 9.x, 12.x).

set -euo pipefail
REGION="us-east-1"
ENDPOINT="http://localhost:4566"
ACCOUNT="000000000000"

echo "==> Creating SNS topic: community-events-topic"
aws --endpoint-url="$ENDPOINT" sns create-topic \
    --name community-events-topic \
    --region "$REGION"

TOPIC_ARN="arn:aws:sns:${REGION}:${ACCOUNT}:community-events-topic"

# ── SQS Queues + DLQs ────────────────────────────────────────────────────────

create_queue_with_dlq() {
  local NAME="$1"
  local DLQ_NAME="${NAME}-dlq"

  echo "==> Creating DLQ: $DLQ_NAME"
  DLQ_URL=$(aws --endpoint-url="$ENDPOINT" sqs create-queue \
      --queue-name "$DLQ_NAME" \
      --region "$REGION" \
      --query QueueUrl --output text)
  DLQ_ARN="arn:aws:sqs:${REGION}:${ACCOUNT}:${DLQ_NAME}"

  echo "==> Creating Queue: $NAME (DLQ=$DLQ_ARN, maxReceiveCount=5)"
  aws --endpoint-url="$ENDPOINT" sqs create-queue \
      --queue-name "$NAME" \
      --region "$REGION" \
      --attributes "{
        \"VisibilityTimeout\": \"30\",
        \"MessageRetentionPeriod\": \"345600\",
        \"RedrivePolicy\": \"{\\\"deadLetterTargetArn\\\":\\\"${DLQ_ARN}\\\",\\\"maxReceiveCount\\\":\\\"5\\\"}\"
      }"
}

create_queue_with_dlq "community-moderation-queue"
create_queue_with_dlq "community-notification-queue"

MODERATION_ARN="arn:aws:sqs:${REGION}:${ACCOUNT}:community-moderation-queue"
NOTIFICATION_ARN="arn:aws:sqs:${REGION}:${ACCOUNT}:community-notification-queue"

# ── SNS Subscriptions with filter policies ─────────────────────────────────

echo "==> Subscribing moderation queue (PostCreated + CommentCreated)"
aws --endpoint-url="$ENDPOINT" sns subscribe \
    --topic-arn "$TOPIC_ARN" \
    --protocol sqs \
    --notification-endpoint "$MODERATION_ARN" \
    --attributes '{"FilterPolicy":"{\"eventType\":[\"PostCreated\",\"CommentCreated\"]}"}' \
    --region "$REGION"

echo "==> Subscribing notification queue (CommentCreated)"
aws --endpoint-url="$ENDPOINT" sns subscribe \
    --topic-arn "$TOPIC_ARN" \
    --protocol sqs \
    --notification-endpoint "$NOTIFICATION_ARN" \
    --attributes '{"FilterPolicy":"{\"eventType\":[\"CommentCreated\"]}"}' \
    --region "$REGION"

echo "==> LocalStack init complete."
