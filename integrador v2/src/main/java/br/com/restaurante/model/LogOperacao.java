package br.com.restaurante.model;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
public class LogOperacao implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private int idLog; private String perfil; private String funcao;
    private String identificadorOperador; private String descricao; private LocalDateTime dataHora;
    public LogOperacao() {}
    public LogOperacao(String perfil,String funcao,String operador,String descricao){
        this.perfil=perfil;this.funcao=funcao;this.identificadorOperador=operador;this.descricao=descricao;}
    public LogOperacao(int id,String perfil,String funcao,String operador,String desc,LocalDateTime dataHora){
        this.idLog=id;this.perfil=perfil;this.funcao=funcao;this.identificadorOperador=operador;
        this.descricao=desc;this.dataHora=dataHora;}
    public int getIdLog(){return idLog;} public void setIdLog(int v){idLog=v;}
    public String getPerfil(){return perfil;} public void setPerfil(String v){perfil=v;}
    public String getFuncao(){return funcao;} public void setFuncao(String v){funcao=v;}
    public String getIdentificadorOperador(){return identificadorOperador;} public void setIdentificadorOperador(String v){identificadorOperador=v;}
    public String getDescricao(){return descricao;} public void setDescricao(String v){descricao=v;}
    public LocalDateTime getDataHora(){return dataHora;} public void setDataHora(LocalDateTime v){dataHora=v;}
    public String getDataHoraFormatada(){return dataHora!=null?dataHora.format(FMT):"";}
    @Override public String toString(){return "LogOperacao[id="+idLog+",operador="+identificadorOperador+"]";}
}
