package com.pos.repuestos;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VentaDAO {

    public Map<String, Object> registrarVenta(VentaDTO venta) throws Exception {
        if (venta.items == null || venta.items.isEmpty()) {
            throw new IllegalArgumentException("No se pueden procesar ventas sin repuestos seleccionados.");
        }

        // 1. Verificación de estado de caja
        Map<String, Object> estadoCaja = obtenerEstadoCaja();
        if ("CERRADA".equalsIgnoreCase((String) estadoCaja.get("estado"))) {
            throw new IllegalStateException("Operación bloqueada: La caja de mostrador se encuentra cerrada.");
        }

        Connection con = null;
        try {
            con = ConexionDB.obtenerConexion();
            con.setAutoCommit(false);

            // 2. Validación de stock con bloqueo FOR UPDATE
            double totalCalculado = 0.0;
            Map<Integer, Integer> stockAnteriorMap = new HashMap<>();

            for (VentaDTO.ItemVentaDTO item : venta.items) {
                String sqlVerificar = "SELECT nombre, precio_venta, stock FROM productos WHERE id_producto = ? FOR UPDATE";
                try (PreparedStatement psVerificar = con.prepareStatement(sqlVerificar)) {
                    psVerificar.setInt(1, item.idProducto);
                    try (ResultSet rs = psVerificar.executeQuery()) {
                        if (!rs.next()) {
                            throw new Exception("El repuesto con ID " + item.idProducto + " no existe.");
                        }
                        int stockActual = rs.getInt("stock");
                        double precioReal = rs.getDouble("precio_venta");
                        String nombreRepuesto = rs.getString("nombre");

                        if (stockActual < item.cantidad) {
                            throw new Exception("Stock insuficiente para: " + nombreRepuesto + ". Disponibles: " + stockActual);
                        }

                        stockAnteriorMap.put(item.idProducto, stockActual);
                        totalCalculado += (precioReal * item.cantidad);
                        item.precioUnitario = precioReal;
                    }
                }
            }

            // 3. Cálculos tributarios (Ley SUNAT Perú: IGV 18%)
            double opGravada = Math.round((totalCalculado / 1.18) * 100.0) / 100.0;
            double igv = Math.round((totalCalculado - opGravada) * 100.0) / 100.0;

            // 4. Correlativo fiscal
            String serieFiscal = "FACTURA".equalsIgnoreCase(venta.tipoComprobante) ? "F001" : "B001";
            String sqlCorrelativo = "SELECT COUNT(*) + 1 AS siguiente FROM ventas WHERE tipo_comprobante = ?";
            String nroComprobanteGenerado;
            int correlativoNum;
            try (PreparedStatement psCorr = con.prepareStatement(sqlCorrelativo)) {
                psCorr.setString(1, venta.tipoComprobante.toUpperCase());
                try (ResultSet rsCorr = psCorr.executeQuery()) {
                    rsCorr.next();
                    correlativoNum = rsCorr.getInt("siguiente");
                    nroComprobanteGenerado = serieFiscal + "-" + String.format("%08d", correlativoNum);
                }
            }

            // 5. Firma criptográfica SHA-256
            String cadenaSeguridad = nroComprobanteGenerado + "|" + totalCalculado + "|" + venta.clienteDoc + "|" + System.currentTimeMillis();
            String hashSeguridad = generarSHA256(cadenaSeguridad);

            // 6. Detección dinámica y construcción de campos de tabla ventas
            boolean tieneSerie = verificarColumnaExiste(con, "ventas", "serie");
            boolean tieneNumeroComp = verificarColumnaExiste(con, "ventas", "numero_comprobante");
            boolean tieneComprobante = verificarColumnaExiste(con, "ventas", "comprobante");
            boolean tieneCorrelativo = verificarColumnaExiste(con, "ventas", "correlativo");
            boolean tieneClienteDoc = verificarColumnaExiste(con, "ventas", "cliente_doc");
            boolean tienePlaca = verificarColumnaExiste(con, "ventas", "placa_vehicular");
            boolean tieneHash = verificarColumnaExiste(con, "ventas", "hash_seguridad");
            boolean tieneOpGravada = verificarColumnaExiste(con, "ventas", "op_gravada");
            boolean tieneIgv = verificarColumnaExiste(con, "ventas", "igv");

            StringBuilder campos = new StringBuilder("tipo_comprobante, metodo_pago, total, monto_recibido, vuelto");
            StringBuilder placeholders = new StringBuilder("?, ?, ?, ?, ?");
            List<Object> parametros = new ArrayList<>();

            parametros.add(venta.tipoComprobante.toUpperCase());
            parametros.add(venta.metodoPago.toUpperCase());
            parametros.add(totalCalculado);
            parametros.add(venta.montoRecibido);
            parametros.add(venta.vuelto);

            if (tieneOpGravada) {
                campos.append(", op_gravada");
                placeholders.append(", ?");
                parametros.add(opGravada);
            }
            if (tieneIgv) {
                campos.append(", igv");
                placeholders.append(", ?");
                parametros.add(igv);
            }
            if (tieneSerie) {
                campos.append(", serie");
                placeholders.append(", ?");
                parametros.add(serieFiscal);
            }
            if (tieneNumeroComp) {
                campos.append(", numero_comprobante");
                placeholders.append(", ?");
                parametros.add(nroComprobanteGenerado);
            } else if (tieneComprobante) {
                campos.append(", comprobante");
                placeholders.append(", ?");
                parametros.add(nroComprobanteGenerado);
            }
            if (tieneCorrelativo) {
                campos.append(", correlativo");
                placeholders.append(", ?");
                parametros.add(correlativoNum);
            }
            if (tieneClienteDoc) {
                campos.append(", cliente_doc");
                placeholders.append(", ?");
                parametros.add(venta.clienteDoc != null ? venta.clienteDoc.trim() : "00000000");
            }
            if (tienePlaca) {
                campos.append(", placa_vehicular");
                placeholders.append(", ?");
                parametros.add(venta.placa != null ? venta.placa.trim().toUpperCase() : "SIN PLACA");
            }
            if (tieneHash) {
                campos.append(", hash_seguridad");
                placeholders.append(", ?");
                parametros.add(hashSeguridad);
            }

            String sqlVenta = "INSERT INTO ventas (" + campos.toString() + ") VALUES (" + placeholders.toString() + ")";
            int idVentaGenerada;

            try (PreparedStatement psVenta = con.prepareStatement(sqlVenta, Statement.RETURN_GENERATED_KEYS)) {
                for (int i = 0; i < parametros.size(); i++) {
                    psVenta.setObject(i + 1, parametros.get(i));
                }
                psVenta.executeUpdate();

                try (ResultSet rsKeys = psVenta.getGeneratedKeys()) {
                    if (rsKeys.next()) {
                        idVentaGenerada = rsKeys.getInt(1);
                    } else {
                        throw new SQLException("No se pudo obtener el identificador de la venta.");
                    }
                }
            }

            // 7. Registro de detalle y Kardex
            String sqlDetalle = "INSERT INTO detalle_ventas (id_venta, id_producto, cantidad, precio_unitario, subtotal) VALUES (?, ?, ?, ?, ?)";
            String sqlDescontar = "UPDATE productos SET stock = stock - ? WHERE id_producto = ?";
            String sqlKardex = "INSERT INTO kardex (id_producto, tipo_movimiento, cantidad, stock_anterior, stock_posterior, motivo) VALUES (?, 'SALIDA', ?, ?, ?, ?)";

            try (PreparedStatement psDetalle = con.prepareStatement(sqlDetalle);
                 PreparedStatement psDescontar = con.prepareStatement(sqlDescontar);
                 PreparedStatement psKardex = con.prepareStatement(sqlKardex)) {

                for (VentaDTO.ItemVentaDTO item : venta.items) {
                    double subtotalItem = item.precioUnitario * item.cantidad;
                    int stockAnt = stockAnteriorMap.get(item.idProducto);
                    int stockPost = stockAnt - item.cantidad;

                    psDetalle.setInt(1, idVentaGenerada);
                    psDetalle.setInt(2, item.idProducto);
                    psDetalle.setInt(3, item.cantidad);
                    psDetalle.setDouble(4, item.precioUnitario);
                    psDetalle.setDouble(5, subtotalItem);
                    psDetalle.addBatch();

                    psDescontar.setInt(1, item.cantidad);
                    psDescontar.setInt(2, item.idProducto);
                    psDescontar.addBatch();

                    psKardex.setInt(1, item.idProducto);
                    psKardex.setInt(2, item.cantidad);
                    psKardex.setInt(3, stockAnt);
                    psKardex.setInt(4, stockPost);
                    psKardex.setString(5, "Venta " + nroComprobanteGenerado);
                    psKardex.addBatch();
                }

                psDetalle.executeBatch();
                psDescontar.executeBatch();
                psKardex.executeBatch();
            }

            con.commit();

            Map<String, Object> respuesta = new HashMap<>();
            respuesta.put("idVenta", idVentaGenerada);
            respuesta.put("comprobante", nroComprobanteGenerado);
            respuesta.put("tipo", venta.tipoComprobante);
            respuesta.put("total", totalCalculado);
            respuesta.put("hash", hashSeguridad);
            return respuesta;

        } catch (Exception e) {
            if (con != null) {
                try { con.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            throw e;
        } finally {
            if (con != null) {
                try {
                    con.setAutoCommit(true);
                    con.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public List<Map<String, Object>> obtenerHistorialPlaca(String placa) {
        List<Map<String, Object>> lista = new ArrayList<>();
        try (Connection con = ConexionDB.obtenerConexion()) {
            String colPlaca = verificarColumnaExiste(con, "ventas", "placa_vehicular") ? "v.placa_vehicular" : "v.placa";

            String sql = "SELECT " +
                         "    v.fecha_hora, " +
                         "    p.nombre AS repuesto, " +
                         "    COALESCE(p.medida, '') AS medida, " +
                         "    COALESCE(m.nombre, 'Genérica') AS marca, " +
                         "    dv.cantidad, " +
                         "    dv.precio_unitario AS precio " +
                         "FROM ventas v " +
                         "INNER JOIN detalle_ventas dv ON v.id_venta = dv.id_venta " +
                         "INNER JOIN productos p ON dv.id_producto = p.id_producto " +
                         "LEFT JOIN marcas m ON p.id_marca = m.id_marca " +
                         "WHERE UPPER(" + colPlaca + ") = UPPER(?) " +
                         "ORDER BY v.fecha_hora DESC " +
                         "LIMIT 30";

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, placa.trim());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> reg = new HashMap<>();
                        reg.put("fecha", rs.getString("fecha_hora"));
                        reg.put("repuesto", rs.getString("repuesto"));
                        reg.put("medida", rs.getString("medida"));
                        reg.put("marca", rs.getString("marca"));
                        reg.put("cantidad", rs.getInt("cantidad"));
                        reg.put("precio", rs.getDouble("precio"));
                        lista.add(reg);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al consultar historial vehicular: " + e.getMessage());
        }
        return lista;
    }

    public Map<String, Object> obtenerArqueoCajaHoy() {
        Map<String, Object> arqueo = new HashMap<>();
        String sql = """
            SELECT 
                COUNT(*) AS total_ventas,
                COALESCE(SUM(total), 0) AS total_recaudado,
                COALESCE(SUM(CASE WHEN metodo_pago = 'EFECTIVO' THEN total ELSE 0 END), 0) AS total_efectivo,
                COALESCE(SUM(CASE WHEN metodo_pago = 'YAPE' THEN total ELSE 0 END), 0) AS total_yape,
                COALESCE(SUM(CASE WHEN metodo_pago = 'PLIN' THEN total ELSE 0 END), 0) AS total_plin,
                COALESCE(SUM(CASE WHEN metodo_pago = 'TARJETA' THEN total ELSE 0 END), 0) AS total_tarjeta,
                COALESCE(SUM(CASE WHEN tipo_comprobante = 'BOLETA' THEN 1 ELSE 0 END), 0) AS boletas_emitidas,
                COALESCE(SUM(CASE WHEN tipo_comprobante = 'FACTURA' THEN 1 ELSE 0 END), 0) AS facturas_emitidas
            FROM ventas 
            WHERE DATE(fecha_hora) = CURDATE()
        """;

        try (Connection con = ConexionDB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                arqueo.put("totalVentas", rs.getInt("total_ventas"));
                arqueo.put("totalRecaudado", rs.getDouble("total_recaudado"));
                arqueo.put("totalEfectivo", rs.getDouble("total_efectivo"));
                arqueo.put("totalYape", rs.getDouble("total_yape"));
                arqueo.put("totalPlin", rs.getDouble("total_plin"));
                arqueo.put("totalTarjeta", rs.getDouble("total_tarjeta"));
                arqueo.put("boletasEmitidas", rs.getInt("boletas_emitidas"));
                arqueo.put("facturasEmitidas", rs.getInt("facturas_emitidas"));
            }
        } catch (SQLException e) {
            System.err.println("Error al calcular arqueo de caja: " + e.getMessage());
        }
        return arqueo;
    }

    public Map<String, Object> obtenerEstadoCaja() {
        Map<String, Object> caja = new HashMap<>();
        String sql = "SELECT * FROM cajas_sesion ORDER BY id_caja DESC LIMIT 1";

        try (Connection con = ConexionDB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                caja.put("idCaja", rs.getInt("id_caja"));
                caja.put("establecimiento", rs.getString("establecimiento"));
                caja.put("nombre", rs.getString("nombre_caja"));
                caja.put("abiertoPor", rs.getString("abierto_por"));
                caja.put("montoApertura", rs.getDouble("monto_apertura"));
                caja.put("montoCierre", rs.getObject("monto_cierre") != null ? rs.getDouble("monto_cierre") : 0.0);
                caja.put("fechaApertura", rs.getString("fecha_apertura"));
                caja.put("fechaCierre", rs.getString("fecha_cierre"));
                caja.put("estado", rs.getString("estado"));
            } else {
                caja.put("estado", "CERRADA");
            }
        } catch (SQLException e) {
            caja.put("estado", "ABIERTA");
            caja.put("abiertoPor", "ADMINISTRADOR");
        }
        return caja;
    }

    public boolean abrirCaja(double montoApertura) {
        String sql = "INSERT INTO cajas_sesion (establecimiento, nombre_caja, abierto_por, monto_apertura, estado) " +
                     "VALUES ('PRINCIPAL', 'Caja Mostrador', 'ADMINISTRADOR', ?, 'ABIERTA')";
        try (Connection con = ConexionDB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDouble(1, montoApertura);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al abrir caja: " + e.getMessage());
            return false;
        }
    }

    public boolean cerrarCaja(int idCaja, double montoCierre) {
        String sql = "UPDATE cajas_sesion SET monto_cierre = ?, fecha_cierre = CURRENT_TIMESTAMP, estado = 'CERRADA' WHERE id_caja = ?";
        try (Connection con = ConexionDB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDouble(1, montoCierre);
            ps.setInt(2, idCaja);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al cerrar caja: " + e.getMessage());
            return false;
        }
    }

    private boolean verificarColumnaExiste(Connection con, String tabla, String columna) {
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SHOW COLUMNS FROM " + tabla + " LIKE '" + columna + "'")) {
            return rs.next();
        } catch (SQLException e) {
            return false;
        }
    }

    private String generarSHA256(String texto) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(texto.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02X", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "DEFAULT_SHA256_HASH";
        }
    }
}