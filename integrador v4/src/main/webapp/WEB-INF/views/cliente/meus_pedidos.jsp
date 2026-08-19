<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@ page import="br.com.restaurante.model.Usuario"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt"%>
<%--
    ================================================================
    MEUS_PEDIDOS.JSP — HISTÓRICO / ACOMPANHAMENTO DO CLIENTE
    (v4 — linha do tempo horizontal estilo Figma)
    ================================================================
  
    ================================================================
--%>
<%
Usuario _u = (Usuario) session.getAttribute("usuarioLogado");
%>
<!DOCTYPE html>
<html lang="pt-BR">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>Meus Pedidos — Integrador</title>
<link rel="stylesheet"
	href="${pageContext.request.contextPath}/assets/style.css">

<%-- Auto-refresh a cada 30s para atualizar o status --%>
<meta http-equiv="refresh" content="30">

<style>
.refresh-bar {
	background: var(--bg-sidebar);
	color: var(--text-secondary);
	text-align: center;
	font-size: 12px;
	padding: 6px;
	position: sticky;
	top: 0;
	z-index: 200;
	border-bottom: 1px solid var(--border-subtle)
}

.refresh-bar span {
	font-weight: 700;
	color: var(--primary)
}

.refresh-bar a {
	color: var(--primary);
	text-decoration: none
}

.pedido-header {
	display: flex;
	justify-content: space-between;
	align-items: center;
	margin-bottom: 16px;
	flex-wrap: wrap;
	gap: 8px
}

.pedido-id {
	font-size: 16px;
	font-weight: 800
}

.pedido-itens {
	font-size: 13px;
	color: var(--text-secondary);
	background: var(--bg-card-hover);
	padding: 10px 14px;
	border-radius: var(--radius-sm);
	margin-bottom: 14px
}

.pedido-footer {
	display: flex;
	justify-content: space-between;
	align-items: center;
	flex-wrap: wrap;
	gap: 8px
}

.pedido-total {
	font-size: 15px;
	font-weight: 700;
	color: var(--success-color)
}

/* ================================================================
   LINHA DO TEMPO HORIZONTAL — círculos + linha conectora
   ================================================================
   Estrutura: uma <div class="timeline-h"> com N <div class="th-step">
   (um por etapa), cada um com um círculo (.th-dot) e um rótulo
   (.th-label) embaixo. A "linha" entre os círculos é desenhada com
   ::before/::after em cada .th-step (metade esquerda + metade
   direita da linha), coloridas de verde quando o passo ANTERIOR já
   foi concluído — assim a linha "enche" progressivamente conforme
   o pedido avança, sem precisar de nenhum SVG ou canvas. */
.timeline-h {
	display: flex;
	margin-bottom: 16px
}

.th-step {
	flex: 1;
	text-align: center;
	position: relative
}

.th-step:first-child {
	flex: 0 0 auto;
	text-align: left
}

.th-step:last-child {
	flex: 0 0 auto;
	text-align: right
}

.th-dot {
	width: 26px;
	height: 26px;
	border-radius: 50%;
	display: inline-flex;
	align-items: center;
	justify-content: center;
	font-size: 12px;
	font-weight: 800;
	background: var(--bg-card-hover);
	color: var(--text-muted);
	border: 2px solid var(--border-subtle-2);
	position: relative;
	z-index: 2
}

.th-step.feita .th-dot {
	background: var(--success-color);
	border-color: var(--success-color);
	color: #06240f
}

.th-step.ativa .th-dot {
	background: var(--success-color);
	border-color: var(--success-color);
	color: #06240f;
	animation: pulse-verde 1.6s ease-in-out infinite
}

@
keyframes pulse-verde { 0%,100%{
	box-shadow: 0 0 0 0 rgba(34, 197, 94, .55)
}

50
%
{
box-shadow
:
0
0
0
6px
rgba(
34
,
197
,
94
,
0
)
}
}

/* Linha conectora: metade que vem do passo anterior (::before) e
   metade que vai para o próximo (::after). Ligamos as duas metades
   de step vizinhos para formar uma linha contínua entre os círculos. */
.th-step:not(:first-child)::before {
	content: '';
	position: absolute;
	top: 13px;
	right: 50%;
	width: 100%;
	height: 2px;
	background: var(--border-subtle-2);
	z-index: 1
}

.th-step.linha-feita:not(:first-child)::before {
	background: var(--success-color)
}

.th-label {
	display: block;
	font-size: 11px;
	color: var(--text-secondary);
	margin-top: 6px;
	white-space: nowrap
}

.th-step.feita .th-label, .th-step.ativa .th-label {
	color: var(--text-primary);
	font-weight: 600
}
</style>
</head>
<body>
	<div class="main-container">
		<%@ include file="/WEB-INF/views/shared/_sidebar.jsp"%>
		<div class="main-content">

			<div class="refresh-bar">
				Atualizando em <span id="contador">30</span>s &nbsp;·&nbsp; <a
					href="">🔄 Atualizar agora</a>
			</div>

			<header class="topbar">
				<div class="topbar-left">
					<h2>🧾 Meus Pedidos</h2>
				</div>
				<div class="topbar-right">
					<a href="${pageContext.request.contextPath}/app/cliente/delivery"
						class="btn btn-primary btn-sm">+ Novo Delivery</a>
					<div class="user-info" style="margin-left: 8px">
						<div class="user-avatar"><%=_u.getNome().substring(0, 1).toUpperCase()%></div>
						<div class="user-details">
							<span class="name"><%=_u.getNome()%></span> <span class="role">Cliente</span>
						</div>
					</div>
				</div>
			</header>

			<main class="content">

				<c:if test="${not empty msgSucesso}">
					<div class="alert alert-success">✓ ${msgSucesso}</div>
				</c:if>

				<c:choose>
					<c:when test="${empty pedidos}">
						<div class="empty-state card">
							<div class="icon">🧾</div>
							<p>Você ainda não fez nenhum pedido.</p>
							<a href="${pageContext.request.contextPath}/app/cliente/delivery"
								class="btn btn-primary" style="margin-top: 16px">Fazer meu
								primeiro pedido</a>
						</div>
					</c:when>
					<c:otherwise>
						<c:forEach var="p" items="${pedidos}">
							<div class="card mb-20">

								<div class="pedido-header">
									<span class="pedido-id">Pedido #${p.idPedido}</span>
									<c:choose>
										<c:when test="${p.status=='aberto'}">
											<span class="badge badge-info">Recebido</span>
										</c:when>
										<c:when test="${p.status=='em_preparo'}">
											<span class="badge badge-warning">Preparando</span>
										</c:when>
										<c:when test="${p.status=='pronto'}">
											<span class="badge badge-success">Pronto</span>
										</c:when>
										<c:when test="${p.status=='entregue'}">
											<span class="badge"
												style="background: var(--bg-card-hover); color: var(--text-secondary)">Entregue</span>
										</c:when>
										<c:when test="${p.status=='cancelado'}">
											<span class="badge badge-danger">Cancelado</span>
										</c:when>
									</c:choose>
								</div>

								<%-- Linha do tempo horizontal (círculos + linha conectora)
                 — pulada para pedidos cancelados, igual à versão
                 anterior. Cada c:set abaixo classifica a etapa como
                 "feita" (já passou), "ativa" (é a etapa atual, ganha
                 o pulso verde) ou nem uma coisa nem outra (futura). --%>
								<c:if test="${p.status != 'cancelado'}">
									<%-- Cada passo é resolvido explicitamente a partir do
                   status real do pedido — sem hacks de substring —
                   para não confundir "ativa" com "feita" (evita, por
                   exemplo, que "em_preparo" ativo seja lido como
                   concluído só porque um passo futuro também usa a
                   palavra "feita" em outro lugar da classe). --%>
									<c:set var="passo2Feita"
										value="${p.status=='pronto' || p.status=='entregue'}" />
									<c:set var="passo2Ativa" value="${p.status=='em_preparo'}" />
									<c:set var="passo3Feita" value="${p.status=='entregue'}" />
									<c:set var="passo3Ativa" value="${p.status=='pronto'}" />
									<c:set var="passo4Ativa" value="${p.status=='entregue'}" />
									<div class="timeline-h">
										<div class="th-step feita">
											<span class="th-dot">✓</span><span class="th-label">Recebido</span>
										</div>
										<div
											class="th-step ${passo2Feita ? 'feita linha-feita' : (passo2Ativa ? 'ativa linha-feita' : '')}">
											<span class="th-dot"><c:choose>
													<c:when test="${passo2Feita}">✓</c:when>
													<c:otherwise>2</c:otherwise>
												</c:choose></span> <span class="th-label">Em Preparo</span>
										</div>
										<div
											class="th-step ${passo3Feita ? 'feita linha-feita' : (passo3Ativa ? 'ativa linha-feita' : '')}">
											<span class="th-dot"><c:choose>
													<c:when test="${passo3Feita}">✓</c:when>
													<c:otherwise>3</c:otherwise>
												</c:choose></span> <span class="th-label">Pronto</span>
										</div>
										<div class="th-step ${passo4Ativa ? 'ativa linha-feita' : ''}">
											<span class="th-dot"><c:choose>
													<c:when test="${p.status=='entregue'}">✓</c:when>
													<c:otherwise>4</c:otherwise>
												</c:choose></span> <span class="th-label">Entregue</span>
										</div>
									</div>
								</c:if>

								<div class="pedido-itens">
									<c:forEach var="item" items="${p.itens}" varStatus="vs">
                ${item.quantidade}x ${item.nomeItem}<c:if
											test="${!vs.last}"> · </c:if>
									</c:forEach>
								</div>

								<div class="pedido-footer">
									<div>
										<span class="pedido-total"> R$ <fmt:formatNumber
												value="${p.calcularTotal()}" minFractionDigits="2"
												maxFractionDigits="2" />
										</span> <span class="text-muted"
											style="font-size: 12px; margin-left: 10px">
											${p.dataAberturaFormatada} </span>
									</div>
									<c:if test="${not empty p.observacao}">
										<span style="font-size: 12px; color: var(--text-secondary)">📝
											${p.observacao}</span>
									</c:if>
								</div>

							</div>
						</c:forEach>
					</c:otherwise>
				</c:choose>

			</main>
		</div>
	</div>

	<script>
var seg = 30;
setInterval(function() {
  seg--;
  document.getElementById('contador').textContent = seg;
  if (seg <= 0) location.reload();
}, 1000);
</script>
</body>
</html>
