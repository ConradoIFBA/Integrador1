package br.com.restaurante.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import br.com.restaurante.model.ItemCardapio;
import br.com.restaurante.model.ItemPedido;

/**
 * DAO para a tabela item_pedido.
 *
 * Métodos:
 *   inserir(ItemPedido)              → novo item no pedido
 *   inserirLote(List, int)           → vários itens de uma vez (mesma transação)
 *   listarPorPedido(int)             → todos os itens de um pedido, com JOIN de cardápio
 *   buscarPorId(int)                 → por PK, com JOIN
 *   atualizarStatus(int, String)     → avança item no ciclo de vida
 *   desativar(int)                   → soft delete de um item
 *   cancelarItensDoPedido(int)       → cancela todos os itens pendentes de um pedido
 */
public class ItemPedidoDAO {

    private final Connection conexao;

    public ItemPedidoDAO(Connection conexao) {
        this.conexao = conexao;
    }

    // ----------------------------------------------------------------
    // SQL BASE — JOIN com item_cardapio para popular o objeto
    // ----------------------------------------------------------------

    private static final String SQL_BASE =
        "SELECT ip.*, " +
        "       ic.nome AS nome_item, ic.tempo_preparo_min, " +
        "       ic.categoria_id, ic.disponivel " +
        "FROM item_pedido ip " +
        "INNER JOIN item_cardapio ic ON ip.item_cardapio_id = ic.id_item ";

    // ----------------------------------------------------------------
    // INSERIR — único item; use dentro de transação do controller
    // ----------------------------------------------------------------

    public void inserir(ItemPedido item) throws SQLException {
        String sql = "INSERT INTO item_pedido " +
                     "(pedido_id, item_cardapio_id, quantidade, preco_unitario, " +
                     " observacao, status, ativo) " +
                     "VALUES (?, ?, ?, ?, ?, 'pendente', 1)";

        try (PreparedStatement stmt = conexao.prepareStatement(sql,
                Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, item.getPedidoId());
            stmt.setInt(2, item.getItemCardapioId());
            stmt.setInt(3, item.getQuantidade());
            stmt.setBigDecimal(4, item.getPrecoUnitario());
            stmt.setString(5, item.getObservacao());
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) item.setIdItemPedido(rs.getInt(1));
            }
        }
    }

    // ----------------------------------------------------------------
    // INSERIR LOTE — vários itens de uma vez na mesma transação aberta
    // pelo controller (autoCommit já deve estar false)
    // ----------------------------------------------------------------

    public void inserirLote(List<ItemPedido> itens, int pedidoId) throws SQLException {
        String sql = "INSERT INTO item_pedido " +
                     "(pedido_id, item_cardapio_id, quantidade, preco_unitario, " +
                     " observacao, status, ativo) " +
                     "VALUES (?, ?, ?, ?, ?, 'pendente', 1)";

        try (PreparedStatement stmt = conexao.prepareStatement(sql,
                Statement.RETURN_GENERATED_KEYS)) {

            for (ItemPedido item : itens) {
                item.setPedidoId(pedidoId);
                stmt.setInt(1, pedidoId);
                stmt.setInt(2, item.getItemCardapioId());
                stmt.setInt(3, item.getQuantidade());
                stmt.setBigDecimal(4, item.getPrecoUnitario());
                stmt.setString(5, item.getObservacao());
                stmt.addBatch();
            }

            stmt.executeBatch();

            // Recupera os IDs gerados na ordem do batch
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                int i = 0;
                while (rs.next() && i < itens.size()) {
                    itens.get(i++).setIdItemPedido(rs.getInt(1));
                }
            }
        }
    }

    // ----------------------------------------------------------------
    // LISTAR POR PEDIDO — com JOIN de item_cardapio
    // ----------------------------------------------------------------

    public List<ItemPedido> listarPorPedido(int pedidoId) throws SQLException {
        List<ItemPedido> lista = new ArrayList<>();
        String sql = SQL_BASE +
                     "WHERE ip.pedido_id = ? AND ip.ativo = 1 " +
                     "ORDER BY ip.id_item_pedido ASC";

        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setInt(1, pedidoId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        }
        return lista;
    }

    // ----------------------------------------------------------------
    // BUSCAR POR ID
    // ----------------------------------------------------------------

    public ItemPedido buscarPorId(int id) throws SQLException {
        String sql = SQL_BASE + "WHERE ip.id_item_pedido = ? AND ip.ativo = 1";

        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapear(rs);
            }
        }
        return null;
    }

    // ----------------------------------------------------------------
    // ATUALIZAR STATUS — avança item no ciclo de vida
    // ----------------------------------------------------------------

    public void atualizarStatus(int idItemPedido, String status) throws SQLException {
        String sql = "UPDATE item_pedido SET status = ? WHERE id_item_pedido = ?";

        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setInt(2, idItemPedido);
            stmt.executeUpdate();
        }
    }

    // ----------------------------------------------------------------
    // SOFT DELETE — item individual
    // ----------------------------------------------------------------

    public void desativar(int id) throws SQLException {
        String sql = "UPDATE item_pedido SET ativo = 0, status = 'cancelado' " +
                     "WHERE id_item_pedido = ?";

        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    // ----------------------------------------------------------------
    // CANCELAR ITENS DO PEDIDO — usado ao cancelar o pedido inteiro;
    // apenas itens pendentes e em_preparo são afetados
    // ----------------------------------------------------------------

    public void cancelarItensDoPedido(int pedidoId) throws SQLException {
        String sql = "UPDATE item_pedido SET status = 'cancelado' " +
                     "WHERE pedido_id = ? AND status IN ('pendente','em_preparo')";

        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setInt(1, pedidoId);
            stmt.executeUpdate();
        }
    }

    // ----------------------------------------------------------------
    // MAPEAMENTO ResultSet → ItemPedido (com ItemCardapio embutido)
    // ----------------------------------------------------------------

    private ItemPedido mapear(ResultSet rs) throws SQLException {
        ItemCardapio ic = new ItemCardapio();
        ic.setIdItem(rs.getInt("item_cardapio_id"));
        ic.setNome(rs.getString("nome_item"));
        ic.setCategoriaId(rs.getInt("categoria_id"));
        ic.setTempoPreparoMin(rs.getInt("tempo_preparo_min"));
        ic.setDisponivel(rs.getBoolean("disponivel"));

        ItemPedido ip = new ItemPedido();
        ip.setIdItemPedido(rs.getInt("id_item_pedido"));
        ip.setPedidoId(rs.getInt("pedido_id"));
        ip.setItemCardapioId(rs.getInt("item_cardapio_id"));
        ip.setItemCardapio(ic);
        ip.setQuantidade(rs.getInt("quantidade"));
        ip.setPrecoUnitario(rs.getBigDecimal("preco_unitario"));
        ip.setObservacao(rs.getString("observacao"));
        ip.setStatus(rs.getString("status"));
        ip.setAtivo(rs.getBoolean("ativo"));
        return ip;
    }
}
