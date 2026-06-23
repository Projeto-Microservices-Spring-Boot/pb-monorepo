# Rate Limit

## Testa Rate Limit em collections/album
for i in {1..10}; do
  curl -s -o /dev/null -w "%{http_code}\n" \
    -H "Authorization: Bearer SEU_TOKEN" \
    http://localhost:8000/collections/album
done
