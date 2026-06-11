package br.com.restaurante.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import br.com.restaurante.model.HistoricoMesa;

/**
 * DAO para a tabela historico_mesa.
 *
 * Métodos:
 *   registrar(HistoricoMesa)    → INSERT de um novo evento
 *   listarPorMesa(int)          → linha do tempo de uma mesa
 *   listarPorMesaHoje(int)      → apenas eventos do dia atual
 */
public class HistoricoMesaDAO {

    private final Connection conexao;

    public HistoricoMesaDAO(Connection conexao) {
        this.conexao = conexao;
    }

    // ----------------------------------------------------------------
    // REGISTRAR — insere um novo evento na linha do tempo da mesa
    // ----------------------------------------------------------------

    /**
     * Grava um novo registro histórico.
     * data_hora é preenchida automaticamente pelo banco (DEFAULT CURRENT_TIMESTAMP).
     */
    public void registrar(HistoricoMesa h) throws SQLException {
        String sql = "INSERT INTO historico_mesa (mesa_id, descricao, ativo) VALUES (?, ?, 1)";

        try (PreparedStatement stmt = conexao.prepareStatement(sql,
                Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, h.getMesaId());
            stmt.setString(2, h.getDescricao());
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) h.setIdHistorico(rs.getInt(1));
            }
        }
    }

    // ----------------------------------------------------------------
    // LISTAR POR MESA — linha do tempo completa (mais antigo → mais novo)
    // ----------------------------------------------------------------

    public List<HistoricoMesa> listarPorMesa(int mesaId) throws SQLException {
        List<HistoricoMesa> lista = new ArrayList<>();
        String sql = "SELECT * FROM historico_mesa " +
                     "WHERE mesa_id = ? AND ativo = 1 " +
                     "ORDER BY data_hora ASC";

        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setInt(1, mesaId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        }
        return lista;
    }

    // ----------------------------------------------------------------
    // LISTAR POR MESA HOJE — apenas eventos do dia corrente
    // ----------------------------------------------------------------

    public List<HistoricoMesa> listarPorMesaHoje(int mesaId) throws SQLException {
        List<HistoricoMesa> lista = new ArrayList<>();
        String sql = "SELECT * FROM historico_mesa " +
                     "WHERE mesa_id = ? AND ativo = 1 AND DATE(data_hora) = CURDATE() " +
                     "ORDER BY data_hora ASC";

        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setInt(1, mesaId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        }
        return lista;
    }

    // ----------------------------------------------------------------
    // MAPEAMENTO ResultSet → HistoricoMesa
    // ----------------------------------------------------------------

    private HistoricoMesa mapear(ResultSet rs) throws SQLException {
        HistoricoMesa h = new HistoricoMesa();
        h.setIdHistorico(rs.getInt("id_historico"));
        h.setMesaId(rs.getInt("mesa_id"));
        h.setDescricao(rs.getString("descricao"));
        h.setDataHora(rs.getTimestamp("data_hora").toLocalDateTime());
        h.setAtivo(rs.getBoolean("ativo"));
        return h;
    }
}
