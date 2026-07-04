package br.com.restaurante.dao;
import java.sql.*;
import java.util.*;
import br.com.restaurante.model.Mesa;
import br.com.restaurante.model.Pedido;
public class PedidoDAO {
    private final Connection conexao;
    private static final String SQL_BASE=
        "SELECT p.*,m.numero AS mesa_numero,m.capacidade,m.status AS mesa_status "+
        "FROM pedido p LEFT JOIN mesa m ON p.mesa_id=m.id_mesa ";
    public PedidoDAO(Connection c){this.conexao=c;}
    public List<Pedido> listarAbertos() throws SQLException {
        List<Pedido> l=new ArrayList<>();
        try(PreparedStatement s=conexao.prepareStatement(SQL_BASE+
            "WHERE p.ativo=1 AND p.status NOT IN('entregue','cancelado','estornado') "+
            "ORDER BY p.urgente DESC,p.data_abertura ASC");
            ResultSet r=s.executeQuery()){while(r.next())l.add(mapear(r));}
        return l;
    }
    public List<Pedido> listarPorMesa(int mesaId) throws SQLException {
        List<Pedido> l=new ArrayList<>();
        try(PreparedStatement s=conexao.prepareStatement(SQL_BASE+
            "WHERE p.mesa_id=? AND p.ativo=1 AND p.status NOT IN('entregue','cancelado','estornado') "+
            "ORDER BY p.data_abertura DESC")){
            s.setInt(1,mesaId);try(ResultSet r=s.executeQuery()){while(r.next())l.add(mapear(r));}
        } return l;
    }
    public List<Pedido> listarPorStatus(String status) throws SQLException {
        List<Pedido> l=new ArrayList<>();
        try(PreparedStatement s=conexao.prepareStatement(SQL_BASE+
            "WHERE p.status=? AND p.ativo=1 ORDER BY p.urgente DESC,p.data_abertura ASC")){
            s.setString(1,status);try(ResultSet r=s.executeQuery()){while(r.next())l.add(mapear(r));}
        } return l;
    }
    public Pedido buscarPorId(int id) throws SQLException {
        try(PreparedStatement s=conexao.prepareStatement(SQL_BASE+"WHERE p.id_pedido=? AND p.ativo=1")){
            s.setInt(1,id);try(ResultSet r=s.executeQuery()){if(r.next())return mapear(r);}
        } return null;
    }
    public void inserir(Pedido p) throws SQLException {
        try(PreparedStatement s=conexao.prepareStatement(
            "INSERT INTO pedido(mesa_id,tipo,urgente,identificador_operador,status,observacao,ativo) VALUES(?,?,?,?,'aberto',?,1)",
            Statement.RETURN_GENERATED_KEYS)){
            if(p.getMesaId()!=null)s.setInt(1,p.getMesaId());else s.setNull(1,Types.INTEGER);
            s.setString(2,p.getTipo());s.setBoolean(3,p.isUrgente());
            s.setString(4,p.getIdentificadorOperador());s.setString(5,p.getObservacao());
            s.executeUpdate();
            try(ResultSet r=s.getGeneratedKeys()){if(r.next())p.setIdPedido(r.getInt(1));}
        }
    }
    public void atualizarStatus(int id,String status) throws SQLException {
        try(PreparedStatement s=conexao.prepareStatement(
            "UPDATE pedido SET status=? WHERE id_pedido=?")){
            s.setString(1,status);s.setInt(2,id);s.executeUpdate();
        }
    }
    public void editar(Pedido p) throws SQLException {
        try(PreparedStatement s=conexao.prepareStatement(
            "UPDATE pedido SET urgente=?,observacao=? WHERE id_pedido=?")){
            s.setBoolean(1,p.isUrgente());s.setString(2,p.getObservacao());s.setInt(3,p.getIdPedido());s.executeUpdate();
        }
    }
    public void desativar(int id) throws SQLException {
        try(PreparedStatement s=conexao.prepareStatement(
            "UPDATE pedido SET ativo=0,status='cancelado' WHERE id_pedido=?")){s.setInt(1,id);s.executeUpdate();}
    }
    private Pedido mapear(ResultSet r) throws SQLException {
        Pedido p=new Pedido();
        p.setIdPedido(r.getInt("id_pedido"));
        int mesaId=r.getInt("mesa_id");
        if(!r.wasNull()){
            p.setMesaId(mesaId);
            Mesa m=new Mesa();m.setIdMesa(mesaId);m.setNumero(r.getInt("mesa_numero"));
            m.setCapacidade(r.getInt("capacidade"));m.setStatus(r.getString("mesa_status"));
            p.setMesa(m);
        }
        p.setTipo(r.getString("tipo"));p.setUrgente(r.getBoolean("urgente"));
        p.setIdentificadorOperador(r.getString("identificador_operador"));
        p.setStatus(r.getString("status"));p.setObservacao(r.getString("observacao"));
        p.setDataAbertura(r.getTimestamp("data_abertura").toLocalDateTime());
        p.setAtivo(r.getBoolean("ativo"));return p;
    }
}
