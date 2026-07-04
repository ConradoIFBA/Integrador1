package br.com.restaurante.model;
import java.io.Serializable;
import java.math.BigDecimal;
public class ItemCardapio implements Serializable {
    private static final long serialVersionUID = 1L;
    private int idItem; private int categoriaId; private CategoriaItem categoria;
    private String nome; private String descricao; private BigDecimal preco;
    private int tempoPreparoMin; private boolean disponivel; private boolean ativo;
    public ItemCardapio() {}
    public ItemCardapio(int id,int catId,String nome,String desc,BigDecimal preco,int tempo,boolean disp,boolean ativo){
        this.idItem=id;this.categoriaId=catId;this.nome=nome;this.descricao=desc;
        this.preco=preco;this.tempoPreparoMin=tempo;this.disponivel=disp;this.ativo=ativo;}
    public int getIdItem(){return idItem;} public void setIdItem(int v){idItem=v;}
    public int getCategoriaId(){return categoriaId;} public void setCategoriaId(int v){categoriaId=v;}
    public CategoriaItem getCategoria(){return categoria;} public void setCategoria(CategoriaItem v){categoria=v;}
    public String getNome(){return nome;} public void setNome(String v){nome=v;}
    public String getDescricao(){return descricao;} public void setDescricao(String v){descricao=v;}
    public BigDecimal getPreco(){return preco;} public void setPreco(BigDecimal v){preco=v;}
    public int getTempoPreparoMin(){return tempoPreparoMin;} public void setTempoPreparoMin(int v){tempoPreparoMin=v;}
    public boolean isDisponivel(){return disponivel;} public void setDisponivel(boolean v){disponivel=v;}
    public boolean isAtivo(){return ativo;} public void setAtivo(boolean v){ativo=v;}
    public String getNomeCategoria(){return categoria!=null?categoria.getNome():"";}
    @Override public String toString(){return "ItemCardapio[id="+idItem+",nome="+nome+"]";}
}
