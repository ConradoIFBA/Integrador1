package br.com.restaurante;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.mindrot.jbcrypt.BCrypt;

import br.com.restaurante.utils.Conexao;

/**
 * ================================================================
 * TESTELOGIN — UTILITÁRIO DE DIAGNÓSTICO E CORREÇÃO DE SENHAS
 * ================================================================
 *
 * NÃO é parte da aplicação web (não é Servlet, não tem rota, não
 * roda dentro do Tomcat) — é uma classe standalone com main(),
 * pensada para ser executada manualmente pelo desenvolvedor direto
 * no Eclipse (botão direito no arquivo → Run As → Java Application)
 * sempre que o login parar de funcionar depois de (re)importar o
 * banco de dados.
 *
 * POR QUE ELA EXISTE — O PROBLEMA QUE RESOLVE:
 *   O script integrador_v3.sql insere os usuários de teste
 *   (gerente, funcionario, cozinha, usuario) com a senha já
 *   preenchida como a STRING LITERAL "$2a$12$placeholder" — ou
 *   seja, um valor que PARECE um hash BCrypt (mesmo prefixo
 *   "$2a$12$") mas na verdade não é um hash de nada, é só texto
 *   fixo. Isso é proposital: gerar um hash BCrypt de verdade
 *   dentro de um arquivo .sql estático não é prático (o hash muda a
 *   cada execução por causa do salt aleatório), então o script deixa
 *   um placeholder e delega a geração do hash real para esta
 *   classe, que roda DEPOIS da importação do banco.
 *
 * O QUE ESTE PROGRAMA FAZ, PASSO A PASSO:
 *   1. Testa se consegue abrir conexão com o banco (via
 *      Conexao.getConnection()) — primeira coisa a falhar se o
 *      MySQL estiver desligado ou o db.properties estiver errado.
 *   2. Lista TODOS os usuários da tabela `usuario`, imprimindo
 *      id/login/perfil/função/ativo de cada um no console — útil
 *      para conferir visualmente se a importação do banco trouxe os
 *      4 usuários de teste esperados.
 *   3. Para CADA usuário, tenta validar se a senha de teste
 *      ("integrador123") bate com o hash armazenado, usando
 *      BCrypt.checkpw(senhaEmTextoPlano, hashArmazenado) — a mesma
 *      função que o AuthController usa de verdade no login.
 *   4. Se o hash NÃO bater (o caso normal logo após importar o
 *      banco, já que o valor lá é só "$2a$12$placeholder"), gera um
 *      hash BCrypt de verdade para "integrador123"
 *      (BCrypt.hashpw com custo 12 — o mesmo custo usado no
 *      cadastro de novos usuários pelo AuthController, garantindo
 *      consistência) e faz um UPDATE no banco para aquele usuário
 *      específico, substituindo o placeholder pelo hash real.
 *
 * RESULTADO PRÁTICO:
 *   Depois de rodar esta classe uma vez, os 4 logins de teste
 *   (gerente / funcionario / cozinha / usuario) passam todos a
 *   aceitar a senha "integrador123" — sem precisar editar o banco
 *   manualmente nem escrever hashes à mão.
 *
 * QUANDO RODAR:
 *   - Depois de importar/reimportar o integrador_v3.sql (ou a
 *     versão corrigida) do zero.
 *   - Se o login passar a falhar sem motivo aparente e você
 *     suspeitar que o hash no banco não é mais válido (por exemplo,
 *     depois de restaurar um backup antigo do banco).
 *   NÃO precisa rodar toda vez que o Tomcat reinicia — os hashes
 *   ficam gravados no banco, não se perdem entre reinicializações
 *   do servidor.
 *
 * ⚠️ ESTE PROGRAMA ALTERA O BANCO DE DADOS:
 *   Ele executa UPDATE nas linhas cujo hash não bate com a senha de
 *   teste. Em um ambiente de desenvolvimento isso é exatamente o
 *   comportamento desejado, mas não é o tipo de utilitário que
 *   deveria existir/rodar em produção (lá as senhas seriam reais,
 *   escolhidas por cada usuário, e sobrescrevê-las por um hash fixo
 *   de "integrador123" seria uma falha de segurança grave).
 *
 * @author Sistema Integrador
 * @see br.com.restaurante.controller.AuthController
 * @see br.com.restaurante.utils.Conexao
 */
public class TesteLogin {

    // Senha de teste "oficial" de todas as contas do ambiente
    // acadêmico — combina com o que está documentado no README/
    // prompt de continuação do projeto ("Senha padrão: integrador123").
    private static final String SENHA_TESTE = "integrador123";

    /**
     * Ponto de entrada — roda como aplicação Java comum, fora do
     * Tomcat. Qualquer exceção não tratada aqui simplesmente sobe e
     * é impressa no console pela própria JVM (daí o "throws
     * Exception" na assinatura em vez de um catch genérico).
     */
    public static void main(String[] args) throws Exception {

        // ── 1. Conexão ──────────────────────────────────────────────
        // Primeiro teste: será que dá pra sequer FALAR com o banco?
        // Se Conexao.getConnection() lançar exceção aqui, cai direto
        // no catch lá embaixo, que já vem com um checklist de causas
        // prováveis (MySQL desligado, credenciais erradas, banco
        // inexistente).
        System.out.println("=== TESTE DE LOGIN ===\n");
        System.out.print("[1] Testando conexão com o banco... ");

        try (Connection conn = Conexao.getConnection()) {
            // getMetaData().getURL() devolve a URL JDBC efetivamente
            // usada — impresso aqui como confirmação visual de que a
            // conexão foi para o banco certo (útil se houver mais de
            // um MySQL rodando na máquina, ou mais de um
            // db.properties por engano no classpath).
            System.out.println("OK (" + conn.getMetaData().getURL() + ")\n");

            // ── 2. Buscar usuários ───────────────────────────────────
            // SELECT * simplificado nos campos relevantes — traz
            // TODOS os usuários (sem filtro de "ativo"), já que o
            // objetivo aqui é diagnóstico, não uma tela de produção.
            System.out.println("[2] Usuários na tabela:");
            String sql = "SELECT id_usuario, nome, login, senha, perfil, funcao, ativo FROM usuario";

            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {

                // Flag usada só para decidir a mensagem final
                // (resumo "tudo certo" vs "corrigi alguma coisa,
                // reinicie o Tomcat").
                boolean algumHashInvalido = false;

                // Percorre linha a linha o ResultSet — rs.next()
                // avança o cursor e retorna false quando acabam as
                // linhas, encerrando o while naturalmente.
                while (rs.next()) {
                    int    id      = rs.getInt("id_usuario");
                    String nome    = rs.getString("nome");
                    String login   = rs.getString("login");
                    String hash    = rs.getString("senha");
                    String perfil  = rs.getString("perfil");
                    String funcao  = rs.getString("funcao");
                    boolean ativo  = rs.getBoolean("ativo");

                    // Impressão formatada em colunas (printf com
                    // largura fixa por campo) só para facilitar a
                    // leitura no console — puramente cosmético.
                    System.out.printf("  id=%-3d  login=%-15s  perfil=%-12s  funcao=%-10s  ativo=%s%n",
                            id, login, perfil, (funcao != null ? funcao : "-"), ativo);

                    // ── 3. Verificar hash ────────────────────────────
                    // BCrypt.checkpw faz o trabalho pesado: extrai o
                    // salt embutido no próprio hash armazenado,
                    // recalcula o hash da senha em texto plano usando
                    // esse mesmo salt, e compara os resultados. Não
                    // dá pra simplesmente comparar strings porque
                    // BCrypt gera um salt diferente a cada hashpw()
                    // — daí dois hashes da MESMA senha nunca serem
                    // idênticos entre si, mas checkpw ainda assim
                    // reconhece ambos como válidos.
                    boolean hashOk = false;
                    try {
                        hashOk = BCrypt.checkpw(SENHA_TESTE, hash);
                    } catch (Exception e) {
                        // BCrypt.checkpw lança exceção (em vez de
                        // retornar false) quando o "hash" fornecido
                        // nem sequer tem o formato esperado — é
                        // exatamente o que acontece com o valor
                        // placeholder "$2a$12$placeholder" do script
                        // SQL, que tem a aparência de um hash BCrypt
                        // mas não segue a estrutura interna real.
                        // Por isso este catch é esperado e normal na
                        // primeira execução após importar o banco.
                        System.out.println("    ⚠ Hash inválido ou corrompido: " + hash);
                    }

                    if (hashOk) {
                        // Hash já é válido e bate com a senha de
                        // teste — nada a fazer para este usuário.
                        System.out.println("    ✅ Senha '" + SENHA_TESTE + "' BATE com o hash.");
                    } else {
                        // Hash ausente/inválido/não confere — este é
                        // o caminho que efetivamente CORRIGE o banco.
                        System.out.println("    ❌ Senha '" + SENHA_TESTE + "' NÃO bate. Corrigindo...");
                        algumHashInvalido = true;

                        // Gera um hash BCrypt de verdade para a senha
                        // de teste. gensalt(12) define o "cost
                        // factor" (fator de trabalho) em 12 — quanto
                        // maior, mais lento (e mais seguro contra
                        // força bruta) fica o cálculo; 12 é o mesmo
                        // valor usado pelo AuthController no cadastro
                        // de usuários reais, garantindo que as contas
                        // de teste fiquem no mesmo padrão de
                        // segurança das contas "de verdade".
                        String novoHash = BCrypt.hashpw(SENHA_TESTE, BCrypt.gensalt(12));

                        // UPDATE pontual, só na linha deste usuário
                        // específico (WHERE id_usuario = ?) — não
                        // mexe nos outros campos (nome, login,
                        // perfil, etc.), só na senha.
                        try (PreparedStatement upd = conn.prepareStatement(
                                "UPDATE usuario SET senha = ? WHERE id_usuario = ?")) {
                            upd.setString(1, novoHash);
                            upd.setInt(2, id);
                            upd.executeUpdate();
                        }
                        System.out.println("    ✅ Hash atualizado no banco para o usuário '" + login + "'.");
                    }
                }

                // ── Resumo final ─────────────────────────────────────
                System.out.println();
                if (algumHashInvalido) {
                    // Pelo menos um usuário teve o hash corrigido —
                    // avisa para reiniciar o Tomcat só por precaução
                    // (não é estritamente necessário, já que a senha
                    // é lida do banco a cada tentativa de login, mas
                    // evita qualquer dúvida sobre cache).
                    System.out.println("⚠  Alguns hashes estavam inválidos e foram corrigidos.");
                    System.out.println("   Reinicie o Tomcat e tente logar novamente.");
                } else {
                    // Todos os hashes já estavam corretos — se o
                    // login ainda assim falhar, o problema está em
                    // outro lugar (não é mais sobre a senha em si).
                    System.out.println("✅ Todos os hashes estão corretos.");
                    System.out.println("   Se o login ainda falhar, verifique se o Tomcat");
                    System.out.println("   está lendo o db.properties correto.");
                }
            }

        } catch (Exception e) {
            // Chegou aqui = falhou já na conexão inicial (passo 1),
            // então nem chegou a testar usuário nenhum. O checklist
            // impresso cobre as causas mais comuns nesse cenário,
            // na ordem em que vale a pena checar.
            System.out.println("FALHOU!\n");
            System.out.println("Erro: " + e.getMessage());
            System.out.println("\nVerifique:");
            System.out.println("  - MySQL está rodando?");
            System.out.println("  - db.properties tem o usuário/senha corretos?");
            System.out.println("  - O banco 'integrador' existe? (rode o integrador.sql)");
            e.printStackTrace();
        }
    }
}
