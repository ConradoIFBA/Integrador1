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
<title>Meus Pedidos — Integrador</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/style.css">

<%-- Auto-refresh a cada 30s para atualizar o status --%>
<meta http-equiv="refresh" content="30">

<style>
.pedido-card{background:#fff;border-radius:10px;box-shadow:0 2px 8px rgba(0,0,0,.08);
  padding:20px;margin-bottom:16px;border-left:4px solid #e2e8f0}
.pedido-card.status-aberto    {border-left-color:#3b82f6}
.pedido-card.status-em_preparo{border-left-color:#f59e0b}
.pedido-card.status-pronto    {border-left-color:#10b981}
.pedido-card.status-entregue  {border-left-color:#94a3b8}
.pedido-card.status-cancelado {border-left-color:#ef4444;opacity:.7}
.pedido-header{display:flex;justify-content:space-between;align-items:center;margin-bottom:10px}
.pedido-id{font-size:17px;font-weight:800;color:#1e293b}
.pedido-itens{font-size:13px;color:#475569;background:#f8fafc;
  padding:9px 12px;border-radius:7px;margin-bottom:12px}
.pedido-footer{display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:8px}
.pedido-total{font-size:15px;font-weight:700;color:#e85d27}
/* Barra de progresso do pedido */
.progresso{display:flex;align-items:center;gap:4px;margin-bottom:12px;flex-wrap:wrap}
.prog-etapa{display:flex;align-items:center;gap:4px;font-size:12px;color:#94a3b8}
.prog-etapa.ativa{color:#e85d27;font-weight:700}
.prog-etapa.feita{color:#10b981;font-weight:600}
.prog-sep{color:#e2e8f0;font-size:14px}
.refresh-bar{background:#1e293b;color:rgba(255,255,255,.8);text-align:center;
  font-size:13px;padding:6px;position:sticky;top:0;z-index:200}
.refresh-bar span{font-weight:700;color:#e85d27}
</style>
</head>
<body>
<div class="main-container">
<%@ include file="/WEB-INF/views/shared/_sidebar.jsp" %>
<div class="main-content">

  <div class="refresh-bar">
    Atualizando em <span id="contador">30</span>s
    &nbsp;·&nbsp;
    <a href="" style="color:#e85d27;text-decoration:none">🔄 Atualizar agora</a>
  </div>

  <header class="topbar">
    <div class="topbar-left"><h2>🧾 Meus Pedidos</h2></div>
    <div class="topbar-right">
      <a href="${pageContext.request.contextPath}/app/cliente/delivery"
         class="btn btn-primary btn-sm">+ Novo Delivery</a>
      <div class="user-info" style="margin-left:8px">
        <div class="user-avatar"><%= _u.getNome().substring(0,1).toUpperCase() %></div>
        <div class="user-details">
          <span class="name"><%= _u.getNome() %></span>
          <span class="role">Cliente</span>
        </div>
      </div>
    </div>
  </header>

  <main class="content">

    <c:if test="${not empty msgSucesso}">
      <div class="alert alert-success">✓ ${msgSucesso}</div>
    </c:if>

    <c:choose>
      <c:when test="${empty pedidos}">
        <div class="empty-state card">
          <div class="icon">🧾</div>
          <p>Você ainda não fez nenhum pedido.</p>
          <a href="${pageContext.request.contextPath}/app/cliente/delivery"
             class="btn btn-primary" style="margin-top:16px">Fazer meu primeiro pedido</a>
        </div>
      </c:when>
      <c:otherwise>
        <c:forEach var="p" items="${pedidos}">
          <div class="pedido-card status-${p.status}">

            <div class="pedido-header">
              <span class="pedido-id">Pedido #${p.idPedido}</span>
              <c:choose>
                <c:when test="${p.status=='aberto'}">
                  <span class="badge badge-info">⏳ Recebido</span>
                </c:when>
                <c:when test="${p.status=='em_preparo'}">
                  <span class="badge badge-warning">👨‍🍳 Preparando</span>
                </c:when>
                <c:when test="${p.status=='pronto'}">
                  <span class="badge badge-success">✅ Pronto</span>
                </c:when>
                <c:when test="${p.status=='entregue'}">
                  <span class="badge" style="background:#f1f5f9;color:#64748b">📦 Entregue</span>
                </c:when>
                <c:when test="${p.status=='cancelado'}">
                  <span class="badge badge-danger">✕ Cancelado</span>
                </c:when>
              </c:choose>
            </div>

            <%-- Barra de progresso visual --%>
            <c:if test="${p.status != 'cancelado'}">
              <div class="progresso">
                <span class="prog-etapa ${p.status=='aberto' ? 'ativa' : 'feita'}">
                  ${p.status=='aberto' ? '📥' : '✓'} Recebido
                </span>
                <span class="prog-sep">›</span>
                <span class="prog-etapa ${p.status=='em_preparo' ? 'ativa' :
                    (p.status=='pronto'||p.status=='entregue') ? 'feita' : ''}">
                  ${(p.status=='pronto'||p.status=='entregue') ? '✓' : '🍳'} Preparando
                </span>
                <span class="prog-sep">›</span>
                <span class="prog-etapa ${p.status=='pronto' ? 'ativa' :
                    p.status=='entregue' ? 'feita' : ''}">
                  ${p.status=='entregue' ? '✓' : '🔔'} Pronto
                </span>
                <span class="prog-sep">›</span>
                <span class="prog-etapa ${p.status=='entregue' ? 'feita' : ''}">
                  ${p.status=='entregue' ? '✓' : '📦'} Entregue
                </span>
              </div>
            </c:if>

            <%-- Itens --%>
            <div class="pedido-itens">
              <c:forEach var="item" items="${p.itens}" varStatus="vs">
                ${item.quantidade}x ${item.nomeItem}<c:if test="${!vs.last}"> · </c:if>
              </c:forEach>
            </div>

            <div class="pedido-footer">
              <div>
                <span class="pedido-total">
                  R$ <fmt:formatNumber value="${p.calcularTotal()}"
                       minFractionDigits="2" maxFractionDigits="2"/>
                </span>
                <span class="text-muted" style="font-size:12px;margin-left:10px">
                  ${p.dataAberturaFormatada}
                </span>
              </div>
              <c:if test="${not empty p.observacao}">
                <span style="font-size:12px;color:#64748b">📝 ${p.observacao}</span>
              </c:if>
            </div>

          </div>
        </c:forEach>
      </c:otherwise>
    </c:choose>

  </main>
</div>
</div>

<script>
var seg = 30;
setInterval(function() {
  seg--;
  document.getElementById('contador').textContent = seg;
  if (seg <= 0) location.reload();
}, 1000);
</script>
</body>
</html>
