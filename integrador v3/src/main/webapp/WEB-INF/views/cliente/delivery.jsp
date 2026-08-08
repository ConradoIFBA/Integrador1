<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="br.com.restaurante.model.Usuario" %>
<%@ taglib prefix="c"   uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt"  %>
<%
    Usuario _u = (Usuario) session.getAttribute("usuarioLogado");
    String _msgErro = (String) request.getAttribute("msgErro");
    if (_msgErro == null) _msgErro = (String) session.getAttribute("msgErro");
    session.removeAttribute("msgErro");
%>
<!DOCTYPE html>
<html lang="pt-BR">
<head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Pedir Delivery — Integrador</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/style.css">
<style>
.layout{display:grid;grid-template-columns:1fr 360px;gap:24px;align-items:start}
.item-card{display:flex;align-items:center;justify-content:space-between;
  padding:12px 14px;border:2px solid #e2e8f0;border-radius:8px;margin-bottom:8px;
  transition:border-color .2s}
.item-card:hover{border-color:#e85d27;background:#fff8f5}
.item-card.selecionado{border-color:#e85d27;background:#fff0ea}
.item-nome{font-size:14px;font-weight:600;color:#1e293b}
.item-preco{font-size:13px;color:#e85d27;font-weight:700}
.item-cat{font-size:12px;color:#94a3b8}
.qtd-controle{display:flex;align-items:center;gap:8px}
.qtd-btn{width:28px;height:28px;border-radius:50%;border:2px solid #e2e8f0;
  background:#fff;font-size:16px;font-weight:700;cursor:pointer;
  display:flex;align-items:center;justify-content:center;transition:all .2s}
.qtd-btn:hover{border-color:#e85d27;color:#e85d27}
.qtd-val{font-size:15px;font-weight:700;min-width:22px;text-align:center}
.sacola{position:sticky;top:80px}
.sacola-item{display:flex;justify-content:space-between;padding:9px 0;
  border-bottom:1px solid #f1f5f9;font-size:13px}
.sacola-item:last-child{border-bottom:none}
.sacola-total{display:flex;justify-content:space-between;padding-top:12px;
  font-size:16px;font-weight:800;border-top:2px solid #e2e8f0;margin-top:6px}
.vazia{text-align:center;padding:24px;color:#94a3b8;font-size:13px}
.secao-titulo{font-size:15px;font-weight:700;color:#1e293b;margin-bottom:12px;
  padding-bottom:8px;border-bottom:2px solid #e2e8f0}
.filtros{display:flex;gap:8px;flex-wrap:wrap;margin-bottom:14px}
.filtro-btn{padding:5px 14px;border:1px solid #e2e8f0;border-radius:14px;
  background:#fff;color:#64748b;font-size:12px;font-weight:600;cursor:pointer}
.filtro-btn.ativo{background:#1e293b;border-color:#1e293b;color:#fff}
</style>
</head>
<body>
<div class="main-container">
<%@ include file="/WEB-INF/views/shared/_sidebar.jsp" %>
<div class="main-content">
  <header class="topbar">
    <div class="topbar-left"><h2>🛵 Pedir Delivery</h2></div>
    <div class="topbar-right">
      <div class="user-info">
        <div class="user-avatar"><%= _u.getNome().substring(0,1).toUpperCase() %></div>
        <div class="user-details">
          <span class="name"><%= _u.getNome() %></span>
          <span class="role">Cliente</span>
        </div>
      </div>
    </div>
  </header>
  <main class="content">

    <% if (_msgErro != null && !_msgErro.isEmpty()) { %>
      <div class="alert alert-error">✕ <%= _msgErro %></div>
    <% } %>

    <form method="POST" action="${pageContext.request.contextPath}/app/cliente/delivery"
          id="formDelivery" onsubmit="return prepararEnvio()">

      <div class="layout">

        <%-- Cardápio --%>
        <div>
          <div class="card">
            <div class="secao-titulo">🍽️ Escolha seus itens</div>

            <div class="filtros" id="filtrosCat">
              <button type="button" class="filtro-btn ativo"
                      onclick="filtrar('todos',this)">Todos</button>
            </div>

            <c:choose>
              <c:when test="${empty itens}">
                <div class="empty-state">
                  <div class="icon">📋</div><p>Nenhum item disponível.</p>
                </div>
              </c:when>
              <c:otherwise>
                <c:forEach var="item" items="${itens}">
                  <div class="item-card" id="card-${item.idItem}"
                       data-cat="${item.categoriaId}"
                       data-cat-nome="${item.nomeCategoria}">
                    <div>
                      <div class="item-nome">${item.nome}</div>
                      <div class="item-preco">
                        R$ <fmt:formatNumber value="${item.preco}"
                             minFractionDigits="2" maxFractionDigits="2"/>
                      </div>
                      <div class="item-cat">
                        ${item.nomeCategoria} · ⏱ ${item.tempoPreparoMin} min
                      </div>
                    </div>
                    <div class="qtd-controle">
                      <button type="button" class="qtd-btn"
                              onclick="diminuir(${item.idItem})">−</button>
                      <span class="qtd-val" id="qtd-${item.idItem}">0</span>
                      <button type="button" class="qtd-btn"
                              onclick="aumentar(${item.idItem},'${item.nome}',${item.preco})">+</button>
                    </div>
                  </div>
                </c:forEach>
              </c:otherwise>
            </c:choose>

            <%-- Observação --%>
            <div class="form-group" style="margin-top:16px;margin-bottom:0">
              <label>Observação (opcional)</label>
              <input type="text" name="observacao"
                     placeholder="Ex: sem cebola, ponto da carne..."
                     maxlength="255">
            </div>
          </div>
        </div>

        <%-- Sacola --%>
        <div class="sacola">
          <div class="card">
            <div class="secao-titulo">🛒 Sua sacola</div>
            <div id="sacolaItens">
              <div class="vazia">Nenhum item ainda.</div>
            </div>
            <div class="sacola-total" id="sacolaTotal" style="display:none">
              <span>Total</span>
              <span id="valorTotal" style="color:#e85d27">R$ 0,00</span>
            </div>
            <div style="margin-top:16px">
              <button type="submit"
                      class="btn btn-primary"
                      style="width:100%;padding:13px;font-size:15px">
                ✅ Confirmar Pedido
              </button>
            </div>
            <div style="margin-top:10px">
              <a href="${pageContext.request.contextPath}/app/cliente/meus-pedidos"
                 class="btn btn-secondary" style="width:100%;text-align:center">
                Ver meus pedidos
              </a>
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
  if (!sacola[id]) sacola[id] = { nome, preco: parseFloat(preco), qtd: 0 };
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
    var it = sacola[id], sub = it.preco * it.qtd;
    total += sub;
    html += '<div class="sacola-item"><span>' + it.qtd + 'x ' + it.nome + '</span>'
          + '<span style="font-weight:700;color:#e85d27">R$ '
          + sub.toFixed(2).replace('.', ',') + '</span></div>';
  }
  div.innerHTML = html || '<div class="vazia">Nenhum item ainda.</div>';
  var tot = document.getElementById('sacolaTotal');
  tot.style.display = html ? 'flex' : 'none';
  document.getElementById('valorTotal').textContent =
    'R$ ' + total.toFixed(2).replace('.', ',');
}

function prepararEnvio() {
  if (Object.keys(sacola).length === 0) {
    alert('Adicione pelo menos um item.');
    return false;
  }
  var cont = document.getElementById('inputsOcultos');
  cont.innerHTML = '';
  for (var id in sacola) {
    cont.innerHTML +=
      '<input type="hidden" name="itemId"    value="' + id + '">'
    + '<input type="hidden" name="quantidade" value="' + sacola[id].qtd + '">';
  }
  return true;
}

function filtrar(cat, btn) {
  document.querySelectorAll('.filtro-btn').forEach(b => b.classList.remove('ativo'));
  btn.classList.add('ativo');
  document.querySelectorAll('.item-card').forEach(el => {
    el.style.display = (cat === 'todos' || el.dataset.cat == cat) ? '' : 'none';
  });
}

// Gera botões de categoria
window.addEventListener('DOMContentLoaded', function() {
  var cats = {}, cont = document.getElementById('filtrosCat');
  document.querySelectorAll('.item-card').forEach(function(el) {
    if (!cats[el.dataset.cat]) cats[el.dataset.cat] = el.dataset.catNome;
  });
  for (var cid in cats) {
    var btn = document.createElement('button');
    btn.type = 'button'; btn.className = 'filtro-btn';
    btn.textContent = cats[cid];
    btn.onclick = (function(c, b) { return function() { filtrar(c, b); }; })(cid, btn);
    cont.appendChild(btn);
  }
});
</script>
</body>
</html>
