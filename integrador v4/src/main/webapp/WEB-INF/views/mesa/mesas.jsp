<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@ page import="br.com.restaurante.model.Usuario"%>
<%@ page import="br.com.restaurante.model.Mesa"%>
<%@ page import="java.time.Duration"%>
<%@ page import="java.time.LocalDateTime"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>
<%--
    ================================================================
    MESAS.JSP — GRID DE MESAS (v4 — visual alinhado ao Figma)
    ================================================================
   
--%>
<%
Usuario _u = (Usuario) session.getAttribute("usuarioLogado");
boolean _isGerente = "GERENTE".equals(_u.getPerfil());
%>
<!DOCTYPE html>
<html lang="pt-BR">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>Mesas — Integrador</title>
<%-- Auto-refresh a cada 30s — igual à Fila de Preparo e Meus Pedidos.
     Sem isso, um chamado de garçom só apareceria na próxima vez que
     alguém abrisse/recarregasse a tela manualmente, o que anula boa
     parte da utilidade do recurso (o objetivo é o funcionário notar
     o chamado rápido, não só quando "der de olhar" na tela). --%>
<meta http-equiv="refresh" content="30">
<link rel="stylesheet"
	href="${pageContext.request.contextPath}/assets/style.css">
<style>
/* ── Caixas-resumo grandes do topo (Livres/Ocupadas/Reservadas) ── */
.resumo-mesas {
	display: grid;
	grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
	gap: 16px;
	margin-bottom: 24px
}

.resumo-box {
	border-radius: var(--radius);
	padding: 20px 24px;
	display: flex;
	align-items: center;
	gap: 16px;
	border: 1px solid
}

.resumo-box .dot {
	width: 12px;
	height: 12px;
	border-radius: 50%;
	flex-shrink: 0
}

.resumo-box .num {
	font-size: 30px;
	font-weight: 800;
	line-height: 1
}

.resumo-box .label {
	font-size: 13px;
	color: var(--text-secondary);
	margin-top: 2px
}

.resumo-livre {
	background: var(--success-bg);
	border-color: var(--success-border)
}

.resumo-livre .dot {
	background: var(--success-color)
}

.resumo-ocupada {
	background: var(--error-bg);
	border-color: var(--error-border)
}

.resumo-ocupada .dot {
	background: var(--error-color)
}

.resumo-reservada {
	background: var(--warning-bg);
	border-color: var(--warning-border)
}

.resumo-reservada .dot {
	background: var(--warning-color)
}

/* ── Grid de cards de mesa ── */
.mesas-grid {
	display: grid;
	grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
	gap: 18px
}

.mesa-card {
	background: var(--bg-card);
	border: 1px solid var(--border-subtle);
	border-radius: var(--radius);
	padding: 20px;
	transition: var(--transition);
	position: relative
}

.mesa-card:hover {
	border-color: var(--border-subtle-2)
}

.mesa-card .status-dot {
	position: absolute;
	top: 20px;
	right: 20px;
	width: 10px;
	height: 10px;
	border-radius: 50%
}

.mesa-card.livre .status-dot {
	background: var(--success-color)
}

.mesa-card.ocupada .status-dot {
	background: var(--error-color)
}

.mesa-card.reservada .status-dot {
	background: var(--warning-color)
}

.mesa-numero {
	font-size: 20px;
	font-weight: 800;
	margin-bottom: 6px
}

.mesa-capacidade {
	font-size: 13px;
	color: var(--text-secondary);
	margin-bottom: 12px
}

.mesa-badge-status {
	margin-bottom: 10px
}

.mesa-info-extra {
	font-size: 12px;
	color: var(--text-muted);
	margin-bottom: 14px;
	min-height: 16px
}

/* Botões de ação: sempre no fundo do card, lado a lado quando são
   dois (grid-cols-2) ou ocupando a largura toda quando é só um. */
.mesa-acoes {
	display: grid;
	gap: 8px;
	margin-top: auto
}

.mesa-acoes.dois {
	grid-template-columns: 1fr 1fr
}

.mesa-card {
	display: flex;
	flex-direction: column
}

.mesa-acoes .btn {
	width: 100%
}

/* ── Alerta "chamando garçom" ── */
.mesa-card.chamando-garcom {
	border-color: #c2410c;
	box-shadow: 0 0 0 2px rgba(194, 65, 12, .35)
}

.alerta-chamado {
	background: #c2410c;
	color: #fff;
	padding: 8px 10px;
	border-radius: var(--radius-sm);
	font-size: 12px;
	font-weight: 700;
	text-align: center;
	margin-bottom: 10px;
	animation: pulse-chamado 1.4s ease-in-out infinite
}

@
keyframes pulse-chamado { 0%,100%{
	box-shadow: 0 0 0 0 rgba(194, 65, 12, .55)
}

50
%
{
box-shadow
:
0
0
0
7px
rgba(
194
,
65
,
12
,
0
)
}
}

/* ── Modal (idêntico ao anterior, só herdando o novo tema escuro) ── */
.modal .form-group label {
	color: var(--text-primary)
}
</style>
</head>
<body>
	<div class="main-container">
		<%@ include file="/WEB-INF/views/shared/_sidebar.jsp"%>
		<div class="main-content">
			<header class="topbar">
				<div class="topbar-left">
					<h2>Mesas</h2>
				</div>
				<div class="topbar-right">
					<%
					if (_isGerente) {
					%>
					<a href="${pageContext.request.contextPath}/app/mesas?acao=novo"
						class="btn btn-primary btn-sm">+ Nova Mesa</a>
					<%
					}
					%>
					<div class="user-info">
						<div class="user-avatar"><%=_u.getNome().substring(0, 1).toUpperCase()%></div>
						<div class="user-details">
							<span class="name"><%=_u.getNome()%></span> <span class="role"><%="GERENTE".equals(_u.getPerfil()) ? "Gerente" : "Atendente"%></span>
						</div>
					</div>
				</div>
			</header>
			<main class="content">

				<%-- Caixas-resumo grandes, no lugar dos antigos stat-cards
         pequenos — mesma proporção visual das 3 caixas "6 Livres /
         4 Ocupadas / 2 Reservadas" do Figma. --%>
				<div class="resumo-mesas">
					<div class="resumo-box resumo-livre">
						<span class="dot"></span>
						<div>
							<div class="num">${livres}</div>
							<div class="label">Livres</div>
						</div>
					</div>
					<div class="resumo-box resumo-ocupada">
						<span class="dot"></span>
						<div>
							<div class="num">${ocupadas}</div>
							<div class="label">Ocupadas</div>
						</div>
					</div>
					<div class="resumo-box resumo-reservada">
						<span class="dot"></span>
						<div>
							<div class="num">${reservadas}</div>
							<div class="label">Reservadas</div>
						</div>
					</div>
				</div>

				<c:choose>
					<c:when test="${empty mesas}">
						<div class="empty-state">
							<div class="icon">🪑</div>
							<p>Nenhuma mesa cadastrada.</p>
						</div>
					</c:when>
					<c:otherwise>
						<div class="mesas-grid">
							<c:forEach var="mesa" items="${mesas}">
								<%
								// Recupera o bean Mesa do escopo da página (o JSTL
								// c:forEach expõe a variável "mesa" também como
								// atributo de página, então dá pra buscá-la aqui de
								// dentro de um scriptlet comum) para calcular o tempo
								// decorrido desde a última mudança de status.
								Mesa _mesa = (Mesa) pageContext.getAttribute("mesa");
								String _tempoDecorrido = null;
								if (_mesa.getDataStatus() != null && !_mesa.isLivre()) {
									long minutos = Duration.between(_mesa.getDataStatus(), LocalDateTime.now()).toMinutes();
									if (minutos < 1)
										_tempoDecorrido = "agora mesmo";
									else if (minutos < 60)
										_tempoDecorrido = minutos + "min atrás";
									else
										_tempoDecorrido = (minutos / 60) + "h atrás";
								}
								%>
								<div
									class="mesa-card ${mesa.status} ${mesa.chamandoGarcom ? 'chamando-garcom' : ''}">
									<span class="status-dot"></span>
									<div class="mesa-numero">#${mesa.numero}</div>
									<div class="mesa-capacidade">👥 ${mesa.capacidade}
										lugares</div>

									<%-- Alerta de "chamando garçom" — aparece ACIMA do badge
                   de status normal, com destaque pulsante (mesma
                   animação já usada na linha do tempo de meus_pedidos.jsp)
                   para chamar atenção mesmo numa tela cheia de cards. --%>
									<c:if test="${mesa.chamandoGarcom}">
										<div class="alerta-chamado">
											🔔 Chamando garçom!
											<form method="POST"
												action="${pageContext.request.contextPath}/app/mesas"
												style="margin-top: 6px">
												<input type="hidden" name="acao" value="atenderChamado">
												<input type="hidden" name="id" value="${mesa.idMesa}">
												<button type="submit" class="btn btn-sm"
													style="width: 100%; background: #fff; color: #7c2d12">✓
													Atendido</button>
											</form>
										</div>
									</c:if>

									<div class="mesa-badge-status">
										<c:choose>
											<c:when test="${mesa.status=='livre'}">
												<span class="badge badge-success">Livre</span>
											</c:when>
											<c:when test="${mesa.status=='ocupada'}">
												<span class="badge badge-danger">Ocupada</span>
											</c:when>
											<c:when test="${mesa.status=='reservada'}">
												<span class="badge badge-warning">Reservada</span>
											</c:when>
										</c:choose>
									</div>

									<%-- Linha de contexto: tempo decorrido (ocupada) ou
                   nome de quem reservou (reservada) — vazio p/ livre. --%>
									<div class="mesa-info-extra">
										<c:if test="${mesa.status=='ocupada'}"><%=_tempoDecorrido != null ? _tempoDecorrido : ""%></c:if>
										<c:if test="${mesa.status=='reservada'}"><%=_mesa.getOperador() != null ? _mesa.getOperador() : ""%></c:if>
									</div>

									<%-- Ações — variam por status, igual ao Figma:
                   livre → dois botões (Abrir / Reservar)
                   ocupada → um botão (Fechar Mesa)
                   reservada → dois botões (Check-in / Cancelar) --%>
									<c:choose>
										<c:when test="${mesa.status=='livre'}">
											<div class="mesa-acoes dois">
												<button class="btn btn-primary btn-sm"
													onclick="abrirModal('abrirMesa',${mesa.idMesa},'Abrir Mesa')">Abrir</button>
												<button class="btn btn-warning btn-sm"
													onclick="abrirModal('reservar',${mesa.idMesa},'Reservar Mesa')">Reservar</button>
											</div>
										</c:when>
										<c:when test="${mesa.status=='ocupada'}">
											<div class="mesa-acoes">
												<button class="btn btn-danger btn-sm"
													onclick="abrirModal('fecharMesa',${mesa.idMesa},'Fechar Mesa')">Fechar
													Mesa</button>
											</div>
										</c:when>
										<c:when test="${mesa.status=='reservada'}">
											<div class="mesa-acoes dois">
												<%-- Check-in = cliente chegou → reaproveita a
                         ação abrirMesa (reservada → ocupada) --%>
												<button class="btn btn-primary btn-sm"
													onclick="abrirModal('abrirMesa',${mesa.idMesa},'Check-in')">Check-in</button>
												<%-- Cancelar reserva = reaproveita fecharMesa
                         (qualquer status → livre), já que não existe
                         uma ação "cancelarReserva" separada no
                         MesaController — funcionalmente é a mesma
                         coisa (libera a mesa). --%>
												<button class="btn btn-secondary btn-sm"
													onclick="abrirModal('fecharMesa',${mesa.idMesa},'Cancelar Reserva')">Cancelar</button>
											</div>
										</c:when>
									</c:choose>

									<a
										href="${pageContext.request.contextPath}/app/mesas?acao=detalhe&id=${mesa.idMesa}"
										class="btn btn-secondary btn-sm mt-10" style="width: 100%">📋
										Ver histórico</a>

									<%
									if (_isGerente) {
									%>
									<div style="display: flex; gap: 6px; margin-top: 6px">
										<a
											href="${pageContext.request.contextPath}/app/mesas?acao=editar&id=${mesa.idMesa}"
											class="btn-icon" title="Editar mesa" style="flex: 1">✏️</a>
										<form method="POST"
											action="${pageContext.request.contextPath}/app/mesas"
											onsubmit="return confirm('Remover a Mesa ${mesa.numero}?')"
											style="flex: 1">
											<input type="hidden" name="acao" value="excluir"> <input
												type="hidden" name="id" value="${mesa.idMesa}">
											<button type="submit" class="btn-icon danger"
												title="Excluir mesa" style="width: 100%">🗑️</button>
										</form>
									</div>
									<%
									}
									%>
								</div>
							</c:forEach>
						</div>
					</c:otherwise>
				</c:choose>
			</main>
		</div>
	</div>

	<div class="modal-overlay" id="modalOverlay">
		<div class="modal">
			<h3 id="modalTitulo">Identificação</h3>
			<form method="POST"
				action="${pageContext.request.contextPath}/app/mesas" id="formModal">
				<input type="hidden" name="acao" id="modalAcao"> <input
					type="hidden" name="id" id="modalId">
				<div class="form-group">
					<label for="operador">Seu identificador (ex: A1, A2)</label> <input
						type="text" id="operador" name="operador" placeholder="Ex: A1"
						maxlength="20" required autocomplete="off">
				</div>
				<div class="modal-acoes">
					<button type="button" class="btn btn-secondary"
						onclick="fecharModal()">Cancelar</button>
					<button type="submit" class="btn btn-primary">Confirmar</button>
				</div>
			</form>
		</div>
	</div>
	<script>
function abrirModal(acao,id,titulo){
  document.getElementById('modalAcao').value=acao;
  document.getElementById('modalId').value=id;
  document.getElementById('modalTitulo').textContent=titulo||'Confirmar';
  document.getElementById('operador').value='';
  document.getElementById('modalOverlay').classList.add('aberto');
  document.getElementById('operador').focus();
}
function fecharModal(){document.getElementById('modalOverlay').classList.remove('aberto');}
document.getElementById('modalOverlay').addEventListener('click',function(e){if(e.target===this)fecharModal();});
document.addEventListener('keydown',function(e){if(e.key==='Escape')fecharModal();});
</script>
</body>
</html>
