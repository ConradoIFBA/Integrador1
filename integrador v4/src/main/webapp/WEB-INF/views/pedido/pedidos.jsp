<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@ page import="br.com.restaurante.model.Usuario"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt"%>
<%@ taglib prefix="fn" uri="jakarta.tags.functions"%>
<%--
    ================================================================
    PEDIDOS.JSP — LISTA DE PEDIDOS (v4 — visual alinhado ao Figma)
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
<title>Pedidos — Integrador</title>
<link rel="stylesheet"
	href="${pageContext.request.contextPath}/assets/style.css">
<style>
.topo {
	display: flex;
	justify-content: space-between;
	align-items: center;
	margin-bottom: 20px;
	flex-wrap: wrap;
	gap: 12px
}

.tabs {
	display: flex;
	gap: 8px;
	flex-wrap: wrap
}

/* Colunas de valor/hora alinhadas de forma legível */
td.col-total {
	color: var(--success-color);
	font-weight: 700
}

td.col-hora {
	color: var(--text-muted);
	font-size: 13px
}

.acoes-linha {
	display: flex;
	gap: 6px
}

/* Modal — herda o novo tema escuro automaticamente via style.css,
   só os campos de select precisam de um pequeno ajuste aqui. */
.modal .form-group select {
	width: 100%;
	padding: 10px 12px;
	border: 1.5px solid var(--border-subtle-2);
	border-radius: var(--radius-sm);
	font-size: 14px;
	font-family: inherit;
	background: var(--bg-input);
	color: var(--text-primary)
}

.modal .form-group select:focus {
	outline: none;
	border-color: var(--primary)
}

/* ── Linhas dinâmicas de pagamento (múltiplas formas) ── */
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
					<h2>Pedidos</h2>
				</div>
				<div class="topbar-right">
					<div class="user-info">
						<div class="user-avatar"><%= _u.getNome().substring(0,1).toUpperCase() %></div>
						<div class="user-details">
							<span class="name"><%= _u.getNome() %></span> <span class="role"><%= _u.getPerfil() %></span>
						</div>
					</div>
				</div>
			</header>
			<main class="content">

				<div class="topo">
					<div class="tabs">
						<button class="pill-filtro ativo"
							onclick="filtrarStatus('todos',this)">Todos</button>
						<button class="pill-filtro" onclick="filtrarStatus('aberto',this)">Recebido</button>
						<button class="pill-filtro"
							onclick="filtrarStatus('em_preparo',this)">Em Preparo</button>
						<button class="pill-filtro" onclick="filtrarStatus('pronto',this)">Pronto</button>
						<button class="pill-filtro"
							onclick="filtrarStatus('entregue',this)">Entregue</button>
						<button class="pill-filtro"
							onclick="filtrarStatus('cancelado',this)">Cancelado</button>
					</div>
					<a href="${pageContext.request.contextPath}/app/pedidos?acao=novo"
						class="btn btn-primary">+ Novo Pedido</a>
				</div>

				<c:if test="${not empty msgSucesso}">
					<div class="alert alert-success">✓ ${msgSucesso}</div>
				</c:if>

				<c:choose>
					<c:when test="${empty pedidos}">
						<div class="empty-state card">
							<div class="icon">🧾</div>
							<p>Nenhum pedido em aberto no momento.</p>
							<a
								href="${pageContext.request.contextPath}/app/pedidos?acao=novo"
								class="btn btn-primary" style="margin-top: 16px">Criar
								primeiro pedido</a>
						</div>
					</c:when>
					<c:otherwise>
						<div class="table-wrapper">
							<table id="tabelaPedidos">
								<thead>
									<tr>
										<th>#</th>
										<th>Cliente</th>
										<th>Tipo</th>
										<th>Itens</th>
										<th>Status</th>
										<th>Total</th>
										<th>Hora</th>
										<th>Ação</th>
									</tr>
								</thead>
								<tbody>
									<c:forEach var="p" items="${pedidos}">
										<tr data-status="${p.status}">
											<td><strong>#${p.idPedido}</strong></td>

											<td><c:choose>
													<c:when test="${p.tipo=='mesa'}">Mesa ${p.mesa.numero}</c:when>
													<c:otherwise>${p.identificadorOperador}</c:otherwise>
												</c:choose> <c:if test="${p.urgente}">
													<span class="badge badge-urgente" style="margin-left: 6px">Urgente</span>
												</c:if></td>

											<td><c:choose>
													<c:when test="${p.tipo=='mesa'}">
														<span class="badge badge-info">🪑 Mesa</span>
													</c:when>
													<c:otherwise>
														<span class="badge badge-roxo">🛵 Delivery</span>
													</c:otherwise>
												</c:choose></td>

											<td>${fn:length(p.itens)}</td>

											<td><c:choose>
													<c:when test="${p.status=='aberto'}">
														<span class="badge badge-info">Recebido</span>
													</c:when>
													<c:when test="${p.status=='em_preparo'}">
														<span class="badge badge-warning">Em Preparo</span>
													</c:when>
													<c:when test="${p.status=='pronto'}">
														<span class="badge badge-success">Pronto</span>
													</c:when>
													<c:when test="${p.status=='entregue'}">
														<span class="badge badge-success">Entregue</span>
													</c:when>
													<c:when test="${p.status=='cancelado'}">
														<span class="badge badge-danger">Cancelado</span>
													</c:when>
													<c:otherwise>
														<span class="badge">${p.status}</span>
													</c:otherwise>
												</c:choose></td>

											<td class="col-total">R$ <fmt:formatNumber
													value="${p.calcularTotal()}" minFractionDigits="2"
													maxFractionDigits="2" /></td>
											<td class="col-hora">${p.dataAberturaFormatada}</td>

											<td>
												<div class="acoes-linha">
													<a
														href="${pageContext.request.contextPath}/app/pedidos?acao=detalhe&id=${p.idPedido}"
														class="btn-icon" title="Ver detalhes">👁</a>

													<c:if
														test="${p.status!='entregue' && p.status!='cancelado'}">
														<c:choose>
															<c:when test="${p.status=='pronto'}">
																<button type="button" class="btn-icon"
																	title="Registrar entrega"
																	onclick="abrirModalEntrega('${p.idPedido}', ${p.calcularTotal()})">🍽️</button>
															</c:when>
															<c:otherwise>
																<button type="button" class="btn-icon"
																	title="Avançar status"
																	onclick="abrirModalStatus('${p.idPedido}')">
																	<c:choose>
																		<c:when test="${p.status=='aberto'}">▶</c:when>
																		<c:when test="${p.status=='em_preparo'}">✓</c:when>
																	</c:choose>
																</button>
															</c:otherwise>
														</c:choose>

														<form method="POST"
															action="${pageContext.request.contextPath}/app/pedidos"
															onsubmit="return confirm('Cancelar pedido #${p.idPedido}?')"
															style="display: inline">
															<input type="hidden" name="acao" value="cancelar">
															<input type="hidden" name="id" value="${p.idPedido}">
															<input type="hidden" name="operador"
																value="${p.identificadorOperador}">
															<button type="submit" class="btn-icon danger"
																title="Cancelar">✕</button>
														</form>
													</c:if>
												</div>
											</td>
										</tr>
									</c:forEach>
								</tbody>
							</table>
						</div>
						<div class="empty-state" id="semResultadoStatus"
							style="display: none">
							<div class="icon">🔎</div>
							<p>Nenhum pedido nesse status.</p>
						</div>
					</c:otherwise>
				</c:choose>

			</main>
		</div>
	</div>

	<%-- Modal avanço de status (aberto→em_preparo, em_preparo→pronto) —
     idêntico em comportamento à versão anterior. --%>
	<div class="modal-overlay" id="modalStatus">
		<div class="modal">
			<h3>Seu identificador</h3>
			<form method="POST"
				action="${pageContext.request.contextPath}/app/pedidos"
				id="formStatus">
				<input type="hidden" name="acao" value="avancarStatus"> <input
					type="hidden" name="id" id="statusPedidoId">
				<div class="form-group">
					<label>Identificador (ex: A1)</label> <input type="text"
						name="operador" id="statusOperador" placeholder="Ex: A1"
						maxlength="20" required>
				</div>
				<div class="modal-acoes">
					<button type="button" class="btn btn-secondary"
						onclick="fecharModal('modalStatus')">Cancelar</button>
					<button type="submit" class="btn btn-primary">Confirmar</button>
				</div>
			</form>
		</div>
	</div>

	<%-- Modal entrega + pagamento (pronto→entregue) — agora com
     SUPORTE A MÚLTIPLAS FORMAS DE PAGAMENTO no mesmo pedido (ex:
     metade no cartão, metade em dinheiro). Cada "linha" é um par de
     campos formaPagamento/valorPagamento (mesmo name repetido em
     todas as linhas — o navegador envia como array, e o Controller
     já lê via request.getParameterValues()). O botão "+ Adicionar
     forma de pagamento" clona a última linha via JS; a lixeira some
     enquanto só existe 1 linha (não faz sentido remover a única). --%>
	<div class="modal-overlay" id="modalEntrega">
		<div class="modal" style="max-width: 420px">
			<h3>🍽️ Registrar Entrega</h3>
			<form method="POST"
				action="${pageContext.request.contextPath}/app/pedidos"
				id="formEntrega" onsubmit="return validarFormaPagamento()">
				<input type="hidden" name="acao" value="avancarStatus"> <input
					type="hidden" name="id" id="entregaPedidoId">

				<div class="form-group">
					<label>Seu identificador</label> <input type="text" name="operador"
						id="entregaOperador" placeholder="Ex: A1" maxlength="20" required>
				</div>

				<div class="form-group" style="margin-bottom: 6px">
					<label
						style="display: flex; justify-content: space-between; align-items: center">
						<span>Pagamento</span> <span
						style="font-weight: 400; text-transform: none; color: var(--text-secondary)">
							Total: <strong id="entregaTotalLabel"
							style="color: var(--success-color)">R$ 0,00</strong>
					</span>
					</label>
				</div>

				<div id="linhasPagamento"></div>

				<button type="button" class="btn btn-secondary btn-sm"
					onclick="adicionarLinhaPagamento()"
					style="width: 100%; margin-bottom: 16px">+ Adicionar forma
					de pagamento</button>

				<div class="modal-acoes">
					<button type="button" class="btn btn-secondary"
						onclick="fecharModal('modalEntrega')">Cancelar</button>
					<button type="submit" class="btn btn-primary">Confirmar
						entrega</button>
				</div>
			</form>
		</div>
	</div>

	<script>
  function abrirModalStatus(pedidoId) {
    document.getElementById('statusPedidoId').value = pedidoId;
    document.getElementById('statusOperador').value = '';
    document.getElementById('modalStatus').classList.add('aberto');
    document.getElementById('statusOperador').focus();
  }

  var entregaTotalPedido = 0;

  function abrirModalEntrega(pedidoId, total) {
    document.getElementById('entregaPedidoId').value = pedidoId;
    document.getElementById('entregaOperador').value = '';
    entregaTotalPedido = parseFloat(total) || 0;
    document.getElementById('entregaTotalLabel').textContent =
      'R$ ' + entregaTotalPedido.toFixed(2).replace('.', ',');

    // Sempre reinicia com UMA linha só, já sugerindo o total inteiro
    // como valor — cobre o caso comum (uma forma só, valor cheio)
    // sem exigir nenhum clique extra; quem for dividir o pagamento
    // ajusta o valor e clica em "+ Adicionar forma de pagamento".
    document.getElementById('linhasPagamento').innerHTML = '';
    adicionarLinhaPagamento(entregaTotalPedido.toFixed(2));

    document.getElementById('modalEntrega').classList.add('aberto');
    document.getElementById('entregaOperador').focus();
  }

  function adicionarLinhaPagamento(valorSugerido) {
    var cont = document.getElementById('linhasPagamento');
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
      '<button type="button" class="btn-remover" onclick="removerLinhaPagamento(this)" title="Remover">✕</button>';
    cont.appendChild(div);
    atualizarBotoesRemover();
  }

  function removerLinhaPagamento(btn) {
    var cont = document.getElementById('linhasPagamento');
    if (cont.children.length <= 1) return; // sempre precisa sobrar 1 linha
    btn.closest('.linha-pagamento').remove();
    atualizarBotoesRemover();
  }

  // Desabilita o botão de remover quando só resta 1 linha — não faz
  // sentido remover a única forma de pagamento do pedido.
  function atualizarBotoesRemover() {
    var linhas = document.querySelectorAll('#linhasPagamento .linha-pagamento');
    linhas.forEach(function(l) {
      l.querySelector('.btn-remover').disabled = (linhas.length <= 1);
    });
  }

  // Não BLOQUEIA a entrega sem pagamento (pode ser intencional — ex:
  // conta fiado, acerto depois) — só confirma com o operador que é
  // isso mesmo que ele quer, evitando o caso mais comum de "esqueci
  // de escolher a forma de pagamento" passar batido sem querer.
  function validarFormaPagamento() {
    var linhas = document.querySelectorAll('#linhasPagamento .linha-pagamento');
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

  function fecharModal(id) {
    document.getElementById(id).classList.remove('aberto');
  }

  // Filtro por status: agora esconde LINHAS da tabela em vez de
  // cards, mas a lógica é a mesma de antes (data-status no <tr>).
  function filtrarStatus(status, btn) {
    document.querySelectorAll('.pill-filtro').forEach(b => b.classList.remove('ativo'));
    btn.classList.add('ativo');
    var linhas = document.querySelectorAll('#tabelaPedidos tbody tr');
    var visiveis = 0;
    linhas.forEach(function(tr) {
      var mostrar = (status === 'todos' || tr.dataset.status === status);
      tr.style.display = mostrar ? '' : 'none';
      if (mostrar) visiveis++;
    });
    var semResultado = document.getElementById('semResultadoStatus');
    if (semResultado) semResultado.style.display = (visiveis === 0) ? '' : 'none';
  }

  ['modalStatus','modalEntrega'].forEach(function(id) {
    document.getElementById(id).addEventListener('click', function(e) {
      if (e.target === this) fecharModal(id);
    });
  });

  document.addEventListener('keydown', function(e) {
    if (e.key === 'Escape') {
      fecharModal('modalStatus');
      fecharModal('modalEntrega');
    }
  });
</script>
</body>
</html>
