<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="br.com.restaurante.model.Usuario" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%--
    ================================================================
    RESERVA.JSP — CLIENTE RESERVA UMA MESA (v4 — tema escuro)
    ================================================================
    Mesma linguagem visual dos cards de mesas.jsp (bolinha de status,
    badge colorido), simplificada para o que o cliente pode fazer
    aqui: só reservar uma mesa LIVRE — mesas ocupadas/reservadas
    aparecem "desabilitadas" (sem clique), igual à versão anterior.
    Nenhuma mudança de lógica/rota — só visual.
    ================================================================
--%>
<%
    Usuario _u = (Usuario) session.getAttribute("usuarioLogado");
%>
<!DOCTYPE html>
<html lang="pt-BR">
<head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Reservar Mesa — Integrador</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/style.css">
<style>
.mesas-grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(200px,1fr));gap:18px}
.mesa-card{
  background:var(--bg-card);border:1px solid var(--border-subtle);border-radius:var(--radius);
  padding:22px 18px;text-align:center;position:relative;transition:var(--transition)
}
.mesa-card.livre:hover{border-color:var(--success-color)}
.mesa-card.ocupada,.mesa-card.reservada{opacity:.55}
.mesa-card .status-dot{position:absolute;top:16px;right:16px;width:10px;height:10px;border-radius:50%}
.mesa-card.livre .status-dot{background:var(--success-color)}
.mesa-card.ocupada .status-dot{background:var(--error-color)}
.mesa-card.reservada .status-dot{background:var(--warning-color)}
.mesa-num{font-size:32px;font-weight:800;margin-bottom:6px}
.mesa-cap{font-size:13px;color:var(--text-secondary);margin-bottom:12px}
.legenda{display:flex;gap:20px;margin-bottom:20px;font-size:13px;color:var(--text-secondary);flex-wrap:wrap}
.legenda .dot{width:8px;height:8px;border-radius:50%;display:inline-block;margin-right:6px}
</style>
</head>
<body>
<div class="main-container">
<%@ include file="/WEB-INF/views/shared/_sidebar.jsp" %>
<div class="main-content">

  <header class="topbar">
    <div class="topbar-left"><h2>📅 Reservar Mesa</h2></div>
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

    <c:if test="${not empty msgSucesso}">
      <div class="alert alert-success">✓ ${msgSucesso}</div>
    </c:if>
    <c:if test="${not empty msgErro}">
      <div class="alert alert-error">✕ ${msgErro}</div>
    </c:if>

    <div class="legenda">
      <span><span class="dot" style="background:var(--success-color)"></span>Livre — clique para reservar</span>
      <span><span class="dot" style="background:var(--error-color)"></span>Ocupada — indisponível</span>
      <span><span class="dot" style="background:var(--warning-color)"></span>Reservada — indisponível</span>
    </div>

    <c:choose>
      <c:when test="${empty mesas}">
        <div class="empty-state card">
          <div class="icon">🪑</div>
          <p>Nenhuma mesa cadastrada.</p>
        </div>
      </c:when>
      <c:otherwise>
        <div class="mesas-grid">
          <c:forEach var="mesa" items="${mesas}">
            <div class="mesa-card ${mesa.status}">
              <span class="status-dot"></span>
              <div class="mesa-num">#${mesa.numero}</div>
              <div class="mesa-cap">👥 ${mesa.capacidade} lugares</div>

              <c:choose>
                <c:when test="${mesa.status=='livre'}"><span class="badge badge-success">Livre</span></c:when>
                <c:when test="${mesa.status=='ocupada'}"><span class="badge badge-danger">Ocupada</span></c:when>
                <c:otherwise><span class="badge badge-warning">Reservada</span></c:otherwise>
              </c:choose>

              <c:if test="${mesa.status == 'livre'}">
                <form method="POST"
                      action="${pageContext.request.contextPath}/app/cliente/reserva"
                      onsubmit="return confirm('Reservar a Mesa ${mesa.numero}?')"
                      style="margin-top:14px">
                  <input type="hidden" name="id" value="${mesa.idMesa}">
                  <button type="submit" class="btn btn-primary btn-sm" style="width:100%">
                    Reservar esta mesa
                  </button>
                </form>
              </c:if>

              <c:if test="${mesa.status != 'livre'}">
                <div style="font-size:12px;color:var(--text-muted);margin-top:10px">
                  <c:choose>
                    <c:when test="${mesa.status=='ocupada'}">Em uso no momento</c:when>
                    <c:otherwise>Já reservada</c:otherwise>
                  </c:choose>
                  <c:if test="${not empty mesa.operador}"><br>por ${mesa.operador}</c:if>
                </div>
              </c:if>
            </div>
          </c:forEach>
        </div>
      </c:otherwise>
    </c:choose>

  </main>
</div>
</div>
</body>
</html>
