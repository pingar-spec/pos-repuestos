package com.pos.repuestos;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class ConexionDB {
    public static void main(String[] args) {
    String sql = "ALTER TABLE productos ADD COLUMN activo TINYINT(1) DEFAULT 1";
    try (java.sql.Connection con = ConexionDB.getConexion();
         java.sql.Statement st = con.createStatement()) {
        st.executeUpdate(sql);
        System.out.println("Columna 'activo' agregada con exito.");
    } catch (Exception e) {
        System.out.println("Aviso: " + e.getMessage());
    }
}
    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        // Cadena optimizada con cache de sentencias y compresion de datos
        config.setJdbcUrl("jdbc:mysql://gateway01.sa-east-1.prod.aws.tidbcloud.com:4000/pos_repuestos_db"
                + "?useSSL=true"
                + "&sslMode=PREFERRED"
                + "&allowPublicKeyRetrieval=true"
                + "&serverTimezone=UTC"
                + "&cachePrepStmts=true"
                + "&prepStmtCacheSize=250"
                + "&prepStmtCacheSqlLimit=2048"
                + "&useServerPrepStmts=true");

        config.setUsername("VqAp2wi3ep6JvL1.root");
        config.setPassword("wYgdmBp2KH2hG3FM");

        // Configuracion del Pool de conexiones
        config.setMaximumPoolSize(10);       // Maximo 10 conexiones abiertas listas
        config.setMinimumIdle(3);           // Al menos 3 conexiones siempre despiertas
        config.setIdleTimeout(30000);       // 30 segundos de inactividad
        config.setMaxLifetime(600000);      // Renovar conexiones cada 10 min
        config.setConnectionTimeout(10000); // Espera maxima de 10 seg

        dataSource = new HikariDataSource(config);
    }

    public static Connection getConexion() throws SQLException {
        return dataSource.getConnection();
    }

    public static Connection getConnexion() throws SQLException {
        return getConexion();
    }

    public static Connection obtenerConexion() throws SQLException {
        return getConexion();

    }
}