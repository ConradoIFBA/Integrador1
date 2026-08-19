package br.com.restaurante.dao;
import java.sql.*;
import java.util.*;
import br.com.restaurante.model.CategoriaItem;
import br.com.restaurante.model.Cardapio;

/**
 * ================================================================
 * CARDAPIO DAO - Acesso à tabela "cardapio" (schema v3)
 * ================================================================
 *
 * PROPÓSITO:
 * Centraliza todo o SQL de leitura e escrita da tabela "cardapio"
 * (renomeada a partir de "item_cardapio" no schema v2→v3 — ver
 * integrador_v3.sql). É o DAO usado por CardapioController,
 * ClienteController e PedidoController para listar itens, validar
 * disponibilidade e obter o preço atual no momento da compra.
 *
 * ⚠️ ATUALIZAÇÃO PARA O SCHEMA V3 (item importante deste arquivo):
 * A tabela "item_cardapio" foi renomeada para "cardapio" e sua PK
 * "id_item" foi renomeada para "id_cardapio". TODAS as queries deste
 * DAO foram ajustadas para os novos nomes:
 *
 *   v2 (antigo)                         v3 (atual, usado aqui)
 *   ---------------------------------   ---------------------------------
 *   FROM item_cardapio i                FROM cardapio i
 *   WHERE i.id_item = ?                 WHERE i.id_cardapio = ?
 *   INSERT INTO item_cardapio(...)      INSERT INTO cardapio(...)
 *   UPDATE item_cardapio SET ...        UPDATE cardapio SET ...
 *   r.getInt("id_item")                 r.getInt("id_cardapio")
 *
 * IMPORTANTE: os nomes dos MÉTODOS Java do model Cardapio (ex:
 * getIdItem()/setIdItem()) NÃO foram alterados aqui — a mudança é
 * apenas no texto do SQL e nas chaves usadas em rs.getXxx("coluna").
 * Isso porque o model Cardapio não foi fornecido para revisão nesta
 * etapa; se o model também for renomeado depois (ex: getIdCardapio()),
 * este DAO precisará de um novo ajuste.
 *
 * TABELA: cardapio
 * Schema (ver integrador_v3.sql):
 * - id_cardapio       (PK, AUTO_INCREMENT)   [antes: id_item]
 * - categoria_id       (FK → categoria_item.id_categoria)
 * - nome
 * - descricao
 * - preco               (DECIMAL 10,2)
 * - tempo_preparo_min   (INT, default 15)
 * - disponivel          (TINYINT(1), default 1)
 * - ativo               (TINYINT(1), default 1 — soft delete)
 *
 * MÉTODOS DISPONÍVEIS:
 * - listar()                         → todos os itens ativos e disponíveis
 * - listarPorCategoria(catId)        → itens ativos/disponíveis de 1 categoria
 * - buscarPorId(id)                  → 1 item específico (mesmo se indisponível)
 * - inserir(item)                    → cria um novo item
 * - editar(item)                     → atualiza dados de um item existente
 * - atualizarDisponibilidade(id,bool)→ liga/desliga disponibilidade
 * - desativar(id)                    → soft delete (ativo=0)
 *
 * PADRÃO SQL_BASE:
 * As consultas de leitura reaproveitam a constante SQL_BASE, que já
 * faz o JOIN com categoria_item — assim listar(), listarPorCategoria()
 * e buscarPorId() sempre trazem também o nome e o setor da categoria
 * (usados, por exemplo, para decidir a fila de preparo em
 * PedidoController/ClienteController).
 *
 * @author Sistema Integrador
 * @version 3.0 - Ajustado para tabela "cardapio" (schema v3)
 * @see Cardapio
 * @see CategoriaItem
 */
public class CardapioDAO {

    // ---- Conexão injetada via construtor (padrão usado em todo o projeto) ----
    private final Connection conexao;

    /**
     * Query base reaproveitada por todos os métodos de LEITURA.
     * Já inclui o JOIN com categoria_item, trazendo nome_categoria e
     * setor junto com os dados do próprio item do cardápio.
     *
     * v3: FROM cardapio (antes: FROM item_cardapio)
     */
    private static final String SQL_BASE=
        "SELECT i.*,c.nome AS nome_categoria,c.setor FROM cardapio i "+
        "INNER JOIN categoria_item c ON i.categoria_id=c.id_categoria ";

    /**
     * Construtor — recebe a Connection já aberta (gerenciada pelo
     * controller via try-with-resources, seguindo o padrão de todo
     * o projeto: uma Connection por requisição/transação).
     */
    public CardapioDAO(Connection c){
        this.conexao=c;
    }

    /* ================================================================
       LISTAR TODOS OS ITENS (ativos + disponíveis)
       ================================================================

       Usado nas telas de cardápio (cliente e gerente) e ao montar o
       formulário de novo pedido — só traz itens que podem ser
       efetivamente vendidos agora (ativo=1 E disponivel=1).

       Ordenado por categoria e depois por nome, para exibição
       agrupada e alfabética na tela.
    */
    public List<Cardapio> listar() throws SQLException {
        List<Cardapio> l=new ArrayList<>();
        try(PreparedStatement s=conexao.prepareStatement(
            SQL_BASE+"WHERE i.ativo=1 AND i.disponivel=1 ORDER BY c.nome,i.nome");
            ResultSet r=s.executeQuery()){
            while(r.next()) l.add(mapear(r));
        }
        return l;
    }

    /* ================================================================
       LISTAR ITENS DE UMA CATEGORIA ESPECÍFICA
       ================================================================

       Mesma regra de listar() (ativo + disponível), mas filtrando
       por categoria_id — útil para telas que separam o cardápio por
       abas/categorias.
    */
    public List<Cardapio> listarPorCategoria(int catId) throws SQLException {
        List<Cardapio> l=new ArrayList<>();
        try(PreparedStatement s=conexao.prepareStatement(
            SQL_BASE+"WHERE i.ativo=1 AND i.disponivel=1 AND i.categoria_id=? ORDER BY i.nome")){
            s.setInt(1,catId);
            try(ResultSet r=s.executeQuery()){
                while(r.next()) l.add(mapear(r));
            }
        }
        return l;
    }

    /* ================================================================
       BUSCAR ITEM POR ID
       ================================================================

       DIFERENÇA IMPORTANTE em relação a listar(): aqui NÃO se filtra
       por "disponivel=1" — só por "ativo=1". Isso é proposital: os
       controllers usam este método também para validar um item que
       está no carrinho/formulário, e precisam saber se ele existe e
       está ativo mesmo que esteja temporariamente indisponível (a
       checagem de disponibilidade, quando necessária, é feita à
       parte pelo chamador via item.isDisponivel()).

       v3: WHERE i.id_cardapio=? (antes: WHERE i.id_item=?)
    */
    public Cardapio buscarPorId(int id) throws SQLException {
        try(PreparedStatement s=conexao.prepareStatement(SQL_BASE+"WHERE i.id_cardapio=? AND i.ativo=1")){
            s.setInt(1,id);
            try(ResultSet r=s.executeQuery()){
                if(r.next()) return mapear(r);
            }
        }
        return null;
    }

    /* ================================================================
       INSERIR NOVO ITEM
       ================================================================

       Cria um item sempre com disponivel=1 e ativo=1 (valores fixos
       no próprio SQL — um item recém-criado nasce disponível para
       venda e ativo no sistema).

       Usa Statement.RETURN_GENERATED_KEYS para recuperar o
       id_cardapio gerado pelo AUTO_INCREMENT e já preenchê-lo de
       volta no objeto recebido (i.setIdItem(...)) — assim o
       controller já tem o id disponível logo após o insert, sem
       precisar de uma segunda consulta.

       v3: INSERT INTO cardapio(...) (antes: INSERT INTO item_cardapio(...))
    */
    public void inserir(Cardapio i) throws SQLException {
        try(PreparedStatement s=conexao.prepareStatement(
            "INSERT INTO cardapio(categoria_id,nome,descricao,imagem,preco,tempo_preparo_min,disponivel,ativo) VALUES(?,?,?,?,?,?,1,1)",
            Statement.RETURN_GENERATED_KEYS)){
            s.setInt(1,i.getCategoriaId());
            s.setString(2,i.getNome());
            s.setString(3,i.getDescricao());
            s.setString(4,i.getImagem());
            s.setBigDecimal(5,i.getPreco());
            s.setInt(6,i.getTempoPreparoMin());
            s.executeUpdate();

            // ---- Recupera o id_cardapio gerado e devolve no objeto ----
            try(ResultSet r=s.getGeneratedKeys()){
                if(r.next()) i.setIdItem(r.getInt(1));
            }
        }
    }

    /* ================================================================
       EDITAR ITEM EXISTENTE
       ================================================================

       Atualiza categoria, nome, descrição, preço e tempo de preparo.
       NÃO mexe em "disponivel" nem "ativo" — essas duas colunas têm
       métodos dedicados (atualizarDisponibilidade / desativar),
       evitando que uma edição comum de texto/preço acidentalmente
       reative ou desative um item.

       v3: UPDATE cardapio ... WHERE id_cardapio=?
           (antes: UPDATE item_cardapio ... WHERE id_item=?)
    */
    public void editar(Cardapio i) throws SQLException {
        try(PreparedStatement s=conexao.prepareStatement(
            "UPDATE cardapio SET categoria_id=?,nome=?,descricao=?,imagem=?,preco=?,tempo_preparo_min=? WHERE id_cardapio=?")){
            s.setInt(1,i.getCategoriaId());
            s.setString(2,i.getNome());
            s.setString(3,i.getDescricao());
            s.setString(4,i.getImagem());
            s.setBigDecimal(5,i.getPreco());
            s.setInt(6,i.getTempoPreparoMin());
            s.setInt(7,i.getIdItem());
            s.executeUpdate();
        }
    }

    /* ================================================================
       ATUALIZAR DISPONIBILIDADE
       ================================================================

       Liga/desliga apenas a coluna "disponivel" — usado para marcar
       itens temporariamente esgotados sem precisar excluí-los do
       cardápio (o item continua existindo e podendo ser reativado
       a qualquer momento).

       v3: UPDATE cardapio SET disponivel=? WHERE id_cardapio=?
    */
    public void atualizarDisponibilidade(int id,boolean disp) throws SQLException {
        try(PreparedStatement s=conexao.prepareStatement(
            "UPDATE cardapio SET disponivel=? WHERE id_cardapio=?")){
            s.setBoolean(1,disp);
            s.setInt(2,id);
            s.executeUpdate();
        }
    }

    /* ================================================================
       DESATIVAR ITEM (SOFT DELETE)
       ================================================================

       Nunca faz DELETE físico — apenas marca ativo=0, preservando o
       vínculo histórico com item_pedido de pedidos já realizados com
       este item (uma FK apontando para uma linha deletada quebraria
       o histórico de pedidos antigos).

       v3: UPDATE cardapio SET ativo=0 WHERE id_cardapio=?
    */
    public void desativar(int id) throws SQLException {
        try(PreparedStatement s=conexao.prepareStatement(
            "UPDATE cardapio SET ativo=0 WHERE id_cardapio=?")){
            s.setInt(1,id);
            s.executeUpdate();
        }
    }

    /* ================================================================
       HELPER: MAPEAR RESULTSET → OBJETO Cardapio
       ================================================================

       Usado por todos os métodos de leitura (listar, listarPorCategoria,
       buscarPorId) para converter uma linha do ResultSet em um objeto
       Cardapio já com a CategoriaItem associada preenchida (evita que
       a camada de controller/view precise buscar a categoria separado).

       v3: r.getInt("id_cardapio") (antes: r.getInt("id_item"))

       NOTA: o nome do método do model continua sendo setIdItem() —
       só a CHAVE lida do ResultSet mudou de "id_item" para
       "id_cardapio", acompanhando a coluna renomeada no banco.
    */
    private Cardapio mapear(ResultSet r) throws SQLException {
        CategoriaItem cat=new CategoriaItem(
                r.getInt("categoria_id"),
                r.getString("nome_categoria"),
                r.getString("setor"),
                true);

        Cardapio i=new Cardapio();
        i.setIdItem(r.getInt("id_cardapio"));
        i.setCategoriaId(r.getInt("categoria_id"));
        i.setCategoria(cat);
        i.setNome(r.getString("nome"));
        i.setDescricao(r.getString("descricao"));
        i.setImagem(r.getString("imagem"));
        i.setPreco(r.getBigDecimal("preco"));
        i.setTempoPreparoMin(r.getInt("tempo_preparo_min"));
        i.setDisponivel(r.getBoolean("disponivel"));
        i.setAtivo(r.getBoolean("ativo"));
        return i;
    }
}

/* ================================================================
   RESUMO DO DAO
   ================================================================

   TABELA: cardapio (renomeada de item_cardapio no schema v3)
   PK: id_cardapio (renomeada de id_item)

   MÉTODOS:
   1. listar()                          → itens ativos + disponíveis
   2. listarPorCategoria(catId)         → idem, filtrado por categoria
   3. buscarPorId(id)                   → item ativo (mesmo indisponível)
   4. inserir(item)                     → cria (sempre disponível+ativo)
   5. editar(item)                      → atualiza dados básicos
   6. atualizarDisponibilidade(id,bool) → liga/desliga venda
   7. desativar(id)                     → soft delete (ativo=0)

   AJUSTES APLICADOS NESTA REVISÃO (v2 → v3):
   ✅ FROM item_cardapio → FROM cardapio (SQL_BASE)
   ✅ WHERE i.id_item=? → WHERE i.id_cardapio=? (buscarPorId)
   ✅ INSERT INTO item_cardapio → INSERT INTO cardapio (inserir)
   ✅ UPDATE item_cardapio → UPDATE cardapio (editar, atualizarDisponibilidade, desativar)
   ✅ WHERE id_item=? → WHERE id_cardapio=? (editar, atualizarDisponibilidade, desativar)
   ✅ r.getInt("id_item") → r.getInt("id_cardapio") (mapear)

   O QUE NÃO FOI ALTERADO:
   ⚠️ Métodos do model Cardapio (getIdItem/setIdItem) continuam com
      o nome antigo — a mudança pedida foi apenas nas QUERIES/colunas
      do banco, não na API Java do model.

   OBSERVAÇÕES GERAIS:
   - Todas as queries usam PreparedStatement (proteção contra SQL
     injection)
   - Conexão é injetada via construtor e gerenciada pelo chamador
     (try-with-resources no controller)
   - listar()/listarPorCategoria() sempre exigem disponivel=1;
     buscarPorId() não — ver nota no método
   ================================================================ */
