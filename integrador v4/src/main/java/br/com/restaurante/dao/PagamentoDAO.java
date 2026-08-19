package br.com.restaurante.dao;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import br.com.restaurante.model.Pagamento;

/**
 * ================================================================
 * PAGAMENTO DAO - Acesso à tabela "pagamento" (schema v3)
 * ================================================================
 *
 * PROPÓSITO:
 * Centraliza o SQL da tabela "pagamento" — que, no schema v2/v3,
 * substituiu a antiga tabela "estorno" (ver comentário de topo do
 * integrador_v3.sql: "estorno RENOMEADA → pagamento"). Em vez de
 * registrar reversões, o sistema agora registra POSITIVAMENTE como
 * cada pedido foi pago, permitindo inclusive múltiplas formas de
 * pagamento para o mesmo pedido (ex: metade em dinheiro, metade no
 * cartão — split de conta).
 *
 * ⚠️ NOTA SOBRE O SCHEMA V3:
 * identificador_operador também foi ampliado para VARCHAR(100) nesta
 * tabela (antes VARCHAR(20)) — mesma mudança aplicada a pedido e
 * fila_preparo. Nenhum ajuste de código foi necessário aqui.
 *
 * TABELA É APPEND-ONLY (SOMENTE INSERT E SELECT):
 * Diferente de praticamente todas as outras tabelas do sistema,
 * "pagamento" NÃO tem coluna "ativo" e NÃO tem método de
 * edição/exclusão neste DAO — uma vez registrado, um pagamento nunca
 * é alterado nem removido. Isso é proposital: pagamento é um registro
 * financeiro, e alterá-lo/apagá-lo retroativamente comprometeria a
 * confiabilidade do histórico. Se um pagamento estiver errado, a
 * correção esperada é via um NOVO registro (ex: complementando o
 * valor faltante), não editando o antigo.
 *
 * TABELA: pagamento
 * Schema (ver integrador_v3.sql):
 * - id_pagamento           (PK, AUTO_INCREMENT)
 * - pedido_id               (FK → pedido.id_pedido)
 * - forma_pagamento         (ENUM: dinheiro, cartao, pix)
 * - valor                   (DECIMAL 10,2)
 * - observacao
 * - identificador_operador  (VARCHAR(100) — quem registrou o pagamento)
 * - data_pagamento          (DATETIME, default CURRENT_TIMESTAMP)
 *
 * MÉTODOS DISPONÍVEIS:
 * - registrar(pagamento)         → INSERT (única forma de escrita)
 * - listarPorPedido(pedidoId)    → todos os pagamentos de um pedido, em ordem cronológica
 * - somarPorPedido(pedidoId)     → soma total já paga (usado para saber quanto falta)
 *
 * QUEM USA ESTE DAO:
 * - PedidoController.avancarStatus(): registra um Pagamento quando o
 *   pedido é marcado como "entregue" e uma forma de pagamento foi
 *   informada no formulário
 * - PedidoController.exibirDetalhe(): lista os pagamentos já feitos e
 *   soma o total pago, para exibir na tela de detalhe do pedido
 *   (útil em cenários de pagamento parcial/split)
 *
 * @author Sistema Integrador
 * @version 3.0
 * @see Pagamento
 */
public class PagamentoDAO {

    // ---- Conexão injetada via construtor (padrão usado em todo o projeto) ----
    private final Connection conexao;

    /**
     * Construtor — recebe a Connection já aberta (gerenciada pelo
     * controller via try-with-resources, tipicamente dentro da mesma
     * transação que avança o status do pedido para "entregue").
     */
    public PagamentoDAO(Connection conexao) {
        this.conexao = conexao;
    }

    // ── REGISTRAR ───────────────────────────────────────────────────

    /* ================================================================
       REGISTRAR PAGAMENTO
       ================================================================

       ÚNICA operação de escrita deste DAO (tabela append-only — sem
       UPDATE nem DELETE). Insere um novo registro de pagamento
       vinculado a um pedido.

       Usa Statement.RETURN_GENERATED_KEYS para recuperar o
       id_pagamento gerado e devolvê-lo no objeto recebido — permite
       ao chamador referenciar o pagamento recém-criado sem precisar
       de uma segunda consulta.

       Chamado por PedidoController.avancarStatus() quando o pedido é
       marcado como "entregue" — o valor pode vir do formulário
       (pagamento manual/parcial) ou ser calculado automaticamente a
       partir do total dos itens do pedido (ver
       PedidoController.calcularTotalPedido()).
    */
    public void registrar(Pagamento p) throws SQLException {
        String sql = "INSERT INTO pagamento " +
                     "(pedido_id, forma_pagamento, valor, observacao, identificador_operador) " +
                     "VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = conexao.prepareStatement(sql,
                Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, p.getPedidoId());
            stmt.setString(2, p.getFormaPagamento());
            stmt.setBigDecimal(3, p.getValor());
            stmt.setString(4, p.getObservacao());
            stmt.setString(5, p.getIdentificadorOperador());
            stmt.executeUpdate();

            // ---- Recupera o id_pagamento gerado e devolve no objeto ----
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) p.setIdPagamento(rs.getInt(1));
            }
        }
    }

    // ── LISTAR POR PEDIDO ───────────────────────────────────────────

    /* ================================================================
       LISTAR PAGAMENTOS DE UM PEDIDO
       ================================================================

       Traz TODOS os pagamentos já registrados para um pedido —
       podem ser vários, no caso de split de conta (ex: um pagamento
       em dinheiro + outro no PIX para completar o valor).

       Ordenado por data_pagamento ASC (do mais antigo para o mais
       recente) — reflete a ordem cronológica em que os pagamentos
       foram efetivamente registrados.

       Usado por PedidoController.exibirDetalhe() para mostrar o
       extrato de pagamentos de um pedido na tela.
    */
    public List<Pagamento> listarPorPedido(int pedidoId) throws SQLException {
        List<Pagamento> lista = new ArrayList<>();
        String sql = "SELECT * FROM pagamento WHERE pedido_id = ? ORDER BY data_pagamento ASC";

        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setInt(1, pedidoId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        }
        return lista;
    }

    // ── SOMAR POR PEDIDO — total já pago ────────────────────────────

    /* ================================================================
       SOMAR TOTAL JÁ PAGO DE UM PEDIDO
       ================================================================

       Agregado direto no banco (SUM), evitando trazer todas as linhas
       de pagamento para somar em Java — mais eficiente quando só o
       total importa (não os detalhes de cada pagamento individual).

       COALESCE(SUM(valor), 0) garante que, se o pedido ainda não tem
       NENHUM pagamento registrado, o retorno seja BigDecimal.ZERO em
       vez de NULL — evita que o chamador precise tratar null
       separadamente.

       Usado por PedidoController.exibirDetalhe() para calcular e
       exibir quanto já foi pago de um pedido (útil para saber, num
       cenário de split, quanto ainda falta receber).
    */
    public BigDecimal somarPorPedido(int pedidoId) throws SQLException {
        String sql = "SELECT COALESCE(SUM(valor), 0) AS total " +
                     "FROM pagamento WHERE pedido_id = ?";

        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setInt(1, pedidoId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getBigDecimal("total") : BigDecimal.ZERO;
            }
        }
    }

    // ── MAPEAMENTO ──────────────────────────────────────────────────

    /* ================================================================
       HELPER: MAPEAR RESULTSET → OBJETO Pagamento
       ================================================================

       Conversão direta, coluna a coluna — não há JOINs nem campos
       nullable especiais aqui (diferente de PedidoDAO/FilaPreparoDAO),
       já que "pagamento" é uma tabela simples e append-only, sem
       vínculos opcionais.
    */
    private Pagamento mapear(ResultSet rs) throws SQLException {
        Pagamento p = new Pagamento();
        p.setIdPagamento(rs.getInt("id_pagamento"));
        p.setPedidoId(rs.getInt("pedido_id"));
        p.setFormaPagamento(rs.getString("forma_pagamento"));
        p.setValor(rs.getBigDecimal("valor"));
        p.setObservacao(rs.getString("observacao"));
        p.setIdentificadorOperador(rs.getString("identificador_operador"));
        p.setDataPagamento(rs.getTimestamp("data_pagamento").toLocalDateTime());
        return p;
    }
}

/* ================================================================
   RESUMO DO DAO
   ================================================================

   TABELA: pagamento (renomeada de "estorno" — mudança de CONCEITO,
   não só de nome: agora registra pagamentos positivamente, em vez
   de reversões)

   CARACTERÍSTICA ÚNICA: APPEND-ONLY
   ✅ Só existe INSERT (registrar) e SELECT (listarPorPedido,
      somarPorPedido) — sem UPDATE nem DELETE, nem soft delete
   ✅ Sem coluna "ativo" (diferente de quase todas as outras tabelas
      do sistema)

   MÉTODOS:
   1. registrar(pagamento)      → única forma de escrita (INSERT)
   2. listarPorPedido(pedidoId) → extrato cronológico de pagamentos
   3. somarPorPedido(pedidoId)  → total já pago (agregado no banco)

   AJUSTES DO SCHEMA V3:
   ⚠️ identificador_operador ampliado para VARCHAR(100) — nenhuma
      mudança de código necessária
   ✅ Nenhuma coluna renomeada nesta tabela

   REGRA DE NEGÓCIO IMPORTANTE:
   ✅ Suporta múltiplos pagamentos por pedido (split de conta) — não
      há nenhuma restrição de "1 pagamento por pedido" no schema nem
      neste DAO

   DEPENDÊNCIAS:
   - Pagamento: model
   - Usado por PedidoController dentro da transação de
     avancarStatus() (ao marcar como "entregue") e na leitura de
     exibirDetalhe()

   OBSERVAÇÕES GERAIS:
   - Todas as queries usam PreparedStatement (proteção contra SQL
     injection)
   - Conexão é injetada via construtor e gerenciada pelo chamador
   ================================================================ */
