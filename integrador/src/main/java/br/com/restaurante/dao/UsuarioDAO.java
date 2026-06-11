package br.com.restaurante.dao;

import java.sql.*;
import br.com.restaurante.model.Usuario;

/**
 * DAO para a tabela usuario.
 *
 * Métodos:
 *   buscarPorLogin(String)  → autenticação
 *   buscarPorId(int)        → carregar da sessão
 *   inserir(Usuario)        → cadastro (não usado neste projeto, mas disponível)
 *   editar(Usuario)         → atualizar dados
 *   desativar(int)          → soft delete
 */
public class UsuarioDAO {

    private final Connection conexao;

    public UsuarioDAO(Connection conexao) {
        this.conexao = conexao;
    }

    // ----------------------------------------------------------------
    // BUSCAR POR LOGIN — usado pelo AuthController no fluxo de login
    // ----------------------------------------------------------------

    /**
     * Busca um usuário ativo pelo login.
     * Retorna null se não encontrar ou se estiver inativo.
     */
    public Usuario buscarPorLogin(String login) throws SQLException {
        String sql = "SELECT * FROM usuario WHERE login = ? AND ativo = 1";

        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setString(1, login);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapear(rs);
                }
            }
        }
        return null;
    }

    // ----------------------------------------------------------------
    // BUSCAR POR ID
    // ----------------------------------------------------------------

    public Usuario buscarPorId(int id) throws SQLException {
        String sql = "SELECT * FROM usuario WHERE id_usuario = ? AND ativo = 1";

        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapear(rs);
                }
            }
        }
        return null;
    }

    // ----------------------------------------------------------------
    // INSERIR
    // ----------------------------------------------------------------

    public void inserir(Usuario u) throws SQLException {
        String sql = "INSERT INTO usuario (nome, login, senha, perfil, funcao, ativo) " +
                     "VALUES (?, ?, ?, ?, ?, 1)";

        try (PreparedStatement stmt = conexao.prepareStatement(sql,
                Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, u.getNome());
            stmt.setString(2, u.getLogin());
            stmt.setString(3, u.getSenha());
            stmt.setString(4, u.getPerfil());
            stmt.setString(5, u.getFuncao()); // nullable

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) u.setIdUsuario(rs.getInt(1));
            }
        }
    }

    // ----------------------------------------------------------------
    // EDITAR — atualiza nome e senha (login e perfil não mudam)
    // ----------------------------------------------------------------

    public void editar(Usuario u) throws SQLException {
        String sql = "UPDATE usuario SET nome = ?, senha = ?, funcao = ? " +
                     "WHERE id_usuario = ?";

        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setString(1, u.getNome());
            stmt.setString(2, u.getSenha());
            stmt.setString(3, u.getFuncao());
            stmt.setInt(4, u.getIdUsuario());
            stmt.executeUpdate();
        }
    }

    // ----------------------------------------------------------------
    // SOFT DELETE
    // ----------------------------------------------------------------

    public void desativar(int id) throws SQLException {
        String sql = "UPDATE usuario SET ativo = 0 WHERE id_usuario = ?";

        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    // ----------------------------------------------------------------
    // MAPEAMENTO ResultSet → Usuario
    // ----------------------------------------------------------------

    private Usuario mapear(ResultSet rs) throws SQLException {
        Usuario u = new Usuario();
        u.setIdUsuario(rs.getInt("id_usuario"));
        u.setNome(rs.getString("nome"));
        u.setLogin(rs.getString("login"));
        u.setSenha(rs.getString("senha"));
        u.setPerfil(rs.getString("perfil"));
        u.setFuncao(rs.getString("funcao")); // pode ser null
        u.setAtivo(rs.getBoolean("ativo"));
        return u;
    }
}
