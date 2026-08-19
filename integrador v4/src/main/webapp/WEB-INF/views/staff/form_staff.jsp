<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@ page import="br.com.restaurante.model.Usuario"%>
<%@ taglib prefix="fn" uri="jakarta.tags.functions"%>
<%--
    ================================================================
    FORM_STAFF.JSP — NOVA CONTA DE FUNCIONÁRIO/GERENTE (só GERENTE)
    ================================================================
    Formulário simples: nome, login, senha (com confirmação), perfil
    (GERENTE/FUNCIONARIO — nunca USUARIO, essa opção nem aparece
    aqui) e, só quando perfil=FUNCIONARIO, um campo extra de função
    (atendente/cozinha) que aparece/some via JS conforme o perfil
    escolhido.

    A validação "perfil só pode ser GERENTE ou FUNCIONARIO" é
    reforçada NO SERVIDOR (UsuarioController.salvar()) mesmo que o
    <select> abaixo só ofereça essas duas opções — nunca confiar só
    na validação do HTML.
    ================================================================
--%>
<%
Usuario _u = (Usuario) session.getAttribute("usuarioLogado");
String erro = (String) request.getAttribute("erro");
%>
<!DOCTYPE html>
<html lang="pt-BR">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>Nova Conta — Integrador</title>
<link rel="stylesheet"
	href="${pageContext.request.contextPath}/assets/style.css">
<style>
.form-card {
	max-width: 520px
}

.form-row {
	display: grid;
	grid-template-columns: 1fr 1fr;
	gap: 16px
}

.form-footer {
	display: flex;
	gap: 12px;
	margin-top: 8px
}

.form-footer .btn {
	flex: 1;
	padding: 12px;
	font-size: 15px
}
</style>
</head>
<body>
	<div class="main-container">
		<%@ include file="/WEB-INF/views/shared/_sidebar.jsp"%>
		<div class="main-content">
			<header class="topbar">
				<div class="topbar-left">
					<h2>Nova Conta de Funcionário</h2>
				</div>
				<div class="topbar-right">
					<a href="${pageContext.request.contextPath}/app/staff"
						class="btn btn-secondary btn-sm">← Voltar</a>
				</div>
			</header>
			<main class="content">
				<div class="card form-card">

					<%
					if (erro != null && !erro.isEmpty()) {
					%>
					<div class="alert alert-error">
						✕
						<%=erro%></div>
					<%
					}
					%>

					<form method="POST"
						action="${pageContext.request.contextPath}/app/staff"
						onsubmit="return validar()">
						<input type="hidden" name="acao" value="salvar">

						<div class="form-group">
							<label for="nome">Nome completo <span
								style="color: var(--error-color)">*</span></label> <input type="text"
								id="nome" name="nome" placeholder="Ex: João Pereira"
								value="${fn:escapeXml(param.nome)}" required maxlength="100">
						</div>

						<div class="form-group">
							<label for="login">Login <span
								style="color: var(--error-color)">*</span></label> <input type="text"
								id="login" name="login" placeholder="Ex: joao.cozinha"
								value="${fn:escapeXml(param.login)}" required minlength="3"
								maxlength="50"
								onkeydown="if(event.key===' ')event.preventDefault()">
						</div>

						<div class="form-row">
							<div class="form-group">
								<label for="senha">Senha <span
									style="color: var(--error-color)">*</span></label> <input
									type="password" id="senha" name="senha" placeholder="••••••••"
									required minlength="6" oninput="validarSenhas()">
							</div>
							<div class="form-group">
								<label for="confirmarSenha">Confirmar senha <span
									style="color: var(--error-color)">*</span></label> <input
									type="password" id="confirmarSenha" name="confirmarSenha"
									placeholder="••••••••" required minlength="6"
									oninput="validarSenhas()">
							</div>
						</div>

						<div class="form-group">
							<label for="perfil">Perfil <span
								style="color: var(--error-color)">*</span></label> <select id="perfil"
								name="perfil" required onchange="atualizarFuncao()">
								<option value="FUNCIONARIO">Funcionário</option>
								<option value="GERENTE">Gerente</option>
							</select>
						</div>

						<div class="form-group" id="grupoFuncao">
							<label for="funcao">Função (opcional)</label> <select id="funcao"
								name="funcao">
								<option value="">Não especificar</option>
								<option value="atendente">Atendente</option>
								<option value="cozinha">Cozinha</option>
							</select>
							<div
								style="font-size: 11px; color: var(--text-muted); margin-top: 5px">
								Informativo — não muda as permissões do sistema, que hoje são
								unificadas para o perfil Funcionário.</div>
						</div>

						<div class="form-footer">
							<a href="${pageContext.request.contextPath}/app/staff"
								class="btn btn-secondary">Cancelar</a>
							<button type="submit" class="btn btn-primary">Criar
								conta</button>
						</div>
					</form>
				</div>
			</main>
		</div>
	</div>
	<script>
		function atualizarFuncao() {
			var perfil = document.getElementById('perfil').value;
			document.getElementById('grupoFuncao').style.display = (perfil === 'FUNCIONARIO') ? ''
					: 'none';
		}
		function validarSenhas() {
			var s1 = document.getElementById('senha').value;
			var s2 = document.getElementById('confirmarSenha').value;
			var campo = document.getElementById('confirmarSenha');
			campo.style.borderColor = (s2.length > 0) ? (s1 === s2 ? 'var(--success-color)'
					: 'var(--error-color)')
					: '';
		}
		function validar() {
			if (document.getElementById('senha').value !== document
					.getElementById('confirmarSenha').value) {
				alert('As senhas não coincidem.');
				return false;
			}
			return true;
		}
		atualizarFuncao();
	</script>
</body>
</html>
