<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="br.com.restaurante.model.Usuario" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%--
    ================================================================
    RELATORIO.JSP — GERAÇÃO DE PDF (v4 — tema escuro)
    ================================================================
    Apenas troca de cores/variáveis para o tema escuro; nenhuma
    mudança de estrutura, lógica de JS (seleção de período, datas
    customizadas, confirmação antes de gerar) ou do formulário em
    si. O aviso do Eclipse sobre "<div> dentro de <label>" nas
    linhas dos .periodo-card é um falso positivo do validador HTML4
    do Eclipse (div dentro de label é válido em HTML5) — já
    identificado e documentado antes, sem ação necessária.
    ================================================================
--%>
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
.layout-rel{display:grid;grid-template-columns:1fr 360px;gap:24px;align-items:start}
@media (max-width:900px){.layout-rel{grid-template-columns:1fr}}

.periodo-grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(160px,1fr));gap:12px;margin-bottom:24px}
.periodo-card{
  border:1.5px solid var(--border-subtle-2);border-radius:var(--radius-sm);padding:18px 14px;
  text-align:center;cursor:pointer;transition:var(--transition);background:var(--bg-card)
}
.periodo-card:hover{border-color:var(--primary)}
.periodo-card.selecionado{border-color:var(--primary);background:var(--primary-light)}
.periodo-card input[type=radio]{display:none}
.periodo-card .icon{font-size:26px;margin-bottom:8px}
.periodo-card .label{font-size:14px;font-weight:600}
.periodo-card .desc{font-size:12px;color:var(--text-secondary);margin-top:3px}

.datas-custom{display:none;background:var(--bg-input);border:1px solid var(--border-subtle);border-radius:var(--radius-sm);padding:16px;margin-bottom:20px}
.datas-custom.visivel{display:block}
.datas-row{display:grid;grid-template-columns:1fr 1fr;gap:14px}

.btn-gerar{
  width:100%;padding:14px;font-size:16px;font-weight:700;background:var(--primary);color:#06240f;
  border:none;border-radius:var(--radius-sm);cursor:pointer;transition:var(--transition);
  font-family:inherit;display:flex;align-items:center;justify-content:center;gap:10px
}
.btn-gerar:hover{background:var(--primary-hover);transform:translateY(-1px)}
.btn-gerar:disabled{opacity:.6;cursor:not-allowed;transform:none}

.historico-item{display:flex;justify-content:space-between;align-items:center;padding:10px 0;border-bottom:1px solid var(--border-subtle);font-size:13px}
.historico-item:last-child{border-bottom:none}

.check-linha{display:flex;gap:10px;align-items:flex-start;font-size:14px}
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

    <div class="layout-rel">

      <div>
        <div class="card">
          <h3>Gerar Relatório de Vendas</h3>

          <form method="POST" action="${pageContext.request.contextPath}/app/relatorios"
                id="formRelatorio" onsubmit="return confirmarGeracao()">

            <div style="margin-bottom:16px">
              <div class="form-group" style="margin-bottom:12px">
                <label>Selecione o período</label>
              </div>

              <div class="periodo-grid">

                <label class="periodo-card" id="card-hoje">
                  <input type="radio" name="periodo" value="hoje" onchange="selecionarPeriodo('hoje')">
                  <div class="icon">📅</div>
                  <div class="label">Hoje</div>
                  <div class="desc" id="desc-hoje">—</div>
                </label>

                <label class="periodo-card" id="card-semana">
                  <input type="radio" name="periodo" value="semana" onchange="selecionarPeriodo('semana')">
                  <div class="icon">📆</div>
                  <div class="label">Últimos 7 dias</div>
                  <div class="desc" id="desc-semana">—</div>
                </label>

                <label class="periodo-card selecionado" id="card-mes">
                  <input type="radio" name="periodo" value="mes" checked onchange="selecionarPeriodo('mes')">
                  <div class="icon">🗓️</div>
                  <div class="label">Mês atual</div>
                  <div class="desc" id="desc-mes">—</div>
                </label>

                <label class="periodo-card" id="card-customizado">
                  <input type="radio" name="periodo" value="customizado" onchange="selecionarPeriodo('customizado')">
                  <div class="icon">✏️</div>
                  <div class="label">Personalizado</div>
                  <div class="desc">Escolha as datas</div>
                </label>

              </div>
            </div>

            <div class="datas-custom" id="datasCustom">
              <div class="datas-row">
                <div class="form-group" style="margin:0">
                  <label for="dataInicio">Data inicial</label>
                  <input type="date" id="dataInicio" name="dataInicio" value="${dataInicioDefault}" required>
                </div>
                <div class="form-group" style="margin:0">
                  <label for="dataFim">Data final</label>
                  <input type="date" id="dataFim" name="dataFim" value="${dataFimDefault}" required>
                </div>
              </div>
            </div>

            <button type="submit" class="btn-gerar" id="btnGerar">
              <span>📄</span> <span id="btnTexto">Gerar PDF</span>
            </button>

          </form>
        </div>

        <div class="card" style="margin-top:20px">
          <h3>📋 Conteúdo do Relatório</h3>
          <div style="display:flex;flex-direction:column;gap:12px">
            <div class="check-linha">
              <span style="color:var(--success-color);font-weight:700;flex-shrink:0">✓</span>
              <span><strong>Resumo do período</strong> — total de pedidos, faturamento e ticket médio</span>
            </div>
            <div class="check-linha">
              <span style="color:var(--success-color);font-weight:700;flex-shrink:0">✓</span>
              <span><strong>Faturamento por categoria</strong> — Entradas, Pratos, Bebidas, Sobremesas</span>
            </div>
            <div class="check-linha">
              <span style="color:var(--success-color);font-weight:700;flex-shrink:0">✓</span>
              <span><strong>Detalhamento dos pedidos</strong> — todos os pedidos entregues no período</span>
            </div>
            <div class="check-linha">
              <span style="color:var(--info-color);font-weight:700;flex-shrink:0">ℹ</span>
              <span class="text-muted">Apenas pedidos com status <strong>entregue</strong> são incluídos.</span>
            </div>
          </div>
        </div>
      </div>

      <div>
        <div class="card">
          <h3>📊 Como usar</h3>
          <div style="font-size:13px;color:var(--text-secondary);display:flex;flex-direction:column;gap:12px">
            <div><strong style="color:var(--text-primary)">Passo 1</strong><br>Selecione o período desejado nos cards à esquerda.</div>
            <div><strong style="color:var(--text-primary)">Passo 2</strong><br>Clique em <strong>Gerar PDF</strong>. O download começa automaticamente.</div>
            <div><strong style="color:var(--text-primary)">Passo 3</strong><br>Abra o arquivo PDF gerado para visualizar ou imprimir o relatório.</div>
          </div>
        </div>

        <div class="card" style="margin-top:16px">
          <h3>📅 Períodos disponíveis</h3>
          <div style="font-size:13px;color:var(--text-secondary);display:flex;flex-direction:column;gap:8px">
            <div class="historico-item">
              <span>📅 Hoje</span>
              <span id="info-hoje" style="color:var(--text-primary);font-weight:600">—</span>
            </div>
            <div class="historico-item">
              <span>📆 Últimos 7 dias</span>
              <span id="info-semana" style="color:var(--text-primary);font-weight:600">—</span>
            </div>
            <div class="historico-item">
              <span>🗓️ Mês atual</span>
              <span id="info-mes" style="color:var(--text-primary);font-weight:600">—</span>
            </div>
          </div>
        </div>
      </div>

    </div>

  </main>
</div>
</div>

<script>
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

  setTimeout(function() {
    btn.disabled = false;
    document.getElementById('btnTexto').textContent = 'Gerar PDF';
  }, 5000);

  return true;
}

window.addEventListener('DOMContentLoaded', inicializar);
document.getElementById('btnTexto').textContent = 'Gerar PDF — Mês atual';
</script>
</body>
</html>
