package br.com.restaurante.model;

import java.io.Serializable;

/**
 * Tabela: usuario
 *
 * perfil : 'GERENTE' | 'FUNCIONARIO' | 'USUARIO'
 * funcao : 'atendente' | 'cozinha' | null  (só para FUNCIONARIO)
 */
public class Usuario implements Serializable {

    private static final long serialVersionUID = 1L;

    private int     idUsuario;
    private String  nome;
    private String  login;
    private String  senha;     // hash BCrypt — nunca expor na view
    private String  perfil;
    private String  funcao;    // nullable
    private boolean ativo;

    public Usuario() {}

    public Usuario(String nome, String login, String senha, String perfil, String funcao) {
        this.nome   = nome;
        this.login  = login;
        this.senha  = senha;
        this.perfil = perfil;
        this.funcao = funcao;
        this.ativo  = true;
    }

    public Usuario(int idUsuario, String nome, String login, String senha,
                   String perfil, String funcao, boolean ativo) {
        this.idUsuario = idUsuario;
        this.nome      = nome;
        this.login     = login;
        this.senha     = senha;
        this.perfil    = perfil;
        this.funcao    = funcao;
        this.ativo     = ativo;
    }

    public int     getIdUsuario()          { return idUsuario; }
    public void    setIdUsuario(int v)     { this.idUsuario = v; }

    public String  getNome()               { return nome; }
    public void    setNome(String v)       { this.nome = v; }

    public String  getLogin()              { return login; }
    public void    setLogin(String v)      { this.login = v; }

    public String  getSenha()              { return senha; }
    public void    setSenha(String v)      { this.senha = v; }

    public String  getPerfil()             { return perfil; }
    public void    setPerfil(String v)     { this.perfil = v; }

    public String  getFuncao()             { return funcao; }
    public void    setFuncao(String v)     { this.funcao = v; }

    public boolean isAtivo()               { return ativo; }
    public void    setAtivo(boolean v)     { this.ativo = v; }

    @Override
    public String toString() {
        return "Usuario [id=" + idUsuario + ", login=" + login + ", perfil=" + perfil + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return idUsuario == ((Usuario) o).idUsuario;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(idUsuario);
    }
}
