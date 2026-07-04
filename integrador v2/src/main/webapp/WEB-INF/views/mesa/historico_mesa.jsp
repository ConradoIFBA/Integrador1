<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="br.com.restaurante.model.Usuario" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<% Usuario _u=(Usuario)session.getAttribute("usuarioLogado"); %>
<!DOCTYPE html>
<html lang="pt-BR">
<head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Histórico Mesa ${mesa.numero} — Integrador</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/style.css">
<style>
.timeline{position:relative;padding-left:28px}
.timeline::before{content:'';position:absolute;left:8px;top:0;bottom:0;width:2px;background:#e2e8f0}
.timeline-item{position:relative;margin-bottom:20px}
.timeline-item::before{content:'';position:absolute;left:-24px;top:5px;width:12px;height:12px;border-radius:50%;background:#e85d27;border:2px solid #fff;box-shadow:0 0 0 2px #e85d27}
.timeline-hora{font-size:11px;color:#94a3b8;margin-bottom:3px;font-weight:600;text-transform:uppercase;letter-spacing:.5px}
.timeline-desc{font-size:14px;color:#1e293b;background:#f8fafc;padding:10px 14px;border-radius:8px;border-left:3px solid #e85d27}
</style>
</head>
<body>
<div class="main-container">
<%@ include file="/WEB-INF/views/shared/_sidebar.jsp" %>
<div class="main-content">
  <header class="topbar">
    <div class="topbar-left"><h2>Mesa ${mesa.numero} — Histórico</h2></div>
    <div class="topbar-right"><a href="${pageContext.request.contextPath}/app/mesas" class="btn btn-secondary btn-sm">← Voltar</a></div>
  </header>
  <main class="content">
    <div class="cards-grid" style="margin-bottom:24px">
      <div class="stat-card"><div class="stat-label">Número</div><div class="stat-value">${mesa.numero}</div></div>
      <div class="stat-card"><div class="stat-label">Capacidade</div><div class="stat-value">${mesa.capacidade} lugares</div></div>
      <div class="stat-card">
        <div class="stat-label">Status</div>
        <div class="stat-value" style="font-size:18px;text-transform:capitalize">
          <c:choose>
            <c:when test="${mesa.status=='livre'}"><span style="color:#10b981">🟢 Livre</span></c:when>
            <c:when test="${mesa.status=='ocupada'}"><span style="color:#ef4444">🔴 Ocupada</span></c:when>
            <c:otherwise><span style="color:#f59e0b">🟡 Reservada</span></c:otherwise>
          </c:choose>
        </div>
      </div>
    </div>
    <div style="display:grid;grid-template-columns:1fr 1fr;gap:24px">
      <div class="card">
        <h3>📋 Linha do Tempo</h3>
        <c:choose>
          <c:when test="${empty historico}">
            <div class="empty-state"><div class="icon">📋</div><p>Nenhum evento registrado.</p></div>
          </c:when>
          <c:otherwise>
            <div class="timeline">
              <c:forEach var="h" items="${historico}">
                <div class="timeline-item">
                  <div class="timeline-hora">${h.dataHoraFormatada}</div>
                  <div class="timeline-desc">${h.descricao}</div>
                </div>
              </c:forEach>
            </div>
          </c:otherwise>
        </c:choose>
      </div>
      <div class="card">
        <h3>🧾 Pedidos em Andamento</h3>
        <c:choose>
          <c:when test="${empty pedidosAbertos}">
            <div class="empty-state"><div class="icon">✅</div><p>Nenhum pedido aberto.</p></div>
          </c:when>
          <c:otherwise>
            <div class="table-wrapper">
              <table>
                <thead><tr><th>#</th><th>Status</th><th>Operador</th><th>Abertura</th></tr></thead>
                <tbody>
                  <c:forEach var="p" items="${pedidosAbertos}">
                    <tr>
                      <td><strong>#${p.idPedido}</strong></td>
                      <td>
                        <c:choose>
                          <c:when test="${p.status=='aberto'}"><span class="badge badge-info">Aberto</span></c:when>
                          <c:when test="${p.status=='em_preparo'}"><span class="badge badge-warning">Em preparo</span></c:when>
                          <c:when test="${p.status=='pronto'}"><span class="badge badge-success">Pronto</span></c:when>
                          <c:otherwise><span class="badge">${p.status}</span></c:otherwise>
                        </c:choose>
                      </td>
                      <td>${p.identificadorOperador}</td>
                      <td class="text-muted">${p.dataAberturaHora}</td>
                    </tr>
                  </c:forEach>
                </tbody>
              </table>
            </div>
          </c:otherwise>
        </c:choose>
        <c:if test="${mesa.status=='ocupada'}">
          <div class="mt-20">
            <a href="${pageContext.request.contextPath}/app/pedidos?acao=novo&mesaId=${mesa.idMesa}" class="btn btn-primary btn-sm">+ Novo Pedido</a>
          </div>
        </c:if>
      </div>
    </div>
  </main>
</div>
</div>
</body></html>
