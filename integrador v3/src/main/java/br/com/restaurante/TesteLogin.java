package br.com.restaurante;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.mindrot.jbcrypt.BCrypt;

import br.com.restaurante.utils.Conexao;

/**
 * Roda como Java Application (botão direito → Run As → Java Application).
 *
 * Faz três verificações:
 *  1. Conexão com o banco
 *  2. Se os usuários existem na tabela
 *  3. Se o hash armazenado bate com "integrador123"
 *
 * Se o hash não bater, gera hashes novos e atualiza o banco automaticamente.
 */
public class TesteLogin {

    private static final String SENHA_TESTE = "integrador123";

    public static void main(String[] args) throws Exception {

        // ── 1. Conexão ──────────────────────────────────────────────
        System.out.println("=== TESTE DE LOGIN ===\n");
        System.out.print("[1] Testando conexão com o banco... ");

        try (Connection conn = Conexao.getConnection()) {
            System.out.println("OK (" + conn.getMetaData().getURL() + ")\n");

            // ── 2. Buscar usuários ───────────────────────────────────
            System.out.println("[2] Usuários na tabela:");
            String sql = "SELECT id_usuario, nome, login, senha, perfil, funcao, ativo FROM usuario";

            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {

                boolean algumHashInvalido = false;

                while (rs.next()) {
                    int    id      = rs.getInt("id_usuario");
                    String nome    = rs.getString("nome");
                    String login   = rs.getString("login");
                    String hash    = rs.getString("senha");
                    String perfil  = rs.getString("perfil");
                    String funcao  = rs.getString("funcao");
                    boolean ativo  = rs.getBoolean("ativo");

                    System.out.printf("  id=%-3d  login=%-15s  perfil=%-12s  funcao=%-10s  ativo=%s%n",
                            id, login, perfil, (funcao != null ? funcao : "-"), ativo);

                    // ── 3. Verificar hash ────────────────────────────
                    boolean hashOk = false;
                    try {
                        hashOk = BCrypt.checkpw(SENHA_TESTE, hash);
                    } catch (Exception e) {
                        System.out.println("    ⚠ Hash inválido ou corrompido: " + hash);
                    }

                    if (hashOk) {
                        System.out.println("    ✅ Senha '" + SENHA_TESTE + "' BATE com o hash.");
                    } else {
                        System.out.println("    ❌ Senha '" + SENHA_TESTE + "' NÃO bate. Corrigindo...");
                        algumHashInvalido = true;

                        // Gera hash correto e atualiza o banco
                        String novoHash = BCrypt.hashpw(SENHA_TESTE, BCrypt.gensalt(12));
                        try (PreparedStatement upd = conn.prepareStatement(
                                "UPDATE usuario SET senha = ? WHERE id_usuario = ?")) {
                            upd.setString(1, novoHash);
                            upd.setInt(2, id);
                            upd.executeUpdate();
                        }
                        System.out.println("    ✅ Hash atualizado no banco para o usuário '" + login + "'.");
                    }
                }

                System.out.println();
                if (algumHashInvalido) {
                    System.out.println("⚠  Alguns hashes estavam inválidos e foram corrigidos.");
                    System.out.println("   Reinicie o Tomcat e tente logar novamente.");
                } else {
                    System.out.println("✅ Todos os hashes estão corretos.");
                    System.out.println("   Se o login ainda falhar, verifique se o Tomcat");
                    System.out.println("   está lendo o db.properties correto.");
                }
            }

        } catch (Exception e) {
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
