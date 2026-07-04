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
<title>Pedido #${pedido.idPedido} — Integrador</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/style.css">
<style>
.detalhe-grid{display:grid;grid-template-columns:1fr 1fr;gap:24px}
.info-row{display:flex;justify-content:space-between;padding:8px 0;
  border-bottom:1px solid #f1f5f9;font-size:14px}
.info-row:last-child{border-bottom:none}
.info-label{color:#64748b;font-weight:600}
.info-valor{color:#1e293b;font-weight:500}
.linha-total{display:flex;justify-content:space-between;padding:14px 0 0;
  font-size:18px;font-weight:800;border-top:2px solid #e2e8f0;margin-top:8px}
</style>
</head>
<body>
<div class="main-container">
<%@ include file="/WEB-INF/views/shared/_sidebar.jsp" %>
<div class="main-content">
  <header class="topbar">
    <div class="topbar-left"><h2>Pedido #${pedido.idPedido}</h2></div>
    <div class="topbar-right">
      <a href="${pageContext.request.contextPath}/app/pedidos"
         class="btn btn-secondary btn-sm">← Voltar</a>
    </div>
  </header>
  <main class="content">
    <div class="detalhe-grid">

      <%-- Informações gerais --%>
      <div class="card">
        <h3>📋 Informações</h3>
        <div class="info-row">
          <span class="info-label">Tipo</span>
          <span class="info-valor">
            <c:choose>
              <c:when test="${pedido.tipo=='mesa'}">🪑 Mesa ${pedido.mesa.numero}</c:when>
              <c:otherwise>🛵 Delivery</c:otherwise>
            </c:choose>
          </span>
        </div>
        <div class="info-row">
          <span class="info-label">Status</span>
          <span class="info-valor">
            <c:choose>
              <c:when test="${pedido.status=='aberto'}"><span class="badge badge-info">Aberto</span></c:when>
              <c:when test="${pedido.status=='em_preparo'}"><span class="badge badge-warning">Em preparo</span></c:when>
              <c:when test="${pedido.status=='pronto'}"><span class="badge badge-success">Pronto</span></c:when>
              <c:when test="${pedido.status=='entregue'}"><span class="badge badge-success">Entregue</span></c:when>
              <c:when test="${pedido.status=='cancelado'}"><span class="badge badge-danger">Cancelado</span></c:when>
              <c:otherwise><span class="badge">${pedido.status}</span></c:otherwise>
            </c:choose>
          </span>
        </div>
        <div class="info-row">
          <span class="info-label">Operador</span>
          <span class="info-valor">${pedido.identificadorOperador}</span>
        </div>
        <div class="info-row">
          <span class="info-label">Abertura</span>
          <span class="info-valor">${pedido.dataAberturaFormatada}</span>
        </div>
        <div class="info-row">
          <span class="info-label">Urgente</span>
          <span class="info-valor">
            <c:choose>
              <c:when test="${pedido.urgente}"><span class="badge badge-urgente">🔴 Sim</span></c:when>
              <c:otherwise>Não</c:otherwise>
            </c:choose>
          </span>
        </div>
        <c:if test="${not empty pedido.observacao}">
          <div class="info-row">
            <span class="info-label">Observação</span>
            <span class="info-valor">${pedido.observacao}</span>
          </div>
        </c:if>
      </div>

      <%-- Itens do pedido --%>
      <div class="card">
        <h3>🍽️ Itens</h3>
        <div class="table-wrapper">
          <table>
            <thead>
              <tr><th>Item</th><th>Qtd</th><th>Unit.</th><th>Subtotal</th></tr>
            </thead>
            <tbody>
              <c:forEach var="item" items="${pedido.itens}">
                <tr>
                  <td>
                    ${item.nomeItem}
                    <c:if test="${not empty item.observacao}">
                      <br><small class="text-muted">${item.observacao}</small>
                    </c:if>
                  </td>
                  <td>${item.quantidade}</td>
                  <td>R$ <fmt:formatNumber value="${item.precoUnitario}"
                         minFractionDigits="2" maxFractionDigits="2"/></td>
                  <td style="font-weight:700;color:#e85d27">
                    R$ <fmt:formatNumber value="${item.subtotal}"
                         minFractionDigits="2" maxFractionDigits="2"/>
                  </td>
                </tr>
              </c:forEach>
            </tbody>
          </table>
        </div>
        <div class="linha-total">
          <span>Total</span>
          <span style="color:#e85d27">
            R$ <fmt:formatNumber value="${pedido.calcularTotal()}"
                 minFractionDigits="2" maxFractionDigits="2"/>
          </span>
        </div>
      </div>

    </div>

    <%-- Ações --%>
    <c:if test="${pedido.status!='entregue' && pedido.status!='cancelado'}">
      <div class="card" style="margin-top:24px">
        <h3>⚡ Ações</h3>
        <div class="d-flex gap-10">

          <form method="POST" action="${pageContext.request.contextPath}/app/pedidos">
            <input type="hidden" name="acao"     value="avancarStatus">
            <input type="hidden" name="id"       value="${pedido.idPedido}">
            <input type="hidden" name="operador" value="<%= _u.getLogin() %>">
            <button type="submit" class="btn btn-primary">
              <c:choose>
                <c:when test="${pedido.status=='aberto'}">▶ Iniciar preparo</c:when>
                <c:when test="${pedido.status=='em_preparo'}">✓ Marcar como pronto</c:when>
                <c:when test="${pedido.status=='pronto'}">🍽️ Marcar como entregue</c:when>
              </c:choose>
            </button>
          </form>

          <form method="POST" action="${pageContext.request.contextPath}/app/pedidos"
                onsubmit="return confirm('Cancelar este pedido?')">
            <input type="hidden" name="acao"     value="cancelar">
            <input type="hidden" name="id"       value="${pedido.idPedido}">
            <input type="hidden" name="operador" value="<%= _u.getLogin() %>">
            <button type="submit" class="btn btn-danger">✕ Cancelar pedido</button>
          </form>

        </div>
      </div>
    </c:if>

  </main>
</div>
</div>
</body></html>
