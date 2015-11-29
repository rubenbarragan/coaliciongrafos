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
public class transicion {

    String name;
    int index = 0;
    int index2 = 0;

    public transicion(String m, int i, int j) {
        name = m;
        index = i;
        index2 = j;
    }

    public int getIndex2() {
        return index2;
    }

    public int getIndex() {
        return index;
    }
}
