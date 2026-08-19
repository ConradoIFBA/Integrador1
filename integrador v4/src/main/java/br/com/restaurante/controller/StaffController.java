package br.com.restaurante.controller;

import java.io.IOException;
import java.sql.Connection;
import java.util.List;

import org.mindrot.jbcrypt.BCrypt;

import br.com.restaurante.dao.UsuarioDAO;
import br.com.restaurante.model.Usuario;
import br.com.restaurante.utils.Conexao;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * ================================================================
 * STAFF CONTROLLER — Gerenciamento de Funcionários/Gerentes
 * ================================================================
 *
 * NOTA SOBRE O NOME "STAFF" (em vez de "Usuario"):
 * O model/tabela do banco continua se chamando "usuario" (é a
 * entidade real, comum a TODOS os perfis — inclusive clientes). Este
 * Controller foi renomeado para "Staff" porque ele NÃO gerencia
 * usuários em geral, só as contas de GERENTE/FUNCIONARIO — chamá-lo
 * de "UsuarioController" confundia com o perfil USUARIO (cliente) do
 * enum, que é uma coisa completamente diferente. UsuarioDAO e o
 * model Usuario continuam com o nome original — eles representam a
 * tabela em si, que é genuína e corretamente compartilhada por todos
 * os perfis.
 *
 * PROPÓSITO:
 * Tela administrativa para o GERENTE cadastrar novas contas de
 * FUNCIONARIO ou GERENTE — algo que antes só existia como
 * autocadastro público (sempre perfil=USUARIO, ver
 * AuthController.processarCadastro()). Isso preenche uma lacuna real
 * do documento de requisitos (RF05: "cadastro de usuários do sistema,
 * associando-os a um perfil de acesso").
 *
 * POR QUE UM CONTROLLER SEPARADO (em vez de estender AuthController):
 * AuthController cuida de AUTENTICAÇÃO (quem sou eu, entrar/sair) —
 * é uma área PÚBLICA, sem exigir login. Esta tela é o oposto: exige
 * estar logado como GERENTE, e é sobre GESTÃO de outras contas, não
 * sobre a própria sessão de quem está usando. Misturar os dois no
 * mesmo controller misturaria também duas responsabilidades e dois
 * níveis de proteção bem diferentes.
 *
 * ROTA MAPEADA: /app/staff
 * GET  (sem acao)        → listar()       — lista funcionários/gerentes ativos
 * GET  acao=novo         → exibirNovo()   — formulário de cadastro
 * POST acao=salvar       → salvar()       — cria a conta
 * POST acao=desativar    → desativar()    — soft delete (com proteções)
 *
 * PERMISSÕES:
 * ✅ TODA rota (GET e POST) exige perfil GERENTE — nem FUNCIONARIO
 *    pode cadastrar outros funcionários, e USUARIO nem chega perto.
 *
 * REGRAS DE SEGURANÇA ESPECÍFICAS DESTA TELA:
 * ✅ O perfil da nova conta só pode ser GERENTE ou FUNCIONARIO —
 *    nunca USUARIO (isso continua sendo só autocadastro público) e
 *    nunca um valor arbitrário vindo do formulário sem validação.
 * ✅ Um gerente NÃO pode desativar a própria conta por aqui (evita
 *    auto-bloqueio acidental).
 * ✅ Não é possível desativar o ÚLTIMO gerente ativo do sistema
 *    (evita o sistema inteiro ficar sem ninguém com acesso a
 *    relatórios/gestão).
 *
 * @author Sistema Integrador
 * @version 1.0
 * @see br.com.restaurante.controller.AuthController
 * @see br.com.restaurante.dao.UsuarioDAO
 */
@WebServlet("/app/staff")
public class StaffController extends HttpServlet {

    private static final long serialVersionUID = 1L;

    // ── GET ─────────────────────────────────────────────────────────

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        System.out.println("\n========== USUARIO CONTROLLER GET ==========");

        if (!isGerente(request)) {
            System.err.println("❌ Acesso negado: usuário não é GERENTE");
            response.sendRedirect(request.getContextPath() + "/app/mesas");
            System.out.println("===============================================\n");
            return;
        }

        String acao = request.getParameter("acao");
        System.out.println("📍 Ação solicitada: " + (acao != null ? acao : "(listar)"));

        if ("novo".equals(acao)) {
            exibirNovo(request, response);
        } else {
            listar(request, response);
        }
        System.out.println("===============================================\n");
    }

    // ── POST ────────────────────────────────────────────────────────

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        System.out.println("\n========== USUARIO CONTROLLER POST ==========");

        if (!isGerente(request)) {
            System.err.println("❌ Acesso negado: usuário não é GERENTE");
            response.sendRedirect(request.getContextPath() + "/app/mesas");
            System.out.println("================================================\n");
            return;
        }

        String acao = request.getParameter("acao");
        System.out.println("📍 Ação solicitada: " + acao);

        switch (acao != null ? acao : "") {
            case "salvar" -> salvar(request, response);
            case "desativar" -> desativar(request, response);
            default -> {
                System.err.println("❌ Ação POST desconhecida: " + acao);
                response.sendRedirect(request.getContextPath() + "/app/staff");
            }
        }
        System.out.println("================================================\n");
    }

    /* ================================================================
       LISTAR FUNCIONÁRIOS/GERENTES
       ================================================================
       URL: GET /app/staff
    */
    private void listar(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        System.out.println("📋 Listando funcionários/gerentes ativos");

        try (Connection conn = Conexao.getConnection()) {
            List<Usuario> staff = new UsuarioDAO(conn).listarStaff();
            System.out.println("✅ " + staff.size() + " conta(s) encontrada(s)");

            request.setAttribute("staff", staff);
            request.setAttribute("msgSucesso", request.getSession().getAttribute("msgSucesso"));
            request.setAttribute("msgErro",    request.getSession().getAttribute("msgErro"));
            request.getSession().removeAttribute("msgSucesso");
            request.getSession().removeAttribute("msgErro");
            request.setAttribute("paginaAtiva", "staff");

            request.getRequestDispatcher("/WEB-INF/views/staff/staff.jsp").forward(request, response);

        } catch (Exception e) {
            System.err.println("❌ ERRO ao listar staff: " + e.getMessage());
            e.printStackTrace();
            request.getRequestDispatcher("/WEB-INF/views/error/500.jsp").forward(request, response);
        }
    }

    /* ================================================================
       EXIBIR FORMULÁRIO DE NOVA CONTA
       ================================================================
       URL: GET /app/staff?acao=novo
    */
    private void exibirNovo(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        System.out.println("📝 Exibindo formulário de nova conta");
        request.setAttribute("paginaAtiva", "staff");
        request.getRequestDispatcher("/WEB-INF/views/staff/form_staff.jsp").forward(request, response);
    }

    /* ================================================================
       SALVAR NOVA CONTA (GERENTE ou FUNCIONARIO)
       ================================================================
       URL: POST /app/staff (acao=salvar)
       nome, login, senha, confirmarSenha, perfil, funcao (opcional)

       Validações — mesmo padrão de AuthController.processarCadastro(),
       com UMA validação a mais que o cadastro público não precisa:
       o PERFIL só pode ser GERENTE ou FUNCIONARIO (nunca aceita
       qualquer outro valor vindo do formulário, mesmo que alguém
       tente manipular a requisição diretamente).
    */
    private void salvar(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String nome      = request.getParameter("nome");
        String login     = request.getParameter("login");
        String senha     = request.getParameter("senha");
        String confirmar = request.getParameter("confirmarSenha");
        String perfil    = request.getParameter("perfil");
        String funcao    = request.getParameter("funcao"); // só relevante se perfil=FUNCIONARIO

        System.out.println("📋 Dados recebidos: nome=" + nome + ", login=" + login
                + ", perfil=" + perfil + ", funcao=" + funcao);

        // ---- Validações básicas (mesmo padrão do cadastro público) ----
        if (nome == null || nome.isBlank()) {
            reexibirComErro(request, response, "Nome obrigatório.");
            return;
        }
        if (login == null || login.trim().length() < 3) {
            reexibirComErro(request, response, "Login deve ter no mínimo 3 caracteres.");
            return;
        }
        if (senha == null || senha.length() < 6) {
            reexibirComErro(request, response, "Senha deve ter no mínimo 6 caracteres.");
            return;
        }
        if (!senha.equals(confirmar)) {
            reexibirComErro(request, response, "As senhas não coincidem.");
            return;
        }
        // ---- Validação de perfil: SÓ GERENTE ou FUNCIONARIO ----
        // Esta é a validação que não existe no cadastro público (que
        // força USUARIO sempre) — aqui o valor VEM do formulário, então
        // precisa ser conferido explicitamente contra uma lista fechada,
        // nunca confiado direto.
        if (!"GERENTE".equals(perfil) && !"FUNCIONARIO".equals(perfil)) {
            reexibirComErro(request, response, "Perfil inválido.");
            return;
        }
        // funcao só faz sentido para FUNCIONARIO; para GERENTE, ignora
        // qualquer valor que porventura tenha vindo e grava null.
        if (!"FUNCIONARIO".equals(perfil)) {
            funcao = null;
        } else if (funcao != null && !funcao.equals("atendente") && !funcao.equals("cozinha")) {
            funcao = null; // valor inesperado — melhor null que um dado inválido
        }

        try (Connection conn = Conexao.getConnection()) {
            UsuarioDAO dao = new UsuarioDAO(conn);

            if (dao.buscarPorLogin(login.trim()) != null) {
                reexibirComErro(request, response, "Login já está em uso.");
                return;
            }

            String senhaHash = BCrypt.hashpw(senha, BCrypt.gensalt());

            Usuario novo = new Usuario();
            novo.setNome(nome.trim());
            novo.setLogin(login.trim().toLowerCase());
            novo.setSenha(senhaHash);
            novo.setPerfil(perfil);
            novo.setFuncao(funcao);
            novo.setAtivo(true);

            dao.inserir(novo);
            System.out.println("✅ Conta criada: id=" + novo.getIdUsuario()
                    + ", login=" + novo.getLogin() + ", perfil=" + novo.getPerfil());

            request.getSession().setAttribute("msgSucesso",
                    "Conta de " + (("GERENTE".equals(perfil)) ? "gerente" : "funcionário")
                    + " criada com sucesso: " + novo.getLogin());

        } catch (Exception e) {
            System.err.println("❌ ERRO ao salvar conta: " + e.getMessage());
            e.printStackTrace();
            request.getSession().setAttribute("msgErro", "Erro ao criar a conta.");
        }

        response.sendRedirect(request.getContextPath() + "/app/staff");
    }

    /* ================================================================
       DESATIVAR CONTA (SOFT DELETE, com proteções)
       ================================================================
       URL: POST /app/staff (acao=desativar&id=X)
    */
    private void desativar(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        int id = parseId(request.getParameter("id"));
        Usuario logado = (Usuario) request.getSession().getAttribute("usuarioLogado");

        System.out.println("🗑️ Desativando conta id=" + id);

        if (id <= 0) {
            request.getSession().setAttribute("msgErro", "Conta inválida.");
            response.sendRedirect(request.getContextPath() + "/app/staff");
            return;
        }

        // ---- Proteção 1: não desativar a própria conta ----
        if (logado != null && logado.getIdUsuario() == id) {
            System.err.println("⚠️ Tentativa de auto-desativação bloqueada (id=" + id + ")");
            request.getSession().setAttribute("msgErro", "Você não pode desativar sua própria conta.");
            response.sendRedirect(request.getContextPath() + "/app/staff");
            return;
        }

        try (Connection conn = Conexao.getConnection()) {
            UsuarioDAO dao = new UsuarioDAO(conn);
            Usuario alvo = dao.buscarPorId(id);
            if (alvo == null) {
                request.getSession().setAttribute("msgErro", "Conta não encontrada.");
                response.sendRedirect(request.getContextPath() + "/app/staff");
                return;
            }

            // ---- Proteção 2: não desativar o último gerente ativo ----
            if ("GERENTE".equals(alvo.getPerfil())) {
                long qtdGerentes = dao.listarStaff().stream()
                        .filter(u -> "GERENTE".equals(u.getPerfil())).count();
                if (qtdGerentes <= 1) {
                    System.err.println("⚠️ Tentativa de desativar o último gerente bloqueada");
                    request.getSession().setAttribute("msgErro",
                        "Não é possível desativar o único gerente ativo do sistema.");
                    response.sendRedirect(request.getContextPath() + "/app/staff");
                    return;
                }
            }

            dao.desativar(id);
            System.out.println("✅ Conta id=" + id + " desativada");
            request.getSession().setAttribute("msgSucesso", "Conta desativada com sucesso.");

        } catch (Exception e) {
            System.err.println("❌ ERRO ao desativar conta id=" + id + ": " + e.getMessage());
            e.printStackTrace();
            request.getSession().setAttribute("msgErro", "Erro ao desativar a conta.");
        }

        response.sendRedirect(request.getContextPath() + "/app/staff");
    }

    // ── HELPERS ─────────────────────────────────────────────────────

    /** Reexibe o formulário de nova conta com uma mensagem de erro, preservando os dados já digitados. */
    private void reexibirComErro(HttpServletRequest request, HttpServletResponse response, String erro)
            throws ServletException, IOException {
        System.err.println("❌ " + erro);
        request.setAttribute("erro", erro);
        request.setAttribute("paginaAtiva", "staff");
        request.getRequestDispatcher("/WEB-INF/views/staff/form_staff.jsp").forward(request, response);
    }

    private boolean isGerente(HttpServletRequest request) {
        Usuario u = (Usuario) request.getSession().getAttribute("usuarioLogado");
        return u != null && "GERENTE".equals(u.getPerfil());
    }

    private int parseId(String v) {
        try { return Integer.parseInt(v); } catch (Exception e) { return -1; }
    }
}
