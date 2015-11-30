/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package redespetri;

import java.io.BufferedWriter;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author edwin
 */
public class RedesPetri {

    String grafo_file = "digraph G {";

    static boolean Repetitiva = false;
    static boolean Conservativa = true;
    static boolean Acotada = true;
    static boolean LibreDeBloqueo = true;
    static int mi[][], pre[][], pos[][];

    int mark[];

    static ArrayList<estado> p = new ArrayList();
    static ArrayList<transicion> t = new ArrayList();
    static ArrayList<String> t_disparados = new ArrayList<>();
    static ArrayList<arco> a = new ArrayList();
    static ArrayList<Nodo> LP = new ArrayList();//lista de nodos que se van formando, pendientes
    static ArrayList<Nodo> LQ = new ArrayList();//nodos ya procesados
    static ArrayList<Nodo> copiaLQdesendiente = new ArrayList<>();//nodos ya procesados
    static int time = 0;
    public void leerArchivo(String file) {
        try {

            File fXmlFile = new File(file);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(fXmlFile);

            doc.getDocumentElement().normalize();

            NodeList nList = doc.getElementsByTagName("place");

            // System.out.println("----------------------------");
//lectura de estados
            int index = 0;
            int index2 = 0;
            for (int temp = 0; temp < nList.getLength(); temp++) {
                Node nNode = nList.item(temp);

                //      System.out.println("\nCurrent Element :" + nNode.getNodeName());
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;
                    //   System.out.println("Estado : " + eElement.getAttribute("id"));
                    String cad = eElement.getElementsByTagName("initialMarking").item(0).getTextContent().replace("Default,", "");
                    String cad2[] = cad.split("\n");
                    //  System.out.println("Marcado inicial : " + cad2[1]);
                    index2 = Integer.parseInt(eElement.getAttribute("id").substring(1, eElement.getAttribute("id").length()));
                    estado tempo = new estado(eElement.getAttribute("id"), Integer.parseInt(cad2[1]), index, index2);
                    p.add(tempo);
                    index++;
                    //  System.out.println("Capacidad : " + eElement.getElementsByTagName("capacity").item(0).getTextContent());
                }
            }
            //ordenar lista de p en base a index2
            ArrayList m = new ArrayList();
            for (int in = 0; in < p.size(); in++) {
                m.add(p.get(in).getIndex2());
            }
            Collections.sort(m);

            //ya ordenados los indices se comparan para ordenar los valores de P
            ArrayList<estado> p2 = new ArrayList();
            for (int i = 0; i < m.size(); i++) {
                for (int j = 0; j < p.size(); j++) {
                    if (p.get(j).index2 == Integer.parseInt(m.get(i).toString())) {
                        p2.add(p.get(j));

                    }
                }
                p2.get(i).index = i;
                //System.out.println(p2.get(i).nombre);

            }
            p = p2;
            p2 = null;

            nList = doc.getElementsByTagName("transition");
//lectura de transiciones
            //System.out.println("----------------------------");
            index = 0;
            for (int temp = 0; temp < nList.getLength(); temp++) {
                Node nNode = nList.item(temp);
                //       System.out.println("\nCurrent Element :" + nNode.getNodeName());
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;
                    //      System.out.println("Transición : " + eElement.getAttribute("id"));
                    index2 = Integer.parseInt(eElement.getAttribute("id").substring(1, eElement.getAttribute("id").length()));
                    transicion tempo = new transicion(eElement.getAttribute("id"), index, index2);
                    t.add(tempo);
                    index++;
                }
            }
            //ordenar t
            //ordenar lista de t en base a index2
            m = new ArrayList();
            for (int in = 0; in < t.size(); in++) {
                m.add(t.get(in).getIndex2());
            }
            Collections.sort(m);

            //ya ordenados los indices se comparan para ordenar los valores de P
            ArrayList<transicion> t2 = new ArrayList();
            for (int i = 0; i < m.size(); i++) {
                for (int j = 0; j < t.size(); j++) {
                    if (t.get(j).index2 == Integer.parseInt(m.get(i).toString())) {
                        t2.add(t.get(j));

                    }
                }
                t2.get(i).index = i;
                //System.out.println(t2.get(i).name);

            }
            t = t2;
            t2 = null;

            nList = doc.getElementsByTagName("arc");
//lectura de arcos
            //System.out.println("----------------------------");
            for (int temp = 0; temp < nList.getLength(); temp++) {
                Node nNode = nList.item(temp);
                //       System.out.println("\nCurrent Element :" + nNode.getNodeName());
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;
                    //      System.out.println("Transición : " + eElement.getAttribute("id"));
                    //    System.out.println("from : " + eElement.getAttribute("source"));
//                    System.out.println("to : " + eElement.getAttribute("target"));
                    String cad = eElement.getElementsByTagName("inscription").item(0).getTextContent().replace("Default,", "");
                    String cad2[] = cad.split("\n");
                    //                  System.out.println("Inscription " + cad2[1]);
                    arco tempo = new arco(eElement.getAttribute("id"), eElement.getAttribute("source"), eElement.getAttribute("target"), Integer.parseInt(cad2[1]));
                    a.add(tempo);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        //ordenar e indexar P
        //crear la matriz de incidencia
        mi = new int[p.size()][t.size()];
        pre = new int[p.size()][t.size()];
        pos = new int[p.size()][t.size()];
        //    System.out.println("tamaño de la matriz" + p.size() + " " + t.size());

        for (int i = 0; i < a.size(); i++) {//recorrer la lista de transiciones
            //cada transicion obtener de donde a donde va y obtener ese indice de estado y transicion
            arco m = a.get(i);
            String from, to;
            from = m.from;
            to = m.to;
            int value = m.inscription;
            int indicep = 0;
            int indicet = 0;
            int ip, it;

            if (from.contains("P")) {
                for (int k = 0; k < p.size(); k++) {
                    if (p.get(k).nombre.equals(from)) {
                        indicep = k;
                    }
                }
                for (int k = 0; k < t.size(); k++) {
                    if (t.get(k).name.equals(to)) {
                        indicet = k;
                    }
                }
                //valor positivo, p es parte del from
                ip = Integer.parseInt(from.substring(1, from.length()));
                // System.out.print(p);

                it = Integer.parseInt(to.substring(1, to.length()));
                //System.out.print(t);

                //          System.out.println(ip + " " + it);
                //        System.out.println(value);
                mi[p.get(indicep).index][t.get(indicet).index] -= value;
                pre[p.get(indicep).index][t.get(indicet).index] += value;
                //System.out.print("indice"+p.get(indicep).index);
            } else {
                for (int k = 0; k < p.size(); k++) {
                    if (p.get(k).nombre.equals(to)) {
                        indicep = k;
                    }
                }
                for (int k = 0; k < t.size(); k++) {
                    if (t.get(k).name.equals(from)) {
                        indicet = k;
                    }
                }
                //valor positivo, p es parte del from
                ip = Integer.parseInt(from.substring(1, from.length()));
                // System.out.print(p);

                it = Integer.parseInt(to.substring(1, to.length()));
                //System.out.print(t);

                //      System.out.println(it + " " + ip);
                //     System.out.println(value);
                mi[p.get(indicep).index][t.get(indicet).index] += value;
                pos[p.get(indicep).index][t.get(indicet).index] += value;
            }
        }

        System.out.println("Matriz de incidencia");
        String cad = "   ";
        for (int i = 0; i < t.size(); i++) {
            cad += t.get(i).name + "|";
        }
        System.out.println(cad);

        for (int i = 0; i < p.size(); i++) {
            System.out.print(p.get(i).nombre + "|");
            for (int j = 0; j < t.size(); j++) {
                System.out.print(mi[i][j] + " |");
            }
            System.out.println();
        }

        System.out.println("\nPRE");
        cad = "   ";
        for (int i = 0; i < t.size(); i++) {
            cad += t.get(i).name + "|";
        }
        System.out.println(cad);

        for (int i = 0; i < p.size(); i++) {
            System.out.print(p.get(i).nombre + "|");
            for (int j = 0; j < t.size(); j++) {
                System.out.print(pre[i][j] + " |");
            }
            System.out.println();
        }

        System.out.println("\nPOS");
        cad = "   ";
        for (int i = 0; i < t.size(); i++) {
            cad += t.get(i).name + "|";
        }
        System.out.println(cad);

        for (int i = 0; i < p.size(); i++) {
            System.out.print(p.get(i).nombre + "|");
            for (int j = 0; j < t.size(); j++) {
                System.out.print(pos[i][j] + " |");
            }
            System.out.println();
        }
        marcadoInicial();
    }

    public void marcadoInicial() {
        mark = new int[p.size()];
        System.out.println("\nMarcado inicial");
        for (int i = 0; i < p.size(); i++) {
            mark[i] = p.get(i).marcado;
            System.out.println(p.get(i).nombre + "|" + mark[i]);
        }
    }

    public void generarMarcados(Nodo padre) {

        for (int j = 0; j < t.size(); j++) {
            int con = 0;
            for (int i = 0; i < p.size(); i++) {
                //modificado para marcado negativo de w
                if (padre.marcado[i] >= pre[i][j] || padre.marcado[i] == -1) {//recordemos pre[p.size][t.size]
                    con++;
                }
            }
            //System.out.println(con);
            if (con == p.size()) {//si marcado mayoriza a pre se realiza el disparo del marcado
                //System.out.println("Marcado generado");
                int[] markTemp = new int[p.size()];
                for (int i = 0; i < p.size(); i++) {
                    if (padre.marcado[i] == -1) {
                        markTemp[i] = padre.marcado[i];
                    } else {
                        markTemp[i] = padre.marcado[i] - pre[i][j] + pos[i][j];
                    }
                }

                Nodo temp = new Nodo(markTemp, padre, t.get(j).name);
                mayoriza(temp);
                //verificar si ya existe
                if (isinQ(temp) == null && isinP(temp) == null) {
                    
                    padre.hijos.add(temp);//añadir el hijo

                    LP.add(temp);

                    if(t_disparados.contains(temp.tranDisparada)==false){
                        t_disparados.add(temp.tranDisparada);
                    }   
                    //anadimos a grafo_file para el archivo node1 -> node2 [label="linea1"];
                    grafo_file += padre.homomorfismo() + " -> " + temp.homomorfismo() + "[label=\"" + temp.tranDisparada + "\"];";
                    
                } else {

                    if (!(isinQ(temp) == null)) { //Está en Q
                        padre.hijos.add(isinQ(temp));
                    } else { //Está en P.
                        padre.hijos.add(isinP(temp));
                    }
                    if(t_disparados.contains(temp.tranDisparada)==false){
                        t_disparados.add(temp.tranDisparada);
                    }   
                    System.out.println("Ya existe");
                    grafo_file += padre.homomorfismo() + " -> " + temp.homomorfismo() + "[label=\"" + temp.tranDisparada + "\"];";
                }
            } else {
                padre.Terminal = true;
                //System.out.println(padre.homomorfismo());
                // LibreDeBloqueo= false;
            }

        }
        LQ.add(LP.remove(0));
        for (int j = 0; j < LP.size(); j++) {
            LP.get(j).Print();
        }
        System.out.println("");

    }

    public void mayoriza(Nodo x) {
        Nodo temp = x.padre;
        boolean repetido = false;
        while (temp != null) {
            //hacer comparacion con el padre para ver si mayoriza
            int m[] = x.marcado;
            int n[] = temp.marcado;
            int mayoriza = 0;
            for (int i = 0; i < n.length; i++) {
                if (m[i] >= n[i] || m[i] == -1) {
                    mayoriza++;
                } else if (n[i] == -1) {
                    m[i] = -1;
                    mayoriza++;
                }
            }
            if (mayoriza == n.length) {
                Acotada = false;
                for (int i = 0; i < n.length; i++) {
                    if (m[i] > n[i]) {
                        m[i] = -1;
                    }
                }
                x.setMarcado(m);
                for (Nodo hijo : x.hijos) {
                    if (hijo.homomorfismo().equals(temp.homomorfismo())) {
                        repetido = true;
                    }
                }
                if (repetido == false) {
                    x.hijos.add(temp);
                }
            }
            repetido = false;
            temp = temp.padre;
            }
    }

    public Nodo isinQ(Nodo x) {
        for (int i = 0; i < LQ.size(); i++) {
            if (LQ.get(i).homomorfismo().equals(x.homomorfismo())) {
                //LQ.get(i).homomorfismo().equals(x.homomorfismo())
                LQ.get(i).Duplicado = true;

                return LQ.get(i);

            }

        }
        return null;
    }

    public static boolean esLibreDeBloqueo() {
        boolean deadlock = false;

        for (Nodo n : LQ) { //Checamos si hay algún nodo terminal en el arbol de covertura.
            //if(n.Terminal == true) {
            //  deadlock = true;
            //}
            if (n.hijos.isEmpty()) {
                // System.out.println(n.homomorfismo() + "-" + n.hijos.isEmpty());
                deadlock = true;
            }
        }
        return deadlock;
    }

    public static void esEstrictamenteConservativa() {
        int minit = LQ.get(0).suma;

        for (Nodo n : LQ) { //Checamos si hay algún nodo terminal en el arbol de covertura.
            if (n.tieneW) {
                Conservativa = false;
            } else if (!(n.suma == minit)) {
                Conservativa = false;
            }
        }
    }

    public Nodo isinP(Nodo x) {
        for (int i = 0; i < LP.size(); i++) {
            if (LP.get(i).homomorfismo().equals(x.homomorfismo())) {
                //LQ.get(i).homomorfismo().equals(x.homomorfismo())
                LP.get(i).Duplicado = true;

                return LP.get(i);

            }

        }
        return null;
    }

    public void primerMarcado() {
        Nodo padre = new Nodo(mark, null, "Ninguna");//int [] marcado, Nodo padre, String tran
        LP.add(padre);
        /*generarMarcados(padre);
         generarMarcados(LP.get(0));
         generarMarcados(LP.get(0));
         generarMarcados(LP.get(0));
         generarMarcados(LP.get(0));
         generarMarcados(LP.get(0));
         generarMarcados(LP.get(0));
         generarMarcados(LP.get(0));
         generarMarcados(LP.get(0));*/// Ralizar generacion mientras p sea distinto a vacio
        while (!LP.isEmpty()) {
            generarMarcados(LP.get(0));
        }

        grafo_file += "}";
        try {
            //System.out.println(grafo_file);
            guardar(grafo_file);
        } catch (IOException ex) {
            Logger.getLogger(RedesPetri.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void guardar(String grafo) throws IOException {
        BufferedWriter bw = null;
        File file = new File("grafo.txt");

        if (!file.exists()) {
            file.createNewFile();
        }
        FileWriter fw = new FileWriter(file);
        bw = new BufferedWriter(fw);
        bw.write(grafo);
        bw.close();

        dibujar();
    }

    public void dibujar() {
        try {

            //String dotPath = "C:\\Archivos de programa\\Graphviz 2.28\\bin\\dot.exe";
            String dotPath = "C:\\Program Files (x86)\\Graphviz2.38\\bin\\dot.exe";

            String fileInputPath = "grafo.txt";
            String fileOutputPath = "grafo.pdf";

            String tParam = "-Tpdf";
            String tOParam = "-o";

            String[] cmd = new String[5];
            cmd[0] = dotPath;
            cmd[1] = tParam;
            cmd[2] = fileInputPath;
            cmd[3] = tOParam;
            cmd[4] = fileOutputPath;

            Runtime rt = Runtime.getRuntime();

            rt.exec(cmd);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

       public static ArrayList CalculaTInvariantes(int[][] mi) {
        ArrayList<int[]> invariantsTemp = new ArrayList();//se usa para iterar
        ArrayList<int[]> invariants = new ArrayList();//devuelve t o p -invariantes

        boolean end = true;
//generar lista de vectores para poder iterar
        for (int i = 0; i < mi.length; i++) {
            int[] temp = new int[mi.length + p.size()];
            temp[i] = 1;
            for (int j = 0; j < p.size(); j++) {
                temp[mi.length + j] = mi[i][j];
            }
            invariantsTemp.add(temp);
        }
/////////////////////////////////
        int j = 0; //indica la columna que se esta convirtiendo a 0

        while (!invariantsTemp.isEmpty() && end) {

            //obtener primer vector de la lista
            int index = invariantsTemp.size();
            for (int indi = 0; indi < index; indi++) {

                int m[] = invariantsTemp.get(0);
                //verificamos si ya es un invariante
                int cont = 0;
                for (int k = 0; k < p.size(); k++) {
                    if (m[mi.length + k] == 0) {
                        cont++;
                    }
                }
                if (cont == p.size()) {
                    invariants.add(invariantsTemp.remove(0));
                } else {
                    //mi.length + t.size(), comenzar despues de la matriz identidad
                    int listaSize = invariantsTemp.size();

                    for (int i = 1; i < listaSize; i++) {//i es el renglon
                        int n[] = invariantsTemp.get(i);
                        if (m[mi.length + j] != 0 && m[mi.length + j] + n[mi.length + j] == 0) {
                            //si alguna posicion sumada da 0 se suma el vector
                            int mt1[] = sumaVector(m, n);
                            if (mt1[0] == 5) {
                                end = false;
                            }
                            /*    int c=0;
                             for (int tempo= 0; tempo<t.size();tempo++){
                             if(mt1[mi.length+tempo]==0)
                             c++;
                             }
                             if(c==t.size()){
                             invariants.add(mt1);
                             }else{*/
                            invariantsTemp.add(mt1);//}
                        }

                    }
                    //una vez que se generan los que pueden generarse
                    if (m[mi.length + j] != 0) {
                        invariantsTemp.remove(0);
                    } else {
                        invariantsTemp.add(invariantsTemp.remove(0));
                    }
                }
            }
            ////////////////////////////////////////////   
            for (int i = 0; i < invariantsTemp.size(); i++) {
                int mtemp[] = invariantsTemp.get(i);
                for (int jk = 0; jk < mi.length + p.size(); jk++) {

                    System.out.print(mtemp[jk] + " ");
                }
                System.out.println("");
            }
            System.out.println("");
            j++;
        }//fin del while
        return invariants;
    }
    
    public static ArrayList CalculaPInvariantes(int[][] mi) {
        ArrayList<int[]> invariantsTemp = new ArrayList();//se usa para iterar
        ArrayList<int[]> invariants = new ArrayList();//devuelve t o p -invariantes

        boolean end = true;
//generar lista de vectores para poder iterar
        for (int i = 0; i < mi.length; i++) {
            int[] temp = new int[mi.length + t.size()];
            temp[i] = 1;
            for (int j = 0; j < t.size(); j++) {
                temp[mi.length + j] = mi[i][j];
            }
            invariantsTemp.add(temp);
        }
/////////////////////////////////
        int j = 0; //indica la columna que se esta convirtiendo a 0

        while (!invariantsTemp.isEmpty() && end) {

            //obtener primer vector de la lista
            int index = invariantsTemp.size();
            for (int indi = 0; indi < index; indi++) {

                int m[] = invariantsTemp.get(0);
                //verificamos si ya es un invariante
                int cont = 0;
                for (int k = 0; k < t.size(); k++) {
                    if (m[mi.length + k] == 0) {
                        cont++;
                    }
                }
                if (cont == t.size()) {
                    invariants.add(invariantsTemp.remove(0));
                } else {
                    //mi.length + t.size(), comenzar despues de la matriz identidad
                    int listaSize = invariantsTemp.size();

                    for (int i = 1; i < listaSize; i++) {//i es el renglon
                        int n[] = invariantsTemp.get(i);
                        if (m[mi.length + j] != 0 && m[mi.length + j] + n[mi.length + j] == 0) {
                            //si alguna posicion sumada da 0 se suma el vector
                            int mt1[] = sumaVector(m, n);
                            if (mt1[0] == 5) {
                                end = false;
                            }
                            /*    int c=0;
                             for (int tempo= 0; tempo<t.size();tempo++){
                             if(mt1[mi.length+tempo]==0)
                             c++;
                             }
                             if(c==t.size()){
                             invariants.add(mt1);
                             }else{*/
                            invariantsTemp.add(mt1);//}
                        }

                    }
                    //una vez que se generan los que pueden generarse
                    if (m[mi.length + j] != 0) {
                        invariantsTemp.remove(0);
                    } else {
                        invariantsTemp.add(invariantsTemp.remove(0));
                    }
                }
            }
            ////////////////////////////////////////////   
            for (int i = 0; i < invariantsTemp.size(); i++) {
                int mtemp[] = invariantsTemp.get(i);
                for (int jk = 0; jk < mi.length + t.size(); jk++) {

                    System.out.print(mtemp[jk] + " ");
                }
                System.out.println("");
            }
            System.out.println("");
            j++;
        }//fin del while
        return invariants;
    }

    public static int[] sumaVector(int[] m, int n[]) {
        int z[] = new int[m.length];
        int sum = 0;
        for (int i = 0; i < m.length; i++) {
            // z[i] = m[i] + n[i];
            sum = m[i] + n[i];
            if (sum > 1 && i < mi.length + t.size()) {
                z[0] = 5;
            } else {
                z[i] = sum;
            }
        }
        return z;
    }

    public static int[][] miTranspuesta() {
        int mtran[][] = new int[t.size()][p.size()];
        for (int i = 0; i < p.size(); i++) {
            for (int j = 0; j < t.size(); j++) {
                mtran[j][i] = mi[i][j];
            }
        }
        return mtran;
    }

    public static ArrayList<Nodo> computeGt() {
        ArrayList<Nodo> LQt = new ArrayList<Nodo>();
        for (Nodo n : LQ) { //Para crear la lista transpuesta.
            LQt.add(new Nodo(n.marcado, null, n.tranDisparada));
        }

        for (Nodo nodo : LQ) {
            for (Nodo hijo : nodo.hijos) {

                Nodo temp = getNodoT(hijo.marcado, LQt);
                temp.hijos.add(getNodoT(nodo.marcado, LQt));

            }
        }

        System.out.println();
        return LQt;
    }

    public static Nodo getNodoT(int[] marcado, ArrayList<Nodo> LQt) {
        Nodo aux = null;
        for (Nodo nodo : LQt) {
            if (nodo.marcado == marcado) {
                aux = nodo;
            }
        }
        return aux;
    }
    public static int DFS(ArrayList<Nodo> G, Nodo u) {
        time = 0;
        for (Nodo nodo : G) {
            nodo.padre = null;
            nodo.color = "WHITE";
        }
        //for(Nodo nodoTemp : copiaLQ){
        //   if(nodoTemp.color.equals("WHITE"));{
        int transDisp[];
        DFS_Visit(u);
        //   }
        //}
        return 0;
    }

    public static int DFS_Visit(Nodo nodoTemp) {
        String trans ="";
        time = time + 1;
        nodoTemp.tiempoInicial = time;
        nodoTemp.color = "GRAY";
        for (int h = 0; h < nodoTemp.hijos.size(); h++) {
            if (nodoTemp.hijos.get(h).color.equals("WHITE")) {
                nodoTemp.padre = nodoTemp;
                DFS_Visit(nodoTemp.hijos.get(h));
            }
        }
        nodoTemp.color = "BLACK";
        time = time + 1;
        nodoTemp.tiempoFinal = time;
        copiaLQdesendiente.add(nodoTemp);
        
        
        return 0;
    }
    
    public static void esViva(){
        //copiaLQ = new ArrayList<>(LQ);
        //DFS(copiaLQ, copiaLQ.get(0));
        ArrayList<Nodo> G_transpuesta = computeGt();
        Nodo nodoInicialGt = getNodoT(LQ.get(0).marcado, G_transpuesta);
        DFS(G_transpuesta, nodoInicialGt);
        if (copiaLQdesendiente.size()==LQ.size()){
            System.out.print("Es reversible\n");
            if (t_disparados.size()==t.size()) {
                System.out.print("Es viva\n");
            }
            else{
                System.out.print("No es viva\n");
            }
        }
        else{
            System.out.print("No es reversible y no es viva\n");
        }
    }
    
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        RedesPetri m = new RedesPetri();

        m.leerArchivo("redes/no acotada 3 estados.xml");
        //generar nodos
        //eliminar comentario para poder realizar las pruebas
        m.primerMarcado();

        ArrayList<Nodo> LQt = computeGt();

        //System.out.println(LQ.size());
        ArrayList<int[]> inva = CalculaPInvariantes(mi);
        System.out.println("P-invariantes");
        if (!inva.isEmpty()) {
            for (int i = 0; i < inva.size(); i++) {
                int mtem[] = inva.get(i);
                for (int j = 0; j < mi.length; j++) {
                    System.out.print(mtem[j] + " ");
                }
                System.out.println("");
            }
        }
        else{
            System.out.println("No se obtuvieron p-invariantes");
        }
        //  System.out.println(LQ.get(0).hijos.get(0).homomorfismo());
        // System.out.println(LQ.get(0).hijos.get(1).homomorfismo());

        // System.out.println(LQ.get(9).homomorfismo()+" "+LQ.get(9).hijos.size());
        int transi[][] = miTranspuesta();
        System.out.println("Calculo de t invariantes");
        ArrayList<int[]> tinva = CalculaTInvariantes(transi);
        System.out.println("t-invariantes");
        int ctaRepetitiva = 0;
        if (!tinva.isEmpty()){
            for (int i = 0; i < tinva.size(); i++) {
                int mtem[] = tinva.get(i);
                for (int j = 0; j < t.size(); j++) {
                     System.out.print(mtem[j] + " ");
                    if (mtem[j] == 1) {
                        ctaRepetitiva++;
                    }
                }
                System.out.println("");
            }
        }
         else{
            System.out.println("No se obtuvieron t-invariantes");
        }
        if (ctaRepetitiva == t.size()) {
            Repetitiva = true;
        }
        if (Acotada) {
            System.out.println("Acotada");
        } else {
            System.out.println("No Acotada");
        }
        LibreDeBloqueo = !esLibreDeBloqueo();
        if (LibreDeBloqueo) {
            System.out.println("Libre de bloqueo");
        } else {
            System.out.println("No Libre de bloqueo");
        }
        //ver si es conservativa
        esEstrictamenteConservativa();
        if (Conservativa) {
            System.out.println("Estrictamente conservativa");
        } else {
            System.out.println("No es conservativa");
        }
        if (Repetitiva) {
            System.out.println("Si es repetitiva");
        } else {
            System.out.println("No es repetitiva");
        }
        esViva();
        /* for (int i = 0; i < t.size(); i++) {
         for (int j = 0; j < p.size(); j++) {
         System.out.print(transi[i][j]);
         }System.out.println("");
         }*/
        /*for (int i = 0; i < copiaLQdesendiente.size(); i++) {
            System.out.println("-------  " + copiaLQdesendiente.get(i).homomorfismo() 
                    + " -----t inicial  " + copiaLQdesendiente.get(i).tiempoInicial
                    + "------ t final  " + copiaLQdesendiente.get(i).tiempoFinal);
            //System.out.println("estado  " + LQ.get(i).homomorfismo());
        }
        for (int i = 0; i < LQ.size(); i++) {
            System.out.println("estado  " + LQ.get(i).homomorfismo());
        }
        for (int j = 0; j < copiaLQdesendiente.size(); j++) {
            System.out.println("estadoc  " + copiaLQdesendiente.get(j).homomorfismo());
        }
        for (int d = 0; d < t_disparados.size(); d++) {
            System.out.println("transiciones disparadas-- " + t_disparados.get(d));
        }*/
    }
}
