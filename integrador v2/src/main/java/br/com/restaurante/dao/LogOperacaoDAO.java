package br.com.restaurante.dao;
import java.sql.*;
import java.util.*;
import br.com.restaurante.model.LogOperacao;
public class LogOperacaoDAO {
    private final Connection conexao;
    public LogOperacaoDAO(Connection c){this.conexao=c;}
    public void registrar(LogOperacao log) throws SQLException {
        try(PreparedStatement s=conexao.prepareStatement(
            "INSERT INTO log_operacao(perfil,funcao,identificador_operador,descricao) VALUES(?,?,?,?)",
            Statement.RETURN_GENERATED_KEYS)){
            s.setString(1,log.getPerfil());s.setString(2,log.getFuncao());
            s.setString(3,log.getIdentificadorOperador());s.setString(4,log.getDescricao());s.executeUpdate();
            try(ResultSet r=s.getGeneratedKeys()){if(r.next())log.setIdLog(r.getInt(1));}
        }
    }
    public List<LogOperacao> listarPorOperador(String operador) throws SQLException {
        List<LogOperacao> l=new ArrayList<>();
        try(PreparedStatement s=conexao.prepareStatement(
            "SELECT * FROM log_operacao WHERE identificador_operador=? ORDER BY data_hora DESC")){
            s.setString(1,operador);try(ResultSet r=s.executeQuery()){while(r.next())l.add(mapear(r));}
        } return l;
    }
    public List<LogOperacao> listarRecentes(int limite) throws SQLException {
        List<LogOperacao> l=new ArrayList<>();
        try(PreparedStatement s=conexao.prepareStatement(
            "SELECT * FROM log_operacao ORDER BY data_hora DESC LIMIT ?")){
            s.setInt(1,limite);try(ResultSet r=s.executeQuery()){while(r.next())l.add(mapear(r));}
        } return l;
    }
    private LogOperacao mapear(ResultSet r) throws SQLException {
        return new LogOperacao(r.getInt("id_log"),r.getString("perfil"),r.getString("funcao"),
            r.getString("identificador_operador"),r.getString("descricao"),
            r.getTimestamp("data_hora").toLocalDateTime());
    }
}
