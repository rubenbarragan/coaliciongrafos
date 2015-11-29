/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package redespetri;

import java.util.ArrayList;

/**
 *
 * @author HectorJalil
 */
public class Nodo {

    ArrayList<Nodo> hijos;
    int[] marcado;
    Nodo padre;
    String tranDisparada;
    boolean Terminal = false;
    boolean Duplicado = false;

    public Nodo(int[] marcado, Nodo padre, String tran) {
        this.marcado = marcado;
        this.padre = padre;
        this.tranDisparada = tran;
        hijos = new ArrayList();
    }

    public void setMarcado(int m[]) {
        this.marcado = m;
    }

    public void Print() {
        System.out.print("Marcado: ");
        for (int i = 0; i < marcado.length; i++) {
            System.out.print(marcado[i] + " ");
        }
        System.out.print("\n Transicion Disparada: " + tranDisparada + "\n");
    }

    public String homomorfismo() {
        String cad = "";
        for (int i = 0; i < marcado.length; i++) {
            if (marcado[i] == -1) {
                cad += "w";
            } else {
                cad += marcado[i];
            }

        }
        return "\""+cad+"\"";
    }
}
