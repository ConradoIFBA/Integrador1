<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="br.com.restaurante.model.Usuario" %>
<%@ taglib prefix="c"  uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%
    Usuario _u = (Usuario) session.getAttribute("usuarioLogado");
%>
<!DOCTYPE html>
<html lang="pt-BR">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>Fila de Preparo — Integrador</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/style.css">

<%-- Auto-refresh a cada 30 segundos --%>
<meta http-equiv="refresh" content="30">

<style>
/* ── Contador regressivo ── */
.refresh-bar {
    background: #1e293b;
    color: rgba(255,255,255,.8);
    text-align: center;
    font-size: 13px;
    padding: 6px;
    position: sticky;
    top: 0;
    z-index: 200;
}
.refresh-bar span { font-weight: 700; color: #e85d27; }

/* ── Grid de setores ── */
.setores-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
    gap: 24px;
}

.setor-titulo {
    font-size: 16px;
    font-weight: 700;
    padding: 12px 16px;
    border-radius: 8px 8px 0 0;
    display: flex;
    align-items: center;
    justify-content: space-between;
}
.setor-cozinha   .setor-titulo { background: #fef3c7; color: #92400e; }
.setor-bebida    .setor-titulo { background: #dbeafe; color: #1e40af; }
.setor-sobremesa .setor-titulo { background: #fce7f3; color: #9d174d; }

.setor-body {
    border: 2px solid #e2e8f0;
    border-top: none;
    border-radius: 0 0 8px 8px;
    min-height: 120px;
}

/* ── Card de pedido na fila ── */
.fila-card {
    padding: 16px;
    border-bottom: 1px solid #f1f5f9;
    position: relative;
}
.fila-card:last-child { border-bottom: none; }

.fila-card.urgente {
    background: #fff5f5;
    border-left: 4px solid #ef4444;
}
.fila-card.aguardando { background: #fff; }
.fila-card.em-preparo { background: #fffbeb; border-left: 4px solid #f59e0b; }

.fila-posicao {
    position: absolute;
    top: 12px; right: 12px;
    font-size: 22px;
    font-weight: 900;
    color: #e2e8f0;
}

.fila-id {
    font-size: 18px;
    font-weight: 800;
    color: #1e293b;
    margin-bottom: 4px;
}

.fila-meta {
    font-size: 12px;
    color: #64748b;
    display: flex;
    gap: 12px;
    flex-wrap: wrap;
    margin-bottom: 10px;
}

.fila-acoes { display: flex; gap: 8px; }
.fila-acoes .btn { font-size: 13px; padding: 7px 14px; }

.tempo-badge {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    padding: 2px 10px;
    border-radius: 12px;
    font-size: 12px;
    font-weight: 600;
    background: #f1f5f9;
    color: #475569;
}

.vazia {
    text-align: center;
    padding: 32px 16px;
    color: #94a3b8;
    font-size: 14px;
}
.vazia .icon { font-size: 32px; margin-bottom: 8px; }

/* ── Modal operador ── */
.modal-overlay {
    display: none;
    position: fixed;
    inset: 0;
    background: rgba(0,0,0,.5);
    z-index: 2000;
    align-items: center;
    justify-content: center;
}
.modal-overlay.aberto { display: flex; }
.modal {
    background: #fff;
    border-radius: 12px;
    padding: 28px;
    width: 100%;
    max-width: 320px;
    box-shadow: 0 20px 60px rgba(0,0,0,.3);
}
.modal h3 { font-size: 17px; margin-bottom: 16px; color: #1e293b; }
.modal input {
    width: 100%;
    padding: 10px 12px;
    border: 2px solid #e2e8f0;
    border-radius: 7px;
    font-size: 14px;
    margin-bottom: 14px;
}
.modal input:focus { outline: none; border-color: #e85d27; }
.modal-acoes { display: flex; gap: 10px; }
.modal-acoes .btn { flex: 1; }
</style>
</head>
<body>
<div class="main-container">
<%@ include file="/WEB-INF/views/shared/_sidebar.jsp" %>
<div class="main-content">

    <%-- Barra de auto-refresh --%>
    <div class="refresh-bar">
        Página atualiza automaticamente em <span id="contador">30</span>s
    </div>

    <header class="topbar">
        <div class="topbar-left"><h2>👨‍🍳 Fila de Preparo</h2></div>
        <div class="topbar-right">
            <button onclick="location.reload()" class="btn btn-secondary btn-sm">
                🔄 Atualizar agora
            </button>
            <div class="user-info" style="margin-left:8px">
                <div class="user-avatar"><%= _u.getNome().substring(0,1).toUpperCase() %></div>
                <div class="user-details">
                    <span class="name"><%= _u.getNome() %></span>
                    <span class="role"><%= _u.getPerfil() %></span>
                </div>
            </div>
        </div>
    </header>

    <main class="content">

        <c:if test="${not empty msgSucesso}">
            <div class="alert alert-success">✓ ${msgSucesso}</div>
        </c:if>

        <div class="setores-grid">

            <%-- ── COZINHA ── --%>
            <div class="setor-cozinha">
                <div class="setor-titulo">
                    <span>🍳 Cozinha</span>
                    <span class="tempo-badge">${fn:length(filaCozinha)} pedido(s)</span>
                </div>
                <div class="setor-body">
                    <c:choose>
                        <c:when test="${empty filaCozinha}">
                            <div class="vazia"><div class="icon">✅</div><p>Fila vazia</p></div>
                        </c:when>
                        <c:otherwise>
                            <c:forEach var="f" items="${filaCozinha}">
                                <jsp:include page="/WEB-INF/views/fila/_card_fila.jsp">
                                    <jsp:param name="idFila"   value="${f.idFila}"/>
                                    <jsp:param name="idPedido" value="${f.pedidoId}"/>
                                    <jsp:param name="posicao"  value="${f.posicao}"/>
                                    <jsp:param name="tipo"     value="${f.pedido.tipo}"/>
                                    <jsp:param name="mesa"     value="${f.pedido.numeroMesa}"/>
                                    <jsp:param name="urgente"  value="${f.pedido.urgente}"/>
                                    <jsp:param name="tempo"    value="${f.tempoEstimadoMin}"/>
                                    <jsp:param name="operador" value="${f.identificadorOperador}"/>
                                    <jsp:param name="aguardando" value="${f.aguardando}"/>
                                    <jsp:param name="entrada"  value="${f.dataEntradaFormatada}"/>
                                </jsp:include>
                            </c:forEach>
                        </c:otherwise>
                    </c:choose>
                </div>
            </div>

            <%-- ── BEBIDAS ── --%>
            <div class="setor-bebida">
                <div class="setor-titulo">
                    <span>🥤 Bebidas</span>
                    <span class="tempo-badge">${fn:length(filaBebida)} pedido(s)</span>
                </div>
                <div class="setor-body">
                    <c:choose>
                        <c:when test="${empty filaBebida}">
                            <div class="vazia"><div class="icon">✅</div><p>Fila vazia</p></div>
                        </c:when>
                        <c:otherwise>
                            <c:forEach var="f" items="${filaBebida}">
                                <jsp:include page="/WEB-INF/views/fila/_card_fila.jsp">
                                    <jsp:param name="idFila"   value="${f.idFila}"/>
                                    <jsp:param name="idPedido" value="${f.pedidoId}"/>
                                    <jsp:param name="posicao"  value="${f.posicao}"/>
                                    <jsp:param name="tipo"     value="${f.pedido.tipo}"/>
                                    <jsp:param name="mesa"     value="${f.pedido.numeroMesa}"/>
                                    <jsp:param name="urgente"  value="${f.pedido.urgente}"/>
                                    <jsp:param name="tempo"    value="${f.tempoEstimadoMin}"/>
                                    <jsp:param name="operador" value="${f.identificadorOperador}"/>
                                    <jsp:param name="aguardando" value="${f.aguardando}"/>
                                    <jsp:param name="entrada"  value="${f.dataEntradaFormatada}"/>
                                </jsp:include>
                            </c:forEach>
                        </c:otherwise>
                    </c:choose>
                </div>
            </div>

            <%-- ── SOBREMESAS ── --%>
            <div class="setor-sobremesa">
                <div class="setor-titulo">
                    <span>🍮 Sobremesas</span>
                    <span class="tempo-badge">${fn:length(filaSobremesa)} pedido(s)</span>
                </div>
                <div class="setor-body">
                    <c:choose>
                        <c:when test="${empty filaSobremesa}">
                            <div class="vazia"><div class="icon">✅</div><p>Fila vazia</p></div>
                        </c:when>
                        <c:otherwise>
                            <c:forEach var="f" items="${filaSobremesa}">
                                <jsp:include page="/WEB-INF/views/fila/_card_fila.jsp">
                                    <jsp:param name="idFila"   value="${f.idFila}"/>
                                    <jsp:param name="idPedido" value="${f.pedidoId}"/>
                                    <jsp:param name="posicao"  value="${f.posicao}"/>
                                    <jsp:param name="tipo"     value="${f.pedido.tipo}"/>
                                    <jsp:param name="mesa"     value="${f.pedido.numeroMesa}"/>
                                    <jsp:param name="urgente"  value="${f.pedido.urgente}"/>
                                    <jsp:param name="tempo"    value="${f.tempoEstimadoMin}"/>
                                    <jsp:param name="operador" value="${f.identificadorOperador}"/>
                                    <jsp:param name="aguardando" value="${f.aguardando}"/>
                                    <jsp:param name="entrada"  value="${f.dataEntradaFormatada}"/>
                                </jsp:include>
                            </c:forEach>
                        </c:otherwise>
                    </c:choose>
                </div>
            </div>

        </div>
    </main>
</div>
</div>

<%-- Modal de identificação --%>
<div class="modal-overlay" id="modalOverlay">
    <div class="modal">
        <h3 id="modalTitulo">Seu identificador</h3>
        <form method="POST" action="${pageContext.request.contextPath}/app/fila" id="formModal">
            <input type="hidden" name="acao"      id="modalAcao">
            <input type="hidden" name="idFila"    id="modalIdFila">
            <input type="hidden" name="idPedido"  id="modalIdPedido">
            <input type="text"   name="operador"  id="inputOperador"
                   placeholder="Ex: C1, C2" maxlength="20" required>
            <div class="modal-acoes">
                <button type="button" class="btn btn-secondary" onclick="fecharModal()">Cancelar</button>
                <button type="submit" class="btn btn-primary">Confirmar</button>
            </div>
        </form>
    </div>
</div>

<script>
    // Contador regressivo
    var seg = 30;
    setInterval(function() {
        seg--;
        document.getElementById('contador').textContent = seg;
        if (seg <= 0) location.reload();
    }, 1000);

    var titulos = {
        iniciar:  '▶ Iniciar preparo',
        concluir: '✓ Concluir preparo'
    };

    function abrirModal(acao, idFila, idPedido) {
        document.getElementById('modalAcao').value    = acao;
        document.getElementById('modalIdFila').value  = idFila;
        document.getElementById('modalIdPedido').value= idPedido;
        document.getElementById('modalTitulo').textContent = titulos[acao] || 'Confirmar';
        document.getElementById('inputOperador').value = '';
        document.getElementById('modalOverlay').classList.add('aberto');
        document.getElementById('inputOperador').focus();
    }

    function fecharModal() {
        document.getElementById('modalOverlay').classList.remove('aberto');
    }

    document.getElementById('modalOverlay').addEventListener('click', function(e) {
        if (e.target === this) fecharModal();
    });
    document.addEventListener('keydown', function(e) {
        if (e.key === 'Escape') fecharModal();
        if (e.key === 'Enter' && document.getElementById('modalOverlay').classList.contains('aberto')) {
            document.getElementById('formModal').submit();
        }
    });
</script>
</body>
</html>
