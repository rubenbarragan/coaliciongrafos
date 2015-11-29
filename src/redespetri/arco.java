/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package redespetri;

/**
 *
 * @author edwin
 */
public class arco {

    String nombre, from, to;
    int inscription;//cantidad de marcas que requiere para dispararse ese arco

    public arco(String n, String f, String t, int i) {
        nombre = n;
        from = f;
        to = t;
        inscription = i;
    }
}
