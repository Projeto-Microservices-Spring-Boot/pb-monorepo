CREATE EXTENSION IF NOT EXISTS pgcrypto;

INSERT INTO users (id, email, name, password, roles)
VALUES
    (gen_random_uuid(), 'nathan@example.com', 'Nathan Vieira', 'password1', 'ADMIN'),
    (gen_random_uuid(), 'vasco@example.com', 'Vasco da Gama', 'vasco123456', 'BUYER'),
    (gen_random_uuid(), 'leo@example.com', 'Leo', 'leoinfnet2026', 'BUYER'),
    (gen_random_uuid(), 'buyer2@example.com', 'Buyer Two', 'password2', 'BUYER'),
    (gen_random_uuid(), 'buyer3@example.com', 'Buyer Three', 'password3', 'BUYER'),
    (gen_random_uuid(), 'buyer4@example.com', 'Buyer Four', 'password4', 'BUYER'),
    (gen_random_uuid(), 'buyer5@example.com', 'Buyer Five', 'password5', 'BUYER'),
    (gen_random_uuid(), 'seller1@example.com', 'Seller One', 'password6', 'SELLER'),
    (gen_random_uuid(), 'seller2@example.com', 'Seller Two', 'password7', 'SELLER'),
    (gen_random_uuid(), 'seller3@example.com', 'Seller Three', 'password8', 'SELLER'),
    (gen_random_uuid(), 'seller4@example.com', 'Seller Four', 'password9', 'SELLER'),
    (gen_random_uuid(), 'seller5@example.com', 'Seller Five', 'password10', 'SELLER')
ON CONFLICT DO NOTHING;
    
