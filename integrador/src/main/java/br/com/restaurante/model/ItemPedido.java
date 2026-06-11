package br.com.restaurante.model;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Tabela: item_pedido
 *
 * status : 'pendente' | 'em_preparo' | 'pronto' | 'entregue' | 'cancelado'
 *
 * precoUnitario é gravado no momento do pedido para preservar o histórico
 * mesmo que o cardápio seja alterado depois.
 */
public class ItemPedido implements Serializable {

    private static final long serialVersionUID = 1L;

    private int          idItemPedido;
    private int          pedidoId;
    private int          itemCardapioId;
    private ItemCardapio itemCardapio;   // nullable — populado com JOIN
    private int          quantidade;
    private BigDecimal   precoUnitario;
    private String       observacao;
    private String       status;
    private boolean      ativo;

    public ItemPedido() {}

    public ItemPedido(int pedidoId, int itemCardapioId,
                      int quantidade, BigDecimal precoUnitario, String observacao) {
        this.pedidoId       = pedidoId;
        this.itemCardapioId = itemCardapioId;
        this.quantidade     = quantidade;
        this.precoUnitario  = precoUnitario;
        this.observacao     = observacao;
        this.status         = "pendente";
        this.ativo          = true;
    }

    public ItemPedido(int idItemPedido, int pedidoId, int itemCardapioId,
                      int quantidade, BigDecimal precoUnitario,
                      String observacao, String status, boolean ativo) {
        this.idItemPedido   = idItemPedido;
        this.pedidoId       = pedidoId;
        this.itemCardapioId = itemCardapioId;
        this.quantidade     = quantidade;
        this.precoUnitario  = precoUnitario;
        this.observacao     = observacao;
        this.status         = status;
        this.ativo          = ativo;
    }

    public int          getIdItemPedido()              { return idItemPedido; }
    public void         setIdItemPedido(int v)         { this.idItemPedido = v; }

    public int          getPedidoId()                  { return pedidoId; }
    public void         setPedidoId(int v)             { this.pedidoId = v; }

    public int          getItemCardapioId()            { return itemCardapioId; }
    public void         setItemCardapioId(int v)       { this.itemCardapioId = v; }

    public ItemCardapio getItemCardapio()              { return itemCardapio; }
    public void         setItemCardapio(ItemCardapio v){ this.itemCardapio = v; }

    public int          getQuantidade()                { return quantidade; }
    public void         setQuantidade(int v)           { this.quantidade = v; }

    public BigDecimal   getPrecoUnitario()             { return precoUnitario; }
    public void         setPrecoUnitario(BigDecimal v) { this.precoUnitario = v; }

    public String       getObservacao()                { return observacao; }
    public void         setObservacao(String v)        { this.observacao = v; }

    public String       getStatus()                    { return status; }
    public void         setStatus(String v)            { this.status = v; }

    public boolean      isAtivo()                      { return ativo; }
    public void         setAtivo(boolean v)            { this.ativo = v; }

    /** Subtotal = precoUnitario × quantidade. */
    public BigDecimal getSubtotal() {
        if (precoUnitario == null) return BigDecimal.ZERO;
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }

    /** Atalho para JSP: ${item.nomeItem} */
    public String getNomeItem() {
        return itemCardapio != null ? itemCardapio.getNome() : "";
    }

    @Override
    public String toString() {
        return "ItemPedido [id=" + idItemPedido + ", pedidoId=" + pedidoId
               + ", itemCardapioId=" + itemCardapioId + ", qtd=" + quantidade + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return idItemPedido == ((ItemPedido) o).idItemPedido;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(idItemPedido);
    }
}
