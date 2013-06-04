/**
 * Created with IntelliJ IDEA.
 * User: JShen
 * Date: 1/18/13
 * Time: 10:41 AM
 * This is a program for LogP calculation. The input is a SDF file with multiple mols, the output will be
 * a text file with properties including several logP values.
 */

import chemaxon.formats.MolExporter;
import chemaxon.formats.MolImporter;
import chemaxon.marvin.calculations.IUPACNamingPlugin;
import chemaxon.marvin.calculations.logPPlugin;
import chemaxon.struc.Molecule;

import java.io.*;
import java.util.ArrayList;
import java.util.Properties;

public class logPcalc {
    static String inputFile=new String();
    static String outputFile=new String();

    public static void usage() throws Exception{
        System.out.println("Usage:");
        System.out.println("\tjava logPcalc -i INPUTSDFFILE -o OUTPUTFILE ");
        System.out.println();
        System.out.println("\tparameters:");
        System.out.println("\t\t-h\t");
        System.out.println("\t\t-i\tinput sdf file");
        System.out.println("\t\t-o\toutput text file");
        System.out.println();
        System.out.println("\tlogP calculate methods:");
        System.out.println("\t\tVG: the calculation method derived from ");
        System.out.println("\t\t\t J. Chem. Inf. Comput. Sci., 1989, 29, 163-172");
        System.out.println("\t\tKLOP: logP data from Klopman's paper (J.Chem.Inf.Comput.Sci., 1994, 34, 752)");
        System.out.println("\t\tPHYSPROP: logP data from PHYSPROP© database is used.");
        System.out.println();
        System.out.println("\tAuthor: Jie Shen (jie.shen@fda.hhs.gov). 01/18/2013");
    }

    public static void initialize(String args[]) throws Exception{
        if (args.length!=4){
            usage();
            System.exit(1);
        }
        int i=0;
        ArrayList<String> passThroughOpts=new ArrayList<String>();
        while (i<args.length){
            if (args.length<1){
                i++;
                usage();
                System.exit(1);
            }else if (args[i].equals("-h")){
                i++;
                usage();
                System.exit(1);
            }else if (args[i].equals("-i")){
                i++;
                inputFile=args[i];
            }else if (args[i].equals("-o")){
                i++;
                outputFile=args[i];
            }else{
                passThroughOpts.add(args[i]);
            }
            i++;
        }

    }
    public static double calcLopP (Molecule m, int methodInt) throws Exception {
        Properties params = new Properties();
        params.put("type","logP");
        logPPlugin plugin = new logPPlugin();
        plugin.setlogPMethod(methodInt); //get logP value from physprop database
        plugin.setMolecule(m);
        plugin.run();
        double logp=plugin.getlogPTrue();
        return logp;
    }

    public static String[] getNames(Molecule m) throws Exception{
        IUPACNamingPlugin plugin = new IUPACNamingPlugin();
        plugin.setMolecule(m);
        plugin.run();
        String[] nameList = new String[2];
        try{
            nameList[0]= plugin.getPreferredIUPACName();
            nameList[1]= plugin.getTraditionalName();
        }catch (Exception e){
            nameList[0]="";
            nameList[1]="";
        }
        return nameList;
    }

    public static void main(String args[]) throws Exception{
        initialize(args);
        Molecule mol;
        MolImporter mi=new MolImporter(inputFile);
        PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(outputFile)),true);
        pw.printf("ID\t" +
                "MolTitle\t" +
                "VGlogP\t" +
                "KLogP\t" +
                "PhysPropsLopP\t" +
                "NAME\t" +
                "SMILES\n");
        int i=0;
        while((mol=mi.read())!=null){
            i++;
            String molID=mol.getName();
            String molName=getNames(mol)[1];
            Double klogp=calcLopP(mol,2);
            Double physlogp=calcLopP(mol,3);
            Double vglogp=calcLopP(mol,1);
            String smileString=MolExporter.exportToFormat(mol,"smiles:u");
            pw.printf("%d\t%s\t%f\t%f\t%f\t%s\t%s\n", i,molID,klogp,physlogp,vglogp,molName,smileString);

        }
        pw.flush();
        pw.close();
    }
}
