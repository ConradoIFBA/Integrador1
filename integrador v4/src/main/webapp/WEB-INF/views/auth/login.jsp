<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@ page import="br.com.restaurante.model.Usuario"%>
<%--
    ================================================================
    LOGIN.JSP — TELA DE ENTRADA (v4 — visual alinhado ao Figma)
    ================================================================
    O protótipo Figma mostra três "abas" no topo do formulário:
    Cliente / Funcionário / Gerência. É importante entender o que
    ISSO significa aqui, porque o sistema real não tem 3 formulários
    de login diferentes — existe UM ÚNICO formulário (login + senha)
    e é o AuthController quem descobre o perfil do usuário consultando
    o banco (usuario.perfil), redirecionando para a área certa depois.

    Ou seja: as "abas" do Figma não mudam qual requisição é enviada
    ao servidor — no protótipo elas são só uma conveniência visual
    de demonstração. Reproduzir isso da forma mais HONESTA possível
    (sem fingir uma funcionalidade que não existe) significa: manter
    as 3 abas como atalho de PREENCHIMENTO do campo "Login" com as
    contas de teste já documentadas no projeto (gerente / funcionario
    / usuario — ver seção "CONTAS DE TESTE"), via um pequeno JS que
    só põe texto no campo. Nenhuma lógica de autorização depende
    dessas abas — quem decide o perfil de verdade continua sendo
    exclusivamente o banco de dados, no AuthController.

    O restante da lógica desta página (redirecionamento se já
    logado, exibição de erro/sucesso) é EXATAMENTE a mesma de antes
    — só o HTML/CSS foi redesenhado.
    ================================================================
--%>
<%
Usuario u = (Usuario) session.getAttribute("usuarioLogado");
if (u != null) {
	String d = request.getContextPath();
	if ("GERENTE".equals(u.getPerfil()))
		d += "/app/dashboard";
	else if ("cozinha".equals(u.getFuncao()))
		d += "/app/fila";
	else if ("FUNCIONARIO".equals(u.getPerfil()))
		d += "/app/mesas";
	else
		d += "/app/cardapio";
	response.sendRedirect(d);
	return;
}
String erro = (String) request.getAttribute("erro");
String sucesso = (String) session.getAttribute("sucesso");
session.removeAttribute("sucesso");
%>
<!DOCTYPE html>
<html lang="pt-BR">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>Login — Integrador</title>
<style>
* {
	margin: 0;
	padding: 0;
	box-sizing: border-box
}

body {
	font-family: 'Segoe UI', system-ui, sans-serif;
	background: #0b0e14;
	min-height: 100vh;
	display: flex;
	justify-content: center;
	align-items: center;
	padding: 20px;
	color: #f1f5f9
}

.card {
	background: #161b25;
	border: 1px solid rgba(255, 255, 255, .08);
	border-radius: 16px;
	box-shadow: 0 12px 40px rgba(0, 0, 0, .5);
	width: 100%;
	max-width: 400px;
	padding: 36px
}

.logo {
	text-align: center;
	margin-bottom: 24px
}

.logo .icon {
	width: 56px;
	height: 56px;
	border-radius: 14px;
	background: #22c55e;
	font-size: 26px;
	display: flex;
	align-items: center;
	justify-content: center;
	margin: 0 auto 12px
}

.logo h1 {
	font-size: 22px;
	font-weight: 700
}

.logo p {
	color: #94a3b8;
	font-size: 13px;
	margin-top: 2px
}

/* Abas de perfil — só preenchem o campo "login", não mudam a
   requisição enviada; ver explicação no comentário JSP acima. */
.tabs-perfil {
	display: grid;
	grid-template-columns: repeat(3, 1fr);
	gap: 8px;
	margin-bottom: 24px
}

.tab-perfil {
	padding: 10px 6px;
	border-radius: 9px;
	border: 1.5px solid rgba(255, 255, 255, .12);
	background: #0f131a;
	color: #94a3b8;
	font-size: 12px;
	font-weight: 600;
	text-align: center;
	cursor: pointer;
	transition: all .2s;
	display: flex;
	flex-direction: column;
	align-items: center;
	gap: 4px
}

.tab-perfil .ti {
	font-size: 16px
}

.tab-perfil:hover {
	border-color: rgba(255, 255, 255, .3)
}

.tab-perfil.ativo {
	background: #7c6a35;
	border-color: #7c6a35;
	color: #f1e9c9
}

.alert {
	padding: 12px 14px;
	border-radius: 9px;
	margin-bottom: 18px;
	font-size: 13px;
	display: flex;
	align-items: center;
	gap: 8px
}

.alert-error {
	background: rgba(239, 68, 68, .15);
	color: #fca5a5;
	border: 1px solid rgba(239, 68, 68, .35)
}

.alert-success {
	background: rgba(34, 197, 94, .15);
	color: #86efac;
	border: 1px solid rgba(34, 197, 94, .35)
}

.form-group {
	margin-bottom: 16px
}

.form-group label {
	display: block;
	font-size: 12px;
	font-weight: 700;
	text-transform: uppercase;
	letter-spacing: .4px;
	color: #94a3b8;
	margin-bottom: 7px
}

.form-group input {
	width: 100%;
	padding: 11px 13px;
	border: 1.5px solid rgba(255, 255, 255, .14);
	border-radius: 9px;
	font-size: 14px;
	font-family: inherit;
	color: #f1f5f9;
	background: #0f131a;
	transition: border-color .2s
}

.form-group input::placeholder {
	color: #64748b
}

.form-group input:focus {
	outline: none;
	border-color: #22c55e;
	box-shadow: 0 0 0 3px rgba(34, 197, 94, .15)
}

.btn-login {
	width: 100%;
	padding: 13px;
	background: #22c55e;
	color: #06240f;
	border: none;
	border-radius: 9px;
	font-size: 15px;
	font-weight: 700;
	cursor: pointer;
	transition: all .2s;
	font-family: inherit;
	margin-top: 6px
}

.btn-login:hover {
	background: #16a34a;
	transform: translateY(-1px)
}

.link-cadastro {
	text-align: center;
	margin-top: 18px;
	font-size: 13px;
	color: #94a3b8
}

.link-cadastro a {
	color: #22c55e;
	font-weight: 600;
	text-decoration: none
}

.hint {
	text-align: center;
	margin-top: 16px;
	padding-top: 16px;
	border-top: 1px solid rgba(255, 255, 255, .08);
	font-size: 11px;
	color: #64748b;
	line-height: 1.8
}

.hint strong {
	color: #94a3b8
}
</style>
</head>
<body>
	<div class="card">

		<div class="logo">
			<div class="icon">🍽️</div>
			<h1>Integrador</h1>
			<p>Sistema de Gestão de Pedidos</p>
		</div>

		<%-- Seletor de perfil — apenas preenche o campo "login" com uma
       conta de teste; a etapa que realmente decide o perfil é o
       AuthController consultando o banco no POST do formulário. --%>
		<div class="tabs-perfil">
			<div class="tab-perfil ativo"
				onclick="selecionarPerfil(this,'usuario')">
				<span class="ti">👤</span>Cliente
			</div>
			<div class="tab-perfil"
				onclick="selecionarPerfil(this,'funcionario')">
				<span class="ti">👥</span>Funcionário
			</div>
			<div class="tab-perfil" onclick="selecionarPerfil(this,'gerente')">
				<span class="ti">🏢</span>Gerência
			</div>
		</div>

		<%
		if (erro != null && !erro.isEmpty()) {
		%><div class="alert alert-error">
			<span>✕</span><span><%=erro%></span>
		</div>
		<%}%>
		<%
		if (sucesso != null && !sucesso.isEmpty()) {
		%><div
			class="alert alert-success">
			<span>✓</span><span><%=sucesso%></span>
		</div>
		<%}%>

		<form method="POST"
			action="${pageContext.request.contextPath}/auth/login">
			<div class="form-group">
				<label for="login">Login</label> <input type="text" id="login"
					name="login" placeholder="Ex: gerente" required autofocus
					autocomplete="username">
			</div>
			<div class="form-group">
				<label for="senha">Senha</label> <input type="password" id="senha"
					name="senha" placeholder="••••••••" required
					autocomplete="current-password">
			</div>
			<button type="submit" class="btn-login">Entrar</button>
		</form>

		<div class="link-cadastro">
			Não tem conta? <a
				href="${pageContext.request.contextPath}/auth/cadastro">Criar
				conta</a>
		</div>

		<div class="hint">
			<p>
				Credenciais de teste — senha: <strong>integrador123</strong>
			</p>
			<p>
				Gerente: <strong>gerente</strong> | Atendente: <strong>funcionario</strong>
				| Cozinha: <strong>cozinha</strong>
			</p>
		</div>
	</div>

	<script>
		// Mapa perfil de teste → login correspondente (mesmas contas
		// documentadas no projeto / criadas pelo integrador_v3.sql).
		var LOGIN_POR_PERFIL = {
			usuario : 'usuario',
			funcionario : 'funcionario',
			gerente : 'gerente'
		};

		function selecionarPerfil(el, perfil) {
			document.querySelectorAll('.tab-perfil').forEach(function(t) {
				t.classList.remove('ativo');
			});
			el.classList.add('ativo');

			// Só preenche o campo — não envia nada, não afeta permissão.
			// O usuário ainda precisa digitar a senha normalmente.
			var loginInput = document.getElementById('login');
			loginInput.value = LOGIN_POR_PERFIL[perfil] || '';
			document.getElementById('senha').focus();
		}
	</script>
</body>
</html>
