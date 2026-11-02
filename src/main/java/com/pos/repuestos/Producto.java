package com.pos.repuestos;

public class Producto {
    private int idProducto;
    private String nombre;
    private String medida;
    private String codigoOem;
    private String compatibilidad;
    private double precioCompra;
    private double precioVenta;
    private int stock;
    private int stockMinimo;
    private String categoria;
    private String marca;
    private Integer idMarca;
    private Integer idCategoria;


    public Producto() {}

    public Integer getIdMarca() {
    return idMarca;
}

public void setIdMarca(Integer idMarca) {
    this.idMarca = idMarca;
}

public Integer getIdCategoria() {
    return idCategoria;
}

public void setIdCategoria(Integer idCategoria) {
    this.idCategoria = idCategoria;
}

    public int getIdProducto() { return idProducto; }
    public void setIdProducto(int idProducto) { this.idProducto = idProducto; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getMedida() { return medida; }
    public void setMedida(String medida) { this.medida = medida; }

    public String getCodigoOem() { return codigoOem; }
    public void setCodigoOem(String codigoOem) { this.codigoOem = codigoOem; }

    public String getCompatibilidad() { return compatibilidad; }
    public void setCompatibilidad(String compatibilidad) { this.compatibilidad = compatibilidad; }

    public double getPrecioCompra() { return precioCompra; }
    public void setPrecioCompra(double precioCompra) { this.precioCompra = precioCompra; }

    public double getPrecioVenta() { return precioVenta; }
    public void setPrecioVenta(double precioVenta) { this.precioVenta = precioVenta; }

    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }

    public int getStockMinimo() { return stockMinimo; }
    public void setStockMinimo(int stockMinimo) { this.stockMinimo = stockMinimo; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }
}