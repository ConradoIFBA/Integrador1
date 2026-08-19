-- ================================================================
-- DADOS DE EXEMPLO PRÉ-GERADOS — Integrador
-- ================================================================
-- Gerado para rodar em cima do banco base (integrador_v3_corrigido.sql).
-- Cobre 14 dias de histórico (até 2026-08-12), com pedidos,
-- itens, pagamentos e fila de preparo para os pedidos ainda ativos de hoje.
-- Não precisa rodar nenhuma procedure — são apenas INSERTs prontos,
-- é só importar este arquivo direto no MySQL Workbench (ou via
-- mysql -u root -p integrador < dados_exemplo_prontos.sql).
--
-- HOJE especificamente tem 6 pedidos de mesa GARANTIDOS como ativos
-- (2 aberto, 2 em preparo, 2 pronto, em mesas diferentes) + um
-- punhado de pedidos extras aleatórios — assim o Painel, as Mesas e
-- a Fila de Preparo sempre têm algo "ao vivo" para mostrar assim que
-- você loga, em vez de depender só da sorte do sorteio aleatório.
-- ================================================================

USE integrador;

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (5, 'mesa', 0, 'A2', 'entregue', '2026-07-30 14:08:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 10, 2, 10.00, 'entregue', 1);
INSERT INTO pagamento (pedido_id, forma_pagamento, valor, identificador_operador, data_pagamento) VALUES (@pid, 'dinheiro', 20.00, 'A2', '2026-07-30 14:29:00');

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (4, 'mesa', 0, 'C2', 'entregue', '2026-07-30 20:01:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 4, 2, 55.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 10, 2, 10.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 13, 1, 5.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 13, 1, 5.00, 'entregue', 1);
INSERT INTO pagamento (pedido_id, forma_pagamento, valor, identificador_operador, data_pagamento) VALUES (@pid, 'pix', 140.00, 'C2', '2026-07-30 20:48:00');

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (3, 'mesa', 1, 'A2', 'entregue', '2026-07-30 16:06:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 10, 2, 10.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 13, 1, 5.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 12, 2, 7.00, 'entregue', 1);
INSERT INTO pagamento (pedido_id, forma_pagamento, valor, identificador_operador, data_pagamento) VALUES (@pid, 'pix', 39.00, 'A2', '2026-07-30 16:33:00');

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (NULL, 'delivery', 0, 'Helena Alves', 'entregue', '2026-07-30 12:35:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 10, 1, 10.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 12, 1, 7.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 1, 3, 12.00, 'entregue', 1);
INSERT INTO pagamento (pedido_id, forma_pagamento, valor, identificador_operador, data_pagamento) VALUES (@pid, 'dinheiro', 53.00, 'Helena Alves', '2026-07-30 13:13:00');

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (NULL, 'delivery', 0, 'Tiago Ramos', 'entregue', '2026-07-30 14:55:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 3, 2, 38.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 6, 1, 72.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 11, 2, 12.00, 'entregue', 1);
INSERT INTO pagamento (pedido_id, forma_pagamento, valor, identificador_operador, data_pagamento) VALUES (@pid, 'pix', 172.00, 'Tiago Ramos', '2026-07-30 15:19:00');

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (3, 'mesa', 0, 'C2', 'entregue', '2026-07-30 22:15:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 11, 2, 12.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 13, 1, 5.00, 'entregue', 1);
INSERT INTO pagamento (pedido_id, forma_pagamento, valor, identificador_operador, data_pagamento) VALUES (@pid, 'dinheiro', 29.00, 'C2', '2026-07-30 22:37:00');

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (NULL, 'delivery', 0, 'Helena Alves', 'entregue', '2026-07-30 15:04:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 4, 3, 55.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 8, 2, 16.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 11, 2, 12.00, 'entregue', 1);
INSERT INTO pagamento (pedido_id, forma_pagamento, valor, identificador_operador, data_pagamento) VALUES (@pid, 'dinheiro', 221.00, 'Helena Alves', '2026-07-30 15:40:00');

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (9, 'mesa', 0, 'C2', 'entregue', '2026-07-30 15:47:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 6, 1, 72.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 3, 3, 38.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 8, 1, 16.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 13, 1, 5.00, 'entregue', 1);
INSERT INTO pagamento (pedido_id, forma_pagamento, valor, identificador_operador, data_pagamento) VALUES (@pid, 'dinheiro', 207.00, 'C2', '2026-07-30 16:16:00');

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (7, 'mesa', 0, 'C2', 'entregue', '2026-07-30 12:24:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 9, 1, 22.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 11, 3, 12.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 2, 3, 22.00, 'entregue', 1);
INSERT INTO pagamento (pedido_id, forma_pagamento, valor, identificador_operador, data_pagamento) VALUES (@pid, 'pix', 124.00, 'C2', '2026-07-30 13:01:00');

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (2, 'mesa', 0, 'A3', 'entregue', '2026-07-31 17:10:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 9, 1, 22.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 9, 1, 22.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 11, 2, 12.00, 'entregue', 1);
INSERT INTO pagamento (pedido_id, forma_pagamento, valor, identificador_operador, data_pagamento) VALUES (@pid, 'pix', 68.00, 'A3', '2026-07-31 18:02:00');

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (3, 'mesa', 0, 'A3', 'cancelado', '2026-07-31 13:34:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 10, 2, 10.00, 'cancelado', 1);

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (2, 'mesa', 0, 'A3', 'entregue', '2026-07-31 15:15:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 2, 3, 22.00, 'entregue', 1);
INSERT INTO pagamento (pedido_id, forma_pagamento, valor, identificador_operador, data_pagamento) VALUES (@pid, 'cartao', 66.00, 'A3', '2026-07-31 15:39:00');

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (NULL, 'delivery', 0, 'Juliana Rocha', 'entregue', '2026-07-31 13:08:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 5, 3, 32.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 10, 2, 10.00, 'entregue', 1);
INSERT INTO pagamento (pedido_id, forma_pagamento, valor, identificador_operador, data_pagamento) VALUES (@pid, 'dinheiro', 116.00, 'Juliana Rocha', '2026-07-31 14:02:00');

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (NULL, 'delivery', 0, 'Rafael Nunes', 'entregue', '2026-07-31 14:45:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 8, 3, 16.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 8, 1, 16.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 4, 1, 55.00, 'entregue', 1);
INSERT INTO pagamento (pedido_id, forma_pagamento, valor, identificador_operador, data_pagamento) VALUES (@pid, 'dinheiro', 119.00, 'Rafael Nunes', '2026-07-31 15:26:00');

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (9, 'mesa', 0, 'A2', 'entregue', '2026-07-31 20:14:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 4, 1, 55.00, 'entregue', 1);
INSERT INTO pagamento (pedido_id, forma_pagamento, valor, identificador_operador, data_pagamento) VALUES (@pid, 'dinheiro', 55.00, 'A2', '2026-07-31 20:55:00');

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (4, 'mesa', 0, 'A3', 'entregue', '2026-07-31 21:31:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 4, 2, 55.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 13, 2, 5.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 4, 1, 55.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 2, 3, 22.00, 'entregue', 1);
INSERT INTO pagamento (pedido_id, forma_pagamento, valor, identificador_operador, data_pagamento) VALUES (@pid, 'cartao', 241.00, 'A3', '2026-07-31 22:13:00');

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (8, 'mesa', 1, 'A1', 'cancelado', '2026-07-31 21:41:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 12, 2, 7.00, 'cancelado', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 13, 1, 5.00, 'cancelado', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 4, 1, 55.00, 'cancelado', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 4, 3, 55.00, 'cancelado', 1);

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (7, 'mesa', 0, 'A2', 'entregue', '2026-07-31 15:29:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 13, 3, 5.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 2, 1, 22.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 11, 3, 12.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 1, 1, 12.00, 'entregue', 1);
INSERT INTO pagamento (pedido_id, forma_pagamento, valor, identificador_operador, data_pagamento) VALUES (@pid, 'dinheiro', 85.00, 'A2', '2026-07-31 15:59:00');

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (8, 'mesa', 0, 'A2', 'entregue', '2026-07-31 17:57:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 5, 2, 32.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 5, 2, 32.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 12, 3, 7.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 13, 3, 5.00, 'entregue', 1);
INSERT INTO pagamento (pedido_id, forma_pagamento, valor, identificador_operador, data_pagamento) VALUES (@pid, 'pix', 164.00, 'A2', '2026-07-31 18:48:00');

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (4, 'mesa', 0, 'A1', 'entregue', '2026-08-01 20:47:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 1, 3, 12.00, 'entregue', 1);
INSERT INTO pagamento (pedido_id, forma_pagamento, valor, identificador_operador, data_pagamento) VALUES (@pid, 'cartao', 36.00, 'A1', '2026-08-01 21:39:00');

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (NULL, 'delivery', 1, 'Juliana Rocha', 'cancelado', '2026-08-01 13:03:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 2, 3, 22.00, 'cancelado', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 2, 3, 22.00, 'cancelado', 1);

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (NULL, 'delivery', 0, 'Helena Alves', 'entregue', '2026-08-01 12:56:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 10, 1, 10.00, 'entregue', 1);
INSERT INTO pagamento (pedido_id, forma_pagamento, valor, identificador_operador, data_pagamento) VALUES (@pid, 'cartao', 10.00, 'Helena Alves', '2026-08-01 13:53:00');

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (6, 'mesa', 0, 'A3', 'entregue', '2026-08-01 14:42:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 3, 3, 38.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 11, 2, 12.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 8, 2, 16.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 13, 1, 5.00, 'entregue', 1);
INSERT INTO pagamento (pedido_id, forma_pagamento, valor, identificador_operador, data_pagamento) VALUES (@pid, 'dinheiro', 175.00, 'A3', '2026-08-01 15:31:00');

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (10, 'mesa', 0, 'A1', 'entregue', '2026-08-01 12:34:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 2, 1, 22.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 6, 2, 72.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 3, 2, 38.00, 'entregue', 1);
INSERT INTO pagamento (pedido_id, forma_pagamento, valor, identificador_operador, data_pagamento) VALUES (@pid, 'pix', 242.00, 'A1', '2026-08-01 13:13:00');

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (NULL, 'delivery', 0, 'Sofia Barros', 'entregue', '2026-08-02 21:33:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 11, 1, 12.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 3, 2, 38.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 2, 1, 22.00, 'entregue', 1);
INSERT INTO pagamento (pedido_id, forma_pagamento, valor, identificador_operador, data_pagamento) VALUES (@pid, 'pix', 110.00, 'Sofia Barros', '2026-08-02 22:28:00');

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (5, 'mesa', 0, 'C2', 'entregue', '2026-08-02 14:45:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 9, 2, 22.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 5, 1, 32.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 2, 3, 22.00, 'entregue', 1);
INSERT INTO pagamento (pedido_id, forma_pagamento, valor, identificador_operador, data_pagamento) VALUES (@pid, 'cartao', 142.00, 'C2', '2026-08-02 15:22:00');

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (6, 'mesa', 0, 'A2', 'entregue', '2026-08-02 21:16:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 9, 1, 22.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 2, 1, 22.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 12, 1, 7.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 9, 1, 22.00, 'entregue', 1);
INSERT INTO pagamento (pedido_id, forma_pagamento, valor, identificador_operador, data_pagamento) VALUES (@pid, 'cartao', 73.00, 'A2', '2026-08-02 22:13:00');

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (7, 'mesa', 0, 'A2', 'entregue', '2026-08-02 11:19:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 6, 1, 72.00, 'entregue', 1);
INSERT INTO pagamento (pedido_id, forma_pagamento, valor, identificador_operador, data_pagamento) VALUES (@pid, 'pix', 72.00, 'A2', '2026-08-02 11:54:00');

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (6, 'mesa', 0, 'C2', 'entregue', '2026-08-02 17:39:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 3, 1, 38.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 7, 1, 65.00, 'entregue', 1);
INSERT INTO pagamento (pedido_id, forma_pagamento, valor, identificador_operador, data_pagamento) VALUES (@pid, 'dinheiro', 103.00, 'C2', '2026-08-02 18:20:00');

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (NULL, 'delivery', 0, 'Helena Alves', 'entregue', '2026-08-02 21:55:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 13, 3, 5.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 2, 2, 22.00, 'entregue', 1);
INSERT INTO pagamento (pedido_id, forma_pagamento, valor, identificador_operador, data_pagamento) VALUES (@pid, 'dinheiro', 59.00, 'Helena Alves', '2026-08-02 22:45:00');

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (8, 'mesa', 0, 'A3', 'entregue', '2026-08-02 15:52:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 11, 1, 12.00, 'entregue', 1);
INSERT INTO pagamento (pedido_id, forma_pagamento, valor, identificador_operador, data_pagamento) VALUES (@pid, 'cartao', 12.00, 'A3', '2026-08-02 16:33:00');

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (2, 'mesa', 0, 'A3', 'entregue', '2026-08-02 16:41:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 1, 1, 12.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 5, 1, 32.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 10, 2, 10.00, 'entregue', 1);
INSERT INTO pagamento (pedido_id, forma_pagamento, valor, identificador_operador, data_pagamento) VALUES (@pid, 'dinheiro', 64.00, 'A3', '2026-08-02 17:07:00');

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (6, 'mesa', 0, 'C1', 'entregue', '2026-08-03 20:32:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 5, 1, 32.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 12, 2, 7.00, 'entregue', 1);
INSERT INTO pagamento (pedido_id, forma_pagamento, valor, identificador_operador, data_pagamento) VALUES (@pid, 'dinheiro', 46.00, 'C1', '2026-08-03 21:25:00');

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (NULL, 'delivery', 0, 'Juliana Rocha', 'cancelado', '2026-08-03 21:46:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 6, 2, 72.00, 'cancelado', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 2, 3, 22.00, 'cancelado', 1);

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (NULL, 'delivery', 0, 'Lucas Martins', 'entregue', '2026-08-03 16:42:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 9, 2, 22.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 11, 2, 12.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 6, 2, 72.00, 'entregue', 1);
INSERT INTO pagamento (pedido_id, forma_pagamento, valor, identificador_operador, data_pagamento) VALUES (@pid, 'pix', 212.00, 'Lucas Martins', '2026-08-03 17:20:00');

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (4, 'mesa', 0, 'C1', 'entregue', '2026-08-03 21:24:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 7, 3, 65.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 1, 2, 12.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 5, 1, 32.00, 'entregue', 1);
INSERT INTO pagamento (pedido_id, forma_pagamento, valor, identificador_operador, data_pagamento) VALUES (@pid, 'cartao', 251.00, 'C1', '2026-08-03 22:21:00');

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (6, 'mesa', 0, 'C1', 'entregue', '2026-08-03 18:28:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 11, 1, 12.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 5, 3, 32.00, 'entregue', 1);
INSERT INTO pagamento (pedido_id, forma_pagamento, valor, identificador_operador, data_pagamento) VALUES (@pid, 'pix', 108.00, 'C1', '2026-08-03 19:28:00');

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (2, 'mesa', 0, 'A2', 'entregue', '2026-08-03 21:19:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 1, 1, 12.00, 'entregue', 1);
INSERT INTO pagamento (pedido_id, forma_pagamento, valor, identificador_operador, data_pagamento) VALUES (@pid, 'cartao', 12.00, 'A2', '2026-08-03 22:18:00');

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (NULL, 'delivery', 0, 'Carlos Mendes', 'cancelado', '2026-08-03 18:26:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 8, 2, 16.00, 'cancelado', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 4, 1, 55.00, 'cancelado', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 11, 3, 12.00, 'cancelado', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 1, 1, 12.00, 'cancelado', 1);

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (NULL, 'delivery', 0, 'Diego Santos', 'cancelado', '2026-08-03 13:51:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 9, 1, 22.00, 'cancelado', 1);

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (3, 'mesa', 0, 'C1', 'cancelado', '2026-08-04 21:33:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 10, 3, 10.00, 'cancelado', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 9, 2, 22.00, 'cancelado', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 9, 2, 22.00, 'cancelado', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 3, 3, 38.00, 'cancelado', 1);

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (NULL, 'delivery', 0, 'Igor Pereira', 'entregue', '2026-08-04 15:48:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 11, 1, 12.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 5, 2, 32.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 2, 3, 22.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 5, 1, 32.00, 'entregue', 1);
INSERT INTO pagamento (pedido_id, forma_pagamento, valor, identificador_operador, data_pagamento) VALUES (@pid, 'cartao', 174.00, 'Igor Pereira', '2026-08-04 16:29:00');

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (9, 'mesa', 0, 'A1', 'entregue', '2026-08-04 13:09:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 2, 2, 22.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 7, 2, 65.00, 'entregue', 1);
INSERT INTO pagamento (pedido_id, forma_pagamento, valor, identificador_operador, data_pagamento) VALUES (@pid, 'pix', 174.00, 'A1', '2026-08-04 13:58:00');

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (4, 'mesa', 0, 'C1', 'entregue', '2026-08-04 17:57:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 13, 3, 5.00, 'entregue', 1);
INSERT INTO pagamento (pedido_id, forma_pagamento, valor, identificador_operador, data_pagamento) VALUES (@pid, 'cartao', 15.00, 'C1', '2026-08-04 18:47:00');

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (6, 'mesa', 0, 'A3', 'cancelado', '2026-08-04 17:54:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 8, 1, 16.00, 'cancelado', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 5, 2, 32.00, 'cancelado', 1);

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (7, 'mesa', 0, 'A3', 'entregue', '2026-08-04 21:43:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 3, 3, 38.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 9, 1, 22.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 7, 3, 65.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 10, 3, 10.00, 'entregue', 1);
INSERT INTO pagamento (pedido_id, forma_pagamento, valor, identificador_operador, data_pagamento) VALUES (@pid, 'dinheiro', 361.00, 'A3', '2026-08-04 22:08:00');

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (3, 'mesa', 0, 'C1', 'entregue', '2026-08-04 13:03:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 6, 2, 72.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 13, 2, 5.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 5, 2, 32.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 5, 1, 32.00, 'entregue', 1);
INSERT INTO pagamento (pedido_id, forma_pagamento, valor, identificador_operador, data_pagamento) VALUES (@pid, 'cartao', 250.00, 'C1', '2026-08-04 13:24:00');

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (NULL, 'delivery', 0, 'Ana Silva', 'entregue', '2026-08-04 16:14:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 13, 1, 5.00, 'entregue', 1);
INSERT INTO pagamento (pedido_id, forma_pagamento, valor, identificador_operador, data_pagamento) VALUES (@pid, 'dinheiro', 5.00, 'Ana Silva', '2026-08-04 16:46:00');

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (NULL, 'delivery', 0, 'Lucas Martins', 'entregue', '2026-08-04 13:15:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 8, 3, 16.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 5, 2, 32.00, 'entregue', 1);
INSERT INTO pagamento (pedido_id, forma_pagamento, valor, identificador_operador, data_pagamento) VALUES (@pid, 'dinheiro', 112.00, 'Lucas Martins', '2026-08-04 14:13:00');

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (2, 'mesa', 0, 'A2', 'entregue', '2026-08-04 15:06:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 7, 3, 65.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 4, 1, 55.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 10, 3, 10.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 11, 1, 12.00, 'entregue', 1);
INSERT INTO pagamento (pedido_id, forma_pagamento, valor, identificador_operador, data_pagamento) VALUES (@pid, 'dinheiro', 292.00, 'A2', '2026-08-04 15:45:00');

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (2, 'mesa', 0, 'C2', 'entregue', '2026-08-05 11:22:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 9, 3, 22.00, 'entregue', 1);
INSERT INTO pagamento (pedido_id, forma_pagamento, valor, identificador_operador, data_pagamento) VALUES (@pid, 'cartao', 66.00, 'C2', '2026-08-05 11:42:00');

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (NULL, 'delivery', 0, 'Tiago Ramos', 'entregue', '2026-08-05 18:06:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 12, 1, 7.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 7, 1, 65.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 12, 3, 7.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 11, 2, 12.00, 'entregue', 1);
INSERT INTO pagamento (pedido_id, forma_pagamento, valor, identificador_operador, data_pagamento) VALUES (@pid, 'pix', 117.00, 'Tiago Ramos', '2026-08-05 19:00:00');

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (NULL, 'delivery', 0, 'Igor Pereira', 'entregue', '2026-08-05 17:52:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 2, 2, 22.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 8, 1, 16.00, 'entregue', 1);
INSERT INTO pagamento (pedido_id, forma_pagamento, valor, identificador_operador, data_pagamento) VALUES (@pid, 'cartao', 60.00, 'Igor Pereira', '2026-08-05 18:48:00');

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (7, 'mesa', 0, 'A3', 'entregue', '2026-08-05 11:31:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 6, 2, 72.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 6, 2, 72.00, 'entregue', 1);
INSERT INTO pagamento (pedido_id, forma_pagamento, valor, identificador_operador, data_pagamento) VALUES (@pid, 'pix', 288.00, 'A3', '2026-08-05 12:08:00');

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (9, 'mesa', 0, 'A2', 'entregue', '2026-08-05 12:15:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 12, 2, 7.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 11, 3, 12.00, 'entregue', 1);
INSERT INTO pagamento (pedido_id, forma_pagamento, valor, identificador_operador, data_pagamento) VALUES (@pid, 'cartao', 50.00, 'A2', '2026-08-05 13:03:00');

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (NULL, 'delivery', 0, 'Carlos Mendes', 'entregue', '2026-08-05 15:14:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 8, 3, 16.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 9, 2, 22.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 7, 3, 65.00, 'entregue', 1);
INSERT INTO pagamento (pedido_id, forma_pagamento, valor, identificador_operador, data_pagamento) VALUES (@pid, 'pix', 287.00, 'Carlos Mendes', '2026-08-05 15:55:00');

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (8, 'mesa', 0, 'A3', 'entregue', '2026-08-05 15:16:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 2, 3, 22.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 9, 3, 22.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 3, 1, 38.00, 'entregue', 1);
INSERT INTO pagamento (pedido_id, forma_pagamento, valor, identificador_operador, data_pagamento) VALUES (@pid, 'dinheiro', 170.00, 'A3', '2026-08-05 16:06:00');

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (10, 'mesa', 0, 'C2', 'cancelado', '2026-08-05 20:18:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 4, 2, 55.00, 'cancelado', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 3, 2, 38.00, 'cancelado', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 1, 3, 12.00, 'cancelado', 1);

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (5, 'mesa', 0, 'A1', 'entregue', '2026-08-05 11:35:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 2, 1, 22.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 10, 2, 10.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 8, 2, 16.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 8, 2, 16.00, 'entregue', 1);
INSERT INTO pagamento (pedido_id, forma_pagamento, valor, identificador_operador, data_pagamento) VALUES (@pid, 'dinheiro', 106.00, 'A1', '2026-08-05 11:58:00');

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (8, 'mesa', 0, 'A1', 'entregue', '2026-08-05 12:25:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 3, 1, 38.00, 'entregue', 1);
INSERT INTO pagamento (pedido_id, forma_pagamento, valor, identificador_operador, data_pagamento) VALUES (@pid, 'pix', 38.00, 'A1', '2026-08-05 13:04:00');

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (NULL, 'delivery', 0, 'Carlos Mendes', 'entregue', '2026-08-06 19:48:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 13, 3, 5.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 7, 2, 65.00, 'entregue', 1);
INSERT INTO pagamento (pedido_id, forma_pagamento, valor, identificador_operador, data_pagamento) VALUES (@pid, 'cartao', 145.00, 'Carlos Mendes', '2026-08-06 20:27:00');

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (NULL, 'delivery', 0, 'Helena Alves', 'entregue', '2026-08-06 15:36:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 13, 1, 5.00, 'entregue', 1);
INSERT INTO pagamento (pedido_id, forma_pagamento, valor, identificador_operador, data_pagamento) VALUES (@pid, 'pix', 5.00, 'Helena Alves', '2026-08-06 16:09:00');

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (2, 'mesa', 0, 'A2', 'entregue', '2026-08-06 14:11:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 8, 3, 16.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 10, 2, 10.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 5, 1, 32.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 4, 2, 55.00, 'entregue', 1);
INSERT INTO pagamento (pedido_id, forma_pagamento, valor, identificador_operador, data_pagamento) VALUES (@pid, 'pix', 210.00, 'A2', '2026-08-06 14:49:00');

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (NULL, 'delivery', 0, 'Igor Pereira', 'entregue', '2026-08-06 12:43:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 7, 1, 65.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 9, 1, 22.00, 'entregue', 1);
INSERT INTO pagamento (pedido_id, forma_pagamento, valor, identificador_operador, data_pagamento) VALUES (@pid, 'pix', 87.00, 'Igor Pereira', '2026-08-06 13:12:00');

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (NULL, 'delivery', 0, 'Carlos Mendes', 'entregue', '2026-08-07 11:10:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 8, 1, 16.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 8, 3, 16.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 5, 3, 32.00, 'entregue', 1);
INSERT INTO pagamento (pedido_id, forma_pagamento, valor, identificador_operador, data_pagamento) VALUES (@pid, 'cartao', 160.00, 'Carlos Mendes', '2026-08-07 11:47:00');

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (8, 'mesa', 0, 'C1', 'entregue', '2026-08-07 12:38:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 10, 2, 10.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 1, 1, 12.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 4, 3, 55.00, 'entregue', 1);
INSERT INTO pagamento (pedido_id, forma_pagamento, valor, identificador_operador, data_pagamento) VALUES (@pid, 'pix', 197.00, 'C1', '2026-08-07 13:35:00');

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (NULL, 'delivery', 1, 'Sofia Barros', 'entregue', '2026-08-07 21:52:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 8, 3, 16.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 11, 2, 12.00, 'entregue', 1);
INSERT INTO pagamento (pedido_id, forma_pagamento, valor, identificador_operador, data_pagamento) VALUES (@pid, 'cartao', 72.00, 'Sofia Barros', '2026-08-07 22:23:00');

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (NULL, 'delivery', 1, 'Helena Alves', 'entregue', '2026-08-07 21:52:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 7, 2, 65.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 6, 3, 72.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 2, 1, 22.00, 'entregue', 1);
INSERT INTO pagamento (pedido_id, forma_pagamento, valor, identificador_operador, data_pagamento) VALUES (@pid, 'cartao', 368.00, 'Helena Alves', '2026-08-07 22:38:00');

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (5, 'mesa', 0, 'C1', 'entregue', '2026-08-07 19:02:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 2, 2, 22.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 9, 1, 22.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 11, 3, 12.00, 'entregue', 1);
INSERT INTO pagamento (pedido_id, forma_pagamento, valor, identificador_operador, data_pagamento) VALUES (@pid, 'cartao', 102.00, 'C1', '2026-08-07 19:48:00');

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (9, 'mesa', 0, 'A3', 'entregue', '2026-08-07 20:48:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 4, 2, 55.00, 'entregue', 1);
INSERT INTO pagamento (pedido_id, forma_pagamento, valor, identificador_operador, data_pagamento) VALUES (@pid, 'pix', 110.00, 'A3', '2026-08-07 21:16:00');

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (8, 'mesa', 0, 'A1', 'entregue', '2026-08-08 11:40:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 5, 3, 32.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 1, 3, 12.00, 'entregue', 1);
INSERT INTO pagamento (pedido_id, forma_pagamento, valor, identificador_operador, data_pagamento) VALUES (@pid, 'cartao', 132.00, 'A1', '2026-08-08 12:05:00');

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (2, 'mesa', 0, 'C1', 'entregue', '2026-08-08 12:41:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 9, 3, 22.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 5, 2, 32.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 8, 2, 16.00, 'entregue', 1);
INSERT INTO pagamento (pedido_id, forma_pagamento, valor, identificador_operador, data_pagamento) VALUES (@pid, 'dinheiro', 162.00, 'C1', '2026-08-08 13:30:00');

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (7, 'mesa', 0, 'A2', 'entregue', '2026-08-08 20:32:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 5, 2, 32.00, 'entregue', 1);
INSERT INTO pagamento (pedido_id, forma_pagamento, valor, identificador_operador, data_pagamento) VALUES (@pid, 'cartao', 64.00, 'A2', '2026-08-08 21:24:00');

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (1, 'mesa', 0, 'A3', 'entregue', '2026-08-08 22:19:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 3, 2, 38.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 9, 2, 22.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 6, 2, 72.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 9, 3, 22.00, 'entregue', 1);
INSERT INTO pagamento (pedido_id, forma_pagamento, valor, identificador_operador, data_pagamento) VALUES (@pid, 'cartao', 330.00, 'A3', '2026-08-08 23:08:00');

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (NULL, 'delivery', 0, 'Tiago Ramos', 'entregue', '2026-08-08 14:44:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 1, 2, 12.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 12, 2, 7.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 12, 2, 7.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 7, 3, 65.00, 'entregue', 1);
INSERT INTO pagamento (pedido_id, forma_pagamento, valor, identificador_operador, data_pagamento) VALUES (@pid, 'pix', 247.00, 'Tiago Ramos', '2026-08-08 15:13:00');

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (1, 'mesa', 1, 'A2', 'entregue', '2026-08-08 19:37:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 2, 3, 22.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 8, 1, 16.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 12, 1, 7.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 7, 3, 65.00, 'entregue', 1);
INSERT INTO pagamento (pedido_id, forma_pagamento, valor, identificador_operador, data_pagamento) VALUES (@pid, 'dinheiro', 284.00, 'A2', '2026-08-08 20:01:00');

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (NULL, 'delivery', 0, 'Fernanda Lima', 'entregue', '2026-08-09 16:39:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 11, 3, 12.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 7, 2, 65.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 11, 3, 12.00, 'entregue', 1);
INSERT INTO pagamento (pedido_id, forma_pagamento, valor, identificador_operador, data_pagamento) VALUES (@pid, 'cartao', 202.00, 'Fernanda Lima', '2026-08-09 17:33:00');

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (2, 'mesa', 0, 'A2', 'cancelado', '2026-08-09 21:43:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 7, 1, 65.00, 'cancelado', 1);

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (NULL, 'delivery', 0, 'Rafael Nunes', 'entregue', '2026-08-09 12:28:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 1, 2, 12.00, 'entregue', 1);
INSERT INTO pagamento (pedido_id, forma_pagamento, valor, identificador_operador, data_pagamento) VALUES (@pid, 'dinheiro', 24.00, 'Rafael Nunes', '2026-08-09 13:06:00');

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (7, 'mesa', 0, 'A2', 'entregue', '2026-08-09 14:33:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 3, 1, 38.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 2, 3, 22.00, 'entregue', 1);
INSERT INTO pagamento (pedido_id, forma_pagamento, valor, identificador_operador, data_pagamento) VALUES (@pid, 'cartao', 104.00, 'A2', '2026-08-09 15:32:00');

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (8, 'mesa', 0, 'C2', 'entregue', '2026-08-09 13:14:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 11, 1, 12.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 13, 2, 5.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 5, 3, 32.00, 'entregue', 1);
INSERT INTO pagamento (pedido_id, forma_pagamento, valor, identificador_operador, data_pagamento) VALUES (@pid, 'pix', 118.00, 'C2', '2026-08-09 13:44:00');

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (6, 'mesa', 0, 'C2', 'cancelado', '2026-08-09 15:40:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 5, 1, 32.00, 'cancelado', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 7, 2, 65.00, 'cancelado', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 2, 1, 22.00, 'cancelado', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 7, 3, 65.00, 'cancelado', 1);

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (5, 'mesa', 0, 'A3', 'entregue', '2026-08-09 11:53:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 10, 3, 10.00, 'entregue', 1);
INSERT INTO pagamento (pedido_id, forma_pagamento, valor, identificador_operador, data_pagamento) VALUES (@pid, 'cartao', 30.00, 'A3', '2026-08-09 12:31:00');

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (NULL, 'delivery', 0, 'Lucas Martins', 'entregue', '2026-08-10 16:14:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 11, 1, 12.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 11, 3, 12.00, 'entregue', 1);
INSERT INTO pagamento (pedido_id, forma_pagamento, valor, identificador_operador, data_pagamento) VALUES (@pid, 'dinheiro', 48.00, 'Lucas Martins', '2026-08-10 16:53:00');

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (NULL, 'delivery', 1, 'Ana Silva', 'entregue', '2026-08-10 20:23:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 6, 3, 72.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 7, 1, 65.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 4, 1, 55.00, 'entregue', 1);
INSERT INTO pagamento (pedido_id, forma_pagamento, valor, identificador_operador, data_pagamento) VALUES (@pid, 'pix', 336.00, 'Ana Silva', '2026-08-10 21:06:00');

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (5, 'mesa', 0, 'A2', 'entregue', '2026-08-10 15:58:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 12, 2, 7.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 13, 1, 5.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 8, 1, 16.00, 'entregue', 1);
INSERT INTO pagamento (pedido_id, forma_pagamento, valor, identificador_operador, data_pagamento) VALUES (@pid, 'dinheiro', 35.00, 'A2', '2026-08-10 16:32:00');

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (NULL, 'delivery', 0, 'Rafael Nunes', 'cancelado', '2026-08-10 21:25:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 2, 2, 22.00, 'cancelado', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 1, 2, 12.00, 'cancelado', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 9, 1, 22.00, 'cancelado', 1);

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (5, 'mesa', 0, 'C2', 'entregue', '2026-08-10 17:52:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 8, 1, 16.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 10, 3, 10.00, 'entregue', 1);
INSERT INTO pagamento (pedido_id, forma_pagamento, valor, identificador_operador, data_pagamento) VALUES (@pid, 'cartao', 46.00, 'C2', '2026-08-10 18:51:00');

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (2, 'mesa', 1, 'C1', 'entregue', '2026-08-10 22:19:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 1, 2, 12.00, 'entregue', 1);
INSERT INTO pagamento (pedido_id, forma_pagamento, valor, identificador_operador, data_pagamento) VALUES (@pid, 'cartao', 24.00, 'C1', '2026-08-10 22:46:00');

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (9, 'mesa', 0, 'A2', 'entregue', '2026-08-10 17:29:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 10, 3, 10.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 12, 1, 7.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 7, 3, 65.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 2, 2, 22.00, 'entregue', 1);
INSERT INTO pagamento (pedido_id, forma_pagamento, valor, identificador_operador, data_pagamento) VALUES (@pid, 'pix', 276.00, 'A2', '2026-08-10 18:15:00');

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (NULL, 'delivery', 0, 'Fernanda Lima', 'entregue', '2026-08-10 11:44:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 6, 1, 72.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 11, 2, 12.00, 'entregue', 1);
INSERT INTO pagamento (pedido_id, forma_pagamento, valor, identificador_operador, data_pagamento) VALUES (@pid, 'pix', 96.00, 'Fernanda Lima', '2026-08-10 12:26:00');

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (5, 'mesa', 1, 'A2', 'entregue', '2026-08-10 12:54:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 11, 3, 12.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 10, 1, 10.00, 'entregue', 1);
INSERT INTO pagamento (pedido_id, forma_pagamento, valor, identificador_operador, data_pagamento) VALUES (@pid, 'dinheiro', 46.00, 'A2', '2026-08-10 13:35:00');

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (3, 'mesa', 0, 'C2', 'entregue', '2026-08-10 14:04:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 4, 2, 55.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 13, 1, 5.00, 'entregue', 1);
INSERT INTO pagamento (pedido_id, forma_pagamento, valor, identificador_operador, data_pagamento) VALUES (@pid, 'pix', 115.00, 'C2', '2026-08-10 14:24:00');

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (NULL, 'delivery', 0, 'Beatriz Costa', 'entregue', '2026-08-11 13:34:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 3, 1, 38.00, 'entregue', 1);
INSERT INTO pagamento (pedido_id, forma_pagamento, valor, identificador_operador, data_pagamento) VALUES (@pid, 'cartao', 38.00, 'Beatriz Costa', '2026-08-11 14:09:00');

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (1, 'mesa', 0, 'A2', 'entregue', '2026-08-11 15:03:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 12, 1, 7.00, 'entregue', 1);
INSERT INTO pagamento (pedido_id, forma_pagamento, valor, identificador_operador, data_pagamento) VALUES (@pid, 'cartao', 7.00, 'A2', '2026-08-11 15:51:00');

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (NULL, 'delivery', 0, 'Juliana Rocha', 'entregue', '2026-08-11 20:06:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 12, 3, 7.00, 'entregue', 1);
INSERT INTO pagamento (pedido_id, forma_pagamento, valor, identificador_operador, data_pagamento) VALUES (@pid, 'pix', 21.00, 'Juliana Rocha', '2026-08-11 20:45:00');

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (1, 'mesa', 0, 'A1', 'entregue', '2026-08-11 18:54:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 12, 2, 7.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 2, 1, 22.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 6, 3, 72.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 3, 1, 38.00, 'entregue', 1);
INSERT INTO pagamento (pedido_id, forma_pagamento, valor, identificador_operador, data_pagamento) VALUES (@pid, 'dinheiro', 290.00, 'A1', '2026-08-11 19:31:00');

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (10, 'mesa', 0, 'C2', 'entregue', '2026-08-11 22:20:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 8, 3, 16.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 10, 2, 10.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 2, 3, 22.00, 'entregue', 1);
INSERT INTO pagamento (pedido_id, forma_pagamento, valor, identificador_operador, data_pagamento) VALUES (@pid, 'dinheiro', 134.00, 'C2', '2026-08-11 23:15:00');

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (NULL, 'delivery', 0, 'Diego Santos', 'cancelado', '2026-08-11 17:28:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 7, 2, 65.00, 'cancelado', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 12, 1, 7.00, 'cancelado', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 6, 2, 72.00, 'cancelado', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 6, 3, 72.00, 'cancelado', 1);

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (5, 'mesa', 0, 'A1', 'aberto', '2026-08-12 12:05:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 13, 1, 5.00, 'pendente', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 9, 1, 22.00, 'pendente', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 10, 3, 10.00, 'pendente', 1);
INSERT INTO fila_preparo (pedido_id, posicao, peso_prioridade, tempo_estimado_min, setor, data_entrada, data_inicio_preparo, ativo, identificador_operador) VALUES (@pid, 1, 1, 27, 'bebida', '2026-08-12 12:05:00', NULL, 1, NULL);

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (6, 'mesa', 0, 'A3', 'aberto', '2026-08-12 21:07:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 12, 1, 7.00, 'pendente', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 5, 3, 32.00, 'pendente', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 5, 2, 32.00, 'pendente', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 2, 3, 22.00, 'pendente', 1);
INSERT INTO fila_preparo (pedido_id, posicao, peso_prioridade, tempo_estimado_min, setor, data_entrada, data_inicio_preparo, ativo, identificador_operador) VALUES (@pid, 1, 1, 26, 'cozinha', '2026-08-12 21:07:00', NULL, 1, NULL);

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (3, 'mesa', 0, 'A2', 'em_preparo', '2026-08-12 13:42:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 6, 3, 72.00, 'em_preparo', 1);
INSERT INTO fila_preparo (pedido_id, posicao, peso_prioridade, tempo_estimado_min, setor, data_entrada, data_inicio_preparo, ativo, identificador_operador) VALUES (@pid, 2, 1, 21, 'cozinha', '2026-08-12 13:42:00', '2026-08-12 13:45:00', 1, 'A2');

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (9, 'mesa', 0, 'A1', 'em_preparo', '2026-08-12 15:36:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 9, 3, 22.00, 'em_preparo', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 10, 3, 10.00, 'em_preparo', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 11, 3, 12.00, 'em_preparo', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 1, 3, 12.00, 'em_preparo', 1);
INSERT INTO fila_preparo (pedido_id, posicao, peso_prioridade, tempo_estimado_min, setor, data_entrada, data_inicio_preparo, ativo, identificador_operador) VALUES (@pid, 3, 1, 18, 'cozinha', '2026-08-12 15:36:00', '2026-08-12 15:39:00', 1, 'A1');

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (4, 'mesa', 0, 'A1', 'pronto', '2026-08-12 13:17:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 6, 2, 72.00, 'pronto', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 1, 1, 12.00, 'pronto', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 3, 3, 38.00, 'pronto', 1);
INSERT INTO fila_preparo (pedido_id, posicao, peso_prioridade, tempo_estimado_min, setor, data_entrada, data_inicio_preparo, ativo, identificador_operador) VALUES (@pid, 4, 1, 22, 'cozinha', '2026-08-12 13:17:00', '2026-08-12 13:20:00', 1, 'A1');

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (1, 'mesa', 0, 'A1', 'pronto', '2026-08-12 13:47:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 2, 3, 22.00, 'pronto', 1);
INSERT INTO fila_preparo (pedido_id, posicao, peso_prioridade, tempo_estimado_min, setor, data_entrada, data_inicio_preparo, ativo, identificador_operador) VALUES (@pid, 5, 1, 26, 'cozinha', '2026-08-12 13:47:00', '2026-08-12 13:50:00', 1, 'A1');

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (8, 'mesa', 0, 'A3', 'pronto', '2026-08-12 13:23:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 1, 1, 12.00, 'pronto', 1);
INSERT INTO fila_preparo (pedido_id, posicao, peso_prioridade, tempo_estimado_min, setor, data_entrada, data_inicio_preparo, ativo, identificador_operador) VALUES (@pid, 6, 1, 15, 'cozinha', '2026-08-12 13:23:00', '2026-08-12 13:26:00', 1, 'A3');

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (NULL, 'delivery', 0, 'Ana Silva', 'em_preparo', '2026-08-12 21:05:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 10, 2, 10.00, 'em_preparo', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 7, 2, 65.00, 'em_preparo', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 4, 3, 55.00, 'em_preparo', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 2, 2, 22.00, 'em_preparo', 1);
INSERT INTO fila_preparo (pedido_id, posicao, peso_prioridade, tempo_estimado_min, setor, data_entrada, data_inicio_preparo, ativo, identificador_operador) VALUES (@pid, 7, 1, 23, 'cozinha', '2026-08-12 21:05:00', '2026-08-12 21:08:00', 1, 'Ana Silva');

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (10, 'mesa', 0, 'C1', 'pronto', '2026-08-12 19:42:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 1, 1, 12.00, 'pronto', 1);
INSERT INTO fila_preparo (pedido_id, posicao, peso_prioridade, tempo_estimado_min, setor, data_entrada, data_inicio_preparo, ativo, identificador_operador) VALUES (@pid, 8, 1, 19, 'cozinha', '2026-08-12 19:42:00', '2026-08-12 19:45:00', 1, 'C1');

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (NULL, 'delivery', 0, 'Sofia Barros', 'em_preparo', '2026-08-12 13:48:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 8, 3, 16.00, 'em_preparo', 1);
INSERT INTO fila_preparo (pedido_id, posicao, peso_prioridade, tempo_estimado_min, setor, data_entrada, data_inicio_preparo, ativo, identificador_operador) VALUES (@pid, 1, 1, 23, 'sobremesa', '2026-08-12 13:48:00', '2026-08-12 13:51:00', 1, 'Sofia Barros');

INSERT INTO pedido (mesa_id, tipo, urgente, identificador_operador, status, data_abertura) VALUES (7, 'mesa', 0, 'C2', 'entregue', '2026-08-12 22:14:00');
SET @pid = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 2, 2, 22.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 12, 1, 7.00, 'entregue', 1);
INSERT INTO item_pedido (pedido_id, cardapio_id, quantidade, preco_unitario, status, ativo) VALUES (@pid, 7, 1, 65.00, 'entregue', 1);
INSERT INTO pagamento (pedido_id, forma_pagamento, valor, identificador_operador, data_pagamento) VALUES (@pid, 'cartao', 116.00, 'C2', '2026-08-12 22:38:00');

-- ================================================================
-- Reflete pedidos ativos de hoje no status das mesas
-- ================================================================
UPDATE mesa SET status='ocupada', operador='A1', data_status='2026-08-12 12:05:00' WHERE id_mesa=5 AND status='livre';
UPDATE mesa SET status='ocupada', operador='A3', data_status='2026-08-12 21:07:00' WHERE id_mesa=6 AND status='livre';
UPDATE mesa SET status='ocupada', operador='A2', data_status='2026-08-12 13:42:00' WHERE id_mesa=3 AND status='livre';
UPDATE mesa SET status='ocupada', operador='A1', data_status='2026-08-12 15:36:00' WHERE id_mesa=9 AND status='livre';
UPDATE mesa SET status='ocupada', operador='A1', data_status='2026-08-12 13:17:00' WHERE id_mesa=4 AND status='livre';
UPDATE mesa SET status='ocupada', operador='A1', data_status='2026-08-12 13:47:00' WHERE id_mesa=1 AND status='livre';
UPDATE mesa SET status='ocupada', operador='A3', data_status='2026-08-12 13:23:00' WHERE id_mesa=8 AND status='livre';
UPDATE mesa SET status='ocupada', operador='C1', data_status='2026-08-12 19:42:00' WHERE id_mesa=10 AND status='livre';

-- ================================================================
-- RESUMO: 110 pedidos | 275 itens | 87 pagamentos | 10 entradas na fila | 8 mesas marcadas como ocupadas
-- ================================================================