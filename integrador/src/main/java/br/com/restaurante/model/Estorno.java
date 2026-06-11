package br.com.restaurante.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Tabela: estorno  (append-only — sem campo ativo, registros imutáveis)
 *
 * tipoEstorno    : 'total' | 'parcial'
 * formaPagamento : 'dinheiro' | 'cartao' | 'pix'
 */
public class Estorno implements Serializable {

    private static final long serialVersionUID = 1L;

    private int           idEstorno;
    private int           pedidoId;
    private BigDecimal    valorEstornado;
    private String        tipoEstorno;
    private String        formaPagamento;
    private String        motivo;
    private LocalDateTime dataHora;
    private String        identificadorOperador;

    public Estorno() {}

    public Estorno(int pedidoId, BigDecimal valorEstornado, String tipoEstorno,
                   String formaPagamento, String motivo, String identificadorOperador) {
        this.pedidoId              = pedidoId;
        this.valorEstornado        = valorEstornado;
        this.tipoEstorno           = tipoEstorno;
        this.formaPagamento        = formaPagamento;
        this.motivo                = motivo;
        this.identificadorOperador = identificadorOperador;
    }

    public Estorno(int idEstorno, int pedidoId, BigDecimal valorEstornado,
                   String tipoEstorno, String formaPagamento,
                   String motivo, LocalDateTime dataHora,
                   String identificadorOperador) {
        this.idEstorno             = idEstorno;
        this.pedidoId              = pedidoId;
        this.valorEstornado        = valorEstornado;
        this.tipoEstorno           = tipoEstorno;
        this.formaPagamento        = formaPagamento;
        this.motivo                = motivo;
        this.dataHora              = dataHora;
        this.identificadorOperador = identificadorOperador;
    }

    public int           getIdEstorno()                       { return idEstorno; }
    public void          setIdEstorno(int v)                  { this.idEstorno = v; }

    public int           getPedidoId()                        { return pedidoId; }
    public void          setPedidoId(int v)                   { this.pedidoId = v; }

    public BigDecimal    getValorEstornado()                  { return valorEstornado; }
    public void          setValorEstornado(BigDecimal v)      { this.valorEstornado = v; }

    public String        getTipoEstorno()                     { return tipoEstorno; }
    public void          setTipoEstorno(String v)             { this.tipoEstorno = v; }

    public String        getFormaPagamento()                  { return formaPagamento; }
    public void          setFormaPagamento(String v)          { this.formaPagamento = v; }

    public String        getMotivo()                          { return motivo; }
    public void          setMotivo(String v)                  { this.motivo = v; }

    public LocalDateTime getDataHora()                        { return dataHora; }
    public void          setDataHora(LocalDateTime v)         { this.dataHora = v; }

    public String        getIdentificadorOperador()           { return identificadorOperador; }
    public void          setIdentificadorOperador(String v)   { this.identificadorOperador = v; }

    @Override
    public String toString() {
        return "Estorno [id=" + idEstorno + ", pedidoId=" + pedidoId
               + ", valor=" + valorEstornado + ", tipo=" + tipoEstorno + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return idEstorno == ((Estorno) o).idEstorno;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(idEstorno);
    }
}
