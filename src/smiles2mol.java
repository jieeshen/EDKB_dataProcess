/**
 * Created with IntelliJ IDEA.
 * User: JShen
 * Date: 12/12/12
 * Time: 2:46 PM
 * To change this template use File | Settings | File Templates.
 */
/**
 * Created with IntelliJ IDEA.
 * User: JShen
 * Date: 12/12/12
 * Time: 11:15 AM
 * This program is used to get molecule structure information from a table containing CAS information. It will write
 * a sdf file of all outputted molecules and a table of all the data. If the entry can be converted to strcutre, the
 * smiles, inchi, inchikey information will be added to it.
 *
 * It depends on the CATUS of NCI and ACTOR in local machine.
 */
import chemaxon.formats.MolExporter;
import chemaxon.formats.MolImporter;
import chemaxon.struc.Molecule;

import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;


public class smiles2mol {

    public static void usage() throws Exception{
        System.out.println("Usage:");
        System.out.println("\tjava smiles2sdf -i INPUTFILE -o OUTPUTSDFFILE [-smiles SMILESCOLUMNNAME -s SEPARATOR -q]");
        System.out.println("\tparameters:");
        System.out.println("\t\t-i\tinput text file with multiple columns");
        System.out.println("\t\t-o\toutput sdf file");
        System.out.println("\t\t-cas\tthe column title of SMILES in the input file [Default: \"SMILES\"]");
        System.out.println("\t\t-s\tthe separator char in the input file [Default: \"\t\"]");
        System.out.println("\t\t-q\tusing Mysql to query Actor[Default is off]\n\n");
    }


    public static ArrayList<String []> readList(String fileName, String separator) throws Exception {
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(fileName)));
        String line = null;
        ArrayList<String[]> outList = new ArrayList<String[]>();
        int i=0;
        while((line=br.readLine())!=null){
            String[] data=line.split(separator);
            outList.add(data);
            i++;
        }
        return outList;
    }

    public static void main(String args[]) throws Exception{
        if (args.length<0 | args.length>9){
            usage();
            System.exit(1);
        }
        int i=0;
        String inputFile="C:\\JShen\\Research\\EDKB\\ER\\DECA\\121211\\UT_data_EDKB_smiles2.txt";
        String outSdFile="C:\\JShen\\Research\\EDKB\\ER\\DECA\\121211\\uterotrophic2_data_EDKB.sdf";
        String outputFile="C:\\JShen\\Research\\EDKB\\ER\\DECA\\121211\\uterotrophic2_mol.txt";
        String smilesTitle="SMILES";
        String separator="\t";
        ArrayList<String> passThroughOpts=new ArrayList<String>();


        while (i<args.length){
            if (args[i].equals("-h")){
                i++;
                usage();
                System.exit(1);
            }else if (args[i].equals("-i")){
                i++;
                inputFile=args[i];
            }else if (args[i].equals("-o")){
                i++;
                outSdFile=args[i];
            }else if (args[i].equals("-smiles")){
                i++;
                smilesTitle=args[i];
            }else if (args[i].equals("-s")){
                i++;
                separator=args[i];
            }else{
                passThroughOpts.add(args[i]);
            }
            i++;
        }

        ArrayList<String[]> dataMatrix=readList(inputFile,separator);
        String[] titles=dataMatrix.get(0);
        int smilesLoc=0;
        for(i=0;i<titles.length;i++){
            if (titles[i].equals(smilesTitle)){
                smilesLoc=i;
                break;
            }
        }
        int noInp=dataMatrix.size()-1;
        int noSmiles=0;
        int noUniqSmi=0;
        int noIllSmi=0;
        int noTransSmi=0;
        MolExporter me=new MolExporter(outSdFile,"sdf");
        PrintWriter pw=new PrintWriter(new OutputStreamWriter(new FileOutputStream(outputFile)),true);
        Molecule mol=new Molecule();
        String smilesStr=new String();
        HashSet<Integer> casSet = new HashSet<Integer>();
        String cas=new String();



        for(i=1;i<=noInp;i++){
            String[] dataString=dataMatrix.get(i);
            smilesStr=dataMatrix.get(i)[smilesLoc];

            for (int j=0;j<titles.length;j++){
                try{
                    pw.printf("%s\t", dataMatrix.get(i)[j]);
                }catch (Exception e){
                    pw.printf("%s\t", "");
                }
            }



            try{
                mol=MolImporter.importMol(smilesStr);
            }catch(Exception e){
                noIllSmi++;
                pw.println();
                continue;
            }


            String inchistr=MolExporter.exportToFormat(mol,"inchi").split("=")[1].replace("\n","");
            mol.setProperty("INCHI",inchistr);
            pw.printf("%s\t", inchistr);
            String inchikeystr=MolExporter.exportToFormat(mol,"inchikey").split("=")[1];
            mol.setProperty("INCHIKEY",inchikeystr);
            pw.printf("%s\t", inchikeystr);
            noTransSmi++;
            me.write(mol);

            pw.println();
        }

        me.close();
        System.out.println();
        System.out.println();
        System.out.printf("The total input entry is %d;\n", noInp);
        System.out.printf("The total input cas is %d;\n",noSmiles);
        System.out.printf("The total illegal cas is %d;\n",noIllSmi);
        System.out.printf("%d CAS have been transformed.\n",noTransSmi);
    }
}

