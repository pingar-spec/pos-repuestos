package com.pos.repuestos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MarcaDAO {

    public List<Marca> listarMarcas() {
        List<Marca> lista = new ArrayList<>();
        String sql = "SELECT m.id_marca, m.nombre, COUNT(p.id_producto) AS total_repuestos " +
                     "FROM marcas m " +
                     "LEFT JOIN productos p ON m.id_marca = p.id_marca " +
                     "GROUP BY m.id_marca, m.nombre " +
                     "ORDER BY m.nombre ASC";

        try (Connection con = ConexionDB.getConnexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Marca m = new Marca();
                m.setIdMarca(rs.getInt("id_marca"));
                m.setNombre(rs.getString("nombre"));
                m.setTotalRepuestos(rs.getInt("total_repuestos"));
                lista.add(m);
            }
        } catch (SQLException e) {
            System.err.println("Error en listarMarcas: " + e.getMessage());
        }
        return lista;
    }

    public boolean agregarMarca(String nombre) {
        String sql = "INSERT INTO marcas (nombre) VALUES (?)";
        try (Connection con = ConexionDB.getConnexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, nombre.trim());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error en agregarMarca: " + e.getMessage());
            return false;
        }

    }
    public boolean eliminarMarca(int idMarca) {
    // 1. Quitar la marca de los productos asociados para no romper la clave foránea
    String sqlDesvincular = "UPDATE productos SET id_marca = NULL WHERE id_marca = ?";
    // 2. Ahora sí, eliminar la marca física
    String sqlEliminar = "DELETE FROM marcas WHERE id_marca = ?";

    try (Connection con = ConexionDB.getConexion()) {
        // Paso A: Desvincular productos (activos o inactivos)
        try (PreparedStatement psDesvincular = con.prepareStatement(sqlDesvincular)) {
            psDesvincular.setInt(1, idMarca);
            psDesvincular.executeUpdate();
        }

        // Paso B: Borrar la marca
        try (PreparedStatement psEliminar = con.prepareStatement(sqlEliminar)) {
            psEliminar.setInt(1, idMarca);
            return psEliminar.executeUpdate() > 0;
        }

    } catch (SQLException e) {
        System.err.println("Error en eliminarMarca: " + e.getMessage());
        return false;
    }
}


}