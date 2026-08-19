<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%--
    ================================================================
    _CARD_FILA.JSP — CARD INDIVIDUAL DENTRO DE UMA COLUNA DO KANBAN
    (v4 — visual alinhado ao Figma)
    ================================================================
    Incluído (via jsp:include + jsp:param) pela fila.jsp uma vez
    para cada pedido em cada setor. Recebe tudo por parâmetro de
    request porque jsp:include não compartilha escopo de página com
    quem inclui — por isso os campos chegam como String e precisam
    ser reconvertidos aqui (Boolean.parseBoolean equivalente manual
    com "true".equals(...), como já era feito antes).

       ================================================================
--%>
<%
    String idFila    = request.getParameter("idFila");
    String idPedido  = request.getParameter("idPedido");
    String tipo      = request.getParameter("tipo");
    String mesa      = request.getParameter("mesa");
    String urgente   = request.getParameter("urgente");
    String tempo     = request.getParameter("tempo");
    String operador  = request.getParameter("operador");
    String aguardando= request.getParameter("aguardando");
    String entrada   = request.getParameter("entrada");

    boolean isUrgente    = "true".equals(urgente);
    boolean isAguardando = "true".equals(aguardando);
%>
<div class="fila-card <%= isUrgente ? "urgente" : "" %>">

	<div class="fila-topo">
		<span class="fila-id">#<%= idPedido %></span>
		<% if (isAguardando) { %>
		<span class="badge badge-info fila-status-badge">Recebido</span>
		<% } else { %>
		<span class="badge badge-warning fila-status-badge">Em Preparo</span>
		<% } %>
	</div>

	<div class="fila-meta">
		<span> <% if ("mesa".equals(tipo)) { %> 🪑 Mesa <%= mesa %> <% } else { %>
			🛵 Delivery <% } %>
		</span>
		<% if (isUrgente) { %>
		<span style="color: var(--error-color); font-weight: 700">🔴
			Urgente</span>
		<% } %>
		<span class="tempo">⏱ ~<%= tempo %> min
		</span> <span class="text-muted">desde <%= entrada %></span>
	</div>

	<% if (!isAguardando && operador != null && !operador.isEmpty()) { %>
	<div class="fila-preparando-por">
		👨‍🍳 Em preparo por
		<%= operador %></div>
	<% } %>

	<div class="fila-acoes">
		<% if (isAguardando) { %>
		<button type="button" class="btn btn-primary"
			onclick="abrirModal('iniciar', '<%= idFila %>', '<%= idPedido %>')">
			▶ Iniciar</button>
		<% } else { %>
		<button type="button" class="btn btn-success"
			onclick="abrirModal('concluir', '<%= idFila %>', '<%= idPedido %>')">
			✓ Concluir</button>
		<% } %>
	</div>

</div>
