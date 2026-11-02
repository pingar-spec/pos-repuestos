package com.pos.repuestos;

public class Categoria {
    private int idCategoria;
    private String nombre;
    private int totalRepuestos;

    public Categoria() {}

    public Categoria(int idCategoria, String nombre, int totalRepuestos) {
        this.idCategoria = idCategoria;
        this.nombre = nombre;
        this.totalRepuestos = totalRepuestos;
    }

    public int getIdCategoria() {
        return idCategoria;
    }

    public void setIdCategoria(int idCategoria) {
        this.idCategoria = idCategoria;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public int getTotalRepuestos() {
        return totalRepuestos;
    }

    public void setTotalRepuestos(int totalRepuestos) {
        this.totalRepuestos = totalRepuestos;
    }
}