<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@ page import="br.com.restaurante.model.Usuario"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt"%>
<%@ taglib prefix="fn" uri="jakarta.tags.functions"%>
<%--
    ================================================================
    CARDAPIO.JSP — GRID DE ITENS
    ================================================================
  
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
<title>Cardápio — Integrador</title>
<link rel="stylesheet"
	href="${pageContext.request.contextPath}/assets/style.css">
<style>
.topo-cardapio {
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 16px;
	margin-bottom: 20px;
	flex-wrap: wrap
}

.busca-box {
	flex: 1;
	min-width: 220px;
	position: relative
}

.busca-box input {
	width: 100%;
	padding: 10px 14px 10px 38px;
	border: 1.5px solid var(--border-subtle-2);
	border-radius: var(--radius-pill);
	background: var(--bg-input);
	color: var(--text-primary);
	font-size: 14px;
	font-family: inherit
}

.busca-box input:focus {
	outline: none;
	border-color: var(--primary)
}

.busca-box .lupa {
	position: absolute;
	left: 14px;
	top: 50%;
	transform: translateY(-50%);
	color: var(--text-muted)
}

.filtros {
	display: flex;
	gap: 8px;
	flex-wrap: wrap;
	margin-bottom: 24px
}

.itens-grid {
	display: grid;
	grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
	gap: 18px
}

.item-card {
	background: var(--bg-card);
	border: 1px solid var(--border-subtle);
	border-radius: var(--radius);
	overflow: hidden;
	transition: var(--transition)
}

.item-card:hover {
	border-color: var(--border-subtle-2)
}

.item-card.indisponivel {
	opacity: .6
}

/* Bloco decorativo no topo do card — ver explicação no comentário
   JSP acima sobre por que não é uma foto real. */
.item-imagem {
	height: 120px;
	display: flex;
	align-items: center;
	justify-content: center;
	font-size: 44px;
	position: relative
}

.item-imagem.cozinha {
	background: linear-gradient(135deg, #7c2d12, #c2410c)
}

.item-imagem.bebida {
	background: linear-gradient(135deg, #1e3a8a, #2563eb)
}

.item-imagem.sobremesa {
	background: linear-gradient(135deg, #831843, #db2777)
}

.item-imagem .tag-setor {
	position: absolute;
	top: 10px;
	left: 10px;
	background: rgba(0, 0, 0, .4);
	color: #fff;
	font-size: 11px;
	font-weight: 700;
	padding: 3px 10px;
	border-radius: var(--radius-pill)
}

.item-corpo {
	padding: 16px
}

.item-nome {
	font-size: 15px;
	font-weight: 700;
	margin-bottom: 6px
}

.item-desc {
	font-size: 12px;
	color: var(--text-secondary);
	margin-bottom: 12px;
	min-height: 32px;
	line-height: 1.5
}

.item-rodape {
	display: flex;
	align-items: center;
	justify-content: space-between;
	margin-bottom: 12px
}

.item-preco {
	font-size: 17px;
	font-weight: 800;
	color: var(--success-color)
}

.item-tempo {
	font-size: 11px;
	color: var(--text-muted)
}

.item-disp-row {
	display: flex;
	align-items: center;
	justify-content: space-between;
	margin-bottom: 12px;
	font-size: 12px
}

.item-disp-row .txt {
	color: var(--text-secondary);
	display: flex;
	align-items: center;
	gap: 5px
}

.item-disp-row .txt::before {
	content: '';
	width: 6px;
	height: 6px;
	border-radius: 50%;
	background: currentColor
}

.item-disp-row .txt.on {
	color: var(--success-color)
}

.item-disp-row .txt.off {
	color: var(--text-muted)
}

.acoes-gerente {
	display: flex;
	gap: 8px
}

/* ── Modal "Gerenciar Categorias" ── */
.cat-linha {
	display: flex;
	align-items: center;
	gap: 10px;
	padding: 9px 6px;
	border-bottom: 1px solid var(--border-subtle)
}

.cat-linha:last-child {
	border-bottom: none
}

.cat-nome {
	flex: 1;
	font-size: 13px;
	font-weight: 600
}

.cat-setor-badge {
	font-size: 11px;
	padding: 2px 9px;
	border-radius: var(--radius-pill);
	background: var(--bg-card-hover);
	color: var(--text-secondary);
	text-transform: capitalize
}

.cat-acoes {
	display: flex;
	gap: 6px;
	flex-shrink: 0
}

.cat-acoes .btn-icon {
	width: 28px;
	height: 28px;
	font-size: 12px
}
</style>
</head>
<body>
	<div class="main-container">
		<%@ include file="/WEB-INF/views/shared/_sidebar.jsp"%>
		<div class="main-content">
			<header class="topbar">
				<div class="topbar-left">
					<h2>Cardápio</h2>
				</div>
				<div class="topbar-right">
					<div class="user-info">
						<div class="user-avatar"><%=_u.getNome().substring(0, 1).toUpperCase()%></div>
						<div class="user-details">
							<span class="name"><%=_u.getNome()%></span> <span class="role"><%=_u.getPerfil()%></span>
						</div>
					</div>
				</div>
			</header>
			<main class="content">

				<div class="topo-cardapio">
					<div class="busca-box">
						<span class="lupa">🔎</span> <input type="text" id="buscaItem"
							placeholder="Buscar item..." oninput="buscar(this.value)">
					</div>
					<%
					if (_isGerente) {
					%>
					<div style="display: flex; gap: 10px">
						<button type="button" class="btn btn-secondary"
							onclick="abrirModalCategorias()">⚙️ Categorias</button>
						<a
							href="${pageContext.request.contextPath}/app/cardapio?acao=novo"
							class="btn btn-primary">+ Novo Item</a>
					</div>
					<%
					}
					%>
				</div>

				<c:if test="${not empty msgSucesso}">
					<div class="alert alert-success">✓ ${msgSucesso}</div>
				</c:if>

				<%-- Filtro por SETOR (não mais por categoria individual) —
				     "Todos" mostra tudo; os outros três casam com o enum
				     categoria_item.setor. --%>
				<div class="filtros">
					<button class="pill-filtro ativo" data-setor="todos"
						onclick="filtrarSetor('todos',this)">🍽️ Todos</button>
					<button class="pill-filtro" data-setor="cozinha"
						onclick="filtrarSetor('cozinha',this)">Pratos</button>
					<button class="pill-filtro" data-setor="bebida"
						onclick="filtrarSetor('bebida',this)">Bebidas</button>
					<button class="pill-filtro" data-setor="sobremesa"
						onclick="filtrarSetor('sobremesa',this)">Sobremesas</button>
				</div>

				<c:choose>
					<c:when test="${empty itens}">
						<div class="empty-state card">
							<div class="icon">📋</div>
							<p>Nenhum item no cardápio ainda.</p>
							<%
							if (_isGerente) {
							%>
							<a
								href="${pageContext.request.contextPath}/app/cardapio?acao=novo"
								class="btn btn-primary" style="margin-top: 16px">+ Adicionar
								primeiro item</a>
							<%
							}
							%>
						</div>
					</c:when>
					<c:otherwise>
						<div class="itens-grid" id="gridItens">
							<c:forEach var="item" items="${itens}">
								<%-- Descobre o setor da categoria deste item, comparando
								     com a lista de categorias já carregada — evita uma
								     nova consulta ao banco por item. --%>
								<c:set var="setorItem" value="" />
								<c:forEach var="cat" items="${categorias}">
									<c:if test="${cat.idCategoria == item.categoriaId}">
										<c:set var="setorItem" value="${cat.setor}" />
									</c:if>
								</c:forEach>

								<div class="item-card ${item.disponivel ? '' : 'indisponivel'}"
									data-setor="${setorItem}"
									data-nome="${fn:toLowerCase(item.nome)}">

									<%-- Foto real (upload feito pelo gerente) se existir; senão,
									     cai no bloco decorativo colorido por setor de sempre. --%>
									<c:choose>
										<c:when test="${item.temImagem}">
											<div class="item-imagem" style="padding: 0">
												<img
													src="${pageContext.request.contextPath}/imagens/cardapio/${item.imagem}"
													alt="${item.nome}"
													style="width: 100%; height: 100%; object-fit: cover; position: absolute; inset: 0">
												<span class="tag-setor" style="z-index: 1"> <c:choose>
														<c:when test="${setorItem=='cozinha'}">Prato</c:when>
														<c:when test="${setorItem=='bebida'}">Bebida</c:when>
														<c:otherwise>Sobremesa</c:otherwise>
													</c:choose>
												</span>
											</div>
										</c:when>
										<c:otherwise>
											<div class="item-imagem ${setorItem}">
												<span class="tag-setor"> <c:choose>
														<c:when test="${setorItem=='cozinha'}">Prato</c:when>
														<c:when test="${setorItem=='bebida'}">Bebida</c:when>
														<c:otherwise>Sobremesa</c:otherwise>
													</c:choose>
												</span>
												<c:choose>
													<c:when test="${setorItem=='cozinha'}">🍽️</c:when>
													<c:when test="${setorItem=='bebida'}">🥤</c:when>
													<c:otherwise>🍮</c:otherwise>
												</c:choose>
											</div>
										</c:otherwise>
									</c:choose>

									<div class="item-corpo">
										<div class="item-nome">${item.nome}</div>
										<div class="item-desc">${item.descricao}</div>

										<div class="item-rodape">
											<span class="item-preco">R$ <fmt:formatNumber
													value="${item.preco}" minFractionDigits="2"
													maxFractionDigits="2" /></span> <span class="item-tempo">⏱
												${item.tempoPreparoMin} min</span>
										</div>

										<%
										if (_isGerente) {
										%>
										<%-- Toggle de disponibilidade — visual novo (switch),
										     mas o contrato com o backend é o mesmo de antes:
										     um POST com acao=disponivel&valor=0|1. O switch
										     dispara esse POST sozinho via JS ao ser clicado
										     (auto-submit), sem precisar dos dois botões
										     Bloquear/Liberar que existiam antes. --%>
										<div class="item-disp-row">
											<span class="txt ${item.disponivel ? 'on' : 'off'}">${item.disponivel ? 'Disponível' : 'Indisponível'}</span>
											<label class="switch"> <input type="checkbox"
												${item.disponivel ? 'checked' : ''}
												onchange="toggleDisponivel(${item.idItem}, this.checked)">
												<span class="track"></span>
											</label>
										</div>

										<div class="acoes-gerente">
											<a
												href="${pageContext.request.contextPath}/app/cardapio?acao=editar&id=${item.idItem}"
												class="btn-icon" title="Editar">✏️</a>
											<form method="POST"
												action="${pageContext.request.contextPath}/app/cardapio"
												onsubmit="return confirm('Remover este item do cardápio?')"
												style="flex: 1">
												<input type="hidden" name="acao" value="excluir"> <input
													type="hidden" name="id" value="${item.idItem}">
												<button type="submit" class="btn-icon danger"
													title="Excluir" style="width: 100%">🗑️</button>
											</form>
										</div>
										<%
										} else {
										%>
										<div class="item-disp-row" style="margin-bottom: 0">
											<span class="txt ${item.disponivel ? 'on' : 'off'}">${item.disponivel ? 'Disponível' : 'Indisponível'}</span>
										</div>
										<%
										}
										%>
									</div>
								</div>
							</c:forEach>
						</div>
						<div class="empty-state" id="semResultado" style="display: none">
							<div class="icon">🔎</div>
							<p>Nenhum item encontrado.</p>
						</div>
					</c:otherwise>
				</c:choose>

			</main>
		</div>
	</div>

	<%-- Form oculto reaproveitado pelo toggle de disponibilidade —
	     evita ter um <form> duplicado por item; só troca os valores
	     de "id" e "valor" antes de enviar. --%>
	<form method="POST"
		action="${pageContext.request.contextPath}/app/cardapio"
		id="formDisponivel" style="display: none">
		<input type="hidden" name="acao" value="disponivel"> <input
			type="hidden" name="id" id="dispId"> <input type="hidden"
			name="valor" id="dispValor">
	</form>

	<%-- Modal "Gerenciar Categorias" (só GERENTE) — lista todas as
	     categorias já carregadas pela página (${categorias}, a mesma
	     lista usada no filtro por setor), com edição inline (nome +
	     setor) e exclusão. Tudo via fetch(), sem recarregar a página —
	     as ações de editar/excluir item de cardápio continuam
	     funcionando pelo padrão POST+redirect de sempre; só esta parte
	     de categorias é que ficou "AJAX" porque faz mais sentido para
	     uma lista pequena que o gerente mexe rapidamente. --%>
	<%
	if (_isGerente) {
	%>
	<div class="modal-overlay" id="modalCategorias">
		<div class="modal" style="max-width: 480px">
			<h3>⚙️ Gerenciar Categorias</h3>

			<div id="listaCategorias"
				style="max-height: 340px; overflow-y: auto; margin-bottom: 16px">
				<c:forEach var="cat" items="${categorias}">
					<div class="cat-linha" data-id="${cat.idCategoria}">
						<span class="cat-nome">${cat.nome}</span> <span
							class="cat-setor-badge">${cat.setor}</span>
						<div class="cat-acoes">
							<button type="button" class="btn-icon" title="Renomear"
								onclick="editarLinhaCategoria(this)">✏️</button>
							<button type="button" class="btn-icon danger" title="Excluir"
								onclick="excluirCategoria(${cat.idCategoria}, this)">🗑️</button>
						</div>
					</div>
				</c:forEach>
				<div id="semCategorias" class="text-muted"
					style="text-align:center;padding:20px;display:${empty categorias ? 'block' : 'none'}">
					Nenhuma categoria cadastrada ainda.</div>
			</div>

			<div
				style="border-top: 1px solid var(--border-subtle); padding-top: 14px">
				<div style="display: flex; gap: 8px">
					<input type="text" id="novaCatNomeModal"
						placeholder="Nome da nova categoria" maxlength="80"
						style="flex: 1; padding: 9px 12px; border: 1.5px solid var(--border-subtle-2); border-radius: var(--radius-sm); background: var(--bg-input); color: var(--text-primary); font-family: inherit">
					<select id="novaCatSetorModal"
						style="padding: 9px 8px; border: 1.5px solid var(--border-subtle-2); border-radius: var(--radius-sm); background: var(--bg-input); color: var(--text-primary); font-family: inherit">
						<option value="cozinha">Cozinha</option>
						<option value="bebida">Bebida</option>
						<option value="sobremesa">Sobremesa</option>
					</select>
					<button type="button" class="btn btn-primary btn-sm"
						onclick="adicionarCategoriaModal()">+ Adicionar</button>
				</div>
				<div id="erroCategoriasModal"
					style="display: none; color: var(--error-color); font-size: 12px; margin-top: 8px"></div>
			</div>

			<div class="modal-acoes">
				<button type="button" class="btn btn-secondary"
					onclick="fecharModalCategorias()" style="width: 100%">Fechar</button>
			</div>
		</div>
	</div>
	<%
	}
	%>

	<script>
	var setorAtual = 'todos';
	var buscaAtual = '';

	function toggleDisponivel(id, disponivelAgora) {
		document.getElementById('dispId').value = id;
		document.getElementById('dispValor').value = disponivelAgora ? '1' : '0';
		document.getElementById('formDisponivel').submit();
	}

	function filtrarSetor(setor, btn) {
		setorAtual = setor;
		document.querySelectorAll('.pill-filtro').forEach(function(b){ b.classList.remove('ativo'); });
		btn.classList.add('ativo');
		aplicarFiltros();
	}

	function buscar(valor) {
		buscaAtual = valor.toLowerCase();
		aplicarFiltros();
	}

	// Filtro combinado (setor + busca por nome) roda inteiramente no
	// navegador, sobre os cards já renderizados pelo servidor — não
	// dispara nenhuma requisição nova, então a busca responde na hora.
	function aplicarFiltros() {
		var cards = document.querySelectorAll('#gridItens .item-card');
		var visiveis = 0;
		cards.forEach(function(card){
			var bateSetor  = (setorAtual === 'todos' || card.dataset.setor === setorAtual);
			var bateBusca  = (buscaAtual === '' || card.dataset.nome.indexOf(buscaAtual) !== -1);
			var mostrar = bateSetor && bateBusca;
			card.style.display = mostrar ? '' : 'none';
			if (mostrar) visiveis++;
		});
		var semResultado = document.getElementById('semResultado');
		if (semResultado) semResultado.style.display = (visiveis === 0) ? '' : 'none';
	}

	// ================================================================
	// GERENCIAR CATEGORIAS — modal com CRUD via fetch()
	// ================================================================

	function abrirModalCategorias() {
		document.getElementById('erroCategoriasModal').style.display = 'none';
		document.getElementById('novaCatNomeModal').value = '';
		document.getElementById('modalCategorias').classList.add('aberto');
	}
	function fecharModalCategorias() {
		document.getElementById('modalCategorias').classList.remove('aberto');
	}

	function mostrarErroCategorias(msg) {
		var box = document.getElementById('erroCategoriasModal');
		box.textContent = msg;
		box.style.display = 'block';
	}

	function chamarApiCategoria(params) {
		return fetch('${pageContext.request.contextPath}/app/cardapio', {
			method: 'POST',
			headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
			body: params.toString()
		}).then(function(resp) {
			return resp.json().then(function(data) { return { ok: resp.ok, data: data }; });
		});
	}

	// ---- Criar (a partir do rodapé do modal de gerenciamento) ----
	function adicionarCategoriaModal() {
		var nome  = document.getElementById('novaCatNomeModal').value.trim();
		var setor = document.getElementById('novaCatSetorModal').value;
		if (!nome) { mostrarErroCategorias('Informe um nome para a categoria.'); return; }

		var params = new URLSearchParams();
		params.set('acao', 'criarCategoria');
		params.set('nome', nome);
		params.set('setor', setor);

		chamarApiCategoria(params).then(function(result) {
			if (!result.ok) { mostrarErroCategorias(result.data.erro || 'Erro ao criar categoria.'); return; }

			document.getElementById('semCategorias').style.display = 'none';
			var linha = document.createElement('div');
			linha.className = 'cat-linha';
			linha.dataset.id = result.data.id;
			linha.innerHTML =
				'<span class="cat-nome">' + escapeHtml(result.data.nome) + '</span>' +
				'<span class="cat-setor-badge">' + result.data.setor + '</span>' +
				'<div class="cat-acoes">' +
				'  <button type="button" class="btn-icon" title="Renomear" onclick="editarLinhaCategoria(this)">✏️</button>' +
				'  <button type="button" class="btn-icon danger" title="Excluir" onclick="excluirCategoria(' + result.data.id + ', this)">🗑️</button>' +
				'</div>';
			document.getElementById('listaCategorias').appendChild(linha);

			document.getElementById('novaCatNomeModal').value = '';
			document.getElementById('erroCategoriasModal').style.display = 'none';
		});
	}

	// ---- Editar (transforma a linha em campos editáveis) ----
	function editarLinhaCategoria(btn) {
		var linha = btn.closest('.cat-linha');
		var nomeAtual  = linha.querySelector('.cat-nome').textContent;
		var setorAtualCat = linha.querySelector('.cat-setor-badge').textContent;

		linha.innerHTML =
			'<input type="text" class="cat-edit-nome" value="' + escapeHtml(nomeAtual) + '" maxlength="80" ' +
			'  style="flex:1;padding:6px 8px;border:1.5px solid var(--primary);border-radius:6px;background:var(--bg-input);color:var(--text-primary);font-family:inherit;font-size:13px">' +
			'<select class="cat-edit-setor" style="padding:6px;border:1.5px solid var(--border-subtle-2);border-radius:6px;background:var(--bg-input);color:var(--text-primary);font-family:inherit;font-size:12px">' +
			'  <option value="cozinha"' + (setorAtualCat === 'cozinha' ? ' selected' : '') + '>Cozinha</option>' +
			'  <option value="bebida"'  + (setorAtualCat === 'bebida'  ? ' selected' : '') + '>Bebida</option>' +
			'  <option value="sobremesa"' + (setorAtualCat === 'sobremesa' ? ' selected' : '') + '>Sobremesa</option>' +
			'</select>' +
			'<div class="cat-acoes">' +
			'  <button type="button" class="btn-icon" title="Salvar" onclick="salvarLinhaCategoria(this)">💾</button>' +
			'  <button type="button" class="btn-icon" title="Cancelar" onclick="location.reload()">✕</button>' +
			'</div>';
	}

	function salvarLinhaCategoria(btn) {
		var linha = btn.closest('.cat-linha');
		var id    = linha.dataset.id;
		var nome  = linha.querySelector('.cat-edit-nome').value.trim();
		var setor = linha.querySelector('.cat-edit-setor').value;
		if (!nome) { mostrarErroCategorias('Informe um nome para a categoria.'); return; }

		var params = new URLSearchParams();
		params.set('acao', 'editarCategoria');
		params.set('id', id);
		params.set('nome', nome);
		params.set('setor', setor);

		chamarApiCategoria(params).then(function(result) {
			if (!result.ok) { mostrarErroCategorias(result.data.erro || 'Erro ao editar categoria.'); return; }

			linha.innerHTML =
				'<span class="cat-nome">' + escapeHtml(result.data.nome) + '</span>' +
				'<span class="cat-setor-badge">' + result.data.setor + '</span>' +
				'<div class="cat-acoes">' +
				'  <button type="button" class="btn-icon" title="Renomear" onclick="editarLinhaCategoria(this)">✏️</button>' +
				'  <button type="button" class="btn-icon danger" title="Excluir" onclick="excluirCategoria(' + result.data.id + ', this)">🗑️</button>' +
				'</div>';
			document.getElementById('erroCategoriasModal').style.display = 'none';

			// Como o nome/categoria pode ter mudado, a página precisa
			// recarregar para os filtros/selects do cardápio refletirem
			// o novo nome — mas só DEPOIS de fechar o modal, então o
			// gerente vê a confirmação visual da troca antes do reload.
			setTimeout(function(){ location.reload(); }, 600);
		});
	}

	// ---- Excluir (soft delete — some da lista, sem afetar itens já
	//      cadastrados com essa categoria, ver comentário no Controller) ----
	function excluirCategoria(id, btn) {
		if (!confirm('Excluir esta categoria? Itens do cardápio que já usam ela NÃO são afetados, só some das opções para novos itens.')) return;

		var params = new URLSearchParams();
		params.set('acao', 'excluirCategoria');
		params.set('id', id);

		chamarApiCategoria(params).then(function(result) {
			if (!result.ok) { mostrarErroCategorias(result.data.erro || 'Erro ao excluir categoria.'); return; }
			btn.closest('.cat-linha').remove();
			if (!document.querySelector('#listaCategorias .cat-linha')) {
				document.getElementById('semCategorias').style.display = 'block';
			}
		});
	}

	function escapeHtml(texto) {
		var div = document.createElement('div');
		div.textContent = texto;
		return div.innerHTML;
	}
	</script>
</body>
</html>
