package br.com.restaurante.model;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
public class FilaPreparo implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM HH:mm");
    private int idFila; private int pedidoId; private Pedido pedido;
    private int posicao; private int pesoPrioridade; private int tempoEstimadoMin;
    private String setor; private LocalDateTime dataEntrada;
    private LocalDateTime dataInicioPreparo; private LocalDateTime dataConclusao;
    private String identificadorOperador; private boolean ativo;
    public FilaPreparo() {}
    public FilaPreparo(int id,int pedidoId,int pos,int peso,int tempo,String setor,
                       LocalDateTime entrada,LocalDateTime inicio,LocalDateTime conclusao,
                       String operador,boolean ativo){
        this.idFila=id;this.pedidoId=pedidoId;this.posicao=pos;this.pesoPrioridade=peso;
        this.tempoEstimadoMin=tempo;this.setor=setor;this.dataEntrada=entrada;
        this.dataInicioPreparo=inicio;this.dataConclusao=conclusao;
        this.identificadorOperador=operador;this.ativo=ativo;}
    public int getIdFila(){return idFila;} public void setIdFila(int v){idFila=v;}
    public int getPedidoId(){return pedidoId;} public void setPedidoId(int v){pedidoId=v;}
    public Pedido getPedido(){return pedido;} public void setPedido(Pedido v){pedido=v;}
    public int getPosicao(){return posicao;} public void setPosicao(int v){posicao=v;}
    public int getPesoPrioridade(){return pesoPrioridade;} public void setPesoPrioridade(int v){pesoPrioridade=v;}
    public int getTempoEstimadoMin(){return tempoEstimadoMin;} public void setTempoEstimadoMin(int v){tempoEstimadoMin=v;}
    public String getSetor(){return setor;} public void setSetor(String v){setor=v;}
    public LocalDateTime getDataEntrada(){return dataEntrada;} public void setDataEntrada(LocalDateTime v){dataEntrada=v;}
    public LocalDateTime getDataInicioPreparo(){return dataInicioPreparo;} public void setDataInicioPreparo(LocalDateTime v){dataInicioPreparo=v;}
    public LocalDateTime getDataConclusao(){return dataConclusao;} public void setDataConclusao(LocalDateTime v){dataConclusao=v;}
    public String getIdentificadorOperador(){return identificadorOperador;} public void setIdentificadorOperador(String v){identificadorOperador=v;}
    public boolean isAtivo(){return ativo;} public void setAtivo(boolean v){ativo=v;}
    public boolean isAguardando(){return dataInicioPreparo==null;}
    public String getDataEntradaFormatada(){return dataEntrada!=null?dataEntrada.format(FMT):"";}
    @Override public String toString(){return "FilaPreparo[id="+idFila+",pedidoId="+pedidoId+",pos="+posicao+"]";}
}
