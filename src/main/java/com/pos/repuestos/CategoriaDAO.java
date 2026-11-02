package com.pos.repuestos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CategoriaDAO {

    public List<Categoria> listarCategorias() {
        List<Categoria> lista = new ArrayList<>();
        String sql = "SELECT c.id_categoria, c.nombre, COUNT(p.id_producto) AS total_repuestos " +
                     "FROM categorias c " +
                     "LEFT JOIN productos p ON c.id_categoria = p.id_categoria " +
                     "GROUP BY c.id_categoria, c.nombre " +
                     "ORDER BY c.nombre ASC";

        try (Connection con = ConexionDB.getConnexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Categoria c = new Categoria();
                c.setIdCategoria(rs.getInt("id_categoria"));
                c.setNombre(rs.getString("nombre"));
                c.setTotalRepuestos(rs.getInt("total_repuestos"));
                lista.add(c);
            }
        } catch (SQLException e) {
            System.err.println("Error en listarCategorias: " + e.getMessage());
        }
        return lista;
    }

    public boolean agregarCategoria(String nombre) {
        String sql = "INSERT INTO categorias (nombre) VALUES (?)";
        try (Connection con = ConexionDB.getConnexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, nombre.trim());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error en agregarCategoria: " + e.getMessage());
            return false;
        }
    }

   public boolean eliminarCategoria(int idCategoria) {
    // 1. Quitar la categoría a los productos que la tenían asignada
    String sqlDesvincular = "UPDATE productos SET id_categoria = NULL WHERE id_categoria = ?";
    // 2. Eliminar la categoría
    String sqlDelete = "DELETE FROM categorias WHERE id_categoria = ?";

    try (Connection con = ConexionDB.getConnexion()) {
        con.setAutoCommit(false); // Transacción para que se hagan las dos cosas juntas

        try (PreparedStatement psDesvincular = con.prepareStatement(sqlDesvincular)) {
            psDesvincular.setInt(1, idCategoria);
            psDesvincular.executeUpdate();
        }

        try (PreparedStatement psDelete = con.prepareStatement(sqlDelete)) {
            psDelete.setInt(1, idCategoria);
            int filasAfectadas = psDelete.executeUpdate();
            
            con.commit(); // Confirmar cambios
            return filasAfectadas > 0;
        } catch (SQLException e) {
            con.rollback(); // Revertir si ocurre un error
            throw e;
        } finally {
            con.setAutoCommit(true);
        }
        } catch (SQLException e) {
        System.err.println("Error en eliminarCategoria: " + e.getMessage());
        return false;
        }  
    } 
}
