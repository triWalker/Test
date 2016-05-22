package semanticActions;

import grammar.GrammarSymbol;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.*;

import symbolTable.SymbolTableEntry;

public class Quadruples {


    private Vector<String[]> Quadruple;
    private int nextQuad;

    public Quadruples() {
        Quadruple = new Vector<String[]>();
        nextQuad = 0;
        String[] dummy_quadruple = new String [4];
        dummy_quadruple[0]=dummy_quadruple[1]=dummy_quadruple[2]=dummy_quadruple[3]= null;
        Quadruple.add(nextQuad,dummy_quadruple);
        nextQuad++;
    }

    public String getField(int quadIndex, int field) {
        return Quadruple.elementAt(quadIndex)[field];
    }

    public void setField(int quadIndex, int index, String field) {
        Quadruple.elementAt(quadIndex)[index] = field;
    }

    public int getNextQuad() {
        return nextQuad;
    }

    public void incrementNextQuad() {
        nextQuad++;
    }

    public String[] getQuad(int index) {
        return (String []) Quadruple.elementAt(index);
    }

    public void addQuad(String[] quad) {
        Quadruple.add(nextQuad, quad);
        nextQuad++;
    }

    public void print() {
        int quadLabel = 1;
        String separator;

        System.out.println("CODE");
        Enumeration<String[]> e = this.Quadruple.elements();
        e.nextElement();

        while (e.hasMoreElements()) {
            String[] quad = e.nextElement();
            separator = " ";
            System.out.print(quadLabel + ":  " + quad[0]);
            if (quad[1] != null) {
                System.out.print(separator + quad[1]);
            }
            if (quad[2] != null) {
                separator = ", ";
                System.out.print(separator + quad[2]);
            }
            if (quad[3] != null)
                System.out.print(separator + quad[3]);

            System.out.println();
            quadLabel++;
        }
    }

    public void write() throws FileNotFoundException {
        //Write Code To A ".tvi" File
        //Create new file
        File file = new File("/home/trwalker/cs331/code.tvi");
        //Writer will write to the new file
        PrintWriter writer = new PrintWriter(file);

        int quadLabel = 1;
        String separator;

        writer.println("CODE");
        Enumeration<String[]> e = this.Quadruple.elements() ;
        e.nextElement();

        while ( e.hasMoreElements() ) {
            String[] quad = e.nextElement();
            separator = " ";
            writer.print(quadLabel + ":  " + quad[0]);
            if (quad[1] != null) {
                writer.print(separator + quad[1]);
            }
            if (quad[2] != null) {
                separator = ", ";
                writer.print(separator + quad[2]);
            }
            if (quad[3] != null)
                writer.print(separator + quad[3]);

            writer.println();
            quadLabel++;
        }
        // Finished writing to file. Close writer
        writer.close();
    }

}