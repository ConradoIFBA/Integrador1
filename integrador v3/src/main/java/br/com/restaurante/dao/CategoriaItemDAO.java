package br.com.restaurante.dao;
import java.sql.*;
import java.util.*;
import br.com.restaurante.model.CategoriaItem;

/**
 * ================================================================
 * CATEGORIA ITEM DAO - Acesso à tabela "categoria_item" (schema v3)
 * ================================================================
 *
 * PROPÓSITO:
 * Centraliza o SQL da tabela "categoria_item" — as categorias do
 * cardápio (ex: Entradas, Pratos Principais, Sobremesas) e o SETOR
 * de preparo associado a cada uma (cozinha/bebida/sobremesa), usado
 * para direcionar pedidos ao painel de fila correto.
 *
 * Usado por CardapioController (montar o <select> de categorias nos
 * formulários de item) e por CardapioDAO (JOIN para trazer nome da
 * categoria e setor junto com cada item do cardápio).
 *
 * ⚠️ NENHUMA MUDANÇA DE SCHEMA V2→V3 AFETA ESTE DAO:
 * A tabela categoria_item e todas as suas colunas mantiveram os
 * mesmos nomes — a renomeação item_cardapio→cardapio não afeta esta
 * tabela (categoria_item é referenciada POR cardapio, não o
 * contrário).
 *
 * TABELA: categoria_item
 * Schema (ver integrador_v3.sql):
 * - id_categoria  (PK, AUTO_INCREMENT)
 * - nome
 * - setor          (ENUM: cozinha, bebida, sobremesa)
 * - ativo           (TINYINT(1), default 1 — soft delete)
 *
 * MÉTODOS DISPONÍVEIS:
 * - listar()                  → todas as categorias ativas
 * - listarPorSetor(setor)     → categorias ativas de 1 setor específico
 * - buscarPorId(id)           → 1 categoria específica
 * - inserir(categoria)        → cria uma nova categoria
 * - editar(categoria)         → atualiza nome/setor
 * - desativar(id)             → soft delete
 *
 * @author Sistema Integrador
 * @version 3.0
 * @see CategoriaItem
 */
public class CategoriaItemDAO {

    // ---- Conexão injetada via construtor (padrão usado em todo o projeto) ----
    private final Connection conexao;

    /**
     * Construtor — recebe a Connection já aberta (gerenciada pelo
     * controller via try-with-resources).
     */
    public CategoriaItemDAO(Connection c){
        this.conexao=c;
    }

    /* ================================================================
       LISTAR TODAS AS CATEGORIAS ATIVAS
       ================================================================

       Traz todas as categorias com ativo=1, ordenadas
       alfabeticamente por nome. Usado por CardapioController para
       montar o <select> de categorias tanto na listagem quanto nos
       formulários de novo item/edição.
    */
    public List<CategoriaItem> listar() throws SQLException {
        List<CategoriaItem> l=new ArrayList<>();
        try(PreparedStatement s=conexao.prepareStatement(
            "SELECT * FROM categoria_item WHERE ativo=1 ORDER BY nome");
            ResultSet r=s.executeQuery()){
            while(r.next()) l.add(mapear(r));
        }
        return l;
    }

    /* ================================================================
       LISTAR CATEGORIAS DE UM SETOR ESPECÍFICO
       ================================================================

       Filtra por "setor" (cozinha/bebida/sobremesa) — útil para
       telas que precisam separar as categorias por área de preparo,
       ao invés de mostrar todas juntas.
    */
    public List<CategoriaItem> listarPorSetor(String setor) throws SQLException {
        List<CategoriaItem> l=new ArrayList<>();
        try(PreparedStatement s=conexao.prepareStatement(
            "SELECT * FROM categoria_item WHERE ativo=1 AND setor=? ORDER BY nome")){
            s.setString(1,setor);
            try(ResultSet r=s.executeQuery()){
                while(r.next()) l.add(mapear(r));
            }
        }
        return l;
    }

    /* ================================================================
       BUSCAR CATEGORIA POR ID
       ================================================================

       Busca uma única categoria ativa pelo id_categoria.
    */
    public CategoriaItem buscarPorId(int id) throws SQLException {
        try(PreparedStatement s=conexao.prepareStatement(
            "SELECT * FROM categoria_item WHERE id_categoria=? AND ativo=1")){
            s.setInt(1,id);
            try(ResultSet r=s.executeQuery()){
                if(r.next()) return mapear(r);
            }
        }
        return null;
    }

    /* ================================================================
       INSERIR NOVA CATEGORIA
       ================================================================

       Cria a categoria sempre com ativo=1 (fixo no SQL). Usa
       Statement.RETURN_GENERATED_KEYS para recuperar o id_categoria
       gerado e devolvê-lo no objeto recebido.
    */
    public void inserir(CategoriaItem c) throws SQLException {
        try(PreparedStatement s=conexao.prepareStatement(
            "INSERT INTO categoria_item(nome,setor,ativo) VALUES(?,?,1)",Statement.RETURN_GENERATED_KEYS)){
            s.setString(1,c.getNome());
            s.setString(2,c.getSetor());
            s.executeUpdate();

            // ---- Recupera o id_categoria gerado e devolve no objeto ----
            try(ResultSet r=s.getGeneratedKeys()){
                if(r.next()) c.setIdCategoria(r.getInt(1));
            }
        }
    }

    /* ================================================================
       EDITAR CATEGORIA
       ================================================================

       Atualiza nome e setor. Trocar o setor de uma categoria já em
       uso afeta indiretamente para qual painel de fila os PRÓXIMOS
       pedidos com itens dessa categoria serão direcionados (o setor
       é lido do cardápio/categoria no momento da criação do pedido —
       ver PedidoController/ClienteController) — pedidos já existentes
       não são retroativamente afetados.
    */
    public void editar(CategoriaItem c) throws SQLException {
        try(PreparedStatement s=conexao.prepareStatement(
            "UPDATE categoria_item SET nome=?,setor=? WHERE id_categoria=?")){
            s.setString(1,c.getNome());
            s.setString(2,c.getSetor());
            s.setInt(3,c.getIdCategoria());
            s.executeUpdate();
        }
    }

    /* ================================================================
       DESATIVAR CATEGORIA (SOFT DELETE)
       ================================================================

       Marca ativo=0 — nunca DELETE físico, preservando o vínculo com
       itens de cardápio que referenciam esta categoria via
       categoria_id (FK com ON DELETE RESTRICT em cardapio, então um
       DELETE físico nem seria permitido pelo banco enquanto houver
       itens vinculados).

       ⚠️ ATENÇÃO: desativar uma categoria NÃO desativa
       automaticamente os itens de cardápio que pertencem a ela — os
       itens continuam ativos e visíveis, só a categoria em si some
       das listagens de categoria_item. Vale considerar, em versões
       futuras, se desativar itens em cascata seria desejável.
    */
    public void desativar(int id) throws SQLException {
        try(PreparedStatement s=conexao.prepareStatement(
            "UPDATE categoria_item SET ativo=0 WHERE id_categoria=?")){
            s.setInt(1,id);
            s.executeUpdate();
        }
    }

    /* ================================================================
       HELPER: MAPEAR RESULTSET → OBJETO CategoriaItem
       ================================================================

       Conversão direta via construtor com todos os campos —
       CategoriaItem é um model simples o suficiente para não
       precisar de setters individuais aqui.
    */
    private CategoriaItem mapear(ResultSet r) throws SQLException {
        return new CategoriaItem(
                r.getInt("id_categoria"),
                r.getString("nome"),
                r.getString("setor"),
                r.getBoolean("ativo"));
    }
}

/* ================================================================
   RESUMO DO DAO
   ================================================================

   TABELA: categoria_item (nenhuma mudança entre v2 e v3)

   MÉTODOS:
   1. listar()               → categorias ativas, ordem alfabética
   2. listarPorSetor(setor)  → idem, filtrado por setor
   3. buscarPorId(id)        → 1 categoria específica
   4. inserir(categoria)     → cria (sempre ativo=1)
   5. editar(categoria)      → atualiza nome/setor
   6. desativar(id)          → soft delete

   AJUSTES DO SCHEMA V3:
   ✅ Nenhum — esta tabela não foi afetada pelas mudanças v2→v3

   RELAÇÃO COM O RESTO DO SISTEMA:
   ✅ Referenciada por cardapio.categoria_id (FK ON DELETE RESTRICT)
   ✅ O campo "setor" desta tabela é o que decide, indiretamente, em
      qual painel da fila de preparo (cozinha/bebida/sobremesa) um
      item vai aparecer quando pedido

   DEPENDÊNCIAS:
   - CategoriaItem: model
   - Usado por CardapioController e via JOIN em CardapioDAO

   OBSERVAÇÕES GERAIS:
   - Todas as queries usam PreparedStatement (proteção contra SQL
     injection)
   - Conexão é injetada via construtor e gerenciada pelo chamador
   ================================================================ */
