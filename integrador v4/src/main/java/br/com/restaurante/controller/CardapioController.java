package br.com.restaurante.controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.List;

import br.com.restaurante.dao.CategoriaItemDAO;
import br.com.restaurante.dao.CardapioDAO;
import br.com.restaurante.model.CategoriaItem;
import br.com.restaurante.model.Cardapio;
import br.com.restaurante.model.Usuario;
import br.com.restaurante.utils.Conexao;
import br.com.restaurante.utils.UploadImagemUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

/**
 * ================================================================
 * CARDAPIO CONTROLLER - Gestão do Cardápio (CRUD de Itens)
 * ================================================================
 *
 * PROPÓSITO:
 * Gerencia a listagem, criação, edição, exclusão (soft delete) e
 * alternância de disponibilidade dos itens do cardápio. É também
 * a tela inicial de todo cliente (perfil USUARIO) após o login.
 *
 * FUNCIONALIDADES:
 * 1. Listar o cardápio completo (visível a todos os perfis logados)
 * 2. Exibir formulário de novo item                  (só GERENTE)
 * 3. Exibir formulário de edição de item existente    (só GERENTE)
 * 4. Salvar (inserir ou atualizar) um item             (só GERENTE)
 * 5. Excluir (soft delete) um item                     (só GERENTE)
 * 6. Alternar disponibilidade (disponível/indisponível)(só GERENTE)
 *
 * ROTA MAPEADA: /app/cardapio (único endpoint, roteado por "acao")
 *
 * GET  sem acao          → lista o cardápio
 * GET  acao=novo         → formulário de novo item        (só GERENTE)
 * GET  acao=editar&id=X  → formulário preenchido           (só GERENTE)
 * POST acao=salvar       → insere ou atualiza              (só GERENTE)
 * POST acao=excluir      → soft delete (ativo=0)           (só GERENTE)
 * POST acao=disponivel   → alterna disponibilidade         (só GERENTE)
 *
 * TABELA: cardapio (antes chamada item_cardapio — ver integrador_v2.sql)
 * Schema:
 * - id_cardapio        (PK, AUTO_INCREMENT)
 * - categoria_id        (FK → categoria_item.id_categoria)
 * - nome                (NOT NULL)
 * - descricao            (TEXT)
 * - preco                (DECIMAL 10,2)
 * - tempo_preparo_min    (INT, default 15)
 * - disponivel           (TINYINT(1), default 1)
 * - ativo                (TINYINT(1), default 1 — soft delete)
 *
 * PERMISSÕES:
 * ✅ Qualquer usuário logado pode LISTAR o cardápio (GET sem acao)
 * ✅ Apenas GERENTE pode criar, editar, excluir ou alternar
 *    disponibilidade — a checagem é feita em isGerente() e aplicada
 *    tanto no doGet() (para acao=novo/editar) quanto no doPost()
 *    (para todas as ações, já que toda escrita exige o perfil).
 *
 * FLUXO DE LISTAGEM (GET sem acao):
 * 1. Busca todas as categorias (para montar filtros/selects na view)
 * 2. Busca todos os itens do cardápio
 * 3. Recupera mensagem de sucesso da sessão (se houver, ex: pós-salvar)
 * 4. Encaminha para cardapio.jsp
 *
 * FLUXO DE SALVAR (POST acao=salvar):
 * 1. Lê os parâmetros do formulário (id, categoriaId, nome, etc.)
 * 2. Converte preço (aceita vírgula como separador decimal) e tempo
 * 3. Se id <= 0 → é um item NOVO → monta objeto e insere
 * 4. Se id > 0  → é uma EDIÇÃO → busca o item existente, atualiza
 *    os campos e salva
 * 5. Define mensagem de sucesso na sessão e redireciona para a lista
 *
 * FLUXO DE EXCLUSÃO (POST acao=excluir):
 * 1. Lê o id do item
 * 2. Chama dao.desativar(id) → soft delete (ativo=0), não remove a
 *    linha do banco (preserva histórico de pedidos já feitos)
 *
 * FLUXO DE DISPONIBILIDADE (POST acao=disponivel):
 * 1. Lê o id do item e o novo valor ("1" = disponível, outro = não)
 * 2. Atualiza apenas a coluna "disponivel", sem afetar o resto do item
 *
 * EXEMPLO DE USO:
 * ```
 * // Listar:
 * GET /app/cardapio
 *
 * // Novo item (form):
 * GET /app/cardapio?acao=novo
 *
 * // Editar item (form preenchido):
 * GET /app/cardapio?acao=editar&id=5
 *
 * // Salvar (novo ou edição):
 * POST /app/cardapio
 * acao=salvar&id=0&categoriaId=2&nome=Risoto&preco=45,00&tempoPreparoMin=25
 *
 * // Excluir:
 * POST /app/cardapio
 * acao=excluir&id=5
 *
 * // Alternar disponibilidade:
 * POST /app/cardapio
 * acao=disponivel&id=5&valor=0
 * ```
 *
 * @author Sistema Integrador
 * @version 3.0
 * @see CardapioDAO
 * @see Cardapio
 * @see CategoriaItemDAO
 */
@WebServlet("/app/cardapio")
// @MultipartConfig habilita o parsing de formulários com
// enctype="multipart/form-data" (necessário para receber o arquivo
// de imagem enviado por form_item.jsp). Sem esta anotação,
// request.getPart("imagem") lançaria exceção — o container só
// reconhece a requisição como multipart com essa configuração
// explícita. Os limites abaixo são intencionalmente folgados (o
// limite "de verdade" de 3 MB por imagem já é aplicado em
// UploadImagemUtil.salvar() antes de gravar em disco); aqui é só
// uma rede de segurança para não deixar o servidor aceitar uploads
// absurdamente grandes por engano.
@MultipartConfig(
    maxFileSize = 5 * 1024 * 1024,        // 5 MB por arquivo
    maxRequestSize = 6 * 1024 * 1024,     // 6 MB por requisição inteira
    fileSizeThreshold = 1024 * 1024       // acima de 1 MB, grava em disco temporário em vez de manter em memória
)
public class CardapioController extends HttpServlet {

    private static final long serialVersionUID = 1L;

    /* ================================================================
       MÉTODO GET - Roteador de Páginas
       ================================================================

       Decide o que exibir baseado no parâmetro "acao":

       acao=novo    → exibirFormulario() (vazio)   — exige GERENTE
       acao=editar  → exibirFormulario() (preenchido) — exige GERENTE
       (sem acao)   → listar()                      — qualquer logado

       IMPORTANTE: se um não-GERENTE tentar acessar novo/editar,
       é redirecionado de volta para a listagem (sem mensagem de erro
       explícita — apenas silenciosamente barrado).
    */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        System.out.println("\n========== CARDAPIO CONTROLLER GET ==========");
        String acao = request.getParameter("acao");
        System.out.println("📍 Ação solicitada: " + (acao != null ? acao : "(listar)"));

        if ("novo".equals(acao) || "editar".equals(acao)) {
            // ========== VERIFICAR PERMISSÃO (só GERENTE edita cardápio) ==========
            if (!isGerente(request)) {
                System.err.println("❌ Acesso negado: usuário não é GERENTE");
                response.sendRedirect(request.getContextPath() + "/app/cardapio");
                System.out.println("===============================================\n");
                return;
            }
            System.out.println("🔀 Roteando para: exibirFormulario()");
            exibirFormulario(request, response);
        } else {
            System.out.println("🔀 Roteando para: listar()");
            listar(request, response);
        }
        System.out.println("===============================================\n");
    }

    /* ================================================================
       MÉTODO POST - Roteador de Ações
       ================================================================

       Toda ação de escrita (salvar/excluir/disponivel) exige GERENTE.
       A checagem é feita ANTES de olhar qual ação foi pedida — ou
       seja, um não-GERENTE nunca chega nem perto de salvar/excluir.

       acao=salvar     → salvar()
       acao=excluir    → excluir()
       acao=disponivel → alternarDisponibilidade()
    */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        System.out.println("\n========== CARDAPIO CONTROLLER POST ==========");

        // ========== VERIFICAR PERMISSÃO (toda escrita exige GERENTE) ==========
        if (!isGerente(request)) {
            System.err.println("❌ Acesso negado: usuário não é GERENTE");
            response.sendRedirect(request.getContextPath() + "/app/cardapio");
            System.out.println("================================================\n");
            return;
        }

        String acao = request.getParameter("acao");
        System.out.println("📍 Ação solicitada: " + acao);

        switch (acao != null ? acao : "") {
            case "salvar"     -> {
                System.out.println("🔀 Roteando para: salvar()");
                salvar(request, response);
            }
            case "excluir"    -> {
                System.out.println("🔀 Roteando para: excluir()");
                excluir(request, response);
            }
            case "disponivel" -> {
                System.out.println("🔀 Roteando para: alternarDisponibilidade()");
                alternarDisponibilidade(request, response);
            }
            case "criarCategoria" -> {
                System.out.println("🔀 Roteando para: criarCategoria()");
                criarCategoria(request, response);
            }
            case "editarCategoria" -> {
                System.out.println("🔀 Roteando para: editarCategoria()");
                editarCategoria(request, response);
            }
            case "excluirCategoria" -> {
                System.out.println("🔀 Roteando para: excluirCategoria()");
                excluirCategoria(request, response);
            }
            default -> {
                System.err.println("❌ Ação POST desconhecida: " + acao);
                response.sendRedirect(request.getContextPath() + "/app/cardapio");
            }
        }
        System.out.println("================================================\n");
    }

    // ── LISTAR ──────────────────────────────────────────────────────

    /* ================================================================
       LISTAR CARDÁPIO
       ================================================================

       URL: GET /app/cardapio (sem parâmetro "acao")
       Acesso: qualquer usuário logado (GERENTE, FUNCIONARIO ou USUARIO)

       Fluxo:
       1. Busca todas as categorias ativas (para exibir agrupamento/
          filtros na tela)
       2. Busca todos os itens do cardápio
       3. Marca "cardapio" como página ativa (usado no menu lateral)
       4. Recupera e limpa mensagem de sucesso salva na sessão
          (padrão POST-REDIRECT-GET: a mensagem é setada antes do
          redirect e consumida aqui, uma única vez)
       5. Encaminha para a JSP de listagem
    */
    private void listar(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        System.out.println("📋 Iniciando listagem do cardápio");

        try (Connection conn = Conexao.getConnection()) {

            System.out.println("⏳ Buscando categorias...");
            List<CategoriaItem> categorias = new CategoriaItemDAO(conn).listar();
            System.out.println("✅ " + categorias.size() + " categoria(s) encontrada(s)");

            System.out.println("⏳ Buscando itens do cardápio...");
            List<Cardapio> itens = new CardapioDAO(conn).listar();
            System.out.println("✅ " + itens.size() + " item(ns) encontrado(s)");

            request.setAttribute("categorias",  categorias);
            request.setAttribute("itens",       itens);
            request.setAttribute("paginaAtiva", "cardapio");

            // ---- Recupera mensagem de sucesso (fluxo POST-REDIRECT-GET) ----
            String msg = (String) request.getSession().getAttribute("msgSucesso");
            if (msg != null) {
                System.out.println("💬 Mensagem de sucesso encontrada: " + msg);
                request.setAttribute("msgSucesso", msg);
                request.getSession().removeAttribute("msgSucesso");
            }

            System.out.println("➡️ Encaminhando para cardapio.jsp");
            request.getRequestDispatcher("/WEB-INF/views/cardapio/cardapio.jsp")
                   .forward(request, response);

        } catch (Exception e) {
            // ========== TRATAMENTO DE ERRO ==========
            System.err.println("❌ ERRO ao listar cardápio:");
            System.err.println("   Tipo: " + e.getClass().getName());
            System.err.println("   Mensagem: " + e.getMessage());
            e.printStackTrace();
            request.getRequestDispatcher("/WEB-INF/views/error/500.jsp")
                   .forward(request, response);
        }
    }

    // ── FORMULÁRIO ──────────────────────────────────────────────────

    /* ================================================================
       EXIBIR FORMULÁRIO (NOVO ITEM OU EDIÇÃO)
       ================================================================

       URL: GET /app/cardapio?acao=novo
            GET /app/cardapio?acao=editar&id=X
       Acesso: apenas GERENTE (já validado em doGet() antes de chamar)

       Fluxo:
       1. Busca todas as categorias (para popular o <select> do form)
       2. Se veio parâmetro "id" → busca o item existente e disponibiliza
          como atributo "item" (a JSP usa isso para pré-preencher os
          campos — se "id" não vier, o form fica em branco = modo "novo")
       3. Encaminha para form_item.jsp (mesma view serve novo e edição)
    */
    private void exibirFormulario(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        System.out.println("📝 Iniciando exibição do formulário de item");

        try (Connection conn = Conexao.getConnection()) {

            System.out.println("⏳ Buscando categorias para o formulário...");
            request.setAttribute("categorias",  new CategoriaItemDAO(conn).listar());
            request.setAttribute("paginaAtiva", "cardapio");

            String idStr = request.getParameter("id");
            if (idStr != null) {
                System.out.println("⏳ Modo EDIÇÃO — buscando item id=" + idStr);
                Cardapio item = new CardapioDAO(conn).buscarPorId(parseId(idStr));
                request.setAttribute("item", item);
                System.out.println(item != null
                        ? "✅ Item encontrado: " + item.getNome()
                        : "⚠️ Item não encontrado para id=" + idStr);
            } else {
                System.out.println("📄 Modo NOVO — formulário em branco");
            }

            request.getRequestDispatcher("/WEB-INF/views/cardapio/form_item.jsp")
                   .forward(request, response);

        } catch (Exception e) {
            // ========== TRATAMENTO DE ERRO ==========
            System.err.println("❌ ERRO ao exibir formulário:");
            System.err.println("   Tipo: " + e.getClass().getName());
            System.err.println("   Mensagem: " + e.getMessage());
            e.printStackTrace();
            request.getRequestDispatcher("/WEB-INF/views/error/500.jsp")
                   .forward(request, response);
        }
    }

    // ── SALVAR ──────────────────────────────────────────────────────

    /* ================================================================
       SALVAR ITEM (INSERIR OU ATUALIZAR)
       ================================================================

       URL: POST /app/cardapio (acao=salvar)
       Acesso: apenas GERENTE (já validado em doPost())

       Parâmetros esperados:
       - id              (0 ou negativo = novo item; >0 = edição)
       - categoriaId
       - nome
       - descricao       (opcional)
       - preco           (aceita vírgula: "45,00" → convertido para "45.00")
       - tempoPreparoMin

       Fluxo:
       1. Lê e converte os parâmetros do formulário
       2. Se id <= 0:
            → cria um novo Cardapio, marca como disponível e ativo
            → insere no banco
       3. Se id > 0:
            → busca o item existente
            → se encontrado, atualiza os campos e chama dao.editar()
              (disponibilidade e ativo NÃO são alterados aqui — isso
              é feito pelas ações "disponivel" e "excluir" separadamente)
       4. Define mensagem de sucesso/erro na sessão
       5. Redireciona (POST-REDIRECT-GET) de volta para a listagem
    */
    private void salvar(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        System.out.println("💾 Iniciando salvamento de item do cardápio");

        // ========== STEP 1: LER PARÂMETROS ==========
        int    id          = parseId(request.getParameter("id"));
        int    categoriaId = parseId(request.getParameter("categoriaId"));
        String nome        = request.getParameter("nome");
        String descricao   = request.getParameter("descricao");
        String precoStr    = request.getParameter("preco");
        String tempoStr    = request.getParameter("tempoPreparoMin");

        System.out.println("📋 Dados recebidos:");
        System.out.println("   - id: " + id);
        System.out.println("   - categoriaId: " + categoriaId);
        System.out.println("   - nome: " + nome);
        System.out.println("   - preco (raw): " + precoStr);
        System.out.println("   - tempoPreparoMin (raw): " + tempoStr);

        // ---- Imagem (campo opcional do formulário) ----
        // request.getPart(...) só funciona porque a classe está anotada
        // com @MultipartConfig — sem isso, o container nem reconheceria
        // a requisição como multipart/form-data e este método lançaria
        // ServletException. Pegamos o Part aqui fora do try(Connection)
        // porque getPart() é uma operação de I/O própria do container,
        // não do banco — não precisa (nem deveria) da conexão aberta.
        Part parteImagem = null;
        try {
            parteImagem = request.getPart("imagem");
        } catch (Exception e) {
            // Requisição não veio como multipart (não deveria acontecer,
            // já que form_item.jsp sempre usa enctype correto) — segue
            // em frente tratando como "nenhuma imagem enviada".
            System.out.println("   (nenhuma parte 'imagem' na requisição)");
        }
        System.out.println("   - imagem enviada: " + (parteImagem != null && parteImagem.getSize() > 0));

        try (Connection conn = Conexao.getConnection()) {

            // ---- Conversão do preço aceitando vírgula como decimal ----
            BigDecimal preco = new BigDecimal(precoStr.replace(",", "."));
            int tempo        = Integer.parseInt(tempoStr);
            CardapioDAO dao = new CardapioDAO(conn);

            if (id <= 0) {
                // ========== MODO: NOVO ITEM ==========
                System.out.println("⏳ Inserindo novo item...");
                Cardapio novo = new Cardapio();
                novo.setCategoriaId(categoriaId);
                novo.setNome(nome.trim());
                novo.setDescricao(descricao != null ? descricao.trim() : "");
                novo.setPreco(preco);
                novo.setTempoPreparoMin(tempo);
                novo.setDisponivel(true);
                novo.setAtivo(true);

                // Item novo nunca tem imagem antiga para apagar —
                // passa null como segundo argumento.
                String nomeArquivo = UploadImagemUtil.salvar(parteImagem, null);
                novo.setImagem(nomeArquivo); // fica null se nenhuma imagem foi enviada, e tudo bem

                dao.inserir(novo);
                System.out.println("✅ Item inserido com sucesso: " + novo.getNome());
                request.getSession().setAttribute("msgSucesso", "Item adicionado com sucesso!");
            } else {
                // ========== MODO: EDIÇÃO ==========
                System.out.println("⏳ Buscando item existente para edição...");
                Cardapio item = dao.buscarPorId(id);
                if (item != null) {
                    item.setCategoriaId(categoriaId);
                    item.setNome(nome.trim());
                    item.setDescricao(descricao != null ? descricao.trim() : "");
                    item.setPreco(preco);
                    item.setTempoPreparoMin(tempo);

                    // Se uma nova imagem foi enviada, salva e substitui
                    // (apagando a antiga); se o campo veio vazio (usuário
                    // não mexeu na foto), UploadImagemUtil.salvar()
                    // devolve null e item.getImagem() continua com o
                    // valor que já veio de buscarPorId() — ou seja, a
                    // foto atual É PRESERVADA automaticamente, sem
                    // precisar de nenhuma lógica extra aqui.
                    String nomeArquivo = UploadImagemUtil.salvar(parteImagem, item.getImagem());
                    if (nomeArquivo != null) {
                        item.setImagem(nomeArquivo);
                    }

                    dao.editar(item);
                    System.out.println("✅ Item atualizado com sucesso: " + item.getNome());
                } else {
                    System.err.println("⚠️ Item id=" + id + " não encontrado — nada foi atualizado");
                }
                request.getSession().setAttribute("msgSucesso", "Item atualizado com sucesso!");
            }

        } catch (IllegalArgumentException e) {
            // ========== ERRO DE VALIDAÇÃO DA IMAGEM ==========
            // Lançado por UploadImagemUtil.salvar() quando o arquivo não
            // passa nas regras (extensão/tamanho) — mensagem já pronta
            // para o usuário, não precisa de tratamento genérico.
            System.err.println("⚠️ Imagem rejeitada: " + e.getMessage());
            request.getSession().setAttribute("msgSucesso", e.getMessage());
        } catch (Exception e) {
            // ========== TRATAMENTO DE ERRO ==========
            System.err.println("❌ ERRO ao salvar item:");
            System.err.println("   Tipo: " + e.getClass().getName());
            System.err.println("   Mensagem: " + e.getMessage());
            e.printStackTrace();
            request.getSession().setAttribute("msgSucesso", "Erro ao salvar o item.");
        }

        // ========== REDIRECIONAR (POST-REDIRECT-GET) ==========
        response.sendRedirect(request.getContextPath() + "/app/cardapio");
    }

    // ── EXCLUIR ─────────────────────────────────────────────────────

    /* ================================================================
       EXCLUIR ITEM (SOFT DELETE)
       ================================================================

       URL: POST /app/cardapio (acao=excluir)
       Acesso: apenas GERENTE

       Não remove a linha do banco — apenas marca ativo=0, preservando
       o histórico de item_pedido de pedidos já realizados com este item.
    */
    private void excluir(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        int id = parseId(request.getParameter("id"));
        System.out.println("🗑️ Iniciando exclusão (soft delete) do item id=" + id);

        try (Connection conn = Conexao.getConnection()) {
            new CardapioDAO(conn).desativar(id);
            System.out.println("✅ Item id=" + id + " desativado (ativo=0)");
            request.getSession().setAttribute("msgSucesso", "Item removido do cardápio.");
        } catch (Exception e) {
            System.err.println("❌ ERRO ao excluir item id=" + id + ": " + e.getMessage());
            e.printStackTrace();
        }
        response.sendRedirect(request.getContextPath() + "/app/cardapio");
    }

    // ── DISPONIBILIDADE ─────────────────────────────────────────────

    /* ================================================================
       ALTERNAR DISPONIBILIDADE
       ================================================================

       URL: POST /app/cardapio (acao=disponivel)
       Acesso: apenas GERENTE

       Parâmetros:
       - id:    identificador do item
       - valor: "1" = disponível, qualquer outro valor = indisponível

       Usado tipicamente para marcar itens temporariamente esgotados
       sem precisar excluí-los do cardápio.
    */
    private void alternarDisponibilidade(HttpServletRequest request,
                                         HttpServletResponse response) throws IOException {
        int     id   = parseId(request.getParameter("id"));
        boolean disp = "1".equals(request.getParameter("valor"));

        System.out.println("🔄 Alternando disponibilidade do item id=" + id
                + " → " + (disp ? "DISPONÍVEL" : "INDISPONÍVEL"));

        try (Connection conn = Conexao.getConnection()) {
            new CardapioDAO(conn).atualizarDisponibilidade(id, disp);
            System.out.println("✅ Disponibilidade atualizada com sucesso");
        } catch (Exception e) {
            System.err.println("❌ ERRO ao atualizar disponibilidade do item id=" + id
                    + ": " + e.getMessage());
            e.printStackTrace();
        }
        response.sendRedirect(request.getContextPath() + "/app/cardapio");
    }

    /* ================================================================
       CRIAR NOVA CATEGORIA (via AJAX — não recarrega a página)
       ================================================================

       Chamado pelo botão "+ Nova categoria" em form_item.jsp. Ao
       contrário das outras ações deste controller (que sempre
       terminam em response.sendRedirect — padrão PRG), esta responde
       com um pequeno JSON e status HTTP puro, porque é chamada via
       fetch() do JavaScript, não pela submissão de um <form> comum:
       o objetivo é deixar o gerente criar a categoria SEM perder o
       que já tinha preenchido no resto do formulário de item (nome,
       descrição, preço...) — um POST/redirect tradicional recarregaria
       a página inteira e apagaria esses campos.

       RESPOSTA DE SUCESSO (200): {"id":7,"nome":"Vegano","setor":"cozinha"}
       O JavaScript usa "id" e "nome" para adicionar a nova opção no
       <select> de categorias e já deixá-la selecionada.

       RESPOSTA DE ERRO (400): {"erro":"mensagem explicando o problema"}
    */
    private void criarCategoria(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String nome  = request.getParameter("nome");
        String setor = request.getParameter("setor");

        response.setContentType("application/json;charset=UTF-8");

        // ---- Validação básica ----
        if (nome == null || nome.trim().isEmpty()) {
            responderErroJson(response, "Informe um nome para a categoria.");
            return;
        }
        if (setor == null || !(setor.equals("cozinha") || setor.equals("bebida") || setor.equals("sobremesa"))) {
            responderErroJson(response, "Setor inválido.");
            return;
        }

        System.out.println("➕ Criando nova categoria: " + nome + " (" + setor + ")");

        try (Connection conn = Conexao.getConnection()) {
            CategoriaItem nova = new CategoriaItem(0, nome.trim(), setor, true);
            new CategoriaItemDAO(conn).inserir(nova);
            System.out.println("✅ Categoria criada com id=" + nova.getIdCategoria());

            String json = "{\"id\":" + nova.getIdCategoria()
                    + ",\"nome\":\"" + escaparJson(nova.getNome()) + "\""
                    + ",\"setor\":\"" + nova.getSetor() + "\"}";
            response.getWriter().write(json);

        } catch (Exception e) {
            System.err.println("❌ ERRO ao criar categoria: " + e.getMessage());
            e.printStackTrace();
            responderErroJson(response, "Erro ao salvar a categoria.");
        }
    }

    /* ================================================================
       EDITAR CATEGORIA (renomear e/ou trocar setor) — via AJAX
       ================================================================

       Mesmo padrão de criarCategoria(): responde JSON, não redireciona,
       porque é chamado pelo modal "Gerenciar Categorias" de
       cardapio.jsp, que atualiza a linha da categoria na tela sem
       recarregar a página inteira.

       RESPOSTA DE SUCESSO (200): {"id":7,"nome":"Vegano","setor":"cozinha"}
       RESPOSTA DE ERRO (400):    {"erro":"mensagem"}
    */
    private void editarCategoria(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        int    id    = parseId(request.getParameter("id"));
        String nome  = request.getParameter("nome");
        String setor = request.getParameter("setor");

        response.setContentType("application/json;charset=UTF-8");

        // ---- Validação básica ----
        if (id <= 0) {
            responderErroJson(response, "Categoria inválida.");
            return;
        }
        if (nome == null || nome.trim().isEmpty()) {
            responderErroJson(response, "Informe um nome para a categoria.");
            return;
        }
        if (setor == null || !(setor.equals("cozinha") || setor.equals("bebida") || setor.equals("sobremesa"))) {
            responderErroJson(response, "Setor inválido.");
            return;
        }

        System.out.println("✏️ Editando categoria id=" + id + " → nome=" + nome + ", setor=" + setor);

        try (Connection conn = Conexao.getConnection()) {
            CategoriaItemDAO dao = new CategoriaItemDAO(conn);
            CategoriaItem existente = dao.buscarPorId(id);
            if (existente == null) {
                responderErroJson(response, "Categoria não encontrada (pode já ter sido excluída).");
                return;
            }

            existente.setNome(nome.trim());
            existente.setSetor(setor);
            dao.editar(existente);
            System.out.println("✅ Categoria id=" + id + " atualizada com sucesso");

            String json = "{\"id\":" + existente.getIdCategoria()
                    + ",\"nome\":\"" + escaparJson(existente.getNome()) + "\""
                    + ",\"setor\":\"" + existente.getSetor() + "\"}";
            response.getWriter().write(json);

        } catch (Exception e) {
            System.err.println("❌ ERRO ao editar categoria id=" + id + ": " + e.getMessage());
            e.printStackTrace();
            responderErroJson(response, "Erro ao atualizar a categoria.");
        }
    }

    /* ================================================================
       EXCLUIR CATEGORIA (soft delete) — via AJAX
       ================================================================

       Assim como TODO "excluir" neste sistema (itens do cardápio,
       mesas, etc.), isto NÃO é um DELETE físico — é
       CategoriaItemDAO.desativar(), que só marca ativo=0. Duas razões
       técnicas para isso, não só de padrão de projeto:

       1. A coluna cardapio.categoria_id tem uma FOREIGN KEY com
          ON DELETE RESTRICT (ver schema) — um DELETE físico numa
          categoria que ainda tem itens de cardápio vinculados seria
          REJEITADO pelo próprio banco com erro de violação de chave
          estrangeira. Soft delete não esbarra nisso.
       2. Mesmo que a categoria não tivesse nenhum item vinculado NO
          MOMENTO da exclusão, um DELETE físico apagaria o registro de
          qualquer jeito — mas pedidos ANTIGOS que já usaram itens
          dessa categoria continuam existindo no histórico
          (item_pedido → cardapio → categoria_item), e queremos que
          relatórios antigos continuem "fazendo sentido" mesmo depois.

       ⚠️ IMPORTANTE — o que isso NÃO faz: desativar uma categoria não
       desativa em cascata os itens de cardápio que pertencem a ela.
       Os itens continuam ativos e aparecendo no cardápio normalmente
       — só a categoria em si some da lista de opções ao criar/editar
       um item novo. Isso já era um comportamento documentado no
       CategoriaItemDAO original; só estou tornando explícito aqui
       porque agora existe uma ação de UI que dispara isso.

       RESPOSTA DE SUCESSO (200): {"ok":true}
       RESPOSTA DE ERRO (400):    {"erro":"mensagem"}
    */
    private void excluirCategoria(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        int id = parseId(request.getParameter("id"));
        response.setContentType("application/json;charset=UTF-8");

        if (id <= 0) {
            responderErroJson(response, "Categoria inválida.");
            return;
        }

        System.out.println("🗑️ Desativando categoria id=" + id);

        try (Connection conn = Conexao.getConnection()) {
            new CategoriaItemDAO(conn).desativar(id);
            System.out.println("✅ Categoria id=" + id + " desativada com sucesso");
            response.getWriter().write("{\"ok\":true}");

        } catch (Exception e) {
            System.err.println("❌ ERRO ao excluir categoria id=" + id + ": " + e.getMessage());
            e.printStackTrace();
            responderErroJson(response, "Erro ao excluir a categoria.");
        }
    }

    /** Escreve uma resposta JSON de erro com status HTTP 400. */
    private void responderErroJson(HttpServletResponse response, String mensagem) throws IOException {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.getWriter().write("{\"erro\":\"" + escaparJson(mensagem) + "\"}");
    }

    /**
     * Escapa aspas duplas e barras invertidas antes de colocar um
     * texto dentro de uma string JSON montada manualmente — este
     * controller não usa nenhuma lib de JSON (Jackson/Gson) porque a
     * resposta é simples o suficiente para não justificar a
     * dependência extra, mas isso significa que a montagem manual
     * PRECISA escapar caracteres especiais, senão um nome de
     * categoria com aspas (ex: Prato "Especial") quebraria o JSON
     * gerado.
     */
    private String escaparJson(String texto) {
        return texto.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /**
     * Verifica se o usuário logado na sessão tem perfil GERENTE.
     * Usado para proteger todas as operações de escrita do cardápio.
     */
    private boolean isGerente(HttpServletRequest request) {
        Usuario u = (Usuario) request.getSession().getAttribute("usuarioLogado");
        return u != null && "GERENTE".equals(u.getPerfil());
    }

    /**
     * Converte uma string de parâmetro para int de forma segura.
     * Retorna -1 caso a conversão falhe (parâmetro nulo, vazio ou
     * não numérico) — convenção usada em todo o controller para
     * indicar "id inválido / não informado".
     */
    private int parseId(String v) {
        try { return Integer.parseInt(v); } catch (Exception e) { return -1; }
    }
}

/* ================================================================
   RESUMO DO CONTROLLER
   ================================================================

   ROTA ÚNICA: /app/cardapio (roteada internamente pelo parâmetro "acao")

   AÇÕES MAPEADAS:
   1. GET  (sem acao)          → listar()                  — qualquer logado
   2. GET  acao=novo           → exibirFormulario() (vazio) — só GERENTE
   3. GET  acao=editar&id=X    → exibirFormulario() (cheio) — só GERENTE
   4. POST acao=salvar         → salvar()                  — só GERENTE
   5. POST acao=excluir        → excluir()                 — só GERENTE
   6. POST acao=disponivel     → alternarDisponibilidade()  — só GERENTE

   CAMPOS DO FORMULÁRIO (novo/editar):
   - id              (oculto, 0 = novo)
   - categoriaId*
   - nome*
   - descricao
   - preco*          (aceita vírgula como decimal)
   - tempoPreparoMin*

   PERMISSÕES:
   ✅ Leitura (listagem) liberada para qualquer perfil logado
   ✅ Toda escrita (novo/editar/excluir/disponibilidade) exige GERENTE
   ✅ Checagem feita tanto no doGet() quanto no doPost(), nunca
      delegada só à view

   SOFT DELETE:
   ✅ excluir() nunca faz DELETE físico — apenas ativo=0, preservando
      o vínculo histórico com item_pedido de pedidos antigos

   PADRÃO POST-REDIRECT-GET:
   ✅ Toda ação de escrita termina em sendRedirect() para /app/cardapio
   ✅ Mensagens de sucesso/erro trafegam via sessão e são consumidas
      (removidas) na primeira listagem seguinte

   DEPENDÊNCIAS:
   - CardapioDAO: acesso à tabela cardapio
   - CategoriaItemDAO: acesso à tabela categoria_item
   - Cardapio / CategoriaItem: models
   - Conexao: gerenciamento de conexões

   OBSERVAÇÕES:
   - Conexões fecham automaticamente (try-with-resources)
   - Logs detalhados em cada etapa (System.out / System.err)
   ================================================================ */
