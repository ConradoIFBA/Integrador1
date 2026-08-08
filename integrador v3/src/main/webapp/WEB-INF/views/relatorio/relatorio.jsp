<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="br.com.restaurante.model.Usuario" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%
    Usuario _u = (Usuario) session.getAttribute("usuarioLogado");
    String _msgErro = (String) session.getAttribute("msgErro");
    session.removeAttribute("msgErro");
%>
<!DOCTYPE html>
<html lang="pt-BR">
<head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Relatórios — Integrador</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/style.css">
<style>
/* Seletor de período */
.periodo-grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(160px,1fr));gap:12px;margin-bottom:24px}
.periodo-card{border:2px solid #e2e8f0;border-radius:10px;padding:18px 14px;text-align:center;cursor:pointer;transition:all .2s;background:#fff}
.periodo-card:hover{border-color:#e85d27;background:#fff8f5}
.periodo-card.selecionado{border-color:#e85d27;background:#fff0ea}
.periodo-card input[type=radio]{display:none}
.periodo-card .icon{font-size:28px;margin-bottom:8px}
.periodo-card .label{font-size:14px;font-weight:600;color:#1e293b}
.periodo-card .desc{font-size:12px;color:#64748b;margin-top:3px}

/* Datas customizadas */
.datas-custom{display:none;background:#f8fafc;border:1px solid #e2e8f0;
  border-radius:8px;padding:16px;margin-bottom:20px}
.datas-custom.visivel{display:block}
.datas-row{display:grid;grid-template-columns:1fr 1fr;gap:14px}

/* Preview de stats */
.preview-stats{display:grid;grid-template-columns:repeat(auto-fill,minmax(180px,1fr));
  gap:14px;margin-top:20px}
.preview-card{background:#fff;border-radius:8px;box-shadow:0 2px 6px rgba(0,0,0,.08);
  padding:16px;border-left:3px solid #e85d27}
.preview-card .plabel{font-size:12px;color:#64748b;text-transform:uppercase;letter-spacing:.4px}
.preview-card .pvalor{font-size:22px;font-weight:800;color:#1e293b;margin-top:4px}

/* Botão de geração */
.btn-gerar{width:100%;padding:14px;font-size:16px;font-weight:700;
  background:linear-gradient(135deg,#e85d27,#c94d1e);color:#fff;
  border:none;border-radius:8px;cursor:pointer;transition:transform .2s,box-shadow .2s;
  font-family:inherit;display:flex;align-items:center;justify-content:center;gap:10px}
.btn-gerar:hover{transform:translateY(-1px);box-shadow:0 6px 18px rgba(232,93,39,.4)}
.btn-gerar:disabled{opacity:.6;cursor:not-allowed;transform:none}

.historico-item{display:flex;justify-content:space-between;align-items:center;
  padding:10px 0;border-bottom:1px solid #f1f5f9;font-size:13px}
.historico-item:last-child{border-bottom:none}
</style>
</head>
<body>
<div class="main-container">
<%@ include file="/WEB-INF/views/shared/_sidebar.jsp" %>
<div class="main-content">

  <header class="topbar">
    <div class="topbar-left"><h2>📄 Relatórios</h2></div>
    <div class="topbar-right">
      <div class="user-info">
        <div class="user-avatar"><%= _u.getNome().substring(0,1).toUpperCase() %></div>
        <div class="user-details">
          <span class="name"><%= _u.getNome() %></span>
          <span class="role">Gerente</span>
        </div>
      </div>
    </div>
  </header>

  <main class="content">

    <% if (_msgErro != null && !_msgErro.isEmpty()) { %>
      <div class="alert alert-error">✕ <%= _msgErro %></div>
    <% } %>

    <div style="display:grid;grid-template-columns:1fr 380px;gap:24px;align-items:start">

      <%-- COLUNA PRINCIPAL --%>
      <div>
        <div class="card">
          <h3>Gerar Relatório de Vendas</h3>

          <form method="POST" action="${pageContext.request.contextPath}/app/relatorios"
                id="formRelatorio" onsubmit="return confirmarGeracao()">

            <%-- Seleção de período --%>
            <div style="margin-bottom:16px">
              <div class="form-group" style="margin-bottom:12px">
                <label>Selecione o período</label>
              </div>

              <div class="periodo-grid">

                <label class="periodo-card" id="card-hoje">
                  <input type="radio" name="periodo" value="hoje"
                         onchange="selecionarPeriodo('hoje')">
                  <div class="icon">📅</div>
                  <div class="label">Hoje</div>
                  <div class="desc" id="desc-hoje">—</div>
                </label>

                <label class="periodo-card" id="card-semana">
                  <input type="radio" name="periodo" value="semana"
                         onchange="selecionarPeriodo('semana')">
                  <div class="icon">📆</div>
                  <div class="label">Últimos 7 dias</div>
                  <div class="desc" id="desc-semana">—</div>
                </label>

                <label class="periodo-card selecionado" id="card-mes">
                  <input type="radio" name="periodo" value="mes"
                         checked onchange="selecionarPeriodo('mes')">
                  <div class="icon">🗓️</div>
                  <div class="label">Mês atual</div>
                  <div class="desc" id="desc-mes">—</div>
                </label>

                <label class="periodo-card" id="card-customizado">
                  <input type="radio" name="periodo" value="customizado"
                         onchange="selecionarPeriodo('customizado')">
                  <div class="icon">✏️</div>
                  <div class="label">Personalizado</div>
                  <div class="desc">Escolha as datas</div>
                </label>

              </div>
            </div>

            <%-- Datas customizadas --%>
            <div class="datas-custom" id="datasCustom">
              <div class="datas-row">
                <div class="form-group" style="margin:0">
                  <label for="dataInicio">Data inicial</label>
                  <input type="date" id="dataInicio" name="dataInicio"
                         value="${dataInicioDefault}" required>
                </div>
                <div class="form-group" style="margin:0">
                  <label for="dataFim">Data final</label>
                  <input type="date" id="dataFim" name="dataFim"
                         value="${dataFimDefault}" required>
                </div>
              </div>
            </div>

            <%-- Botão de geração --%>
            <button type="submit" class="btn-gerar" id="btnGerar">
              <span>📄</span> <span id="btnTexto">Gerar PDF</span>
            </button>

          </form>
        </div>

        <%-- O que o PDF contém --%>
        <div class="card" style="margin-top:20px">
          <h3>📋 Conteúdo do Relatório</h3>
          <div style="display:flex;flex-direction:column;gap:10px;font-size:14px;color:#475569">
            <div style="display:flex;gap:10px;align-items:flex-start">
              <span style="color:#10b981;font-weight:700;flex-shrink:0">✓</span>
              <span><strong>Resumo do período</strong> — total de pedidos, faturamento e ticket médio</span>
            </div>
            <div style="display:flex;gap:10px;align-items:flex-start">
              <span style="color:#10b981;font-weight:700;flex-shrink:0">✓</span>
              <span><strong>Faturamento por categoria</strong> — Entradas, Pratos, Bebidas, Sobremesas</span>
            </div>
            <div style="display:flex;gap:10px;align-items:flex-start">
              <span style="color:#10b981;font-weight:700;flex-shrink:0">✓</span>
              <span><strong>Detalhamento dos pedidos</strong> — todos os pedidos entregues no período</span>
            </div>
            <div style="display:flex;gap:10px;align-items:flex-start">
              <span style="color:#3b82f6;font-weight:700;flex-shrink:0">ℹ</span>
              <span style="color:#64748b">Apenas pedidos com status <strong>entregue</strong> são incluídos.</span>
            </div>
          </div>
        </div>
      </div>

      <%-- COLUNA LATERAL --%>
      <div>
        <div class="card">
          <h3>📊 Como usar</h3>
          <div style="font-size:13px;color:#64748b;display:flex;flex-direction:column;gap:12px">
            <div>
              <strong style="color:#1e293b">Passo 1</strong><br>
              Selecione o período desejado nos cards à esquerda.
            </div>
            <div>
              <strong style="color:#1e293b">Passo 2</strong><br>
              Clique em <strong>Gerar PDF</strong>. O download começa automaticamente.
            </div>
            <div>
              <strong style="color:#1e293b">Passo 3</strong><br>
              Abra o arquivo PDF gerado para visualizar ou imprimir o relatório.
            </div>
          </div>
        </div>

        <div class="card" style="margin-top:16px">
          <h3>📅 Períodos disponíveis</h3>
          <div style="font-size:13px;color:#64748b;display:flex;flex-direction:column;gap:8px">
            <div class="historico-item">
              <span>📅 Hoje</span>
              <span id="info-hoje" style="color:#1e293b;font-weight:600">—</span>
            </div>
            <div class="historico-item">
              <span>📆 Últimos 7 dias</span>
              <span id="info-semana" style="color:#1e293b;font-weight:600">—</span>
            </div>
            <div class="historico-item">
              <span>🗓️ Mês atual</span>
              <span id="info-mes" style="color:#1e293b;font-weight:600">—</span>
            </div>
          </div>
        </div>
      </div>

    </div>

  </main>
</div>
</div>

<script>
// ── Calcula e exibe datas nos cards ──────────────────────────────
var meses = ['Jan','Fev','Mar','Abr','Mai','Jun','Jul','Ago','Set','Out','Nov','Dez'];

function fmtBr(d) {
  return d.getDate().toString().padStart(2,'0') + '/' +
         (d.getMonth()+1).toString().padStart(2,'0') + '/' + d.getFullYear();
}

function inicializar() {
  var hoje  = new Date();
  var sem   = new Date(); sem.setDate(sem.getDate() - 6);
  var iMes  = new Date(hoje.getFullYear(), hoje.getMonth(), 1);

  document.getElementById('desc-hoje').textContent   = fmtBr(hoje);
  document.getElementById('desc-semana').textContent = fmtBr(sem) + ' – ' + fmtBr(hoje);
  document.getElementById('desc-mes').textContent    = meses[hoje.getMonth()] + '/' + hoje.getFullYear();

  document.getElementById('info-hoje').textContent   = fmtBr(hoje);
  document.getElementById('info-semana').textContent = fmtBr(sem) + ' a ' + fmtBr(hoje);
  document.getElementById('info-mes').textContent    = fmtBr(iMes) + ' a ' + fmtBr(hoje);
}

function selecionarPeriodo(p) {
  document.querySelectorAll('.periodo-card').forEach(c => c.classList.remove('selecionado'));
  document.getElementById('card-' + p).classList.add('selecionado');

  var custom = document.getElementById('datasCustom');
  if (p === 'customizado') {
    custom.classList.add('visivel');
    document.getElementById('dataInicio').required = true;
    document.getElementById('dataFim').required    = true;
  } else {
    custom.classList.remove('visivel');
    document.getElementById('dataInicio').required = false;
    document.getElementById('dataFim').required    = false;
  }

  var labels = {
    hoje: 'Gerar PDF — Hoje',
    semana: 'Gerar PDF — Últimos 7 dias',
    mes: 'Gerar PDF — Mês atual',
    customizado: 'Gerar PDF — Período personalizado'
  };
  document.getElementById('btnTexto').textContent = labels[p] || 'Gerar PDF';
}

function confirmarGeracao() {
  var periodo = document.querySelector('input[name="periodo"]:checked').value;

  if (periodo === 'customizado') {
    var di = document.getElementById('dataInicio').value;
    var df = document.getElementById('dataFim').value;
    if (!di || !df) { alert('Informe as duas datas.'); return false; }
    if (di > df)    { alert('A data inicial deve ser anterior à final.'); return false; }
  }

  var btn = document.getElementById('btnGerar');
  btn.disabled = true;
  document.getElementById('btnTexto').textContent = 'Gerando PDF...';

  // Reabilita após 5s caso o download falhe
  setTimeout(function() {
    btn.disabled = false;
    document.getElementById('btnTexto').textContent = 'Gerar PDF';
  }, 5000);

  return true;
}

// Inicializa ao carregar
window.addEventListener('DOMContentLoaded', inicializar);
// Define o texto inicial do botão
document.getElementById('btnTexto').textContent = 'Gerar PDF — Mês atual';
</script>
</body>
</html>
