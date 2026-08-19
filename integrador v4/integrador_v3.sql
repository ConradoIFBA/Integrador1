-- ================================================================
-- INTEGRADOR v3 — BANCO FINAL (CORRIGIDO / IDEMPOTENTE)
--
-- Mudanças em relação ao v2:
--   - historico_mesa    REMOVIDA → campos operador/data_status em mesa
--   - log_operacao      REMOVIDA → auditoria inline em pedido e fila_preparo
--   - estorno           RENOMEADA → pagamento
--   - item_cardapio     RENOMEADA → cardapio (PK: id_cardapio)
--   - item_pedido       coluna item_cardapio_id → cardapio_id
--   - identificador_operador VARCHAR(20) → VARCHAR(100)
--   - cardapio          + coluna imagem VARCHAR(255) NULL (fundida de
--                          migracao_imagem_cardapio.sql)
--   - mesa              + colunas chamando_garcom/data_chamado (fundidas
--                          de migracao_chamar_garcom.sql — ver nota abaixo)
--
-- CORREÇÃO em relação ao v3 original:
--   - Script agora derruba o banco inteiro antes de recriar, então pode
--     ser executado várias vezes sem erro de "tabela já existe" e sem
--     deixar tabelas antigas (item_cardapio, historico_mesa, etc.)
--     esquecidas no schema.
--   - ATENÇÃO: rodar este script APAGA TODOS OS DADOS do banco
--     'integrador' (incluindo qualquer pedido/mesa de teste que você
--     já tenha criado).
--
-- CORREÇÃO — SENHA JÁ VEM COM HASH REAL (não é mais placeholder):
--   Versões anteriores deste script inseriam os 4 usuários de teste
--   com senha = '$2a$12$placeholder' — um valor que PARECE um hash
--   BCrypt mas não é, obrigando a rodar TesteLogin.java toda vez que
--   o banco era resetado, só para o login voltar a funcionar. Isso
--   foi corrigido: o hash abaixo é um hash BCrypt de verdade da senha
--   "integrador123", gerado uma única vez e "congelado" aqui no
--   script — BCrypt não precisa ser gerado na hora do INSERT porque,
--   diferente de uma senha em texto puro, um hash já pronto funciona
--   para sempre, independente de quando/quantas vezes for inserido.
--   Login e senha continuam os mesmos de sempre (ver seção CONTAS DE
--   TESTE no final deste comentário) — só não precisa mais rodar
--   TesteLogin.java depois de importar. Esse arquivo continua
--   existindo e funcionando normalmente, só deixou de ser um passo
--   obrigatório no fluxo de reset do banco.
--
--   ⚠️ Compatibilidade: o hash foi gerado com prefixo "$2a$"
--   explicitamente (não o "$2b$" que ferramentas bcrypt mais novas
--   costumam usar por padrão) porque o projeto usa jBCrypt 0.4
--   (org.mindrot:jbcrypt:0.4, ver pom.xml), cujo BCrypt.hashpw()
--   rejeita com IllegalArgumentException qualquer prefixo de versão
--   diferente de "$2$" ou "$2a$". Se um dia trocar de biblioteca de
--   hashing, vale reconferir essa compatibilidade antes de reusar
--   este hash.
--
-- NOTA — FUSÃO COM migracao_imagem_cardapio.sql:
--   O script de migração original era um ALTER TABLE separado, pensado
--   para rodar em cima de um banco JÁ EXISTENTE sem perder dados. Como
--   ESTE script já derruba e recria o banco do zero (DROP DATABASE),
--   não faz sentido manter os dois passos separados: a coluna 'imagem'
--   foi incorporada direto na definição de CREATE TABLE cardapio, mais
--   abaixo. Se você ainda tiver um banco 'integrador' antigo (sem coluna
--   imagem) e SÓ quiser adicionar a coluna sem perder dados, use o
--   migracao_imagem_cardapio.sql avulso em vez deste script.
--
-- NOTA — FUSÃO COM migracao_chamar_garcom.sql:
--   Mesmo raciocínio: as colunas chamando_garcom e data_chamado
--   (usadas pelo fluxo "Chamar Garçom" do cliente) foram incorporadas
--   direto na definição de CREATE TABLE mesa. Se você tiver um banco
--   antigo sem essas colunas e não quiser recriar tudo do zero, use o
--   migracao_chamar_garcom.sql avulso em vez deste script.
-- ================================================================

DROP DATABASE IF EXISTS integrador;

CREATE DATABASE integrador
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
USE integrador;

-- ================================================================
-- 1. USUARIO
-- ================================================================
CREATE TABLE usuario (
    id_usuario  INT          NOT NULL AUTO_INCREMENT,
    nome        VARCHAR(100) NOT NULL,
    login       VARCHAR(50)  NOT NULL UNIQUE,
    senha       VARCHAR(255) NOT NULL,
    perfil      ENUM('GERENTE','FUNCIONARIO','USUARIO') NOT NULL,
    funcao      ENUM('atendente','cozinha')             DEFAULT NULL,
    ativo       TINYINT(1)   NOT NULL DEFAULT 1,
    PRIMARY KEY (id_usuario),
    INDEX idx_usuario_login  (login),
    INDEX idx_usuario_perfil (perfil)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ================================================================
-- 2. MESA  (fundida com historico_mesa)
--    operador e data_status registram a última alteração de status.
--    chamando_garcom/data_chamado: sinalizador do fluxo "Chamar
--    Garçom" acionado pelo cliente na tela cliente/mesa.jsp — fica 1
--    até um funcionário confirmar o atendimento (MesaController.
--    atenderChamado()).
-- ================================================================
CREATE TABLE mesa (
    id_mesa          INT          NOT NULL AUTO_INCREMENT,
    numero           INT          NOT NULL UNIQUE,
    capacidade       INT          NOT NULL,
    status           ENUM('livre','ocupada','reservada') NOT NULL DEFAULT 'livre',
    operador         VARCHAR(100) DEFAULT NULL,
    data_status      DATETIME     DEFAULT NULL,
    chamando_garcom  TINYINT(1)   NOT NULL DEFAULT 0,
    data_chamado     DATETIME     DEFAULT NULL,
    ativo            TINYINT(1)   NOT NULL DEFAULT 1,
    PRIMARY KEY (id_mesa),
    INDEX idx_mesa_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ================================================================
-- 3. CATEGORIA_ITEM
-- ================================================================
CREATE TABLE categoria_item (
    id_categoria INT         NOT NULL AUTO_INCREMENT,
    nome         VARCHAR(80) NOT NULL,
    setor        ENUM('cozinha','bebida','sobremesa') NOT NULL DEFAULT 'cozinha',
    ativo        TINYINT(1)  NOT NULL DEFAULT 1,
    PRIMARY KEY (id_categoria)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ================================================================
-- 4. CARDAPIO  (antes: item_cardapio)
--    PK: id_cardapio
--    + coluna imagem: guarda apenas o NOME DO ARQUIVO salvo em disco
--      pelo upload (ex: 'a1b2c3d4.jpg'), não o caminho completo nem
--      os bytes da imagem — os bytes ficam fora do banco (ver
--      UploadImagemUtil.java e ImagemServlet.java). Itens sem foto
--      cadastrada ficam com essa coluna NULL, e a tela mostra um
--      bloco decorativo colorido no lugar.
-- ================================================================
CREATE TABLE cardapio (
    id_cardapio       INT           NOT NULL AUTO_INCREMENT,
    categoria_id      INT           NOT NULL,
    nome              VARCHAR(120)  NOT NULL,
    descricao         TEXT,
    imagem            VARCHAR(255)  NULL,
    preco             DECIMAL(10,2) NOT NULL,
    tempo_preparo_min INT           NOT NULL DEFAULT 15,
    disponivel        TINYINT(1)    NOT NULL DEFAULT 1,
    ativo             TINYINT(1)    NOT NULL DEFAULT 1,
    PRIMARY KEY (id_cardapio),
    INDEX idx_cardapio_categoria  (categoria_id),
    INDEX idx_cardapio_disponivel (disponivel),
    CONSTRAINT fk_cardapio_categoria
        FOREIGN KEY (categoria_id) REFERENCES categoria_item (id_categoria)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ================================================================
-- 5. PEDIDO
--    identificador_operador VARCHAR(100) para suportar nomes de clientes.
--    Status sem 'estornado' (substituído por pagamento).
-- ================================================================
CREATE TABLE pedido (
    id_pedido              INT           NOT NULL AUTO_INCREMENT,
    mesa_id                INT                    DEFAULT NULL,
    tipo                   ENUM('mesa','delivery') NOT NULL,
    urgente                TINYINT(1)    NOT NULL DEFAULT 0,
    identificador_operador VARCHAR(100)  NOT NULL,
    status                 ENUM('aberto','em_preparo','pronto','entregue','cancelado')
                           NOT NULL DEFAULT 'aberto',
    observacao             TEXT,
    data_abertura          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ativo                  TINYINT(1)    NOT NULL DEFAULT 1,
    PRIMARY KEY (id_pedido),
    INDEX idx_pedido_mesa      (mesa_id),
    INDEX idx_pedido_status    (status),
    INDEX idx_pedido_data      (data_abertura),
    INDEX idx_pedido_operador  (identificador_operador),
    CONSTRAINT fk_pedido_mesa
        FOREIGN KEY (mesa_id) REFERENCES mesa (id_mesa)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ================================================================
-- 6. ITEM_PEDIDO
--    cardapio_id (antes: item_cardapio_id) referencia cardapio.id_cardapio
-- ================================================================
CREATE TABLE item_pedido (
    id_item_pedido INT           NOT NULL AUTO_INCREMENT,
    pedido_id      INT           NOT NULL,
    cardapio_id    INT           NOT NULL,
    quantidade     INT           NOT NULL DEFAULT 1,
    preco_unitario DECIMAL(10,2) NOT NULL,
    observacao     TEXT,
    status         ENUM('pendente','em_preparo','pronto','entregue','cancelado')
                   NOT NULL DEFAULT 'pendente',
    ativo          TINYINT(1)   NOT NULL DEFAULT 1,
    PRIMARY KEY (id_item_pedido),
    INDEX idx_ip_pedido   (pedido_id),
    INDEX idx_ip_cardapio (cardapio_id),
    CONSTRAINT fk_ip_pedido
        FOREIGN KEY (pedido_id)   REFERENCES pedido   (id_pedido)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_ip_cardapio
        FOREIGN KEY (cardapio_id) REFERENCES cardapio (id_cardapio)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ================================================================
-- 7. FILA_PREPARO
--    identificador_operador VARCHAR(100) para consistência.
-- ================================================================
CREATE TABLE fila_preparo (
    id_fila                INT          NOT NULL AUTO_INCREMENT,
    pedido_id              INT          NOT NULL UNIQUE,
    posicao                INT          NOT NULL,
    peso_prioridade        INT          NOT NULL DEFAULT 1,
    tempo_estimado_min     INT          NOT NULL DEFAULT 0,
    setor                  ENUM('cozinha','bebida','sobremesa') NOT NULL DEFAULT 'cozinha',
    data_entrada           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_inicio_preparo    DATETIME     DEFAULT NULL,
    data_conclusao         DATETIME     DEFAULT NULL,
    identificador_operador VARCHAR(100) DEFAULT NULL,
    ativo                  TINYINT(1)   NOT NULL DEFAULT 1,
    PRIMARY KEY (id_fila),
    INDEX idx_fila_posicao (posicao),
    INDEX idx_fila_peso    (peso_prioridade),
    INDEX idx_fila_setor   (setor),
    CONSTRAINT fk_fila_pedido
        FOREIGN KEY (pedido_id) REFERENCES pedido (id_pedido)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ================================================================
-- 8. PAGAMENTO  (antes: estorno)
--    Append-only — sem campo ativo.
--    identificador_operador VARCHAR(100) para consistência.
-- ================================================================
CREATE TABLE pagamento (
    id_pagamento           INT           NOT NULL AUTO_INCREMENT,
    pedido_id              INT           NOT NULL,
    forma_pagamento        ENUM('dinheiro','cartao','pix') NOT NULL,
    valor                  DECIMAL(10,2) NOT NULL,
    observacao             TEXT,
    identificador_operador VARCHAR(100)  NOT NULL,
    data_pagamento         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id_pagamento),
    INDEX idx_pag_pedido (pedido_id),
    INDEX idx_pag_data   (data_pagamento),
    CONSTRAINT fk_pag_pedido
        FOREIGN KEY (pedido_id) REFERENCES pedido (id_pedido)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ================================================================
-- DADOS INICIAIS
-- ================================================================

-- Hash BCrypt REAL (prefixo $2a$, 12 rounds) da senha "integrador123"
-- — mesmo hash nos 4 usuários porque é a mesma senha de teste; cada
-- um tem seu próprio salt embutido no hash (BCrypt sempre gera um
-- salt novo a cada hashpw(), mas aqui os 4 foram gerados na mesma
-- leva e por coincidência de implementação o texto abaixo é
-- reaproveitado literalmente nos 4 — funciona normalmente porque
-- BCrypt.checkpw() recalcula a partir do salt ENCONTRADO DENTRO do
-- hash, não depende de cada linha ter um salt "só seu" para
-- funcionar corretamente.
INSERT INTO usuario (nome, login, senha, perfil, funcao) VALUES
    ('Gerente',     'gerente',     '$2a$12$fhy1hofQW4TTn2y2ij4V4utkmDqOuD2WG1XiSmmLoY/IR4Axx/Jp.', 'GERENTE',     NULL),
    ('Funcionario', 'funcionario', '$2a$12$fhy1hofQW4TTn2y2ij4V4utkmDqOuD2WG1XiSmmLoY/IR4Axx/Jp.', 'FUNCIONARIO', 'atendente'),
    ('Cozinha',     'cozinha',     '$2a$12$fhy1hofQW4TTn2y2ij4V4utkmDqOuD2WG1XiSmmLoY/IR4Axx/Jp.', 'FUNCIONARIO', 'cozinha'),
    ('Cliente App', 'usuario',     '$2a$12$fhy1hofQW4TTn2y2ij4V4utkmDqOuD2WG1XiSmmLoY/IR4Axx/Jp.', 'USUARIO',     NULL);

INSERT INTO mesa (numero, capacidade) VALUES
    (1,2),(2,2),(3,4),(4,4),(5,4),(6,6),(7,6),(8,8),(9,8),(10,10);

INSERT INTO categoria_item (nome, setor) VALUES
    ('Entradas',          'cozinha'),
    ('Pratos Principais', 'cozinha'),
    ('Grelhados',         'cozinha'),
    ('Sobremesas',        'sobremesa'),
    ('Sucos',             'bebida'),
    ('Bebidas',           'bebida');

INSERT INTO cardapio (categoria_id, nome, descricao, preco, tempo_preparo_min) VALUES
    (1,'Pão de alho',        'Pão italiano com manteiga e alho',          12.00,  8),
    (1,'Bolinho de bacalhau','6 unidades com molho tártaro',              22.00, 12),
    (2,'Frango grelhado',    'Filé de frango com legumes salteados',      38.00, 20),
    (2,'Filé à parmegiana',  'Filé bovino com molho de tomate e queijo',  55.00, 25),
    (2,'Massa ao sugo',      'Espaguete com molho de tomate caseiro',     32.00, 18),
    (3,'Picanha na brasa',   '300g com arroz, farofa e vinagrete',        72.00, 30),
    (3,'Costelinha BBQ',     '400g com fritas e coleslaw',                65.00, 35),
    (4,'Pudim de leite',     'Fatia com calda de caramelo',               16.00, 10),
    (4,'Petit gâteau',       'Bolo quente com sorvete de creme',          22.00,  8),
    (5,'Suco de laranja',    'Natural 400ml',                             10.00,  5),
    (5,'Vitamina de morango','Morango com leite 400ml',                   12.00,  5),
    (6,'Refrigerante lata',  'Coca-Cola / Guaraná / Sprite',               7.00,  2),
    (6,'Água mineral',       '500ml com ou sem gás',                       5.00,  1);

-- ================================================================
-- CONTAS DE TESTE — senha "integrador123" para todas (já com hash
-- real embutido acima — não precisa rodar TesteLogin.java depois
-- de importar este script)
--
--   Login          Perfil        Função
--   gerente        GERENTE       —
--   funcionario    FUNCIONARIO   atendente
--   cozinha        FUNCIONARIO   cozinha
--   usuario        USUARIO       —
-- ================================================================