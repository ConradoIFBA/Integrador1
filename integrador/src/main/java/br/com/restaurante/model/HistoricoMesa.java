package br.com.restaurante.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Tabela: historico_mesa
 * Linha do tempo automática de cada mesa (ex: "17h00 – mesa aberta por A1").
 */
public class HistoricoMesa implements Serializable {

    private static final long serialVersionUID = 1L;

    private int           idHistorico;
    private int           mesaId;
    private String        descricao;
    private LocalDateTime dataHora;
    private boolean       ativo;

    public HistoricoMesa() {}

    /** Construtor rápido para inserir um novo evento. dataHora fica a cargo do banco. */
    public HistoricoMesa(int mesaId, String descricao) {
        this.mesaId    = mesaId;
        this.descricao = descricao;
        this.ativo     = true;
    }

    public HistoricoMesa(int idHistorico, int mesaId, String descricao,
                         LocalDateTime dataHora, boolean ativo) {
        this.idHistorico = idHistorico;
        this.mesaId      = mesaId;
        this.descricao   = descricao;
        this.dataHora    = dataHora;
        this.ativo       = ativo;
    }

    public int           getIdHistorico()             { return idHistorico; }
    public void          setIdHistorico(int v)        { this.idHistorico = v; }

    public int           getMesaId()                  { return mesaId; }
    public void          setMesaId(int v)             { this.mesaId = v; }

    public String        getDescricao()               { return descricao; }
    public void          setDescricao(String v)       { this.descricao = v; }

    public LocalDateTime getDataHora()                { return dataHora; }
    public void          setDataHora(LocalDateTime v) { this.dataHora = v; }

    public boolean       isAtivo()                    { return ativo; }
    public void          setAtivo(boolean v)          { this.ativo = v; }

    @Override
    public String toString() {
        return "HistoricoMesa [id=" + idHistorico + ", mesaId=" + mesaId
               + ", dataHora=" + dataHora + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return idHistorico == ((HistoricoMesa) o).idHistorico;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(idHistorico);
    }
}
