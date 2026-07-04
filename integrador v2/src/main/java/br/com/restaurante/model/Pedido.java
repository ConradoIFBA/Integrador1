package br.com.restaurante.model;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
public class Pedido implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter FMT     = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FMT_HORA = DateTimeFormatter.ofPattern("HH:mm");
    private int idPedido; private Integer mesaId; private Mesa mesa;
    private String tipo; private boolean urgente; private String identificadorOperador;
    private String status; private String observacao; private LocalDateTime dataAbertura;
    private boolean ativo;
    private List<ItemPedido> itens = new ArrayList<>();
    public Pedido() {}
    public Pedido(Integer mesaId,String tipo,boolean urgente,String operador,String obs){
        this.mesaId=mesaId;this.tipo=tipo;this.urgente=urgente;
        this.identificadorOperador=operador;this.observacao=obs;this.status="aberto";this.ativo=true;}
    public Pedido(int id,Integer mesaId,String tipo,boolean urgente,String operador,
                  String status,String obs,LocalDateTime dataAbertura,boolean ativo){
        this.idPedido=id;this.mesaId=mesaId;this.tipo=tipo;this.urgente=urgente;
        this.identificadorOperador=operador;this.status=status;this.observacao=obs;
        this.dataAbertura=dataAbertura;this.ativo=ativo;}
    public int getIdPedido(){return idPedido;} public void setIdPedido(int v){idPedido=v;}
    public Integer getMesaId(){return mesaId;} public void setMesaId(Integer v){mesaId=v;}
    public Mesa getMesa(){return mesa;} public void setMesa(Mesa v){mesa=v;}
    public String getTipo(){return tipo;} public void setTipo(String v){tipo=v;}
    public boolean isUrgente(){return urgente;} public void setUrgente(boolean v){urgente=v;}
    public String getIdentificadorOperador(){return identificadorOperador;}
    public void setIdentificadorOperador(String v){identificadorOperador=v;}
    public String getStatus(){return status;} public void setStatus(String v){status=v;}
    public String getObservacao(){return observacao;} public void setObservacao(String v){observacao=v;}
    public LocalDateTime getDataAbertura(){return dataAbertura;}
    public void setDataAbertura(LocalDateTime v){dataAbertura=v;}
    public boolean isAtivo(){return ativo;} public void setAtivo(boolean v){ativo=v;}
    public List<ItemPedido> getItens(){return itens;} public void setItens(List<ItemPedido> v){itens=v;}
    /** Usado nos JSPs: ${p.dataAberturaFormatada} */
    public String getDataAberturaFormatada(){return dataAbertura!=null?dataAbertura.format(FMT):"";}
    public String getDataAberturaHora(){return dataAbertura!=null?dataAbertura.format(FMT_HORA):"";}
    public int calcularPeso(){
        if("mesa".equals(tipo)) return urgente?4:3;
        else return urgente?2:1;}
    public String getNumeroMesa(){return mesa!=null?String.valueOf(mesa.getNumero()):"-";}
    public BigDecimal calcularTotal(){
        return itens.stream().map(ItemPedido::getSubtotal)
               .reduce(BigDecimal.ZERO,BigDecimal::add);}
    @Override public String toString(){return "Pedido[id="+idPedido+",tipo="+tipo+",status="+status+"]";}
}
