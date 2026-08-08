<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="br.com.restaurante.model.Usuario" %>
<%
    Usuario _sbU   = (Usuario) session.getAttribute("usuarioLogado");
    String _perfil = _sbU != null ? _sbU.getPerfil() : "";
    String _ativo  = (String) request.getAttribute("paginaAtiva");
    if (_ativo == null) _ativo = "";
%>
<nav class="sidebar">
  <div class="sidebar-header">
    <h1>🍽️ Integrador</h1>
    <p>Sistema de Pedidos</p>
  </div>
  <ul class="sidebar-menu">

    <%-- GERENTE --%>
    <% if ("GERENTE".equals(_perfil)) { %>
      <li><a href="${pageContext.request.contextPath}/app/dashboard"
             class="<%= "dashboard".equals(_ativo) ? "active" : "" %>">
        <span class="icon">📊</span><span class="label">Dashboard</span></a></li>
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

  <div style="position:absolute;bottom:0;left:0;right:0;padding:16px 20px;
              border-top:1px solid rgba(255,255,255,.1);background:rgba(0,0,0,.15)">
    <div style="font-size:13px;color:rgba(255,255,255,.9);font-weight:600;
                white-space:nowrap;overflow:hidden;text-overflow:ellipsis">
      <%= _sbU != null ? _sbU.getNome() : "" %>
    </div>
    <div style="font-size:11px;color:rgba(255,255,255,.5);margin-top:2px">
      <%= _perfil %>
    </div>
    <a href="${pageContext.request.contextPath}/auth/logout"
       style="display:inline-block;margin-top:10px;padding:5px 12px;
              background:rgba(255,255,255,.1);color:rgba(255,255,255,.8);
              border-radius:5px;font-size:12px;text-decoration:none">Sair</a>
  </div>
</nav>
