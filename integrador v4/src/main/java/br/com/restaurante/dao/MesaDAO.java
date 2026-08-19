package br.com.restaurante.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import br.com.restaurante.model.Mesa;

/**
 * ================================================================
 * MESA DAO - Acesso à tabela "mesa" (schema v3)
 * ================================================================
 *
 * PROPÓSITO:
 * Centraliza o SQL da tabela "mesa" — inclusive os dois campos que
 * fundiram a extinta tabela "historico_mesa" diretamente na própria
 * mesa (operador e data_status). Usado por MesaController (CRUD
 * completo), ClienteController (listar/reservar) e PedidoController
 * (listar mesas livres ao criar um pedido).
 *
 * ⚠️ HISTÓRICO DA FUSÃO COM historico_mesa (v1 → v2):
 * Antes existia uma tabela separada "historico_mesa" que registrava
 * cada mudança de status como uma linha própria. A partir do v2, essa
 * tabela foi REMOVIDA e substituída por dois campos direto na tabela
 * mesa: "operador" (quem fez a ÚLTIMA alteração) e "data_status"
 * (QUANDO foi feita). Isso simplifica o modelo, ao custo de não ter
 * mais o histórico COMPLETO de todas as mudanças — só a mais recente
 * fica registrada. O método atualizarStatus() abaixo é o responsável
 * por manter esses dois campos sempre sincronizados a cada mudança.
 *
 * Nenhuma coluna desta tabela foi renomeada entre v2 e v3 — apenas o
 * TAMANHO de "operador" acompanhou o VARCHAR(100) padronizado em
 * outras colunas de operador do sistema (mesa.operador já nasceu
 * VARCHAR(100) desde o v2, diferente de identificador_operador em
 * pedido/fila_preparo/pagamento, que só foi ampliado no v3).
 *
 * TABELA: mesa
 * Schema (ver integrador_v3.sql):
 * - id_mesa       (PK, AUTO_INCREMENT)
 * - numero         (UNIQUE — número visível da mesa no salão)
 * - capacidade
 * - status          (ENUM: livre, ocupada, reservada)
 * - operador        (VARCHAR(100), NULLABLE — último operador que mudou o status)
 * - data_status     (DATETIME, NULLABLE — quando foi a última mudança)
 * - ativo           (TINYINT(1), default 1 — soft delete)
 *
 * MÉTODOS DISPONÍVEIS:
 * - listar()                              → todas as mesas ativas
 * - listarLivres()                         → só as mesas com status='livre'
 * - buscarPorId(id)                        → 1 mesa específica
 * - inserir(mesa)                          → cria (sempre status='livre')
 * - atualizarStatus(id, status, operador)  → muda status + registra quem/quando
 * - atualizarStatus(id, status)            → sobrecarga sem operador (uso interno)
 * - editar(mesa)                           → atualiza capacidade/status
 * - desativar(id)                          → soft delete
 *
 * @author Sistema Integrador
 * @version 3.0
 * @see Mesa
 */
public class MesaDAO {

    // ---- Conexão injetada via construtor (padrão usado em todo o projeto) ----
    private final Connection conexao;

    /**
     * Construtor — recebe a Connection já aberta (gerenciada pelo
     * controller via try-with-resources).
     */
    public MesaDAO(Connection conexao) {
        this.conexao = conexao;
    }

    // ── LISTAR ──────────────────────────────────────────────────────

    /* ================================================================
       LISTAR TODAS AS MESAS ATIVAS
       ================================================================

       Traz todas as mesas com ativo=1, independente do status (livre,
       ocupada ou reservada) — usado nas telas de visão geral do
       salão (MesaController.listarMesas(), ClienteController.
       exibirReserva()), onde o próprio front-end distingue
       visualmente cada status.

       Ordenado por número — ordem natural de exibição no salão.
    */
    public List<Mesa> listar() throws SQLException {
        List<Mesa> lista = new ArrayList<>();
        String sql = "SELECT * FROM mesa WHERE ativo = 1 ORDER BY numero";
        try (PreparedStatement s = conexao.prepareStatement(sql);
             ResultSet rs = s.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    // ── LISTAR LIVRES ───────────────────────────────────────────────

    /* ================================================================
       LISTAR APENAS MESAS LIVRES
       ================================================================

       Filtro adicional status='livre' — usado por PedidoController.
       exibirFormulario() para montar a lista de mesas disponíveis
       ao criar um novo pedido do tipo "mesa" (não faz sentido lançar
       um pedido novo numa mesa já ocupada por outro atendimento).
    */
    public List<Mesa> listarLivres() throws SQLException {
        List<Mesa> lista = new ArrayList<>();
        String sql = "SELECT * FROM mesa WHERE ativo = 1 AND status = 'livre' ORDER BY numero";
        try (PreparedStatement s = conexao.prepareStatement(sql);
             ResultSet rs = s.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    // ── BUSCAR POR ID ───────────────────────────────────────────────

    /* ================================================================
       BUSCAR MESA POR ID
       ================================================================

       Busca uma única mesa ativa pelo id_mesa. Usado sempre que um
       controller precisa validar o estado atual de uma mesa antes de
       operar sobre ela (ex: confirmar reserva só se estiver livre —
       ver ClienteController.confirmarReserva()).
    */
    public Mesa buscarPorId(int id) throws SQLException {
        String sql = "SELECT * FROM mesa WHERE id_mesa = ? AND ativo = 1";
        try (PreparedStatement s = conexao.prepareStatement(sql)) {
            s.setInt(1, id);
            try (ResultSet rs = s.executeQuery()) {
                if (rs.next()) return mapear(rs);
            }
        }
        return null;
    }

    // ── INSERIR ─────────────────────────────────────────────────────

    /* ================================================================
       INSERIR NOVA MESA
       ================================================================

       Cria a mesa sempre com status='livre' e ativo=1 (fixos no SQL)
       — toda mesa nasce disponível para uso. operador e data_status
       ficam NULL até a primeira mudança de status via
       atualizarStatus().

       Usa Statement.RETURN_GENERATED_KEYS para recuperar o id_mesa
       gerado e devolvê-lo no objeto recebido.
    */
    public void inserir(Mesa m) throws SQLException {
        String sql = "INSERT INTO mesa (numero, capacidade, status, ativo) VALUES (?, ?, 'livre', 1)";
        try (PreparedStatement s = conexao.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            s.setInt(1, m.getNumero());
            s.setInt(2, m.getCapacidade());
            s.executeUpdate();
            try (ResultSet rs = s.getGeneratedKeys()) {
                if (rs.next()) m.setIdMesa(rs.getInt(1));
            }
        }
    }

    // ── ATUALIZAR STATUS v2 — persiste operador e data_status ───────

    /**
     * ================================================================
     * ATUALIZAR STATUS DA MESA (com registro de operador/data)
     * ================================================================
     *
     * Atualiza status, operador e data_status=NOW() em uma única
     * instrução — é este método que "substitui" a extinta tabela
     * historico_mesa: em vez de inserir uma nova linha de histórico,
     * sobrescreve os campos de auditoria diretamente na própria mesa,
     * mantendo sempre só a informação da ÚLTIMA alteração.
     *
     * Chamado por MesaController.mudarStatus() (abrir/fechar/reservar,
     * tanto por funcionário quanto por cliente) — o parâmetro
     * "operador" pode ser o login de um funcionário ou o nome de um
     * cliente, dependendo de quem disparou a ação.
     */
    public void atualizarStatus(int idMesa, String status, String operador) throws SQLException {
        String sql = "UPDATE mesa SET status = ?, operador = ?, data_status = NOW() " +
                     "WHERE id_mesa = ?";
        try (PreparedStatement s = conexao.prepareStatement(sql)) {
            s.setString(1, status);
            s.setString(2, operador);
            s.setInt(3, idMesa);
            s.executeUpdate();
        }
    }

    /**
     * ================================================================
     * SOBRECARGA: ATUALIZAR STATUS SEM OPERADOR
     * ================================================================
     *
     * Atalho para cenários internos onde não faz sentido/não é
     * necessário registrar quem fez a alteração — delega para a
     * versão completa passando operador=null (o próprio SQL aceita
     * NULL na coluna "operador", que é NULLABLE).
     *
     * Nenhum controller revisado até aqui usa esta sobrecarga
     * diretamente (todos passam um operador explícito ou o nome do
     * usuário logado como fallback), mas ela fica disponível para
     * casos futuros de atualização "sem rastro de operador".
     */
    public void atualizarStatus(int idMesa, String status) throws SQLException {
        atualizarStatus(idMesa, status, null);
    }

    /* ================================================================
       CHAMAR GARÇOM (acionado pelo cliente, tela cliente/mesa.jsp)
       ================================================================

       Marca a mesa como "chamando garçom" — não mexe em status/
       operador da mesa (aquilo é sobre OCUPAÇÃO da mesa; isto é só
       um sinalizador temporário de "preciso de atenção"). Fica ativo
       até um funcionário confirmar via atenderChamado().
    */
    public void chamarGarcom(int idMesa) throws SQLException {
        try (PreparedStatement s = conexao.prepareStatement(
                "UPDATE mesa SET chamando_garcom = 1, data_chamado = NOW() WHERE id_mesa = ?")) {
            s.setInt(1, idMesa);
            s.executeUpdate();
        }
    }

    /* ================================================================
       ATENDER CHAMADO (acionado pelo funcionário, tela mesas.jsp)
       ================================================================

       Limpa o sinalizador — não apaga data_chamado (fica como
       registro de quando foi a última chamada, mesmo já atendida;
       só chamando_garcom volta a 0 para sumir o alerta visual).
    */
    public void atenderChamado(int idMesa) throws SQLException {
        try (PreparedStatement s = conexao.prepareStatement(
                "UPDATE mesa SET chamando_garcom = 0 WHERE id_mesa = ?")) {
            s.setInt(1, idMesa);
            s.executeUpdate();
        }
    }

    // ── EDITAR ──────────────────────────────────────────────────────

    /* ================================================================
       EDITAR MESA (CRUD administrativo — número e capacidade)
       ================================================================

       Atualiza NÚMERO e CAPACIDADE — nunca o status por aqui. Editar
       status é uma operação DIFERENTE, com semântica própria (abrir/
       fechar/reservar via atualizarStatus(), que também registra
       operador/data_status para auditoria) — misturar os dois num
       único método confundiria "reconfigurar a mesa" (isto aqui, uso
       raro, feito pelo Gerente na tela de administração) com
       "mudar o atendimento da mesa" (uso constante, no dia a dia do
       salão). Se um dia editar() também aceitasse status, um Gerente
       poderia acidentalmente "abrir" uma mesa sem passar pelo fluxo
       de auditoria normal.

       ⚠️ numero tem UNIQUE no schema — um número duplicado gera
       SQLIntegrityConstraintViolationException, que o Controller
       precisa tratar e traduzir numa mensagem amigável (ver
       MesaController.salvar()).
    */
    public void editar(Mesa m) throws SQLException {
        String sql = "UPDATE mesa SET numero = ?, capacidade = ? WHERE id_mesa = ?";
        try (PreparedStatement s = conexao.prepareStatement(sql)) {
            s.setInt(1, m.getNumero());
            s.setInt(2, m.getCapacidade());
            s.setInt(3, m.getIdMesa());
            s.executeUpdate();
        }
    }

    // ── SOFT DELETE ─────────────────────────────────────────────────

    /* ================================================================
       DESATIVAR MESA (SOFT DELETE)
       ================================================================

       Marca ativo=0 — nunca DELETE físico, preservando o vínculo
       histórico com pedidos antigos que referenciam esta mesa via
       mesa_id (uma FK apontando para uma linha deletada quebraria o
       histórico de pedidos).
    */
    public void desativar(int id) throws SQLException {
        String sql = "UPDATE mesa SET ativo = 0 WHERE id_mesa = ?";
        try (PreparedStatement s = conexao.prepareStatement(sql)) {
            s.setInt(1, id);
            s.executeUpdate();
        }
    }

    // ── MAPEAMENTO ──────────────────────────────────────────────────

    /* ================================================================
       HELPER: MAPEAR RESULTSET → OBJETO Mesa
       ================================================================

       Conversão direta, sem JOINs — mas com atenção especial aos
       dois campos herdados da fusão com historico_mesa, que são
       NULLABLE (uma mesa recém-criada ainda não tem operador nem
       data_status até sua primeira mudança de status):

       - operador: lido diretamente com getString() — já retorna null
         naturalmente se a coluna for NULL (String é um tipo de
         referência, não precisa de tratamento especial como int)
       - data_status: lido como Timestamp primeiro, com checagem
         explícita "if (ts != null)" antes de converter para
         LocalDateTime — mesmo padrão de nullable usado em
         PedidoDAO/FilaPreparoDAO para colunas DATETIME opcionais
    */
    private Mesa mapear(ResultSet rs) throws SQLException {
        Mesa m = new Mesa();
        m.setIdMesa(rs.getInt("id_mesa"));
        m.setNumero(rs.getInt("numero"));
        m.setCapacidade(rs.getInt("capacidade"));
        m.setStatus(rs.getString("status"));
        m.setOperador(rs.getString("operador"));               // nullable
        Timestamp ts = rs.getTimestamp("data_status");
        if (ts != null) m.setDataStatus(ts.toLocalDateTime()); // nullable
        m.setAtivo(rs.getBoolean("ativo"));
        m.setChamandoGarcom(rs.getBoolean("chamando_garcom"));
        Timestamp tsChamado = rs.getTimestamp("data_chamado");
        if (tsChamado != null) m.setDataChamado(tsChamado.toLocalDateTime()); // nullable
        return m;
    }
}

/* ================================================================
   RESUMO DO DAO
   ================================================================

   TABELA: mesa (nenhuma coluna renomeada entre v2 e v3)

   MÉTODOS:
   1. listar()                             → todas as mesas ativas
   2. listarLivres()                        → só status='livre'
   3. buscarPorId(id)                       → 1 mesa específica
   4. inserir(mesa)                         → cria (sempre 'livre')
   5. atualizarStatus(id,status,operador)   → muda status + auditoria inline
   6. atualizarStatus(id,status)            → sobrecarga sem operador
   7. editar(mesa)                          → atualiza capacidade/status (sem auditoria)
   8. desativar(id)                         → soft delete

   HERANÇA DA FUSÃO COM historico_mesa (v1 → v2):
   ✅ Colunas "operador" e "data_status" vivem na própria tabela mesa
   ✅ atualizarStatus(id,status,operador) é o único método que
      mantém esses dois campos sincronizados — sempre usar este
      método (não editar()) quando a mudança de status precisar de
      rastreabilidade de quem/quando

   AJUSTES DO SCHEMA V3:
   ✅ Nenhum — esta tabela não teve colunas renomeadas nem
      redimensionadas entre v2 e v3

   DEPENDÊNCIAS:
   - Mesa: model
   - Usado por MesaController, ClienteController e PedidoController

   OBSERVAÇÕES GERAIS:
   - Todas as queries usam PreparedStatement (proteção contra SQL
     injection)
   - Conexão é injetada via construtor e gerenciada pelo chamador
   - Tratamento cuidadoso de campos NULLABLE (operador, data_status)
     no método mapear()
   ================================================================ */
