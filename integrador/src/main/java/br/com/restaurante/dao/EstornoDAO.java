package br.com.restaurante.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import br.com.restaurante.model.Estorno;

/**
 * DAO para a tabela estorno (append-only — somente INSERT e SELECT).
 *
 * Nunca haverá UPDATE nem DELETE nesta tabela.
 * Registros financeiros são imutáveis.
 *
 * Métodos:
 *   registrar(Estorno)          → INSERT
 *   listarPorPedido(int)        → histórico de estornos de um pedido
 *   somarEstornosPorPedido(int) → total já estornado (para calcular estorno parcial)
 */
public class EstornoDAO {

    private final Connection conexao;

    public EstornoDAO(Connection conexao) {
        this.conexao = conexao;
    }

    // ----------------------------------------------------------------
    // REGISTRAR — único método de escrita
    // ----------------------------------------------------------------

    public void registrar(Estorno e) throws SQLException {
        String sql = "INSERT INTO estorno " +
                     "(pedido_id, valor_estornado, tipo_estorno, forma_pagamento, " +
                     " motivo, identificador_operador) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = conexao.prepareStatement(sql,
                Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, e.getPedidoId());
            stmt.setBigDecimal(2, e.getValorEstornado());
            stmt.setString(3, e.getTipoEstorno());
            stmt.setString(4, e.getFormaPagamento());
            stmt.setString(5, e.getMotivo());
            stmt.setString(6, e.getIdentificadorOperador());
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) e.setIdEstorno(rs.getInt(1));
            }
        }
    }

    // ----------------------------------------------------------------
    // LISTAR POR PEDIDO
    // ----------------------------------------------------------------

    public List<Estorno> listarPorPedido(int pedidoId) throws SQLException {
        List<Estorno> lista = new ArrayList<>();
        String sql = "SELECT * FROM estorno WHERE pedido_id = ? ORDER BY data_hora ASC";

        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setInt(1, pedidoId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        }
        return lista;
    }

    // ----------------------------------------------------------------
    // SOMAR ESTORNOS — útil para validar se valor solicitado é válido
    // antes de registrar um novo estorno parcial
    // ----------------------------------------------------------------

    public java.math.BigDecimal somarEstornosPorPedido(int pedidoId) throws SQLException {
        String sql = "SELECT COALESCE(SUM(valor_estornado), 0) AS total " +
                     "FROM estorno WHERE pedido_id = ?";

        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setInt(1, pedidoId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getBigDecimal("total");
            }
        }
        return java.math.BigDecimal.ZERO;
    }

    // ----------------------------------------------------------------
    // MAPEAMENTO ResultSet → Estorno
    // ----------------------------------------------------------------

    private Estorno mapear(ResultSet rs) throws SQLException {
        Estorno e = new Estorno();
        e.setIdEstorno(rs.getInt("id_estorno"));
        e.setPedidoId(rs.getInt("pedido_id"));
        e.setValorEstornado(rs.getBigDecimal("valor_estornado"));
        e.setTipoEstorno(rs.getString("tipo_estorno"));
        e.setFormaPagamento(rs.getString("forma_pagamento"));
        e.setMotivo(rs.getString("motivo"));
        e.setDataHora(rs.getTimestamp("data_hora").toLocalDateTime());
        e.setIdentificadorOperador(rs.getString("identificador_operador"));
        return e;
    }
}
