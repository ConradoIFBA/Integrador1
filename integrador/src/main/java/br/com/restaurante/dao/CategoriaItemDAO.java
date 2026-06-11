package br.com.restaurante.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import br.com.restaurante.model.CategoriaItem;

/**
 * DAO para a tabela categoria_item.
 *
 * Métodos:
 *   listar()                 → todas ativas, ordenadas por nome
 *   listarPorSetor(String)   → filtradas por setor (cozinha/bebida/sobremesa)
 *   buscarPorId(int)         → por PK
 *   inserir(CategoriaItem)   → nova categoria
 *   editar(CategoriaItem)    → atualizar nome e setor
 *   desativar(int)           → soft delete
 */
public class CategoriaItemDAO {

    private final Connection conexao;

    public CategoriaItemDAO(Connection conexao) {
        this.conexao = conexao;
    }

    // ----------------------------------------------------------------
    // LISTAR — todas ativas
    // ----------------------------------------------------------------

    public List<CategoriaItem> listar() throws SQLException {
        List<CategoriaItem> lista = new ArrayList<>();
        String sql = "SELECT * FROM categoria_item WHERE ativo = 1 ORDER BY nome";

        try (PreparedStatement stmt = conexao.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    // ----------------------------------------------------------------
    // LISTAR POR SETOR
    // ----------------------------------------------------------------

    public List<CategoriaItem> listarPorSetor(String setor) throws SQLException {
        List<CategoriaItem> lista = new ArrayList<>();
        String sql = "SELECT * FROM categoria_item WHERE ativo = 1 AND setor = ? ORDER BY nome";

        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setString(1, setor);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        }
        return lista;
    }

    // ----------------------------------------------------------------
    // BUSCAR POR ID
    // ----------------------------------------------------------------

    public CategoriaItem buscarPorId(int id) throws SQLException {
        String sql = "SELECT * FROM categoria_item WHERE id_categoria = ? AND ativo = 1";

        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapear(rs);
            }
        }
        return null;
    }

    // ----------------------------------------------------------------
    // INSERIR
    // ----------------------------------------------------------------

    public void inserir(CategoriaItem c) throws SQLException {
        String sql = "INSERT INTO categoria_item (nome, setor, ativo) VALUES (?, ?, 1)";

        try (PreparedStatement stmt = conexao.prepareStatement(sql,
                Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, c.getNome());
            stmt.setString(2, c.getSetor());
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) c.setIdCategoria(rs.getInt(1));
            }
        }
    }

    // ----------------------------------------------------------------
    // EDITAR
    // ----------------------------------------------------------------

    public void editar(CategoriaItem c) throws SQLException {
        String sql = "UPDATE categoria_item SET nome = ?, setor = ? WHERE id_categoria = ?";

        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setString(1, c.getNome());
            stmt.setString(2, c.getSetor());
            stmt.setInt(3, c.getIdCategoria());
            stmt.executeUpdate();
        }
    }

    // ----------------------------------------------------------------
    // SOFT DELETE
    // ----------------------------------------------------------------

    public void desativar(int id) throws SQLException {
        String sql = "UPDATE categoria_item SET ativo = 0 WHERE id_categoria = ?";

        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    // ----------------------------------------------------------------
    // MAPEAMENTO ResultSet → CategoriaItem
    // ----------------------------------------------------------------

    private CategoriaItem mapear(ResultSet rs) throws SQLException {
        CategoriaItem c = new CategoriaItem();
        c.setIdCategoria(rs.getInt("id_categoria"));
        c.setNome(rs.getString("nome"));
        c.setSetor(rs.getString("setor"));
        c.setAtivo(rs.getBoolean("ativo"));
        return c;
    }
}
