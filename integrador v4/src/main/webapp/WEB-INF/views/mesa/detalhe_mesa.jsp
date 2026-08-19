<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="br.com.restaurante.model.Usuario" %>
<%@ taglib prefix="c"   uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt"  %>
<%--
    ================================================================
    DETALHE_MESA.JSP — HISTÓRICO/DETALHE DE UMA MESA (v4)
    ================================================================
 
--%>
<%
    Usuario _u = (Usuario) session.getAttribute("usuarioLogado");
%>
<!DOCTYPE html>
<html lang="pt-BR">
<head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Mesa ${mesa.numero} — Integrador</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/style.css">
</head>
<body>
<div class="main-container">
<%@ include file="/WEB-INF/views/shared/_sidebar.jsp" %>
<div class="main-content">

  <header class="topbar">
    <div class="topbar-left"><h2>Mesa ${mesa.numero}</h2></div>
    <div class="topbar-right">
      <a href="${pageContext.request.contextPath}/app/mesas"
         class="btn btn-secondary btn-sm">← Voltar</a>
    </div>
  </header>

  <main class="content">

    <div class="cards-grid" style="margin-bottom:24px">

      <div class="stat-card">
        <div class="icon-box azul">🪑</div>
        <div class="stat-label">Número</div>
        <div class="stat-value">${mesa.numero}</div>
      </div>

      <div class="stat-card">
        <div class="icon-box roxo">👥</div>
        <div class="stat-label">Capacidade</div>
        <div class="stat-value" style="font-size:20px">${mesa.capacidade} lugares</div>
      </div>

      <div class="stat-card">
        <c:choose>
          <c:when test="${mesa.status=='livre'}"><div class="icon-box verde">●</div></c:when>
          <c:when test="${mesa.status=='ocupada'}"><div class="icon-box vermelho">●</div></c:when>
          <c:otherwise><div class="icon-box ambar">●</div></c:otherwise>
        </c:choose>
        <div class="stat-label">Status atual</div>
        <div class="stat-value" style="font-size:18px">
          <c:choose>
            <c:when test="${mesa.status=='livre'}">Livre</c:when>
            <c:when test="${mesa.status=='ocupada'}">Ocupada</c:when>
            <c:otherwise>Reservada</c:otherwise>
          </c:choose>
        </div>
      </div>

      <c:if test="${not empty mesa.operador}">
        <div class="stat-card">
          <div class="icon-box azul">🕐</div>
          <div class="stat-label">Última ação</div>
          <div style="font-size:13px;font-weight:600;margin-top:6px">
            ${mesa.ultimaAcao}
          </div>
        </div>
      </c:if>

    </div>

    <div class="card">
      <h3>🧾 Pedidos em Andamento</h3>

      <c:choose>
        <c:when test="${empty pedidosAbertos}">
          <div class="empty-state">
            <div class="icon">✅</div>
            <p>Nenhum pedido aberto nesta mesa.</p>
          </div>
        </c:when>
        <c:otherwise>
          <div class="table-wrapper">
            <table>
              <thead>
                <tr>
                  <th>#</th><th>Status</th><th>Operador</th><th>Abertura</th><th>Urgente</th><th></th>
                </tr>
              </thead>
              <tbody>
                <c:forEach var="p" items="${pedidosAbertos}">
                  <tr>
                    <td><strong>#${p.idPedido}</strong></td>
                    <td>
                      <c:choose>
                        <c:when test="${p.status=='aberto'}"><span class="badge badge-info">Recebido</span></c:when>
                        <c:when test="${p.status=='em_preparo'}"><span class="badge badge-warning">Em preparo</span></c:when>
                        <c:when test="${p.status=='pronto'}"><span class="badge badge-success">Pronto</span></c:when>
                        <c:otherwise><span class="badge">${p.status}</span></c:otherwise>
                      </c:choose>
                    </td>
                    <td>${p.identificadorOperador}</td>
                    <td class="text-muted">${p.dataAberturaFormatada}</td>
                    <td><c:if test="${p.urgente}"><span class="badge badge-urgente">Urgente</span></c:if></td>
                    <td>
                      <a href="${pageContext.request.contextPath}/app/pedidos?acao=detalhe&id=${p.idPedido}"
                         class="btn btn-secondary btn-sm">Ver</a>
                    </td>
                  </tr>
                </c:forEach>
              </tbody>
            </table>
          </div>
        </c:otherwise>
      </c:choose>

      <c:if test="${mesa.status=='ocupada'}">
        <div class="mt-20">
          <a href="${pageContext.request.contextPath}/app/pedidos?acao=novo&mesaId=${mesa.idMesa}"
             class="btn btn-primary btn-sm">+ Novo Pedido para esta Mesa</a>
        </div>
      </c:if>
    </div>

  </main>
</div>
</div>
</body>
</html>
