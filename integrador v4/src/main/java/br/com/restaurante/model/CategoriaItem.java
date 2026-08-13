package br.com.restaurante.model;
import java.io.Serializable;
public class CategoriaItem implements Serializable {
    private static final long serialVersionUID = 1L;
    private int idCategoria; private String nome; private String setor; private boolean ativo;
    public CategoriaItem() {}
    public CategoriaItem(int id,String nome){this.idCategoria=id;this.nome=nome;this.ativo=true;}
    public CategoriaItem(int id,String nome,String setor,boolean ativo){
        this.idCategoria=id;this.nome=nome;this.setor=setor;this.ativo=ativo;}
    public int getIdCategoria(){return idCategoria;} public void setIdCategoria(int v){idCategoria=v;}
    public String getNome(){return nome;} public void setNome(String v){nome=v;}
    public String getSetor(){return setor;} public void setSetor(String v){setor=v;}
    public boolean isAtivo(){return ativo;} public void setAtivo(boolean v){ativo=v;}
    @Override public String toString(){return nome;}
}
