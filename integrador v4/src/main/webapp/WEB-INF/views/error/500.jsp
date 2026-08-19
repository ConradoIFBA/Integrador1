<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" %>
<%-- ================================================================
     500.JSP — ERRO INTERNO (v4 — tema escuro nativo)
     ================================================================
     As 3 páginas de erro (403/404/500) tinham fundo CLARO fixo no
     CSS (background:#f1f5f9), mesmo depois da migração do resto do
     sistema para tema escuro — por isso, se você viu um print dessa
     tela já com fundo escuro antes desta correção, era o modo escuro
     do NAVEGADOR "forçando" a renderização (comum no Chrome/Edge),
     não a página em si. Agora ela é escura de verdade, no próprio
     CSS, e vai aparecer igual em qualquer navegador/config. Nenhuma
     mudança de lógica — a página continua sendo puramente estática. ================================================================ --%>
<!DOCTYPE html><html lang="pt-BR"><head><meta charset="UTF-8"><title>500 — Integrador</title>
<style>*{margin:0;padding:0;box-sizing:border-box}
body{font-family:'Segoe UI',system-ui,sans-serif;background:#0b0e14;color:#f1f5f9;display:flex;align-items:center;justify-content:center;min-height:100vh}
.box{background:#161b25;border:1px solid rgba(255,255,255,.08);border-radius:16px;box-shadow:0 12px 40px rgba(0,0,0,.5);padding:48px;text-align:center;max-width:440px}
.code{font-size:72px;font-weight:800;color:#ef4444}
.h2{font-size:22px;margin:12px 0 8px;font-weight:700}
.p{color:#94a3b8;font-size:14px}
a{display:inline-block;margin-top:24px;padding:11px 26px;background:#22c55e;color:#06240f;border-radius:9px;text-decoration:none;font-weight:700}
a:hover{background:#16a34a}</style>
</head><body><div class="box"><div class="code">500</div>
<p class="h2">Erro interno</p><p class="p">Ocorreu um problema inesperado. Tente novamente.</p><a href="${pageContext.request.contextPath}/app/dashboard">Voltar ao início</a>
</div></body></html>
