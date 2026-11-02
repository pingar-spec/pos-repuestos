package com.pos.repuestos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class CajaDAO {

    // 1. Abrir caja registrando el monto inicial
  public boolean abrirCaja(double montoInicial) {
    String sql = "INSERT INTO arqueo_caja (monto_inicial, estado) VALUES (?, 'ABIERTA')";
    try (Connection con = ConexionDB.getConexion();
         PreparedStatement ps = con.prepareStatement(sql)) {
        ps.setDouble(1, montoInicial);
        int filas = ps.executeUpdate();
        System.out.println("Filas insertadas en caja: " + filas);
        return filas > 0;
    } catch (SQLException e) {
        System.err.println("FALLO EN TiDB: " + e.getMessage());
        e.printStackTrace();
        return false;
    }
}
    // 2. Obtener la caja activa actual (para saber si está abierta y cuál es su ID)
    public Integer obtenerIdCajaAbierta() {
        String sql = "SELECT id_arqueo FROM arqueo_caja WHERE estado = 'ABIERTA' ORDER BY id_arqueo DESC LIMIT 1";
        try (Connection con = ConexionDB.getConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("id_arqueo");
            }
        } catch (SQLException e) {
            System.err.println("Error en obtenerIdCajaAbierta: " + e.getMessage());
        }
        return null;
    }

    // 3. Cerrar caja con la fecha actual, monto final y cambio de estado
    public boolean cerrarCaja(int idArqueo, double montoFinal) {
        String sql = "UPDATE arqueo_caja SET fecha_cierre = NOW(), monto_final = ?, estado = 'CERRADA' WHERE id_arqueo = ?";
        try (Connection con = ConexionDB.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDouble(1, montoFinal);
            ps.setInt(2, idArqueo);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error en cerrarCaja: " + e.getMessage());
            return false;
        }
    }
}