package br.com.restaurante.controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.List;

import br.com.restaurante.dao.CategoriaItemDAO;
import br.com.restaurante.dao.ItemCardapioDAO;
import br.com.restaurante.model.CategoriaItem;
import br.com.restaurante.model.ItemCardapio;
import br.com.restaurante.model.Usuario;
import br.com.restaurante.utils.Conexao;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Rota: /app/cardapio
 *
 * GET  sem acao          → lista cardápio
 * GET  acao=novo         → formulário de novo item   (só GERENTE)
 * GET  acao=editar&id=X  → formulário preenchido     (só GERENTE)
 * POST acao=salvar       → insere ou atualiza        (só GERENTE)
 * POST acao=excluir      → soft delete               (só GERENTE)
 * POST acao=disponivel   → alterna disponibilidade   (só GERENTE)
 */
@WebServlet("/app/cardapio")
public class CardapioController extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String acao = request.getParameter("acao");

        if ("novo".equals(acao) || "editar".equals(acao)) {
            if (!isGerente(request)) {
                response.sendRedirect(request.getContextPath() + "/app/cardapio");
                return;
            }
            exibirFormulario(request, response);
        } else {
            listar(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!isGerente(request)) {
            response.sendRedirect(request.getContextPath() + "/app/cardapio");
            return;
        }

        String acao = request.getParameter("acao");
        switch (acao != null ? acao : "") {
            case "salvar"     -> salvar(request, response);
            case "excluir"    -> excluir(request, response);
            case "disponivel" -> alternarDisponibilidade(request, response);
            default -> response.sendRedirect(request.getContextPath() + "/app/cardapio");
        }
    }

    // ── LISTAR ──────────────────────────────────────────────────────

    private void listar(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        try (Connection conn = Conexao.getConnection()) {
            request.setAttribute("categorias",  new CategoriaItemDAO(conn).listar());
            request.setAttribute("itens",       new ItemCardapioDAO(conn).listar());
            request.setAttribute("paginaAtiva", "cardapio");

            String msg = (String) request.getSession().getAttribute("msgSucesso");
            if (msg != null) {
                request.setAttribute("msgSucesso", msg);
                request.getSession().removeAttribute("msgSucesso");
            }

            request.getRequestDispatcher("/WEB-INF/views/cardapio/cardapio.jsp")
                   .forward(request, response);

        } catch (Exception e) {
            e.printStackTrace();
            request.getRequestDispatcher("/WEB-INF/views/error/500.jsp")
                   .forward(request, response);
        }
    }

    // ── FORMULÁRIO ──────────────────────────────────────────────────

    private void exibirFormulario(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        try (Connection conn = Conexao.getConnection()) {

            request.setAttribute("categorias",  new CategoriaItemDAO(conn).listar());
            request.setAttribute("paginaAtiva", "cardapio");

            String idStr = request.getParameter("id");
            if (idStr != null) {
                request.setAttribute("item",
                    new ItemCardapioDAO(conn).buscarPorId(parseId(idStr)));
            }

            request.getRequestDispatcher("/WEB-INF/views/cardapio/form_item.jsp")
                   .forward(request, response);

        } catch (Exception e) {
            e.printStackTrace();
            request.getRequestDispatcher("/WEB-INF/views/error/500.jsp")
                   .forward(request, response);
        }
    }

    // ── SALVAR ──────────────────────────────────────────────────────

    private void salvar(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        int    id          = parseId(request.getParameter("id"));
        int    categoriaId = parseId(request.getParameter("categoriaId"));
        String nome        = request.getParameter("nome");
        String descricao   = request.getParameter("descricao");
        String precoStr    = request.getParameter("preco");
        String tempoStr    = request.getParameter("tempoPreparoMin");

        try (Connection conn = Conexao.getConnection()) {

            BigDecimal preco = new BigDecimal(precoStr.replace(",", "."));
            int tempo        = Integer.parseInt(tempoStr);
            ItemCardapioDAO dao = new ItemCardapioDAO(conn);

            if (id <= 0) {
                ItemCardapio novo = new ItemCardapio();
                novo.setCategoriaId(categoriaId);
                novo.setNome(nome.trim());
                novo.setDescricao(descricao != null ? descricao.trim() : "");
                novo.setPreco(preco);
                novo.setTempoPreparoMin(tempo);
                novo.setDisponivel(true);
                novo.setAtivo(true);
                dao.inserir(novo);
                request.getSession().setAttribute("msgSucesso", "Item adicionado com sucesso!");
            } else {
                ItemCardapio item = dao.buscarPorId(id);
                if (item != null) {
                    item.setCategoriaId(categoriaId);
                    item.setNome(nome.trim());
                    item.setDescricao(descricao != null ? descricao.trim() : "");
                    item.setPreco(preco);
                    item.setTempoPreparoMin(tempo);
                    dao.editar(item);
                }
                request.getSession().setAttribute("msgSucesso", "Item atualizado com sucesso!");
            }

        } catch (Exception e) {
            e.printStackTrace();
            request.getSession().setAttribute("msgSucesso", "Erro ao salvar o item.");
        }

        response.sendRedirect(request.getContextPath() + "/app/cardapio");
    }

    // ── EXCLUIR ─────────────────────────────────────────────────────

    private void excluir(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        int id = parseId(request.getParameter("id"));
        try (Connection conn = Conexao.getConnection()) {
            new ItemCardapioDAO(conn).desativar(id);
            request.getSession().setAttribute("msgSucesso", "Item removido do cardápio.");
        } catch (Exception e) {
            e.printStackTrace();
        }
        response.sendRedirect(request.getContextPath() + "/app/cardapio");
    }

    // ── DISPONIBILIDADE ─────────────────────────────────────────────

    private void alternarDisponibilidade(HttpServletRequest request,
                                         HttpServletResponse response) throws IOException {
        int     id   = parseId(request.getParameter("id"));
        boolean disp = "1".equals(request.getParameter("valor"));
        try (Connection conn = Conexao.getConnection()) {
            new ItemCardapioDAO(conn).atualizarDisponibilidade(id, disp);
        } catch (Exception e) {
            e.printStackTrace();
        }
        response.sendRedirect(request.getContextPath() + "/app/cardapio");
    }

    // ── HELPERS ─────────────────────────────────────────────────────

    private boolean isGerente(HttpServletRequest request) {
        Usuario u = (Usuario) request.getSession().getAttribute("usuarioLogado");
        return u != null && "GERENTE".equals(u.getPerfil());
    }

    private int parseId(String v) {
        try { return Integer.parseInt(v); } catch (Exception e) { return -1; }
    }
}
