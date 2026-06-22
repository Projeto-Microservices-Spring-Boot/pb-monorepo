-- ============================================================
-- MOCK de dados para user_collections (sem CTE, estilo simples)
-- ============================================================

INSERT INTO user_collections (user_id, sticker_id, quantity, added_at)
VALUES
    -- Usuário A
    (gen_random_uuid(), (SELECT id FROM stickers WHERE sticker_code = 'ARG 17'), 2, now()),
    (gen_random_uuid(), (SELECT id FROM stickers WHERE sticker_code = 'FRA 20'), 3, now()),

    -- Usuário B
    (gen_random_uuid(), (SELECT id FROM stickers WHERE sticker_code = 'BRA 14'), 2, now()),
    (gen_random_uuid(), (SELECT id FROM stickers WHERE sticker_code = 'FRA 20'), 1, now()),

    -- Usuário C
    (gen_random_uuid(), (SELECT id FROM stickers WHERE sticker_code = 'ARG 17'), 1, now()),
    (gen_random_uuid(), (SELECT id FROM stickers WHERE sticker_code = 'BRA 14'), 1, now());