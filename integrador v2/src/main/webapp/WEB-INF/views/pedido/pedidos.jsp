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
<title>Pedidos — Integrador</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/style.css">
<style>
.pedido-card{background:#fff;border-radius:10px;box-shadow:0 2px 8px rgba(0,0,0,.08);padding:20px;margin-bottom:16px;border-left:4px solid #e2e8f0;transition:box-shadow .2s}
.pedido-card:hover{box-shadow:0 4px 16px rgba(0,0,0,.12)}
.pedido-card.status-aberto    {border-left-color:#3b82f6}
.pedido-card.status-em_preparo{border-left-color:#f59e0b}
.pedido-card.status-pronto    {border-left-color:#10b981}
.pedido-header{display:flex;align-items:center;justify-content:space-between;margin-bottom:12px}
.pedido-id{font-size:18px;font-weight:800;color:#1e293b}
.pedido-meta{font-size:13px;color:#64748b;display:flex;gap:16px;flex-wrap:wrap;margin-bottom:12px}
.pedido-itens{font-size:13px;color:#475569;margin-bottom:14px;background:#f8fafc;padding:10px 14px;border-radius:7px}
.pedido-total{font-size:16px;font-weight:700;color:#e85d27}
.pedido-acoes{display:flex;gap:8px;flex-wrap:wrap}
.pedido-acoes .btn{font-size:13px;padding:7px 14px}
.topo{display:flex;justify-content:space-between;align-items:center;margin-bottom:20px}
.tabs{display:flex;gap:4px;margin-bottom:20px}
.tab-btn{padding:8px 20px;border:2px solid #e2e8f0;border-radius:7px;background:#fff;color:#64748b;font-size:13px;font-weight:600;cursor:pointer;transition:all .2s}
.tab-btn.ativo{background:#e85d27;border-color:#e85d27;color:#fff}
</style>
</head>
<body>
<div class="main-container">
<%@ include file="/WEB-INF/views/shared/_sidebar.jsp" %>
<div class="main-content">
  <header class="topbar">
    <div class="topbar-left"><h2>Pedidos</h2></div>
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

    <div class="topo">
      <div class="tabs">
        <button class="tab-btn ativo" onclick="filtrarStatus('todos',this)">Todos</button>
        <button class="tab-btn" onclick="filtrarStatus('aberto',this)">Abertos</button>
        <button class="tab-btn" onclick="filtrarStatus('em_preparo',this)">Em preparo</button>
        <button class="tab-btn" onclick="filtrarStatus('pronto',this)">Prontos</button>
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
          <a href="${pageContext.request.contextPath}/app/pedidos?acao=novo"
             class="btn btn-primary" style="margin-top:16px">Criar primeiro pedido</a>
        </div>
      </c:when>
      <c:otherwise>
        <div id="listaPedidos">
          <c:forEach var="p" items="${pedidos}">
            <div class="pedido-card status-${p.status}" data-status="${p.status}">

              <div class="pedido-header">
                <span class="pedido-id">#${p.idPedido}</span>
                <c:choose>
                  <c:when test="${p.status=='aberto'}">
                    <span class="badge badge-info">Aberto</span>
                  </c:when>
                  <c:when test="${p.status=='em_preparo'}">
                    <span class="badge badge-warning">Em preparo</span>
                  </c:when>
                  <c:when test="${p.status=='pronto'}">
                    <span class="badge badge-success">Pronto ✓</span>
                  </c:when>
                  <c:otherwise>
                    <span class="badge">${p.status}</span>
                  </c:otherwise>
                </c:choose>
              </div>

              <div class="pedido-meta">
                <span>
                  <c:choose>
                    <c:when test="${p.tipo=='mesa'}">🪑 Mesa ${p.mesa.numero}</c:when>
                    <c:otherwise>🛵 Delivery</c:otherwise>
                  </c:choose>
                </span>
                <span>👤 ${p.identificadorOperador}</span>
                <span>🕐 ${p.dataAberturaFormatada}</span>
                <c:if test="${p.urgente}">
                  <span class="badge badge-urgente">🔴 Urgente</span>
                </c:if>
              </div>

              <%-- Itens do pedido --%>
              <div class="pedido-itens">
                <c:forEach var="item" items="${p.itens}" varStatus="vs">
                  ${item.quantidade}x ${item.nomeItem}
                  <c:if test="${!vs.last}"> · </c:if>
                </c:forEach>
              </div>

              <div style="display:flex;align-items:center;justify-content:space-between;flex-wrap:wrap;gap:10px">
                <span class="pedido-total">
                  Total: R$ <fmt:formatNumber value="${p.calcularTotal()}"
                              minFractionDigits="2" maxFractionDigits="2"/>
                </span>

                <div class="pedido-acoes">
                  <%-- Detalhes --%>
                  <a href="${pageContext.request.contextPath}/app/pedidos?acao=detalhe&id=${p.idPedido}"
                     class="btn btn-secondary">📋 Detalhes</a>

                  <%-- Avançar status --%>
                  <c:if test="${p.status!='entregue' && p.status!='cancelado'}">
                    <form method="POST" action="${pageContext.request.contextPath}/app/pedidos"
                          onsubmit="return pedirOperador(this)">
                      <input type="hidden" name="acao" value="avancarStatus">
                      <input type="hidden" name="id"   value="${p.idPedido}">
                      <input type="hidden" name="operador" class="campo-operador" value="">
                      <button type="submit" class="btn btn-primary">
                        <c:choose>
                          <c:when test="${p.status=='aberto'}">▶ Iniciar preparo</c:when>
                          <c:when test="${p.status=='em_preparo'}">✓ Marcar pronto</c:when>
                          <c:when test="${p.status=='pronto'}">🍽️ Entregar</c:when>
                        </c:choose>
                      </button>
                    </form>
                  </c:if>

                  <%-- Cancelar --%>
                  <c:if test="${p.status!='entregue' && p.status!='cancelado'}">
                    <form method="POST" action="${pageContext.request.contextPath}/app/pedidos"
                          onsubmit="return confirm('Cancelar o pedido #${p.idPedido}?')">
                      <input type="hidden" name="acao" value="cancelar">
                      <input type="hidden" name="id"   value="${p.idPedido}">
                      <input type="hidden" name="operador" value="${p.identificadorOperador}">
                      <button type="submit" class="btn btn-danger">✕ Cancelar</button>
                    </form>
                  </c:if>
                </div>
              </div>

            </div>
          </c:forEach>
        </div>
      </c:otherwise>
    </c:choose>

  </main>
</div>
</div>

<%-- Modal de identificação do operador --%>
<div id="modalOperador" style="display:none;position:fixed;inset:0;background:rgba(0,0,0,.5);z-index:2000;align-items:center;justify-content:center">
  <div style="background:#fff;border-radius:12px;padding:32px;width:100%;max-width:340px;box-shadow:0 20px 60px rgba(0,0,0,.3)">
    <h3 style="font-size:18px;margin-bottom:18px;color:#1e293b">Seu identificador</h3>
    <div class="form-group">
      <label for="inputOperador">Ex: A1, A2, G1</label>
      <input type="text" id="inputOperador" placeholder="Ex: A1" maxlength="20"
             style="width:100%;padding:10px 12px;border:2px solid #e2e8f0;border-radius:7px;font-size:14px">
    </div>
    <div style="display:flex;gap:10px;margin-top:8px">
      <button onclick="fecharModal()" class="btn btn-secondary" style="flex:1">Cancelar</button>
      <button onclick="confirmarOperador()" class="btn btn-primary" style="flex:1">Confirmar</button>
    </div>
  </div>
</div>

<script>
  var _formPendente = null;

  function pedirOperador(form) {
    _formPendente = form;
    document.getElementById('inputOperador').value = '';
    document.getElementById('modalOperador').style.display = 'flex';
    document.getElementById('inputOperador').focus();
    return false;
  }

  function confirmarOperador() {
    var op = document.getElementById('inputOperador').value.trim();
    if (!op) { alert('Informe seu identificador.'); return; }
    _formPendente.querySelector('.campo-operador').value = op;
    fecharModal();
    _formPendente.submit();
  }

  function fecharModal() {
    document.getElementById('modalOperador').style.display = 'none';
    _formPendente = null;
  }

  function filtrarStatus(status, btn) {
    document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('ativo'));
    btn.classList.add('ativo');
    document.querySelectorAll('.pedido-card').forEach(card => {
      card.style.display = (status === 'todos' || card.dataset.status === status) ? '' : 'none';
    });
  }

  document.getElementById('modalOperador').addEventListener('click', function(e) {
    if (e.target === this) fecharModal();
  });

  document.addEventListener('keydown', function(e) {
    if (e.key === 'Escape') fecharModal();
    if (e.key === 'Enter' && _formPendente) confirmarOperador();
  });
</script>
</body></html>
