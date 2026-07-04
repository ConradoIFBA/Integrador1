<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="br.com.restaurante.model.Usuario" %>
<%@ taglib prefix="c"   uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt"  %>
<% Usuario _u=(Usuario)session.getAttribute("usuarioLogado"); %>
<!DOCTYPE html>
<html lang="pt-BR">
<head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Dashboard — Integrador</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/style.css">
</head>
<body>
<div class="main-container">
<%@ include file="/WEB-INF/views/shared/_sidebar.jsp" %>
<div class="main-content">
  <header class="topbar">
    <div class="topbar-left"><h2>Dashboard</h2></div>
    <div class="topbar-right">
      <div class="user-info">
        <div class="user-avatar"><%=_u.getNome().substring(0,1).toUpperCase()%></div>
        <div class="user-details">
          <span class="name"><%=_u.getNome()%></span>
          <span class="role">Gerente</span>
        </div>
      </div>
    </div>
  </header>
  <main class="content">
    <c:if test="${not empty erro}"><div class="alert alert-error">${erro}</div></c:if>

    <div class="cards-grid">
      <div class="stat-card" style="border-left-color:#10b981">
        <div class="stat-label">Mesas Livres</div>
        <div class="stat-value" style="color:#10b981">${mesasLivres}</div>
        <div style="font-size:12px;color:#64748b;margin-top:4px">de ${totalMesas} no total</div>
      </div>
      <div class="stat-card" style="border-left-color:#ef4444">
        <div class="stat-label">Mesas Ocupadas</div>
        <div class="stat-value" style="color:#ef4444">${mesasOcupadas}</div>
      </div>
      <div class="stat-card" style="border-left-color:#f59e0b">
        <div class="stat-label">Pedidos em Aberto</div>
        <div class="stat-value" style="color:#f59e0b">${pedidosAbertos}</div>
      </div>
      <div class="stat-card" style="border-left-color:#e85d27">
        <div class="stat-label">Faturamento Hoje</div>
        <div class="stat-value" style="color:#e85d27;font-size:20px">
          R$ <fmt:formatNumber value="${totalHoje}" minFractionDigits="2" maxFractionDigits="2"/>
        </div>
      </div>
    </div>

    <div class="card mb-20">
      <h3>Ações Rápidas</h3>
      <div class="d-flex gap-10" style="flex-wrap:wrap">
        <a href="${pageContext.request.contextPath}/app/mesas"     class="btn btn-primary">🪑 Ver Mesas</a>
        <a href="${pageContext.request.contextPath}/app/pedidos"   class="btn btn-secondary">🧾 Pedidos</a>
        <a href="${pageContext.request.contextPath}/app/fila"      class="btn btn-secondary">👨‍🍳 Fila de Preparo</a>
        <a href="${pageContext.request.contextPath}/app/cardapio"  class="btn btn-secondary">📋 Cardápio</a>
        <a href="${pageContext.request.contextPath}/app/relatorios" class="btn btn-secondary">📄 Relatórios</a>
      </div>
    </div>

    <div class="card">
      <h3>Pedidos em Andamento</h3>
      <c:choose>
        <c:when test="${empty ultimosPedidos}">
          <div class="empty-state"><div class="icon">🎉</div><p>Nenhum pedido em aberto.</p></div>
        </c:when>
        <c:otherwise>
          <div class="table-wrapper">
            <table>
              <thead><tr><th>#</th><th>Tipo</th><th>Mesa</th><th>Operador</th><th>Status</th><th>Urgente</th><th>Abertura</th></tr></thead>
              <tbody>
                <c:forEach var="p" items="${ultimosPedidos}">
                  <tr>
                    <td><strong>#${p.idPedido}</strong></td>
                    <td><c:choose><c:when test="${p.tipo=='mesa'}">🪑 Mesa</c:when><c:otherwise>🛵 Delivery</c:otherwise></c:choose></td>
                    <td><c:choose><c:when test="${not empty p.mesa}">Mesa ${p.mesa.numero}</c:when><c:otherwise><span class="text-muted">—</span></c:otherwise></c:choose></td>
                    <td>${p.identificadorOperador}</td>
                    <td>
                      <c:choose>
                        <c:when test="${p.status=='aberto'}"><span class="badge badge-info">Aberto</span></c:when>
                        <c:when test="${p.status=='em_preparo'}"><span class="badge badge-warning">Em preparo</span></c:when>
                        <c:when test="${p.status=='pronto'}"><span class="badge badge-success">Pronto</span></c:when>
                        <c:otherwise><span class="badge">${p.status}</span></c:otherwise>
                      </c:choose>
                    </td>
                    <td><c:if test="${p.urgente}"><span class="badge badge-urgente">🔴 Urgente</span></c:if><c:if test="${!p.urgente}"><span class="text-muted">Normal</span></c:if></td>
                    <td class="text-muted">${p.dataAberturaFormatada}</td>
                  </tr>
                </c:forEach>
              </tbody>
            </table>
          </div>
          <c:if test="${pedidosAbertos > 8}">
            <div class="text-center mt-10">
              <a href="${pageContext.request.contextPath}/app/pedidos" class="btn btn-secondary btn-sm">Ver todos os ${pedidosAbertos} pedidos</a>
            </div>
          </c:if>
        </c:otherwise>
      </c:choose>
    </div>
  </main>
</div>
</div>
</body></html>
