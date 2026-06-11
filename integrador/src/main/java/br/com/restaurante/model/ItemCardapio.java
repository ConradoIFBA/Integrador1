package br.com.restaurante.model;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Tabela: item_cardapio
 *
 * disponivel = 0 → item bloqueado por falta de estoque (sem excluir).
 * ativo      = 0 → soft delete.
 */
public class ItemCardapio implements Serializable {

    private static final long serialVersionUID = 1L;

    private int           idItem;
    private int           categoriaId;
    private CategoriaItem categoria;      // nullable — populado em queries com JOIN
    private String        nome;
    private String        descricao;
    private BigDecimal    preco;
    private int           tempoPreparoMin;
    private boolean       disponivel;
    private boolean       ativo;

    public ItemCardapio() {}

    public ItemCardapio(int idItem, int categoriaId, String nome, String descricao,
                        BigDecimal preco, int tempoPreparoMin,
                        boolean disponivel, boolean ativo) {
        this.idItem          = idItem;
        this.categoriaId     = categoriaId;
        this.nome            = nome;
        this.descricao       = descricao;
        this.preco           = preco;
        this.tempoPreparoMin = tempoPreparoMin;
        this.disponivel      = disponivel;
        this.ativo           = ativo;
    }

    public int           getIdItem()                       { return idItem; }
    public void          setIdItem(int v)                  { this.idItem = v; }

    public int           getCategoriaId()                  { return categoriaId; }
    public void          setCategoriaId(int v)             { this.categoriaId = v; }

    public CategoriaItem getCategoria()                    { return categoria; }
    public void          setCategoria(CategoriaItem v)     { this.categoria = v; }

    public String        getNome()                         { return nome; }
    public void          setNome(String v)                 { this.nome = v; }

    public String        getDescricao()                    { return descricao; }
    public void          setDescricao(String v)            { this.descricao = v; }

    public BigDecimal    getPreco()                        { return preco; }
    public void          setPreco(BigDecimal v)            { this.preco = v; }

    public int           getTempoPreparoMin()              { return tempoPreparoMin; }
    public void          setTempoPreparoMin(int v)         { this.tempoPreparoMin = v; }

    public boolean       isDisponivel()                    { return disponivel; }
    public void          setDisponivel(boolean v)          { this.disponivel = v; }

    public boolean       isAtivo()                         { return ativo; }
    public void          setAtivo(boolean v)               { this.ativo = v; }

    /** Atalho para JSP: ${item.nomeCategoria} */
    public String getNomeCategoria() {
        return categoria != null ? categoria.getNome() : "";
    }

    @Override
    public String toString() {
        return "ItemCardapio [id=" + idItem + ", nome=" + nome + ", preco=" + preco + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return idItem == ((ItemCardapio) o).idItem;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(idItem);
    }
}
