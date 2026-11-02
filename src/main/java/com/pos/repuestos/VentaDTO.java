package com.pos.repuestos;

import java.util.List;

public class VentaDTO {
    public String tipoComprobante;
    public String clienteDoc;
    public String placa;
    public String metodoPago;
    public double montoRecibido;
    public double vuelto;
    public List<ItemVentaDTO> items;

    public VentaDTO() {
    }

    public static class ItemVentaDTO {
        public int idProducto;
        public int cantidad;
        public double precioUnitario;

        public ItemVentaDTO() {
        }
    }
}