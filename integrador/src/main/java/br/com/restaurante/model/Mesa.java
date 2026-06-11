package br.com.restaurante.model;

import java.io.Serializable;

/**
 * Tabela: mesa
 *
 * status : 'livre' | 'ocupada' | 'reservada'
 */
public class Mesa implements Serializable {

    private static final long serialVersionUID = 1L;

    private int     idMesa;
    private int     numero;
    private int     capacidade;
    private String  status;
    private boolean ativo;

    public Mesa() {}

    public Mesa(int numero, int capacidade) {
        this.numero     = numero;
        this.capacidade = capacidade;
        this.status     = "livre";
        this.ativo      = true;
    }

    public Mesa(int idMesa, int numero, int capacidade, String status, boolean ativo) {
        this.idMesa     = idMesa;
        this.numero     = numero;
        this.capacidade = capacidade;
        this.status     = status;
        this.ativo      = ativo;
    }

    public int     getIdMesa()             { return idMesa; }
    public void    setIdMesa(int v)        { this.idMesa = v; }

    public int     getNumero()             { return numero; }
    public void    setNumero(int v)        { this.numero = v; }

    public int     getCapacidade()         { return capacidade; }
    public void    setCapacidade(int v)    { this.capacidade = v; }

    public String  getStatus()             { return status; }
    public void    setStatus(String v)     { this.status = v; }

    public boolean isAtivo()               { return ativo; }
    public void    setAtivo(boolean v)     { this.ativo = v; }

    /** Atalho para verificar se mesa está livre. */
    public boolean isLivre() { return "livre".equals(status); }

    @Override
    public String toString() {
        return "Mesa [id=" + idMesa + ", numero=" + numero + ", status=" + status + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return idMesa == ((Mesa) o).idMesa;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(idMesa);
    }
}
