package br.com.restaurante.model;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
public class Estorno implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private int idEstorno; private int pedidoId; private BigDecimal valorEstornado;
    private String tipoEstorno; private String formaPagamento; private String motivo;
    private LocalDateTime dataHora; private String identificadorOperador;
    public Estorno() {}
    public Estorno(int pedidoId,BigDecimal valor,String tipo,String forma,String motivo,String operador){
        this.pedidoId=pedidoId;this.valorEstornado=valor;this.tipoEstorno=tipo;
        this.formaPagamento=forma;this.motivo=motivo;this.identificadorOperador=operador;}
    public Estorno(int id,int pedidoId,BigDecimal valor,String tipo,String forma,
                   String motivo,LocalDateTime dataHora,String operador){
        this.idEstorno=id;this.pedidoId=pedidoId;this.valorEstornado=valor;this.tipoEstorno=tipo;
        this.formaPagamento=forma;this.motivo=motivo;this.dataHora=dataHora;this.identificadorOperador=operador;}
    public int getIdEstorno(){return idEstorno;} public void setIdEstorno(int v){idEstorno=v;}
    public int getPedidoId(){return pedidoId;} public void setPedidoId(int v){pedidoId=v;}
    public BigDecimal getValorEstornado(){return valorEstornado;} public void setValorEstornado(BigDecimal v){valorEstornado=v;}
    public String getTipoEstorno(){return tipoEstorno;} public void setTipoEstorno(String v){tipoEstorno=v;}
    public String getFormaPagamento(){return formaPagamento;} public void setFormaPagamento(String v){formaPagamento=v;}
    public String getMotivo(){return motivo;} public void setMotivo(String v){motivo=v;}
    public LocalDateTime getDataHora(){return dataHora;} public void setDataHora(LocalDateTime v){dataHora=v;}
    public String getIdentificadorOperador(){return identificadorOperador;} public void setIdentificadorOperador(String v){identificadorOperador=v;}
    public String getDataHoraFormatada(){return dataHora!=null?dataHora.format(FMT):"";}
    @Override public String toString(){return "Estorno[id="+idEstorno+",pedidoId="+pedidoId+"]";}
}
