<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="br.com.restaurante.model.Usuario" %>
<%@ page import="br.com.restaurante.model.Mesa" %>
<%--
    ================================================================
    FORM_MESA.JSP — CRIAR/EDITAR MESA (só GERENTE)
    ================================================================
    Formulário simples: número e capacidade. Status NÃO aparece aqui
    de propósito — mudar o status de uma mesa é uma operação
    diferente (abrir/fechar/reservar, com auditoria de operador/data),
    já coberta pelos botões da tela de Mesas. Misturar os dois fluxos
    no mesmo formulário confundiria "configurar a mesa" com "mudar o
    atendimento em andamento".
    ================================================================
--%>
<%
    Usuario _u   = (Usuario) session.getAttribute("usuarioLogado");
    Mesa    _mesa = (Mesa) request.getAttribute("mesa");
    boolean _editando = _mesa != null;
%>
<!DOCTYPE html>
<html lang="pt-BR">
<head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title><%= _editando ? "Editar Mesa" : "Nova Mesa" %> — Integrador</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/style.css">
<style>
.form-card{max-width:420px}
.form-footer{display:flex;gap:12px;margin-top:8px}
.form-footer .btn{flex:1;padding:12px;font-size:15px}
</style>
</head>
<body>
<div class="main-container">
<%@ include file="/WEB-INF/views/shared/_sidebar.jsp" %>
<div class="main-content">
  <header class="topbar">
    <div class="topbar-left"><h2><%= _editando ? "Editar Mesa" : "Nova Mesa" %></h2></div>
    <div class="topbar-right">
      <a href="${pageContext.request.contextPath}/app/mesas" class="btn btn-secondary btn-sm">← Voltar</a>
    </div>
  </header>
  <main class="content">
    <div class="card form-card">
      <form method="POST" action="${pageContext.request.contextPath}/app/mesas" onsubmit="return validar()">
        <input type="hidden" name="acao" value="salvar">
        <% if (_editando) { %>
          <input type="hidden" name="id" value="<%= _mesa.getIdMesa() %>">
        <% } %>

        <div class="form-group">
          <label for="numero">Número da mesa <span style="color:var(--error-color)">*</span></label>
          <input type="number" id="numero" name="numero" placeholder="Ex: 11" min="1" required
                 value="<%= _editando ? String.valueOf(_mesa.getNumero()) : "" %>">
        </div>

        <div class="form-group">
          <label for="capacidade">Capacidade (lugares) <span style="color:var(--error-color)">*</span></label>
          <input type="number" id="capacidade" name="capacidade" placeholder="Ex: 4" min="1" required
                 value="<%= _editando ? String.valueOf(_mesa.getCapacidade()) : "" %>">
        </div>

        <% if (_editando) { %>
          <div style="font-size:12px;color:var(--text-muted);margin-bottom:16px">
            Status atual: <strong><%= _mesa.getStatus() %></strong> — não editável aqui.
            Use os botões da tela de Mesas para abrir/fechar/reservar.
          </div>
        <% } %>

        <div class="form-footer">
          <a href="${pageContext.request.contextPath}/app/mesas" class="btn btn-secondary">Cancelar</a>
          <button type="submit" class="btn btn-primary"><%= _editando ? "💾 Salvar" : "➕ Criar mesa" %></button>
        </div>
      </form>
    </div>
  </main>
</div>
</div>
<script>
function validar() {
  var numero = parseInt(document.getElementById('numero').value);
  var cap = parseInt(document.getElementById('capacidade').value);
  if (isNaN(numero) || numero <= 0) { alert('Informe um número de mesa válido.'); return false; }
  if (isNaN(cap) || cap <= 0) { alert('Informe uma capacidade válida.'); return false; }
  return true;
}
</script>
</body>
</html>
