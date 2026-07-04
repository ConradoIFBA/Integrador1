package br.com.restaurante.model;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
public class HistoricoMesa implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private int idHistorico; private int mesaId; private String descricao;
    private LocalDateTime dataHora; private boolean ativo;
    public HistoricoMesa() {}
    public HistoricoMesa(int mesaId,String descricao){this.mesaId=mesaId;this.descricao=descricao;this.ativo=true;}
    public HistoricoMesa(int id,int mesaId,String descricao,LocalDateTime dataHora,boolean ativo){
        this.idHistorico=id;this.mesaId=mesaId;this.descricao=descricao;this.dataHora=dataHora;this.ativo=ativo;}
    public int getIdHistorico(){return idHistorico;} public void setIdHistorico(int v){idHistorico=v;}
    public int getMesaId(){return mesaId;} public void setMesaId(int v){mesaId=v;}
    public String getDescricao(){return descricao;} public void setDescricao(String v){descricao=v;}
    public LocalDateTime getDataHora(){return dataHora;} public void setDataHora(LocalDateTime v){dataHora=v;}
    public boolean isAtivo(){return ativo;} public void setAtivo(boolean v){ativo=v;}
    /** Usado nos JSPs: ${h.dataHoraFormatada} */
    public String getDataHoraFormatada(){return dataHora!=null?dataHora.format(FMT):"";}
    @Override public String toString(){return "HistoricoMesa[id="+idHistorico+",mesaId="+mesaId+"]";}
}
