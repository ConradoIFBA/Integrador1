package br.com.restaurante.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Tabela: log_operacao  (append-only — sem campo ativo, sem FK para usuario)
 *
 * perfil : 'GERENTE' | 'FUNCIONARIO' | 'USUARIO'
 * funcao : 'atendente' | 'cozinha' | null  (só para FUNCIONARIO)
 *
 * Exemplo de descricao: "A1 registrou pedido #5 para mesa 3"
 */
public class LogOperacao implements Serializable {

    private static final long serialVersionUID = 1L;

    private int           idLog;
    private String        perfil;
    private String        funcao;                // nullable
    private String        identificadorOperador;
    private String        descricao;
    private LocalDateTime dataHora;

    public LogOperacao() {}

    /** Construtor rápido para gravar um novo log antes de persistir. */
    public LogOperacao(String perfil, String funcao,
                       String identificadorOperador, String descricao) {
        this.perfil                = perfil;
        this.funcao                = funcao;
        this.identificadorOperador = identificadorOperador;
        this.descricao             = descricao;
    }

    public LogOperacao(int idLog, String perfil, String funcao,
                       String identificadorOperador, String descricao,
                       LocalDateTime dataHora) {
        this.idLog                 = idLog;
        this.perfil                = perfil;
        this.funcao                = funcao;
        this.identificadorOperador = identificadorOperador;
        this.descricao             = descricao;
        this.dataHora              = dataHora;
    }

    public int           getIdLog()                         { return idLog; }
    public void          setIdLog(int v)                    { this.idLog = v; }

    public String        getPerfil()                        { return perfil; }
    public void          setPerfil(String v)                { this.perfil = v; }

    public String        getFuncao()                        { return funcao; }
    public void          setFuncao(String v)                { this.funcao = v; }

    public String        getIdentificadorOperador()         { return identificadorOperador; }
    public void          setIdentificadorOperador(String v) { this.identificadorOperador = v; }

    public String        getDescricao()                     { return descricao; }
    public void          setDescricao(String v)             { this.descricao = v; }

    public LocalDateTime getDataHora()                      { return dataHora; }
    public void          setDataHora(LocalDateTime v)       { this.dataHora = v; }

    @Override
    public String toString() {
        return "LogOperacao [id=" + idLog + ", operador=" + identificadorOperador
               + ", descricao=" + descricao + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return idLog == ((LogOperacao) o).idLog;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(idLog);
    }
}
