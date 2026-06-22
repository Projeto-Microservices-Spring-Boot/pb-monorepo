-- ============================================================
-- MOCK de dados para user_collections — gera matches reais
-- ============================================================
-- UUIDs fixos para facilitar testes no Postman:
--   Usuário A = 11111111-1111-1111-1111-111111111111
--   Usuário B = 22222222-2222-2222-2222-222222222222
--   Usuário C = 33333333-3333-3333-3333-333333333333
--   Usuário D = 44444444-4444-4444-4444-444444444444
--
-- Regra para gerar match entre dois usuários:
--   X precisa ter repetida (quantity > 1) uma sticker que Y não possui, E
--   Y precisa ter repetida (quantity > 1) uma sticker que X não possui.
-- ============================================================

INSERT INTO user_collections (user_id, sticker_id, quantity, added_at)
VALUES
    -- ---------------------------------------------------------
    -- Usuário A: tem ARG 17 repetida (oferece) e FRA 20 única (não oferece)
    -- ---------------------------------------------------------
    ('11111111-1111-1111-1111-111111111111', (SELECT id FROM stickers WHERE sticker_code = 'ARG 17'), 2, now()),
    ('11111111-1111-1111-1111-111111111111', (SELECT id FROM stickers WHERE sticker_code = 'FRA 20'), 1, now()),

    -- ---------------------------------------------------------
    -- Usuário B: tem FRA 20 repetida (oferece) e BRA 14 única (não oferece)
    -- Match A <-> B esperado: A oferece ARG 17, B oferece FRA 20
    -- ---------------------------------------------------------
    ('22222222-2222-2222-2222-222222222222', (SELECT id FROM stickers WHERE sticker_code = 'FRA 20'), 3, now()),
    ('22222222-2222-2222-2222-222222222222', (SELECT id FROM stickers WHERE sticker_code = 'BRA 14'), 1, now()),

    -- ---------------------------------------------------------
    -- Usuário C: tem BRA 14 repetida (oferece) e ARG 17 única (não oferece)
    -- Match B <-> C esperado: B oferece FRA 20, C oferece BRA 14
    -- Match A <-> C esperado: A oferece ARG 17, C oferece BRA 14
    -- ---------------------------------------------------------
    ('33333333-3333-3333-3333-333333333333', (SELECT id FROM stickers WHERE sticker_code = 'BRA 14'), 2, now()),
    ('33333333-3333-3333-3333-333333333333', (SELECT id FROM stickers WHERE sticker_code = 'ARG 17'), 1, now()),

    -- ---------------------------------------------------------
    -- Usuário D: só tem GER 17 repetida, mas ninguém mais tem stickers
    -- que ele precise, e ele não tem nada que os outros precisem
    -- (cenário de "sem match" — para validar que o endpoint não força
    -- um resultado quando não há troca mutuamente vantajosa)
    -- ---------------------------------------------------------
    ('44444444-4444-4444-4444-444444444444', (SELECT id FROM stickers WHERE sticker_code = 'GER 17'), 5, now());