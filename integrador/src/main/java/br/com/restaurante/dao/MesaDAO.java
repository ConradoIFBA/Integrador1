package br.com.restaurante.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import br.com.restaurante.model.Mesa;

/**
 * DAO para a tabela mesa.
 *
 * Métodos:
 *   listar()               → todas ativas, ordenadas por número
 *   listarLivres()         → apenas com status='livre'
 *   buscarPorId(int)       → por PK
 *   buscarPorNumero(int)   → por número físico
 *   inserir(Mesa)          → nova mesa
 *   atualizarStatus(int, String) → muda status (livre/ocupada/reservada)
 *   editar(Mesa)           → atualiza capacidade e status
 *   desativar(int)         → soft delete
 */
public class MesaDAO {

    private final Connection conexao;

    public MesaDAO(Connection conexao) {
        this.conexao = conexao;
    }

    // ----------------------------------------------------------------
    // LISTAR — todas ativas
    // ----------------------------------------------------------------

    public List<Mesa> listar() throws SQLException {
        List<Mesa> lista = new ArrayList<>();
        String sql = "SELECT * FROM mesa WHERE ativo = 1 ORDER BY numero";

        try (PreparedStatement stmt = conexao.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    // ----------------------------------------------------------------
    // LISTAR LIVRES
    // ----------------------------------------------------------------

    public List<Mesa> listarLivres() throws SQLException {
        List<Mesa> lista = new ArrayList<>();
        String sql = "SELECT * FROM mesa WHERE ativo = 1 AND status = 'livre' ORDER BY numero";

        try (PreparedStatement stmt = conexao.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    // ----------------------------------------------------------------
    // BUSCAR POR ID
    // ----------------------------------------------------------------

    public Mesa buscarPorId(int id) throws SQLException {
        String sql = "SELECT * FROM mesa WHERE id_mesa = ? AND ativo = 1";

        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapear(rs);
            }
        }
        return null;
    }

    // ----------------------------------------------------------------
    // BUSCAR POR NÚMERO
    // ----------------------------------------------------------------

    public Mesa buscarPorNumero(int numero) throws SQLException {
        String sql = "SELECT * FROM mesa WHERE numero = ? AND ativo = 1";

        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setInt(1, numero);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapear(rs);
            }
        }
        return null;
    }

    // ----------------------------------------------------------------
    // INSERIR
    // ----------------------------------------------------------------

    public void inserir(Mesa m) throws SQLException {
        String sql = "INSERT INTO mesa (numero, capacidade, status, ativo) VALUES (?, ?, 'livre', 1)";

        try (PreparedStatement stmt = conexao.prepareStatement(sql,
                Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, m.getNumero());
            stmt.setInt(2, m.getCapacidade());
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) m.setIdMesa(rs.getInt(1));
            }
        }
    }

    // ----------------------------------------------------------------
    // ATUALIZAR STATUS — operação mais frequente (abrir/fechar mesa)
    // ----------------------------------------------------------------

    /**
     * Atualiza apenas o status de uma mesa.
     * status: 'livre' | 'ocupada' | 'reservada'
     */
    public void atualizarStatus(int idMesa, String status) throws SQLException {
        String sql = "UPDATE mesa SET status = ? WHERE id_mesa = ?";

        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setInt(2, idMesa);
            stmt.executeUpdate();
        }
    }

    // ----------------------------------------------------------------
    // EDITAR — capacidade e status
    // ----------------------------------------------------------------

    public void editar(Mesa m) throws SQLException {
        String sql = "UPDATE mesa SET capacidade = ?, status = ? WHERE id_mesa = ?";

        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setInt(1, m.getCapacidade());
            stmt.setString(2, m.getStatus());
            stmt.setInt(3, m.getIdMesa());
            stmt.executeUpdate();
        }
    }

    // ----------------------------------------------------------------
    // SOFT DELETE
    // ----------------------------------------------------------------

    public void desativar(int id) throws SQLException {
        String sql = "UPDATE mesa SET ativo = 0 WHERE id_mesa = ?";

        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    // ----------------------------------------------------------------
    // MAPEAMENTO ResultSet → Mesa
    // ----------------------------------------------------------------

    private Mesa mapear(ResultSet rs) throws SQLException {
        Mesa m = new Mesa();
        m.setIdMesa(rs.getInt("id_mesa"));
        m.setNumero(rs.getInt("numero"));
        m.setCapacidade(rs.getInt("capacidade"));
        m.setStatus(rs.getString("status"));
        m.setAtivo(rs.getBoolean("ativo"));
        return m;
    }
}
