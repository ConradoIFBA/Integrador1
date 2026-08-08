<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="br.com.restaurante.model.Usuario" %>
<%@ taglib prefix="c"   uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt"  %>
<%
    Usuario _u = (Usuario) session.getAttribute("usuarioLogado");
%>
<!DOCTYPE html>
<html lang="pt-BR">
<head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Novo Pedido — Integrador</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/style.css">
<style>
.layout{display:grid;grid-template-columns:1fr 380px;gap:24px;align-items:start}
.secao-titulo{font-size:15px;font-weight:700;color:#1e293b;margin-bottom:14px;padding-bottom:8px;border-bottom:2px solid #e2e8f0}
.item-selecao{display:flex;align-items:center;justify-content:space-between;padding:12px 14px;border:2px solid #e2e8f0;border-radius:8px;margin-bottom:8px;transition:border-color .2s}
.item-selecao:hover{border-color:#e85d27;background:#fff8f5}
.item-selecao.selecionado{border-color:#e85d27;background:#fff0ea}
.item-info .nome{font-size:14px;font-weight:600;color:#1e293b}
.item-info .preco{font-size:13px;color:#e85d27;font-weight:700}
.item-info .tempo{font-size:12px;color:#94a3b8}
.qtd-controle{display:flex;align-items:center;gap:8px}
.qtd-controle button{width:28px;height:28px;border-radius:50%;border:2px solid #e2e8f0;background:#fff;font-size:16px;font-weight:700;cursor:pointer;transition:all .2s;display:flex;align-items:center;justify-content:center}
.qtd-controle button:hover{border-color:#e85d27;color:#e85d27}
.qtd-valor{font-size:15px;font-weight:700;min-width:24px;text-align:center;color:#1e293b}
.sacola{position:sticky;top:80px}
.sacola-item{display:flex;justify-content:space-between;align-items:center;padding:10px 0;border-bottom:1px solid #f1f5f9;font-size:13px}
.sacola-item:last-child{border-bottom:none}
.sacola-total{display:flex;justify-content:space-between;padding-top:14px;font-size:16px;font-weight:800;color:#1e293b;border-top:2px solid #e2e8f0;margin-top:8px}
.sacola-vazia{text-align:center;padding:24px;color:#94a3b8;font-size:13px}
.filtros-cat{display:flex;gap:8px;flex-wrap:wrap;margin-bottom:16px}
.filtro-cat{padding:5px 14px;border:1px solid #e2e8f0;border-radius:14px;background:#fff;color:#64748b;font-size:12px;font-weight:600;cursor:pointer}
.filtro-cat.ativo{background:#1e293b;border-color:#1e293b;color:#fff}
</style>
</head>
<body>
<div class="main-container">
<%@ include file="/WEB-INF/views/shared/_sidebar.jsp" %>
<div class="main-content">
  <header class="topbar">
    <div class="topbar-left"><h2>Novo Pedido</h2></div>
    <div class="topbar-right">
      <a href="${pageContext.request.contextPath}/app/pedidos" class="btn btn-secondary btn-sm">← Voltar</a>
    </div>
  </header>
  <main class="content">
    <form method="POST" action="${pageContext.request.contextPath}/app/pedidos"
          id="formPedido" onsubmit="return prepararEnvio()">
      <input type="hidden" name="acao" value="criar">

      <div class="layout">

        <%-- ESQUERDA --%>
        <div>
          <div class="card" style="margin-bottom:20px">
            <div class="secao-titulo">📋 Dados do Pedido</div>

            <div style="display:grid;grid-template-columns:1fr 1fr;gap:16px">
              <div class="form-group" style="margin:0">
                <label>Tipo</label>
                <select name="tipo" id="tipoPedido" onchange="atualizarTipo()">
                  <option value="mesa">🪑 Mesa</option>
                  <option value="delivery">🛵 Delivery</option>
                </select>
              </div>

              <%-- CORRIGIDO: usa c:if em vez de scriptlet para selected --%>
              <div class="form-group" style="margin:0" id="grupaMesa">
                <label>Mesa</label>
                <select name="mesaId" id="selectMesa">
                  <option value="">Selecione...</option>
                  <c:forEach var="mesa" items="${mesas}">
                    <option value="${mesa.idMesa}"
                      <c:if test="${mesaIdSelecionada == mesa.idMesa}">selected</c:if>>
                      Mesa ${mesa.numero} (${mesa.capacidade} lugares)
                    </option>
                  </c:forEach>
                </select>
              </div>
            </div>

            <div style="display:grid;grid-template-columns:1fr 1fr;gap:16px;margin-top:14px">
              <div class="form-group" style="margin:0">
                <label>Seu identificador</label>
                <input type="text" name="operador" placeholder="Ex: A1" maxlength="20" required>
              </div>
              <div class="form-group" style="margin:0;display:flex;align-items:flex-end">
                <label style="display:flex;align-items:center;gap:8px;cursor:pointer;padding-bottom:4px">
                  <input type="checkbox" name="urgente" style="width:18px;height:18px;accent-color:#e85d27">
                  <span style="font-weight:600;color:#1e293b">🔴 Pedido urgente</span>
                </label>
              </div>
            </div>

            <div class="form-group" style="margin-top:14px;margin-bottom:0">
              <label>Observação</label>
              <input type="text" name="observacao" placeholder="Alguma observação geral?" maxlength="255">
            </div>
          </div>

          <div class="card">
            <div class="secao-titulo">🍽️ Selecione os Itens</div>
            <div class="filtros-cat" id="filtrosCat">
              <button type="button" class="filtro-cat ativo" onclick="filtrarCat('todos',this)">Todos</button>
            </div>
            <div id="listaItens">
              <c:forEach var="item" items="${itens}">
                <div class="item-selecao"
                     id="card-${item.idItem}"
                     data-cat="${item.categoriaId}"
                     data-nome="${item.nome}"
                     data-preco="${item.preco}">
                  <div class="item-info">
                    <div class="nome">${item.nome}</div>
                    <div class="preco">
                      R$ <fmt:formatNumber value="${item.preco}" minFractionDigits="2" maxFractionDigits="2"/>
                    </div>
                    <div class="tempo" data-cat-nome="${item.nomeCategoria}">
                      ⏱ ${item.tempoPreparoMin} min · ${item.nomeCategoria}
                    </div>
                  </div>
                  <div class="qtd-controle">
                    <button type="button" onclick="diminuir(${item.idItem})">−</button>
                    <span class="qtd-valor" id="qtd-${item.idItem}">0</span>
                    <button type="button" onclick="aumentar(${item.idItem},'${item.nome}',${item.preco})">+</button>
                  </div>
                </div>
              </c:forEach>
              <c:if test="${empty itens}">
                <div class="empty-state"><div class="icon">📋</div><p>Nenhum item disponível.</p></div>
              </c:if>
            </div>
          </div>
        </div>

        <%-- DIREITA: sacola --%>
        <div class="sacola">
          <div class="card">
            <div class="secao-titulo">🛒 Sacola</div>
            <div id="sacolaItens">
              <div class="sacola-vazia" id="sacolaVazia">Nenhum item adicionado ainda.</div>
            </div>
            <div class="sacola-total" id="sacolaTotal" style="display:none">
              <span>Total</span><span id="valorTotal">R$ 0,00</span>
            </div>
            <div style="margin-top:18px">
              <button type="submit" class="btn btn-primary" style="width:100%;padding:14px;font-size:15px">
                ✅ Confirmar Pedido
              </button>
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
          + '<span style="font-weight:700;color:#e85d27">R$ ' + sub.toFixed(2).replace('.',',') + '</span></div>';
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
  document.querySelectorAll('.filtro-cat').forEach(b => b.classList.remove('ativo'));
  btn.classList.add('ativo');
  document.querySelectorAll('.item-selecao').forEach(function(el) {
    el.style.display = (cat === 'todos' || el.dataset.cat == cat) ? '' : 'none';
  });
}

// Gera botões de categoria automaticamente
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
    btn.type = 'button'; btn.className = 'filtro-cat';
    btn.textContent = cats[cid];
    btn.setAttribute('data-cid', cid);
    btn.onclick = (function(c, b){ return function(){ filtrarCat(c, b); }; })(cid, btn);
    cont.appendChild(btn);
  }
});
</script>
</body></html>
