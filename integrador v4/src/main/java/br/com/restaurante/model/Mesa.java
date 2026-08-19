package br.com.restaurante.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Tabela: mesa (v2)
 *
 * v2 — fundida com historico_mesa.
 * Os campos operador e dataStatus registram a última alteração de status,
 * eliminando a necessidade da tabela historico_mesa.
 *
 * status: 'livre' | 'ocupada' | 'reservada'
 */
public class Mesa implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter FMT =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private int           idMesa;
    private int           numero;
    private int           capacidade;
    private String        status;
    private String        operador;      // quem alterou o status por último
    private LocalDateTime dataStatus;   // quando o status foi alterado
    private boolean       ativo;
    // Chamada de garçom feita pelo cliente (fluxo self-service na
    // mesa) — fica true entre o clique do cliente em "Chamar Garçom"
    // e o funcionário confirmar o atendimento na tela de Mesas.
    private boolean       chamandoGarcom;
    private LocalDateTime dataChamado;

    public Mesa() {}

    public Mesa(int numero, int capacidade) {
        this.numero     = numero;
        this.capacidade = capacidade;
        this.status     = "livre";
        this.ativo      = true;
    }

    public Mesa(int idMesa, int numero, int capacidade, String status,
                String operador, LocalDateTime dataStatus, boolean ativo) {
        this.idMesa    = idMesa;
        this.numero    = numero;
        this.capacidade= capacidade;
        this.status    = status;
        this.operador  = operador;
        this.dataStatus= dataStatus;
        this.ativo     = ativo;
    }

    public int           getIdMesa()                    { return idMesa; }
    public void          setIdMesa(int v)               { this.idMesa = v; }

    public int           getNumero()                    { return numero; }
    public void          setNumero(int v)               { this.numero = v; }

    public int           getCapacidade()                { return capacidade; }
    public void          setCapacidade(int v)           { this.capacidade = v; }

    public String        getStatus()                    { return status; }
    public void          setStatus(String v)            { this.status = v; }

    public String        getOperador()                  { return operador; }
    public void          setOperador(String v)          { this.operador = v; }

    public LocalDateTime getDataStatus()                { return dataStatus; }
    public void          setDataStatus(LocalDateTime v) { this.dataStatus = v; }

    public boolean       isAtivo()                      { return ativo; }
    public void          setAtivo(boolean v)            { this.ativo = v; }

    public boolean       isChamandoGarcom()             { return chamandoGarcom; }
    public void          setChamandoGarcom(boolean v)   { this.chamandoGarcom = v; }

    public LocalDateTime getDataChamado()               { return dataChamado; }
    public void          setDataChamado(LocalDateTime v){ this.dataChamado = v; }

    public boolean isLivre() { return "livre".equals(status); }

    /** Usado nos JSPs: ${mesa.dataStatusFormatada} */
    public String getDataStatusFormatada() {
        return dataStatus != null ? dataStatus.format(FMT) : "";
    }

    /** Rótulo legível da última ação — ex: "Aberta por A1 em 03/06 14:30" */
    public String getUltimaAcao() {
        if (operador == null || operador.isBlank()) return "";
        String quando = getDataStatusFormatada();
        return switch (status) {
            case "ocupada"   -> "Aberta por "   + operador + (quando.isEmpty() ? "" : " em " + quando);
            case "reservada" -> "Reservada por " + operador + (quando.isEmpty() ? "" : " em " + quando);
            case "livre"     -> "Fechada por "  + operador + (quando.isEmpty() ? "" : " em " + quando);
            default          -> operador + " – " + quando;
        };
    }

    @Override
    public String toString() {
        return "Mesa[id=" + idMesa + ", numero=" + numero + ", status=" + status + "]";
    }
}