package br.com.restaurante.dao;
import java.sql.*;
import java.util.*;
import br.com.restaurante.model.CategoriaItem;
public class CategoriaItemDAO {
    private final Connection conexao;
    public CategoriaItemDAO(Connection c){this.conexao=c;}
    public List<CategoriaItem> listar() throws SQLException {
        List<CategoriaItem> l=new ArrayList<>();
        try(PreparedStatement s=conexao.prepareStatement(
            "SELECT * FROM categoria_item WHERE ativo=1 ORDER BY nome");
            ResultSet r=s.executeQuery()){while(r.next())l.add(mapear(r));}
        return l;
    }
    public List<CategoriaItem> listarPorSetor(String setor) throws SQLException {
        List<CategoriaItem> l=new ArrayList<>();
        try(PreparedStatement s=conexao.prepareStatement(
            "SELECT * FROM categoria_item WHERE ativo=1 AND setor=? ORDER BY nome")){
            s.setString(1,setor);try(ResultSet r=s.executeQuery()){while(r.next())l.add(mapear(r));}
        } return l;
    }
    public CategoriaItem buscarPorId(int id) throws SQLException {
        try(PreparedStatement s=conexao.prepareStatement(
            "SELECT * FROM categoria_item WHERE id_categoria=? AND ativo=1")){
            s.setInt(1,id);try(ResultSet r=s.executeQuery()){if(r.next())return mapear(r);}
        } return null;
    }
    public void inserir(CategoriaItem c) throws SQLException {
        try(PreparedStatement s=conexao.prepareStatement(
            "INSERT INTO categoria_item(nome,setor,ativo) VALUES(?,?,1)",Statement.RETURN_GENERATED_KEYS)){
            s.setString(1,c.getNome());s.setString(2,c.getSetor());s.executeUpdate();
            try(ResultSet r=s.getGeneratedKeys()){if(r.next())c.setIdCategoria(r.getInt(1));}
        }
    }
    public void editar(CategoriaItem c) throws SQLException {
        try(PreparedStatement s=conexao.prepareStatement(
            "UPDATE categoria_item SET nome=?,setor=? WHERE id_categoria=?")){
            s.setString(1,c.getNome());s.setString(2,c.getSetor());s.setInt(3,c.getIdCategoria());s.executeUpdate();
        }
    }
    public void desativar(int id) throws SQLException {
        try(PreparedStatement s=conexao.prepareStatement(
            "UPDATE categoria_item SET ativo=0 WHERE id_categoria=?")){s.setInt(1,id);s.executeUpdate();}
    }
    private CategoriaItem mapear(ResultSet r) throws SQLException {
        return new CategoriaItem(r.getInt("id_categoria"),r.getString("nome"),r.getString("setor"),r.getBoolean("ativo"));
    }
}
