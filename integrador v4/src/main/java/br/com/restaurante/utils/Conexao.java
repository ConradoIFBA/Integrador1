package br.com.restaurante.utils;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * ================================================================
 * CONEXAO — FÁBRICA DE CONEXÕES JDBC
 * ================================================================
 *
 * Classe utilitária responsável por abrir conexões com o MySQL.
 * É o ÚNICO ponto do sistema que sabe onde está o banco (URL,
 * usuário, senha) e qual driver usar — todos os DAOs recebem a
 * Connection já pronta a partir daqui, nunca montam a string de
 * conexão sozinhos.
 *
 * COMO FUNCIONA (visão geral):
 *   1. Na primeira vez que ALGUÉM referencia esta classe, o bloco
 *      `static { ... }` roda automaticamente UMA ÚNICA VEZ durante
 *      o carregamento da classe pela JVM (não a cada chamada).
 *   2. Esse bloco lê o arquivo db.properties (que fica em
 *      src/main/resources, e por isso vai parar em WEB-INF/classes
 *      dentro do WAR) e guarda url/usuario/senha em campos static.
 *   3. Também registra o driver JDBC do MySQL
 *      (com.mysql.cj.jdbc.Driver) via Class.forName — necessário
 *      para que o DriverManager saiba "falar" com MySQL.
 *   4. Cada chamada a getConnection() depois disso apenas pede uma
 *      conexão NOVA ao DriverManager usando esses dados já
 *      carregados.
 *
 * ⚠️ IMPORTANTE — NÃO HÁ POOL DE CONEXÕES:
 *   getConnection() abre uma conexão TCP nova com o MySQL a cada
 *   chamada. Isso é aceitável para um projeto acadêmico com poucos
 *   usuários simultâneos, mas NÃO seria adequado em produção — lá
 *   se usaria um pool (HikariCP, Tomcat JDBC Pool, etc.) para
 *   reaproveitar conexões e evitar o custo de abrir/fechar TCP toda
 *   hora. Por isso todo o resto do sistema segue rigorosamente o
 *   padrão try-with-resources ao usar Connection — é o que garante
 *   que cada conexão aberta aqui seja sempre fechada logo em
 *   seguida, mesmo se der exceção no meio do caminho.
 *
 * DE ONDE VÊM OS DADOS (db.properties):
 *   Arquivo texto simples com três chaves esperadas:
 *     db.url     → ex: jdbc:mysql://localhost:3306/integrador?...
 *     db.usuario → ex: root
 *     db.senha   → senha do MySQL local (pode ficar vazia se o
 *                  usuário do MySQL não tiver senha configurada)
 *   Fica em src/main/resources/db.properties e é lido do CLASSPATH
 *   (getResourceAsStream), não do disco — por isso funciona igual
 *   tanto rodando pelo Eclipse quanto empacotado num WAR.
 *
 * ERROS NA INICIALIZAÇÃO:
 *   Se o arquivo não existir, o driver não estiver no classpath, ou
 *   qualquer outra coisa falhar dentro do bloco static, a classe
 *   lança ExceptionInInitializerError — um erro "fatal" de
 *   carregamento de classe. Na prática isso aparece no console do
 *   Tomcat como uma cadeia de causas (Caused by...) apontando para
 *   o motivo real (FileNotFoundException, ClassNotFoundException
 *   do driver, etc.) — é sempre o primeiro lugar a olhar se a
 *   aplicação inteira não sobe.
 *
 * @author Sistema Integrador
 * @version 3.0
 * @see br.com.restaurante.dao
 */
public class Conexao {

    // ── DADOS DE CONEXÃO (carregados uma única vez, no static{}) ──
    // Ficam static porque são os MESMOS para toda a aplicação — não
    // faz sentido guardar url/usuario/senha por instância, já que
    // esta classe nunca é instanciada (só usa o método static
    // getConnection()).
    private static String url, usuario, senha;

    /**
     * BLOCO DE INICIALIZAÇÃO ESTÁTICA
     * ------------------------------------------------------------
     * Roda automaticamente quando a JVM carrega a classe Conexao
     * pela primeira vez (tipicamente na primeira requisição que
     * passa por algum DAO). É aqui — e só aqui — que o
     * db.properties é lido e o driver JDBC é registrado.
     */
    static {
        try (InputStream in = Conexao.class.getClassLoader()
                .getResourceAsStream("db.properties")) {

            // getResourceAsStream retorna null (em vez de lançar
            // exceção) quando o arquivo não é encontrado no
            // classpath — por isso o null-check manual abaixo é
            // obrigatório, senão o erro real ficaria escondido
            // atrás de um NullPointerException confuso no p.load().
            if (in == null) {
                throw new ExceptionInInitializerError(
                        "db.properties não encontrado.");
            }

            Properties p = new Properties();
            p.load(in);

            url     = p.getProperty("db.url");
            usuario = p.getProperty("db.usuario");
            senha   = p.getProperty("db.senha");

            // Registra o driver do MySQL Connector/J no
            // DriverManager. Em versões recentes do JDBC (4.0+)
            // isso costuma acontecer sozinho via META-INF/services,
            // mas manter o Class.forName explícito não faz mal e
            // deixa claro qual driver o projeto espera encontrar
            // no classpath (via Maven, o mysql-connector-j do
            // pom.xml).
            Class.forName("com.mysql.cj.jdbc.Driver");

        } catch (Exception e) {
            // Qualquer falha aqui (arquivo ausente, propriedade
            // faltando, driver não encontrado no classpath, etc.)
            // impede a aplicação de funcionar — por isso é
            // propagada como erro de inicialização de classe em vez
            // de ser engolida silenciosamente.
            throw new ExceptionInInitializerError(e);
        }
    }

    /**
     * Abre e retorna uma NOVA conexão JDBC com o MySQL, usando os
     * dados carregados no bloco static acima.
     *
     * Quem chama este método é responsável por fechar a conexão
     * (idealmente via try-with-resources) assim que terminar de
     * usá-la — esta classe não gerencia o ciclo de vida da conexão
     * depois de entregá-la.
     *
     * @return uma Connection nova e aberta, pronta para uso
     * @throws SQLException se o MySQL estiver fora do ar, as
     *         credenciais estiverem erradas, ou a URL for inválida
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, usuario, senha);
    }
}
