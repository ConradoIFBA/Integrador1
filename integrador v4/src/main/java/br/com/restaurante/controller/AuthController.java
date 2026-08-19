package br.com.restaurante.controller;

import java.io.IOException;
import java.sql.Connection;
import org.mindrot.jbcrypt.BCrypt;
import br.com.restaurante.dao.UsuarioDAO;
import br.com.restaurante.model.Usuario;
import br.com.restaurante.utils.Conexao;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

/**
 * ================================================================
 * AUTH CONTROLLER - Autenticação, Cadastro e Redirecionamento
 * ================================================================
 *
 * PROPÓSITO:
 * Gerencia todo o processo de autenticação do sistema Integrador.
 * Unifica Login, Cadastro e Logout em um único controller, e decide
 * para onde cada perfil de usuário deve ser enviado após entrar
 * no sistema.
 *
 * FUNCIONALIDADES:
 * 1. Login de usuários (autenticação com login + senha)
 * 2. Cadastro de novos usuários (sempre como perfil USUARIO/cliente)
 * 3. Logout (encerramento de sessão)
 * 4. Redirecionamento por perfil (GERENTE / FUNCIONARIO / USUARIO)
 *
 * ROTAS MAPEADAS (todas registradas no mesmo @WebServlet):
 * - GET  /auth/login     → Exibe formulário de login
 * - POST /auth/login     → Processa autenticação
 * - GET  /auth/cadastro  → Exibe formulário de cadastro
 * - POST /auth/cadastro  → Processa novo usuário (cliente)
 * - GET  /auth/logout    → Encerra sessão e redireciona para login
 *
 * TABELA: usuario
 * Schema (ver integrador_v2.sql):
 * - id_usuario (PK, AUTO_INCREMENT)
 * - nome       (NOT NULL)
 * - login      (UNIQUE, NOT NULL)      → usado como username
 * - senha      (VARCHAR 255, hash BCrypt)
 * - perfil     (ENUM: GERENTE, FUNCIONARIO, USUARIO)
 * - funcao     (ENUM: atendente, cozinha — só relevante p/ FUNCIONARIO)
 * - ativo      (TINYINT(1), default 1)
 *
 * SEGURANÇA:
 * ✅ Senhas criptografadas com BCrypt
 * ✅ Validação de login único (verificada antes de inserir)
 * ✅ Sessões com timeout de 1800s (30 minutos)
 * ✅ PreparedStatement para prevenir SQL injection (via DAO)
 * ✅ Mensagem genérica de erro no login (não revela se o login existe)
 *
 * FLUXO DE LOGIN:
 * 1. Usuário informa login + senha
 * 2. Valida se os campos não estão em branco
 * 3. Busca usuário no banco por login (UsuarioDAO)
 * 4. Valida senha com BCrypt.checkpw()
 * 5. Se OK: cria sessão e redireciona conforme o perfil (destino())
 * 6. Se ERRO: exibe mensagem genérica e volta para login.jsp
 *
 * FLUXO DE CADASTRO:
 * 1. Valida nome, login (mín. 3 caracteres), senha (mín. 6) e confirmação
 * 2. Verifica se o login já está em uso
 * 3. Gera hash BCrypt da senha
 * 4. Cria o usuário sempre com perfil USUARIO (cliente do app)
 * 5. Insere no banco via UsuarioDAO
 * 6. Redireciona para /auth/login com mensagem de sucesso
 *
 * REGRA DE REDIRECIONAMENTO POR PERFIL (v3):
 * - GERENTE     → /app/dashboard
 * - FUNCIONARIO → /app/mesas   (unificado: atendente e cozinha vão
 *                                para o mesmo lugar, sem distinção
 *                                pela coluna "funcao")
 * - USUARIO     → /app/cardapio (tela inicial do cliente)
 *
 * EXEMPLO DE USO:
 * ```
 * // Login:
 * POST /auth/login
 * login=funcionario&senha=minhaSenha123
 *
 * // Cadastro:
 * POST /auth/cadastro
 * nome=João Silva&login=joaosilva&senha=senha123&confirmarSenha=senha123
 *
 * // Logout:
 * GET /auth/logout
 * ```
 *
 * @author Sistema Integrador
 * @version 3.0 - Redirecionamento simplificado por perfil
 * @see UsuarioDAO
 * @see Usuario
 */
@WebServlet({"/auth/login","/auth/cadastro","/auth/logout"})
public class AuthController extends HttpServlet {

    private static final long serialVersionUID = 1L;

    /* ================================================================
       MÉTODO GET - Roteador de Páginas
       ================================================================

       Decide qual página exibir baseado na rota acessada:

       /auth/login    → exibirLogin()    → login.jsp
       /auth/cadastro → exibirCadastro() → cadastro.jsp
       /auth/logout   → logout()         → redireciona para /auth/login

       IMPORTANTE: Se o usuário já está logado e tenta acessar
       /auth/login ou /auth/cadastro, é redirecionado direto para
       a tela correspondente ao seu perfil (via destino()).
    */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        // ========== IDENTIFICAR ROTA ==========
        System.out.println("\n========== AUTH CONTROLLER GET ==========");
        System.out.println("📍 Rota acessada: " + req.getServletPath());

        // ========== ROTEAMENTO ==========
        switch (req.getServletPath()) {
            case "/auth/login"    -> {
                System.out.println("🔀 Roteando para: exibirLogin()");
                exibirLogin(req, res);
            }
            case "/auth/cadastro" -> {
                System.out.println("🔀 Roteando para: exibirCadastro()");
                exibirCadastro(req, res);
            }
            case "/auth/logout"   -> {
                System.out.println("🔀 Roteando para: logout()");
                logout(req, res);
            }
            default               -> {
                System.err.println("❌ Rota GET desconhecida: " + req.getServletPath());
                res.sendError(404);
            }
        }
        System.out.println("==========================================\n");
    }

    /* ================================================================
       MÉTODO POST - Roteador de Ações
       ================================================================

       Processa dados enviados por formulários:

       POST /auth/login    → processarLogin()    → autentica usuário
       POST /auth/cadastro → processarCadastro() → cria novo usuário

       NOTA: Logout não usa POST, apenas GET (é uma ação idempotente
       de leitura simples — invalida sessão e redireciona).
    */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        // ========== IDENTIFICAR ROTA ==========
        System.out.println("\n========== AUTH CONTROLLER POST ==========");
        System.out.println("📍 Rota acessada: " + req.getServletPath());

        // ========== ROTEAMENTO ==========
        switch (req.getServletPath()) {
            case "/auth/login"    -> {
                System.out.println("🔀 Roteando para: processarLogin()");
                processarLogin(req, res);
            }
            case "/auth/cadastro" -> {
                System.out.println("🔀 Roteando para: processarCadastro()");
                processarCadastro(req, res);
            }
            default               -> {
                System.err.println("❌ Rota POST desconhecida: " + req.getServletPath());
                res.sendError(404);
            }
        }
        System.out.println("===========================================\n");
    }

    /* ================================================================
       ROTA 1: EXIBIR FORMULÁRIO DE LOGIN
       ================================================================

       URL: GET /auth/login

       Comportamento:
       - Se usuário JÁ está logado → redireciona para a tela do seu
         perfil (calculada por destino())
       - Se usuário NÃO está logado → exibe login.jsp

       JSP: /WEB-INF/views/auth/login.jsp

       Sessão: Verifica atributo "usuarioLogado"
    */
    private void exibirLogin(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        System.out.println("📝 Iniciando exibição de LOGIN");

        // ========== VERIFICAR SE JÁ ESTÁ LOGADO ==========
        // getSession(false) → não cria sessão nova, só verifica se já existe
        HttpSession s = req.getSession(false);
        if (s != null && s.getAttribute("usuarioLogado") != null) {
            Usuario logado = (Usuario) s.getAttribute("usuarioLogado");
            System.out.println("✅ Usuário já logado: " + logado.getNome()
                    + " (perfil: " + logado.getPerfil() + ")");
            System.out.println("➡️ Redirecionando para tela do perfil");
            res.sendRedirect(destino(logado, req));
        } else {
            // ========== EXIBIR FORMULÁRIO ==========
            System.out.println("📄 Exibindo formulário de login");
            req.getRequestDispatcher("/WEB-INF/views/auth/login.jsp").forward(req, res);
        }
    }

    /* ================================================================
       ROTA 2: EXIBIR FORMULÁRIO DE CADASTRO
       ================================================================

       URL: GET /auth/cadastro

       Comportamento:
       - Se usuário JÁ está logado → redireciona para a tela do seu
         perfil (calculada por destino())
       - Se usuário NÃO está logado → exibe cadastro.jsp

       JSP: /WEB-INF/views/auth/cadastro.jsp

       Campos do formulário:
       - nome            (obrigatório)
       - login           (obrigatório, mínimo 3 caracteres)
       - senha           (obrigatório, mínimo 6 caracteres)
       - confirmarSenha  (obrigatório, deve coincidir com senha)

       OBS: Este formulário só cadastra clientes (perfil USUARIO).
       Usuários GERENTE/FUNCIONARIO são cadastrados por outra via
       (ex: seed inicial do banco / cadastro administrativo).
    */
    private void exibirCadastro(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        System.out.println("📝 Iniciando exibição de CADASTRO");

        // ========== VERIFICAR SE JÁ ESTÁ LOGADO ==========
        HttpSession s = req.getSession(false);
        if (s != null && s.getAttribute("usuarioLogado") != null) {
            Usuario logado = (Usuario) s.getAttribute("usuarioLogado");
            System.out.println("✅ Usuário já logado: " + logado.getNome()
                    + " (perfil: " + logado.getPerfil() + ")");
            System.out.println("➡️ Redirecionando para tela do perfil");
            res.sendRedirect(destino(logado, req));
        } else {
            // ========== EXIBIR FORMULÁRIO ==========
            System.out.println("📄 Exibindo formulário de cadastro");
            req.getRequestDispatcher("/WEB-INF/views/auth/cadastro.jsp").forward(req, res);
        }
    }

    /* ================================================================
       AÇÃO 1: PROCESSAR LOGIN (Autenticação)
       ================================================================

       URL: POST /auth/login

       Parâmetros obrigatórios:
       - login: username cadastrado (comparado direto, sem máscara)
       - senha: texto plano (comparado com hash BCrypt)

       Fluxo completo:
       1. Recebe login e senha do formulário
       2. Valida se os campos não estão vazios/em branco
       3. Busca usuário no banco por login (UsuarioDAO.buscarPorLogin)
       4. Verifica se o usuário existe
       5. Compara senha com hash usando BCrypt.checkpw()
       6. Se OK: cria sessão e redireciona conforme o perfil (destino())
       7. Se ERRO: volta para login.jsp com mensagem genérica
       8. Se EXCEÇÃO: loga o erro e exibe mensagem de sistema

       Sessão criada:
       - Atributo "usuarioLogado": objeto Usuario completo
       - Timeout: 1800 segundos (30 minutos)

       Mensagens de erro:
       - "Login e senha são obrigatórios."
       - "Login ou senha incorretos." (mensagem única para os dois casos,
         não revela se o login existe — mitiga enumeração de usuários)

       SEGURANÇA:
       ✅ Não informa se o login existe ou não (evita ataques de enumeração)
       ✅ Mensagem genérica "Login ou senha incorretos"
       ✅ BCrypt.checkpw() valida o hash com salt embutido
       ✅ try-with-resources fecha a conexão automaticamente
    */
    private void processarLogin(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        System.out.println("🔐 Iniciando processamento de LOGIN");

        // ========== STEP 1: RECEBER PARÂMETROS ==========
        String login = req.getParameter("login");
        String senha = req.getParameter("senha");

        System.out.println("📋 Dados recebidos:");
        System.out.println("   - Login: " + login);
        System.out.println("   - Senha: " + (senha != null ? "***" : "null"));

        // ========== STEP 2: VALIDAR CAMPOS VAZIOS ==========
        if (login == null || login.isBlank() || senha == null || senha.isBlank()) {
            System.err.println("❌ Validação falhou: login ou senha em branco");
            req.setAttribute("erro", "Login e senha são obrigatórios.");
            req.getRequestDispatcher("/WEB-INF/views/auth/login.jsp").forward(req, res);
            return;
        }
        System.out.println("✅ Campos obrigatórios preenchidos");

        // ========== STEP 3: CONECTAR AO BANCO E AUTENTICAR ==========
        try (Connection conn = Conexao.getConnection()) {

            System.out.println("✅ Conexão com banco estabelecida");

            // ---- STEP 3.1: Buscar usuário por login ----
            System.out.println("⏳ Buscando usuário no banco...");
            System.out.println("   SQL equivalente: SELECT * FROM usuario WHERE login = ?");
            System.out.println("   Parâmetro: " + login.trim());

            Usuario u = new UsuarioDAO(conn).buscarPorLogin(login.trim());

            // ---- STEP 3.2: Validar existência + senha em uma só checagem ----
            // Combinar as duas validações (usuário nulo OU senha errada) evita
            // vazar informação sobre qual delas falhou — mensagem sempre igual.
            if (u == null || !BCrypt.checkpw(senha, u.getSenha())) {
                System.err.println("❌ Login ou senha incorretos (mensagem genérica por segurança)");
                req.setAttribute("erro", "Login ou senha incorretos.");
                req.getRequestDispatcher("/WEB-INF/views/auth/login.jsp").forward(req, res);
                return;
            }

            System.out.println("✅ Usuário autenticado:");
            System.out.println("   - ID: " + u.getIdUsuario());
            System.out.println("   - Nome: " + u.getNome());
            System.out.println("   - Perfil: " + u.getPerfil());

            // ========== STEP 4: CRIAR SESSÃO ==========
            System.out.println("⏳ Criando sessão...");
            HttpSession session = req.getSession();
            session.setAttribute("usuarioLogado", u);
            session.setMaxInactiveInterval(1800); // 30 minutos

            System.out.println("✅ Sessão criada:");
            System.out.println("   - Session ID: " + session.getId());
            System.out.println("   - Timeout: 1800s (30 min)");
            System.out.println("   - Atributo 'usuarioLogado': " + u.getNome());

            // ========== STEP 5: REDIRECIONAR CONFORME O PERFIL ==========
            System.out.println("✅ LOGIN BEM-SUCEDIDO!");
            System.out.println("➡️ Redirecionando conforme perfil: " + u.getPerfil());
            res.sendRedirect(destino(u, req));

        } catch (Exception e) {
            // ========== TRATAMENTO DE ERRO ==========
            System.err.println("❌ ERRO ao processar login:");
            System.err.println("   Tipo: " + e.getClass().getName());
            System.err.println("   Mensagem: " + e.getMessage());
            e.printStackTrace();

            req.setAttribute("erro", "Erro no sistema. Tente novamente.");
            req.getRequestDispatcher("/WEB-INF/views/auth/login.jsp").forward(req, res);
        }
    }

    /* ================================================================
       AÇÃO 2: PROCESSAR CADASTRO
       ================================================================

       URL: POST /auth/cadastro

       Parâmetros obrigatórios:
       - nome:           texto não vazio
       - login:          mínimo 3 caracteres, único
       - senha:          mínimo 6 caracteres
       - confirmarSenha: deve ser igual a senha

       Validações realizadas (em ordem, "fail fast" — a primeira que
       falhar já interrompe o fluxo e devolve para o formulário):
       1. Nome preenchido
       2. Login com no mínimo 3 caracteres
       3. Senha com no mínimo 6 caracteres
       4. Confirmação de senha igual à senha
       5. Login ainda não cadastrado (único)

       Fluxo completo:
       1. Recebe dados do formulário
       2. Valida campos obrigatórios (nome, login, senha, confirmação)
       3. Verifica se o login já existe no banco
       4. Gera hash BCrypt da senha
       5. Cria objeto Usuario com perfil fixo "USUARIO" (cliente)
       6. Insere no banco via UsuarioDAO
       7. Redireciona para /auth/login com mensagem de sucesso na sessão

       Em caso de erro:
       - Volta para cadastro.jsp com mensagem específica (via request)

       SEGURANÇA:
       ✅ Senha com hash BCrypt (gensalt padrão)
       ✅ Login único na base (verificado antes de inserir)
       ✅ Perfil sempre forçado para "USUARIO" — cadastro público nunca
          cria GERENTE nem FUNCIONARIO (evita escalonamento de privilégio)
       ✅ PreparedStatement (via DAO)
    */
    private void processarCadastro(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        System.out.println("📝 Iniciando processamento de CADASTRO");

        // ========== STEP 1: RECEBER PARÂMETROS ==========
        String nome     = req.getParameter("nome");
        String login    = req.getParameter("login");
        String senha    = req.getParameter("senha");
        String confirmar = req.getParameter("confirmarSenha");

        System.out.println("📋 Dados recebidos:");
        System.out.println("   - Nome: " + nome);
        System.out.println("   - Login: " + login);
        System.out.println("   - Senha: " + (senha != null ? "***" : "null"));
        System.out.println("   - Confirmar: " + (confirmar != null ? "***" : "null"));

        // ========== STEP 2: VALIDAR CAMPOS OBRIGATÓRIOS ==========

        // Validação 1: Nome
        if (nome == null || nome.isBlank()) {
            System.err.println("❌ Nome vazio!");
            req.setAttribute("erro", "Nome obrigatório.");
            req.getRequestDispatcher("/WEB-INF/views/auth/cadastro.jsp").forward(req, res);
            return;
        }

        // Validação 2: Login (mínimo 3 caracteres)
        if (login == null || login.length() < 3) {
            System.err.println("❌ Login inválido: " + (login != null ? login.length() : 0)
                    + " caracteres (mínimo: 3)");
            req.setAttribute("erro", "Login deve ter no mínimo 3 caracteres.");
            req.getRequestDispatcher("/WEB-INF/views/auth/cadastro.jsp").forward(req, res);
            return;
        }

        // Validação 3: Senha (mínimo 6 caracteres)
        if (senha == null || senha.length() < 6) {
            System.err.println("❌ Senha curta: " + (senha != null ? senha.length() : 0)
                    + " caracteres (mínimo: 6)");
            req.setAttribute("erro", "Senha deve ter no mínimo 6 caracteres.");
            req.getRequestDispatcher("/WEB-INF/views/auth/cadastro.jsp").forward(req, res);
            return;
        }

        // Validação 4: Confirmação de senha
        if (!senha.equals(confirmar)) {
            System.err.println("❌ Senhas não coincidem!");
            req.setAttribute("erro", "As senhas não coincidem.");
            req.getRequestDispatcher("/WEB-INF/views/auth/cadastro.jsp").forward(req, res);
            return;
        }

        System.out.println("✅ Campos obrigatórios OK");

        // ========== STEP 3: CONECTAR AO BANCO ==========
        try (Connection conn = Conexao.getConnection()) {

            System.out.println("✅ Conexão com banco estabelecida");
            UsuarioDAO dao = new UsuarioDAO(conn);

            // ========== STEP 4: VERIFICAR LOGIN ÚNICO ==========
            System.out.println("⏳ Verificando se login já existe...");
            System.out.println("   SQL equivalente: SELECT * FROM usuario WHERE login = ?");
            System.out.println("   Parâmetro: " + login.trim());

            if (dao.buscarPorLogin(login.trim()) != null) {
                System.err.println("❌ Login já cadastrado!");
                req.setAttribute("erro", "Login já está em uso.");
                req.getRequestDispatcher("/WEB-INF/views/auth/cadastro.jsp").forward(req, res);
                return;
            }
            System.out.println("✅ Login disponível");

            // ========== STEP 5: GERAR HASH BCRYPT DA SENHA ==========
            System.out.println("⏳ Gerando hash BCrypt da senha...");
            String senhaHash = BCrypt.hashpw(senha, BCrypt.gensalt());
            System.out.println("✅ Hash gerado: " + senhaHash.substring(0, 20) + "...");

            // ========== STEP 6: CRIAR OBJETO USUARIO ==========
            System.out.println("⏳ Criando objeto Usuario...");
            Usuario u = new Usuario();
            u.setNome(nome.trim());
            u.setLogin(login.trim().toLowerCase());
            u.setSenha(senhaHash);
            u.setPerfil("USUARIO"); // cadastro público → sempre cliente
            u.setAtivo(true);

            System.out.println("✅ Objeto criado:");
            System.out.println("   - Nome: " + u.getNome());
            System.out.println("   - Login: " + u.getLogin());
            System.out.println("   - Perfil: " + u.getPerfil());
            System.out.println("   - Senha: [hash]");

            // ========== STEP 7: INSERIR NO BANCO ==========
            System.out.println("⏳ Inserindo no banco de dados...");
            System.out.println("   SQL equivalente: INSERT INTO usuario (nome, login, senha, perfil, ativo) VALUES (?, ?, ?, ?, ?)");

            dao.inserir(u);

            System.out.println("✅ CADASTRO BEM-SUCEDIDO!");

            // ========== STEP 8: REDIRECIONAR PARA LOGIN ==========
            req.getSession().setAttribute("sucesso", "Cadastro realizado! Faça login.");
            System.out.println("➡️ Redirecionando para /auth/login");
            res.sendRedirect(req.getContextPath() + "/auth/login");

        } catch (Exception e) {
            // ========== TRATAMENTO DE ERRO ==========
            System.err.println("❌ ERRO ao processar cadastro:");
            System.err.println("   Tipo: " + e.getClass().getName());
            System.err.println("   Mensagem: " + e.getMessage());
            e.printStackTrace();

            req.setAttribute("erro", "Erro ao cadastrar.");
            req.getRequestDispatcher("/WEB-INF/views/auth/cadastro.jsp").forward(req, res);
        }
    }

    /* ================================================================
       AÇÃO 3: EXECUTAR LOGOUT
       ================================================================

       URL: GET /auth/logout

       Comportamento:
       1. Obtém a sessão atual (sem criar uma nova)
       2. Invalida a sessão (se existir), removendo todos os atributos
       3. Redireciona para /auth/login

       Não requer autenticação prévia (pode ser chamado mesmo sem
       sessão ativa — nesse caso simplesmente não faz nada e redireciona).

       IMPORTANTE: Sempre redireciona para /auth/login, nunca exibe página.
    */
    private void logout(HttpServletRequest req, HttpServletResponse res) throws IOException {

        System.out.println("🚪 Iniciando LOGOUT");

        // ========== STEP 1: OBTER SESSÃO (sem criar nova) ==========
        HttpSession s = req.getSession(false);

        // ========== STEP 2: INVALIDAR SESSÃO (se existir) ==========
        if (s != null) {
            Usuario logado = (Usuario) s.getAttribute("usuarioLogado");
            if (logado != null) {
                System.out.println("👤 Usuário a deslogar:");
                System.out.println("   - ID: " + logado.getIdUsuario());
                System.out.println("   - Nome: " + logado.getNome());
                System.out.println("   - Perfil: " + logado.getPerfil());
            }

            System.out.println("⏳ Invalidando sessão...");
            System.out.println("   - Session ID: " + s.getId());
            s.invalidate();
            System.out.println("✅ Sessão invalidada com sucesso");
        } else {
            System.out.println("ℹ️ Nenhuma sessão ativa para invalidar");
        }

        // ========== STEP 3: REDIRECIONAR PARA LOGIN ==========
        System.out.println("➡️ Redirecionando para /auth/login");
        res.sendRedirect(req.getContextPath() + "/auth/login");
    }

    /* ================================================================
       HELPER: CALCULAR DESTINO PÓS-LOGIN POR PERFIL
       ================================================================

       Centraliza a regra de redirecionamento — usada tanto em
       exibirLogin()/exibirCadastro() (quando já está logado) quanto
       em processarLogin() (logo após autenticar).

       v3 — FUNCIONARIO sempre vai para /app/mesas, independente da
       coluna "funcao" (atendente ou cozinha). Antes da v3 existia
       distinção por função; agora o fluxo é unificado.

       Regra:
       - GERENTE     → /app/dashboard
       - FUNCIONARIO → /app/mesas
       - USUARIO (e qualquer outro valor) → /app/cardapio
    */
    private String destino(Usuario u, HttpServletRequest req) {
        String b = req.getContextPath();
        return switch (u.getPerfil()) {
            case "GERENTE"     -> b + "/app/dashboard";
            case "FUNCIONARIO" -> b + "/app/mesas";
            default            -> b + "/app/cardapio";   // USUARIO
        };
    }
}

/* ================================================================
   RESUMO DO CONTROLLER
   ================================================================

   ROTAS MAPEADAS:
   1. GET  /auth/login    → Exibe formulário login.jsp (ou redireciona se já logado)
   2. POST /auth/login    → Autentica usuário (login + senha)
   3. GET  /auth/cadastro → Exibe formulário cadastro.jsp (ou redireciona se já logado)
   4. POST /auth/cadastro → Cria novo usuário (sempre perfil USUARIO)
   5. GET  /auth/logout   → Encerra sessão

   CAMPOS DO FORMULÁRIO DE LOGIN:
   - login
   - senha

   CAMPOS DO FORMULÁRIO DE CADASTRO:
   - nome*            (obrigatório)
   - login*           (obrigatório, mínimo 3 caracteres, único)
   - senha*           (obrigatório, mínimo 6 caracteres)
   - confirmarSenha*  (obrigatório, deve coincidir)

   VALIDAÇÕES:
   ✅ Nome: não vazio
   ✅ Login: mínimo 3 caracteres, único
   ✅ Senha: mínimo 6 caracteres
   ✅ Confirmação: deve coincidir

   SEGURANÇA:
   ✅ BCrypt para senhas
   ✅ PreparedStatement (via DAO)
   ✅ Mensagem genérica no login (não revela se o login existe)
   ✅ Perfil sempre forçado para USUARIO no cadastro público
   ✅ Sessão com timeout de 30 minutos

   SESSÃO CRIADA NO LOGIN:
   - Atributo: "usuarioLogado" (objeto Usuario)
   - Timeout: 1800 segundos (30 minutos)
   - Inativa após timeout ou logout

   REDIRECIONAMENTO PÓS-LOGIN (destino()):
   - GERENTE     → /app/dashboard
   - FUNCIONARIO → /app/mesas
   - USUARIO     → /app/cardapio

   EXEMPLOS DE USO:
   ```
   // Login:
   POST /auth/login
   login=funcionario&senha=minhaSenha123

   // Cadastro:
   POST /auth/cadastro
   nome=João Silva&login=joaosilva&senha=senha123&confirmarSenha=senha123

   // Logout:
   GET /auth/logout
   ```

   DEPENDÊNCIAS:
   - UsuarioDAO: Acesso ao banco (tabela usuario)
   - Usuario: Model
   - Conexao: Gerenciamento de conexões
   - BCrypt: Criptografia de senhas

   OBSERVAÇÕES:
   - Conexões fecham automaticamente (try-with-resources)
   - Logs detalhados em cada etapa (System.out / System.err)
   - Tratamento de exceções em processarLogin() e processarCadastro()
   ================================================================ */
