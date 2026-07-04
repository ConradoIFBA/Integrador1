package br.com.restaurante.model;
import java.io.Serializable;
import java.math.BigDecimal;
public class ItemPedido implements Serializable {
    private static final long serialVersionUID = 1L;
    private int idItemPedido; private int pedidoId; private int itemCardapioId;
    private ItemCardapio itemCardapio; private int quantidade;
    private BigDecimal precoUnitario; private String observacao; private String status; private boolean ativo;
    public ItemPedido() {}
    public ItemPedido(int pedidoId,int itemCardapioId,int qtd,BigDecimal preco,String obs){
        this.pedidoId=pedidoId;this.itemCardapioId=itemCardapioId;this.quantidade=qtd;
        this.precoUnitario=preco;this.observacao=obs;this.status="pendente";this.ativo=true;}
    public ItemPedido(int id,int pedidoId,int itemCardapioId,int qtd,BigDecimal preco,String obs,String status,boolean ativo){
        this.idItemPedido=id;this.pedidoId=pedidoId;this.itemCardapioId=itemCardapioId;
        this.quantidade=qtd;this.precoUnitario=preco;this.observacao=obs;this.status=status;this.ativo=ativo;}
    public int getIdItemPedido(){return idItemPedido;} public void setIdItemPedido(int v){idItemPedido=v;}
    public int getPedidoId(){return pedidoId;} public void setPedidoId(int v){pedidoId=v;}
    public int getItemCardapioId(){return itemCardapioId;} public void setItemCardapioId(int v){itemCardapioId=v;}
    public ItemCardapio getItemCardapio(){return itemCardapio;} public void setItemCardapio(ItemCardapio v){itemCardapio=v;}
    public int getQuantidade(){return quantidade;} public void setQuantidade(int v){quantidade=v;}
    public BigDecimal getPrecoUnitario(){return precoUnitario;} public void setPrecoUnitario(BigDecimal v){precoUnitario=v;}
    public String getObservacao(){return observacao;} public void setObservacao(String v){observacao=v;}
    public String getStatus(){return status;} public void setStatus(String v){status=v;}
    public boolean isAtivo(){return ativo;} public void setAtivo(boolean v){ativo=v;}
    public BigDecimal getSubtotal(){
        return precoUnitario==null?BigDecimal.ZERO:precoUnitario.multiply(BigDecimal.valueOf(quantidade));}
    public String getNomeItem(){return itemCardapio!=null?itemCardapio.getNome():"";}
    @Override public String toString(){return "ItemPedido[id="+idItemPedido+",pedidoId="+pedidoId+"]";}
}
