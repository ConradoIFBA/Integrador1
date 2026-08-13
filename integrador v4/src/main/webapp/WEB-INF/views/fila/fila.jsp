<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="br.com.restaurante.model.Usuario" %>
<%@ taglib prefix="c"  uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%--
    ================================================================
    FILA.JSP — FILA DE PREPARO / KANBAN (v4 — visual alinhado ao Figma)
    ================================================================
    A estrutura de 3 colunas (Cozinha/Bebidas/Sobremesas) já batia
    muito bem com o Figma antes — mudei principalmente CORES (tema
    escuro, cabeçalho de coluna colorido sólido em vez de fundo
    pastel) e o visual dos cards dentro de cada coluna (badge de
    status "Recebido"/"Em Preparo" no topo do card, igual ao
    protótipo). O JS de auto-refresh (30s), o modal de identificação
    e as ações (Iniciar/Concluir) continuam 100% iguais.
    ================================================================
--%>
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

<%-- Auto-refresh a cada 30 segundos — inalterado --%>
<meta http-equiv="refresh" content="30">

<style>
.refresh-bar{
  background:var(--bg-sidebar);color:var(--text-secondary);text-align:center;font-size:12px;
  padding:6px;position:sticky;top:0;z-index:200;border-bottom:1px solid var(--border-subtle)
}
.refresh-bar span{font-weight:700;color:var(--primary)}

.setores-grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(320px,1fr));gap:20px}

.setor-coluna{background:var(--bg-card);border:1px solid var(--border-subtle);border-radius:var(--radius);overflow:hidden}
.setor-titulo{
  font-size:15px;font-weight:700;padding:14px 16px;display:flex;align-items:center;
  justify-content:space-between;color:#fff
}
.setor-cozinha   .setor-titulo{background:#c2410c}
.setor-bebida    .setor-titulo{background:#2563eb}
.setor-sobremesa .setor-titulo{background:#be185d}
.setor-titulo .contador{
  background:rgba(255,255,255,.25);padding:1px 10px;border-radius:var(--radius-pill);font-size:13px
}

.setor-body{min-height:120px}

.fila-card{padding:16px;border-bottom:1px solid var(--border-subtle);position:relative}
.fila-card:last-child{border-bottom:none}
.fila-card.urgente{background:var(--error-bg)}

.fila-topo{display:flex;align-items:center;justify-content:space-between;margin-bottom:8px}
.fila-id{font-size:15px;font-weight:700}
.fila-status-badge{font-size:11px}

.fila-meta{font-size:12px;color:var(--text-secondary);display:flex;gap:10px;flex-wrap:wrap;margin-bottom:12px}
.fila-meta .tempo{color:var(--warning-color);font-weight:600}

.fila-preparando-por{font-size:12px;color:var(--warning-color);font-weight:600;margin-bottom:10px}

.fila-acoes .btn{width:100%;font-size:13px;padding:8px 14px}

.vazia{text-align:center;padding:32px 16px;color:var(--text-muted);font-size:13px}
.vazia .icon{font-size:30px;margin-bottom:8px;opacity:.6}
</style>
</head>
<body>
<div class="main-container">
<%@ include file="/WEB-INF/views/shared/_sidebar.jsp" %>
<div class="main-content">

    <div class="refresh-bar">
        Página atualiza automaticamente em <span id="contador">30</span>s
    </div>

    <header class="topbar">
        <div class="topbar-left"><h2>Fila de Preparo</h2></div>
        <div class="topbar-right">
            <button onclick="location.reload()" class="btn btn-secondary btn-sm">🔄 Atualizar</button>
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

            <div class="setor-cozinha setor-coluna">
                <div class="setor-titulo">
                    <span>🔥 Cozinha</span>
                    <span class="contador">${fn:length(filaCozinha)}</span>
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

            <div class="setor-bebida setor-coluna">
                <div class="setor-titulo">
                    <span>🥤 Bebidas</span>
                    <span class="contador">${fn:length(filaBebida)}</span>
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

            <div class="setor-sobremesa setor-coluna">
                <div class="setor-titulo">
                    <span>🍮 Sobremesas</span>
                    <span class="contador">${fn:length(filaSobremesa)}</span>
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

<div class="modal-overlay" id="modalOverlay">
    <div class="modal">
        <h3 id="modalTitulo">Seu identificador</h3>
        <form method="POST" action="${pageContext.request.contextPath}/app/fila" id="formModal">
            <input type="hidden" name="acao"      id="modalAcao">
            <input type="hidden" name="idFila"    id="modalIdFila">
            <input type="hidden" name="idPedido"  id="modalIdPedido">
            <div class="form-group">
                <input type="text" name="operador" id="inputOperador"
                       placeholder="Ex: C1, C2" maxlength="20" required>
            </div>
            <div class="modal-acoes">
                <button type="button" class="btn btn-secondary" onclick="fecharModal()">Cancelar</button>
                <button type="submit" class="btn btn-primary">Confirmar</button>
            </div>
        </form>
    </div>
</div>

<script>
    var seg = 30;
    setInterval(function() {
        seg--;
        document.getElementById('contador').textContent = seg;
        if (seg <= 0) location.reload();
    }, 1000);

    var titulos = { iniciar: '▶ Iniciar preparo', concluir: '✓ Concluir preparo' };

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
