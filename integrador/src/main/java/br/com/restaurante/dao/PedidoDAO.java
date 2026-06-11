package br.com.restaurante.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import br.com.restaurante.model.ItemPedido;
import br.com.restaurante.model.Mesa;
import br.com.restaurante.model.Pedido;

/**
 * DAO para a tabela pedido.
 *
 * Métodos:
 *   listarAbertos()              → pedidos ativos para o atendente
 *   listarPorMesa(int)           → pedidos de uma mesa (abertos)
 *   listarPorStatus(String)      → filtro genérico por status
 *   buscarPorId(int)             → com JOIN de mesa
 *   buscarComItens(int)          → pedido + todos os itens (ItemPedidoDAO é chamado externamente)
 *   inserir(Pedido)              → INSERT simples (sem itens; itens inseridos via ItemPedidoDAO)
 *   atualizarStatus(int, String) → avança no ciclo de vida
 *   editar(Pedido)               → atualiza urgente e observacao
 *   desativar(int)               → soft delete
 */
public class PedidoDAO {

    private final Connection conexao;

    public PedidoDAO(Connection conexao) {
        this.conexao = conexao;
    }

    // ----------------------------------------------------------------
    // SQL BASE
    // ----------------------------------------------------------------

    private static final String SQL_BASE =
        "SELECT p.*, " +
        "       m.numero AS mesa_numero, m.capacidade, m.status AS mesa_status " +
        "FROM pedido p " +
        "LEFT JOIN mesa m ON p.mesa_id = m.id_mesa ";

    // ----------------------------------------------------------------
    // LISTAR ABERTOS — dashboard do atendente
    // ----------------------------------------------------------------

    public List<Pedido> listarAbertos() throws SQLException {
        List<Pedido> lista = new ArrayList<>();
        String sql = SQL_BASE +
                     "WHERE p.ativo = 1 AND p.status NOT IN ('entregue','cancelado','estornado') " +
                     "ORDER BY p.urgente DESC, p.data_abertura ASC";

        try (PreparedStatement stmt = conexao.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    // ----------------------------------------------------------------
    // LISTAR POR MESA
    // ----------------------------------------------------------------

    public List<Pedido> listarPorMesa(int mesaId) throws SQLException {
        List<Pedido> lista = new ArrayList<>();
        String sql = SQL_BASE +
                     "WHERE p.mesa_id = ? AND p.ativo = 1 " +
                     "AND p.status NOT IN ('entregue','cancelado','estornado') " +
                     "ORDER BY p.data_abertura DESC";

        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setInt(1, mesaId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        }
        return lista;
    }

    // ----------------------------------------------------------------
    // LISTAR POR STATUS
    // ----------------------------------------------------------------

    public List<Pedido> listarPorStatus(String status) throws SQLException {
        List<Pedido> lista = new ArrayList<>();
        String sql = SQL_BASE +
                     "WHERE p.status = ? AND p.ativo = 1 " +
                     "ORDER BY p.urgente DESC, p.data_abertura ASC";

        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setString(1, status);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        }
        return lista;
    }

    // ----------------------------------------------------------------
    // BUSCAR POR ID
    // ----------------------------------------------------------------

    public Pedido buscarPorId(int id) throws SQLException {
        String sql = SQL_BASE + "WHERE p.id_pedido = ? AND p.ativo = 1";

        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapear(rs);
            }
        }
        return null;
    }

    // ----------------------------------------------------------------
    // INSERIR — apenas o cabeçalho; itens inseridos via ItemPedidoDAO
    // na mesma transação gerenciada pelo controller/service
    // ----------------------------------------------------------------

    public void inserir(Pedido p) throws SQLException {
        String sql = "INSERT INTO pedido " +
                     "(mesa_id, tipo, urgente, identificador_operador, status, observacao, ativo) " +
                     "VALUES (?, ?, ?, ?, 'aberto', ?, 1)";

        try (PreparedStatement stmt = conexao.prepareStatement(sql,
                Statement.RETURN_GENERATED_KEYS)) {

            // mesa_id nullable (delivery)
            if (p.getMesaId() != null) stmt.setInt(1, p.getMesaId());
            else                       stmt.setNull(1, Types.INTEGER);

            stmt.setString(2, p.getTipo());
            stmt.setBoolean(3, p.isUrgente());
            stmt.setString(4, p.getIdentificadorOperador());
            stmt.setString(5, p.getObservacao());
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) p.setIdPedido(rs.getInt(1));
            }
        }
    }

    // ----------------------------------------------------------------
    // ATUALIZAR STATUS
    // ----------------------------------------------------------------

    public void atualizarStatus(int idPedido, String status) throws SQLException {
        String sql = "UPDATE pedido SET status = ? WHERE id_pedido = ?";

        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setInt(2, idPedido);
            stmt.executeUpdate();
        }
    }

    // ----------------------------------------------------------------
    // EDITAR — urgente e observacao
    // ----------------------------------------------------------------

    public void editar(Pedido p) throws SQLException {
        String sql = "UPDATE pedido SET urgente = ?, observacao = ? WHERE id_pedido = ?";

        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setBoolean(1, p.isUrgente());
            stmt.setString(2, p.getObservacao());
            stmt.setInt(3, p.getIdPedido());
            stmt.executeUpdate();
        }
    }

    // ----------------------------------------------------------------
    // SOFT DELETE
    // ----------------------------------------------------------------

    public void desativar(int id) throws SQLException {
        String sql = "UPDATE pedido SET ativo = 0, status = 'cancelado' WHERE id_pedido = ?";

        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    // ----------------------------------------------------------------
    // MAPEAMENTO ResultSet → Pedido (com Mesa embutida via LEFT JOIN)
    // ----------------------------------------------------------------

    private Pedido mapear(ResultSet rs) throws SQLException {
        Pedido p = new Pedido();
        p.setIdPedido(rs.getInt("id_pedido"));

        int mesaId = rs.getInt("mesa_id");
        if (!rs.wasNull()) {
            p.setMesaId(mesaId);

            Mesa m = new Mesa();
            m.setIdMesa(mesaId);
            m.setNumero(rs.getInt("mesa_numero"));
            m.setCapacidade(rs.getInt("capacidade"));
            m.setStatus(rs.getString("mesa_status"));
            p.setMesa(m);
        }

        p.setTipo(rs.getString("tipo"));
        p.setUrgente(rs.getBoolean("urgente"));
        p.setIdentificadorOperador(rs.getString("identificador_operador"));
        p.setStatus(rs.getString("status"));
        p.setObservacao(rs.getString("observacao"));
        p.setDataAbertura(rs.getTimestamp("data_abertura").toLocalDateTime());
        p.setAtivo(rs.getBoolean("ativo"));
        return p;
    }
}
