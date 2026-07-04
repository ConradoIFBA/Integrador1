<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" %>
<!DOCTYPE html><html lang="pt-BR"><head><meta charset="UTF-8"><title>403 — Integrador</title>
<style>*{margin:0;padding:0;box-sizing:border-box}body{font-family:'Segoe UI',sans-serif;background:#f1f5f9;display:flex;align-items:center;justify-content:center;min-height:100vh}
.box{background:#fff;border-radius:12px;box-shadow:0 4px 20px rgba(0,0,0,.1);padding:48px;text-align:center;max-width:440px}
.code{font-size:72px;font-weight:700;color:#e85d27}.h2{font-size:22px;margin:12px 0 8px;color:#1e293b}.p{color:#64748b;font-size:14px}
a{display:inline-block;margin-top:24px;padding:10px 24px;background:#e85d27;color:#fff;border-radius:7px;text-decoration:none;font-weight:600}</style>
</head><body><div class="box"><div class="code">403</div>
<p class="h2">Acesso negado</p><p class="p">Você não tem permissão para acessar esta página.</p><a href="${pageContext.request.contextPath}/auth/login">Voltar ao login</a>
</div></body></html>
