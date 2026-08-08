<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="br.com.restaurante.model.Usuario" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
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
.mesa-card{border-radius:12px;padding:22px 18px;text-align:center;
  border:2px solid transparent;transition:transform .2s,box-shadow .2s}
.mesa-card:hover{transform:translateY(-2px);box-shadow:0 6px 20px rgba(0,0,0,.12)}
.mesa-livre    {background:#d1fae5;border-color:#10b981}
.mesa-ocupada  {background:#fee2e2;border-color:#ef4444;opacity:.6;pointer-events:none}
.mesa-reservada{background:#fef3c7;border-color:#f59e0b;opacity:.6;pointer-events:none}
.mesa-num{font-size:38px;font-weight:800;margin-bottom:6px}
.mesa-livre .mesa-num    {color:#065f46}
.mesa-ocupada .mesa-num  {color:#991b1b}
.mesa-reservada .mesa-num{color:#92400e}
.mesa-cap{font-size:13px;color:#64748b;margin-bottom:12px}
.mesa-badge{display:inline-block;padding:3px 12px;border-radius:20px;
  font-size:12px;font-weight:700;text-transform:uppercase;margin-bottom:14px}
.mesa-livre .mesa-badge    {background:#10b981;color:#fff}
.mesa-ocupada .mesa-badge  {background:#ef4444;color:#fff}
.mesa-reservada .mesa-badge{background:#f59e0b;color:#fff}
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

    <%-- Legenda --%>
    <div style="display:flex;gap:20px;margin-bottom:20px;font-size:13px;color:#64748b">
      <span>🟢 Livre — clique para reservar</span>
      <span>🔴 Ocupada — indisponível</span>
      <span>🟡 Reservada — indisponível</span>
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
            <div class="mesa-card mesa-${mesa.status}">
              <div class="mesa-num">${mesa.numero}</div>
              <div class="mesa-cap">👥 ${mesa.capacidade} lugares</div>
              <div class="mesa-badge">${mesa.status}</div>

              <c:if test="${mesa.status == 'livre'}">
                <form method="POST"
                      action="${pageContext.request.contextPath}/app/cliente/reserva"
                      onsubmit="return confirm('Reservar a Mesa ${mesa.numero}?')">
                  <input type="hidden" name="id" value="${mesa.idMesa}">
                  <button type="submit" class="btn btn-success btn-sm"
                          style="width:100%">
                    📅 Reservar esta mesa
                  </button>
                </form>
              </c:if>

              <c:if test="${mesa.status != 'livre'}">
                <div style="font-size:12px;color:#64748b;margin-top:4px">
                  <c:choose>
                    <c:when test="${mesa.status=='ocupada'}">Em uso no momento</c:when>
                    <c:otherwise>Já reservada</c:otherwise>
                  </c:choose>
                  <c:if test="${not empty mesa.operador}">
                    <br>por ${mesa.operador}
                  </c:if>
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
