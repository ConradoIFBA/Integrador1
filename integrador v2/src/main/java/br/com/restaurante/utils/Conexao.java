package br.com.restaurante.utils;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
public class Conexao {
    private static String url, usuario, senha;
    static {
        try (InputStream in = Conexao.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in == null) throw new ExceptionInInitializerError("db.properties não encontrado.");
            Properties p = new Properties(); p.load(in);
            url = p.getProperty("db.url"); usuario = p.getProperty("db.usuario"); senha = p.getProperty("db.senha");
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (Exception e) { throw new ExceptionInInitializerError(e); }
    }
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, usuario, senha);
    }
}
