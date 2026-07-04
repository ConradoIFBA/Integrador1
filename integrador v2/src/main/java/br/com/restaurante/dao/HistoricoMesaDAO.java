package br.com.restaurante.dao;
import java.sql.*;
import java.util.*;
import br.com.restaurante.model.HistoricoMesa;
public class HistoricoMesaDAO {
    private final Connection conexao;
    public HistoricoMesaDAO(Connection c){this.conexao=c;}
    public void registrar(HistoricoMesa h) throws SQLException {
        try(PreparedStatement s=conexao.prepareStatement(
            "INSERT INTO historico_mesa(mesa_id,descricao,ativo) VALUES(?,?,1)",
            Statement.RETURN_GENERATED_KEYS)){
            s.setInt(1,h.getMesaId());s.setString(2,h.getDescricao());s.executeUpdate();
            try(ResultSet r=s.getGeneratedKeys()){if(r.next())h.setIdHistorico(r.getInt(1));}
        }
    }
    public List<HistoricoMesa> listarPorMesa(int mesaId) throws SQLException {
        List<HistoricoMesa> l=new ArrayList<>();
        try(PreparedStatement s=conexao.prepareStatement(
            "SELECT * FROM historico_mesa WHERE mesa_id=? AND ativo=1 ORDER BY data_hora ASC")){
            s.setInt(1,mesaId);try(ResultSet r=s.executeQuery()){while(r.next())l.add(mapear(r));}
        } return l;
    }
    public List<HistoricoMesa> listarPorMesaHoje(int mesaId) throws SQLException {
        List<HistoricoMesa> l=new ArrayList<>();
        try(PreparedStatement s=conexao.prepareStatement(
            "SELECT * FROM historico_mesa WHERE mesa_id=? AND ativo=1 AND DATE(data_hora)=CURDATE() ORDER BY data_hora ASC")){
            s.setInt(1,mesaId);try(ResultSet r=s.executeQuery()){while(r.next())l.add(mapear(r));}
        } return l;
    }
    private HistoricoMesa mapear(ResultSet r) throws SQLException {
        HistoricoMesa h=new HistoricoMesa();
        h.setIdHistorico(r.getInt("id_historico"));h.setMesaId(r.getInt("mesa_id"));
        h.setDescricao(r.getString("descricao"));
        h.setDataHora(r.getTimestamp("data_hora").toLocalDateTime());
        h.setAtivo(r.getBoolean("ativo"));return h;
    }
}
