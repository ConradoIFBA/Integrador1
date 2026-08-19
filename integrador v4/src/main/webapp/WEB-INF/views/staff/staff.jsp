<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@ page import="br.com.restaurante.model.Usuario"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>
<%--
    ================================================================
    STAFF.JSP — GERENCIAR FUNCIONÁRIOS/GERENTES (v1, só GERENTE)
    ================================================================
    Lista as contas de perfil GERENTE/FUNCIONARIO já cadastradas
    (nunca mostra contas USUARIO — essa tela não gerencia clientes,
    só o quadro de funcionários administrativo). Cada linha tem um
    botão "Desativar" — protegido no Controller contra auto-
    desativação e contra desativar o último gerente ativo.
    ================================================================
--%>
<%
    Usuario _u = (Usuario) session.getAttribute("usuarioLogado");
    // _u é uma variável de SCRIPTLET — o EL (${...}) não enxerga
    // variáveis Java soltas, só atributos de escopo. Por isso
    // publicamos o id como atributo de página antes de comparar
    // ${s.idUsuario == meuId} lá embaixo (mesmo ajuste já feito
    // antes no dashboard.jsp, pelo mesmo motivo).
    pageContext.setAttribute("meuId", _u.getIdUsuario());
%>
<!DOCTYPE html>
<html lang="pt-BR">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>Funcionários — Integrador</title>
<link rel="stylesheet"
	href="${pageContext.request.contextPath}/assets/style.css">
</head>
<body>
	<div class="main-container">
		<%@ include file="/WEB-INF/views/shared/_sidebar.jsp"%>
		<div class="main-content">
			<header class="topbar">
				<div class="topbar-left">
					<h2>👥 Funcionários</h2>
				</div>
				<div class="topbar-right">
					<a href="${pageContext.request.contextPath}/app/staff?acao=novo"
						class="btn btn-primary">+ Nova Conta</a>
				</div>
			</header>
			<main class="content">

				<c:if test="${not empty msgSucesso}">
					<div class="alert alert-success">✓ ${msgSucesso}</div>
				</c:if>
				<c:if test="${not empty msgErro}">
					<div class="alert alert-error">✕ ${msgErro}</div>
				</c:if>

				<div class="card">
					<h3>Contas de Gerência e Funcionários</h3>
					<c:choose>
						<c:when test="${empty staff}">
							<div class="empty-state">
								<div class="icon">👥</div>
								<p>Nenhuma conta cadastrada ainda.</p>
							</div>
						</c:when>
						<c:otherwise>
							<div class="table-wrapper">
								<table>
									<thead>
										<tr>
											<th>Nome</th>
											<th>Login</th>
											<th>Perfil</th>
											<th>Função</th>
											<th></th>
										</tr>
									</thead>
									<tbody>
										<c:forEach var="s" items="${staff}">
											<tr>
												<td><strong>${s.nome}</strong></td>
												<td class="text-muted">${s.login}</td>
												<td><c:choose>
														<c:when test="${s.perfil=='GERENTE'}">
															<span class="badge badge-roxo">Gerente</span>
														</c:when>
														<c:otherwise>
															<span class="badge badge-info">Funcionário</span>
														</c:otherwise>
													</c:choose></td>
												<td class="text-muted"><c:choose>
														<c:when test="${s.funcao=='atendente'}">Atendente</c:when>
														<c:when test="${s.funcao=='cozinha'}">Cozinha</c:when>
														<c:otherwise>—</c:otherwise>
													</c:choose></td>
												<td><c:choose>
														<c:when test="${s.idUsuario == meuId}">
															<span class="text-muted" style="font-size: 12px">(você)</span>
														</c:when>
														<c:otherwise>
															<form method="POST"
																action="${pageContext.request.contextPath}/app/staff"
																onsubmit="return confirm('Desativar a conta de ${s.nome}? Ela não conseguirá mais fazer login.')">
																<input type="hidden" name="acao" value="desativar">
																<input type="hidden" name="id" value="${s.idUsuario}">
																<button type="submit" class="btn-icon danger"
																	title="Desativar">🗑️</button>
															</form>
														</c:otherwise>
													</c:choose></td>
											</tr>
										</c:forEach>
									</tbody>
								</table>
							</div>
						</c:otherwise>
					</c:choose>
				</div>

			</main>
		</div>
	</div>
</body>
</html>
