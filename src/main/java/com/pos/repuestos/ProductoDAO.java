package com.pos.repuestos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ProductoDAO {

    // 1. Listar todos los productos
    public List<Producto> listarProductos() {
        List<Producto> lista = new ArrayList<>();
       // Debe ir entre comillas dobles y con punto y coma:

        String  sql = "SELECT p.id_producto, p.nombre, p.medida, p.codigo_oem, p.compatibilidad, " +
                     "p.precio_compra, p.precio_venta, p.stock, p.stock_minimo, " +
                     "COALESCE(m.nombre, '') AS marca, COALESCE(c.nombre, '') AS categoria " +
                     "FROM productos p " +
                     "LEFT JOIN marcas m ON p.id_marca = m.id_marca " +
                     "LEFT JOIN categorias c ON p.id_categoria = c.id_categoria " +
             "WHERE (p.activo = 1 OR p.activo IS NULL)";

        try (Connection con = ConexionDB.getConnexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Producto p = new Producto();
                p.setIdProducto(rs.getInt("id_producto"));
                p.setNombre(rs.getString("nombre"));
                p.setMedida(rs.getString("medida"));
                p.setCodigoOem(rs.getString("codigo_oem"));
                p.setCompatibilidad(rs.getString("compatibilidad"));
                p.setPrecioCompra(rs.getDouble("precio_compra"));
                p.setPrecioVenta(rs.getDouble("precio_venta"));
                p.setStock(rs.getInt("stock"));
                p.setStockMinimo(rs.getInt("stock_minimo"));
                p.setMarca(rs.getString("marca"));
                p.setCategoria(rs.getString("categoria"));

                lista.add(p);
            }
        } catch (SQLException e) {
            System.err.println("Error en listarProductos: " + e.getMessage());
        }
        return lista;
    }

    // 2. Agregar un nuevo producto
    public boolean agregarProducto(Producto p) {
        String sql = "INSERT INTO productos (codigo_barras, nombre, medida, codigo_oem, compatibilidad, " +
                     "precio_compra, precio_venta, stock, stock_minimo, id_marca, id_categoria) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection con = ConexionDB.getConnexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            int idMarca = obtenerOcrearMarca(con, p.getMarca());
            int idCategoria = obtenerOcrearCategoria(con, p.getCategoria());

            // Si el producto no trae código de barras, usamos su código OEM o un identificador numérico
            String codigoBarras = (p.getCodigoOem() != null && !p.getCodigoOem().trim().isEmpty())
                                  ? p.getCodigoOem().trim()
                                  : String.valueOf(System.currentTimeMillis() / 1000);

            ps.setString(1, codigoBarras);
            ps.setString(2, p.getNombre());
            ps.setString(3, p.getMedida());
            ps.setString(4, p.getCodigoOem());
            ps.setString(5, p.getCompatibilidad() != null ? p.getCompatibilidad() : "");
            ps.setDouble(6, p.getPrecioCompra());
            ps.setDouble(7, p.getPrecioVenta());
            ps.setInt(8, p.getStock());
            ps.setInt(9, p.getStockMinimo());
            ps.setInt(10, idMarca);
            ps.setInt(11, idCategoria);

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error en agregarProducto: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // 3. Editar datos de un producto
    public boolean editarProducto(Producto p) {
        String sql = "UPDATE productos SET nombre = ?, medida = ?, codigo_oem = ?, compatibilidad = ?, " +
                     "precio_compra = ?, precio_venta = ?, stock = ?, stock_minimo = ?, id_marca = ?, id_categoria = ? " +
                     "WHERE id_producto = ?";

        try (Connection con = ConexionDB.getConnexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            int idMarca = obtenerOcrearMarca(con, p.getMarca());
            int idCategoria = obtenerOcrearCategoria(con, p.getCategoria());

            ps.setString(1, p.getNombre());
            ps.setString(2, p.getMedida());
            ps.setString(3, p.getCodigoOem());
            ps.setString(4, p.getCompatibilidad() != null ? p.getCompatibilidad() : "");
            ps.setDouble(5, p.getPrecioCompra());
            ps.setDouble(6, p.getPrecioVenta());
            ps.setInt(7, p.getStock());
            ps.setInt(8, p.getStockMinimo());
            ps.setInt(9, idMarca);
            ps.setInt(10, idCategoria);
            ps.setInt(11, p.getIdProducto());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error en editarProducto: " + e.getMessage());
            return false;
        }
    }
    public boolean actualizarProducto(Producto p) {
    String sql = "UPDATE productos SET nombre = ?, medida = ?, codigo_oem = ?, compatibilidad = ?, " +
                 "precio_compra = ?, precio_venta = ?, stock = ?, stock_minimo = ?, " +
                 "id_marca = ?, id_categoria = ? WHERE id_producto = ?";

    try (Connection con = ConexionDB.getConexion();
         PreparedStatement ps = con.prepareStatement(sql)) {

        ps.setString(1, p.getNombre());
        ps.setString(2, p.getMedida());
        ps.setString(3, p.getCodigoOem());
        ps.setString(4, p.getCompatibilidad());
        ps.setDouble(5, p.getPrecioCompra());
        ps.setDouble(6, p.getPrecioVenta());
        ps.setInt(7, p.getStock());
        ps.setInt(8, p.getStockMinimo());

        // Manejo de claves foráneas nulas o enteros
        if (p.getIdMarca() != null && p.getIdMarca() > 0) {
            ps.setInt(9, p.getIdMarca());
        } else {
            ps.setNull(9, java.sql.Types.INTEGER);
        }

        if (p.getIdCategoria() != null && p.getIdCategoria() > 0) {
            ps.setInt(10, p.getIdCategoria());
        } else {
            ps.setNull(10, java.sql.Types.INTEGER);
        }

        ps.setInt(11, p.getIdProducto());

        return ps.executeUpdate() > 0;
    } catch (SQLException e) {
        System.err.println("Error en actualizarProducto: " + e.getMessage());
        return false;
    }
}

    // 4. Eliminar producto por ID}
    public boolean eliminarProducto(int idProducto) {
    String sql = "UPDATE productos SET activo = 0 WHERE id_producto = ?";

    try (Connection con = ConexionDB.getConnexion();
         PreparedStatement ps = con.prepareStatement(sql)) {

        ps.setInt(1, idProducto);
        return ps.executeUpdate() > 0;
    } catch (SQLException e) {
        System.err.println("Error en eliminarProducto: " + e.getMessage());
        return false;
    }
}
    // 5. Reponer stock
    public boolean reponerStock(int idProducto, int cantidad, double nuevoPrecioCompra) {
        String sql = "UPDATE productos SET stock = stock + ?, precio_compra = ? WHERE id_producto = ?";

        try (Connection con = ConexionDB.getConnexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, cantidad);
            ps.setDouble(2, nuevoPrecioCompra);
            ps.setInt(3, idProducto);

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error en reponerStock: " + e.getMessage());
            return false;
        }
    }

    // 6. Consultar movimientos del kardex
    public List<String> obtenerKardexProducto(int idProducto) {
        List<String> kardex = new ArrayList<>();
        String sql = "SELECT fecha, tipo_movimiento, cantidad, saldo FROM kardex WHERE id_producto = ? ORDER BY fecha ASC";

        try (Connection con = ConexionDB.getConnexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idProducto);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String linea = rs.getString("fecha") + " | " +
                                   rs.getString("tipo_movimiento") + " | Cant: " +
                                   rs.getInt("cantidad") + " | Saldo: " +
                                   rs.getInt("saldo");
                    kardex.add(linea);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error en obtenerKardexProducto: " + e.getMessage());
        }
        return kardex;
    }

    // --- Métodos de apoyo para resolver IDs de marcas y categorías ---

    private int obtenerOcrearMarca(Connection con, String nombreMarca) throws SQLException {
        if (nombreMarca == null || nombreMarca.trim().isEmpty()) return 1;

        String sqlBuscar = "SELECT id_marca FROM marcas WHERE LOWER(nombre) = LOWER(?) LIMIT 1";
        try (PreparedStatement ps = con.prepareStatement(sqlBuscar)) {
            ps.setString(1, nombreMarca.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("id_marca");
            }
        }

        String sqlInsertar = "INSERT INTO marcas (nombre) VALUES (?)";
        try (PreparedStatement ps = con.prepareStatement(sqlInsertar, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, nombreMarca.trim());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 1;
    }

    private int obtenerOcrearCategoria(Connection con, String nombreCategoria) throws SQLException {
        if (nombreCategoria == null || nombreCategoria.trim().isEmpty()) return 1;

        String sqlBuscar = "SELECT id_categoria FROM categorias WHERE LOWER(nombre) = LOWER(?) LIMIT 1";
        try (PreparedStatement ps = con.prepareStatement(sqlBuscar)) {
            ps.setString(1, nombreCategoria.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("id_categoria");
            }
        }

        String sqlInsertar = "INSERT INTO categorias (nombre) VALUES (?)";
        try (PreparedStatement ps = con.prepareStatement(sqlInsertar, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, nombreCategoria.trim());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 1;
    }
}