<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@ page import="br.com.restaurante.model.Usuario"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt"%>
<%--
    ================================================================
    NOVO_PEDIDO.JSP — GERENTE/FUNCIONÁRIO CRIA UM PEDIDO (v4)
    ================================================================
    Mesmo padrão visual do delivery.jsp (lista densa de itens com
    seletor de quantidade + sacola lateral), só que com os campos
    extras que só o staff usa (tipo mesa/delivery, seleção de mesa,
    urgente, identificador). Reaproveitei .pill-filtro do style.css
    global no lugar da antiga classe local .filtro-cat, e troquei
    todas as cores fixas (#e85d27, #1e293b...) pelas variáveis do
    tema escuro. JS e lógica de submissão continuam idênticos.
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
<title>Novo Pedido — Integrador</title>
<link rel="stylesheet"
	href="${pageContext.request.contextPath}/assets/style.css">
<style>
.layout {
	display: grid;
	grid-template-columns: 1fr 360px;
	gap: 24px;
	align-items: start
}

@media ( max-width :900px) {
	.layout {
		grid-template-columns: 1fr
	}
}

.secao-titulo {
	font-size: 15px;
	font-weight: 700;
	margin-bottom: 14px;
	padding-bottom: 10px;
	border-bottom: 1px solid var(--border-subtle)
}

.item-selecao {
	display: flex;
	align-items: center;
	justify-content: space-between;
	padding: 12px 14px;
	border: 1px solid var(--border-subtle);
	border-radius: var(--radius-sm);
	margin-bottom: 8px;
	transition: var(--transition);
	background: var(--bg-card)
}

.item-selecao:hover {
	border-color: var(--border-subtle-2)
}

.item-selecao.selecionado {
	border-color: var(--primary);
	background: var(--primary-light)
}

.item-info .nome {
	font-size: 14px;
	font-weight: 600
}

.item-info .preco {
	font-size: 13px;
	color: var(--success-color);
	font-weight: 700
}

.item-info .tempo {
	font-size: 12px;
	color: var(--text-muted)
}

.qtd-controle {
	display: flex;
	align-items: center;
	gap: 10px
}

.qtd-controle button {
	width: 28px;
	height: 28px;
	border-radius: 50%;
	border: 1.5px solid var(--border-subtle-2);
	background: var(--bg-card-hover);
	color: var(--text-primary);
	font-size: 16px;
	font-weight: 700;
	cursor: pointer;
	transition: var(--transition);
	display: flex;
	align-items: center;
	justify-content: center
}

.qtd-controle button:hover {
	border-color: var(--primary);
	color: var(--primary)
}

.qtd-valor {
	font-size: 15px;
	font-weight: 700;
	min-width: 22px;
	text-align: center
}

.sacola {
	position: sticky;
	top: 20px
}

.sacola-item {
	display: flex;
	justify-content: space-between;
	align-items: center;
	padding: 10px 0;
	border-bottom: 1px solid var(--border-subtle);
	font-size: 13px
}

.sacola-item:last-child {
	border-bottom: none
}

.sacola-total {
	display: flex;
	justify-content: space-between;
	padding-top: 14px;
	font-size: 16px;
	font-weight: 800;
	border-top: 1px solid var(--border-subtle-2);
	margin-top: 8px
}

.sacola-vazia {
	text-align: center;
	padding: 24px;
	color: var(--text-muted);
	font-size: 13px
}

.filtros-cat {
	display: flex;
	gap: 8px;
	flex-wrap: wrap;
	margin-bottom: 16px
}
</style>
</head>
<body>
	<div class="main-container">
		<%@ include file="/WEB-INF/views/shared/_sidebar.jsp"%>
		<div class="main-content">
			<header class="topbar">
				<div class="topbar-left">
					<h2>Novo Pedido</h2>
				</div>
				<div class="topbar-right">
					<a href="${pageContext.request.contextPath}/app/pedidos"
						class="btn btn-secondary btn-sm">← Voltar</a>
				</div>
			</header>
			<main class="content">
				<form method="POST"
					action="${pageContext.request.contextPath}/app/pedidos"
					id="formPedido" onsubmit="return prepararEnvio()">
					<input type="hidden" name="acao" value="criar">

					<div class="layout">

						<div>
							<div class="card" style="margin-bottom: 20px">
								<div class="secao-titulo">📋 Dados do Pedido</div>

								<div
									style="display: grid; grid-template-columns: 1fr 1fr; gap: 16px">
									<div class="form-group" style="margin: 0">
										<label>Tipo</label> <select name="tipo" id="tipoPedido"
											onchange="atualizarTipo()">
											<option value="mesa">🪑 Mesa</option>
											<option value="delivery">🛵 Delivery</option>
										</select>
									</div>

									<div class="form-group" style="margin: 0" id="grupaMesa">
										<label>Mesa</label> <select name="mesaId" id="selectMesa">
											<option value="">Selecione...</option>
											<c:forEach var="mesa" items="${mesas}">
												<option value="${mesa.idMesa}"
													<c:if test="${mesaIdSelecionada == mesa.idMesa}">selected</c:if>>
													Mesa ${mesa.numero} (${mesa.capacidade} lugares)</option>
											</c:forEach>
										</select>
									</div>
								</div>

								<div
									style="display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-top: 14px">
									<div class="form-group" style="margin: 0">
										<label>Seu identificador</label> <input type="text"
											name="operador" placeholder="Ex: A1" maxlength="20" required>
									</div>
									<div class="form-group"
										style="margin: 0; display: flex; align-items: flex-end">
										<label
											style="display: flex; align-items: center; gap: 8px; cursor: pointer; padding-bottom: 4px">
											<input type="checkbox" name="urgente"
											style="width: 18px; height: 18px; accent-color: var(--error-color)">
											<span style="font-weight: 600">🔴 Pedido urgente</span>
										</label>
									</div>
								</div>

								<div class="form-group"
									style="margin-top: 14px; margin-bottom: 0">
									<label>Observação</label> <input type="text" name="observacao"
										placeholder="Alguma observação geral?" maxlength="255">
								</div>
							</div>

							<div class="card">
								<div class="secao-titulo">🍽️ Selecione os Itens</div>
								<div class="filtros-cat" id="filtrosCat">
									<button type="button" class="pill-filtro ativo"
										onclick="filtrarCat('todos',this)">Todos</button>
								</div>
								<div id="listaItens">
									<c:forEach var="item" items="${itens}">
										<div class="item-selecao" id="card-${item.idItem}"
											data-cat="${item.categoriaId}" data-nome="${item.nome}"
											data-preco="${item.preco}">
											<div class="item-info">
												<div class="nome">${item.nome}</div>
												<div class="preco">
													R$
													<fmt:formatNumber value="${item.preco}"
														minFractionDigits="2" maxFractionDigits="2" />
												</div>
												<div class="tempo" data-cat-nome="${item.nomeCategoria}">
													⏱ ${item.tempoPreparoMin} min · ${item.nomeCategoria}</div>
											</div>
											<div class="qtd-controle">
												<button type="button" onclick="diminuir(${item.idItem})">−</button>
												<span class="qtd-valor" id="qtd-${item.idItem}">0</span>
												<button type="button"
													onclick="aumentar(${item.idItem},'${item.nome}',${item.preco})">+</button>
											</div>
										</div>
									</c:forEach>
									<c:if test="${empty itens}">
										<div class="empty-state">
											<div class="icon">📋</div>
											<p>Nenhum item disponível.</p>
										</div>
									</c:if>
								</div>
							</div>
						</div>

						<div class="sacola">
							<div class="card">
								<div class="secao-titulo">🛒 Sacola</div>
								<div id="sacolaItens">
									<div class="sacola-vazia" id="sacolaVazia">Nenhum item
										adicionado ainda.</div>
								</div>
								<div class="sacola-total" id="sacolaTotal" style="display: none">
									<span>Total</span><span id="valorTotal"
										style="color: var(--success-color)">R$ 0,00</span>
								</div>
								<div style="margin-top: 18px">
									<button type="submit" class="btn btn-primary"
										style="width: 100%; padding: 14px; font-size: 15px">
										✅ Confirmar Pedido</button>
								</div>
							</div>
						</div>

					</div>
					<div id="inputsOcultos"></div>
				</form>
			</main>
		</div>
	</div>
	<script>
var sacola = {};

function aumentar(id, nome, preco) {
  if (!sacola[id]) sacola[id] = { nome: nome, preco: parseFloat(preco), qtd: 0 };
  sacola[id].qtd++;
  document.getElementById('qtd-' + id).textContent = sacola[id].qtd;
  document.getElementById('card-' + id).classList.add('selecionado');
  renderSacola();
}

function diminuir(id) {
  if (!sacola[id] || sacola[id].qtd === 0) return;
  sacola[id].qtd--;
  document.getElementById('qtd-' + id).textContent = sacola[id].qtd;
  if (sacola[id].qtd === 0) {
    delete sacola[id];
    document.getElementById('card-' + id).classList.remove('selecionado');
  }
  renderSacola();
}

function renderSacola() {
  var div = document.getElementById('sacolaItens');
  var total = 0, html = '';
  for (var id in sacola) {
    var it = sacola[id];
    var sub = it.preco * it.qtd;
    total += sub;
    html += '<div class="sacola-item"><span>' + it.qtd + 'x ' + it.nome + '</span>'
          + '<span style="font-weight:700;color:var(--success-color)">R$ ' + sub.toFixed(2).replace('.',',') + '</span></div>';
  }
  if (html) {
    div.innerHTML = html;
    document.getElementById('sacolaTotal').style.display = 'flex';
    document.getElementById('valorTotal').textContent = 'R$ ' + total.toFixed(2).replace('.',',');
  } else {
    div.innerHTML = '<div class="sacola-vazia" id="sacolaVazia">Nenhum item adicionado ainda.</div>';
    document.getElementById('sacolaTotal').style.display = 'none';
  }
}

function prepararEnvio() {
  if (Object.keys(sacola).length === 0) { alert('Adicione pelo menos um item.'); return false; }
  var tipo = document.getElementById('tipoPedido').value;
  if (tipo === 'mesa' && !document.getElementById('selectMesa').value) {
    alert('Selecione a mesa.'); return false;
  }
  var cont = document.getElementById('inputsOcultos');
  cont.innerHTML = '';
  for (var id in sacola) {
    cont.innerHTML += '<input type="hidden" name="itemId"    value="' + id + '">'
                    + '<input type="hidden" name="quantidade" value="' + sacola[id].qtd + '">';
  }
  return true;
}

function atualizarTipo() {
  var tipo = document.getElementById('tipoPedido').value;
  document.getElementById('grupaMesa').style.display = tipo === 'mesa' ? '' : 'none';
}

function filtrarCat(cat, btn) {
  document.querySelectorAll('.pill-filtro').forEach(b => b.classList.remove('ativo'));
  btn.classList.add('ativo');
  document.querySelectorAll('.item-selecao').forEach(function(el) {
    el.style.display = (cat === 'todos' || el.dataset.cat == cat) ? '' : 'none';
  });
}

// Gera botões de categoria automaticamente — inalterado
window.addEventListener('DOMContentLoaded', function() {
  var cats = {}, cont = document.getElementById('filtrosCat');
  document.querySelectorAll('.item-selecao').forEach(function(el) {
    var cid  = el.dataset.cat;
    var nome = el.querySelector('[data-cat-nome]');
    if (nome && !cats[cid]) {
      cats[cid] = nome.getAttribute('data-cat-nome');
    }
  });
  for (var cid in cats) {
    var btn = document.createElement('button');
    btn.type = 'button'; btn.className = 'pill-filtro';
    btn.textContent = cats[cid];
    btn.setAttribute('data-cid', cid);
    btn.onclick = (function(c, b){ return function(){ filtrarCat(c, b); }; })(cid, btn);
    cont.appendChild(btn);
  }
});
</script>
</body>
</html>
