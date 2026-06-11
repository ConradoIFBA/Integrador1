package br.com.restaurante.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Tabela: pedido
 *
 * tipo   : 'mesa' | 'delivery'
 * status : 'aberto' | 'em_preparo' | 'pronto' | 'entregue' | 'cancelado' | 'estornado'
 *
 * mesaId nullable → pedido é delivery quando null.
 *
 * Peso de prioridade na fila: mesa urgente=4, mesa=3, delivery urgente=2, delivery=1.
 */
public class Pedido implements Serializable {

    private static final long serialVersionUID = 1L;

    private int              idPedido;
    private Integer          mesaId;            // nullable
    private Mesa             mesa;              // nullable — populado com JOIN
    private String           tipo;
    private boolean          urgente;
    private String           identificadorOperador;
    private String           status;
    private String           observacao;
    private LocalDateTime    dataAbertura;
    private boolean          ativo;

    private List<ItemPedido> itens = new ArrayList<>(); // populado sob demanda

    public Pedido() {}

    public Pedido(Integer mesaId, String tipo, boolean urgente,
                  String identificadorOperador, String observacao) {
        this.mesaId                = mesaId;
        this.tipo                  = tipo;
        this.urgente               = urgente;
        this.identificadorOperador = identificadorOperador;
        this.observacao            = observacao;
        this.status                = "aberto";
        this.ativo                 = true;
    }

    public Pedido(int idPedido, Integer mesaId, String tipo, boolean urgente,
                  String identificadorOperador, String status,
                  String observacao, LocalDateTime dataAbertura, boolean ativo) {
        this.idPedido              = idPedido;
        this.mesaId                = mesaId;
        this.tipo                  = tipo;
        this.urgente               = urgente;
        this.identificadorOperador = identificadorOperador;
        this.status                = status;
        this.observacao            = observacao;
        this.dataAbertura          = dataAbertura;
        this.ativo                 = ativo;
    }

    public int           getIdPedido()                       { return idPedido; }
    public void          setIdPedido(int v)                  { this.idPedido = v; }

    public Integer       getMesaId()                         { return mesaId; }
    public void          setMesaId(Integer v)                { this.mesaId = v; }

    public Mesa          getMesa()                           { return mesa; }
    public void          setMesa(Mesa v)                     { this.mesa = v; }

    public String        getTipo()                           { return tipo; }
    public void          setTipo(String v)                   { this.tipo = v; }

    public boolean       isUrgente()                         { return urgente; }
    public void          setUrgente(boolean v)               { this.urgente = v; }

    public String        getIdentificadorOperador()          { return identificadorOperador; }
    public void          setIdentificadorOperador(String v)  { this.identificadorOperador = v; }

    public String        getStatus()                         { return status; }
    public void          setStatus(String v)                 { this.status = v; }

    public String        getObservacao()                     { return observacao; }
    public void          setObservacao(String v)             { this.observacao = v; }

    public LocalDateTime getDataAbertura()                   { return dataAbertura; }
    public void          setDataAbertura(LocalDateTime v)    { this.dataAbertura = v; }

    public boolean       isAtivo()                           { return ativo; }
    public void          setAtivo(boolean v)                 { this.ativo = v; }

    public List<ItemPedido> getItens()                       { return itens; }
    public void             setItens(List<ItemPedido> v)     { this.itens = v; }

    /** Peso de prioridade: mesa urgente=4, mesa=3, delivery urgente=2, delivery=1 */
    public int calcularPeso() {
        if ("mesa".equals(tipo)) return urgente ? 4 : 3;
        else                     return urgente ? 2 : 1;
    }

    /** Atalho: número da mesa ou "-" se delivery. */
    public String getNumeroMesa() {
        return mesa != null ? String.valueOf(mesa.getNumero()) : "-";
    }

    @Override
    public String toString() {
        return "Pedido [id=" + idPedido + ", tipo=" + tipo + ", status=" + status + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return idPedido == ((Pedido) o).idPedido;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(idPedido);
    }
}
