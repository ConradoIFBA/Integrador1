package br.com.restaurante.dao;
import java.sql.*;
import java.util.*;
import br.com.restaurante.model.FilaPreparo;
import br.com.restaurante.model.Mesa;
import br.com.restaurante.model.Pedido;

/**
 * ================================================================
 * FILA PREPARO DAO - Acesso à tabela "fila_preparo" (schema v3)
 * ================================================================
 *
 * PROPÓSITO:
 * Centraliza todo o SQL de leitura e escrita da tabela "fila_preparo"
 * — a fila de trabalho da cozinha/bar/sobremesas. Usado por
 * FilaController (listar/iniciar/concluir) e por PedidoController/
 * ClienteController (inserir a entrada da fila junto com a criação
 * do pedido, e desativar ao cancelar).
 *
 * ⚠️ NOTA SOBRE O SCHEMA V3:
 * Assim como em PedidoDAO, a coluna identificador_operador desta
 * tabela também foi ampliada para VARCHAR(100) (antes VARCHAR(20)).
 * Nenhuma alteração de código foi necessária aqui por causa disso —
 * o método iniciarPreparo() já grava qualquer String recebida sem
 * truncamento manual.
 *
 * A tabela "fila_preparo" e todas as suas colunas mantiveram os
 * mesmos nomes entre v2 e v3 — nenhuma renomeação de coluna aqui
 * (a mudança item_cardapio→cardapio não afeta esta tabela, que só
 * referencia "pedido", não o cardápio diretamente).
 *
 * TABELA: fila_preparo
 * Schema (ver integrador_v3.sql):
 * - id_fila                (PK, AUTO_INCREMENT)
 * - pedido_id               (FK → pedido.id_pedido, UNIQUE — 1 fila por pedido)
 * - posicao                 (posição calculada automaticamente por setor)
 * - peso_prioridade          (usado na ordenação — urgente = peso maior)
 * - tempo_estimado_min       (maior tempo de preparo entre os itens do pedido)
 * - setor                    (ENUM: cozinha, bebida, sobremesa)
 * - data_entrada             (DATETIME, default CURRENT_TIMESTAMP)
 * - data_inicio_preparo      (NULL até alguém "assumir" o pedido)
 * - data_conclusao           (NULL até o preparo ser concluído)
 * - identificador_operador   (VARCHAR(100) — quem assumiu o preparo)
 * - ativo                    (TINYINT(1), default 1 — soft delete)
 *
 * MÉTODOS DISPONÍVEIS:
 * - listarFila(setor)             → fila de UM setor específico, pendente
 * - listarFilaGeral()              → fila de TODOS os setores juntos
 * - buscarPorPedido(pedidoId)      → a entrada da fila de um pedido específico
 * - buscarPorId(id)                → 1 entrada da fila
 * - inserir(fila)                  → cria a entrada (calcula a posição automaticamente)
 * - iniciarPreparo(id, operador)   → marca data_inicio_preparo + operador
 * - concluir(id)                   → marca data_conclusao
 * - desativar(id)                  → soft delete (ex: pedido cancelado)
 *
 * PADRÃO SQL_BASE:
 * As consultas de leitura fazem JOIN com pedido (para trazer tipo,
 * urgente, status, observação, data de abertura) e LEFT JOIN com
 * mesa (para trazer o número da mesa, quando aplicável) — assim a
 * tela da fila de preparo já exibe contexto completo do pedido sem
 * precisar de consultas adicionais.
 *
 * @author Sistema Integrador
 * @version 3.0
 * @see FilaPreparo
 * @see Pedido
 * @see Mesa
 */
public class FilaPreparoDAO {

    // ---- Conexão injetada via construtor (padrão usado em todo o projeto) ----
    private final Connection conexao;

    /**
     * Query base reaproveitada pelos métodos de LEITURA. Usa alias
     * (AS operador_pedido, AS status_pedido, AS obs_pedido) para
     * evitar colisão de nomes de coluna entre fila_preparo e pedido
     * (ambas têm "identificador_operador" e conceitos de "status").
     */
    private static final String SQL_BASE=
        "SELECT f.*,p.tipo,p.urgente,p.identificador_operador AS operador_pedido,"+
        "p.status AS status_pedido,p.observacao AS obs_pedido,p.data_abertura,p.mesa_id,m.numero AS mesa_numero "+
        "FROM fila_preparo f INNER JOIN pedido p ON f.pedido_id=p.id_pedido "+
        "LEFT JOIN mesa m ON p.mesa_id=m.id_mesa ";

    /**
     * Construtor — recebe a Connection já aberta (gerenciada pelo
     * controller via try-with-resources, geralmente dentro da mesma
     * transação que cria/atualiza o pedido).
     */
    public FilaPreparoDAO(Connection c){
        this.conexao=c;
    }

    /* ================================================================
       LISTAR FILA DE UM SETOR ESPECÍFICO
       ================================================================

       Traz apenas entradas ATIVAS, do setor informado, que AINDA NÃO
       foram concluídas (data_conclusao IS NULL) — ou seja, o que
       realmente está pendente de trabalho naquele setor agora.

       ORDENAÇÃO: peso_prioridade DESC primeiro (pedidos urgentes/com
       maior peso aparecem no topo), depois data_entrada ASC (entre
       pedidos de mesmo peso, o que entrou primeiro na fila é
       trabalhado primeiro — ordem FIFO dentro de cada nível de
       prioridade).

       Usado por FilaController.doGet() — chamado 3 vezes (uma por
       setor: cozinha, bebida, sobremesa) para montar os 3 painéis
       da tela.
    */
    public List<FilaPreparo> listarFila(String setor) throws SQLException {
        List<FilaPreparo> l=new ArrayList<>();
        try(PreparedStatement s=conexao.prepareStatement(SQL_BASE+
            "WHERE f.ativo=1 AND f.setor=? AND f.data_conclusao IS NULL "+
            "ORDER BY f.peso_prioridade DESC,f.data_entrada ASC")){
            s.setString(1,setor);
            try(ResultSet r=s.executeQuery()){
                while(r.next()) l.add(mapear(r));
            }
        }
        return l;
    }

    /* ================================================================
       LISTAR FILA GERAL (todos os setores juntos)
       ================================================================

       Mesma lógica de listarFila(), mas sem filtrar por setor — traz
       tudo que está pendente em qualquer setor. Útil para uma visão
       consolidada (ex: um painel único de "tudo que falta preparar",
       ao invés de painéis separados por setor).

       Atualmente não é chamado por nenhum controller revisado até
       aqui, mas fica disponível como utilitário do DAO.
    */
    public List<FilaPreparo> listarFilaGeral() throws SQLException {
        List<FilaPreparo> l=new ArrayList<>();
        try(PreparedStatement s=conexao.prepareStatement(SQL_BASE+
            "WHERE f.ativo=1 AND f.data_conclusao IS NULL ORDER BY f.peso_prioridade DESC,f.data_entrada ASC");
            ResultSet r=s.executeQuery()){
            while(r.next()) l.add(mapear(r));
        }
        return l;
    }

    /* ================================================================
       BUSCAR ENTRADA DA FILA POR PEDIDO
       ================================================================

       Como pedido_id é UNIQUE na tabela fila_preparo (1 pedido tem
       no máximo 1 entrada na fila), este método sempre retorna 0 ou
       1 resultado. Usado extensivamente por PedidoController.
       avancarStatus() e FilaController para localizar a entrada da
       fila correspondente a um pedido antes de iniciar/concluir o
       preparo.
    */
    public FilaPreparo buscarPorPedido(int pedidoId) throws SQLException {
        try(PreparedStatement s=conexao.prepareStatement(SQL_BASE+"WHERE f.pedido_id=? AND f.ativo=1")){
            s.setInt(1,pedidoId);
            try(ResultSet r=s.executeQuery()){
                if(r.next()) return mapear(r);
            }
        }
        return null;
    }

    /* ================================================================
       BUSCAR ENTRADA DA FILA POR ID
       ================================================================

       Busca direta pelo id_fila — usado quando o controller já tem
       o id da entrada da fila em mãos (ex: vindo de um parâmetro de
       formulário, como em FilaController.iniciarPreparo()).
    */
    public FilaPreparo buscarPorId(int id) throws SQLException {
        try(PreparedStatement s=conexao.prepareStatement(SQL_BASE+"WHERE f.id_fila=? AND f.ativo=1")){
            s.setInt(1,id);
            try(ResultSet r=s.executeQuery()){
                if(r.next()) return mapear(r);
            }
        }
        return null;
    }

    /* ================================================================
       INSERIR NOVA ENTRADA NA FILA
       ================================================================

       Fluxo:
       1. ANTES de inserir, calcula automaticamente a próxima posição
          disponível PARA AQUELE SETOR (proximaPosicao) — cada setor
          tem sua própria numeração de posição, independente dos
          outros setores.
       2. Insere a entrada com ativo=1 (fixo no SQL).
       3. Recupera o id_fila gerado via RETURN_GENERATED_KEYS e
          devolve no objeto recebido.

       Chamado sempre logo após a criação de um pedido (dentro da
       mesma transação, em PedidoController.criar() e
       ClienteController.confirmarDelivery()) — todo pedido novo
       automaticamente entra na fila de preparo do setor apropriado.
    */
    public void inserir(FilaPreparo f) throws SQLException {
        // ---- Calcula a posição antes de montar o INSERT ----
        f.setPosicao(proximaPosicao(f.getSetor()));

        try(PreparedStatement s=conexao.prepareStatement(
            "INSERT INTO fila_preparo(pedido_id,posicao,peso_prioridade,tempo_estimado_min,setor,ativo) VALUES(?,?,?,?,?,1)",
            Statement.RETURN_GENERATED_KEYS)){
            s.setInt(1,f.getPedidoId());
            s.setInt(2,f.getPosicao());
            s.setInt(3,f.getPesoPrioridade());
            s.setInt(4,f.getTempoEstimadoMin());
            s.setString(5,f.getSetor());
            s.executeUpdate();

            // ---- Recupera o id_fila gerado e devolve no objeto ----
            try(ResultSet r=s.getGeneratedKeys()){
                if(r.next()) f.setIdFila(r.getInt(1));
            }
        }
    }

    /* ================================================================
       INICIAR PREPARO (ASSUMIR PEDIDO)
       ================================================================

       Marca data_inicio_preparo=NOW() e grava o operador que assumiu
       o pedido. Chamado por FilaController.iniciarPreparo() (e por
       PedidoController.avancarStatus() quando o status avança para
       "em_preparo"), sempre dentro de uma transação que também
       atualiza o status do PEDIDO (não só da fila).

       ⚠️ identificador_operador agora comporta até 100 caracteres
       (schema v3) — este método não precisou de ajuste porque já
       gravava qualquer String recebida via setString(), sem limite
       imposto pelo código Java.
    */
    public void iniciarPreparo(int id,String operador) throws SQLException {
        try(PreparedStatement s=conexao.prepareStatement(
            "UPDATE fila_preparo SET data_inicio_preparo=NOW(),identificador_operador=? WHERE id_fila=?")){
            s.setString(1,operador);
            s.setInt(2,id);
            s.executeUpdate();
        }
    }

    /* ================================================================
       CONCLUIR PREPARO
       ================================================================

       Marca data_conclusao=NOW() — a partir daqui, esta entrada não
       aparece mais em listarFila()/listarFilaGeral() (que filtram
       por data_conclusao IS NULL), mesmo que ativo continue = 1
       (permanece no histórico, só "sai da fila visível").

       Chamado por FilaController.concluirPreparo() e por
       PedidoController.avancarStatus() quando o status avança para
       "entregue" (o pedido já passou por "pronto" antes).
    */
    public void concluir(int id) throws SQLException {
        try(PreparedStatement s=conexao.prepareStatement(
            "UPDATE fila_preparo SET data_conclusao=NOW() WHERE id_fila=?")){
            s.setInt(1,id);
            s.executeUpdate();
        }
    }

    /* ================================================================
       DESATIVAR ENTRADA DA FILA (SOFT DELETE)
       ================================================================

       Marca ativo=0 — usado quando o PEDIDO correspondente é
       cancelado (PedidoController.cancelar()), removendo a entrada
       da fila de trabalho sem apagar o registro histórico.
    */
    public void desativar(int id) throws SQLException {
        try(PreparedStatement s=conexao.prepareStatement(
            "UPDATE fila_preparo SET ativo=0 WHERE id_fila=?")){
            s.setInt(1,id);
            s.executeUpdate();
        }
    }

    /* ================================================================
       HELPER: CALCULAR A PRÓXIMA POSIÇÃO DE UM SETOR
       ================================================================

       Busca o MAIOR valor de "posicao" já usado entre as entradas
       ATIVAS daquele setor e retorna +1 (ou 1, se a fila do setor
       estiver vazia — COALESCE cobre esse caso).

       IMPORTANTE: a posição é por SETOR — cozinha, bebida e
       sobremesa têm cada uma sua própria sequência de posições,
       independentes entre si. Isso é intencional: cada painel de
       fila (um por setor) mostra sua própria numeração sequencial,
       sem "buracos" causados por pedidos de outros setores.

       NOTA: esta é uma posição "de nascimento" — reflete a ordem em
       que os pedidos ENTRARAM na fila daquele setor, não
       necessariamente a ordem de exibição (que também considera
       peso_prioridade na hora de listar).
    */
    private int proximaPosicao(String setor) throws SQLException {
        try(PreparedStatement s=conexao.prepareStatement(
            "SELECT COALESCE(MAX(posicao),0)+1 AS prox FROM fila_preparo WHERE ativo=1 AND setor=?")){
            s.setString(1,setor);
            try(ResultSet r=s.executeQuery()){
                return r.next() ? r.getInt("prox") : 1;
            }
        }
    }

    /* ================================================================
       HELPER: MAPEAR RESULTSET → OBJETO FilaPreparo
       ================================================================

       Monta tanto o objeto FilaPreparo quanto um objeto Pedido
       "embutido" (com os dados vindos do JOIN — tipo, urgente,
       status, observação, data de abertura, e a Mesa associada, se
       houver) — assim a tela da fila pode exibir contexto completo
       do pedido sem consultas adicionais.

       TRATAMENTO DE MESA NULLABLE:
       Mesmo padrão usado em PedidoDAO — lê mesa_id com getInt() e
       checa wasNull() antes de montar o objeto Mesa, já que pedidos
       de delivery não têm mesa vinculada (LEFT JOIN traz NULL).

       TRATAMENTO DE DATAS NULLABLE:
       data_inicio_preparo e data_conclusao podem ser NULL (pedido
       ainda não foi assumido / ainda não foi concluído) — o código
       verifica if(timestamp != null) antes de converter para
       LocalDateTime, evitando NullPointerException.
    */
    private FilaPreparo mapear(ResultSet r) throws SQLException {
        // ---- Monta a Mesa associada, se houver (mesma checagem de nullable de PedidoDAO) ----
        Mesa mesa=null;
        int mesaId=r.getInt("mesa_id");
        if(!r.wasNull()){
            mesa=new Mesa();
            mesa.setIdMesa(mesaId);
            mesa.setNumero(r.getInt("mesa_numero"));
        }

        // ---- Monta o Pedido "embutido" com os dados trazidos pelo JOIN ----
        Pedido p=new Pedido();
        p.setIdPedido(r.getInt("pedido_id"));
        p.setTipo(r.getString("tipo"));
        p.setUrgente(r.getBoolean("urgente"));
        p.setIdentificadorOperador(r.getString("operador_pedido"));
        p.setStatus(r.getString("status_pedido"));
        p.setObservacao(r.getString("obs_pedido"));
        p.setDataAbertura(r.getTimestamp("data_abertura").toLocalDateTime());
        if(mesa!=null){
            p.setMesaId(mesaId);
            p.setMesa(mesa);
        }

        // ---- Monta a própria entrada da fila ----
        FilaPreparo f=new FilaPreparo();
        f.setIdFila(r.getInt("id_fila"));
        f.setPedidoId(r.getInt("pedido_id"));
        f.setPedido(p);
        f.setPosicao(r.getInt("posicao"));
        f.setPesoPrioridade(r.getInt("peso_prioridade"));
        f.setTempoEstimadoMin(r.getInt("tempo_estimado_min"));
        f.setSetor(r.getString("setor"));
        f.setDataEntrada(r.getTimestamp("data_entrada").toLocalDateTime());

        // ---- Datas opcionais: só converte se não forem NULL ----
        Timestamp ini=r.getTimestamp("data_inicio_preparo");
        if(ini!=null) f.setDataInicioPreparo(ini.toLocalDateTime());
        Timestamp con=r.getTimestamp("data_conclusao");
        if(con!=null) f.setDataConclusao(con.toLocalDateTime());

        f.setIdentificadorOperador(r.getString("identificador_operador"));
        f.setAtivo(r.getBoolean("ativo"));
        return f;
    }
}

/* ================================================================
   RESUMO DO DAO
   ================================================================

   TABELA: fila_preparo (sem renomeação de tabela ou coluna entre v2 e v3)

   MÉTODOS:
   1. listarFila(setor)          → fila pendente de 1 setor
   2. listarFilaGeral()           → fila pendente de todos os setores
   3. buscarPorPedido(pedidoId)   → entrada da fila de 1 pedido (0 ou 1)
   4. buscarPorId(id)             → 1 entrada específica
   5. inserir(fila)                → cria (calcula posição automaticamente)
   6. iniciarPreparo(id,operador) → marca início + operador
   7. concluir(id)                 → marca conclusão
   8. desativar(id)                → soft delete

   AJUSTES DO SCHEMA V3:
   ⚠️ identificador_operador ampliado para VARCHAR(100) — nenhuma
      mudança de código necessária (Java já gravava String sem
      truncamento)
   ✅ Nenhuma coluna renomeada nesta tabela

   PONTOS TÉCNICOS IMPORTANTES:
   ✅ Posição da fila é calculada por SETOR (cada setor tem sua
      própria sequência independente)
   ✅ "Fila visível" (listarFila/listarFilaGeral) = ativo=1 E
      data_conclusao IS NULL — entradas concluídas somem da fila mas
      continuam no banco para histórico
   ✅ Ordenação sempre prioriza peso_prioridade DESC, depois FIFO
      (data_entrada ASC)
   ✅ pedido_id é UNIQUE — buscarPorPedido() nunca retorna mais de 1
      resultado

   DEPENDÊNCIAS:
   - FilaPreparo / Pedido / Mesa: models
   - Usado em conjunto, na mesma transação, com PedidoDAO (ver
     PedidoController e ClienteController)

   OBSERVAÇÕES GERAIS:
   - Todas as queries usam PreparedStatement (proteção contra SQL
     injection)
   - Conexão é injetada via construtor e gerenciada pelo chamador
   ================================================================ */
