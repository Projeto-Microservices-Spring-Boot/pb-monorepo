-- ============================================================
-- MOCK de dados para testar GET /collections/matches
-- Pressupõe que a tabela "stickers" já foi populada (ex: pelo
-- INSERT INTO stickers fornecido separadamente, com o álbum
-- completo da Copa). Este script só insere em user_collections,
-- referenciando stickers reais já existentes pelo sticker_code.
--
-- Os user_id são UUIDs aleatórios gerados em tempo de execução
-- usuário tem seu UUID gerado UMA ÚNICA VEZ na CTE "usuarios" e
-- reutilizado em todas as suas linhas (join pela coluna "papel"),
-- garantindo que não vire um usuário diferente por linha.
--
-- Cenário simulado: usuários A, B e C
--   A: ARG 17 (Messi, qty 2, repetida), FRA 20 (Mbappé, qty 3, repetida)
--   B: BRA 14 (Vini Jr, qty 2, repetida), FRA 20 (Mbappé, qty 1)
--   C: ARG 17 (Messi, qty 1), BRA 14 (Vini Jr, qty 1)
--
-- Resultado esperado: match A-B
--   - B oferece a A: Vini Jr (B tem repetida, A não possui)
--   - A oferece a B: Messi (A tem repetida, B não possui)
--   -> os dois lados oferecem algo, então É match entre A-B.
--
-- A-C e B-C não geram match: C não possui nenhuma sticker repetida.
-- ============================================================

WITH usuarios AS (
    SELECT 'A' AS papel, gen_random_uuid() AS user_id
    UNION ALL
    SELECT 'B', gen_random_uuid()
    UNION ALL
    SELECT 'C', gen_random_uuid()
),
     itens AS (
         SELECT 'A' AS papel, 'ARG 17' AS sticker_code, 2 AS quantity
         UNION ALL
         SELECT 'A', 'FRA 20', 3
         UNION ALL
         SELECT 'B', 'BRA 14', 2
         UNION ALL
         SELECT 'B', 'FRA 20', 1
         UNION ALL
         SELECT 'C', 'ARG 17', 1
         UNION ALL
         SELECT 'C', 'BRA 14', 1
     )
INSERT INTO user_collections (user_id, sticker_id, quantity, added_at)
SELECT
    u.user_id,
    s.id,
    i.quantity,
    now()
FROM itens i
         JOIN usuarios u ON u.papel = i.papel
         JOIN stickers s ON s.sticker_code = i.sticker_code;