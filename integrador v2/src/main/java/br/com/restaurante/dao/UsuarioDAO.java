package br.com.restaurante.dao;
import java.sql.*;
import br.com.restaurante.model.Usuario;
public class UsuarioDAO {
    private final Connection conexao;
    public UsuarioDAO(Connection c){this.conexao=c;}
    public Usuario buscarPorLogin(String login) throws SQLException {
        try(PreparedStatement s=conexao.prepareStatement(
            "SELECT * FROM usuario WHERE login=? AND ativo=1")){
            s.setString(1,login);
            try(ResultSet r=s.executeQuery()){if(r.next())return mapear(r);}
        } return null;
    }
    public Usuario buscarPorId(int id) throws SQLException {
        try(PreparedStatement s=conexao.prepareStatement(
            "SELECT * FROM usuario WHERE id_usuario=? AND ativo=1")){
            s.setInt(1,id);
            try(ResultSet r=s.executeQuery()){if(r.next())return mapear(r);}
        } return null;
    }
    public void inserir(Usuario u) throws SQLException {
        try(PreparedStatement s=conexao.prepareStatement(
            "INSERT INTO usuario(nome,login,senha,perfil,funcao,ativo) VALUES(?,?,?,?,?,1)",
            Statement.RETURN_GENERATED_KEYS)){
            s.setString(1,u.getNome());s.setString(2,u.getLogin());
            s.setString(3,u.getSenha());s.setString(4,u.getPerfil());s.setString(5,u.getFuncao());
            s.executeUpdate();
            try(ResultSet r=s.getGeneratedKeys()){if(r.next())u.setIdUsuario(r.getInt(1));}
        }
    }
    public void editar(Usuario u) throws SQLException {
        try(PreparedStatement s=conexao.prepareStatement(
            "UPDATE usuario SET nome=?,senha=?,funcao=? WHERE id_usuario=?")){
            s.setString(1,u.getNome());s.setString(2,u.getSenha());
            s.setString(3,u.getFuncao());s.setInt(4,u.getIdUsuario());s.executeUpdate();
        }
    }
    public void desativar(int id) throws SQLException {
        try(PreparedStatement s=conexao.prepareStatement(
            "UPDATE usuario SET ativo=0 WHERE id_usuario=?")){
            s.setInt(1,id);s.executeUpdate();
        }
    }
    private Usuario mapear(ResultSet r) throws SQLException {
        return new Usuario(r.getInt("id_usuario"),r.getString("nome"),r.getString("login"),
            r.getString("senha"),r.getString("perfil"),r.getString("funcao"),r.getBoolean("ativo"));
    }
}
