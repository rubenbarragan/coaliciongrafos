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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
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
    static ArrayList<ArrayList<Nodo>> copiaLQdesendiente = new ArrayList<>();//nodos ya procesados
    static ArrayList<String> tInCFC = new ArrayList<>();
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

        //System.out.println("Matriz de incidencia");
        String cad = "   ";
        for (int i = 0; i < t.size(); i++) {
            cad += t.get(i).name + "|";
        }
        //System.out.println(cad);

        for (int i = 0; i < p.size(); i++) {
            //System.out.print(p.get(i).nombre + "|");
            for (int j = 0; j < t.size(); j++) {
                //System.out.print(mi[i][j] + " |");
            }
            //System.out.println();
        }

        //System.out.println("\nPRE");
        cad = "   ";
        for (int i = 0; i < t.size(); i++) {
            cad += t.get(i).name + "|";
        }
        //System.out.println(cad);

        for (int i = 0; i < p.size(); i++) {
            //System.out.print(p.get(i).nombre + "|");
            for (int j = 0; j < t.size(); j++) {
                //System.out.print(pre[i][j] + " |");
            }
            //System.out.println();
        }

        //System.out.println("\nPOS");
        cad = "   ";
        for (int i = 0; i < t.size(); i++) {
            cad += t.get(i).name + "|";
        }
        //System.out.println(cad);

        for (int i = 0; i < p.size(); i++) {
            //System.out.print(p.get(i).nombre + "|");
            for (int j = 0; j < t.size(); j++) {
                //System.out.print(pos[i][j] + " |");
            }
            //System.out.println();
        }
        marcadoInicial();
    }

    public void marcadoInicial() {
        mark = new int[p.size()];
        //System.out.println("\nMarcado inicial");
        for (int i = 0; i < p.size(); i++) {
            mark[i] = p.get(i).marcado;
            //System.out.println(p.get(i).nombre + "|" + mark[i]);
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

                    for (String t : temp.tranDisparada) {
                        if (t_disparados.contains(t) == false) {
                            t_disparados.add(t);
                        }
                    }
                    //anadimos a grafo_file para el archivo node1 -> node2 [label="linea1"];
                    grafo_file += padre.homomorfismo() + " -> " + temp.homomorfismo() + "[label=\"" + temp.tranDisparada + "\"];";

                } else {

                    if (!(isinQ(temp) == null)) { //Está en Q
                        //padre.hijos.add(isinQ(temp));
                        Nodo n = isInHijos(padre, temp);
                        if (n == null) {
                            padre.hijos.add(isinQ(temp));
                        } else {
                            n.addTrans(t.get(j).name);
                        }
                    } else { //Está en P.
//                        padre.hijos.add(isinP(temp));
                        Nodo n = isInHijos(padre, temp);
                        if (n == null) {
                            padre.hijos.add(isinP(temp));
                        } else {
                            n.addTrans(t.get(j).name);
                        }
                    }
                    for (String t : temp.tranDisparada) {
                        if (t_disparados.contains(t) == false) {
                            t_disparados.add(t);
                        }
                    }
                    //System.out.println("Ya existe");
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

    public Nodo isInHijos(Nodo padre, Nodo nodoabuscar) {
        for (Nodo h : padre.hijos) {
            if (nodoabuscar.homomorfismo().equals(h.homomorfismo())) {
                return h;
            }
        }
        return null;
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
                //Acotada = false;
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
                Acotada = false;
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
            String fileOutputPath = "grafo2.png";

            String tParam = "-Tpng";
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

    public static void imprimirLista(ArrayList<int[]> lista) {
        for (int[] actualFila : lista) {
            for (int j = 0; j < actualFila.length; j++) {
                System.out.print(actualFila[j] + "   ");
            }
            System.out.print("\n");
        }
    }

    public static ArrayList CalculaTInvariantes(int[][] mi) {
        ArrayList<int[]> invariantsTemp = new ArrayList();//se usa para iterar
        ArrayList<int[]> invariants = new ArrayList();//devuelve t o p -invariantes

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
        int cont = 0;
        int multiploActual = 1;
        int multiploTemp = 1;
        int filaTemp[] = new int[mi.length + p.size()];
        int filaActual[] = new int[mi.length + p.size()];
        int filasAeliminar[] = new int[invariantsTemp.size() * invariantsTemp.size()];
        Arrays.fill(filasAeliminar, 0);

        //imprimirLista(invariantsTemp);
        for (int columna = mi.length; columna < mi.length + p.size(); columna++) {
            for (int tmpo = 0; tmpo < invariantsTemp.size(); tmpo++) {
                filaTemp = invariantsTemp.get(tmpo);
                for (int k = 0; k < p.size(); k++) {
                    if (filaTemp[mi.length + k] == 0) {
                        cont++;
                    }
                }
                if (cont == p.size()) {
                    invariants.add(invariantsTemp.remove(tmpo));
                }
                cont = 0;
            }
            for (int fila = 0; fila < invariantsTemp.size(); fila++) {
                filaActual = invariantsTemp.get(fila);
                if (filaActual[columna] != 0) {
                    for (int i = fila + 1; i < invariantsTemp.size(); i++) {
                        filaTemp = invariantsTemp.get(i);

                        if (filaActual[columna] > 0 && filaTemp[columna] < 0) {
                            if (filaActual[columna] % filaTemp[columna] == 0) {
                                multiploTemp = filaActual[columna] / filaTemp[columna];
                                if (multiploTemp < 0) {
                                    multiploTemp = multiploTemp * (-1);
                                }
                                int mt1[] = sumaVector(multiploActual, filaActual, multiploTemp, filaTemp);
                                invariantsTemp.add(mt1);
                                filasAeliminar[fila] = 1;
                                filasAeliminar[i] = 1;
                            } else {
                                if (filaActual[columna] < 0) {
                                    multiploTemp = filaActual[columna] * (-1);
                                    multiploActual = filaTemp[columna];
                                    int mt1[] = sumaVector(multiploActual, filaActual, multiploTemp, filaTemp);
                                    invariantsTemp.add(mt1);
                                    filasAeliminar[fila] = 1;
                                    filasAeliminar[i] = 1;
                                } else {
                                    multiploTemp = filaActual[columna];
                                    multiploActual = filaTemp[columna] * (-1);
                                    int mt1[] = sumaVector(multiploActual, filaActual, multiploTemp, filaTemp);
                                    invariantsTemp.add(mt1);
                                    filasAeliminar[fila] = 1;
                                    filasAeliminar[i] = 1;
                                }
                            }
                        } else if (filaActual[columna] < 0 && filaTemp[columna] > 0) {
                            if (filaTemp[columna] % filaActual[columna] == 0) {
                                multiploActual = filaTemp[columna] / filaActual[columna];
                                if (multiploActual < 0) {
                                    multiploActual = multiploActual * (-1);
                                }
                                int mt1[] = sumaVector(multiploActual, filaActual, multiploTemp, filaTemp);
                                invariantsTemp.add(mt1);
                                filasAeliminar[fila] = 1;
                                filasAeliminar[i] = 1;
                            } else {
                                if (filaActual[columna] < 0) {
                                    multiploTemp = filaActual[columna] * (-1);
                                    multiploActual = filaTemp[columna];
                                    int mt1[] = sumaVector(multiploActual, filaActual, multiploTemp, filaTemp);
                                    invariantsTemp.add(mt1);
                                    filasAeliminar[fila] = 1;
                                    filasAeliminar[i] = 1;
                                } else {
                                    multiploTemp = filaActual[columna];
                                    multiploActual = filaTemp[columna] * (-1);
                                    int mt1[] = sumaVector(multiploActual, filaActual, multiploTemp, filaTemp);
                                    invariantsTemp.add(mt1);
                                    filasAeliminar[fila] = 1;
                                    filasAeliminar[i] = 1;
                                }
                            }
                        }
                        multiploActual = 1;
                        multiploTemp = 1;
                    }
                }
            }
            //System.out.print("creando las filas de la suma\n");
            //imprimirLista(invariantsTemp);
            //eliminar las filas utilizadas
            for (int x = filasAeliminar.length - 1; x >= 0; x--) {
                if (filasAeliminar[x] == 1) {
                    invariantsTemp.remove(x);
                }
            }
            Arrays.fill(filasAeliminar, 0);
            //eliminar las filas diferentes de cero que no pudiero ser quitadas
            //System.out.print("eliminando las filas usadas....\n");
            //imprimirLista(invariantsTemp);
            for (int y = invariantsTemp.size() - 1; y >= 0; y--) {
                filaTemp = invariantsTemp.get(y);
                if (filaTemp[columna] != 0) {
                    invariantsTemp.remove(y);
                }
            }
            for (int tmpo = 0; tmpo < invariantsTemp.size(); tmpo++) {
                filaTemp = invariantsTemp.get(tmpo);
                for (int k = 0; k < p.size(); k++) {
                    if (filaTemp[mi.length + k] == 0) {
                        cont++;
                    }
                }
                if (cont == p.size()) {
                    invariants.add(invariantsTemp.remove(tmpo));
                }
                cont = 0;
            }
        }//fin del while
        return invariants;
    }

    public static ArrayList CalculaPInvariantes(int[][] mi) {
        ArrayList<int[]> invariantsTemp = new ArrayList();//se usa para iterar
        ArrayList<int[]> invariants = new ArrayList();//devuelve t o p -invariantes

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
        int cont = 0;
        int multiploActual = 1;
        int multiploTemp = 1;
        int filaTemp[] = new int[mi.length + t.size()];
        int filaActual[] = new int[mi.length + t.size()];
        int filasAeliminar[] = new int[invariantsTemp.size() * invariantsTemp.size()];
        Arrays.fill(filasAeliminar, 0);

        for (int columna = mi.length; columna < mi.length + t.size(); columna++) {
            for (int tmpo = 0; tmpo < invariantsTemp.size(); tmpo++) {
                filaTemp = invariantsTemp.get(tmpo);
                for (int k = 0; k < t.size(); k++) {
                    if (filaTemp[mi.length + k] == 0) {
                        cont++;
                    }
                }
                if (cont == t.size()) {
                    invariants.add(invariantsTemp.remove(tmpo));
                }
                cont = 0;
            }
            for (int fila = 0; fila < invariantsTemp.size(); fila++) {
                filaActual = invariantsTemp.get(fila);
                if (filaActual[columna] != 0) {
                    for (int i = fila + 1; i < invariantsTemp.size(); i++) {
                        filaTemp = invariantsTemp.get(i);
                        if (filaActual[columna] > 0 && filaTemp[columna] < 0) {
                            if (filaActual[columna] % filaTemp[columna] == 0) {
                                multiploTemp = filaActual[columna] / filaTemp[columna];
                                if (multiploTemp < 0) {
                                    multiploTemp = multiploTemp * (-1);
                                }
                                int mt1[] = sumaVector(multiploActual, filaActual, multiploTemp, filaTemp);
                                invariantsTemp.add(mt1);
                                filasAeliminar[fila] = 1;
                                filasAeliminar[i] = 1;
                            } else {
                                if (filaActual[columna] < 0) {
                                    multiploTemp = filaActual[columna] * (-1);
                                    multiploActual = filaTemp[columna];
                                    int mt1[] = sumaVector(multiploActual, filaActual, multiploTemp, filaTemp);
                                    invariantsTemp.add(mt1);
                                    filasAeliminar[fila] = 1;
                                    filasAeliminar[i] = 1;
                                } else {
                                    multiploTemp = filaActual[columna];
                                    multiploActual = filaTemp[columna] * (-1);
                                    int mt1[] = sumaVector(multiploActual, filaActual, multiploTemp, filaTemp);
                                    invariantsTemp.add(mt1);
                                    filasAeliminar[fila] = 1;
                                    filasAeliminar[i] = 1;
                                }
                            }
                        } else if (filaActual[columna] < 0 && filaTemp[columna] > 0) {
                            if (filaTemp[columna] % filaActual[columna] == 0) {
                                multiploActual = filaTemp[columna] / filaActual[columna];
                                if (multiploActual < 0) {
                                    multiploActual = multiploActual * (-1);
                                }
                                int mt1[] = sumaVector(multiploActual, filaActual, multiploTemp, filaTemp);
                                invariantsTemp.add(mt1);
                                filasAeliminar[fila] = 1;
                                filasAeliminar[i] = 1;
                            } else {
                                if (filaActual[columna] < 0) {
                                    multiploTemp = filaActual[columna] * (-1);
                                    multiploActual = filaTemp[columna];
                                    int mt1[] = sumaVector(multiploActual, filaActual, multiploTemp, filaTemp);
                                    invariantsTemp.add(mt1);
                                    filasAeliminar[fila] = 1;
                                    filasAeliminar[i] = 1;
                                } else {
                                    multiploTemp = filaActual[columna];
                                    multiploActual = filaTemp[columna] * (-1);
                                    int mt1[] = sumaVector(multiploActual, filaActual, multiploTemp, filaTemp);
                                    invariantsTemp.add(mt1);
                                    filasAeliminar[fila] = 1;
                                    filasAeliminar[i] = 1;
                                }
                            }
                        }
                        multiploActual = 1;
                        multiploTemp = 1;
                    }
                }
            }
            //eliminar las filas utilizadas
            for (int x = filasAeliminar.length - 1; x >= 0; x--) {
                if (filasAeliminar[x] == 1) {
                    invariantsTemp.remove(x);
                }
            }
            Arrays.fill(filasAeliminar, 0);
            //eliminar las filas diferentes de cero que no pudiero ser quitadas
            for (int y = 0; y < invariantsTemp.size(); y++) {
                filaTemp = invariantsTemp.get(y);
                if (filaTemp[columna] != 0) {
                    invariantsTemp.remove(y);
                }
            }
        }//fin del while
        return invariants;
    }

    public static int[] sumaVector(int multiploM, int[] m, int multiploN, int n[]) {
        int z[] = new int[m.length];
        for (int i = 0; i < m.length; i++) {
            z[i] = multiploM * m[i] + multiploN * n[i];
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

    public static ArrayList<Nodo> computeGt(ArrayList<Nodo> LQTiempos) {
        ArrayList<Nodo> LQt = new ArrayList<Nodo>();
        ArrayList<Nodo> LQt2 = new ArrayList<Nodo>();
        for (Nodo n : LQTiempos) { //Para crear la lista transpuesta.
            LQt.add(new Nodo(n.marcado, null, n.tranDisparada));
        }

        for (Nodo nodo : LQTiempos) {
            for (Nodo hijo : nodo.hijos) {
                Nodo temp = getNodoT(hijo.marcado, LQt);
                temp.hijos.add(getNodoT(nodo.marcado, LQt));
            }
        }

        //aqui poner lo de acomodas los nodos de  lqt
        return LQt;
    }

    public static boolean esComFueCon() {
        boolean CFC = true;
        int transInCFC[] = new int[t.size()];
        boolean temp = true;
        
        for (ArrayList<Nodo> listaCFC : copiaLQdesendiente) {
            for (Nodo nodo : listaCFC) {
                for (Nodo hijos : nodo.hijos) {
                    for (String transi : hijos.tranDisparada) {
                        if (transi != "Ninguna") {
                            String c = transi.substring(1);
                            int p = Integer.parseInt(c);
                            transInCFC[p] = 1;
                        }

                    }
                }
            }
            for (int z = 0; z < t.size(); z++) {
                if (transInCFC[z] != 1) {
                    temp = false;
                }
            }
            if (temp == true) {
                CFC = true;
            }
        }
        return CFC;
    }

    public static ArrayList<Nodo> acomodar(ArrayList<Nodo> desordenada, ArrayList<Nodo> ordenada) {

        ArrayList<Nodo> transAcomodada = new ArrayList<>();
        ArrayList<Nodo> transAcomodadafinal = new ArrayList<>();
        
        for (int i = ordenada.size()-1; i >=0; i--) {
            transAcomodada.add(ordenada.get(i));
        }
        
        for (int i = 0; i < ordenada.size(); i++) {
            transAcomodadafinal.add(getNodoT(transAcomodada.get(i).marcado, desordenada));
        }

        return transAcomodadafinal;

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
        tInCFC.clear();
        int indiceCFC = 0;
        copiaLQdesendiente.clear();
        for (Nodo nodo : G) {
            nodo.padre = null;
            nodo.color = "WHITE";
        }
        for (Nodo nodoTemp : G) {
            if ("WHITE".equals(nodoTemp.color)) {
                tInCFC.add("cambio");
                copiaLQdesendiente.add(new ArrayList<Nodo>());
                DFS_Visit(G, nodoTemp, indiceCFC);
                indiceCFC++;
            }
        }

        return 0;
    }

    public static int DFS_Visit(ArrayList<Nodo> G, Nodo nodoTemp, int indiceCFCtmp) {
        String trans = "";
        time = time + 1;
        nodoTemp.tiempoInicial = time;
        nodoTemp.color = "GRAY";
        for (int h = 0; h < nodoTemp.hijos.size(); h++) {
            if (nodoTemp.hijos.get(h).color.equals("WHITE")) {
                nodoTemp.hijos.get(h).padre = nodoTemp;
                tInCFC.add(nodoTemp.homomorfismo());
                DFS_Visit(G, nodoTemp.hijos.get(h), indiceCFCtmp);
            }
        }
        nodoTemp.color = "BLACK";
        time = time + 1;
        nodoTemp.tiempoFinal = time;
        copiaLQdesendiente.get(indiceCFCtmp).add(nodoTemp);

        return 0;
    }

    public static int numeroTenTinvariant() {
        int transi[][] = miTranspuesta();
        ArrayList<int[]> tinva = CalculaTInvariantes(transi);
        int sumTinv[] = new int[t.size()];
        int numTenTinv = 0;

        for (int[] tinv : tinva) {
            for (int i = 0; i < tinv.length; i++) {
                if (tinv[i] > 0) {
                    sumTinv[i] += tinv[i];
                }
            }
        }
        for (int j = 0; j < sumTinv.length; j++) {
            if (sumTinv[j] > 0) {
                numTenTinv++;
            }
        }
        return numTenTinv;
    }

    public static void esReversible() {
        ArrayList<Nodo> copiaLQ = new ArrayList<>(LQ);
        DFS(copiaLQ, copiaLQ.get(0));
        ArrayList<Nodo> G_transpuesta = computeGt(copiaLQdesendiente.get(0));
        ArrayList<Nodo> G_transpuestaDesentiente= acomodar(G_transpuesta, copiaLQdesendiente.get(0));
        System.out.print("segunfaaaa\n");

        DFS(G_transpuestaDesentiente, G_transpuestaDesentiente.get(0));
        if (copiaLQdesendiente.size() == 1) {
            System.out.print("Es reversible\n");
        } else {
            System.out.print("No es reversible\n");
        }
    }

    public static void esViva() {
        if (esComFueCon()) {
            System.out.print("Es viva\n");
        } else {
            System.out.print("No es viva\n");
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        RedesPetri m = new RedesPetri();

        m.leerArchivo("redes/Petri net 1.xml");
        //generar nodos
        //eliminar comentario para poder realizar las pruebas
        m.primerMarcado();

        //ArrayList<Nodo> LQt = computeGt();
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
        } else {
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
        if (!tinva.isEmpty()) {
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
        } else {
            System.out.println("No se obtuvieron t-invariantes");
        }
        if (ctaRepetitiva == t.size()) {
            Repetitiva = true;
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
        if (Acotada) {
            System.out.println("Acotada");
        } else {
            System.out.println("No Acotada");
        }
        if (Repetitiva) {
            System.out.println("Si es repetitiva");
        } else {
            System.out.println("No es repetitiva");
        }
        esReversible();
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
//        for (int i = 0; i < tInCFC.size(); i++) {
//            System.out.print(tInCFC.get(i).toString() + "\n");
//        }
//        for (Nodo padre : LQ) {
//            System.out.print("Marcado:\n " + padre.homomorfismo() + "hijos:\n");
//            for (Nodo hijo : padre.hijos) {
//                System.out.print(hijo.homomorfismo() + "\n");
//            }
//        }

    }
}
