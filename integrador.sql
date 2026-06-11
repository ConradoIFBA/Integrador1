
-- INTEGRADOR — SISTEMA DE PEDIDOS PARA RESTAURANTE

CREATE DATABASE IF NOT EXISTS integrador
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
USE integrador;

-- =============================================================
-- 1. USUARIO
-- Três perfis de acesso:
--   GERENTE    → login individual, acesso total e relatórios
--   FUNCIONARIO → conta única compartilhada; cada funcionário
--                 se identifica por ID (ex: A1, A2 para atendente
--                 e C1, C2 para cozinha) ao realizar ações.
--                 A subdivisão atendente/cozinha é registrada
--                 no campo funcao e nos logs.
--   USUARIO    → cliente com acesso ao sistema com a subdivisao entre "mesa" e "delievery"
-- =============================================================
CREATE TABLE usuario (
    id_usuario  INT             NOT NULL AUTO_INCREMENT,
    nome        VARCHAR(100)    NOT NULL,
    login       VARCHAR(50)     NOT NULL UNIQUE,
    senha       VARCHAR(255)    NOT NULL,               -- hash BCrypt
    perfil      ENUM(
                    'GERENTE',
                    'FUNCIONARIO',
                    'USUARIO'
                )               NOT NULL,
    -- funcao só é preenchida para perfil FUNCIONARIO
    funcao      ENUM(
                    'atendente',
                    'cozinha'
                )               DEFAULT NULL,
    ativo       TINYINT(1)      NOT NULL DEFAULT 1,
    PRIMARY KEY (id_usuario),
    INDEX idx_usuario_login  (login),
    INDEX idx_usuario_perfil (perfil)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- =============================================================
-- 2. MESA
-- Mesas físicas do restaurante.
-- =============================================================
CREATE TABLE mesa (
    id_mesa     INT             NOT NULL AUTO_INCREMENT,
    numero      INT             NOT NULL UNIQUE,
    capacidade  INT             NOT NULL,
    status      ENUM(
                    'livre',
                    'ocupada',
                    'reservada'
                )               NOT NULL DEFAULT 'livre',
    ativo       TINYINT(1)      NOT NULL DEFAULT 1,
    PRIMARY KEY (id_mesa),
    INDEX idx_mesa_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- =============================================================
-- 3. HISTORICO_MESA
-- Linha do tempo automática de cada mesa.
-- Ex: "17h00 abertura", "17h15 pedido #3 registrado por A1".
-- =============================================================
CREATE TABLE historico_mesa (
    id_historico    INT             NOT NULL AUTO_INCREMENT,
    mesa_id         INT             NOT NULL,
    descricao       VARCHAR(255)    NOT NULL,
    data_hora       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ativo           TINYINT(1)      NOT NULL DEFAULT 1,
    PRIMARY KEY (id_historico),
    INDEX idx_historico_mesa (mesa_id),
    INDEX idx_historico_data (data_hora),
    CONSTRAINT fk_historico_mesa
        FOREIGN KEY (mesa_id) REFERENCES mesa (id_mesa)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- =============================================================
-- 4. CATEGORIA_ITEM
-- Agrupa itens por setor de preparo.
-- =============================================================
CREATE TABLE categoria_item (
    id_categoria    INT             NOT NULL AUTO_INCREMENT,
    nome            VARCHAR(80)     NOT NULL,
    setor           ENUM(
                        'cozinha',
                        'bebida',
                        'sobremesa'
                    )               NOT NULL DEFAULT 'cozinha',
    ativo           TINYINT(1)      NOT NULL DEFAULT 1,
    PRIMARY KEY (id_categoria)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- =============================================================
-- 5. ITEM_CARDAPIO
-- tempo_preparo_min alimenta o cálculo do tempo estimado na fila.
-- disponivel = 0 bloqueia o item sem excluí-lo (falta de estoque).
-- =============================================================
CREATE TABLE item_cardapio (
    id_item             INT             NOT NULL AUTO_INCREMENT,
    categoria_id        INT             NOT NULL,
    nome                VARCHAR(120)    NOT NULL,
    descricao           TEXT,
    preco               DECIMAL(10,2)   NOT NULL,
    tempo_preparo_min   INT             NOT NULL DEFAULT 15,
    disponivel          TINYINT(1)      NOT NULL DEFAULT 1,
    ativo               TINYINT(1)      NOT NULL DEFAULT 1,
    PRIMARY KEY (id_item),
    INDEX idx_item_categoria  (categoria_id),
    INDEX idx_item_disponivel (disponivel),
    CONSTRAINT fk_item_categoria
        FOREIGN KEY (categoria_id) REFERENCES categoria_item (id_categoria)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- =============================================================
-- 6. PEDIDO
-- Cabeçalho do pedido.
-- mesa_id nullable → pedido é delivery quando NULL.
-- urgente eleva o peso na fila de preparo.
-- identificador_operador → ID do funcionário que abriu (ex: A1).
-- =============================================================
CREATE TABLE pedido (
    id_pedido               INT             NOT NULL AUTO_INCREMENT,
    mesa_id                 INT,
    tipo                    ENUM(
                                'mesa',
                                'delivery'
                            )               NOT NULL,
    urgente                 TINYINT(1)      NOT NULL DEFAULT 0,
    identificador_operador  VARCHAR(20)     NOT NULL,
    status                  ENUM(
                                'aberto',
                                'em_preparo',
                                'pronto',
                                'entregue',
                                'cancelado',
                                'estornado'
                            )               NOT NULL DEFAULT 'aberto',
    observacao              TEXT,
    data_abertura           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ativo                   TINYINT(1)      NOT NULL DEFAULT 1,
    PRIMARY KEY (id_pedido),
    INDEX idx_pedido_mesa   (mesa_id),
    INDEX idx_pedido_status (status),
    INDEX idx_pedido_data   (data_abertura),
    CONSTRAINT fk_pedido_mesa
        FOREIGN KEY (mesa_id) REFERENCES mesa (id_mesa)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- =============================================================
-- 7. ITEM_PEDIDO
-- Itens individuais vinculados ao pedido.
-- preco_unitario gravado no momento do pedido para preservar
-- o valor histórico mesmo se o cardápio for alterado depois.
-- =============================================================
CREATE TABLE item_pedido (
    id_item_pedido      INT             NOT NULL AUTO_INCREMENT,
    pedido_id           INT             NOT NULL,
    item_cardapio_id    INT             NOT NULL,
    quantidade          INT             NOT NULL DEFAULT 1,
    preco_unitario      DECIMAL(10,2)   NOT NULL,
    observacao          TEXT,
    status              ENUM(
                            'pendente',
                            'em_preparo',
                            'pronto',
                            'entregue',
                            'cancelado'
                        )               NOT NULL DEFAULT 'pendente',
    ativo               TINYINT(1)      NOT NULL DEFAULT 1,
    PRIMARY KEY (id_item_pedido),
    INDEX idx_ip_pedido (pedido_id),
    INDEX idx_ip_item   (item_cardapio_id),
    CONSTRAINT fk_ip_pedido
        FOREIGN KEY (pedido_id) REFERENCES pedido (id_pedido)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_ip_cardapio
        FOREIGN KEY (item_cardapio_id) REFERENCES item_cardapio (id_item)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- =============================================================
-- 8. FILA_PREPARO
-- Uma linha por pedido (UNIQUE em pedido_id).
-- Regra de peso:
--   4 = mesa urgente
--   3 = mesa normal
--   2 = delivery urgente
--   1 = delivery normal
-- Quanto maior o peso, mais alta a posição na fila.
-- identificador_operador → funcionário da cozinha que assumiu.
-- =============================================================
CREATE TABLE fila_preparo (
    id_fila                 INT             NOT NULL AUTO_INCREMENT,
    pedido_id               INT             NOT NULL UNIQUE,
    posicao                 INT             NOT NULL,
    peso_prioridade         INT             NOT NULL DEFAULT 1,
    tempo_estimado_min      INT             NOT NULL DEFAULT 0,
    setor                   ENUM(
                                'cozinha',
                                'bebida',
                                'sobremesa'
                            )               NOT NULL DEFAULT 'cozinha',
    data_entrada            DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_inicio_preparo     DATETIME        DEFAULT NULL,
    data_conclusao          DATETIME        DEFAULT NULL,
    identificador_operador  VARCHAR(20)     DEFAULT NULL,
    ativo                   TINYINT(1)      NOT NULL DEFAULT 1,
    PRIMARY KEY (id_fila),
    INDEX idx_fila_posicao (posicao),
    INDEX idx_fila_peso    (peso_prioridade),
    INDEX idx_fila_setor   (setor),
    CONSTRAINT fk_fila_pedido
        FOREIGN KEY (pedido_id) REFERENCES pedido (id_pedido)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- =============================================================
-- 9. ESTORNO
-- Reversão financeira (total ou parcial) de um pedido.
-- Separado do pedido para registrar forma de pagamento e motivo.
-- Append-only — sem campo ativo; registros financeiros são imutáveis.
-- =============================================================
CREATE TABLE estorno (
    id_estorno              INT             NOT NULL AUTO_INCREMENT,
    pedido_id               INT             NOT NULL,
    valor_estornado         DECIMAL(10,2)   NOT NULL,
    tipo_estorno            ENUM(
                                'total',
                                'parcial'
                            )               NOT NULL,
    forma_pagamento         ENUM(
                                'dinheiro',
                                'cartao',
                                'pix'
                            )               NOT NULL,
    motivo                  TEXT,
    data_hora               DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    identificador_operador  VARCHAR(20)     NOT NULL,
    PRIMARY KEY (id_estorno),
    INDEX idx_estorno_pedido (pedido_id),
    INDEX idx_estorno_data   (data_hora),
    CONSTRAINT fk_estorno_pedido
        FOREIGN KEY (pedido_id) REFERENCES pedido (id_pedido)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- =============================================================
-- 10. LOG_OPERACAO
-- Auditoria completa de todas as ações do sistema.
-- funcao registra a subdivisão do funcionário (atendente/cozinha).
-- Append-only — sem UPDATE, sem DELETE, sem campo ativo.
-- =============================================================
CREATE TABLE log_operacao (
    id_log                  INT             NOT NULL AUTO_INCREMENT,
    perfil                  ENUM(
                                'GERENTE',
                                'FUNCIONARIO',
                                'USUARIO'
                            )               NOT NULL,
    funcao                  ENUM(
                                'atendente',
                                'cozinha'
                            )               DEFAULT NULL,           -- preenchido só para FUNCIONARIO
    identificador_operador  VARCHAR(20)     NOT NULL,               -- ex: A1, C2, G1
    descricao               VARCHAR(255)    NOT NULL,               -- ex: A1 registrou pedido #5 para mesa 3
    data_hora               DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id_log),
    INDEX idx_log_operador (identificador_operador),
    INDEX idx_log_perfil   (perfil),
    INDEX idx_log_data     (data_hora)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- =============================================================
-- DADOS INICIAIS
-- =============================================================

-- Contas do sistema
-- Hash corresponde à senha: integrador123
INSERT INTO usuario (nome, login, senha, perfil, funcao) VALUES
    ('Gerente',      'gerente',     '$2a$12$KIx9wZzGQHqv9UkB4XoJxO6z3f2RqYz1Xk5e7pLmNvWdCjT8sHqAu', 'GERENTE',     NULL),
    ('Funcionario',  'funcionario', '$2a$12$KIx9wZzGQHqv9UkB4XoJxO6z3f2RqYz1Xk5e7pLmNvWdCjT8sHqAu', 'FUNCIONARIO', 'atendente'),
    ('Cozinha',      'cozinha',     '$2a$12$KIx9wZzGQHqv9UkB4XoJxO6z3f2RqYz1Xk5e7pLmNvWdCjT8sHqAu', 'FUNCIONARIO', 'cozinha'),
    ('Cliente App',  'usuario',     '$2a$12$KIx9wZzGQHqv9UkB4XoJxO6z3f2RqYz1Xk5e7pLmNvWdCjT8sHqAu', 'USUARIO',     NULL);

-- Mesas de 1 a 10
INSERT INTO mesa (numero, capacidade) VALUES
    (1, 2), (2, 2), (3, 4), (4, 4), (5, 4),
    (6, 6), (7, 6), (8, 8), (9, 8), (10, 10);

-- Categorias por setor
INSERT INTO categoria_item (nome, setor) VALUES
    ('Entradas',          'cozinha'),
    ('Pratos Principais', 'cozinha'),
    ('Grelhados',         'cozinha'),
    ('Sobremesas',        'sobremesa'),
    ('Sucos',             'bebida'),
    ('Bebidas',           'bebida');

-- Itens do cardápio
INSERT INTO item_cardapio (categoria_id, nome, descricao, preco, tempo_preparo_min) VALUES
    (1, 'Pão de alho',         'Pão italiano com manteiga e alho',          12.00,  8),
    (1, 'Bolinho de bacalhau', '6 unidades com molho tártaro',              22.00, 12),
    (2, 'Frango grelhado',     'Filé de frango com legumes salteados',      38.00, 20),
    (2, 'Filé à parmegiana',   'Filé bovino com molho de tomate e queijo',  55.00, 25),
    (2, 'Massa ao sugo',       'Espaguete com molho de tomate caseiro',     32.00, 18),
    (3, 'Picanha na brasa',    '300g com arroz, farofa e vinagrete',        72.00, 30),
    (3, 'Costelinha BBQ',      '400g com fritas e coleslaw',                65.00, 35),
    (4, 'Pudim de leite',      'Fatia com calda de caramelo',               16.00, 10),
    (4, 'Petit gâteau',        'Bolo quente com sorvete de creme',          22.00,  8),
    (5, 'Suco de laranja',     'Natural 400ml',                             10.00,  5),
    (5, 'Vitamina de morango', 'Morango com leite 400ml',                   12.00,  5),
    (6, 'Refrigerante lata',   'Coca-Cola / Guaraná / Sprite',               7.00,  2),
    (6, 'Água mineral',        '500ml com ou sem gás',                       5.00,  1);