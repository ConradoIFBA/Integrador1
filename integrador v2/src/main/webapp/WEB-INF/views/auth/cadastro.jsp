<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="br.com.restaurante.model.Usuario" %>
<%
    if(session.getAttribute("usuarioLogado")!=null){response.sendRedirect(request.getContextPath()+"/auth/login");return;}
    String erro=(String)request.getAttribute("erro");
    String nome=request.getParameter("nome")!=null?request.getParameter("nome"):"";
    String login=request.getParameter("login")!=null?request.getParameter("login"):"";
%>
<!DOCTYPE html>
<html lang="pt-BR">
<head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Cadastro — Integrador</title>
<style>
*{margin:0;padding:0;box-sizing:border-box}
body{font-family:'Segoe UI',sans-serif;background:linear-gradient(135deg,#e85d27,#c94d1e);min-height:100vh;display:flex;justify-content:center;align-items:center;padding:20px}
.card{background:#fff;border-radius:14px;box-shadow:0 12px 40px rgba(0,0,0,.2);width:100%;max-width:440px;padding:40px}
.logo{text-align:center;margin-bottom:26px}
.logo .icon{font-size:44px}
.logo h1{font-size:24px;font-weight:700;color:#e85d27;margin:8px 0 4px}
.logo p{color:#64748b;font-size:13px}
.divider{height:2px;background:#e2e8f0;margin:0 0 22px}
.alert{padding:12px 14px;border-radius:7px;margin-bottom:18px;font-size:14px;display:flex;align-items:center;gap:8px}
.alert-error{background:#fee2e2;color:#991b1b;border-left:4px solid #ef4444}
.form-group{margin-bottom:16px}
.form-group label{display:block;font-size:13px;font-weight:600;color:#1e293b;margin-bottom:6px}
.form-group .ob{color:#ef4444;margin-left:2px}
.form-group input{width:100%;padding:11px 13px;border:2px solid #e2e8f0;border-radius:7px;font-size:14px;font-family:inherit;color:#1e293b;transition:border-color .2s}
.form-group input:focus{outline:none;border-color:#e85d27;box-shadow:0 0 0 3px rgba(232,93,39,.12)}
.form-group input::placeholder{color:#94a3b8}
.form-group .dica{font-size:12px;color:#94a3b8;margin-top:4px}
.btn-cadastrar{width:100%;padding:12px;background:linear-gradient(135deg,#e85d27,#c94d1e);color:#fff;border:none;border-radius:7px;font-size:15px;font-weight:600;cursor:pointer;transition:transform .2s;font-family:inherit;margin-top:4px}
.btn-cadastrar:hover{transform:translateY(-1px)}
.link-login{text-align:center;margin-top:18px;font-size:13px;color:#64748b}
.link-login a{color:#e85d27;font-weight:600;text-decoration:none}
</style>
</head>
<body>
<div class="card">
  <div class="logo"><div class="icon">🍽️</div><h1>Criar Conta</h1><p>Sistema de Pedidos para Restaurante</p></div>
  <div class="divider"></div>
  <%if(erro!=null&&!erro.isEmpty()){%><div class="alert alert-error"><span>✕</span><span><%=erro%></span></div><%}%>
  <form method="POST" action="${pageContext.request.contextPath}/auth/cadastro" onsubmit="return validar()">
    <div class="form-group"><label>Nome completo <span class="ob">*</span></label>
      <input type="text" name="nome" placeholder="Seu nome" value="<%=nome%>" required autofocus maxlength="100"></div>
    <div class="form-group"><label>Login <span class="ob">*</span></label>
      <input type="text" name="login" placeholder="Ex: joao.silva" value="<%=login%>" required minlength="3" maxlength="50" onkeydown="if(event.key==' ')event.preventDefault()" autocomplete="username">
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
  if(s2.length>0)c.style.borderColor=s1===s2?'#10b981':'#ef4444';else c.style.borderColor='#e2e8f0';
}
function validar(){
  if(document.getElementById('s1').value!==document.getElementById('s2').value){alert('As senhas não coincidem.');return false;}
  return true;
}
</script>
</body></html>
