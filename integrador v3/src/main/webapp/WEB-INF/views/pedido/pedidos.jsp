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
<title>Pedidos — Integrador</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/style.css">
<style>
.pedido-card{background:#fff;border-radius:10px;box-shadow:0 2px 8px rgba(0,0,0,.08);padding:20px;margin-bottom:16px;border-left:4px solid #e2e8f0;transition:box-shadow .2s}
.pedido-card:hover{box-shadow:0 4px 16px rgba(0,0,0,.12)}
.pedido-card.status-aberto    {border-left-color:#3b82f6}
.pedido-card.status-em_preparo{border-left-color:#f59e0b}
.pedido-card.status-pronto    {border-left-color:#10b981}
.pedido-header{display:flex;align-items:center;justify-content:space-between;margin-bottom:12px}
.pedido-id{font-size:18px;font-weight:800;color:#1e293b}
.pedido-meta{font-size:13px;color:#64748b;display:flex;gap:16px;flex-wrap:wrap;margin-bottom:12px}
.pedido-itens{font-size:13px;color:#475569;margin-bottom:14px;background:#f8fafc;padding:10px 14px;border-radius:7px}
.pedido-total{font-size:16px;font-weight:700;color:#e85d27}
.pedido-acoes{display:flex;gap:8px;flex-wrap:wrap}
.pedido-acoes .btn{font-size:13px;padding:7px 14px}
.topo{display:flex;justify-content:space-between;align-items:center;margin-bottom:20px}
.tabs{display:flex;gap:4px;margin-bottom:20px}
.tab-btn{padding:8px 20px;border:2px solid #e2e8f0;border-radius:7px;background:#fff;color:#64748b;font-size:13px;font-weight:600;cursor:pointer;transition:all .2s}
.tab-btn.ativo{background:#e85d27;border-color:#e85d27;color:#fff}
/* Modal operador */
.modal-overlay{display:none;position:fixed;inset:0;background:rgba(0,0,0,.5);z-index:2000;align-items:center;justify-content:center}
.modal-overlay.aberto{display:flex}
.modal{background:#fff;border-radius:12px;padding:28px;width:100%;max-width:380px;box-shadow:0 20px 60px rgba(0,0,0,.3)}
.modal h3{font-size:17px;margin-bottom:16px;color:#1e293b}
.modal .form-group{margin-bottom:12px}
.modal .form-group label{display:block;font-size:13px;font-weight:600;color:#1e293b;margin-bottom:5px}
.modal .form-group input,.modal .form-group select{width:100%;padding:10px 12px;border:2px solid #e2e8f0;border-radius:7px;font-size:14px;font-family:inherit}
.modal .form-group input:focus,.modal .form-group select:focus{outline:none;border-color:#e85d27}
.modal-acoes{display:flex;gap:10px;margin-top:14px}
.modal-acoes .btn{flex:1}
/* campos pagamento — só aparecem quando status=pronto */
.campos-pagamento{display:none}
</style>
</head>
<body>
<div class="main-container">
<%@ include file="/WEB-INF/views/shared/_sidebar.jsp" %>
<div class="main-content">
  <header class="topbar">
    <div class="topbar-left"><h2>Pedidos</h2></div>
    <div class="topbar-right">
      <div class="user-info">
        <div class="user-avatar"><%= _u.getNome().substring(0,1).toUpperCase() %></div>
        <div class="user-details">
          <span class="name"><%= _u.getNome() %></span>
          <span class="role"><%= _u.getPerfil() %></span>
        </div>
      </div>
    </div>
  </header>
  <main class="content">

    <div class="topo">
      <div class="tabs">
        <button class="tab-btn ativo" onclick="filtrarStatus('todos',this)">Todos</button>
        <button class="tab-btn" onclick="filtrarStatus('aberto',this)">Abertos</button>
        <button class="tab-btn" onclick="filtrarStatus('em_preparo',this)">Em preparo</button>
        <button class="tab-btn" onclick="filtrarStatus('pronto',this)">Prontos</button>
      </div>
      <a href="${pageContext.request.contextPath}/app/pedidos?acao=novo"
         class="btn btn-primary">+ Novo Pedido</a>
    </div>

    <c:if test="${not empty msgSucesso}">
      <div class="alert alert-success">✓ ${msgSucesso}</div>
    </c:if>

    <c:choose>
      <c:when test="${empty pedidos}">
        <div class="empty-state card">
          <div class="icon">🧾</div>
          <p>Nenhum pedido em aberto no momento.</p>
          <a href="${pageContext.request.contextPath}/app/pedidos?acao=novo"
             class="btn btn-primary" style="margin-top:16px">Criar primeiro pedido</a>
        </div>
      </c:when>
      <c:otherwise>
        <div id="listaPedidos">
          <c:forEach var="p" items="${pedidos}">
            <div class="pedido-card status-${p.status}" data-status="${p.status}">

              <div class="pedido-header">
                <span class="pedido-id">#${p.idPedido}</span>
                <c:choose>
                  <c:when test="${p.status=='aberto'}"><span class="badge badge-info">Aberto</span></c:when>
                  <c:when test="${p.status=='em_preparo'}"><span class="badge badge-warning">Em preparo</span></c:when>
                  <c:when test="${p.status=='pronto'}"><span class="badge badge-success">Pronto ✓</span></c:when>
                  <c:otherwise><span class="badge">${p.status}</span></c:otherwise>
                </c:choose>
              </div>

              <div class="pedido-meta">
                <span>
                  <c:choose>
                    <c:when test="${p.tipo=='mesa'}">🪑 Mesa ${p.mesa.numero}</c:when>
                    <c:otherwise>🛵 Delivery</c:otherwise>
                  </c:choose>
                </span>
                <span>👤 ${p.identificadorOperador}</span>
                <span>🕐 ${p.dataAberturaFormatada}</span>
                <c:if test="${p.urgente}">
                  <span class="badge badge-urgente">🔴 Urgente</span>
                </c:if>
              </div>

              <div class="pedido-itens">
                <c:forEach var="item" items="${p.itens}" varStatus="vs">
                  ${item.quantidade}x ${item.nomeItem}<c:if test="${!vs.last}"> · </c:if>
                </c:forEach>
              </div>

              <div style="display:flex;align-items:center;justify-content:space-between;flex-wrap:wrap;gap:10px">
                <span class="pedido-total">
                  Total: R$ <fmt:formatNumber value="${p.calcularTotal()}" minFractionDigits="2" maxFractionDigits="2"/>
                </span>

                <div class="pedido-acoes">
                  <a href="${pageContext.request.contextPath}/app/pedidos?acao=detalhe&id=${p.idPedido}"
                     class="btn btn-secondary">📋 Detalhes</a>

                  <c:if test="${p.status!='entregue' && p.status!='cancelado'}">
                    <c:choose>
                      <%-- Entrega: abre modal com pagamento --%>
                      <c:when test="${p.status=='pronto'}">
                        <button type="button" class="btn btn-primary"
                                onclick="abrirModalEntrega('${p.idPedido}')">
                          🍽️ Entregar
                        </button>
                      </c:when>
                      <%-- Demais status: modal simples de operador --%>
                      <c:otherwise>
                        <button type="button" class="btn btn-primary"
                                onclick="abrirModalStatus('${p.idPedido}')">
                          <c:choose>
                            <c:when test="${p.status=='aberto'}">▶ Iniciar preparo</c:when>
                            <c:when test="${p.status=='em_preparo'}">✓ Marcar pronto</c:when>
                          </c:choose>
                        </button>
                      </c:otherwise>
                    </c:choose>

                    <form method="POST" action="${pageContext.request.contextPath}/app/pedidos"
                          onsubmit="return confirm('Cancelar pedido #${p.idPedido}?')">
                      <input type="hidden" name="acao"     value="cancelar">
                      <input type="hidden" name="id"       value="${p.idPedido}">
                      <input type="hidden" name="operador" value="${p.identificadorOperador}">
                      <button type="submit" class="btn btn-danger">✕</button>
                    </form>
                  </c:if>
                </div>
              </div>

            </div>
          </c:forEach>
        </div>
      </c:otherwise>
    </c:choose>

  </main>
</div>
</div>

<%-- Modal avanço de status (aberto→em_preparo, em_preparo→pronto) --%>
<div class="modal-overlay" id="modalStatus">
  <div class="modal">
    <h3>Seu identificador</h3>
    <form method="POST" action="${pageContext.request.contextPath}/app/pedidos" id="formStatus">
      <input type="hidden" name="acao" value="avancarStatus">
      <input type="hidden" name="id"   id="statusPedidoId">
      <div class="form-group">
        <label>Identificador (ex: A1)</label>
        <input type="text" name="operador" id="statusOperador"
               placeholder="Ex: A1" maxlength="20" required>
      </div>
      <div class="modal-acoes">
        <button type="button" class="btn btn-secondary" onclick="fecharModal('modalStatus')">Cancelar</button>
        <button type="submit" class="btn btn-primary">Confirmar</button>
      </div>
    </form>
  </div>
</div>

<%-- Modal entrega + pagamento (pronto→entregue) --%>
<div class="modal-overlay" id="modalEntrega">
  <div class="modal">
    <h3>🍽️ Registrar Entrega</h3>
    <form method="POST" action="${pageContext.request.contextPath}/app/pedidos" id="formEntrega">
      <input type="hidden" name="acao" value="avancarStatus">
      <input type="hidden" name="id"   id="entregaPedidoId">

      <div class="form-group">
        <label>Seu identificador</label>
        <input type="text" name="operador" id="entregaOperador"
               placeholder="Ex: A1" maxlength="20" required>
      </div>

      <div class="form-group">
        <label>Forma de pagamento <span style="color:#ef4444">*</span></label>
        <select name="formaPagamento" required>
          <option value="">Selecione...</option>
          <option value="dinheiro">💵 Dinheiro</option>
          <option value="cartao">💳 Cartão</option>
          <option value="pix">📱 PIX</option>
        </select>
      </div>

      <div class="form-group">
        <label>Valor pago (R$) — opcional, usa o total do pedido se vazio</label>
        <input type="number" name="valorPagamento" placeholder="0,00"
               step="0.01" min="0">
      </div>

      <div class="modal-acoes">
        <button type="button" class="btn btn-secondary" onclick="fecharModal('modalEntrega')">Cancelar</button>
        <button type="submit" class="btn btn-primary">Confirmar entrega</button>
      </div>
    </form>
  </div>
</div>

<script>
  function abrirModalStatus(pedidoId) {
    document.getElementById('statusPedidoId').value = pedidoId;
    document.getElementById('statusOperador').value = '';
    document.getElementById('modalStatus').classList.add('aberto');
    document.getElementById('statusOperador').focus();
  }

  function abrirModalEntrega(pedidoId) {
    document.getElementById('entregaPedidoId').value = pedidoId;
    document.getElementById('entregaOperador').value = '';
    document.getElementById('modalEntrega').classList.add('aberto');
    document.getElementById('entregaOperador').focus();
  }

  function fecharModal(id) {
    document.getElementById(id).classList.remove('aberto');
  }

  function filtrarStatus(status, btn) {
    document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('ativo'));
    btn.classList.add('ativo');
    document.querySelectorAll('.pedido-card').forEach(card => {
      card.style.display = (status === 'todos' || card.dataset.status === status) ? '' : 'none';
    });
  }

  // Fechar modais ao clicar fora
  ['modalStatus','modalEntrega'].forEach(function(id) {
    document.getElementById(id).addEventListener('click', function(e) {
      if (e.target === this) fecharModal(id);
    });
  });

  document.addEventListener('keydown', function(e) {
    if (e.key === 'Escape') {
      fecharModal('modalStatus');
      fecharModal('modalEntrega');
    }
  });
</script>
</body>
</html>
