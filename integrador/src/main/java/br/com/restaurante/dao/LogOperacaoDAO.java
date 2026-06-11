package br.com.restaurante.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import br.com.restaurante.model.LogOperacao;

/**
 * DAO para a tabela log_operacao (append-only — somente INSERT e SELECT).
 *
 * Nunca haverá UPDATE nem DELETE nesta tabela.
 * Auditoria é imutável por definição.
 *
 * Métodos:
 *   registrar(LogOperacao)              → INSERT de um evento de auditoria
 *   listarPorOperador(String)           → histórico de um operador
 *   listarPorPerfil(String)             → filtrar por GERENTE/FUNCIONARIO/USUARIO
 *   listarRecentes(int)                 → últimos N registros
 */
public class LogOperacaoDAO {

    private final Connection conexao;

    public LogOperacaoDAO(Connection conexao) {
        this.conexao = conexao;
    }

    // ----------------------------------------------------------------
    // REGISTRAR — único método de escrita
    // ----------------------------------------------------------------

    /**
     * Grava um novo registro de auditoria.
     * data_hora é preenchida automaticamente pelo banco.
     */
    public void registrar(LogOperacao log) throws SQLException {
        String sql = "INSERT INTO log_operacao " +
                     "(perfil, funcao, identificador_operador, descricao) " +
                     "VALUES (?, ?, ?, ?)";

        try (PreparedStatement stmt = conexao.prepareStatement(sql,
                Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, log.getPerfil());
            stmt.setString(2, log.getFuncao()); // nullable
            stmt.setString(3, log.getIdentificadorOperador());
            stmt.setString(4, log.getDescricao());
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) log.setIdLog(rs.getInt(1));
            }
        }
    }

    // ----------------------------------------------------------------
    // LISTAR POR OPERADOR — ex: todos os eventos de "A1"
    // ----------------------------------------------------------------

    public List<LogOperacao> listarPorOperador(String identificador) throws SQLException {
        List<LogOperacao> lista = new ArrayList<>();
        String sql = "SELECT * FROM log_operacao " +
                     "WHERE identificador_operador = ? " +
                     "ORDER BY data_hora DESC";

        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setString(1, identificador);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        }
        return lista;
    }

    // ----------------------------------------------------------------
    // LISTAR POR PERFIL
    // ----------------------------------------------------------------

    public List<LogOperacao> listarPorPerfil(String perfil) throws SQLException {
        List<LogOperacao> lista = new ArrayList<>();
        String sql = "SELECT * FROM log_operacao WHERE perfil = ? ORDER BY data_hora DESC";

        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setString(1, perfil);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        }
        return lista;
    }

    // ----------------------------------------------------------------
    // LISTAR RECENTES — útil para tela de auditoria do gerente
    // ----------------------------------------------------------------

    public List<LogOperacao> listarRecentes(int limite) throws SQLException {
        List<LogOperacao> lista = new ArrayList<>();
        String sql = "SELECT * FROM log_operacao ORDER BY data_hora DESC LIMIT ?";

        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setInt(1, limite);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        }
        return lista;
    }

    // ----------------------------------------------------------------
    // MAPEAMENTO ResultSet → LogOperacao
    // ----------------------------------------------------------------

    private LogOperacao mapear(ResultSet rs) throws SQLException {
        LogOperacao log = new LogOperacao();
        log.setIdLog(rs.getInt("id_log"));
        log.setPerfil(rs.getString("perfil"));
        log.setFuncao(rs.getString("funcao")); // pode ser null
        log.setIdentificadorOperador(rs.getString("identificador_operador"));
        log.setDescricao(rs.getString("descricao"));
        log.setDataHora(rs.getTimestamp("data_hora").toLocalDateTime());
        return log;
    }
}
