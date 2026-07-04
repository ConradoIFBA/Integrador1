<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="br.com.restaurante.model.Usuario" %>
<%
    // Variável com nome único (_sbU) para não conflitar com _u das páginas que incluem este arquivo
    Usuario _sbU    = (Usuario) session.getAttribute("usuarioLogado");
    String _perfil  = _sbU != null ? _sbU.getPerfil() : "";
    String _funcao  = _sbU != null && _sbU.getFuncao() != null ? _sbU.getFuncao() : "";
    String _sbNome  = _sbU != null ? _sbU.getNome() : "";
    String _ativo   = (String) request.getAttribute("paginaAtiva");
    if (_ativo == null) _ativo = "";
%>
<nav class="sidebar">

    <div class="sidebar-header">
        <h1>🍽️ Integrador</h1>
        <p>Sistema de Pedidos</p>
    </div>

    <ul class="sidebar-menu">

        <% if ("GERENTE".equals(_perfil)) { %>
        <li>
            <a href="${pageContext.request.contextPath}/app/dashboard"
               class="<%= "dashboard".equals(_ativo) ? "active" : "" %>">
                <span class="icon">📊</span><span class="label">Dashboard</span>
            </a>
        </li>
        <% } %>

        <% if ("GERENTE".equals(_perfil) || ("FUNCIONARIO".equals(_perfil) && "atendente".equals(_funcao))) { %>
        <li>
            <a href="${pageContext.request.contextPath}/app/mesas"
               class="<%= "mesas".equals(_ativo) ? "active" : "" %>">
                <span class="icon">🪑</span><span class="label">Mesas</span>
            </a>
        </li>
        <% } %>

        <% if ("GERENTE".equals(_perfil) || "USUARIO".equals(_perfil)
               || ("FUNCIONARIO".equals(_perfil) && "atendente".equals(_funcao))) { %>
        <li>
            <a href="${pageContext.request.contextPath}/app/cardapio"
               class="<%= "cardapio".equals(_ativo) ? "active" : "" %>">
                <span class="icon">📋</span><span class="label">Cardápio</span>
            </a>
        </li>
        <% } %>

        <% if ("GERENTE".equals(_perfil) || ("FUNCIONARIO".equals(_perfil) && "atendente".equals(_funcao))) { %>
        <li>
            <a href="${pageContext.request.contextPath}/app/pedidos"
               class="<%= "pedidos".equals(_ativo) ? "active" : "" %>">
                <span class="icon">🧾</span><span class="label">Pedidos</span>
            </a>
        </li>
        <% } %>

        <% if ("GERENTE".equals(_perfil) || ("FUNCIONARIO".equals(_perfil) && "cozinha".equals(_funcao))) { %>
        <li>
            <a href="${pageContext.request.contextPath}/app/fila"
               class="<%= "fila".equals(_ativo) ? "active" : "" %>">
                <span class="icon">👨‍🍳</span><span class="label">Fila de Preparo</span>
            </a>
        </li>
        <% } %>

        <% if ("GERENTE".equals(_perfil)) { %>
        <li>
            <a href="${pageContext.request.contextPath}/app/relatorios"
               class="<%= "relatorios".equals(_ativo) ? "active" : "" %>">
                <span class="icon">📄</span><span class="label">Relatórios</span>
            </a>
        </li>
        <% } %>

    </ul>

    <div style="position:absolute; bottom:0; left:0; right:0;
                padding:16px 20px; border-top:1px solid rgba(255,255,255,.1);
                background:rgba(0,0,0,.15);">
        <div style="font-size:13px; color:rgba(255,255,255,.9); font-weight:600;
                    white-space:nowrap; overflow:hidden; text-overflow:ellipsis;">
            <%= _sbNome %>
        </div>
        <div style="font-size:11px; color:rgba(255,255,255,.5); margin-top:2px;">
            <%= _perfil %>
            <% if (!_funcao.isEmpty()) { %> · <%= _funcao %><% } %>
        </div>
        <a href="${pageContext.request.contextPath}/auth/logout"
           style="display:inline-block; margin-top:10px; padding:5px 12px;
                  background:rgba(255,255,255,.1); color:rgba(255,255,255,.8);
                  border-radius:5px; font-size:12px; text-decoration:none;
                  transition:background .2s;"
           onmouseover="this.style.background='rgba(255,255,255,.2)'"
           onmouseout="this.style.background='rgba(255,255,255,.1)'">
            Sair
        </a>
    </div>

</nav>
