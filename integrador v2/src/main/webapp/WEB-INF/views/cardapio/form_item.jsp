<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="br.com.restaurante.model.Usuario" %>
<%@ page import="br.com.restaurante.model.ItemCardapio" %>
<%@ taglib prefix="c"   uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt"  %>
<%
    Usuario _u     = (Usuario) session.getAttribute("usuarioLogado");
    ItemCardapio _item = (ItemCardapio) request.getAttribute("item");
    boolean _editando  = _item != null;
%>
<!DOCTYPE html>
<html lang="pt-BR">
<head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title><%= _editando ? "Editar Item" : "Novo Item" %> — Integrador</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/style.css">
<style>
.form-card{background:#fff;border-radius:12px;box-shadow:0 2px 8px rgba(0,0,0,.1);padding:32px;max-width:600px}
.form-row{display:grid;grid-template-columns:1fr 1fr;gap:16px}
.form-footer{display:flex;gap:12px;margin-top:8px}
.form-footer .btn{flex:1;padding:12px;font-size:15px}
</style>
</head>
<body>
<div class="main-container">
<%@ include file="/WEB-INF/views/shared/_sidebar.jsp" %>
<div class="main-content">

  <header class="topbar">
    <div class="topbar-left">
      <h2><%= _editando ? "Editar Item" : "Novo Item no Cardápio" %></h2>
    </div>
    <div class="topbar-right">
      <a href="${pageContext.request.contextPath}/app/cardapio"
         class="btn btn-secondary btn-sm">← Voltar</a>
    </div>
  </header>

  <main class="content">
    <div class="form-card">
      <form method="POST" action="${pageContext.request.contextPath}/app/cardapio"
            onsubmit="return validar()">

        <input type="hidden" name="acao" value="salvar">
        <c:if test="${not empty item}">
          <input type="hidden" name="id" value="${item.idItem}">
        </c:if>

        <%-- Categoria — CORRIGIDO: usa JSTL c:if em vez de scriptlet --%>
        <div class="form-group">
          <label for="categoriaId">Categoria <span style="color:#ef4444">*</span></label>
          <select id="categoriaId" name="categoriaId" required>
            <option value="">Selecione...</option>
            <c:forEach var="cat" items="${categorias}">
              <option value="${cat.idCategoria}"
                <c:if test="${not empty item && item.categoriaId == cat.idCategoria}">selected</c:if>>
                ${cat.nome} (${cat.setor})
              </option>
            </c:forEach>
          </select>
        </div>

        <%-- Nome --%>
        <div class="form-group">
          <label for="nome">Nome do item <span style="color:#ef4444">*</span></label>
          <input type="text"
                 id="nome"
                 name="nome"
                 placeholder="Ex: Frango grelhado"
                 value="${not empty item ? item.nome : ''}"
                 required
                 maxlength="120">
        </div>

        <%-- Descrição --%>
        <div class="form-group">
          <label for="descricao">Descrição</label>
          <textarea id="descricao"
                    name="descricao"
                    placeholder="Ex: Filé de frango com legumes salteados"
                    maxlength="500">${not empty item ? item.descricao : ''}</textarea>
        </div>

        <%-- Preço e tempo --%>
        <div class="form-row">
          <div class="form-group">
            <label for="preco">Preço (R$) <span style="color:#ef4444">*</span></label>
            <input type="number"
                   id="preco"
                   name="preco"
                   placeholder="0.00"
                   step="0.01"
                   min="0.01"
                   value="${not empty item ? item.preco : ''}"
                   required>
          </div>
          <div class="form-group">
            <label for="tempoPreparoMin">Tempo preparo (min) <span style="color:#ef4444">*</span></label>
            <input type="number"
                   id="tempoPreparoMin"
                   name="tempoPreparoMin"
                   placeholder="Ex: 15"
                   min="1"
                   max="120"
                   value="${not empty item ? item.tempoPreparoMin : ''}"
                   required>
          </div>
        </div>

        <div class="form-footer">
          <a href="${pageContext.request.contextPath}/app/cardapio"
             class="btn btn-secondary">Cancelar</a>
          <button type="submit" class="btn btn-primary">
            <%= _editando ? "💾 Salvar alterações" : "➕ Adicionar item" %>
          </button>
        </div>

      </form>
    </div>
  </main>
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
</script>
</body>
</html>
