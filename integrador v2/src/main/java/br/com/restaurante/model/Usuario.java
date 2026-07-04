package br.com.restaurante.model;
import java.io.Serializable;
public class Usuario implements Serializable {
    private static final long serialVersionUID = 1L;
    private int idUsuario; private String nome; private String login;
    private String senha; private String perfil; private String funcao; private boolean ativo;
    public Usuario() {}
    public Usuario(String nome,String login,String senha,String perfil,String funcao){
        this.nome=nome;this.login=login;this.senha=senha;this.perfil=perfil;this.funcao=funcao;this.ativo=true;}
    public Usuario(int id,String nome,String login,String senha,String perfil,String funcao,boolean ativo){
        this.idUsuario=id;this.nome=nome;this.login=login;this.senha=senha;this.perfil=perfil;this.funcao=funcao;this.ativo=ativo;}
    public int getIdUsuario(){return idUsuario;} public void setIdUsuario(int v){idUsuario=v;}
    public String getNome(){return nome;} public void setNome(String v){nome=v;}
    public String getLogin(){return login;} public void setLogin(String v){login=v;}
    public String getSenha(){return senha;} public void setSenha(String v){senha=v;}
    public String getPerfil(){return perfil;} public void setPerfil(String v){perfil=v;}
    public String getFuncao(){return funcao;} public void setFuncao(String v){funcao=v;}
    public boolean isAtivo(){return ativo;} public void setAtivo(boolean v){ativo=v;}
    @Override public String toString(){return "Usuario[id="+idUsuario+",login="+login+",perfil="+perfil+"]";}
}
