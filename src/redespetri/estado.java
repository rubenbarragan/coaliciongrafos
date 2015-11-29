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
public class estado {
    String nombre;
    int marcado;
    int index;
    int index2;
    public estado(String n, int m, int i, int ii){
    nombre= n;
    marcado=m;
    index=i;
    index2=ii;}
    
    public int getIndex(){
    return index;
    }
    
     public int getIndex2(){
    return index2;
    }
    
    
    

  

}
