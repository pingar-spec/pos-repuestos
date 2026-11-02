package com.pos.repuestos;

public class Marca {
    private int idMarca;
    private String nombre;
    private int totalRepuestos;

    public Marca() {}

    public Marca(int idMarca, String nombre, int totalRepuestos) {
        this.idMarca = idMarca;
        this.nombre = nombre;
        this.totalRepuestos = totalRepuestos;
    }

    public int getIdMarca() {
        return idMarca;
    }

    public void setIdMarca(int idMarca) {
        this.idMarca = idMarca;
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