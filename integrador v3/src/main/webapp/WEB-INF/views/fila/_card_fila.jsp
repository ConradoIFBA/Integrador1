<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
    // Parâmetros passados via jsp:param
    String idFila    = request.getParameter("idFila");
    String idPedido  = request.getParameter("idPedido");
    String posicao   = request.getParameter("posicao");
    String tipo      = request.getParameter("tipo");
    String mesa      = request.getParameter("mesa");
    String urgente   = request.getParameter("urgente");
    String tempo     = request.getParameter("tempo");
    String operador  = request.getParameter("operador");
    String aguardando= request.getParameter("aguardando");
    String entrada   = request.getParameter("entrada");

    boolean isUrgente    = "true".equals(urgente);
    boolean isAguardando = "true".equals(aguardando);
    String cssCard = isUrgente ? "urgente" : (isAguardando ? "aguardando" : "em-preparo");
%>
<div class="fila-card <%= cssCard %>">

    <div class="fila-posicao">#<%= posicao %></div>

    <div class="fila-id">Pedido #<%= idPedido %></div>

    <div class="fila-meta">
        <span>
            <% if ("mesa".equals(tipo)) { %>
                🪑 Mesa <%= mesa %>
            <% } else { %>
                🛵 Delivery
            <% } %>
        </span>
        <% if (isUrgente) { %>
            <span style="color:#ef4444; font-weight:700">🔴 Urgente</span>
        <% } %>
        <span>⏱ ~<%= tempo %> min</span>
        <span class="text-muted">desde <%= entrada %></span>
    </div>

    <% if (!isAguardando && operador != null && !operador.isEmpty()) { %>
        <div style="font-size:12px; color:#f59e0b; font-weight:600; margin-bottom:10px">
            👨‍🍳 Em preparo por <%= operador %>
        </div>
    <% } %>

    <div class="fila-acoes">
        <% if (isAguardando) { %>
            <%-- Ainda não iniciou: botão Iniciar --%>
            <button type="button"
                    class="btn btn-primary"
                    onclick="abrirModal('iniciar', '<%= idFila %>', '<%= idPedido %>')">
                ▶ Iniciar preparo
            </button>
        <% } else { %>
            <%-- Já em preparo: botão Concluir --%>
            <button type="button"
                    class="btn btn-success"
                    onclick="abrirModal('concluir', '<%= idFila %>', '<%= idPedido %>')">
                ✓ Concluir
            </button>
        <% } %>
    </div>

</div>
