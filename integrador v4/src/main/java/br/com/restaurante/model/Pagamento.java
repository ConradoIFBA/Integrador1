package br.com.restaurante.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Tabela: pagamento  (append-only — sem campo ativo)
 *
 * Substitui estorno na v2.
 * Registra COMO o pedido foi pago.
 * Um pedido pode ter múltiplos pagamentos (ex: metade PIX, metade dinheiro).
 *
 * forma_pagamento: 'dinheiro' | 'cartao' | 'pix'
 */
public class Pagamento implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter FMT =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private int           idPagamento;
    private int           pedidoId;
    private String        formaPagamento;
    private BigDecimal    valor;
    private String        observacao;
    private String        identificadorOperador;
    private LocalDateTime dataPagamento;

    public Pagamento() {}

    public Pagamento(int pedidoId, String formaPagamento, BigDecimal valor,
                     String observacao, String identificadorOperador) {
        this.pedidoId              = pedidoId;
        this.formaPagamento        = formaPagamento;
        this.valor                 = valor;
        this.observacao            = observacao;
        this.identificadorOperador = identificadorOperador;
    }

    public int           getIdPagamento()                        { return idPagamento; }
    public void          setIdPagamento(int v)                   { this.idPagamento = v; }

    public int           getPedidoId()                           { return pedidoId; }
    public void          setPedidoId(int v)                      { this.pedidoId = v; }

    public String        getFormaPagamento()                     { return formaPagamento; }
    public void          setFormaPagamento(String v)             { this.formaPagamento = v; }

    public BigDecimal    getValor()                              { return valor; }
    public void          setValor(BigDecimal v)                  { this.valor = v; }

    public String        getObservacao()                         { return observacao; }
    public void          setObservacao(String v)                 { this.observacao = v; }

    public String        getIdentificadorOperador()              { return identificadorOperador; }
    public void          setIdentificadorOperador(String v)      { this.identificadorOperador = v; }

    public LocalDateTime getDataPagamento()                      { return dataPagamento; }
    public void          setDataPagamento(LocalDateTime v)       { this.dataPagamento = v; }

    public String getDataPagamentoFormatada() {
        return dataPagamento != null ? dataPagamento.format(FMT) : "";
    }

    @Override
    public String toString() {
        return "Pagamento[id=" + idPagamento + ", pedidoId=" + pedidoId
               + ", forma=" + formaPagamento + ", valor=" + valor + "]";
    }
}