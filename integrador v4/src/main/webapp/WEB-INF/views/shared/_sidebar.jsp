<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="br.com.restaurante.model.Usuario" %>
<%--
    ================================================================
    _SIDEBAR.JSP — MENU LATERAL (v4 — visual alinhado ao Figma)
    ================================================================
    Componente reutilizável (incluído via <%@ include %> em toda
    página logada) que monta a barra lateral inteira: marca do app no
    topo, itens de navegação filtrados por PERFIL do usuário logado,
    e o rodapé com avatar/nome/perfil/sair.

    O QUE MUDOU NESTA VERSÃO (em relação à anterior):
      - Visual: fundo escuro + cabeçalho com "marca" (ícone quadrado
        verde) e rodapé com "pílula" de perfil, para bater com o
        protótipo Figma (ver telas "Roberto Lima / Gerência" e
        "Ana Silva / Cliente").
      - O rótulo do Dashboard virou "Painel" (só o texto exibido —
        a rota continua sendo /app/dashboard, então nenhuma mudança
        de Controller foi necessária).
      - O bloco de rodapé, que antes era todo montado com
        style="..." inline, agora usa classes do style.css
        (.sidebar-footer, .role-pill etc.) — mais fácil de manter e
        consistente com o resto da folha de estilo.
    O QUE **NÃO** MUDOU (de propósito):
      - A lógica de quais itens aparecem para cada perfil continua
        EXATAMENTE a mesma (GERENTE vê tudo, FUNCIONARIO vê
        Mesas/Cardápio/Pedidos/Fila, USUARIO vê a área de cliente).
        O Figma mostra conjuntos de menu ligeiramente diferentes por
        tela (ex: a tela de "Funcionário" no protótipo não lista
        Cardápio, e a de "Gerente" não lista Fila de Preparo) — isso
        não foi replicado aqui porque contradiria as regras de
        permissão já implementadas e documentadas nos Controllers
        (MesaController, FilaController etc.). Mudar ISSO seria
        alterar comportamento/rota, não visual, e está fora do
        escopo de "deixar a UI parecida com o Figma".
    ================================================================
--%>
<%
    Usuario _sbU   = (Usuario) session.getAttribute("usuarioLogado");
    String _perfil = _sbU != null ? _sbU.getPerfil() : "";
    String _ativo  = (String) request.getAttribute("paginaAtiva");
    if (_ativo == null) _ativo = "";

    // Rótulo amigável do perfil, usado na "pílula" do rodapé —
    // GERENTE/FUNCIONARIO/USUARIO (valores do enum no banco) viram
    // "Gerência"/"Funcionário"/"Cliente" (mais legível, e alinhado
    // com o texto usado no Figma: "Gerência", "Cliente").
    String _perfilLabel;
    if ("GERENTE".equals(_perfil))          _perfilLabel = "Gerência";
    else if ("FUNCIONARIO".equals(_perfil)) _perfilLabel = "Funcionário";
    else                                    _perfilLabel = "Cliente";
%>
<nav class="sidebar">

  <%-- Cabeçalho / marca do app — ícone quadrado verde + nome,
       reproduzindo o bloco "🍽 Integrador / Sistema de Pedidos"
       que aparece fixo no topo da sidebar em todas as telas do
       protótipo Figma. --%>
  <div class="sidebar-header">
    <div class="brand-icon">🍽️</div>
    <div class="brand-text">
      <h1>Integrador</h1>
      <p>Sistema de Pedidos</p>
    </div>
  </div>

  <ul class="sidebar-menu">

    <%-- GERENTE --%>
    <% if ("GERENTE".equals(_perfil)) { %>
      <li><a href="${pageContext.request.contextPath}/app/dashboard"
             class="<%= "dashboard".equals(_ativo) ? "active" : "" %>">
        <span class="icon">📊</span><span class="label">Painel</span></a></li>
      <li><a href="${pageContext.request.contextPath}/app/mesas"
             class="<%= "mesas".equals(_ativo) ? "active" : "" %>">
        <span class="icon">🪑</span><span class="label">Mesas</span></a></li>
      <li><a href="${pageContext.request.contextPath}/app/cardapio"
             class="<%= "cardapio".equals(_ativo) ? "active" : "" %>">
        <span class="icon">📋</span><span class="label">Cardápio</span></a></li>
      <li><a href="${pageContext.request.contextPath}/app/pedidos"
             class="<%= "pedidos".equals(_ativo) ? "active" : "" %>">
        <span class="icon">🧾</span><span class="label">Pedidos</span></a></li>
      <li><a href="${pageContext.request.contextPath}/app/fila"
             class="<%= "fila".equals(_ativo) ? "active" : "" %>">
        <span class="icon">👨‍🍳</span><span class="label">Fila de Preparo</span></a></li>
      <li><a href="${pageContext.request.contextPath}/app/relatorios"
             class="<%= "relatorios".equals(_ativo) ? "active" : "" %>">
        <span class="icon">📄</span><span class="label">Relatórios</span></a></li>
    <% } %>

    <%-- FUNCIONARIO — unificado: vê tudo exceto dashboard e relatórios --%>
    <% if ("FUNCIONARIO".equals(_perfil)) { %>
      <li><a href="${pageContext.request.contextPath}/app/mesas"
             class="<%= "mesas".equals(_ativo) ? "active" : "" %>">
        <span class="icon">🪑</span><span class="label">Mesas</span></a></li>
      <li><a href="${pageContext.request.contextPath}/app/cardapio"
             class="<%= "cardapio".equals(_ativo) ? "active" : "" %>">
        <span class="icon">📋</span><span class="label">Cardápio</span></a></li>
      <li><a href="${pageContext.request.contextPath}/app/pedidos"
             class="<%= "pedidos".equals(_ativo) ? "active" : "" %>">
        <span class="icon">🧾</span><span class="label">Pedidos</span></a></li>
      <li><a href="${pageContext.request.contextPath}/app/fila"
             class="<%= "fila".equals(_ativo) ? "active" : "" %>">
        <span class="icon">👨‍🍳</span><span class="label">Fila de Preparo</span></a></li>
    <% } %>

    <%-- USUARIO (cliente) — cardápio, delivery e reserva --%>
    <% if ("USUARIO".equals(_perfil)) { %>
      <li><a href="${pageContext.request.contextPath}/app/cardapio"
             class="<%= "cardapio".equals(_ativo) ? "active" : "" %>">
        <span class="icon">📋</span><span class="label">Cardápio</span></a></li>
      <li><a href="${pageContext.request.contextPath}/app/cliente/delivery"
             class="<%= "delivery".equals(_ativo) ? "active" : "" %>">
        <span class="icon">🛵</span><span class="label">Pedir Delivery</span></a></li>
      <li><a href="${pageContext.request.contextPath}/app/cliente/reserva"
             class="<%= "reserva".equals(_ativo) ? "active" : "" %>">
        <span class="icon">📅</span><span class="label">Reservar Mesa</span></a></li>
      <li><a href="${pageContext.request.contextPath}/app/cliente/meus-pedidos"
             class="<%= "meus-pedidos".equals(_ativo) ? "active" : "" %>">
        <span class="icon">🧾</span><span class="label">Meus Pedidos</span></a></li>
    <% } %>

  </ul>

  <%-- Rodapé fixo: avatar (iniciais do nome) + nome + pílula de
       perfil + link de logout — mesmo padrão visual do bloco
       "Roberto Lima / Gerência" no protótipo. --%>
  <div class="sidebar-footer">
    <div class="user-row">
      <div class="user-avatar">
        <%= (_sbU != null && _sbU.getNome() != null && !_sbU.getNome().isEmpty())
              ? _sbU.getNome().substring(0,1).toUpperCase() : "?" %>
      </div>
      <div style="overflow:hidden">
        <div class="user-name"><%= _sbU != null ? _sbU.getNome() : "" %></div>
        <span class="role-pill"><%= _perfilLabel %></span>
      </div>
    </div>
    <a href="${pageContext.request.contextPath}/auth/logout" class="logout-link">Sair</a>
  </div>
</nav>
