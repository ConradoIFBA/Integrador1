package br.com.restaurante.dao;
import java.sql.*;
import java.util.*;
import br.com.restaurante.model.CategoriaItem;
import br.com.restaurante.model.ItemCardapio;
public class ItemCardapioDAO {
    private final Connection conexao;
    private static final String SQL_BASE=
        "SELECT i.*,c.nome AS nome_categoria,c.setor FROM item_cardapio i "+
        "INNER JOIN categoria_item c ON i.categoria_id=c.id_categoria ";
    public ItemCardapioDAO(Connection c){this.conexao=c;}
    public List<ItemCardapio> listar() throws SQLException {
        List<ItemCardapio> l=new ArrayList<>();
        try(PreparedStatement s=conexao.prepareStatement(
            SQL_BASE+"WHERE i.ativo=1 AND i.disponivel=1 ORDER BY c.nome,i.nome");
            ResultSet r=s.executeQuery()){while(r.next())l.add(mapear(r));}
        return l;
    }
    public List<ItemCardapio> listarPorCategoria(int catId) throws SQLException {
        List<ItemCardapio> l=new ArrayList<>();
        try(PreparedStatement s=conexao.prepareStatement(
            SQL_BASE+"WHERE i.ativo=1 AND i.disponivel=1 AND i.categoria_id=? ORDER BY i.nome")){
            s.setInt(1,catId);try(ResultSet r=s.executeQuery()){while(r.next())l.add(mapear(r));}
        } return l;
    }
    public ItemCardapio buscarPorId(int id) throws SQLException {
        try(PreparedStatement s=conexao.prepareStatement(SQL_BASE+"WHERE i.id_item=? AND i.ativo=1")){
            s.setInt(1,id);try(ResultSet r=s.executeQuery()){if(r.next())return mapear(r);}
        } return null;
    }
    public void inserir(ItemCardapio i) throws SQLException {
        try(PreparedStatement s=conexao.prepareStatement(
            "INSERT INTO item_cardapio(categoria_id,nome,descricao,preco,tempo_preparo_min,disponivel,ativo) VALUES(?,?,?,?,?,1,1)",
            Statement.RETURN_GENERATED_KEYS)){
            s.setInt(1,i.getCategoriaId());s.setString(2,i.getNome());s.setString(3,i.getDescricao());
            s.setBigDecimal(4,i.getPreco());s.setInt(5,i.getTempoPreparoMin());s.executeUpdate();
            try(ResultSet r=s.getGeneratedKeys()){if(r.next())i.setIdItem(r.getInt(1));}
        }
    }
    public void editar(ItemCardapio i) throws SQLException {
        try(PreparedStatement s=conexao.prepareStatement(
            "UPDATE item_cardapio SET categoria_id=?,nome=?,descricao=?,preco=?,tempo_preparo_min=? WHERE id_item=?")){
            s.setInt(1,i.getCategoriaId());s.setString(2,i.getNome());s.setString(3,i.getDescricao());
            s.setBigDecimal(4,i.getPreco());s.setInt(5,i.getTempoPreparoMin());s.setInt(6,i.getIdItem());s.executeUpdate();
        }
    }
    public void atualizarDisponibilidade(int id,boolean disp) throws SQLException {
        try(PreparedStatement s=conexao.prepareStatement(
            "UPDATE item_cardapio SET disponivel=? WHERE id_item=?")){
            s.setBoolean(1,disp);s.setInt(2,id);s.executeUpdate();
        }
    }
    public void desativar(int id) throws SQLException {
        try(PreparedStatement s=conexao.prepareStatement(
            "UPDATE item_cardapio SET ativo=0 WHERE id_item=?")){s.setInt(1,id);s.executeUpdate();}
    }
    private ItemCardapio mapear(ResultSet r) throws SQLException {
        CategoriaItem cat=new CategoriaItem(r.getInt("categoria_id"),r.getString("nome_categoria"),r.getString("setor"),true);
        ItemCardapio i=new ItemCardapio();
        i.setIdItem(r.getInt("id_item"));i.setCategoriaId(r.getInt("categoria_id"));i.setCategoria(cat);
        i.setNome(r.getString("nome"));i.setDescricao(r.getString("descricao"));
        i.setPreco(r.getBigDecimal("preco"));i.setTempoPreparoMin(r.getInt("tempo_preparo_min"));
        i.setDisponivel(r.getBoolean("disponivel"));i.setAtivo(r.getBoolean("ativo"));return i;
    }
}
