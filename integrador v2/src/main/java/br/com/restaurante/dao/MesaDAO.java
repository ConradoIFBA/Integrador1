package br.com.restaurante.dao;
import java.sql.*;
import java.util.*;
import br.com.restaurante.model.Mesa;
public class MesaDAO {
    private final Connection conexao;
    public MesaDAO(Connection c){this.conexao=c;}
    public List<Mesa> listar() throws SQLException {
        List<Mesa> l=new ArrayList<>();
        try(PreparedStatement s=conexao.prepareStatement(
            "SELECT * FROM mesa WHERE ativo=1 ORDER BY numero");
            ResultSet r=s.executeQuery()){while(r.next())l.add(mapear(r));}
        return l;
    }
    public List<Mesa> listarLivres() throws SQLException {
        List<Mesa> l=new ArrayList<>();
        try(PreparedStatement s=conexao.prepareStatement(
            "SELECT * FROM mesa WHERE ativo=1 AND status='livre' ORDER BY numero");
            ResultSet r=s.executeQuery()){while(r.next())l.add(mapear(r));}
        return l;
    }
    public Mesa buscarPorId(int id) throws SQLException {
        try(PreparedStatement s=conexao.prepareStatement(
            "SELECT * FROM mesa WHERE id_mesa=? AND ativo=1")){
            s.setInt(1,id);try(ResultSet r=s.executeQuery()){if(r.next())return mapear(r);}
        } return null;
    }
    public void inserir(Mesa m) throws SQLException {
        try(PreparedStatement s=conexao.prepareStatement(
            "INSERT INTO mesa(numero,capacidade,status,ativo) VALUES(?,?,'livre',1)",
            Statement.RETURN_GENERATED_KEYS)){
            s.setInt(1,m.getNumero());s.setInt(2,m.getCapacidade());s.executeUpdate();
            try(ResultSet r=s.getGeneratedKeys()){if(r.next())m.setIdMesa(r.getInt(1));}
        }
    }
    public void atualizarStatus(int id,String status) throws SQLException {
        try(PreparedStatement s=conexao.prepareStatement(
            "UPDATE mesa SET status=? WHERE id_mesa=?")){
            s.setString(1,status);s.setInt(2,id);s.executeUpdate();
        }
    }
    public void editar(Mesa m) throws SQLException {
        try(PreparedStatement s=conexao.prepareStatement(
            "UPDATE mesa SET capacidade=?,status=? WHERE id_mesa=?")){
            s.setInt(1,m.getCapacidade());s.setString(2,m.getStatus());s.setInt(3,m.getIdMesa());s.executeUpdate();
        }
    }
    public void desativar(int id) throws SQLException {
        try(PreparedStatement s=conexao.prepareStatement(
            "UPDATE mesa SET ativo=0 WHERE id_mesa=?")){s.setInt(1,id);s.executeUpdate();}
    }
    private Mesa mapear(ResultSet r) throws SQLException {
        return new Mesa(r.getInt("id_mesa"),r.getInt("numero"),
            r.getInt("capacidade"),r.getString("status"),r.getBoolean("ativo"));
    }
}
