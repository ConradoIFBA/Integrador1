<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="br.com.restaurante.model.Usuario" %>
<%@ taglib prefix="c"   uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt"  %>
<%
    Usuario _u = (Usuario) session.getAttribute("usuarioLogado");
    boolean _isGerente = "GERENTE".equals(_u.getPerfil());
%>
<!DOCTYPE html>
<html lang="pt-BR">
<head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Cardápio — Integrador</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/style.css">
<style>
.topo-cardapio{display:flex;align-items:center;justify-content:space-between;margin-bottom:20px}
.filtros{display:flex;gap:10px;flex-wrap:wrap;margin-bottom:24px}
.filtro-btn{padding:7px 18px;border:2px solid #e2e8f0;border-radius:20px;background:#fff;color:#64748b;font-size:13px;font-weight:600;cursor:pointer;transition:all .2s}
.filtro-btn:hover,.filtro-btn.ativo{background:#e85d27;border-color:#e85d27;color:#fff}
.secao-categoria{margin-bottom:32px}
.secao-titulo{font-size:16px;font-weight:700;color:#1e293b;padding-bottom:10px;border-bottom:2px solid #e2e8f0;margin-bottom:16px;display:flex;align-items:center;gap:8px}
.setor-badge{font-size:11px;font-weight:600;padding:2px 8px;border-radius:10px;background:#f1f5f9;color:#64748b;text-transform:uppercase}
.itens-grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(280px,1fr));gap:16px}
.item-card{background:#fff;border-radius:10px;box-shadow:0 2px 8px rgba(0,0,0,.08);padding:18px;border:2px solid transparent;transition:box-shadow .2s;position:relative}
.item-card:hover{box-shadow:0 4px 16px rgba(0,0,0,.12)}
.item-card.indisponivel{opacity:.55;border-color:#fecaca;background:#fff5f5}
.item-nome{font-size:15px;font-weight:700;color:#1e293b;margin-bottom:4px;padding-right:90px}
.item-desc{font-size:13px;color:#64748b;margin-bottom:12px;min-height:36px}
.item-rodape{display:flex;align-items:center;justify-content:space-between;margin-bottom:12px}
.item-preco{font-size:18px;font-weight:800;color:#e85d27}
.item-tempo{font-size:12px;color:#94a3b8}
.item-status{position:absolute;top:12px;right:12px;font-size:11px;font-weight:700;padding:2px 8px;border-radius:10px}
.status-disp{background:#d1fae5;color:#065f46}
.status-indisp{background:#fee2e2;color:#991b1b}
.acoes-gerente{display:flex;gap:6px;flex-wrap:wrap}
.acoes-gerente .btn{flex:1;padding:6px 10px;font-size:12px;text-align:center}
</style>
</head>
<body>
<div class="main-container">
<%@ include file="/WEB-INF/views/shared/_sidebar.jsp" %>
<div class="main-content">
  <header class="topbar">
    <div class="topbar-left"><h2>Cardápio</h2></div>
    <div class="topbar-right">
      <div class="user-info">
        <div class="user-avatar"><%= _u.getNome().substring(0,1).toUpperCase() %></div>
        <div class="user-details">
          <span class="name"><%= _u.getNome() %></span>
          <span class="role"><%= _u.getPerfil() %></span>
        </div>
      </div>
    </div>
  </header>
  <main class="content">

    <% if (_isGerente) { %>
    <div class="topo-cardapio">
      <div></div>
      <a href="${pageContext.request.contextPath}/app/cardapio?acao=novo" class="btn btn-primary">
        + Novo Item
      </a>
    </div>
    <% } %>

    <c:if test="${not empty msgSucesso}">
      <div class="alert alert-success">✓ ${msgSucesso}</div>
    </c:if>

    <div class="filtros">
      <button class="filtro-btn ativo" onclick="filtrar('todos',this)">🍽️ Todos</button>
      <c:forEach var="cat" items="${categorias}">
        <button class="filtro-btn" onclick="filtrar('cat-${cat.idCategoria}',this)">${cat.nome}</button>
      </c:forEach>
    </div>

    <c:forEach var="cat" items="${categorias}">
      <div class="secao-categoria" id="cat-${cat.idCategoria}">
        <div class="secao-titulo">
          <span>${cat.nome}</span>
          <span class="setor-badge">${cat.setor}</span>
        </div>
        <div class="itens-grid">
          <c:forEach var="item" items="${itens}">
            <c:if test="${item.categoriaId == cat.idCategoria}">
              <div class="item-card ${item.disponivel ? '' : 'indisponivel'}">

                <span class="item-status ${item.disponivel ? 'status-disp' : 'status-indisp'}">
                  ${item.disponivel ? '✓ Disponível' : '✕ Indisponível'}
                </span>

                <div class="item-nome">${item.nome}</div>
                <div class="item-desc">${item.descricao}</div>

                <div class="item-rodape">
                  <span class="item-preco">
                    R$ <fmt:formatNumber value="${item.preco}" minFractionDigits="2" maxFractionDigits="2"/>
                  </span>
                  <span class="item-tempo">⏱ ${item.tempoPreparoMin} min</span>
                </div>

                <% if (_isGerente) { %>
                <div class="acoes-gerente">
                  <a href="${pageContext.request.contextPath}/app/cardapio?acao=editar&id=${item.idItem}"
                     class="btn btn-secondary btn-sm">✏️ Editar</a>

                  <form method="POST" action="${pageContext.request.contextPath}/app/cardapio" style="flex:1">
                    <input type="hidden" name="acao"  value="disponivel">
                    <input type="hidden" name="id"    value="${item.idItem}">
                    <c:choose>
                      <c:when test="${item.disponivel}">
                        <input type="hidden" name="valor" value="0">
                        <button type="submit" class="btn btn-warning btn-sm" style="width:100%">🚫 Bloquear</button>
                      </c:when>
                      <c:otherwise>
                        <input type="hidden" name="valor" value="1">
                        <button type="submit" class="btn btn-success btn-sm" style="width:100%">✅ Liberar</button>
                      </c:otherwise>
                    </c:choose>
                  </form>

                  <form method="POST" action="${pageContext.request.contextPath}/app/cardapio"
                        onsubmit="return confirm('Remover este item do cardápio?')">
                    <input type="hidden" name="acao" value="excluir">
                    <input type="hidden" name="id"   value="${item.idItem}">
                    <button type="submit" class="btn btn-danger btn-sm">🗑️</button>
                  </form>
                </div>
                <% } %>

              </div>
            </c:if>
          </c:forEach>
        </div>
      </div>
    </c:forEach>

    <c:if test="${empty itens}">
      <div class="empty-state card">
        <div class="icon">📋</div>
        <p>Nenhum item no cardápio ainda.</p>
        <% if (_isGerente) { %>
        <a href="${pageContext.request.contextPath}/app/cardapio?acao=novo"
           class="btn btn-primary" style="margin-top:16px">+ Adicionar primeiro item</a>
        <% } %>
      </div>
    </c:if>

  </main>
</div>
</div>
<script>
function filtrar(alvo, btn) {
  document.querySelectorAll('.filtro-btn').forEach(b => b.classList.remove('ativo'));
  btn.classList.add('ativo');
  document.querySelectorAll('.secao-categoria').forEach(s => {
    s.style.display = (alvo === 'todos' || s.id === alvo) ? '' : 'none';
  });
}
</script>
</body></html>
