package br.com.restaurante.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import br.com.restaurante.model.FilaPreparo;
import br.com.restaurante.model.Mesa;
import br.com.restaurante.model.Pedido;

/**
 * DAO para a tabela fila_preparo.
 *
 * Regra de peso (gerada por Pedido.calcularPeso() antes do INSERT):
 *   4 = mesa urgente | 3 = mesa | 2 = delivery urgente | 1 = delivery
 *
 * Métodos:
 *   listarFila(String)               → fila ativa de um setor, ordenada por prioridade
 *   listarFilaGeral()                → todos os setores
 *   buscarPorPedido(int)             → por pedido_id (UNIQUE)
 *   buscarPorId(int)                 → por PK
 *   inserir(FilaPreparo)             → adiciona pedido à fila
 *   iniciarPreparo(int, String)      → seta data_inicio_preparo e operador
 *   concluir(int)                    → seta data_conclusao
 *   recalcularPosicoes(String)       → reordena posicao após inserção ou remoção
 *   desativar(int)                   → soft delete (remove da fila visível)
 */
public class FilaPreparoDAO {

    private final Connection conexao;

    public FilaPreparoDAO(Connection conexao) {
        this.conexao = conexao;
    }

    // ----------------------------------------------------------------
    // SQL BASE
    // ----------------------------------------------------------------

    private static final String SQL_BASE =
        "SELECT f.*, " +
        "       p.tipo, p.urgente, p.identificador_operador AS operador_pedido, " +
        "       p.status AS status_pedido, p.observacao AS obs_pedido, " +
        "       p.data_abertura, p.mesa_id, " +
        "       m.numero AS mesa_numero " +
        "FROM fila_preparo f " +
        "INNER JOIN pedido p ON f.pedido_id = p.id_pedido " +
        "LEFT  JOIN mesa   m ON p.mesa_id   = m.id_mesa ";

    // ----------------------------------------------------------------
    // LISTAR FILA DE UM SETOR — ordenada por peso DESC, entrada ASC
    // ----------------------------------------------------------------

    /**
     * Retorna a fila ativa de um setor, do mais prioritário para o menos.
     * setor: 'cozinha' | 'bebida' | 'sobremesa'
     */
    public List<FilaPreparo> listarFila(String setor) throws SQLException {
        List<FilaPreparo> lista = new ArrayList<>();
        String sql = SQL_BASE +
                     "WHERE f.ativo = 1 AND f.setor = ? AND f.data_conclusao IS NULL " +
                     "ORDER BY f.peso_prioridade DESC, f.data_entrada ASC";

        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setString(1, setor);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        }
        return lista;
    }

    // ----------------------------------------------------------------
    // LISTAR FILA GERAL — todos os setores
    // ----------------------------------------------------------------

    public List<FilaPreparo> listarFilaGeral() throws SQLException {
        List<FilaPreparo> lista = new ArrayList<>();
        String sql = SQL_BASE +
                     "WHERE f.ativo = 1 AND f.data_conclusao IS NULL " +
                     "ORDER BY f.peso_prioridade DESC, f.data_entrada ASC";

        try (PreparedStatement stmt = conexao.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    // ----------------------------------------------------------------
    // BUSCAR POR PEDIDO (UNIQUE)
    // ----------------------------------------------------------------

    public FilaPreparo buscarPorPedido(int pedidoId) throws SQLException {
        String sql = SQL_BASE + "WHERE f.pedido_id = ? AND f.ativo = 1";

        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setInt(1, pedidoId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapear(rs);
            }
        }
        return null;
    }

    // ----------------------------------------------------------------
    // BUSCAR POR ID
    // ----------------------------------------------------------------

    public FilaPreparo buscarPorId(int id) throws SQLException {
        String sql = SQL_BASE + "WHERE f.id_fila = ? AND f.ativo = 1";

        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapear(rs);
            }
        }
        return null;
    }

    // ----------------------------------------------------------------
    // INSERIR — posicao calculada pelo próximo número disponível
    // ----------------------------------------------------------------

    public void inserir(FilaPreparo f) throws SQLException {
        // Calcula a próxima posição dentro do setor
        int proximaPosicao = proximaPosicao(f.getSetor());
        f.setPosicao(proximaPosicao);

        String sql = "INSERT INTO fila_preparo " +
                     "(pedido_id, posicao, peso_prioridade, tempo_estimado_min, " +
                     " setor, ativo) " +
                     "VALUES (?, ?, ?, ?, ?, 1)";

        try (PreparedStatement stmt = conexao.prepareStatement(sql,
                Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, f.getPedidoId());
            stmt.setInt(2, f.getPosicao());
            stmt.setInt(3, f.getPesoPrioridade());
            stmt.setInt(4, f.getTempoEstimadoMin());
            stmt.setString(5, f.getSetor());
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) f.setIdFila(rs.getInt(1));
            }
        }
    }

    // ----------------------------------------------------------------
    // INICIAR PREPARO — cozinheiro assume o pedido
    // ----------------------------------------------------------------

    public void iniciarPreparo(int idFila, String identificadorOperador) throws SQLException {
        String sql = "UPDATE fila_preparo " +
                     "SET data_inicio_preparo = NOW(), identificador_operador = ? " +
                     "WHERE id_fila = ?";

        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setString(1, identificadorOperador);
            stmt.setInt(2, idFila);
            stmt.executeUpdate();
        }
    }

    // ----------------------------------------------------------------
    // CONCLUIR — preparo finalizado
    // ----------------------------------------------------------------

    public void concluir(int idFila) throws SQLException {
        String sql = "UPDATE fila_preparo SET data_conclusao = NOW() WHERE id_fila = ?";

        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setInt(1, idFila);
            stmt.executeUpdate();
        }
    }

    // ----------------------------------------------------------------
    // RECALCULAR POSIÇÕES — reordena após mudança de estado
    // Chame após inserir ou concluir para manter posicao consistente
    // ----------------------------------------------------------------

    public void recalcularPosicoes(String setor) throws SQLException {
        // Lê os IDs ativos na ordem de prioridade
        String sqlSelect = "SELECT id_fila FROM fila_preparo " +
                           "WHERE ativo = 1 AND setor = ? AND data_conclusao IS NULL " +
                           "ORDER BY peso_prioridade DESC, data_entrada ASC";

        String sqlUpdate = "UPDATE fila_preparo SET posicao = ? WHERE id_fila = ?";

        try (PreparedStatement sel = conexao.prepareStatement(sqlSelect);
             PreparedStatement upd = conexao.prepareStatement(sqlUpdate)) {

            sel.setString(1, setor);

            try (ResultSet rs = sel.executeQuery()) {
                int pos = 1;
                while (rs.next()) {
                    upd.setInt(1, pos++);
                    upd.setInt(2, rs.getInt("id_fila"));
                    upd.addBatch();
                }
            }
            upd.executeBatch();
        }
    }

    // ----------------------------------------------------------------
    // SOFT DELETE
    // ----------------------------------------------------------------

    public void desativar(int id) throws SQLException {
        String sql = "UPDATE fila_preparo SET ativo = 0 WHERE id_fila = ?";

        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    // ----------------------------------------------------------------
    // HELPER — próxima posição disponível para um setor
    // ----------------------------------------------------------------

    private int proximaPosicao(String setor) throws SQLException {
        String sql = "SELECT COALESCE(MAX(posicao), 0) + 1 AS prox " +
                     "FROM fila_preparo WHERE ativo = 1 AND setor = ?";

        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setString(1, setor);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt("prox") : 1;
            }
        }
    }

    // ----------------------------------------------------------------
    // MAPEAMENTO ResultSet → FilaPreparo (com Pedido e Mesa embutidos)
    // ----------------------------------------------------------------

    private FilaPreparo mapear(ResultSet rs) throws SQLException {
        // Mesa (pode ser null se delivery)
        Mesa mesa = null;
        int mesaId = rs.getInt("mesa_id");
        if (!rs.wasNull()) {
            mesa = new Mesa();
            mesa.setIdMesa(mesaId);
            mesa.setNumero(rs.getInt("mesa_numero"));
        }

        // Pedido resumido (apenas campos necessários na fila)
        Pedido pedido = new Pedido();
        pedido.setIdPedido(rs.getInt("pedido_id"));
        pedido.setTipo(rs.getString("tipo"));
        pedido.setUrgente(rs.getBoolean("urgente"));
        pedido.setIdentificadorOperador(rs.getString("operador_pedido"));
        pedido.setStatus(rs.getString("status_pedido"));
        pedido.setObservacao(rs.getString("obs_pedido"));
        pedido.setDataAbertura(rs.getTimestamp("data_abertura").toLocalDateTime());
        if (mesa != null) {
            pedido.setMesaId(mesaId);
            pedido.setMesa(mesa);
        }

        FilaPreparo f = new FilaPreparo();
        f.setIdFila(rs.getInt("id_fila"));
        f.setPedidoId(rs.getInt("pedido_id"));
        f.setPedido(pedido);
        f.setPosicao(rs.getInt("posicao"));
        f.setPesoPrioridade(rs.getInt("peso_prioridade"));
        f.setTempoEstimadoMin(rs.getInt("tempo_estimado_min"));
        f.setSetor(rs.getString("setor"));
        f.setDataEntrada(rs.getTimestamp("data_entrada").toLocalDateTime());

        Timestamp inicio = rs.getTimestamp("data_inicio_preparo");
        if (inicio != null) f.setDataInicioPreparo(inicio.toLocalDateTime());

        Timestamp conclusao = rs.getTimestamp("data_conclusao");
        if (conclusao != null) f.setDataConclusao(conclusao.toLocalDateTime());

        f.setIdentificadorOperador(rs.getString("identificador_operador"));
        f.setAtivo(rs.getBoolean("ativo"));
        return f;
    }
}
