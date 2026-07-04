<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="br.com.restaurante.model.Usuario" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<% Usuario _u=(Usuario)session.getAttribute("usuarioLogado"); %>
<!DOCTYPE html>
<html lang="pt-BR">
<head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Mesas — Integrador</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/style.css">
<style>
.mesas-grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(200px,1fr));gap:18px;margin-top:20px}
.mesa-card{border-radius:12px;padding:22px 18px;text-align:center;transition:transform .2s,box-shadow .2s}
.mesa-card:hover{transform:translateY(-3px);box-shadow:0 8px 24px rgba(0,0,0,.15)}
.mesa-livre{background:#d1fae5;border:2px solid #10b981}
.mesa-ocupada{background:#fee2e2;border:2px solid #ef4444}
.mesa-reservada{background:#fef3c7;border:2px solid #f59e0b}
.mesa-numero{font-size:36px;font-weight:800;line-height:1;margin-bottom:6px}
.mesa-livre .mesa-numero{color:#065f46}
.mesa-ocupada .mesa-numero{color:#991b1b}
.mesa-reservada .mesa-numero{color:#92400e}
.mesa-capacidade{font-size:13px;color:#64748b;margin-bottom:14px}
.mesa-status-badge{display:inline-block;padding:3px 12px;border-radius:20px;font-size:12px;font-weight:700;text-transform:uppercase;letter-spacing:.5px;margin-bottom:14px}
.mesa-livre .mesa-status-badge{background:#10b981;color:#fff}
.mesa-ocupada .mesa-status-badge{background:#ef4444;color:#fff}
.mesa-reservada .mesa-status-badge{background:#f59e0b;color:#fff}
.mesa-acoes{display:flex;flex-direction:column;gap:6px}
.mesa-acoes .btn{width:100%;padding:7px;font-size:13px}
.modal-overlay{display:none;position:fixed;inset:0;background:rgba(0,0,0,.5);z-index:2000;align-items:center;justify-content:center}
.modal-overlay.aberto{display:flex}
.modal{background:#fff;border-radius:12px;padding:32px;width:100%;max-width:360px;box-shadow:0 20px 60px rgba(0,0,0,.3)}
.modal h3{font-size:18px;margin-bottom:18px;color:#1e293b}
.modal .form-group{margin-bottom:16px}
.modal .form-group label{display:block;font-size:13px;font-weight:600;color:#1e293b;margin-bottom:6px}
.modal .form-group input{width:100%;padding:10px 12px;border:2px solid #e2e8f0;border-radius:7px;font-size:14px;font-family:inherit;color:#1e293b}
.modal .form-group input:focus{outline:none;border-color:#e85d27;box-shadow:0 0 0 3px rgba(232,93,39,.12)}
.modal-acoes{display:flex;gap:10px;margin-top:8px}
.modal-acoes .btn{flex:1}
</style>
</head>
<body>
<div class="main-container">
<%@ include file="/WEB-INF/views/shared/_sidebar.jsp" %>
<div class="main-content">
  <header class="topbar">
    <div class="topbar-left"><h2>Mesas</h2></div>
    <div class="topbar-right">
      <div class="user-info">
        <div class="user-avatar"><%=_u.getNome().substring(0,1).toUpperCase()%></div>
        <div class="user-details">
          <span class="name"><%=_u.getNome()%></span>
          <span class="role"><%="GERENTE".equals(_u.getPerfil())?"Gerente":"Atendente"%></span>
        </div>
      </div>
    </div>
  </header>
  <main class="content">
    <div class="cards-grid">
      <div class="stat-card" style="border-left-color:#10b981"><div class="stat-label">Livres</div><div class="stat-value" style="color:#10b981">${livres}</div></div>
      <div class="stat-card" style="border-left-color:#ef4444"><div class="stat-label">Ocupadas</div><div class="stat-value" style="color:#ef4444">${ocupadas}</div></div>
      <div class="stat-card" style="border-left-color:#f59e0b"><div class="stat-label">Reservadas</div><div class="stat-value" style="color:#f59e0b">${reservadas}</div></div>
    </div>
    <div style="display:flex;gap:20px;margin-bottom:16px;font-size:13px;color:#64748b">
      <span>🟢 Livre</span><span>🔴 Ocupada</span><span>🟡 Reservada</span>
    </div>
    <c:choose>
      <c:when test="${empty mesas}">
        <div class="empty-state"><div class="icon">🪑</div><p>Nenhuma mesa cadastrada.</p></div>
      </c:when>
      <c:otherwise>
        <div class="mesas-grid">
          <c:forEach var="mesa" items="${mesas}">
            <div class="mesa-card mesa-${mesa.status}">
              <div class="mesa-numero">${mesa.numero}</div>
              <div class="mesa-capacidade">👥 ${mesa.capacidade} lugares</div>
              <div class="mesa-status-badge">${mesa.status}</div>
              <div class="mesa-acoes">
                <c:if test="${mesa.status != 'ocupada'}">
                  <button class="btn btn-success btn-sm" onclick="abrirModal('abrirMesa',${mesa.idMesa})">✅ Abrir Mesa</button>
                </c:if>
                <c:if test="${mesa.status != 'livre'}">
                  <button class="btn btn-danger btn-sm" onclick="abrirModal('fecharMesa',${mesa.idMesa})">🔒 Fechar Mesa</button>
                </c:if>
                <c:if test="${mesa.status == 'livre'}">
                  <button class="btn btn-warning btn-sm" onclick="abrirModal('reservar',${mesa.idMesa})">📅 Reservar</button>
                </c:if>
                <c:if test="${mesa.status == 'ocupada'}">
                  <a href="${pageContext.request.contextPath}/app/pedidos?acao=novo&mesaId=${mesa.idMesa}" class="btn btn-primary btn-sm">🧾 Novo Pedido</a>
                </c:if>
                <a href="${pageContext.request.contextPath}/app/mesas?acao=historico&id=${mesa.idMesa}" class="btn btn-secondary btn-sm">📋 Histórico</a>
              </div>
            </div>
          </c:forEach>
        </div>
      </c:otherwise>
    </c:choose>
  </main>
</div>
</div>
<div class="modal-overlay" id="modalOverlay">
  <div class="modal">
    <h3 id="modalTitulo">Identificação</h3>
    <form method="POST" action="${pageContext.request.contextPath}/app/mesas" id="formModal">
      <input type="hidden" name="acao" id="modalAcao">
      <input type="hidden" name="id" id="modalId">
      <div class="form-group">
        <label for="operador">Seu identificador (ex: A1, A2)</label>
        <input type="text" id="operador" name="operador" placeholder="Ex: A1" maxlength="20" required autocomplete="off">
      </div>
      <div class="modal-acoes">
        <button type="button" class="btn btn-secondary" onclick="fecharModal()">Cancelar</button>
        <button type="submit" class="btn btn-primary">Confirmar</button>
      </div>
    </form>
  </div>
</div>
<script>
const titulos={abrirMesa:'✅ Abrir Mesa',fecharMesa:'🔒 Fechar Mesa',reservar:'📅 Reservar Mesa'};
function abrirModal(acao,id){
  document.getElementById('modalAcao').value=acao;
  document.getElementById('modalId').value=id;
  document.getElementById('modalTitulo').textContent=titulos[acao]||'Confirmar';
  document.getElementById('operador').value='';
  document.getElementById('modalOverlay').classList.add('aberto');
  document.getElementById('operador').focus();
}
function fecharModal(){document.getElementById('modalOverlay').classList.remove('aberto');}
document.getElementById('modalOverlay').addEventListener('click',function(e){if(e.target===this)fecharModal();});
document.addEventListener('keydown',function(e){if(e.key==='Escape')fecharModal();});
</script>
</body></html>
