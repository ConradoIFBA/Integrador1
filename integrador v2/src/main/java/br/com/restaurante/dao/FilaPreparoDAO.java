package br.com.restaurante.dao;
import java.sql.*;
import java.util.*;
import br.com.restaurante.model.FilaPreparo;
import br.com.restaurante.model.Mesa;
import br.com.restaurante.model.Pedido;
public class FilaPreparoDAO {
    private final Connection conexao;
    private static final String SQL_BASE=
        "SELECT f.*,p.tipo,p.urgente,p.identificador_operador AS operador_pedido,"+
        "p.status AS status_pedido,p.observacao AS obs_pedido,p.data_abertura,p.mesa_id,m.numero AS mesa_numero "+
        "FROM fila_preparo f INNER JOIN pedido p ON f.pedido_id=p.id_pedido "+
        "LEFT JOIN mesa m ON p.mesa_id=m.id_mesa ";
    public FilaPreparoDAO(Connection c){this.conexao=c;}
    public List<FilaPreparo> listarFila(String setor) throws SQLException {
        List<FilaPreparo> l=new ArrayList<>();
        try(PreparedStatement s=conexao.prepareStatement(SQL_BASE+
            "WHERE f.ativo=1 AND f.setor=? AND f.data_conclusao IS NULL "+
            "ORDER BY f.peso_prioridade DESC,f.data_entrada ASC")){
            s.setString(1,setor);try(ResultSet r=s.executeQuery()){while(r.next())l.add(mapear(r));}
        } return l;
    }
    public List<FilaPreparo> listarFilaGeral() throws SQLException {
        List<FilaPreparo> l=new ArrayList<>();
        try(PreparedStatement s=conexao.prepareStatement(SQL_BASE+
            "WHERE f.ativo=1 AND f.data_conclusao IS NULL ORDER BY f.peso_prioridade DESC,f.data_entrada ASC");
            ResultSet r=s.executeQuery()){while(r.next())l.add(mapear(r));}
        return l;
    }
    public FilaPreparo buscarPorPedido(int pedidoId) throws SQLException {
        try(PreparedStatement s=conexao.prepareStatement(SQL_BASE+"WHERE f.pedido_id=? AND f.ativo=1")){
            s.setInt(1,pedidoId);try(ResultSet r=s.executeQuery()){if(r.next())return mapear(r);}
        } return null;
    }
    public FilaPreparo buscarPorId(int id) throws SQLException {
        try(PreparedStatement s=conexao.prepareStatement(SQL_BASE+"WHERE f.id_fila=? AND f.ativo=1")){
            s.setInt(1,id);try(ResultSet r=s.executeQuery()){if(r.next())return mapear(r);}
        } return null;
    }
    public void inserir(FilaPreparo f) throws SQLException {
        f.setPosicao(proximaPosicao(f.getSetor()));
        try(PreparedStatement s=conexao.prepareStatement(
            "INSERT INTO fila_preparo(pedido_id,posicao,peso_prioridade,tempo_estimado_min,setor,ativo) VALUES(?,?,?,?,?,1)",
            Statement.RETURN_GENERATED_KEYS)){
            s.setInt(1,f.getPedidoId());s.setInt(2,f.getPosicao());s.setInt(3,f.getPesoPrioridade());
            s.setInt(4,f.getTempoEstimadoMin());s.setString(5,f.getSetor());s.executeUpdate();
            try(ResultSet r=s.getGeneratedKeys()){if(r.next())f.setIdFila(r.getInt(1));}
        }
    }
    public void iniciarPreparo(int id,String operador) throws SQLException {
        try(PreparedStatement s=conexao.prepareStatement(
            "UPDATE fila_preparo SET data_inicio_preparo=NOW(),identificador_operador=? WHERE id_fila=?")){
            s.setString(1,operador);s.setInt(2,id);s.executeUpdate();
        }
    }
    public void concluir(int id) throws SQLException {
        try(PreparedStatement s=conexao.prepareStatement(
            "UPDATE fila_preparo SET data_conclusao=NOW() WHERE id_fila=?")){s.setInt(1,id);s.executeUpdate();}
    }
    public void desativar(int id) throws SQLException {
        try(PreparedStatement s=conexao.prepareStatement(
            "UPDATE fila_preparo SET ativo=0 WHERE id_fila=?")){s.setInt(1,id);s.executeUpdate();}
    }
    private int proximaPosicao(String setor) throws SQLException {
        try(PreparedStatement s=conexao.prepareStatement(
            "SELECT COALESCE(MAX(posicao),0)+1 AS prox FROM fila_preparo WHERE ativo=1 AND setor=?")){
            s.setString(1,setor);try(ResultSet r=s.executeQuery()){return r.next()?r.getInt("prox"):1;}
        }
    }
    private FilaPreparo mapear(ResultSet r) throws SQLException {
        Mesa mesa=null;int mesaId=r.getInt("mesa_id");
        if(!r.wasNull()){mesa=new Mesa();mesa.setIdMesa(mesaId);mesa.setNumero(r.getInt("mesa_numero"));}
        Pedido p=new Pedido();p.setIdPedido(r.getInt("pedido_id"));p.setTipo(r.getString("tipo"));
        p.setUrgente(r.getBoolean("urgente"));p.setIdentificadorOperador(r.getString("operador_pedido"));
        p.setStatus(r.getString("status_pedido"));p.setObservacao(r.getString("obs_pedido"));
        p.setDataAbertura(r.getTimestamp("data_abertura").toLocalDateTime());
        if(mesa!=null){p.setMesaId(mesaId);p.setMesa(mesa);}
        FilaPreparo f=new FilaPreparo();f.setIdFila(r.getInt("id_fila"));f.setPedidoId(r.getInt("pedido_id"));
        f.setPedido(p);f.setPosicao(r.getInt("posicao"));f.setPesoPrioridade(r.getInt("peso_prioridade"));
        f.setTempoEstimadoMin(r.getInt("tempo_estimado_min"));f.setSetor(r.getString("setor"));
        f.setDataEntrada(r.getTimestamp("data_entrada").toLocalDateTime());
        Timestamp ini=r.getTimestamp("data_inicio_preparo");if(ini!=null)f.setDataInicioPreparo(ini.toLocalDateTime());
        Timestamp con=r.getTimestamp("data_conclusao");if(con!=null)f.setDataConclusao(con.toLocalDateTime());
        f.setIdentificadorOperador(r.getString("identificador_operador"));f.setAtivo(r.getBoolean("ativo"));
        return f;
    }
}
