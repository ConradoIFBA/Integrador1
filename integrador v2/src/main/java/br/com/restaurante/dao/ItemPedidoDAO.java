package br.com.restaurante.dao;
import java.sql.*;
import java.util.*;
import br.com.restaurante.model.ItemCardapio;
import br.com.restaurante.model.ItemPedido;
public class ItemPedidoDAO {
    private final Connection conexao;
    private static final String SQL_BASE=
        "SELECT ip.*,ic.nome AS nome_item,ic.tempo_preparo_min,ic.categoria_id,ic.disponivel "+
        "FROM item_pedido ip INNER JOIN item_cardapio ic ON ip.item_cardapio_id=ic.id_item ";
    public ItemPedidoDAO(Connection c){this.conexao=c;}
    public void inserir(ItemPedido item) throws SQLException {
        try(PreparedStatement s=conexao.prepareStatement(
            "INSERT INTO item_pedido(pedido_id,item_cardapio_id,quantidade,preco_unitario,observacao,status,ativo) VALUES(?,?,?,?,?,'pendente',1)",
            Statement.RETURN_GENERATED_KEYS)){
            s.setInt(1,item.getPedidoId());s.setInt(2,item.getItemCardapioId());
            s.setInt(3,item.getQuantidade());s.setBigDecimal(4,item.getPrecoUnitario());
            s.setString(5,item.getObservacao());s.executeUpdate();
            try(ResultSet r=s.getGeneratedKeys()){if(r.next())item.setIdItemPedido(r.getInt(1));}
        }
    }
    public void inserirLote(List<ItemPedido> itens,int pedidoId) throws SQLException {
        try(PreparedStatement s=conexao.prepareStatement(
            "INSERT INTO item_pedido(pedido_id,item_cardapio_id,quantidade,preco_unitario,observacao,status,ativo) VALUES(?,?,?,?,?,'pendente',1)",
            Statement.RETURN_GENERATED_KEYS)){
            for(ItemPedido i:itens){
                i.setPedidoId(pedidoId);s.setInt(1,pedidoId);s.setInt(2,i.getItemCardapioId());
                s.setInt(3,i.getQuantidade());s.setBigDecimal(4,i.getPrecoUnitario());
                s.setString(5,i.getObservacao());s.addBatch();
            }
            s.executeBatch();
            try(ResultSet r=s.getGeneratedKeys()){int i=0;while(r.next()&&i<itens.size())itens.get(i++).setIdItemPedido(r.getInt(1));}
        }
    }
    public List<ItemPedido> listarPorPedido(int pedidoId) throws SQLException {
        List<ItemPedido> l=new ArrayList<>();
        try(PreparedStatement s=conexao.prepareStatement(
            SQL_BASE+"WHERE ip.pedido_id=? AND ip.ativo=1 ORDER BY ip.id_item_pedido")){
            s.setInt(1,pedidoId);try(ResultSet r=s.executeQuery()){while(r.next())l.add(mapear(r));}
        } return l;
    }
    public ItemPedido buscarPorId(int id) throws SQLException {
        try(PreparedStatement s=conexao.prepareStatement(SQL_BASE+"WHERE ip.id_item_pedido=? AND ip.ativo=1")){
            s.setInt(1,id);try(ResultSet r=s.executeQuery()){if(r.next())return mapear(r);}
        } return null;
    }
    public void atualizarStatus(int id,String status) throws SQLException {
        try(PreparedStatement s=conexao.prepareStatement(
            "UPDATE item_pedido SET status=? WHERE id_item_pedido=?")){
            s.setString(1,status);s.setInt(2,id);s.executeUpdate();
        }
    }
    public void desativar(int id) throws SQLException {
        try(PreparedStatement s=conexao.prepareStatement(
            "UPDATE item_pedido SET ativo=0,status='cancelado' WHERE id_item_pedido=?")){s.setInt(1,id);s.executeUpdate();}
    }
    public void cancelarItensDoPedido(int pedidoId) throws SQLException {
        try(PreparedStatement s=conexao.prepareStatement(
            "UPDATE item_pedido SET status='cancelado' WHERE pedido_id=? AND status IN('pendente','em_preparo')")){
            s.setInt(1,pedidoId);s.executeUpdate();
        }
    }
    private ItemPedido mapear(ResultSet r) throws SQLException {
        ItemCardapio ic=new ItemCardapio();ic.setIdItem(r.getInt("item_cardapio_id"));
        ic.setNome(r.getString("nome_item"));ic.setCategoriaId(r.getInt("categoria_id"));
        ic.setTempoPreparoMin(r.getInt("tempo_preparo_min"));ic.setDisponivel(r.getBoolean("disponivel"));
        ItemPedido ip=new ItemPedido();ip.setIdItemPedido(r.getInt("id_item_pedido"));
        ip.setPedidoId(r.getInt("pedido_id"));ip.setItemCardapioId(r.getInt("item_cardapio_id"));
        ip.setItemCardapio(ic);ip.setQuantidade(r.getInt("quantidade"));
        ip.setPrecoUnitario(r.getBigDecimal("preco_unitario"));ip.setObservacao(r.getString("observacao"));
        ip.setStatus(r.getString("status"));ip.setAtivo(r.getBoolean("ativo"));return ip;
    }
}
