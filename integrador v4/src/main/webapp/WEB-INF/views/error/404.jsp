<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" %>
<%-- Ver comentário completo em 500.jsp — mesma correção aplicada
     às 3 páginas de erro (403/404/500): fundo escuro nativo no CSS,
     não dependente de dark-mode do navegador. --%>
<!DOCTYPE html><html lang="pt-BR"><head><meta charset="UTF-8"><title>404 — Integrador</title>
<style>*{margin:0;padding:0;box-sizing:border-box}
body{font-family:'Segoe UI',system-ui,sans-serif;background:#0b0e14;color:#f1f5f9;display:flex;align-items:center;justify-content:center;min-height:100vh}
.box{background:#161b25;border:1px solid rgba(255,255,255,.08);border-radius:16px;box-shadow:0 12px 40px rgba(0,0,0,.5);padding:48px;text-align:center;max-width:440px}
.code{font-size:72px;font-weight:800;color:#3b82f6}
.h2{font-size:22px;margin:12px 0 8px;font-weight:700}
.p{color:#94a3b8;font-size:14px}
a{display:inline-block;margin-top:24px;padding:11px 26px;background:#22c55e;color:#06240f;border-radius:9px;text-decoration:none;font-weight:700}
a:hover{background:#16a34a}</style>
</head><body><div class="box"><div class="code">404</div>
<p class="h2">Página não encontrada</p><p class="p">A página que você procura não existe.</p><a href="${pageContext.request.contextPath}/app/dashboard">Voltar ao início</a>
</div></body></html>
