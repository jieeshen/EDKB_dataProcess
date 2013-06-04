/**
 * Created with IntelliJ IDEA.
 * User: jshen
 * Date: 7/9/12
 * Time: 9:03 PM
 * Description: This class will reads a sdf file and write out a table with specified properties
 *
 * This version is generate the txt file for STEVE on 2012.07.10
 *
 */

import chemaxon.formats.MolExporter;
import chemaxon.formats.MolImporter;
import chemaxon.marvin.io.MPropHandler;
import chemaxon.reaction.Standardizer;
import chemaxon.struc.Molecule;
import chemaxon.util.MolHandler;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

public class sd2table {
    final public static String INPUTFILE="C:\\Documents and Settings\\jshen\\My Documents\\Research\\TCKB\\TCKB_DEMO\\joined_120525_2.sdf";
    final public static String OUTFILE="C:\\Documents and Settings\\jshen\\My Documents\\Research\\TCKB\\forSteve\\updatedMolTable.txt";

    public static void main(String args[]) throws Exception {
        Molecule mol;
        MolImporter mi=new MolImporter(INPUTFILE);
        PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream("C:\\Temp_data3\\TCKBupdatedMol.txt")),true);
        PrintWriter pw_cpmd_struct = new PrintWriter(new OutputStreamWriter(new FileOutputStream("C:\\Temp_data3\\TCKBupdtMolStruct.txt")),true);
        pw.printf("ID\tNAME\t" +
                "SMILES\t" +
                "SMILES_BAD\t" +
                "MOLECULAR_FORMULA\t" +
                "MOLECULAR_WEIGHT\t" +
                "CAS\t" +
                "LOG_KOW\tLOG_KOW_METHOD\t" +
                "BOILING_POINT_MIN\tBOILING_POINT_MAX\tBOILING_POINT_METHOD\t" +
                "MELTING_POINT_MIN\tMELTING_POINT_MAX\tMELTING_POINT_METHOD\n");
        pw_cpmd_struct.printf("ID\tMOL\n");
        while((mol=mi.read())!=null){
            String idStr=mol.getProperty("NoCN2");
            int id=Math.round(Float.parseFloat(idStr));
            pw.printf("%d\t",id);
            pw_cpmd_struct.printf("%d\t",id);
            String molString=MolExporter.exportToFormat(mol,"mol");
            pw_cpmd_struct.print(molString.replace("\n","\\n")+"\n");

            pw.printf("%s\t",mol.getProperty("Name"));
            pw.printf("%s\t",mol.getProperty("UNIQUE_SMILES"));
            try{
                if (mol.getProperty("USER_SUPPLIED_SMILES").equals(mol.getProperty("modified_smiles"))){
                    pw.printf("\t");
                }else{
                    pw.printf("%s\t", mol.getProperty("modified_smiles"));
                }
            }catch (Exception e){
                pw.printf("\t");
            }
            try{
                pw.printf("%s\t",mol.getProperty("MF"));
            }catch (Exception e){
                pw.printf("\t");
            }
            try{
                MolHandler mh= new MolHandler(mol);
                pw.printf("%f\t",mh.calcMolWeight());
            }catch (Exception e){
                pw.printf("\t");
            }
            try{
                pw.printf("%s\t", MPropHandler.convertToString(mol.properties(),"CAS"));
            }catch (Exception e){
                pw.printf("\t");
            }
            try{
                pw.printf("%s\t", MPropHandler.convertToString(mol.properties(),"logKowValue"));
            }catch (Exception e){
                pw.printf("\t");
            }
            try{
                pw.printf("%s\t", MPropHandler.convertToString(mol.properties(),"logKowMethod"));
            }catch (Exception e){
                pw.printf("\t");
            }
            try{
                pw.printf("%s\t", MPropHandler.convertToString(mol.properties(),"bpvMN"));
            }catch (Exception e){
                pw.printf("\t");
            }
            try{
                pw.printf("%s\t", MPropHandler.convertToString(mol.properties(),"bpvMX"));
            }catch (Exception e){
                pw.printf("\t");
            }
            try{
                pw.printf("%s\t", MPropHandler.convertToString(mol.properties(),"BoilingPointMethod"));
            }catch (Exception e){
                pw.printf("\t");
            }
            try{
                pw.printf("%s\t", MPropHandler.convertToString(mol.properties(),"mpvMN"));
            }catch (Exception e){
                pw.printf("\t");
            }
            try{
                pw.printf("%s\t", MPropHandler.convertToString(mol.properties(),"mpvMX"));
            }catch (Exception e){
                pw.printf("\t");
            }
            try{
                pw.printf("%s\n", MPropHandler.convertToString(mol.properties(),"MeltingPointMethod"));
            }catch (Exception e){
                pw.printf("\t");
            }
        }
        mi.close();
        pw.close();
        pw_cpmd_struct.close();


    }

}
