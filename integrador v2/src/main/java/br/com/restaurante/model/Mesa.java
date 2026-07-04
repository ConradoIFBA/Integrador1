package br.com.restaurante.model;
import java.io.Serializable;
public class Mesa implements Serializable {
    private static final long serialVersionUID = 1L;
    private int idMesa; private int numero; private int capacidade; private String status; private boolean ativo;
    public Mesa() {}
    public Mesa(int numero,int capacidade){this.numero=numero;this.capacidade=capacidade;this.status="livre";this.ativo=true;}
    public Mesa(int id,int numero,int capacidade,String status,boolean ativo){
        this.idMesa=id;this.numero=numero;this.capacidade=capacidade;this.status=status;this.ativo=ativo;}
    public int getIdMesa(){return idMesa;} public void setIdMesa(int v){idMesa=v;}
    public int getNumero(){return numero;} public void setNumero(int v){numero=v;}
    public int getCapacidade(){return capacidade;} public void setCapacidade(int v){capacidade=v;}
    public String getStatus(){return status;} public void setStatus(String v){status=v;}
    public boolean isAtivo(){return ativo;} public void setAtivo(boolean v){ativo=v;}
    public boolean isLivre(){return "livre".equals(status);}
    @Override public String toString(){return "Mesa[id="+idMesa+",numero="+numero+",status="+status+"]";}
}
