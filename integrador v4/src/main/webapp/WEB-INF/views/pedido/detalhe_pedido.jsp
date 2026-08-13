<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="br.com.restaurante.model.Usuario" %>
<%@ taglib prefix="c"   uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt"  %>
<%--
    ================================================================
    DETALHE_PEDIDO.JSP — DETALHE DE UM PEDIDO (v4 — tema escuro)
    ================================================================
    Duas mudanças em relação à versão anterior:
      1. Cores hard-coded (style="color:#e85d27" etc.) trocadas
         pelas variáveis do tema (var(--success-color) etc.) — no
         fundo escuro, laranja/preto fixo ficava ilegível.
      2. O modal de "Registrar Entrega" tinha sua PRÓPRIA classe
         .modal-pag (CSS duplicado, quase idêntico ao .modal global
         que já existe no style.css desde a v4). Troquei para usar
         .modal/.modal-overlay do style.css compartilhado — menos
         CSS duplicado, e herda qualquer ajuste futuro no tema
         automaticamente. O comportamento do modal (abrir/fechar,
         click fora, Esc) continua o mesmo.
    Nenhuma mudança de lógica/rota.
    ================================================================
--%>
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
@media (max-width:900px){.detalhe-grid{grid-template-columns:1fr}}
.info-row{display:flex;justify-content:space-between;padding:9px 0;border-bottom:1px solid var(--border-subtle);font-size:14px}
.info-row:last-child{border-bottom:none}
.info-label{color:var(--text-secondary);font-weight:600}
.info-valor{font-weight:500}
.linha-total{display:flex;justify-content:space-between;padding:14px 0 0;font-size:18px;font-weight:800;border-top:1px solid var(--border-subtle-2);margin-top:8px}
</style>
</head>
<body>
<div class="main-container">
<%@ include file="/WEB-INF/views/shared/_sidebar.jsp" %>
<div class="main-content">
  <header class="topbar">
    <div class="topbar-left"><h2>Pedido #${pedido.idPedido}</h2></div>
    <div class="topbar-right">
      <a href="${pageContext.request.contextPath}/app/pedidos" class="btn btn-secondary btn-sm">← Voltar</a>
    </div>
  </header>
  <main class="content">
    <div class="detalhe-grid">

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
              <c:when test="${pedido.status=='aberto'}"><span class="badge badge-info">Recebido</span></c:when>
              <c:when test="${pedido.status=='em_preparo'}"><span class="badge badge-warning">Em preparo</span></c:when>
              <c:when test="${pedido.status=='pronto'}"><span class="badge badge-success">Pronto</span></c:when>
              <c:when test="${pedido.status=='entregue'}"><span class="badge badge-success">Entregue ✓</span></c:when>
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
              <c:when test="${pedido.urgente}"><span class="badge badge-urgente">Sim</span></c:when>
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

      <div class="card">
        <h3>🍽️ Itens</h3>
        <div class="table-wrapper">
          <table>
            <thead><tr><th>Item</th><th>Qtd</th><th>Unit.</th><th>Subtotal</th></tr></thead>
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
                  <td>R$ <fmt:formatNumber value="${item.precoUnitario}" minFractionDigits="2" maxFractionDigits="2"/></td>
                  <td style="font-weight:700;color:var(--success-color)">
                    R$ <fmt:formatNumber value="${item.subtotal}" minFractionDigits="2" maxFractionDigits="2"/>
                  </td>
                </tr>
              </c:forEach>
            </tbody>
          </table>
        </div>
        <div class="linha-total">
          <span>Total do Pedido</span>
          <span style="color:var(--success-color)">
            R$ <fmt:formatNumber value="${pedido.calcularTotal()}" minFractionDigits="2" maxFractionDigits="2"/>
          </span>
        </div>
      </div>

    </div>

    <c:if test="${not empty pagamentos || pedido.status == 'entregue'}">
      <div class="card" style="margin-top:24px">
        <h3>💳 Pagamentos</h3>
        <c:choose>
          <c:when test="${empty pagamentos}">
            <p class="text-muted">Nenhum pagamento registrado ainda.</p>
          </c:when>
          <c:otherwise>
            <div class="table-wrapper">
              <table>
                <thead><tr><th>Forma</th><th>Valor</th><th>Operador</th><th>Horário</th></tr></thead>
                <tbody>
                  <c:forEach var="pag" items="${pagamentos}">
                    <tr>
                      <td>
                        <c:choose>
                          <c:when test="${pag.formaPagamento=='dinheiro'}">💵 Dinheiro</c:when>
                          <c:when test="${pag.formaPagamento=='cartao'}">💳 Cartão</c:when>
                          <c:when test="${pag.formaPagamento=='pix'}">📱 PIX</c:when>
                          <c:otherwise>${pag.formaPagamento}</c:otherwise>
                        </c:choose>
                      </td>
                      <td style="font-weight:700;color:var(--success-color)">
                        R$ <fmt:formatNumber value="${pag.valor}" minFractionDigits="2" maxFractionDigits="2"/>
                      </td>
                      <td>${pag.identificadorOperador}</td>
                      <td class="text-muted">${pag.dataPagamentoFormatada}</td>
                    </tr>
                  </c:forEach>
                </tbody>
              </table>
            </div>
            <div class="linha-total">
              <span>Total Pago</span>
              <span style="color:var(--success-color)">
                R$ <fmt:formatNumber value="${totalPago}" minFractionDigits="2" maxFractionDigits="2"/>
              </span>
            </div>
          </c:otherwise>
        </c:choose>
      </div>
    </c:if>

    <c:if test="${pedido.status!='entregue' && pedido.status!='cancelado'}">
      <div class="card" style="margin-top:24px">
        <h3>⚡ Ações</h3>
        <div class="d-flex gap-10">

          <c:choose>
            <c:when test="${pedido.status == 'pronto'}">
              <button type="button" class="btn btn-primary" onclick="abrirModalPagamento()">
                🍽️ Registrar Entrega + Pagamento
              </button>
            </c:when>
            <c:otherwise>
              <form method="POST" action="${pageContext.request.contextPath}/app/pedidos">
                <input type="hidden" name="acao"     value="avancarStatus">
                <input type="hidden" name="id"       value="${pedido.idPedido}">
                <input type="hidden" name="operador" value="<%= _u.getLogin() %>">
                <button type="submit" class="btn btn-primary">
                  <c:choose>
                    <c:when test="${pedido.status=='aberto'}">▶ Iniciar preparo</c:when>
                    <c:when test="${pedido.status=='em_preparo'}">✓ Marcar como pronto</c:when>
                  </c:choose>
                </button>
              </form>
            </c:otherwise>
          </c:choose>

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

<%-- Modal de entrega + pagamento — usa .modal/.modal-overlay
     globais (style.css), em vez da classe .modal-pag própria que
     existia antes. --%>
<div class="modal-overlay" id="modalPagamento">
  <div class="modal">
    <h3>🍽️ Registrar Entrega</h3>
    <form method="POST" action="${pageContext.request.contextPath}/app/pedidos">
      <input type="hidden" name="acao"     value="avancarStatus">
      <input type="hidden" name="id"       value="${pedido.idPedido}">

      <div class="form-group">
        <label>Seu identificador</label>
        <input type="text" name="operador" placeholder="Ex: A1" maxlength="20" required>
      </div>

      <div class="form-group">
        <label>Forma de pagamento</label>
        <select name="formaPagamento" required>
          <option value="">Selecione...</option>
          <option value="dinheiro">💵 Dinheiro</option>
          <option value="cartao">💳 Cartão</option>
          <option value="pix">📱 PIX</option>
        </select>
      </div>

      <div class="form-group">
        <label>Valor pago (R$) — deixe em branco para usar o total do pedido</label>
        <input type="number" name="valorPagamento" placeholder="0,00"
               step="0.01" min="0">
      </div>

      <div class="modal-acoes">
        <button type="button" class="btn btn-secondary"
                onclick="fecharModalPagamento()">Cancelar</button>
        <button type="submit" class="btn btn-primary">Confirmar entrega</button>
      </div>
    </form>
  </div>
</div>

<script>
function abrirModalPagamento() {
  document.getElementById('modalPagamento').classList.add('aberto');
}
function fecharModalPagamento() {
  document.getElementById('modalPagamento').classList.remove('aberto');
}
document.getElementById('modalPagamento').addEventListener('click', function(e) {
  if (e.target === this) fecharModalPagamento();
});
document.addEventListener('keydown', function(e) {
  if (e.key === 'Escape') fecharModalPagamento();
});
</script>
</body>
</html>
