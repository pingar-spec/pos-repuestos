package com.pos.repuestos;

import java.sql.Connection;
import java.sql.Statement;

public class Migracion {
    public static void main(String[] args) {
        String sql = "CREATE TABLE IF NOT EXISTS arqueo_caja (" +
                     "  id_arqueo INT AUTO_INCREMENT PRIMARY KEY," +
                     "  fecha_apertura DATETIME DEFAULT CURRENT_TIMESTAMP," +
                     "  monto_inicial DECIMAL(10,2) NOT NULL," +
                     "  fecha_cierre DATETIME NULL," +
                     "  monto_final DECIMAL(10,2) NULL," +
                     "  estado VARCHAR(20) DEFAULT 'ABIERTA'" +
                     ");";

        try (Connection con = ConexionDB.getConexion();
             Statement st = con.createStatement()) {
            st.execute(sql);
            System.out.println(">>> TABLA arqueo_caja CREADA / VERIFICADA CON ÉXITO <<<");
        } catch (Exception e) {
            System.err.println("Error creando tabla: " + e.getMessage());
            e.printStackTrace();
        }
    }
}