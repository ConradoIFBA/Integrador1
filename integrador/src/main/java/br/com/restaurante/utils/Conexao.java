package br.com.restaurante.utils;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Fornece conexões JDBC lendo as configurações de db.properties.
 *
 * Uso padrão (try-with-resources):
 *   try (Connection conn = Conexao.getConnection()) {
 *       // usar conn
 *   }
 */
public class Conexao {

    private static String url;
    private static String usuario;
    private static String senha;

    static {
        try (InputStream in = Conexao.class
                .getClassLoader()
                .getResourceAsStream("db.properties")) {

            if (in == null) {
                throw new ExceptionInInitializerError(
                    "db.properties não encontrado no classpath.");
            }

            Properties props = new Properties();
            props.load(in);

            url     = props.getProperty("db.url");
            usuario = props.getProperty("db.usuario");
            senha   = props.getProperty("db.senha");

            Class.forName("com.mysql.cj.jdbc.Driver");

        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /** Retorna uma nova conexão com o banco. Sempre fechar com try-with-resources. */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, usuario, senha);
    }
}
