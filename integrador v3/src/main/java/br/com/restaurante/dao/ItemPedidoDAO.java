package br.com.restaurante.dao;
import java.sql.*;
import java.util.*;
import br.com.restaurante.model.Cardapio;
import br.com.restaurante.model.ItemPedido;

/**
 * ================================================================
 * ITEM PEDIDO DAO - Acesso à tabela "item_pedido" (schema v3)
 * ================================================================
 *
 * PROPÓSITO:
 * Centraliza todo o SQL de leitura e escrita da tabela "item_pedido"
 * — as linhas de cada pedido (produto + quantidade + preço no
 * momento da compra). É usado por PedidoController, ClienteController
 * e RelatorioController para inserir, listar e cancelar itens.
 *
 * ⚠️ ATUALIZAÇÃO PARA O SCHEMA V3 (item importante deste arquivo):
 * A coluna "item_cardapio_id" foi renomeada para "cardapio_id",
 * acompanhando a renomeação da tabela "item_cardapio" → "cardapio"
 * (e sua PK "id_item" → "id_cardapio" — ver CardapioDAO). TODAS as
 * queries e JOINs deste DAO foram ajustados para o novo nome:
 *
 *   v2 (antigo)                                  v3 (atual, usado aqui)
 *   -------------------------------------------  -------------------------------------------
 *   JOIN item_cardapio ic ON                     JOIN cardapio ic ON
 *     ip.item_cardapio_id=ic.id_item                ip.cardapio_id=ic.id_cardapio
 *   INSERT INTO item_pedido(...,                 INSERT INTO item_pedido(...,
 *     item_cardapio_id,...)                          cardapio_id,...)
 *   r.getInt("item_cardapio_id")                  r.getInt("cardapio_id")
 *
 * IMPORTANTE: assim como no CardapioDAO, os nomes dos MÉTODOS Java
 * dos models (ex: item.getItemCardapioId(), ip.setItemCardapio())
 * NÃO foram alterados aqui — a mudança é apenas no texto do SQL e
 * nas chaves usadas em rs.getXxx("coluna"). A tabela "item_pedido"
 * EM SI não foi renomeada (só a coluna que referencia o cardápio).
 *
 * TABELA: item_pedido
 * Schema (ver integrador_v3.sql):
 * - id_item_pedido  (PK, AUTO_INCREMENT)
 * - pedido_id        (FK → pedido.id_pedido)
 * - cardapio_id       (FK → cardapio.id_cardapio)   [antes: item_cardapio_id]
 * - quantidade
 * - preco_unitario    (preço travado no momento da compra — nunca
 *                       recalculado a partir do cardápio depois)
 * - observacao
 * - status             (pendente/em_preparo/pronto/entregue/cancelado)
 * - ativo               (TINYINT(1), default 1 — soft delete)
 *
 * MÉTODOS DISPONÍVEIS:
 * - inserir(item)                   → insere 1 item de pedido
 * - inserirLote(itens, pedidoId)    → insere vários itens de uma vez (batch)
 * - listarPorPedido(pedidoId)       → todos os itens ativos de um pedido
 * - buscarPorId(id)                 → 1 item específico
 * - atualizarStatus(id, status)     → avança o status de 1 item
 * - desativar(id)                   → soft delete + status='cancelado'
 * - cancelarItensDoPedido(pedidoId) → cancela em lote (usado ao cancelar o pedido inteiro)
 *
 * PADRÃO SQL_BASE:
 * As consultas de leitura reaproveitam SQL_BASE, que já faz o JOIN
 * com cardapio — assim listarPorPedido() e buscarPorId() sempre
 * trazem também nome, tempo de preparo, categoria e disponibilidade
 * atual do item vendido (últil para a tela de detalhe do pedido).
 *
 * @author Sistema Integrador
 * @version 3.0 - Ajustado para coluna "cardapio_id" (schema v3)
 * @see ItemPedido
 * @see Cardapio
 */
public class ItemPedidoDAO {

    // ---- Conexão injetada via construtor (padrão usado em todo o projeto) ----
    private final Connection conexao;

    /**
     * Query base reaproveitada pelos métodos de LEITURA
     * (listarPorPedido e buscarPorId).
     *
     * v3: JOIN cardapio ic ON ip.cardapio_id=ic.id_cardapio
     *     (antes: JOIN item_cardapio ic ON ip.item_cardapio_id=ic.id_item)
     */
    private static final String SQL_BASE=
        "SELECT ip.*,ic.nome AS nome_item,ic.tempo_preparo_min,ic.categoria_id,ic.disponivel "+
        "FROM item_pedido ip INNER JOIN cardapio ic ON ip.cardapio_id=ic.id_cardapio ";

    /**
     * Construtor — recebe a Connection já aberta (gerenciada pelo
     * controller via try-with-resources, geralmente dentro de uma
     * transação maior junto com PedidoDAO/FilaPreparoDAO).
     */
    public ItemPedidoDAO(Connection c){
        this.conexao=c;
    }

    /* ================================================================
       INSERIR 1 ITEM DE PEDIDO
       ================================================================

       Sempre cria o item com status='pendente' e ativo=1 (valores
       fixos no SQL) — um item recém-adicionado a um pedido sempre
       começa aguardando preparo.

       Usa Statement.RETURN_GENERATED_KEYS para recuperar o
       id_item_pedido gerado e já preenchê-lo de volta no objeto
       recebido.

       v3: coluna cardapio_id no INSERT (antes: item_cardapio_id)
       NOTA: o valor gravado ainda vem de item.getItemCardapioId() —
       método do model ItemPedido, que não foi renomeado.
    */
    public void inserir(ItemPedido item) throws SQLException {
        try(PreparedStatement s=conexao.prepareStatement(
            "INSERT INTO item_pedido(pedido_id,cardapio_id,quantidade,preco_unitario,observacao,status,ativo) VALUES(?,?,?,?,?,'pendente',1)",
            Statement.RETURN_GENERATED_KEYS)){
            s.setInt(1,item.getPedidoId());
            s.setInt(2,item.getItemCardapioId());
            s.setInt(3,item.getQuantidade());
            s.setBigDecimal(4,item.getPrecoUnitario());
            s.setString(5,item.getObservacao());
            s.executeUpdate();

            // ---- Recupera o id_item_pedido gerado e devolve no objeto ----
            try(ResultSet r=s.getGeneratedKeys()){
                if(r.next()) item.setIdItemPedido(r.getInt(1));
            }
        }
    }

    /* ================================================================
       INSERIR VÁRIOS ITENS DE UMA VEZ (BATCH)
       ================================================================

       Usado por PedidoController.criar() e ClienteController.
       confirmarDelivery() para inserir todo o carrinho de uma vez,
       dentro da mesma transação do pedido — muito mais eficiente do
       que chamar inserir() em loop (uma única ida ao banco via
       addBatch()/executeBatch() em vez de N idas).

       Fluxo:
       1. Para cada item da lista: vincula o pedidoId (garantindo que
          todo item da lista pertença ao pedido recém-criado), seta
          os parâmetros e adiciona ao batch
       2. Executa o batch inteiro de uma vez
       3. Recupera as chaves geradas NA MESMA ORDEM em que os itens
          foram adicionados ao batch, e preenche o id_item_pedido de
          volta em cada objeto da lista original — importante para
          que o chamador tenha os ids corretos sem precisar de uma
          segunda consulta

       v3: coluna cardapio_id no INSERT (antes: item_cardapio_id)
    */
    public void inserirLote(List<ItemPedido> itens,int pedidoId) throws SQLException {
        try(PreparedStatement s=conexao.prepareStatement(
            "INSERT INTO item_pedido(pedido_id,cardapio_id,quantidade,preco_unitario,observacao,status,ativo) VALUES(?,?,?,?,?,'pendente',1)",
            Statement.RETURN_GENERATED_KEYS)){
            for(ItemPedido i:itens){
                i.setPedidoId(pedidoId);
                s.setInt(1,pedidoId);
                s.setInt(2,i.getItemCardapioId());
                s.setInt(3,i.getQuantidade());
                s.setBigDecimal(4,i.getPrecoUnitario());
                s.setString(5,i.getObservacao());
                s.addBatch();
            }
            s.executeBatch();

            // ---- Devolve o id gerado de cada item, na ordem do batch ----
            try(ResultSet r=s.getGeneratedKeys()){
                int i=0;
                while(r.next() && i<itens.size()) itens.get(i++).setIdItemPedido(r.getInt(1));
            }
        }
    }

    /* ================================================================
       LISTAR ITENS DE UM PEDIDO
       ================================================================

       Traz todos os itens ATIVOS de um pedido específico (itens
       cancelados individualmente, ainda que o pedido continue ativo,
       não aparecem aqui — ver desativar() e cancelarItensDoPedido()).

       Ordenado por id_item_pedido (ordem de inserção), preservando
       a sequência em que os itens foram adicionados ao pedido.
    */
    public List<ItemPedido> listarPorPedido(int pedidoId) throws SQLException {
        List<ItemPedido> l=new ArrayList<>();
        try(PreparedStatement s=conexao.prepareStatement(
            SQL_BASE+"WHERE ip.pedido_id=? AND ip.ativo=1 ORDER BY ip.id_item_pedido")){
            s.setInt(1,pedidoId);
            try(ResultSet r=s.executeQuery()){
                while(r.next()) l.add(mapear(r));
            }
        }
        return l;
    }

    /* ================================================================
       BUSCAR ITEM DE PEDIDO POR ID
       ================================================================

       Busca um único item de pedido (ativo) pelo seu id_item_pedido.
       Menos usado diretamente pelos controllers atuais (que preferem
       listarPorPedido para carregar tudo de uma vez), mas disponível
       para casos de uso pontuais.
    */
    public ItemPedido buscarPorId(int id) throws SQLException {
        try(PreparedStatement s=conexao.prepareStatement(SQL_BASE+"WHERE ip.id_item_pedido=? AND ip.ativo=1")){
            s.setInt(1,id);
            try(ResultSet r=s.executeQuery()){
                if(r.next()) return mapear(r);
            }
        }
        return null;
    }

    /* ================================================================
       ATUALIZAR STATUS DE UM ITEM
       ================================================================

       Atualiza apenas a coluna "status" de um item específico
       (pendente/em_preparo/pronto/entregue/cancelado). Diferente do
       status do PEDIDO como um todo (controlado por PedidoDAO), este
       é o status de cada LINHA do pedido — útil em cenários onde
       itens diferentes de um mesmo pedido avançam em ritmos distintos
       (ex: bebida pronta antes do prato principal).
    */
    public void atualizarStatus(int id,String status) throws SQLException {
        try(PreparedStatement s=conexao.prepareStatement(
            "UPDATE item_pedido SET status=? WHERE id_item_pedido=?")){
            s.setString(1,status);
            s.setInt(2,id);
            s.executeUpdate();
        }
    }

    /* ================================================================
       DESATIVAR ITEM (SOFT DELETE INDIVIDUAL)
       ================================================================

       Marca um único item como ativo=0 E status='cancelado' ao mesmo
       tempo — usado quando se cancela apenas UM item de um pedido
       (não o pedido inteiro). Nunca faz DELETE físico, preservando o
       histórico para relatórios.
    */
    public void desativar(int id) throws SQLException {
        try(PreparedStatement s=conexao.prepareStatement(
            "UPDATE item_pedido SET ativo=0,status='cancelado' WHERE id_item_pedido=?")){
            s.setInt(1,id);
            s.executeUpdate();
        }
    }

    /* ================================================================
       CANCELAR TODOS OS ITENS DE UM PEDIDO (EM LOTE)
       ================================================================

       Usado por PedidoController.cancelar() quando o pedido INTEIRO
       é cancelado — cancela todos os itens que ainda estão em
       andamento (status IN 'pendente','em_preparo').

       DIFERENÇA IMPORTANTE em relação a desativar():
       - Aqui NÃO se marca ativo=0, apenas status='cancelado'. Isso é
         proposital: o cancelamento do PEDIDO já é controlado pelo
         ativo=0 na tabela pedido (via PedidoDAO.desativar), então
         manter ativo=1 aqui preserva o item "visível" no histórico
         de itens do pedido, só com o status refletindo o cancelamento.
       - Só afeta itens que ainda não foram concluídos (pendente ou
         em_preparo) — itens que já estavam "pronto" ou "entregue"
         permanecem com seu status original, pois já foram
         efetivamente preparados/entregues antes do cancelamento.
    */
    public void cancelarItensDoPedido(int pedidoId) throws SQLException {
        try(PreparedStatement s=conexao.prepareStatement(
            "UPDATE item_pedido SET status='cancelado' WHERE pedido_id=? AND status IN('pendente','em_preparo')")){
            s.setInt(1,pedidoId);
            s.executeUpdate();
        }
    }

    /* ================================================================
       HELPER: MAPEAR RESULTSET → OBJETO ItemPedido
       ================================================================

       Usado por listarPorPedido() e buscarPorId() para converter uma
       linha do ResultSet (que já vem com o JOIN em cardapio) em um
       objeto ItemPedido completo, incluindo um Cardapio parcialmente
       preenchido (id, nome, categoria, tempo de preparo e
       disponibilidade — o suficiente para exibir na tela sem precisar
       de uma segunda consulta ao CardapioDAO).

       v3: r.getInt("cardapio_id") (antes: r.getInt("item_cardapio_id"))

       NOTA: os nomes dos métodos dos models (setIdItem, setItemCardapioId,
       setItemCardapio) continuam os mesmos — só a CHAVE lida do
       ResultSet mudou, acompanhando a coluna renomeada no banco.
    */
    private ItemPedido mapear(ResultSet r) throws SQLException {
        Cardapio ic=new Cardapio();
        ic.setIdItem(r.getInt("cardapio_id"));
        ic.setNome(r.getString("nome_item"));
        ic.setCategoriaId(r.getInt("categoria_id"));
        ic.setTempoPreparoMin(r.getInt("tempo_preparo_min"));
        ic.setDisponivel(r.getBoolean("disponivel"));

        ItemPedido ip=new ItemPedido();
        ip.setIdItemPedido(r.getInt("id_item_pedido"));
        ip.setPedidoId(r.getInt("pedido_id"));
        ip.setItemCardapioId(r.getInt("cardapio_id"));
        ip.setItemCardapio(ic);
        ip.setQuantidade(r.getInt("quantidade"));
        ip.setPrecoUnitario(r.getBigDecimal("preco_unitario"));
        ip.setObservacao(r.getString("observacao"));
        ip.setStatus(r.getString("status"));
        ip.setAtivo(r.getBoolean("ativo"));
        return ip;
    }
}

/* ================================================================
   RESUMO DO DAO
   ================================================================

   TABELA: item_pedido (nome da tabela NÃO mudou)
   COLUNA RENOMEADA: item_cardapio_id → cardapio_id (aponta agora
   para cardapio.id_cardapio, antes item_cardapio.id_item)

   MÉTODOS:
   1. inserir(item)                    → 1 item, status inicial 'pendente'
   2. inserirLote(itens, pedidoId)     → vários itens via batch
   3. listarPorPedido(pedidoId)        → itens ativos de um pedido
   4. buscarPorId(id)                  → 1 item específico
   5. atualizarStatus(id, status)      → muda status de 1 item
   6. desativar(id)                    → soft delete de 1 item (ativo=0 + cancelado)
   7. cancelarItensDoPedido(pedidoId)  → cancela em lote (só status, sem ativo=0)

   AJUSTES APLICADOS NESTA REVISÃO (v2 → v3):
   ✅ JOIN item_cardapio ic ON ip.item_cardapio_id=ic.id_item
      → JOIN cardapio ic ON ip.cardapio_id=ic.id_cardapio (SQL_BASE)
   ✅ INSERT INTO item_pedido(..., item_cardapio_id, ...)
      → INSERT INTO item_pedido(..., cardapio_id, ...) (inserir, inserirLote)
   ✅ r.getInt("item_cardapio_id") → r.getInt("cardapio_id") (mapear)

   O QUE NÃO FOI ALTERADO:
   ⚠️ Nome da tabela item_pedido (só a coluna que referencia o
      cardápio foi renomeada, não a tabela em si)
   ⚠️ Métodos dos models (getItemCardapioId/setItemCardapioId,
      setIdItem, etc.) continuam com os nomes antigos — a mudança
      pedida foi apenas nas QUERIES/colunas do banco

   REGRA DE NEGÓCIO IMPORTANTE (preço travado):
   ✅ preco_unitario é gravado no momento da inserção e NUNCA
      recalculado a partir do cardápio depois — garante que o valor
      cobrado do cliente não mude retroativamente se o preço do
      cardápio for atualizado após o pedido já ter sido feito

   DIFERENÇA ENTRE desativar() E cancelarItensDoPedido():
   ✅ desativar(id): cancela 1 item específico, marcando ativo=0
      (some das listagens) + status='cancelado'
   ✅ cancelarItensDoPedido(pedidoId): cancela em lote todos os itens
      ainda em andamento de um pedido, mas mantém ativo=1 (continuam
      visíveis no histórico do pedido, só com status='cancelado')

   OBSERVAÇÕES GERAIS:
   - Todas as queries usam PreparedStatement (proteção contra SQL
     injection)
   - inserirLote() usa addBatch()/executeBatch() para eficiência
   - Conexão é injetada via construtor e gerenciada pelo chamador,
     tipicamente dentro de uma transação maior (pedido + item_pedido
     + fila_preparo)
   ================================================================ */
