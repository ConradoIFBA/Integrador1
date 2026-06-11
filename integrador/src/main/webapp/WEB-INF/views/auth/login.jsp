<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="br.com.restaurante.model.Usuario" %>
<%
    // Se já está logado, redireciona para a área adequada
    Usuario u = (Usuario) session.getAttribute("usuarioLogado");
    if (u != null) {
        String destino = request.getContextPath();
        if ("GERENTE".equals(u.getPerfil()))          destino += "/app/dashboard";
        else if ("cozinha".equals(u.getFuncao()))     destino += "/app/fila";
        else if ("FUNCIONARIO".equals(u.getPerfil())) destino += "/app/mesas";
        else                                          destino += "/app/cardapio";
        response.sendRedirect(destino);
        return;
    }

    // Mensagens vindas da requisição (forward) ou da sessão (redirect)
    String erro   = (String) request.getAttribute("erro");
    String sucesso = (String) session.getAttribute("sucesso");
    session.removeAttribute("sucesso");
%>
<!DOCTYPE html>
<html lang="pt-BR">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Login — Integrador</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }

        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            background: linear-gradient(135deg, #e85d27 0%, #c94d1e 100%);
            min-height: 100vh;
            display: flex;
            justify-content: center;
            align-items: center;
            padding: 20px;
        }

        .card {
            background: #fff;
            border-radius: 14px;
            box-shadow: 0 12px 40px rgba(0,0,0,.2);
            width: 100%;
            max-width: 400px;
            padding: 40px;
        }

        .logo {
            text-align: center;
            margin-bottom: 28px;
        }
        .logo .icon { font-size: 48px; }
        .logo h1 { font-size: 26px; font-weight: 700; color: #e85d27; margin: 8px 0 4px; }
        .logo p  { color: #64748b; font-size: 13px; }

        .divider { height: 2px; background: #e2e8f0; margin: 0 0 24px; }

        .alert {
            padding: 12px 14px;
            border-radius: 7px;
            margin-bottom: 18px;
            font-size: 14px;
            display: flex;
            align-items: center;
            gap: 8px;
        }
        .alert-error   { background: #fee2e2; color: #991b1b; border-left: 4px solid #ef4444; }
        .alert-success { background: #d1fae5; color: #065f46; border-left: 4px solid #10b981; }

        .form-group { margin-bottom: 18px; }
        .form-group label {
            display: block;
            font-size: 13px;
            font-weight: 600;
            color: #1e293b;
            margin-bottom: 7px;
        }
        .form-group input {
            width: 100%;
            padding: 11px 13px;
            border: 2px solid #e2e8f0;
            border-radius: 7px;
            font-size: 14px;
            font-family: inherit;
            transition: border-color .2s;
            color: #1e293b;
        }
        .form-group input:focus {
            outline: none;
            border-color: #e85d27;
            box-shadow: 0 0 0 3px rgba(232,93,39,.12);
        }
        .form-group input::placeholder { color: #94a3b8; }

        .btn-login {
            width: 100%;
            padding: 12px;
            background: linear-gradient(135deg, #e85d27, #c94d1e);
            color: #fff;
            border: none;
            border-radius: 7px;
            font-size: 15px;
            font-weight: 600;
            cursor: pointer;
            transition: transform .2s, box-shadow .2s;
            font-family: inherit;
            margin-top: 6px;
        }
        .btn-login:hover {
            transform: translateY(-1px);
            box-shadow: 0 6px 16px rgba(232,93,39,.4);
        }

        .hint {
            text-align: center;
            margin-top: 22px;
            font-size: 12px;
            color: #94a3b8;
            line-height: 1.8;
        }
        .hint strong { color: #64748b; }
    </style>
</head>
<body>
<div class="card">

    <div class="logo">
        <div class="icon">🍽️</div>
        <h1>Integrador</h1>
        <p>Sistema de Pedidos para Restaurante</p>
    </div>

    <div class="divider"></div>

    <%-- Mensagem de erro --%>
    <% if (erro != null && !erro.isEmpty()) { %>
        <div class="alert alert-error">
            <span>✕</span> <span><%= erro %></span>
        </div>
    <% } %>

    <%-- Mensagem de sucesso (ex: após logout) --%>
    <% if (sucesso != null && !sucesso.isEmpty()) { %>
        <div class="alert alert-success">
            <span>✓</span> <span><%= sucesso %></span>
        </div>
    <% } %>

    <form method="POST" action="${pageContext.request.contextPath}/auth/login">

        <div class="form-group">
            <label for="login">Login</label>
            <input type="text"
                   id="login"
                   name="login"
                   placeholder="Ex: gerente"
                   required
                   autofocus
                   autocomplete="username">
        </div>

        <div class="form-group">
            <label for="senha">Senha</label>
            <input type="password"
                   id="senha"
                   name="senha"
                   placeholder="••••••••"
                   required
                   autocomplete="current-password">
        </div>

        <button type="submit" class="btn-login">Entrar</button>

    </form>

    <div class="hint">
        <p>Credenciais de teste (senha: <strong>integrador123</strong>)</p>
        <p>Gerente: <strong>gerente</strong> &nbsp;|&nbsp;
           Atendente: <strong>funcionario</strong> &nbsp;|&nbsp;
           Cozinha: <strong>cozinha</strong></p>
    </div>

</div>
</body>
</html>
