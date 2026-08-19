<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@ page import="br.com.restaurante.model.Usuario"%>
<%@ page import="java.math.BigDecimal"%>
<%@ page import="java.util.Map"%>
<%@ page import="java.util.Locale"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt"%>
<%@ taglib prefix="fn" uri="jakarta.tags.functions"%>
<%--
    ================================================================
    DASHBOARD.JSP — PAINEL DO GERENTE (v5 — gráficos reais + limpeza)
    ================================================================
  
    COMO OS GRÁFICOS SÃO DESENHADOS (sem biblioteca JS):
    Não há nenhuma lib de gráficos (Chart.js, Recharts...) instalada
    neste projeto JSP puro, então os dois gráficos são <svg> puro,
    com as coordenadas calculadas em um scriptlet logo abaixo — a
    mesma técnica de "cálculo derivado no servidor" já usada em
    mesas.jsp (tempo decorrido) e RelatorioPDF.java (montagem do PDF).
      - Linha "Receita da Semana": um <polyline> ligando 7 pontos,
        com o eixo Y arredondado para múltiplos "redondos" (o mesmo
        efeito visual do "R$2.0k, R$4.0k..." do Figma).
      - Rosca "Vendas por Categoria": 3 <circle> concêntricos com
        stroke-dasharray/stroke-dashoffset — uma técnica clássica de
        SVG para desenhar fatias de rosca sem precisar calcular arcos
        de path manualmente (só precisa da % de cada fatia).
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
<title>Painel — Integrador</title>
<link rel="stylesheet"
	href="${pageContext.request.contextPath}/assets/style.css">
<style>
.graficos-row {
	display: grid;
	grid-template-columns: 2fr 1fr;
	gap: 20px;
	margin-bottom: 24px;
	align-items: stretch
}

@media ( max-width :900px) {
	.graficos-row {
		grid-template-columns: 1fr
	}
}

.chart-eixo-label {
	font-size: 10px;
	fill: var(--text-muted);
	font-family: inherit
}

.chart-grid-line {
	stroke: var(--border-subtle);
	stroke-width: 1
}

.chart-linha {
	fill: none;
	stroke: var(--primary);
	stroke-width: 2.5
}

.chart-area {
	fill: url(#gradienteReceita);
	opacity: .25
}

.rosca-wrap {
	display: flex;
	align-items: center;
	gap: 20px;
	justify-content: center;
	height: 100%
}

.legenda-setores {
	display: flex;
	flex-direction: column;
	gap: 10px;
	font-size: 13px
}

.legenda-item {
	display: flex;
	align-items: center;
	gap: 8px
}

.legenda-item .dot {
	width: 9px;
	height: 9px;
	border-radius: 50%;
	flex-shrink: 0
}

.legenda-item .pct {
	margin-left: auto;
	font-weight: 700;
	color: var(--text-primary);
	padding-left: 14px
}
</style>
</head>
<body>
	<div class="main-container">
		<%@ include file="/WEB-INF/views/shared/_sidebar.jsp"%>
		<div class="main-content">
			<header class="topbar">
				<div class="topbar-left">
					<h2>Painel</h2>
				</div>
				<div class="topbar-right">
					<div class="user-info">
						<div class="user-avatar"><%=_u.getNome().substring(0, 1).toUpperCase()%></div>
						<div class="user-details">
							<span class="name"><%=_u.getNome()%></span> <span class="role">Gerente</span>
						</div>
					</div>
				</div>
			</header>
			<main class="content">
				<c:if test="${not empty erro}">
					<div class="alert alert-error">${erro}</div>
				</c:if>

				<%-- 4 stat-cards, agora incluindo Ticket Médio (dado real, vindo
         de ticketMedioHoje = totalHoje ÷ qtdPedidosHoje calculado no
         Controller) — bate com o layout de 4 cards do Figma. --%>
				<div class="cards-grid">
					<div class="stat-card">
						<div class="icon-box verde">🪑</div>
						<div class="stat-label">Mesas Livres</div>
						<div class="stat-value">${mesasLivres}<span
								style="font-size: 15px; color: var(--text-secondary)">/${totalMesas}</span>
						</div>
						<div class="stat-context">${mesasOcupadas}ocupadas</div>
					</div>
					<div class="stat-card">
						<div class="icon-box ambar">🧾</div>
						<div class="stat-label">Pedidos Ativos</div>
						<div class="stat-value">${pedidosAbertos}</div>
						<div class="stat-context">em andamento</div>
					</div>
					<div class="stat-card">
						<div class="icon-box azul">💰</div>
						<div class="stat-label">Faturamento Hoje</div>
						<div class="stat-value" style="font-size: 22px">
							R$
							<fmt:formatNumber value="${totalHoje}" minFractionDigits="2"
								maxFractionDigits="2" />
						</div>
						<div class="stat-context">desde a abertura</div>
					</div>
					<div class="stat-card">
						<div class="icon-box roxo">💳</div>
						<div class="stat-label">Ticket Médio</div>
						<div class="stat-value" style="font-size: 22px">
							R$
							<fmt:formatNumber value="${ticketMedioHoje}"
								minFractionDigits="2" maxFractionDigits="2" />
						</div>
						<div class="stat-context">${qtdPedidosHoje}pedido(s) hoje</div>
					</div>
				</div>

				<%
				// ============================================================
				// SCRIPTLET — cálculo geométrico dos dois gráficos SVG
				// ============================================================
				// Tudo aqui é derivado puramente dos Maps que o
				// DashboardController já publicou como atributos de request
				// (receitaPorDia, vendasPorSetor) — nenhum dado é inventado
				// nesta camada, só transformado em coordenadas de desenho.

				@SuppressWarnings("unchecked")
				Map<String, BigDecimal> _receita = (Map<String, BigDecimal>) request.getAttribute("receitaPorDia");
				@SuppressWarnings("unchecked")
				Map<String, BigDecimal> _vendasSetor = (Map<String, BigDecimal>) request.getAttribute("vendasPorSetor");

				// ---------- GRÁFICO DE LINHA: "Receita da Semana" ----------
				int _chartW = 620, _chartH = 210, _padL = 50, _padB = 24, _padT = 12, _padR = 12;
				int _plotW = _chartW - _padL - _padR;
				int _plotH = _chartH - _padT - _padB;

				BigDecimal _max = BigDecimal.ZERO;
				if (_receita != null) {
					for (BigDecimal v : _receita.values())
						if (v.compareTo(_max) > 0)
					_max = v;
				}

				// Arredonda o teto do eixo Y para um múltiplo "redondo" (passo
				// que dobra a partir de 1000: 1000, 2000, 4000, 8000...) até
				// cobrir o maior valor — dá as legendas limpas tipo "R$4.0k".
				long _passoY = 1000;
				while (_passoY * 4 < _max.doubleValue())
					_passoY *= 2;
				double _chartMax = _passoY * 4.0;

				StringBuilder _pontosLinha = new StringBuilder();
				StringBuilder _pontosArea = new StringBuilder();
				StringBuilder _marcadores = new StringBuilder();
				StringBuilder _rotulosX = new StringBuilder();

				if (_receita != null && !_receita.isEmpty()) {
					int _n = _receita.size(), _i = 0;
					for (Map.Entry<String, BigDecimal> _e : _receita.entrySet()) {
						double _x = _padL + (_n <= 1 ? 0 : (_plotW * _i / (double) (_n - 1)));
						double _y = _padT + _plotH - (_e.getValue().doubleValue() / _chartMax * _plotH);
						String _xs = String.format(Locale.US, "%.1f", _x);
						String _ys = String.format(Locale.US, "%.1f", _y);

						_pontosLinha.append(_i == 0 ? "" : " ").append(_xs).append(",").append(_ys);
						_pontosArea.append(_xs).append(",").append(_ys).append(" ");

						_marcadores.append("<circle cx='").append(_xs).append("' cy='").append(_ys)
						.append("' r='4' fill='#22c55e' stroke='#161b25' stroke-width='2'/>");

						_rotulosX.append("<text x='").append(_xs).append("' y='").append(_chartH - 4)
						.append("' text-anchor='middle' class='chart-eixo-label'>").append(_e.getKey()).append("</text>");
						_i++;
					}
				}

				// Fecha o polígono da área sombreada embaixo da linha (desce
				// até a base do gráfico e volta pelo eixo X).
				String _areaPath = "";
				if (_pontosArea.length() > 0) {
					_areaPath = _pontosArea.toString()
					+ String.format(Locale.US, "%.1f,%.1f ", (double) (_chartW - _padR), (double) (_padT + _plotH))
					+ String.format(Locale.US, "%.1f,%.1f", (double) _padL, (double) (_padT + _plotH));
				}

				// Linhas de grade horizontais + rótulos do eixo Y (0, 1x, 2x, 3x, 4x o passo)
				StringBuilder _gradeY = new StringBuilder();
				for (int _g = 0; _g <= 4; _g++) {
					double _valor = _passoY * _g;
					double _y = _padT + _plotH - (_valor / _chartMax * _plotH);
					String _ys = String.format(Locale.US, "%.1f", _y);
					_gradeY.append("<line x1='").append(_padL).append("' y1='").append(_ys).append("' x2='").append(_chartW - _padR)
					.append("' y2='").append(_ys).append("' class='chart-grid-line'/>");
					_gradeY.append("<text x='").append(_padL - 8).append("' y='").append(_ys)
					.append("' text-anchor='end' dominant-baseline='middle' class='chart-eixo-label'>").append("R$")
					.append(String.format(Locale.US, "%.1f", _valor / 1000.0)).append("k</text>");
				}

				// ---------- GRÁFICO DE ROSCA: "Vendas por Categoria" ----------
				BigDecimal _totalSetor = BigDecimal.ZERO;
				if (_vendasSetor != null) {
					for (BigDecimal v : _vendasSetor.values())
						_totalSetor = _totalSetor.add(v);
				}

				String[] _coresSetor = { "#22c55e", "#3b82f6", "#f59e0b" }; // Pratos / Bebidas / Sobremesas
				double _raio = 62, _espessura = 20, _cx = 74, _cy = 74;
				double _circunferencia = 2 * Math.PI * _raio;
				double _acumulado = 0;
				StringBuilder _fatias = new StringBuilder();
				StringBuilder _legendaSetor = new StringBuilder();

				if (_vendasSetor != null) {
					int _si = 0;
					for (Map.Entry<String, BigDecimal> _e : _vendasSetor.entrySet()) {
						double _pct = _totalSetor.compareTo(BigDecimal.ZERO) > 0
						? _e.getValue().doubleValue() / _totalSetor.doubleValue()
						: 0;
						double _tamanhoArco = _pct * _circunferencia;
						String _cor = _coresSetor[_si % _coresSetor.length];

						_fatias.append("<circle cx='").append(_cx).append("' cy='").append(_cy).append("' r='").append(_raio)
						.append("' fill='none' stroke='").append(_cor).append("' stroke-width='").append(_espessura)
						.append("' stroke-dasharray='").append(String.format(Locale.US, "%.2f", _tamanhoArco)).append(" ")
						.append(String.format(Locale.US, "%.2f", _circunferencia - _tamanhoArco))
						.append("' stroke-dashoffset='").append(String.format(Locale.US, "%.2f", -_acumulado))
						.append("' transform='rotate(-90 ").append(_cx).append(" ").append(_cy).append(")'/>");
						_acumulado += _tamanhoArco;

						long _pctArred = Math.round(_pct * 100);
						_legendaSetor.append("<div class='legenda-item'><span class='dot' style='background:").append(_cor)
						.append("'></span>").append(_e.getKey()).append("<span class='pct'>").append(_pctArred)
						.append("%</span></div>");
						_si++;
					}
				}

				// _totalSetor é uma variável LOCAL do scriptlet — o EL (${...})
				// não enxerga variáveis Java soltas, só atributos de escopo
				// (request/session/page). Por isso publicamos o resultado da
				// comparação como atributo de página antes de usá-lo lá embaixo
				// no c:choose (${totalSetorZero}), em vez de tentar referenciar
				// "_totalSetor" diretamente dentro de uma expressão EL — isso
				// silenciosamente resolveria para null/false e quebraria a
				// checagem de "sem dados ainda" da rosca.
				pageContext.setAttribute("totalSetorZero", _totalSetor.compareTo(BigDecimal.ZERO) == 0);
				%>

				<div class="graficos-row">

					<div class="card">
						<h3>Receita da Semana</h3>
						<c:choose>
							<c:when test="${empty receitaPorDia}">
								<div class="empty-state">
									<div class="icon">📈</div>
									<p>Sem dados de vendas ainda.</p>
								</div>
							</c:when>
							<c:otherwise>
								<svg viewBox="0 0 <%=_chartW%> <%=_chartH%>"
									style="width: 100%; height: auto">
              <defs>
                <linearGradient id="gradienteReceita" x1="0" y1="0"
										x2="0" y2="1">
                  <stop offset="0%" stop-color="#22c55e" />
                  <stop offset="100%" stop-color="#22c55e"
										stop-opacity="0" />
                </linearGradient>
              </defs>
              <%=_gradeY%>
              <polygon points="<%=_areaPath%>" class="chart-area" />
              <polyline points="<%=_pontosLinha%>" class="chart-linha" />
              <%=_marcadores%>
              <%=_rotulosX%>
            </svg>
							</c:otherwise>
						</c:choose>
					</div>

					<div class="card">
						<h3>Vendas por Categoria</h3>
						<c:choose>
							<c:when test="${empty vendasPorSetor || totalSetorZero}">
								<div class="empty-state">
									<div class="icon">🍩</div>
									<p>Sem dados de vendas ainda.</p>
								</div>
							</c:when>
							<c:otherwise>
								<div class="rosca-wrap">
									<svg viewBox="0 0 148 148" width="140" height="140">
                <%=_fatias%>
              </svg>
									<div class="legenda-setores"><%=_legendaSetor%></div>
								</div>
							</c:otherwise>
						</c:choose>
					</div>

				</div>

				<div class="card">
					<h3>Últimos Pedidos</h3>
					<c:choose>
						<c:when test="${empty ultimosPedidos}">
							<div class="empty-state">
								<div class="icon">🎉</div>
								<p>Nenhum pedido em aberto.</p>
							</div>
						</c:when>
						<c:otherwise>
							<div class="table-wrapper">
								<table>
									<thead>
										<tr>
											<th>Pedido</th>
											<th>Cliente</th>
											<th>Itens</th>
											<th>Status</th>
											<th>Total</th>
											<th>Hora</th>
										</tr>
									</thead>
									<tbody>
										<c:forEach var="p" items="${ultimosPedidos}">
											<tr>
												<td><strong>#${p.idPedido}</strong> <c:if
														test="${p.urgente}">
														<span class="badge badge-urgente" style="margin-left: 6px">Urgente</span>
													</c:if></td>
												<td><c:choose>
														<c:when test="${not empty p.mesa}">Mesa ${p.mesa.numero}</c:when>
														<c:otherwise>${p.identificadorOperador}</c:otherwise>
													</c:choose></td>
												<td>${fn:length(p.itens)}itens</td>
												<td><c:choose>
														<c:when test="${p.status=='aberto'}">
															<span class="badge badge-info">Recebido</span>
														</c:when>
														<c:when test="${p.status=='em_preparo'}">
															<span class="badge badge-warning">Em preparo</span>
														</c:when>
														<c:when test="${p.status=='pronto'}">
															<span class="badge badge-success">Pronto</span>
														</c:when>
														<c:otherwise>
															<span class="badge">${p.status}</span>
														</c:otherwise>
													</c:choose></td>
												<td style="color: var(--success-color); font-weight: 700">
													R$ <fmt:formatNumber value="${p.calcularTotal()}"
														minFractionDigits="2" maxFractionDigits="2" />
												</td>
												<td class="text-muted">${p.dataAberturaFormatada}</td>
											</tr>
										</c:forEach>
									</tbody>
								</table>
							</div>
							<c:if test="${pedidosAbertos > 8}">
								<div class="text-center mt-10">
									<a href="${pageContext.request.contextPath}/app/pedidos"
										class="btn btn-secondary btn-sm">Ver todos os
										${pedidosAbertos} pedidos</a>
								</div>
							</c:if>
						</c:otherwise>
					</c:choose>
				</div>
			</main>
		</div>
	</div>
</body>
</html>
