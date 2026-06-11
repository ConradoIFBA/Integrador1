package br.com.restaurante.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Tabela: fila_preparo  (UNIQUE em pedido_id)
 *
 * setor         : 'cozinha' | 'bebida' | 'sobremesa'
 * pesoPrioridade: 4=mesa urgente | 3=mesa | 2=delivery urgente | 1=delivery
 */
public class FilaPreparo implements Serializable {

    private static final long serialVersionUID = 1L;

    private int           idFila;
    private int           pedidoId;
    private Pedido        pedido;                  // nullable — populado com JOIN
    private int           posicao;
    private int           pesoPrioridade;
    private int           tempoEstimadoMin;
    private String        setor;
    private LocalDateTime dataEntrada;
    private LocalDateTime dataInicioPreparo;       // nullable
    private LocalDateTime dataConclusao;           // nullable
    private String        identificadorOperador;   // nullable
    private boolean       ativo;

    public FilaPreparo() {}

    public FilaPreparo(int idFila, int pedidoId, int posicao, int pesoPrioridade,
                       int tempoEstimadoMin, String setor,
                       LocalDateTime dataEntrada, LocalDateTime dataInicioPreparo,
                       LocalDateTime dataConclusao, String identificadorOperador,
                       boolean ativo) {
        this.idFila                = idFila;
        this.pedidoId              = pedidoId;
        this.posicao               = posicao;
        this.pesoPrioridade        = pesoPrioridade;
        this.tempoEstimadoMin      = tempoEstimadoMin;
        this.setor                 = setor;
        this.dataEntrada           = dataEntrada;
        this.dataInicioPreparo     = dataInicioPreparo;
        this.dataConclusao         = dataConclusao;
        this.identificadorOperador = identificadorOperador;
        this.ativo                 = ativo;
    }

    public int           getIdFila()                           { return idFila; }
    public void          setIdFila(int v)                      { this.idFila = v; }

    public int           getPedidoId()                         { return pedidoId; }
    public void          setPedidoId(int v)                    { this.pedidoId = v; }

    public Pedido        getPedido()                           { return pedido; }
    public void          setPedido(Pedido v)                   { this.pedido = v; }

    public int           getPosicao()                          { return posicao; }
    public void          setPosicao(int v)                     { this.posicao = v; }

    public int           getPesoPrioridade()                   { return pesoPrioridade; }
    public void          setPesoPrioridade(int v)              { this.pesoPrioridade = v; }

    public int           getTempoEstimadoMin()                 { return tempoEstimadoMin; }
    public void          setTempoEstimadoMin(int v)            { this.tempoEstimadoMin = v; }

    public String        getSetor()                            { return setor; }
    public void          setSetor(String v)                    { this.setor = v; }

    public LocalDateTime getDataEntrada()                      { return dataEntrada; }
    public void          setDataEntrada(LocalDateTime v)       { this.dataEntrada = v; }

    public LocalDateTime getDataInicioPreparo()                { return dataInicioPreparo; }
    public void          setDataInicioPreparo(LocalDateTime v) { this.dataInicioPreparo = v; }

    public LocalDateTime getDataConclusao()                    { return dataConclusao; }
    public void          setDataConclusao(LocalDateTime v)     { this.dataConclusao = v; }

    public String        getIdentificadorOperador()            { return identificadorOperador; }
    public void          setIdentificadorOperador(String v)    { this.identificadorOperador = v; }

    public boolean       isAtivo()                             { return ativo; }
    public void          setAtivo(boolean v)                   { this.ativo = v; }

    /** true enquanto nenhum cozinheiro assumiu o pedido. */
    public boolean isAguardando() {
        return dataInicioPreparo == null;
    }

    @Override
    public String toString() {
        return "FilaPreparo [id=" + idFila + ", pedidoId=" + pedidoId
               + ", posicao=" + posicao + ", setor=" + setor + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return idFila == ((FilaPreparo) o).idFila;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(idFila);
    }
}
