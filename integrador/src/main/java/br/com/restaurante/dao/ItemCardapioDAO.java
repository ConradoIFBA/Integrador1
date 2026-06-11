package br.com.restaurante.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import br.com.restaurante.model.CategoriaItem;
import br.com.restaurante.model.ItemCardapio;

/**
 * DAO para a tabela item_cardapio.
 *
 * Métodos:
 *   listar()                      → ativos e disponíveis, com JOIN de categoria
 *   listarPorCategoria(int)        → filtrado por categoria
 *   buscarPorId(int)              → com JOIN de categoria
 *   inserir(ItemCardapio)         → novo item
 *   editar(ItemCardapio)          → atualizar dados
 *   atualizarDisponibilidade(int, boolean) → bloquear/liberar sem excluir
 *   desativar(int)                → soft delete
 */
public class ItemCardapioDAO {

    private final Connection conexao;

    public ItemCardapioDAO(Connection conexao) {
        this.conexao = conexao;
    }

    // ----------------------------------------------------------------
    // SQL BASE — reutilizado nos métodos de busca com JOIN
    // ----------------------------------------------------------------

    private static final String SQL_BASE =
        "SELECT i.*, c.nome AS nome_categoria, c.setor " +
        "FROM item_cardapio i " +
        "INNER JOIN categoria_item c ON i.categoria_id = c.id_categoria ";

    // ----------------------------------------------------------------
    // LISTAR — ativos e disponíveis
    // ----------------------------------------------------------------

    public List<ItemCardapio> listar() throws SQLException {
        List<ItemCardapio> lista = new ArrayList<>();
        String sql = SQL_BASE + "WHERE i.ativo = 1 AND i.disponivel = 1 ORDER BY c.nome, i.nome";

        try (PreparedStatement stmt = conexao.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    // ----------------------------------------------------------------
    // LISTAR POR CATEGORIA
    // ----------------------------------------------------------------

    public List<ItemCardapio> listarPorCategoria(int categoriaId) throws SQLException {
        List<ItemCardapio> lista = new ArrayList<>();
        String sql = SQL_BASE +
                     "WHERE i.ativo = 1 AND i.disponivel = 1 AND i.categoria_id = ? " +
                     "ORDER BY i.nome";

        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setInt(1, categoriaId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        }
        return lista;
    }

    // ----------------------------------------------------------------
    // BUSCAR POR ID — inclui categoria via JOIN
    // ----------------------------------------------------------------

    public ItemCardapio buscarPorId(int id) throws SQLException {
        String sql = SQL_BASE + "WHERE i.id_item = ? AND i.ativo = 1";

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

    public void inserir(ItemCardapio item) throws SQLException {
        String sql = "INSERT INTO item_cardapio " +
                     "(categoria_id, nome, descricao, preco, tempo_preparo_min, disponivel, ativo) " +
                     "VALUES (?, ?, ?, ?, ?, 1, 1)";

        try (PreparedStatement stmt = conexao.prepareStatement(sql,
                Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, item.getCategoriaId());
            stmt.setString(2, item.getNome());
            stmt.setString(3, item.getDescricao());
            stmt.setBigDecimal(4, item.getPreco());
            stmt.setInt(5, item.getTempoPreparoMin());
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) item.setIdItem(rs.getInt(1));
            }
        }
    }

    // ----------------------------------------------------------------
    // EDITAR
    // ----------------------------------------------------------------

    public void editar(ItemCardapio item) throws SQLException {
        String sql = "UPDATE item_cardapio " +
                     "SET categoria_id = ?, nome = ?, descricao = ?, " +
                     "    preco = ?, tempo_preparo_min = ? " +
                     "WHERE id_item = ?";

        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setInt(1, item.getCategoriaId());
            stmt.setString(2, item.getNome());
            stmt.setString(3, item.getDescricao());
            stmt.setBigDecimal(4, item.getPreco());
            stmt.setInt(5, item.getTempoPreparoMin());
            stmt.setInt(6, item.getIdItem());
            stmt.executeUpdate();
        }
    }

    // ----------------------------------------------------------------
    // ATUALIZAR DISPONIBILIDADE — bloquear sem excluir (falta de estoque)
    // ----------------------------------------------------------------

    public void atualizarDisponibilidade(int idItem, boolean disponivel) throws SQLException {
        String sql = "UPDATE item_cardapio SET disponivel = ? WHERE id_item = ?";

        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setBoolean(1, disponivel);
            stmt.setInt(2, idItem);
            stmt.executeUpdate();
        }
    }

    // ----------------------------------------------------------------
    // SOFT DELETE
    // ----------------------------------------------------------------

    public void desativar(int id) throws SQLException {
        String sql = "UPDATE item_cardapio SET ativo = 0 WHERE id_item = ?";

        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    // ----------------------------------------------------------------
    // MAPEAMENTO ResultSet → ItemCardapio (com CategoriaItem embutido)
    // ----------------------------------------------------------------

    private ItemCardapio mapear(ResultSet rs) throws SQLException {
        CategoriaItem cat = new CategoriaItem();
        cat.setIdCategoria(rs.getInt("categoria_id"));
        cat.setNome(rs.getString("nome_categoria"));
        cat.setSetor(rs.getString("setor"));

        ItemCardapio item = new ItemCardapio();
        item.setIdItem(rs.getInt("id_item"));
        item.setCategoriaId(rs.getInt("categoria_id"));
        item.setCategoria(cat);
        item.setNome(rs.getString("nome"));
        item.setDescricao(rs.getString("descricao"));
        item.setPreco(rs.getBigDecimal("preco"));
        item.setTempoPreparoMin(rs.getInt("tempo_preparo_min"));
        item.setDisponivel(rs.getBoolean("disponivel"));
        item.setAtivo(rs.getBoolean("ativo"));
        return item;
    }
}
