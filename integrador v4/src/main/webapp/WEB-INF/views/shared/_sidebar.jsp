<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@ page import="br.com.restaurante.model.Usuario"%>
<%--
    ================================================================
    _SIDEBAR.JSP — MENU LATERAL (v4 — visual alinhado ao Figma)
    ================================================================
 
--%>
<%
Usuario _sbU = (Usuario) session.getAttribute("usuarioLogado");
String _perfil = _sbU != null ? _sbU.getPerfil() : "";
String _ativo = (String) request.getAttribute("paginaAtiva");
if (_ativo == null)
	_ativo = "";

// Rótulo amigável do perfil, usado na "pílula" do rodapé —
// GERENTE/FUNCIONARIO/USUARIO (valores do enum no banco) viram
// "Gerência"/"Funcionário"/"Cliente" (mais legível, e alinhado
// com o texto usado no Figma: "Gerência", "Cliente").
String _perfilLabel;
if ("GERENTE".equals(_perfil))
	_perfilLabel = "Gerência";
else if ("FUNCIONARIO".equals(_perfil))
	_perfilLabel = "Funcionário";
else
	_perfilLabel = "Cliente";
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
		<%
		if ("GERENTE".equals(_perfil)) {
		%>
		<li><a href="${pageContext.request.contextPath}/app/dashboard"
			class="<%= "dashboard".equals(_ativo) ? "active" : "" %>"> <span
				class="icon">📊</span><span class="label">Painel</span></a></li>
		<li><a href="${pageContext.request.contextPath}/app/mesas"
			class="<%= "mesas".equals(_ativo) ? "active" : "" %>"> <span
				class="icon">🪑</span><span class="label">Mesas</span></a></li>
		<li><a href="${pageContext.request.contextPath}/app/cardapio"
			class="<%= "cardapio".equals(_ativo) ? "active" : "" %>"> <span
				class="icon">📋</span><span class="label">Cardápio</span></a></li>
		<li><a href="${pageContext.request.contextPath}/app/pedidos"
			class="<%= "pedidos".equals(_ativo) ? "active" : "" %>"> <span
				class="icon">🧾</span><span class="label">Pedidos</span></a></li>
		<li><a href="${pageContext.request.contextPath}/app/fila"
			class="<%= "fila".equals(_ativo) ? "active" : "" %>"> <span
				class="icon">👨‍🍳</span><span class="label">Fila de Preparo</span></a></li>
		<li><a href="${pageContext.request.contextPath}/app/relatorios"
			class="<%= "relatorios".equals(_ativo) ? "active" : "" %>"> <span
				class="icon">📄</span><span class="label">Relatórios</span></a></li>
		<li><a href="${pageContext.request.contextPath}/app/staff"
			class="<%= "staff".equals(_ativo) ? "active" : "" %>"> <span
				class="icon">👥</span><span class="label">Funcionários</span></a></li>
		<%
		}
		%>

		<%-- FUNCIONARIO — unificado: vê tudo exceto dashboard e relatórios --%>
		<%
		if ("FUNCIONARIO".equals(_perfil)) {
		%>
		<li><a href="${pageContext.request.contextPath}/app/mesas"
			class="<%= "mesas".equals(_ativo) ? "active" : "" %>"> <span
				class="icon">🪑</span><span class="label">Mesas</span></a></li>
		<li><a href="${pageContext.request.contextPath}/app/cardapio"
			class="<%= "cardapio".equals(_ativo) ? "active" : "" %>"> <span
				class="icon">📋</span><span class="label">Cardápio</span></a></li>
		<li><a href="${pageContext.request.contextPath}/app/pedidos"
			class="<%= "pedidos".equals(_ativo) ? "active" : "" %>"> <span
				class="icon">🧾</span><span class="label">Pedidos</span></a></li>
		<li><a href="${pageContext.request.contextPath}/app/fila"
			class="<%= "fila".equals(_ativo) ? "active" : "" %>"> <span
				class="icon">👨‍🍳</span><span class="label">Fila de Preparo</span></a></li>
		<%
		}
		%>

		<%-- USUARIO (cliente) — cardápio, delivery e reserva --%>
		<%
		if ("USUARIO".equals(_perfil)) {
		%>
		<li><a href="${pageContext.request.contextPath}/app/cardapio"
			class="<%= "cardapio".equals(_ativo) ? "active" : "" %>"> <span
				class="icon">📋</span><span class="label">Cardápio</span></a></li>
		<li><a href="${pageContext.request.contextPath}/app/cliente/mesa"
			class="<%= "mesa".equals(_ativo) ? "active" : "" %>"> <span
				class="icon">🍽️</span><span class="label">Pedido Direto</span></a></li>
		<li><a
			href="${pageContext.request.contextPath}/app/cliente/delivery"
			class="<%= "delivery".equals(_ativo) ? "active" : "" %>"> <span
				class="icon">🛵</span><span class="label">Pedir Delivery</span></a></li>
		<li><a
			href="${pageContext.request.contextPath}/app/cliente/reserva"
			class="<%= "reserva".equals(_ativo) ? "active" : "" %>"> <span
				class="icon">📅</span><span class="label">Reservar Mesa</span></a></li>
		<li><a
			href="${pageContext.request.contextPath}/app/cliente/meus-pedidos"
			class="<%= "meus-pedidos".equals(_ativo) ? "active" : "" %>"> <span
				class="icon">🧾</span><span class="label">Meus Pedidos</span></a></li>
		<%
		}
		%>

	</ul>

	<%-- Rodapé fixo: avatar (iniciais do nome) + nome + pílula de
       perfil + botão de tema + link de logout — mesmo padrão visual
       do bloco "Roberto Lima / Gerência" no protótipo (que também
       tinha um ícone de lua no canto). --%>
	<div class="sidebar-footer">
		<div class="user-row">
			<div class="user-avatar">
				<%=(_sbU != null && _sbU.getNome() != null && !_sbU.getNome().isEmpty())
		? _sbU.getNome().substring(0, 1).toUpperCase()
		: "?"%>
			</div>
			<div style="overflow: hidden; flex: 1">
				<div class="user-name"><%=_sbU != null ? _sbU.getNome() : ""%></div>
				<span class="role-pill"><%=_perfilLabel%></span>
			</div>
			<%-- Botão de alternar tema claro/escuro. O ícone e o título
           mudam dinamicamente (ver script abaixo) conforme o tema
           ATUAL — sempre mostra o ícone do tema PARA O QUAL vai
           mudar se clicado (☀️ enquanto está escuro, 🌙 enquanto
           está claro), convenção comum nesse tipo de botão. --%>
			<button type="button" id="btnTema" class="theme-toggle-btn"
				title="Alternar tema" onclick="alternarTema()">🌙</button>
		</div>
		<a href="${pageContext.request.contextPath}/auth/logout"
			class="logout-link">Sair</a>
	</div>
</nav>

<script>
	(function() {
		// ============================================================
		// TEMA CLARO/ESCURO — aplicação e persistência (localStorage)
		// ============================================================
		// Roda como parte da própria sidebar (não do <head> de cada
		// página) — como a sidebar é o primeiro conteúdo de dentro do
		// <body> em toda tela logada, o "flash" do tema errado antes
		// deste script rodar fica bem pequeno na prática (só o instante
		// de renderizar o HTML acima desta tag <script>). Eliminar esse
		// flash por completo exigiria injetar um script equivalente no
		// <head> de cada uma das ~20 JSPs — não fizemos isso aqui para
		// não teres que editar todo arquivo de novo só por causa disso;
		// se um dia incomodar, é um ajuste rápido de replicar.

		var chave = 'tema'; // chave usada no localStorage do navegador

		function aplicarTema(tema) {
			document.documentElement.setAttribute('data-tema', tema);
			var btn = document.getElementById('btnTema');
			if (btn) {
				// Mostra o ícone do tema OPOSTO (o que o clique vai ativar)
				btn.textContent = tema === 'claro' ? '🌙' : '☀️';
				btn.title = tema === 'claro' ? 'Mudar para tema escuro'
						: 'Mudar para tema claro';
			}
		}

		window.alternarTema = function() {
			var atual = document.documentElement.getAttribute('data-tema') === 'claro' ? 'claro'
					: 'escuro';
			var novo = atual === 'claro' ? 'escuro' : 'claro';
			localStorage.setItem(chave, novo);
			aplicarTema(novo);
		};

		// Aplica a preferência salva (ou "escuro", o padrão do sistema,
		// se o usuário nunca tiver mexido no botão antes).
		aplicarTema(localStorage.getItem(chave) === 'claro' ? 'claro'
				: 'escuro');
	})();
</script>
