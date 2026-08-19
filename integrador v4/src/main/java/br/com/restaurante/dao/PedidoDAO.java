package br.com.restaurante.dao;

import java.sql.*;
import java.util.*;
import br.com.restaurante.model.Mesa;
import br.com.restaurante.model.Pedido;

/**
 * ================================================================
 * PEDIDO DAO - Acesso à tabela "pedido" (schema v3)
 * ================================================================
 *
 * PROPÓSITO:
 * Centraliza todo o SQL de leitura e escrita da tabela "pedido" —
 * o núcleo do sistema, usado por praticamente todos os controllers
 * (PedidoController, ClienteController, MesaController, FilaController,
 * DashboardController e RelatorioController, este último com queries
 * próprias fora deste DAO).
 *
 * ⚠️ NOTA SOBRE O SCHEMA V3:
 * A coluna identificador_operador foi ampliada de VARCHAR(20) para
 * VARCHAR(100) no schema v3 (ver integrador_v3.sql), para suportar
 * nomes de clientes mais longos (o campo é usado tanto para o
 * login/nome de funcionários quanto para o NOME COMPLETO do cliente
 * em pedidos de delivery). Este DAO não precisou de nenhuma alteração
 * de código por causa disso — o Java já trabalha com String sem
 * nenhum truncamento manual, então o aumento do limite no banco é
 * transparente aqui. Fica só o registro para referência.
 *
 * A tabela "pedido" EM SI não foi renomeada e nenhuma de suas colunas
 * lidas/escritas por este DAO mudou de nome entre v2 e v3 (a mudança
 * de "item_cardapio" → "cardapio" afeta item_pedido, não pedido).
 *
 * TABELA: pedido
 * Schema (ver integrador_v3.sql):
 * - id_pedido              (PK, AUTO_INCREMENT)
 * - mesa_id                 (FK → mesa.id_mesa, NULLABLE — delivery não tem mesa)
 * - tipo                     (ENUM: mesa, delivery)
 * - urgente                  (TINYINT(1) — usado no cálculo de prioridade na fila)
 * - identificador_operador   (VARCHAR(100) — quem abriu o pedido: login do
 *                             funcionário ou nome do cliente)
 * - status                   (ENUM: aberto, em_preparo, pronto, entregue, cancelado)
 * - observacao
 * - data_abertura            (DATETIME, default CURRENT_TIMESTAMP)
 * - ativo                    (TINYINT(1), default 1 — soft delete)
 *
 * MÉTODOS DISPONÍVEIS:
 * - listarAbertos()               → pedidos não finalizados, urgentes primeiro
 * - listarPorMesa(mesaId)         → pedidos abertos de uma mesa específica
 * - listarPorStatus(status)       → pedidos filtrados por status exato
 * - listarPorOperador(operador)   → "Meus Pedidos" do cliente (por nome)
 * - buscarPorId(id)               → 1 pedido específico
 * - inserir(pedido)               → cria um novo pedido (status inicial 'aberto')
 * - atualizarStatus(id, status)   → avança/altera o status
 * - editar(pedido)                → atualiza urgente/observacao
 * - desativar(id)                 → soft delete + status='cancelado'
 *
 * PADRÃO SQL_BASE:
 * Todas as consultas de leitura reaproveitam SQL_BASE, que já faz um
 * LEFT JOIN com mesa — assim, mesmo pedidos de delivery (sem mesa)
 * são retornados corretamente (colunas de mesa vêm NULL), e pedidos
 * de mesa já trazem número/capacidade/status da mesa junto, sem
 * precisar de uma segunda consulta.
 *
 * @author Sistema Integrador
 * @version 3.0
 * @see Pedido
 * @see Mesa
 */
public class PedidoDAO {

    // ---- Conexão injetada via construtor (padrão usado em todo o projeto) ----
    private final Connection conexao;

    /**
     * Query base reaproveitada por todos os métodos de LEITURA.
     * LEFT JOIN (não INNER JOIN) é essencial aqui: pedidos de
     * delivery têm mesa_id = NULL, e um INNER JOIN os excluiria
     * completamente dos resultados.
     */
    private static final String SQL_BASE =
        "SELECT p.*, m.numero AS mesa_numero, m.capacidade, m.status AS mesa_status " +
        "FROM pedido p LEFT JOIN mesa m ON p.mesa_id = m.id_mesa ";

    /**
     * Construtor — recebe a Connection já aberta (gerenciada pelo
     * controller via try-with-resources, geralmente dentro de uma
     * transação maior junto com ItemPedidoDAO/FilaPreparoDAO/PagamentoDAO).
     */
    public PedidoDAO(Connection c) { this.conexao = c; }

    // ── LISTAR ABERTOS ──────────────────────────────────────────────

    /* ================================================================
       LISTAR PEDIDOS ABERTOS (não finalizados)
       ================================================================

       Traz todos os pedidos ativos cujo status NÃO seja 'entregue'
       nem 'cancelado' — ou seja, tudo que ainda está em andamento
       (aberto, em_preparo, pronto).

       ORDENAÇÃO IMPORTANTE: pedidos urgentes primeiro (p.urgente DESC),
       e dentro de cada grupo (urgente/não urgente), o mais antigo
       primeiro (data_abertura ASC) — isso reflete a ordem real de
       atendimento esperada: urgências furam a fila, e entre pedidos
       de mesma prioridade, quem chegou primeiro é atendido primeiro.

       Usado por: PedidoController.listar(), DashboardController
       (para contar pedidos abertos e montar "últimos pedidos").
    */
    public List<Pedido> listarAbertos() throws SQLException {
        List<Pedido> l = new ArrayList<>();
        try (PreparedStatement s = conexao.prepareStatement(SQL_BASE +
                "WHERE p.ativo=1 AND p.status NOT IN('entregue','cancelado') " +
                "ORDER BY p.urgente DESC, p.data_abertura ASC");
             ResultSet r = s.executeQuery()) {
            while (r.next()) l.add(mapear(r));
        }
        return l;
    }

    // ── LISTAR POR MESA ─────────────────────────────────────────────

    /* ================================================================
       LISTAR PEDIDOS ABERTOS DE UMA MESA
       ================================================================

       Filtra por mesa_id E pelos mesmos critérios de "aberto" que
       listarAbertos() (ativo=1, status fora de entregue/cancelado).

       Usado por MesaController.exibirDetalhe() — mostra ao
       funcionário todos os pedidos ainda em andamento naquela mesa
       específica.

       Ordenado do mais recente para o mais antigo (data_abertura DESC)
       — diferente de listarAbertos(), aqui não há distinção por
       urgência porque o contexto já é uma única mesa.
    */
    public List<Pedido> listarPorMesa(int mesaId) throws SQLException {
        List<Pedido> l = new ArrayList<>();
        try (PreparedStatement s = conexao.prepareStatement(SQL_BASE +
                "WHERE p.mesa_id=? AND p.ativo=1 " +
                "AND p.status NOT IN('entregue','cancelado') " +
                "ORDER BY p.data_abertura DESC")) {
            s.setInt(1, mesaId);
            try (ResultSet r = s.executeQuery()) { while (r.next()) l.add(mapear(r)); }
        }
        return l;
    }

    // ── LISTAR POR STATUS ───────────────────────────────────────────

    /* ================================================================
       LISTAR PEDIDOS POR STATUS EXATO
       ================================================================

       Diferente de listarAbertos() (que exclui entregue/cancelado),
       este método filtra por UM status específico — útil, por
       exemplo, para relatórios ou telas que precisam ver só os
       "prontos" aguardando entrega, ou só os "cancelados" de um
       período.

       Mesma ordenação de prioridade de listarAbertos() (urgente
       primeiro, depois mais antigo primeiro).
    */
    public List<Pedido> listarPorStatus(String status) throws SQLException {
        List<Pedido> l = new ArrayList<>();
        try (PreparedStatement s = conexao.prepareStatement(SQL_BASE +
                "WHERE p.status=? AND p.ativo=1 " +
                "ORDER BY p.urgente DESC, p.data_abertura ASC")) {
            s.setString(1, status);
            try (ResultSet r = s.executeQuery()) { while (r.next()) l.add(mapear(r)); }
        }
        return l;
    }

    // ── LISTAR POR OPERADOR — usado pelo cliente para "Meus Pedidos" ─

    /* ================================================================
       LISTAR PEDIDOS POR OPERADOR (identificador_operador)
       ================================================================

       Filtra pedidos pelo identificador_operador exato — usado
       principalmente por ClienteController.exibirMeusPedidos(),
       passando o NOME do cliente logado (usuario.getNome()) como
       parâmetro, já que não existe uma tabela de "cliente" separada
       no sistema (ver observação em ClienteController).

       ⚠️ Como identificador_operador agora é VARCHAR(100) no schema
       v3 (antes VARCHAR(20)), nomes completos de clientes cabem sem
       truncamento — este método já funcionava corretamente com
       Strings Java de qualquer tamanho; a limitação anterior estava
       só na coluna do banco, nunca neste código.

       Ordenado do pedido mais recente para o mais antigo.
    */
    public List<Pedido> listarPorOperador(String operador) throws SQLException {
        List<Pedido> l = new ArrayList<>();
        try (PreparedStatement s = conexao.prepareStatement(SQL_BASE +
                "WHERE p.identificador_operador=? AND p.ativo=1 " +
                "ORDER BY p.data_abertura DESC")) {
            s.setString(1, operador);
            try (ResultSet r = s.executeQuery()) { while (r.next()) l.add(mapear(r)); }
        }
        return l;
    }

    // ── BUSCAR POR ID ───────────────────────────────────────────────

    /* ================================================================
       BUSCAR PEDIDO POR ID
       ================================================================

       Busca um único pedido ativo pelo id_pedido. Usado sempre que
       um controller precisa carregar um pedido específico antes de
       operar sobre ele (ex: avançar status, exibir detalhe).
    */
    public Pedido buscarPorId(int id) throws SQLException {
        try (PreparedStatement s = conexao.prepareStatement(
                SQL_BASE + "WHERE p.id_pedido=? AND p.ativo=1")) {
            s.setInt(1, id);
            try (ResultSet r = s.executeQuery()) {
                if (r.next()) return mapear(r);
            }
        }
        return null;
    }

    // ── INSERIR ─────────────────────────────────────────────────────

    /* ================================================================
       INSERIR NOVO PEDIDO
       ================================================================

       Cria um pedido sempre com status='aberto' e ativo=1 (valores
       fixos no SQL). Todo pedido nasce "aberto" — o avanço para
       em_preparo/pronto/entregue é sempre feito depois, via
       atualizarStatus().

       TRATAMENTO DE mesa_id NULLABLE:
       Pedidos de delivery não têm mesa vinculada. Se p.getMesaId()
       retornar null (Integer, não int — permite null), o código
       grava explicitamente Types.INTEGER como NULL no banco via
       s.setNull(). Isso é necessário porque não dá para simplesmente
       chamar s.setInt() com um valor nulo — teria NullPointerException
       no unboxing do Integer para int.

       Usa Statement.RETURN_GENERATED_KEYS para recuperar o id_pedido
       gerado e devolvê-lo no próprio objeto Pedido — essencial, pois
       o chamador (ex: PedidoController.criar()) precisa desse id
       imediatamente para inserir os itens do pedido em seguida, na
       mesma transação.
    */
    public void inserir(Pedido p) throws SQLException {
        try (PreparedStatement s = conexao.prepareStatement(
                "INSERT INTO pedido(mesa_id,tipo,urgente,identificador_operador," +
                "status,observacao,ativo) VALUES(?,?,?,?,'aberto',?,1)",
                Statement.RETURN_GENERATED_KEYS)) {

            // ---- mesa_id é NULLABLE: trata explicitamente o caso null ----
            if (p.getMesaId() != null) s.setInt(1, p.getMesaId());
            else s.setNull(1, Types.INTEGER);

            s.setString(2, p.getTipo());
            s.setBoolean(3, p.isUrgente());
            s.setString(4, p.getIdentificadorOperador());
            s.setString(5, p.getObservacao());
            s.executeUpdate();

            // ---- Recupera o id_pedido gerado e devolve no objeto ----
            try (ResultSet r = s.getGeneratedKeys()) {
                if (r.next()) p.setIdPedido(r.getInt(1));
            }
        }
    }

    // ── ATUALIZAR STATUS ────────────────────────────────────────────

    /* ================================================================
       ATUALIZAR STATUS DO PEDIDO
       ================================================================

       Atualiza APENAS a coluna status — usado repetidamente ao longo
       do ciclo de vida do pedido (aberto → em_preparo → pronto →
       entregue), sempre chamado por PedidoController.avancarStatus()
       ou FilaController (iniciarPreparo/concluirPreparo), nunca
       diretamente pela UI sem passar por essas regras de negócio.
    */
    public void atualizarStatus(int id, String status) throws SQLException {
        try (PreparedStatement s = conexao.prepareStatement(
                "UPDATE pedido SET status=? WHERE id_pedido=?")) {
            s.setString(1, status); s.setInt(2, id); s.executeUpdate();
        }
    }

    // ── EDITAR ──────────────────────────────────────────────────────

    /* ================================================================
       EDITAR PEDIDO
       ================================================================

       Atualiza APENAS urgente e observacao — campos "editáveis" de um
       pedido já criado. Note que tipo, mesa_id e identificador_operador
       NÃO são editáveis por este método (são definidos na criação e
       não deveriam mudar depois — trocar a mesa de um pedido já aberto,
       por exemplo, exigiria uma lógica de negócio própria, não uma
       edição genérica).
    */
    public void editar(Pedido p) throws SQLException {
        try (PreparedStatement s = conexao.prepareStatement(
                "UPDATE pedido SET urgente=?, observacao=? WHERE id_pedido=?")) {
            s.setBoolean(1, p.isUrgente());
            s.setString(2, p.getObservacao());
            s.setInt(3, p.getIdPedido());
            s.executeUpdate();
        }
    }

    // ── SOFT DELETE ─────────────────────────────────────────────────

    /* ================================================================
       DESATIVAR PEDIDO (SOFT DELETE)
       ================================================================

       Marca ativo=0 E status='cancelado' ao mesmo tempo — usado por
       PedidoController.cancelar() como parte de uma transação maior
       que também cancela os itens do pedido (ItemPedidoDAO.
       cancelarItensDoPedido) e desativa a entrada na fila de preparo
       (FilaPreparoDAO.desativar), se existir.

       Nunca faz DELETE físico — preserva o pedido no histórico para
       fins de auditoria/relatório, apenas marcando-o como inativo e
       cancelado.
    */
    public void desativar(int id) throws SQLException {
        try (PreparedStatement s = conexao.prepareStatement(
                "UPDATE pedido SET ativo=0, status='cancelado' WHERE id_pedido=?")) {
            s.setInt(1, id); s.executeUpdate();
        }
    }

    // ── MAPEAMENTO ──────────────────────────────────────────────────

    /* ================================================================
       HELPER: MAPEAR RESULTSET → OBJETO Pedido
       ================================================================

       Usado por todos os métodos de leitura para converter uma linha
       do ResultSet (já com o LEFT JOIN em mesa) em um objeto Pedido.

       TRATAMENTO DE MESA NULLABLE:
       Como o JOIN é LEFT, mesa_id pode vir NULL (pedido de delivery).
       O código lê mesa_id com r.getInt() (que retorna 0 se for NULL)
       e IMEDIATAMENTE checa r.wasNull() para saber se o valor
       realmente veio nulo do banco ou se era um 0 legítimo — só
       monta e vincula o objeto Mesa se NÃO for null. Esse é o padrão
       correto para lidar com colunas INT nullable via JDBC (r.getInt()
       sozinho não distingue "0" de "NULL").
    */
    private Pedido mapear(ResultSet r) throws SQLException {
        Pedido p = new Pedido();
        p.setIdPedido(r.getInt("id_pedido"));

        int mesaId = r.getInt("mesa_id");
        if (!r.wasNull()) {
            p.setMesaId(mesaId);
            Mesa m = new Mesa();
            m.setIdMesa(mesaId);
            m.setNumero(r.getInt("mesa_numero"));
            m.setCapacidade(r.getInt("capacidade"));
            m.setStatus(r.getString("mesa_status"));
            p.setMesa(m);
        }

        p.setTipo(r.getString("tipo"));
        p.setUrgente(r.getBoolean("urgente"));
        p.setIdentificadorOperador(r.getString("identificador_operador"));
        p.setStatus(r.getString("status"));
        p.setObservacao(r.getString("observacao"));
        p.setDataAbertura(r.getTimestamp("data_abertura").toLocalDateTime());
        p.setAtivo(r.getBoolean("ativo"));
        return p;
    }
}

/* ================================================================
   RESUMO DO DAO
   ================================================================

   TABELA: pedido (sem renomeação entre v2 e v3)

   MÉTODOS:
   1. listarAbertos()             → não finalizados, urgentes primeiro
   2. listarPorMesa(mesaId)       → pedidos abertos de 1 mesa
   3. listarPorStatus(status)     → filtro por status exato
   4. listarPorOperador(operador) → "Meus Pedidos" do cliente
   5. buscarPorId(id)             → 1 pedido específico
   6. inserir(pedido)             → cria (status inicial 'aberto')
   7. atualizarStatus(id,status)  → avança/altera status
   8. editar(pedido)              → atualiza urgente/observacao
   9. desativar(id)               → soft delete + cancelado

   AJUSTES DO SCHEMA V3:
   ⚠️ identificador_operador ampliado para VARCHAR(100) — nenhuma
      mudança de código necessária neste DAO (Java já trabalha com
      String sem truncamento manual)
   ✅ Nenhuma coluna lida/escrita por este DAO foi renomeada

   PONTOS TÉCNICOS IMPORTANTES:
   ✅ LEFT JOIN com mesa (não INNER) — obrigatório para não excluir
      pedidos de delivery (mesa_id NULL) dos resultados
   ✅ mesa_id é tratado como nullable tanto na escrita (setNull vs
      setInt) quanto na leitura (getInt + wasNull())
   ✅ listarAbertos()/listarPorStatus() ordenam por urgente DESC
      primeiro — reflete a prioridade real de atendimento

   DEPENDÊNCIAS:
   - Pedido / Mesa: models
   - Usado em conjunto, na mesma transação, com ItemPedidoDAO,
     FilaPreparoDAO e PagamentoDAO (ver PedidoController)

   OBSERVAÇÕES GERAIS:
   - Todas as queries usam PreparedStatement (proteção contra SQL
     injection)
   - Conexão é injetada via construtor e gerenciada pelo chamador
   ================================================================ */
