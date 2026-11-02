package com.pos.repuestos;

import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import java.util.Map;
import java.util.HashMap;

public class App {

    public static void main(String[] args) {
        ProductoDAO productoDAO = new ProductoDAO();
        MarcaDAO marcaDAO = new MarcaDAO();
        CategoriaDAO categoriaDAO = new CategoriaDAO();
        VentaDAO ventaDAO = new VentaDAO();
        CajaDAO cajaDAO = new CajaDAO();

        Javalin app = Javalin.create(config -> {
            config.staticFiles.add("public", Location.EXTERNAL);
            config.plugins.enableCors(cors -> {
                cors.add(it -> it.anyHost());
            });
        });

        app.get("/favicon.ico", ctx -> ctx.status(204));

        // ==========================================
        // RUTAS DE MARCAS
        // ==========================================
        app.get("/api/marcas", ctx -> {
            ctx.json(marcaDAO.listarMarcas());
        });

        app.post("/api/marcas", ctx -> {
            Map<?, ?> body = ctx.bodyAsClass(Map.class);
            String nombre = (String) body.get("nombre");
            if (nombre != null && !nombre.trim().isEmpty()) {
                boolean ok = marcaDAO.agregarMarca(nombre);
                if (ok) {
                    ctx.status(200).result("OK");
                } else {
                    ctx.status(500).result("Error en base de datos al guardar la marca");
                }
            } else {
                ctx.status(400).result("Nombre requerido");
            }
        });

        app.delete("/api/marcas/{id}", ctx -> {
            try {
                int id = Integer.parseInt(ctx.pathParam("id"));
                boolean ok = marcaDAO.eliminarMarca(id);
                if (ok) {
                    ctx.status(200).result("OK");
                } else {
                    ctx.status(400).result("No se pudo eliminar la marca");
                }
            } catch (Exception e) {
                ctx.status(400).result("ID inválido");
            }
        });

        // ==========================================
        // RUTAS DE CATEGORÍAS
        // ==========================================
        app.get("/api/categorias", ctx -> {
            ctx.json(categoriaDAO.listarCategorias());
        });

        app.post("/api/categorias", ctx -> {
            Map<?, ?> body = ctx.bodyAsClass(Map.class);
            String nombre = (String) body.get("nombre");
            if (nombre != null && !nombre.trim().isEmpty()) {
                boolean ok = categoriaDAO.agregarCategoria(nombre);
                if (ok) {
                    ctx.status(200).result("OK");
                } else {
                    ctx.status(500).result("Error en base de datos al guardar la categoría");
                }
            } else {
                ctx.status(400).result("Nombre requerido");
            }
        });

        app.delete("/api/categorias/{id}", ctx -> {
            try {
                int id = Integer.parseInt(ctx.pathParam("id"));
                boolean ok = categoriaDAO.eliminarCategoria(id);
                if (ok) {
                    ctx.status(200).result("OK");
                } else {
                    ctx.status(400).result("No se pudo eliminar la categoría");
                }
            } catch (Exception e) {
                ctx.status(400).result("ID inválido");
            }
        });

        // ==========================================
        // RUTAS DE PRODUCTOS
        // ==========================================
        app.get("/api/productos", ctx -> {
            try {
                ctx.json(productoDAO.listarProductos());
            } catch (Exception e) {
                e.printStackTrace();
                ctx.status(500).result("Error al obtener productos: " + e.getMessage());
            }
        });

        app.post("/api/productos", ctx -> {
            try {
                Producto nuevoProducto = ctx.bodyAsClass(Producto.class);
                boolean exito = productoDAO.agregarProducto(nuevoProducto);

                if (exito) {
                    ctx.status(201).json(Map.of("status", "ok", "mensaje", "Producto guardado"));
                } else {
                    ctx.status(500).json(Map.of("status", "error", "mensaje", "No se pudo guardar"));
                }
            } catch (Exception e) {
                e.printStackTrace();
                ctx.status(500).json(Map.of("status", "error", "mensaje", e.getMessage()));
            }
        });

        app.get("/api/productos/{id}", ctx -> {
            try {
                int id = Integer.parseInt(ctx.pathParam("id"));
                Producto encontrado = null;
                for (Producto p : productoDAO.listarProductos()) {
                    if (p.getIdProducto() == id) {
                        encontrado = p;
                        break;
                    }
                }
                if (encontrado != null) {
                    ctx.json(encontrado);
                } else {
                    ctx.status(404).result("Producto no encontrado");
                }
            } catch (Exception e) {
                ctx.status(400).result("ID inválido");
            }
        });

        app.delete("/api/productos/{id}", ctx -> {
            try {
                int id = Integer.parseInt(ctx.pathParam("id"));
                boolean ok = productoDAO.eliminarProducto(id);

                if (ok) {
                    ctx.status(200).result("OK");
                } else {
                    ctx.status(400).result("No se pudo eliminar el producto");
                }
            } catch (Exception e) {
                e.printStackTrace();
                ctx.status(500).result("Error en el servidor: " + e.getMessage());
            }
        });

        app.put("/api/productos/{id}", ctx -> {
            try {
                int id = Integer.parseInt(ctx.pathParam("id"));
                Producto p = ctx.bodyAsClass(Producto.class);
                p.setIdProducto(id);

                boolean actualizado = productoDAO.actualizarProducto(p);
                if (actualizado) {
                    ctx.status(200).result("Producto actualizado correctamente");
                } else {
                    ctx.status(400).result("No se pudo actualizar el producto");
                }
            } catch (Exception e) {
                ctx.status(400).result("Error al actualizar");
            }
        });

        // ==========================================
        // RUTAS DE CAJA (Apertura, Estado, Arqueo y Cierre)
        // ==========================================
        app.get("/api/caja/estado", ctx -> {
            Integer id = cajaDAO.obtenerIdCajaAbierta();
            if (id != null) {
                ctx.json(Map.of("abierta", true, "idCaja", id, "estado", "ABIERTA"));
            } else {
                ctx.json(Map.of("abierta", false, "estado", "CERRADA"));
            }
        });

        app.get("/api/caja/activa", ctx -> {
            Integer id = cajaDAO.obtenerIdCajaAbierta();
            ctx.json(Map.of("activa", id != null, "abierta", id != null));
        });

        io.javalin.http.Handler abrirHandler = ctx -> {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> body = ctx.bodyAsClass(Map.class);
                Object m = body.getOrDefault("monto", body.get("montoInicial"));
                double monto = (m != null) ? Double.parseDouble(m.toString()) : 0.0;

                boolean abierta = cajaDAO.abrirCaja(monto);
                if (abierta) {
                    ctx.status(200).json(Map.of("mensaje", "Caja abierta correctamente"));
                } else {
                    ctx.status(400).json(Map.of("error", "No se pudo registrar la apertura de caja"));
                }
            } catch (Exception e) {
                System.err.println("Error en abrir caja: " + e.getMessage());
                ctx.status(500).json(Map.of("error", e.getMessage()));
            }
        };

        app.post("/api/caja/abrir", abrirHandler);
        app.post("/api/caja/apertura", abrirHandler);
        app.post("/api/caja", abrirHandler);

        // Cierre de caja
        app.post("/api/caja/cerrar", ctx -> {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> body = ctx.bodyAsClass(Map.class);
                Object m = body.getOrDefault("montoFinal", body.get("monto"));
                double montoFinal = (m != null) ? Double.parseDouble(m.toString()) : 0.0;

                Integer idCaja = cajaDAO.obtenerIdCajaAbierta();
                if (idCaja == null) {
                    ctx.status(400).json(Map.of("error", "No hay ninguna caja abierta actualmente"));
                    return;
                }

                boolean cerrada = cajaDAO.cerrarCaja(idCaja, montoFinal);
                if (cerrada) {
                    ctx.status(200).json(Map.of("mensaje", "Caja cerrada correctamente"));
                } else {
                    ctx.status(400).json(Map.of("error", "No se pudo cerrar la caja"));
                }
            } catch (Exception e) {
                ctx.status(500).json(Map.of("error", e.getMessage()));
            }
        });

        app.get("/api/caja/arqueo", ctx -> {
            Map<String, Object> arqueo = new HashMap<>();
            arqueo.put("totalRecaudado", 0.0);
            arqueo.put("totalEfectivo", 0.0);
            arqueo.put("totalYape", 0.0);
            arqueo.put("totalPlin", 0.0);
            arqueo.put("totalTarjeta", 0.0);
            arqueo.put("totalVentas", 0);
            arqueo.put("ventasTotales", 0.0);
            ctx.json(arqueo);
        });

        // ==========================================
        // RUTA DE VENTAS
        // ==========================================
        app.post("/api/ventas", ctx -> {
            try {
                VentaDTO venta = ctx.bodyAsClass(VentaDTO.class);
                Map<String, Object> resultado = ventaDAO.registrarVenta(venta);
                ctx.status(201).json(resultado);
            } catch (Exception e) {
                e.printStackTrace();
                Map<String, Object> errorRes = new HashMap<>();
                errorRes.put("status", "error");
                errorRes.put("mensaje", e.getMessage());
                ctx.status(500).json(errorRes);
            }
        });

        int port = System.getenv("PORT") != null ? Integer.parseInt(System.getenv("PORT")) : 8080;
        app.start(port);
    }
}