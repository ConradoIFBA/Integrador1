<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="br.com.restaurante.model.Usuario" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%--
    ================================================================
    CADASTRO.JSP — CRIAR CONTA DE CLIENTE (v4 — tema escuro)
    ================================================================
    Duas mudanças em relação à versão anterior:
      1. Visual: mesmo tema escuro do login.jsp (fundo, cores,
         inputs), já que as duas telas fazem parte do mesmo fluxo de
         autenticação e devem parecer a mesma "família" visual.
      2. CORREÇÃO DE SEGURANÇA (aproveitando que já estava mexendo
         no arquivo): os campos "nome" e "login" eram reexibidos
         após um cadastro malsucedido com <%=nome%> / <%=login%> —
         ou seja, IMPRIMIDOS SEM ESCAPE. Se alguém preenchesse o
         campo nome com algo como <script>...</script> e o cadastro
         falhasse por outro motivo (ex: login duplicado), esse script
         seria refletido de volta na página e executado no navegador
         de quem quer que visse essa resposta — um XSS refletido
         clássico. Troquei para ${fn:escapeXml(param.nome)} e
         ${fn:escapeXml(param.login)} (função padrão do JSTL que
         converte < > & " ' nos códigos HTML equivalentes), então o
         texto aparece como TEXTO na tela, nunca como HTML/JS
         executável. Nenhuma outra lógica do formulário mudou.
    ================================================================
--%>
<%
    if(session.getAttribute("usuarioLogado")!=null){response.sendRedirect(request.getContextPath()+"/auth/login");return;}
    String erro=(String)request.getAttribute("erro");
%>
<!DOCTYPE html>
<html lang="pt-BR">
<head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Cadastro — Integrador</title>
<style>
*{margin:0;padding:0;box-sizing:border-box}
body{font-family:'Segoe UI',system-ui,sans-serif;background:#0b0e14;min-height:100vh;display:flex;justify-content:center;align-items:center;padding:20px;color:#f1f5f9}
.card{background:#161b25;border:1px solid rgba(255,255,255,.08);border-radius:16px;box-shadow:0 12px 40px rgba(0,0,0,.5);width:100%;max-width:440px;padding:36px}
.logo{text-align:center;margin-bottom:24px}
.logo .icon{width:52px;height:52px;border-radius:13px;background:#22c55e;font-size:24px;display:flex;align-items:center;justify-content:center;margin:0 auto 12px}
.logo h1{font-size:21px;font-weight:700}
.logo p{color:#94a3b8;font-size:13px;margin-top:2px}
.alert-error{background:rgba(239,68,68,.15);color:#fca5a5;border:1px solid rgba(239,68,68,.35);padding:12px 14px;border-radius:9px;margin-bottom:18px;font-size:13px;display:flex;align-items:center;gap:8px}
.form-group{margin-bottom:15px}
.form-group label{display:block;font-size:12px;font-weight:700;text-transform:uppercase;letter-spacing:.4px;color:#94a3b8;margin-bottom:7px}
.form-group .ob{color:#ef4444;margin-left:2px;text-transform:none}
.form-group input{width:100%;padding:11px 13px;border:1.5px solid rgba(255,255,255,.14);border-radius:9px;font-size:14px;font-family:inherit;color:#f1f5f9;background:#0f131a;transition:border-color .2s}
.form-group input::placeholder{color:#64748b}
.form-group input:focus{outline:none;border-color:#22c55e;box-shadow:0 0 0 3px rgba(34,197,94,.15)}
.form-group .dica{font-size:11px;color:#64748b;margin-top:4px}
.btn-cadastrar{width:100%;padding:13px;background:#22c55e;color:#06240f;border:none;border-radius:9px;font-size:15px;font-weight:700;cursor:pointer;transition:all .2s;font-family:inherit;margin-top:4px}
.btn-cadastrar:hover{background:#16a34a;transform:translateY(-1px)}
.link-login{text-align:center;margin-top:18px;font-size:13px;color:#94a3b8}
.link-login a{color:#22c55e;font-weight:600;text-decoration:none}
</style>
</head>
<body>
<div class="card">
  <div class="logo"><div class="icon">🍽️</div><h1>Criar Conta</h1><p>Sistema de Pedidos para Restaurante</p></div>
  <%if(erro!=null&&!erro.isEmpty()){%><div class="alert-error"><span>✕</span><span><%=erro%></span></div><%}%>
  <form method="POST" action="${pageContext.request.contextPath}/auth/cadastro" onsubmit="return validar()">
    <div class="form-group"><label>Nome completo <span class="ob">*</span></label>
      <%-- fn:escapeXml evita refletir HTML/JS digitado de volta na
           página em caso de erro no cadastro — ver explicação no
           comentário do topo do arquivo. --%>
      <input type="text" name="nome" placeholder="Seu nome" value="${fn:escapeXml(param.nome)}" required autofocus maxlength="100"></div>
    <div class="form-group"><label>Login <span class="ob">*</span></label>
      <input type="text" name="login" placeholder="Ex: joao.silva" value="${fn:escapeXml(param.login)}" required minlength="3" maxlength="50" onkeydown="if(event.key==' ')event.preventDefault()" autocomplete="username">
      <span class="dica">Mínimo 3 caracteres, sem espaços.</span></div>
    <div class="form-group"><label>Senha <span class="ob">*</span></label>
      <input type="password" id="s1" name="senha" placeholder="••••••••" required minlength="6" oninput="validarSenhas()" autocomplete="new-password"></div>
    <div class="form-group"><label>Confirmar senha <span class="ob">*</span></label>
      <input type="password" id="s2" name="confirmarSenha" placeholder="••••••••" required minlength="6" oninput="validarSenhas()" autocomplete="new-password"></div>
    <button type="submit" class="btn-cadastrar">Criar conta</button>
  </form>
  <div class="link-login">Já tem conta? <a href="${pageContext.request.contextPath}/auth/login">Faça login</a></div>
</div>
<script>
function validarSenhas(){
  var s1=document.getElementById('s1').value,s2=document.getElementById('s2').value,c=document.getElementById('s2');
  if(s2.length>0)c.style.borderColor=s1===s2?'#22c55e':'#ef4444';else c.style.borderColor='rgba(255,255,255,.14)';
}
function validar(){
  if(document.getElementById('s1').value!==document.getElementById('s2').value){alert('As senhas não coincidem.');return false;}
  return true;
}
</script>
</body></html>
