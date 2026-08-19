package br.com.restaurante.filter;

import java.io.IOException;
import br.com.restaurante.model.Usuario;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.*;

/**
 * ================================================================
 * AUTHFILTER — GUARDA DE AUTENTICAÇÃO DA ÁREA LOGADA (/app/*)
 * ================================================================
 *
 * Filtro de segurança que protege TODAS as rotas internas do
 * sistema (tudo debaixo de /app/*: dashboard, mesas, cardápio,
 * pedidos, fila, relatórios, área do cliente). A regra é simples e
 * única: "se não tem usuário na sessão, não passa" — a distinção
 * mais fina de PERMISSÃO por perfil (GERENTE vs FUNCIONARIO vs
 * USUARIO) NÃO é feita aqui, e sim dentro de cada Controller
 * individualmente (ex: DashboardController só deixa GERENTE passar,
 * MesaController deixa GERENTE+FUNCIONARIO, etc.). Este filtro
 * resolve só a pergunta "está autenticado?", nunca "está autorizado
 * a fazer ISSO especificamente?".
 *
 * MAPEAMENTO: @WebFilter("/app/*")
 *   Note que /auth/* (login, cadastro, logout) e recursos estáticos
 *   (/assets/*) NÃO passam por este filtro — são as únicas rotas
 *   acessíveis sem estar logado, o que faz sentido: é justamente
 *   por ali que o usuário CONSEGUE se autenticar.
 *
 * FLUXO DE DECISÃO:
 *   1. Pega a HttpSession existente, SEM criar uma nova caso não
 *      exista (getSession(false) — importante: getSession(true) ou
 *      getSession() sem argumento criaria uma sessão vazia à toa
 *      para todo visitante anônimo, desperdiçando memória no
 *      servidor).
 *   2. Se existe sessão, tenta ler o atributo "usuarioLogado" —
 *      esse é o MESMO nome de atributo usado pelo AuthController ao
 *      fazer login (session.setAttribute("usuarioLogado", usuario))
 *      e por todas as JSPs ao exibir dados do usuário
 *      (session.getAttribute("usuarioLogado")). Precisa bater
 *      exatamente, senão o filtro trata o usuário como deslogado
 *      mesmo com sessão ativa.
 *   3. Se não há sessão OU o atributo está ausente/nulo → usuário
 *      não autenticado → redireciona para /auth/login e INTERROMPE
 *      a cadeia (return, sem chamar chain.doFilter) — a requisição
 *      original nunca chega ao Controller de destino.
 *   4. Se há usuário válido na sessão → deixa a requisição
 *      continuar normalmente (chain.doFilter).
 *
 * request.getContextPath() NO REDIRECT:
 *   Sempre usado em vez de um caminho fixo tipo "/auth/login", para
 *   o link funcionar independente de o projeto estar publicado na
 *   raiz do servidor ou num subcaminho (ex: /integrador). Sem isso,
 *   o redirect quebraria assim que o context path da aplicação
 *   mudasse.
 *
 * ⚠️ REGISTRO DUPLICADO (mesmo ponto de atenção do CharsetFilter):
 *   Esta classe tem @WebFilter("/app/*") E também está declarada no
 *   web.xml via <filter>/<filter-mapping> com filter-name
 *   "AuthFilter". Como os nomes não coincidem entre a anotação e o
 *   web.xml, o Tomcat registra DUAS instâncias do filtro, e por
 *   isso ele roda duas vezes por requisição às rotas /app/* — dá
 *   pra confirmar isso olhando qualquer stack trace de erro nessas
 *   rotas: "AuthFilter.doFilter" aparece duas vezes na pilha. Não
 *   causa bug funcional aqui porque checar a sessão duas vezes
 *   seguidas é inofensivo (idempotente), mas é redundante e pode
 *   confundir quem for ler os logs. Fix: remover o bloco
 *   <filter>/<filter-mapping> referente a "AuthFilter" do web.xml,
 *   já que a anotação sozinha é suficiente para o registro.
 *
 * O QUE ESTE FILTRO **NÃO** FAZ (de propósito):
 *   - Não verifica PERFIL (GERENTE/FUNCIONARIO/USUARIO) — cada
 *     Controller faz sua própria checagem de autorização depois que
 *     este filtro já garantiu que existe alguém logado.
 *   - Não renova/expira sessão manualmente — isso é gerenciado pelo
 *     próprio container (timeout configurado em session.setMaxInactiveInterval,
 *     30 min conforme visto nos logs do AuthController).
 *   - Não distingue GET de POST — a regra "precisa estar logado" é
 *     igual para os dois verbos.
 *
 * @author Sistema Integrador
 * @version 3.0
 * @see br.com.restaurante.controller.AuthController
 * @see br.com.restaurante.filter.CharsetFilter
 */
@WebFilter("/app/*")
public class AuthFilter implements Filter {

    /**
     * Nada a inicializar — este filtro não tem estado próprio
     * (diferente do CharsetFilter, que guarda o encoding lido do
     * init-param). Método presente apenas porque a interface
     * Filter exige a implementação.
     */
    @Override
    public void init(FilterConfig c) throws ServletException {}

    /**
     * Executado para toda requisição que casa com "/app/*".
     * Decide, com base na sessão HTTP, se a requisição pode
     * prosseguir para o Controller de destino ou se deve ser
     * desviada para a tela de login.
     */
    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        // Downcast necessário: a interface genérica Filter trabalha
        // com ServletRequest/ServletResponse (que também cobrem
        // protocolos não-HTTP, como sockets brutos), mas aqui
        // sabemos que é sempre HTTP porque o mapeamento é "/app/*"
        // dentro de uma aplicação web comum.
        HttpServletRequest  request  = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        // getSession(false): NÃO cria sessão nova se ainda não
        // existir uma. Isso é essencial aqui — um visitante nunca
        // logado batendo em /app/mesas não deveria fazer o servidor
        // alocar uma HttpSession (com seu JSESSIONID e memória
        // associada) só para descobrir que vai ser redirecionado
        // mesmo. Usar getSession(true) por engano criaria uma
        // sessão "fantasma" a cada tentativa de acesso anônimo.
        HttpSession session = request.getSession(false);

        // Se não há sessão, u fica direto null (operador ternário
        // curto-circuita sem nem tentar getAttribute). Se há
        // sessão, busca o objeto Usuario guardado nela no momento
        // do login bem-sucedido (ver AuthController.processarLogin).
        Usuario u = (session != null)
                ? (Usuario) session.getAttribute("usuarioLogado")
                : null;

        if (u == null) {
            // Sem usuário válido → bloqueia o acesso e manda para
            // login. O "return" aqui é crítico: sem ele, o código
            // continuaria e chamaria chain.doFilter mesmo depois de
            // já ter iniciado um redirect, o que geraria erro de
            // "resposta já commitada" (IllegalStateException) ou,
            // na melhor das hipóteses, deixaria o Controller de
            // destino rodar por engano com um usuário nulo.
            response.sendRedirect(request.getContextPath() + "/auth/login");
            return;
        }

        // Usuário autenticado — libera a requisição para o próximo
        // filtro da cadeia (no caso deste projeto, possivelmente
        // outra passada do próprio AuthFilter por causa do registro
        // duplicado, e depois o Controller de destino).
        chain.doFilter(req, res);
    }

    /**
     * Nada a liberar na destruição do filtro — sem recursos abertos
     * (sockets, arquivos, conexões) mantidos por esta classe.
     */
    @Override
    public void destroy() {}
}
