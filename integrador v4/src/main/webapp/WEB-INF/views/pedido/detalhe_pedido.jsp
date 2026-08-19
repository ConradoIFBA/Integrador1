<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@ page import="br.com.restaurante.model.Usuario"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt"%>
<%--
    ================================================================
    DETALHE_PEDIDO.JSP — DETALHE DE UM PEDIDO (v4 — tema escuro)
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
<title>Pedido #${pedido.idPedido} — Integrador</title>
<link rel="stylesheet"
	href="${pageContext.request.contextPath}/assets/style.css">
<style>
.detalhe-grid {
	display: grid;
	grid-template-columns: 1fr 1fr;
	gap: 24px
}

@media ( max-width :900px) {
	.detalhe-grid {
		grid-template-columns: 1fr
	}
}

.info-row {
	display: flex;
	justify-content: space-between;
	padding: 9px 0;
	border-bottom: 1px solid var(--border-subtle);
	font-size: 14px
}

.info-row:last-child {
	border-bottom: none
}

.info-label {
	color: var(--text-secondary);
	font-weight: 600
}

.info-valor {
	font-weight: 500
}

.linha-total {
	display: flex;
	justify-content: space-between;
	padding: 14px 0 0;
	font-size: 18px;
	font-weight: 800;
	border-top: 1px solid var(--border-subtle-2);
	margin-top: 8px
}

/* ── Linhas dinâmicas de pagamento (mesmo padrão de pedidos.jsp) ── */
.linha-pagamento {
	display: grid;
	grid-template-columns: 1fr 110px auto;
	gap: 8px;
	margin-bottom: 8px;
	align-items: center
}

.linha-pagamento select, .linha-pagamento input {
	padding: 9px 10px;
	border: 1.5px solid var(--border-subtle-2);
	border-radius: var(--radius-sm);
	font-size: 13px;
	font-family: inherit;
	background: var(--bg-input);
	color: var(--text-primary)
}

.linha-pagamento select:focus, .linha-pagamento input:focus {
	outline: none;
	border-color: var(--primary)
}

.linha-pagamento .btn-remover {
	width: 32px;
	height: 32px;
	border-radius: var(--radius-sm);
	border: 1px solid var(--border-subtle-2);
	background: var(--bg-card-hover);
	color: var(--text-secondary);
	cursor: pointer;
	font-size: 13px;
	display: flex;
	align-items: center;
	justify-content: center
}

.linha-pagamento .btn-remover:hover {
	color: var(--error-color);
	border-color: var(--error-color)
}

.linha-pagamento .btn-remover:disabled {
	opacity: .3;
	cursor: not-allowed
}
</style>
</head>
<body>
	<div class="main-container">
		<%@ include file="/WEB-INF/views/shared/_sidebar.jsp"%>
		<div class="main-content">
			<header class="topbar">
				<div class="topbar-left">
					<h2>Pedido #${pedido.idPedido}</h2>
				</div>
				<div class="topbar-right">
					<a href="${pageContext.request.contextPath}/app/pedidos"
						class="btn btn-secondary btn-sm">← Voltar</a>
				</div>
			</header>
			<main class="content">
				<div class="detalhe-grid">

					<div class="card">
						<h3>📋 Informações</h3>
						<div class="info-row">
							<span class="info-label">Tipo</span> <span class="info-valor">
								<c:choose>
									<c:when test="${pedido.tipo=='mesa'}">🪑 Mesa ${pedido.mesa.numero}</c:when>
									<c:otherwise>🛵 Delivery</c:otherwise>
								</c:choose>
							</span>
						</div>
						<div class="info-row">
							<span class="info-label">Status</span> <span class="info-valor">
								<c:choose>
									<c:when test="${pedido.status=='aberto'}">
										<span class="badge badge-info">Recebido</span>
									</c:when>
									<c:when test="${pedido.status=='em_preparo'}">
										<span class="badge badge-warning">Em preparo</span>
									</c:when>
									<c:when test="${pedido.status=='pronto'}">
										<span class="badge badge-success">Pronto</span>
									</c:when>
									<c:when test="${pedido.status=='entregue'}">
										<span class="badge badge-success">Entregue ✓</span>
									</c:when>
									<c:when test="${pedido.status=='cancelado'}">
										<span class="badge badge-danger">Cancelado</span>
									</c:when>
									<c:otherwise>
										<span class="badge">${pedido.status}</span>
									</c:otherwise>
								</c:choose>
							</span>
						</div>
						<div class="info-row">
							<span class="info-label">Operador</span> <span class="info-valor">${pedido.identificadorOperador}</span>
						</div>
						<div class="info-row">
							<span class="info-label">Abertura</span> <span class="info-valor">${pedido.dataAberturaFormatada}</span>
						</div>
						<div class="info-row">
							<span class="info-label">Urgente</span> <span class="info-valor">
								<c:choose>
									<c:when test="${pedido.urgente}">
										<span class="badge badge-urgente">Sim</span>
									</c:when>
									<c:otherwise>Não</c:otherwise>
								</c:choose>
							</span>
						</div>
						<c:if test="${not empty pedido.observacao}">
							<div class="info-row">
								<span class="info-label">Observação</span> <span
									class="info-valor">${pedido.observacao}</span>
							</div>
						</c:if>
					</div>

					<div class="card">
						<h3>🍽️ Itens</h3>
						<div class="table-wrapper">
							<table>
								<thead>
									<tr>
										<th>Item</th>
										<th>Qtd</th>
										<th>Unit.</th>
										<th>Subtotal</th>
									</tr>
								</thead>
								<tbody>
									<c:forEach var="item" items="${pedido.itens}">
										<tr>
											<td>${item.nomeItem} <c:if
													test="${not empty item.observacao}">
													<br>
													<small class="text-muted">${item.observacao}</small>
												</c:if>
											</td>
											<td>${item.quantidade}</td>
											<td>R$ <fmt:formatNumber value="${item.precoUnitario}"
													minFractionDigits="2" maxFractionDigits="2" /></td>
											<td style="font-weight: 700; color: var(--success-color)">
												R$ <fmt:formatNumber value="${item.subtotal}"
													minFractionDigits="2" maxFractionDigits="2" />
											</td>
										</tr>
									</c:forEach>
								</tbody>
							</table>
						</div>
						<div class="linha-total">
							<span>Total do Pedido</span> <span
								style="color: var(--success-color)"> R$ <fmt:formatNumber
									value="${pedido.calcularTotal()}" minFractionDigits="2"
									maxFractionDigits="2" />
							</span>
						</div>
					</div>

				</div>

				<c:if test="${not empty pagamentos || pedido.status == 'entregue'}">
					<div class="card" style="margin-top: 24px">
						<h3>💳 Pagamentos</h3>
						<c:choose>
							<c:when test="${empty pagamentos}">
								<p class="text-muted">Nenhum pagamento registrado ainda.</p>
							</c:when>
							<c:otherwise>
								<div class="table-wrapper">
									<table>
										<thead>
											<tr>
												<th>Forma</th>
												<th>Valor</th>
												<th>Operador</th>
												<th>Horário</th>
											</tr>
										</thead>
										<tbody>
											<c:forEach var="pag" items="${pagamentos}">
												<tr>
													<td><c:choose>
															<c:when test="${pag.formaPagamento=='dinheiro'}">💵 Dinheiro</c:when>
															<c:when test="${pag.formaPagamento=='cartao'}">💳 Cartão</c:when>
															<c:when test="${pag.formaPagamento=='pix'}">📱 PIX</c:when>
															<c:otherwise>${pag.formaPagamento}</c:otherwise>
														</c:choose></td>
													<td style="font-weight: 700; color: var(--success-color)">
														R$ <fmt:formatNumber value="${pag.valor}"
															minFractionDigits="2" maxFractionDigits="2" />
													</td>
													<td>${pag.identificadorOperador}</td>
													<td class="text-muted">${pag.dataPagamentoFormatada}</td>
												</tr>
											</c:forEach>
										</tbody>
									</table>
								</div>
								<div class="linha-total">
									<span>Total Pago</span> <span
										style="color: var(--success-color)"> R$ <fmt:formatNumber
											value="${totalPago}" minFractionDigits="2"
											maxFractionDigits="2" />
									</span>
								</div>
							</c:otherwise>
						</c:choose>
					</div>
				</c:if>

				<c:if
					test="${pedido.status!='entregue' && pedido.status!='cancelado'}">
					<div class="card" style="margin-top: 24px">
						<h3>⚡ Ações</h3>
						<div class="d-flex gap-10">

							<c:choose>
								<c:when test="${pedido.status == 'pronto'}">
									<button type="button" class="btn btn-primary"
										onclick="abrirModalPagamento()">🍽️ Registrar Entrega
										+ Pagamento</button>
								</c:when>
								<c:otherwise>
									<form method="POST"
										action="${pageContext.request.contextPath}/app/pedidos">
										<input type="hidden" name="acao" value="avancarStatus">
										<input type="hidden" name="id" value="${pedido.idPedido}">
										<input type="hidden" name="operador"
											value="<%= _u.getLogin() %>">
										<button type="submit" class="btn btn-primary">
											<c:choose>
												<c:when test="${pedido.status=='aberto'}">▶ Iniciar preparo</c:when>
												<c:when test="${pedido.status=='em_preparo'}">✓ Marcar como pronto</c:when>
											</c:choose>
										</button>
									</form>
								</c:otherwise>
							</c:choose>

							<form method="POST"
								action="${pageContext.request.contextPath}/app/pedidos"
								onsubmit="return confirm('Cancelar este pedido?')">
								<input type="hidden" name="acao" value="cancelar"> <input
									type="hidden" name="id" value="${pedido.idPedido}"> <input
									type="hidden" name="operador" value="<%= _u.getLogin() %>">
								<button type="submit" class="btn btn-danger">✕ Cancelar
									pedido</button>
							</form>

						</div>
					</div>
				</c:if>

			</main>
		</div>
	</div>

	<%-- Modal de entrega + pagamento — agora com SUPORTE A MÚLTIPLAS
     FORMAS DE PAGAMENTO no mesmo pedido (mesmo padrão de
     pedidos.jsp — ver comentário lá para a explicação completa de
     como o Controller lê as linhas). Como aqui o id e o total do
     pedido já são conhecidos no servidor (${pedido.idPedido},
     ${pedido.calcularTotal()}), não precisa de JS pra preencher
     campo oculto nenhum — só pra montar/gerenciar as linhas. --%>
	<div class="modal-overlay" id="modalPagamento">
		<div class="modal" style="max-width: 420px">
			<h3>🍽️ Registrar Entrega</h3>
			<form method="POST"
				action="${pageContext.request.contextPath}/app/pedidos"
				onsubmit="return validarFormaPagamentoDetalhe()">
				<input type="hidden" name="acao" value="avancarStatus"> <input
					type="hidden" name="id" value="${pedido.idPedido}">

				<div class="form-group">
					<label>Seu identificador</label> <input type="text" name="operador"
						placeholder="Ex: A1" maxlength="20" required>
				</div>

				<div class="form-group" style="margin-bottom: 6px">
					<label
						style="display: flex; justify-content: space-between; align-items: center">
						<span>Pagamento</span> <span
						style="font-weight: 400; text-transform: none; color: var(--text-secondary)">
							Total: <strong style="color: var(--success-color)"> R$ <fmt:formatNumber
									value="${pedido.calcularTotal()}" minFractionDigits="2"
									maxFractionDigits="2" />
						</strong>
					</span>
					</label>
				</div>

				<div id="linhasPagamentoDetalhe"></div>

				<button type="button" class="btn btn-secondary btn-sm"
					onclick="adicionarLinhaPagamentoDetalhe()"
					style="width: 100%; margin-bottom: 16px">+ Adicionar forma
					de pagamento</button>

				<div class="modal-acoes">
					<button type="button" class="btn btn-secondary"
						onclick="fecharModalPagamento()">Cancelar</button>
					<button type="submit" class="btn btn-primary">Confirmar
						entrega</button>
				</div>
			</form>
		</div>
	</div>

	<script>
// Total do pedido, já vindo pronto do servidor (mesmo valor exibido
// no cabeçalho do modal) — usado só para pré-preencher a primeira
// linha de pagamento, sem precisar reformatar nada em JS.
// EL (${...}) e não scriptlet porque "pedido" só existe como
// atributo de request, não como variável Java solta neste ponto.
var detalheTotalPedido = ${pedido.calcularTotal()};

function abrirModalPagamento() {
  document.getElementById('linhasPagamentoDetalhe').innerHTML = '';
  adicionarLinhaPagamentoDetalhe(detalheTotalPedido.toFixed(2));
  document.getElementById('modalPagamento').classList.add('aberto');
}
function fecharModalPagamento() {
  document.getElementById('modalPagamento').classList.remove('aberto');
}

function adicionarLinhaPagamentoDetalhe(valorSugerido) {
  var cont = document.getElementById('linhasPagamentoDetalhe');
  var div = document.createElement('div');
  div.className = 'linha-pagamento';
  div.innerHTML =
    '<select name="formaPagamento">' +
    '  <option value="">Selecione...</option>' +
    '  <option value="dinheiro">💵 Dinheiro</option>' +
    '  <option value="cartao">💳 Cartão</option>' +
    '  <option value="pix">📱 PIX</option>' +
    '</select>' +
    '<input type="number" name="valorPagamento" placeholder="0,00" step="0.01" min="0"' +
    (valorSugerido ? ' value="' + valorSugerido + '"' : '') + '>' +
    '<button type="button" class="btn-remover" onclick="removerLinhaPagamentoDetalhe(this)" title="Remover">✕</button>';
  cont.appendChild(div);
  atualizarBotoesRemoverDetalhe();
}

function removerLinhaPagamentoDetalhe(btn) {
  var cont = document.getElementById('linhasPagamentoDetalhe');
  if (cont.children.length <= 1) return;
  btn.closest('.linha-pagamento').remove();
  atualizarBotoesRemoverDetalhe();
}

function atualizarBotoesRemoverDetalhe() {
  var linhas = document.querySelectorAll('#linhasPagamentoDetalhe .linha-pagamento');
  linhas.forEach(function(l) {
    l.querySelector('.btn-remover').disabled = (linhas.length <= 1);
  });
}

function validarFormaPagamentoDetalhe() {
  var linhas = document.querySelectorAll('#linhasPagamentoDetalhe .linha-pagamento');
  var algumaPreenchida = false;
  linhas.forEach(function(l) {
    var forma = l.querySelector('select[name=formaPagamento]').value;
    var valor = parseFloat(l.querySelector('input[name=valorPagamento]').value);
    if (forma && valor > 0) algumaPreenchida = true;
  });
  if (!algumaPreenchida) {
    return confirm('Nenhuma forma de pagamento preenchida — confirmar entrega mesmo assim, sem registrar pagamento?');
  }
  return true;
}

document.getElementById('modalPagamento').addEventListener('click', function(e) {
  if (e.target === this) fecharModalPagamento();
});
document.addEventListener('keydown', function(e) {
  if (e.key === 'Escape') fecharModalPagamento();
});
</script>
</body>
</html>
