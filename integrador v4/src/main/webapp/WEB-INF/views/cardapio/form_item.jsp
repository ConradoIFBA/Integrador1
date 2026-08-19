<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@ page import="br.com.restaurante.model.Usuario"%>
<%@ page import="br.com.restaurante.model.Cardapio"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt"%>
<%--
    ================================================================
    FORM_ITEM.JSP — CRIAR/EDITAR ITEM DO CARDÁPIO (v5 — upload de imagem)
    ================================================================
    NOVO NESTA VERSÃO: campo de upload de imagem, com preview antes
    de enviar e preview da foto atual ao editar um item que já tem
    uma. Duas mudanças técnicas necessárias para isso funcionar:

    1. enctype="multipart/form-data" no <form> — sem isso, o
       navegador nem envia os bytes do arquivo, só o nome dele como
       texto. É o CardapioController (@MultipartConfig) que sabe ler
       esse tipo de requisição.
    2. O <input type="file"> preview usa só JavaScript puro
       (URL.createObjectURL) — não depende de nenhuma lib, e mostra
       a imagem escolhida ANTES mesmo de enviar o formulário, então o
       usuário vê o que está prestes a subir.

    O restante da página (validação de preço/tempo, campos de
    categoria/nome/descrição) continua exatamente igual.
    ================================================================
--%>
<%
Usuario _u = (Usuario) session.getAttribute("usuarioLogado");
Cardapio _item = (Cardapio) request.getAttribute("item");
boolean _editando = _item != null;
%>
<!DOCTYPE html>
<html lang="pt-BR">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title><%=_editando ? "Editar Item" : "Novo Item"%> —
	Integrador</title>
<link rel="stylesheet"
	href="${pageContext.request.contextPath}/assets/style.css">
<style>
.form-card {
	max-width: 600px
}

.form-row {
	display: grid;
	grid-template-columns: 1fr 1fr;
	gap: 16px
}

.form-footer {
	display: flex;
	gap: 12px;
	margin-top: 8px
}

.form-footer .btn {
	flex: 1;
	padding: 12px;
	font-size: 15px
}

/* ── Upload de imagem com preview ── */
.upload-imagem {
	display: flex;
	align-items: center;
	gap: 16px;
	padding: 14px;
	border: 1.5px dashed var(--border-subtle-2);
	border-radius: var(--radius-sm);
	background: var(--bg-input)
}

.upload-preview {
	width: 88px;
	height: 88px;
	border-radius: var(--radius-sm);
	object-fit: cover;
	flex-shrink: 0;
	background: var(--bg-card-hover);
	display: flex;
	align-items: center;
	justify-content: center;
	font-size: 28px;
	color: var(--text-muted);
	border: 1px solid var(--border-subtle)
}

.upload-preview img {
	width: 100%;
	height: 100%;
	object-fit: cover;
	border-radius: var(--radius-sm)
}

.upload-info {
	flex: 1
}

.upload-info .dica {
	font-size: 12px;
	color: var(--text-secondary);
	margin-top: 4px
}

.upload-info input[type=file] {
	font-size: 13px;
	color: var(--text-primary);
	width: 100%
}
</style>
</head>
<body>
	<div class="main-container">
		<%@ include file="/WEB-INF/views/shared/_sidebar.jsp"%>
		<div class="main-content">

			<header class="topbar">
				<div class="topbar-left">
					<h2><%=_editando ? "Editar Item" : "Novo Item no Cardápio"%></h2>
				</div>
				<div class="topbar-right">
					<a href="${pageContext.request.contextPath}/app/cardapio"
						class="btn btn-secondary btn-sm">← Voltar</a>
				</div>
			</header>

			<main class="content">
				<div class="card form-card">
					<form method="POST"
						action="${pageContext.request.contextPath}/app/cardapio"
						enctype="multipart/form-data" onsubmit="return validar()">

						<input type="hidden" name="acao" value="salvar">
						<c:if test="${not empty item}">
							<input type="hidden" name="id" value="${item.idItem}">
						</c:if>

						<div class="form-group">
							<label>Foto do item</label>
							<div class="upload-imagem">
								<div class="upload-preview" id="previewBox">
									<c:choose>
										<c:when test="${not empty item && item.temImagem}">
											<img id="previewImg"
												src="${pageContext.request.contextPath}/imagens/cardapio/${item.imagem}"
												alt="">
										</c:when>
										<c:otherwise>
											<span id="previewIcon">🍽️</span>
											<img id="previewImg" style="display: none" alt="">
										</c:otherwise>
									</c:choose>
								</div>
								<div class="upload-info">
									<input type="file" name="imagem"
										accept="image/jpeg,image/png,image/webp"
										onchange="previewImagem(this)">
									<div class="dica">
										JPG, PNG ou WEBP — até 3 MB.
										<c:if test="${not empty item && item.temImagem}">Deixe em branco para manter a foto atual.</c:if>
									</div>
								</div>
							</div>
						</div>

						<div class="form-group">
							<label for="categoriaId"
								style="display: flex; justify-content: space-between; align-items: center">
								<span>Categoria <span style="color: var(--error-color)">*</span></span>
								<a href="javascript:void(0)" onclick="abrirModalCategoria()"
								style="font-size: 12px; font-weight: 600; color: var(--primary); text-transform: none">+
									Nova categoria</a>
							</label> <select id="categoriaId" name="categoriaId" required>
								<option value="">Selecione...</option>
								<c:forEach var="cat" items="${categorias}">
									<option value="${cat.idCategoria}"
										<c:if test="${not empty item && item.categoriaId == cat.idCategoria}">selected</c:if>>
										${cat.nome} (${cat.setor})</option>
								</c:forEach>
							</select>
						</div>

						<div class="form-group">
							<label for="nome">Nome do item <span
								style="color: var(--error-color)">*</span></label> <input type="text"
								id="nome" name="nome" placeholder="Ex: Frango grelhado"
								value="${not empty item ? item.nome : ''}" required
								maxlength="120">
						</div>

						<div class="form-group">
							<label for="descricao">Descrição</label>
							<textarea id="descricao" name="descricao"
								placeholder="Ex: Filé de frango com legumes salteados"
								maxlength="500">${not empty item ? item.descricao : ''}</textarea>
						</div>

						<div class="form-row">
							<div class="form-group">
								<label for="preco">Preço (R$) <span
									style="color: var(--error-color)">*</span></label> <input type="number"
									id="preco" name="preco" placeholder="0.00" step="0.01"
									min="0.01" value="${not empty item ? item.preco : ''}" required>
							</div>
							<div class="form-group">
								<label for="tempoPreparoMin">Tempo preparo (min) <span
									style="color: var(--error-color)">*</span></label> <input type="number"
									id="tempoPreparoMin" name="tempoPreparoMin"
									placeholder="Ex: 15" min="1" max="120"
									value="${not empty item ? item.tempoPreparoMin : ''}" required>
							</div>
						</div>

						<div class="form-footer">
							<a href="${pageContext.request.contextPath}/app/cardapio"
								class="btn btn-secondary">Cancelar</a>
							<button type="submit" class="btn btn-primary">
								<%=_editando ? "💾 Salvar alterações" : "➕ Adicionar item"%>
							</button>
						</div>

					</form>
				</div>
			</main>
		</div>
	</div>

	<%-- Modal "+ Nova categoria" — criada via fetch() para não perder
	     o restante do formulário já preenchido (ver comentário do
	     método criarCategoria() no CardapioController para o
	     detalhe de por que isso não é um <form> comum). --%>
	<div class="modal-overlay" id="modalCategoria">
		<div class="modal">
			<h3>Nova Categoria</h3>
			<div class="form-group">
				<label for="novaCategoriaNome">Nome</label> <input type="text"
					id="novaCategoriaNome" placeholder="Ex: Vegano" maxlength="80">
			</div>
			<div class="form-group">
				<label for="novaCategoriaSetor">Setor</label> <select
					id="novaCategoriaSetor">
					<option value="cozinha">Cozinha (pratos)</option>
					<option value="bebida">Bebida</option>
					<option value="sobremesa">Sobremesa</option>
				</select>
			</div>
			<div id="erroCategoria"
				style="display: none; color: var(--error-color); font-size: 12px; margin-bottom: 10px"></div>
			<div class="modal-acoes">
				<button type="button" class="btn btn-secondary"
					onclick="fecharModalCategoria()">Cancelar</button>
				<button type="button" class="btn btn-primary"
					onclick="salvarCategoria()">Criar categoria</button>
			</div>
		</div>
	</div>

	<script>
function validar() {
  var preco = parseFloat(document.getElementById('preco').value);
  if (isNaN(preco) || preco <= 0) {
    alert('Informe um preço válido maior que zero.'); return false;
  }
  var tempo = parseInt(document.getElementById('tempoPreparoMin').value);
  if (isNaN(tempo) || tempo < 1) {
    alert('Informe um tempo de preparo válido (mínimo 1 minuto).'); return false;
  }
  return true;
}

// Mostra a imagem escolhida imediatamente, antes de enviar o
// formulário — usa URL.createObjectURL, que cria uma URL temporária
// apontando pros bytes do arquivo já selecionado no navegador (não
// sobe nada ao servidor só para pré-visualizar).
function previewImagem(input) {
  if (!input.files || !input.files[0]) return;
  var img = document.getElementById('previewImg');
  var icon = document.getElementById('previewIcon');
  img.src = URL.createObjectURL(input.files[0]);
  img.style.display = 'block';
  if (icon) icon.style.display = 'none';
}

function abrirModalCategoria() {
  document.getElementById('novaCategoriaNome').value = '';
  document.getElementById('erroCategoria').style.display = 'none';
  document.getElementById('modalCategoria').classList.add('aberto');
  document.getElementById('novaCategoriaNome').focus();
}
function fecharModalCategoria() {
  document.getElementById('modalCategoria').classList.remove('aberto');
}

// Cria a categoria via fetch() (sem recarregar a página) e, se der
// certo, adiciona a nova opção no <select> de categorias do
// formulário principal e já deixa ela selecionada — o gerente
// continua exatamente de onde parou preenchendo o resto do item.
function salvarCategoria() {
  var nome  = document.getElementById('novaCategoriaNome').value.trim();
  var setor = document.getElementById('novaCategoriaSetor').value;
  var erroBox = document.getElementById('erroCategoria');

  if (!nome) {
    erroBox.textContent = 'Informe um nome para a categoria.';
    erroBox.style.display = 'block';
    return;
  }

  var params = new URLSearchParams();
  params.set('acao', 'criarCategoria');
  params.set('nome', nome);
  params.set('setor', setor);

  fetch('${pageContext.request.contextPath}/app/cardapio', {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: params.toString()
  })
  .then(function(resp) { return resp.json().then(function(data) { return { ok: resp.ok, data: data }; }); })
  .then(function(result) {
    if (!result.ok) {
      erroBox.textContent = result.data.erro || 'Erro ao criar categoria.';
      erroBox.style.display = 'block';
      return;
    }
    var select = document.getElementById('categoriaId');
    var opt = document.createElement('option');
    opt.value = result.data.id;
    opt.textContent = result.data.nome + ' (' + result.data.setor + ')';
    opt.selected = true;
    select.appendChild(opt);
    fecharModalCategoria();
  })
  .catch(function() {
    erroBox.textContent = 'Erro de conexão. Tente novamente.';
    erroBox.style.display = 'block';
  });
}
</script>
</body>
</html>
