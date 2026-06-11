package br.com.restaurante.model;

import java.io.Serializable;

/**
 * Tabela: categoria_item
 *
 * setor : 'cozinha' | 'bebida' | 'sobremesa'
 */
public class CategoriaItem implements Serializable {

    private static final long serialVersionUID = 1L;

    private int     idCategoria;
    private String  nome;
    private String  setor;
    private boolean ativo;

    public CategoriaItem() {}

    public CategoriaItem(int idCategoria, String nome) {
        this.idCategoria = idCategoria;
        this.nome        = nome;
        this.ativo       = true;
    }

    public CategoriaItem(int idCategoria, String nome, String setor, boolean ativo) {
        this.idCategoria = idCategoria;
        this.nome        = nome;
        this.setor       = setor;
        this.ativo       = ativo;
    }

    public int     getIdCategoria()          { return idCategoria; }
    public void    setIdCategoria(int v)     { this.idCategoria = v; }

    public String  getNome()                 { return nome; }
    public void    setNome(String v)         { this.nome = v; }

    public String  getSetor()                { return setor; }
    public void    setSetor(String v)        { this.setor = v; }

    public boolean isAtivo()                 { return ativo; }
    public void    setAtivo(boolean v)       { this.ativo = v; }

    /** Útil em <select> JSP: ${categoria} exibe o nome diretamente. */
    @Override
    public String toString() { return nome; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return idCategoria == ((CategoriaItem) o).idCategoria;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(idCategoria);
    }
}
