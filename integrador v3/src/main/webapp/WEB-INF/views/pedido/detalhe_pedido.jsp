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
.info-row{display:flex;justify-content:space-between;padding:8px 0;border-bottom:1px solid #f1f5f9;font-size:14px}
.info-row:last-child{border-bottom:none}
.info-label{color:#64748b;font-weight:600}
.info-valor{color:#1e293b;font-weight:500}
.linha-total{display:flex;justify-content:space-between;padding:14px 0 0;font-size:18px;font-weight:800;border-top:2px solid #e2e8f0;margin-top:8px}
/* Modal pagamento */
.modal-overlay{display:none;position:fixed;inset:0;background:rgba(0,0,0,.5);z-index:2000;align-items:center;justify-content:center}
.modal-overlay.aberto{display:flex}
.modal-pag{background:#fff;border-radius:12px;padding:28px;width:100%;max-width:380px;box-shadow:0 20px 60px rgba(0,0,0,.3)}
.modal-pag h3{font-size:18px;margin-bottom:18px;color:#1e293b}
.modal-pag .form-group{margin-bottom:14px}
.modal-pag label{display:block;font-size:13px;font-weight:600;color:#1e293b;margin-bottom:6px}
.modal-pag input,.modal-pag select{width:100%;padding:10px 12px;border:2px solid #e2e8f0;border-radius:7px;font-size:14px;font-family:inherit}
.modal-pag input:focus,.modal-pag select:focus{outline:none;border-color:#e85d27}
.modal-acoes{display:flex;gap:10px;margin-top:16px}
.modal-acoes .btn{flex:1}
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
                  <td style="font-weight:700;color:#e85d27">
                    R$ <fmt:formatNumber value="${item.subtotal}" minFractionDigits="2" maxFractionDigits="2"/>
                  </td>
                </tr>
              </c:forEach>
            </tbody>
          </table>
        </div>
        <div class="linha-total">
          <span>Total do Pedido</span>
          <span style="color:#e85d27">
            R$ <fmt:formatNumber value="${pedido.calcularTotal()}" minFractionDigits="2" maxFractionDigits="2"/>
          </span>
        </div>
      </div>

    </div>

    <%-- Seção de Pagamentos (v2) — exibida sempre que houver registros ou pedido entregue --%>
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
                      <td style="font-weight:700;color:#10b981">
                        R$ <fmt:formatNumber value="${pag.valor}" minFractionDigits="2" maxFractionDigits="2"/>
                      </td>
                      <td>${pag.identificadorOperador}</td>
                      <td class="text-muted">${pag.dataPagamentoFormatada}</td>
                    </tr>
                  </c:forEach>
                </tbody>
              </table>
            </div>
            <div style="display:flex;justify-content:space-between;padding-top:12px;font-size:16px;font-weight:800;border-top:2px solid #e2e8f0;margin-top:8px">
              <span>Total Pago</span>
              <span style="color:#10b981">
                R$ <fmt:formatNumber value="${totalPago}" minFractionDigits="2" maxFractionDigits="2"/>
              </span>
            </div>
          </c:otherwise>
        </c:choose>
      </div>
    </c:if>

    <%-- Ações --%>
    <c:if test="${pedido.status!='entregue' && pedido.status!='cancelado'}">
      <div class="card" style="margin-top:24px">
        <h3>⚡ Ações</h3>
        <div class="d-flex gap-10">

          <%-- Avançar: se for "pronto→entregue", abre modal de pagamento --%>
          <c:choose>
            <c:when test="${pedido.status == 'pronto'}">
              <%-- Entrega requer forma de pagamento --%>
              <button type="button" class="btn btn-primary"
                      onclick="abrirModalPagamento()">
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

<%-- Modal de entrega + pagamento --%>
<div class="modal-overlay" id="modalPagamento">
  <div class="modal-pag">
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
