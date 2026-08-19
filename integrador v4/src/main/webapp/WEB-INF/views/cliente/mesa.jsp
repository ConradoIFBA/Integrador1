<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@ page import="br.com.restaurante.model.Usuario"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt"%>
<%--
    ================================================================
    MESA.JSP — "ESTOU NA MESA" (cliente escolhe garçom ou pedido direto)
    ================================================================
    Fluxo em DOIS PASSOS, tudo numa página só (sem ida ao servidor
    entre os passos, já que tanto a lista de mesas quanto o cardápio
    inteiro já vieram prontos do ClienteController.exibirMesa()):

    PASSO 1 — Escolher a mesa: grid de números, igual ao padrão já
    usado em reserva.jsp. Ao clicar numa mesa, aparecem duas opções:
      🔔 Chamar Garçom      → envia um form pequeno na hora (POST
                               imediato, sem passo 2)
      📝 Fazer Pedido Direto → NÃO envia nada ainda; troca a tela
                               para o Passo 2, carregando a mesa
                               escolhida num campo oculto

    PASSO 2 — Montar o pedido: mesmo padrão de carrinho já usado em
    delivery.jsp/novo_pedido.jsp (lista de itens com seletor de
    quantidade + sacola lateral) — só que aqui o pedido nasce com
    tipo='mesa' e vinculado à mesa escolhida no Passo 1, em vez de
    tipo='delivery'.

    Se a mesa escolhida ainda estiver 'livre', o Controller abre ela
    automaticamente ao confirmar o pedido (usando o nome do cliente
    como operador) — o cliente não precisa esperar um funcionário
    "abrir a mesa" antes de poder pedir sozinho.
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
<title>Estou na Mesa — Integrador</title>
<link rel="stylesheet"
	href="${pageContext.request.contextPath}/assets/style.css">
<style>
/* ── Passo 1: grid de mesas ── */
.mesas-grid {
	display: grid;
	grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
	gap: 14px
}

.mesa-card {
	background: var(--bg-card);
	border: 1.5px solid var(--border-subtle);
	border-radius: var(--radius);
	padding: 18px;
	text-align: center;
	cursor: pointer;
	transition: var(--transition)
}

.mesa-card:hover {
	border-color: var(--primary)
}

.mesa-card.selecionada {
	border-color: var(--primary);
	background: var(--primary-light)
}

.mesa-card .num {
	font-size: 26px;
	font-weight: 800
}

.mesa-card .cap {
	font-size: 12px;
	color: var(--text-secondary);
	margin-top: 2px
}

.mesa-card .status-mini {
	font-size: 11px;
	margin-top: 6px;
	font-weight: 600
}

.mesa-card .status-mini.livre {
	color: var(--success-color)
}

.mesa-card .status-mini.ocupada {
	color: var(--error-color)
}

.mesa-card .status-mini.reservada {
	color: var(--warning-color)
}

.acoes-mesa-escolhida {
	display: none;
	margin-top: 22px;
	padding: 20px;
	background: var(--bg-card);
	border: 1px solid var(--border-subtle);
	border-radius: var(--radius);
	text-align: center
}

.acoes-mesa-escolhida.visivel {
	display: block
}

.acoes-mesa-escolhida h3 {
	margin-bottom: 16px;
	font-size: 16px
}

.botoes-acao {
	display: flex;
	gap: 14px;
	justify-content: center;
	flex-wrap: wrap
}

.botoes-acao .btn {
	padding: 16px 28px;
	font-size: 15px;
	min-width: 200px
}

/* ── Passo 2: carrinho (mesmo padrão de delivery.jsp) ── */
#passo2 {
	display: none
}

#passo2.visivel {
	display: block
}

.layout {
	display: grid;
	grid-template-columns: 1fr 340px;
	gap: 24px;
	align-items: start
}

@media ( max-width :900px) {
	.layout {
		grid-template-columns: 1fr
	}
}

.item-linha {
	display: flex;
	align-items: center;
	justify-content: space-between;
	padding: 14px 16px;
	border: 1px solid var(--border-subtle);
	border-radius: var(--radius-sm);
	margin-bottom: 8px;
	transition: var(--transition);
	background: var(--bg-card)
}

.item-linha.selecionado {
	border-color: var(--primary);
	background: var(--primary-light)
}

.item-nome {
	font-size: 14px;
	font-weight: 600
}

.item-preco {
	font-size: 13px;
	color: var(--success-color);
	font-weight: 700
}

.qtd-controle {
	display: flex;
	align-items: center;
	gap: 10px
}

.qtd-btn {
	width: 28px;
	height: 28px;
	border-radius: 50%;
	border: 1.5px solid var(--border-subtle-2);
	background: var(--bg-card-hover);
	color: var(--text-primary);
	font-size: 16px;
	font-weight: 700;
	cursor: pointer;
	display: flex;
	align-items: center;
	justify-content: center
}

.qtd-val {
	font-size: 15px;
	font-weight: 700;
	min-width: 20px;
	text-align: center
}

.sacola {
	position: sticky;
	top: 20px
}

.sacola-item {
	display: flex;
	justify-content: space-between;
	padding: 9px 0;
	border-bottom: 1px solid var(--border-subtle);
	font-size: 13px
}

.sacola-total {
	display: flex;
	justify-content: space-between;
	padding-top: 12px;
	font-size: 16px;
	font-weight: 800;
	border-top: 1px solid var(--border-subtle-2);
	margin-top: 6px
}

.voltar-link {
	font-size: 13px;
	color: var(--text-secondary);
	cursor: pointer;
	display: inline-block;
	margin-bottom: 16px
}

.voltar-link:hover {
	color: var(--primary)
}
</style>
</head>
<body>
	<div class="main-container">
		<%@ include file="/WEB-INF/views/shared/_sidebar.jsp"%>
		<div class="main-content">
			<header class="topbar">
				<div class="topbar-left">
					<h2>🍽️ Estou na Mesa</h2>
				</div>
				<div class="topbar-right">
					<div class="user-info">
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
				<c:if test="${not empty msgErro}">
					<div class="alert alert-error">✕ ${msgErro}</div>
				</c:if>

				<%-- ============ PASSO 1: escolher mesa + ação ============ --%>
				<div id="passo1">
					<div class="card">
						<h3>Em qual mesa você está?</h3>
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
										<div class="mesa-card" data-id="${mesa.idMesa}"
											data-numero="${mesa.numero}" onclick="selecionarMesa(this)">
											<div class="num">#${mesa.numero}</div>
											<div class="cap">👥 ${mesa.capacidade}</div>
											<div class="status-mini ${mesa.status}">
												<c:choose>
													<c:when test="${mesa.status=='livre'}">Livre</c:when>
													<c:when test="${mesa.status=='ocupada'}">Ocupada</c:when>
													<c:otherwise>Reservada</c:otherwise>
												</c:choose>
											</div>
										</div>
									</c:forEach>
								</div>
							</c:otherwise>
						</c:choose>

						<div class="acoes-mesa-escolhida" id="acoesMesa">
							<h3>
								Mesa <span id="numeroMesaEscolhida"></span> selecionada
							</h3>
							<div class="botoes-acao">
								<form method="POST"
									action="${pageContext.request.contextPath}/app/cliente/mesa"
									style="display: inline">
									<input type="hidden" name="acao" value="chamarGarcom">
									<input type="hidden" name="mesaId" id="mesaIdChamar">
									<button type="submit" class="btn btn-secondary">🔔
										Chamar Garçom</button>
								</form>
								<button type="button" class="btn btn-primary"
									onclick="irParaPedidoDireto()">📝 Fazer Pedido Direto</button>
							</div>
						</div>
					</div>
				</div>

				<%-- ============ PASSO 2: montar o pedido direto ============ --%>
				<div id="passo2">
					<span class="voltar-link" onclick="voltarParaPasso1()">←
						Trocar de mesa</span>

					<form method="POST"
						action="${pageContext.request.contextPath}/app/cliente/mesa"
						id="formPedidoDireto" onsubmit="return prepararEnvio()">
						<input type="hidden" name="acao" value="pedidoDireto"> <input
							type="hidden" name="mesaId" id="mesaIdPedido">

						<div class="layout">
							<div>
								<div class="card">
									<h3>
										🍽️ Mesa <span id="numeroMesaPedido"></span> — Escolha seus
										itens
									</h3>

									<c:choose>
										<c:when test="${empty itens}">
											<div class="empty-state">
												<div class="icon">📋</div>
												<p>Nenhum item disponível.</p>
											</div>
										</c:when>
										<c:otherwise>
											<c:forEach var="item" items="${itens}">
												<div class="item-linha" id="dcard-${item.idItem}">
													<div>
														<div class="item-nome">${item.nome}</div>
														<div class="item-preco">
															R$
															<fmt:formatNumber value="${item.preco}"
																minFractionDigits="2" maxFractionDigits="2" />
														</div>
														<div style="font-size: 12px; color: var(--text-muted)">${item.nomeCategoria}
															· ⏱ ${item.tempoPreparoMin} min</div>
													</div>
													<div class="qtd-controle">
														<button type="button" class="qtd-btn"
															onclick="diminuir(${item.idItem})">−</button>
														<span class="qtd-val" id="dqtd-${item.idItem}">0</span>
														<button type="button" class="qtd-btn"
															onclick="aumentar(${item.idItem},'${item.nome}',${item.preco})">+</button>
													</div>
												</div>
											</c:forEach>
										</c:otherwise>
									</c:choose>

									<div class="form-group"
										style="margin-top: 16px; margin-bottom: 0">
										<label>Observação (opcional)</label> <input type="text"
											name="observacao"
											placeholder="Ex: sem cebola, ponto da carne..."
											maxlength="255">
									</div>
								</div>
							</div>

							<div class="sacola">
								<div class="card">
									<h3>🛒 Sua sacola</h3>
									<div id="dsacolaItens">
										<div class="text-muted"
											style="text-align: center; padding: 20px">Nenhum item
											ainda.</div>
									</div>
									<div class="sacola-total" id="dsacolaTotal"
										style="display: none">
										<span>Total</span><span id="dvalorTotal"
											style="color: var(--success-color)">R$ 0,00</span>
									</div>
									<button type="submit" class="btn btn-primary"
										style="width: 100%; padding: 13px; font-size: 15px; margin-top: 16px">
										✅ Enviar Pedido para a Cozinha</button>
								</div>
							</div>
						</div>
						<div id="dinputsOcultos"></div>
					</form>
				</div>

			</main>
		</div>
	</div>

	<script>
var sacolaDireta = {};
var mesaSelecionadaId = null, mesaSelecionadaNumero = null;

function selecionarMesa(card) {
  document.querySelectorAll('.mesa-card').forEach(c => c.classList.remove('selecionada'));
  card.classList.add('selecionada');
  mesaSelecionadaId = card.dataset.id;
  mesaSelecionadaNumero = card.dataset.numero;

  document.getElementById('numeroMesaEscolhida').textContent = mesaSelecionadaNumero;
  document.getElementById('mesaIdChamar').value = mesaSelecionadaId;
  document.getElementById('acoesMesa').classList.add('visivel');
  document.getElementById('acoesMesa').scrollIntoView({ behavior: 'smooth', block: 'nearest' });
}

function irParaPedidoDireto() {
  if (!mesaSelecionadaId) return;
  document.getElementById('mesaIdPedido').value = mesaSelecionadaId;
  document.getElementById('numeroMesaPedido').textContent = mesaSelecionadaNumero;
  document.getElementById('passo1').style.display = 'none';
  document.getElementById('passo2').classList.add('visivel');
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

function voltarParaPasso1() {
  document.getElementById('passo2').classList.remove('visivel');
  document.getElementById('passo1').style.display = 'block';
}

function aumentar(id, nome, preco) {
  if (!sacolaDireta[id]) sacolaDireta[id] = { nome, preco: parseFloat(preco), qtd: 0 };
  sacolaDireta[id].qtd++;
  document.getElementById('dqtd-' + id).textContent = sacolaDireta[id].qtd;
  document.getElementById('dcard-' + id).classList.add('selecionado');
  renderSacolaDireta();
}
function diminuir(id) {
  if (!sacolaDireta[id] || sacolaDireta[id].qtd === 0) return;
  sacolaDireta[id].qtd--;
  document.getElementById('dqtd-' + id).textContent = sacolaDireta[id].qtd;
  if (sacolaDireta[id].qtd === 0) {
    delete sacolaDireta[id];
    document.getElementById('dcard-' + id).classList.remove('selecionado');
  }
  renderSacolaDireta();
}
function renderSacolaDireta() {
  var div = document.getElementById('dsacolaItens');
  var total = 0, html = '';
  for (var id in sacolaDireta) {
    var it = sacolaDireta[id], sub = it.preco * it.qtd;
    total += sub;
    html += '<div class="sacola-item"><span>' + it.qtd + 'x ' + it.nome + '</span>'
          + '<span style="font-weight:700;color:var(--success-color)">R$ ' + sub.toFixed(2).replace('.', ',') + '</span></div>';
  }
  div.innerHTML = html || '<div class="text-muted" style="text-align:center;padding:20px">Nenhum item ainda.</div>';
  document.getElementById('dsacolaTotal').style.display = html ? 'flex' : 'none';
  document.getElementById('dvalorTotal').textContent = 'R$ ' + total.toFixed(2).replace('.', ',');
}
function prepararEnvio() {
  if (Object.keys(sacolaDireta).length === 0) { alert('Adicione pelo menos um item.'); return false; }
  var cont = document.getElementById('dinputsOcultos');
  cont.innerHTML = '';
  for (var id in sacolaDireta) {
    cont.innerHTML += '<input type="hidden" name="itemId" value="' + id + '">'
                     + '<input type="hidden" name="quantidade" value="' + sacolaDireta[id].qtd + '">';
  }
  return true;
}
</script>
</body>
</html>
