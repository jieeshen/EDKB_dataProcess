/**
 * Created with IntelliJ IDEA.
 * User: JShen
 * Date: 12/18/12
 * Time: 10:56 PM
 * This program is used to calculate the concordance (cd) of each molecule in the DECA. The cd is
 * defined as ([number of active activities]-[number of inactive activities])
 * /([number of active activities]+[number of inactive activities])
 */

import chemaxon.formats.MolExporter;
import chemaxon.formats.MolImporter;
import chemaxon.marvin.calculations.IUPACNamingPlugin;
import chemaxon.marvin.calculations.TopologyAnalyserPlugin;
import chemaxon.marvin.calculations.logPPlugin;
import chemaxon.struc.Molecule;
import chemaxon.util.MolHandler;

import java.io.*;
import java.sql.*;
import java.util.*;

public class Concordance {
    final public static String MOLLISTFILE="C:\\JShen\\Research\\EDKB\\ER\\DECA\\121211\\molregnoList.txt";
    final public static String OUTPUTSDFILE="C:\\JShen\\Research\\EDKB\\ER\\DEAC\\130219_QSAR\\exported_data\\deca_cd.sdf";
    final public static String OUTPUTSDFILE_BA="C:\\JShen\\Research\\EDKB\\ER\\DEAC\\130219_QSAR\\exported_data\\deca_ba.sdf";
    final public static String OUTPUTSDFILE_BI="C:\\JShen\\Research\\EDKB\\ER\\DEAC\\130219_QSAR\\exported_data\\deca_bi.sdf";
    final public static String OUTPUTSDFILE_RA="C:\\JShen\\Research\\EDKB\\ER\\DEAC\\130219_QSAR\\exported_data\\deca_ra.sdf";
    final public static String OUTPUTSDFILE_RI="C:\\JShen\\Research\\EDKB\\ER\\DEAC\\130219_QSAR\\exported_data\\deca_ri.sdf";
    final public static String OUTPUTSDFILE_CA="C:\\JShen\\Research\\EDKB\\ER\\DEAC\\130219_QSAR\\exported_data\\deca_ca.sdf";
    final public static String OUTPUTSDFILE_CI="C:\\JShen\\Research\\EDKB\\ER\\DEAC\\130219_QSAR\\exported_data\\deca_ci.sdf";
    final public static String OUTPUTSDFILE_VA="C:\\JShen\\Research\\EDKB\\ER\\DEAC\\130219_QSAR\\exported_data\\deca_va.sdf";
    final public static String OUTPUTSDFILE_VI="C:\\JShen\\Research\\EDKB\\ER\\DEAC\\130219_QSAR\\exported_data\\deca_vi.sdf";
    final public static String OUTPUTFILE="C:\\JShen\\Research\\EDKB\\ER\\DEAC\\130219_QSAR\\exported_data\\deca_cdIndex.txt";
    final public static String driver="com.mysql.jdbc.Driver";
    final public static String url="jdbc:mysql://localhost:3306/deac_130211";
    final public static String user="root";
    final public static String password="";

    public static ArrayList readList(String fileName) throws Exception {
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(fileName)));
        String data = null;
        ArrayList outList = new ArrayList();
        while((data=br.readLine())!=null){
            outList.add(data);
        }
        return outList;
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

    public static void main(String args[]) throws Exception {

        MolExporter me = new MolExporter(OUTPUTSDFILE, "sdf");// define the sdf output file
        MolExporter meBA = new MolExporter(OUTPUTSDFILE_BA, "sdf");
        MolExporter meBI = new MolExporter(OUTPUTSDFILE_BI, "sdf");
        MolExporter meRA = new MolExporter(OUTPUTSDFILE_RA, "sdf");
        MolExporter meRI = new MolExporter(OUTPUTSDFILE_RI, "sdf");
        MolExporter meCA = new MolExporter(OUTPUTSDFILE_CA, "sdf");
        MolExporter meCI = new MolExporter(OUTPUTSDFILE_CI, "sdf");
        MolExporter meVA = new MolExporter(OUTPUTSDFILE_VA, "sdf");
        MolExporter meVI = new MolExporter(OUTPUTSDFILE_VI, "sdf");
        ArrayList molList=readList(MOLLISTFILE); // read sorded molregno from the list

        // set connection to mysql db.
        Class.forName(driver);
        Connection conn = DriverManager.getConnection(url, user, password);
        Statement st= conn.createStatement();
        // set output file
        PrintWriter pw=new PrintWriter(new OutputStreamWriter(new FileOutputStream(OUTPUTFILE)),true);
        pw.printf("ID\tmolregno\tName\tSMILES\tcd_binding\tNo_binding\tcd_reporter_gene\tNo_rg\tcd_cell_proliferation\tno_cp\tcd_in_vivo\tno_iv\n");

        String sqBinding=new String();
        String sqReporter=new String();
        String sqCell=new String();
        String sqVivo=new String();

        int active=0;
        int inactive=0;
        // hander molecule one by one
        for(int i=1;i<=molList.size();i++){
            String molregno=molList.get(i-1).toString(); //got the molregno

            // got the molecule information
            String sqlquery_Mol="select * from er_invitro_compound_structures where molregno="+molregno+";";
            ResultSet rsM=st.executeQuery(sqlquery_Mol);
            if (rsM.next()){

            } else {
                System.out.print(molregno+"not found in db... Something wrong....");
            }

            Molecule m = MolImporter.importMol(rsM.getString("molfile").replace("\\n","\n"),"mol");
            String inchi=rsM.getString("standard_inchi");
            String inchi_key=rsM.getString("standard_inchi_key");
            String smiles=rsM.getString("canonical_smiles");
            String mf=rsM.getString("molformula");

            active=0;
            inactive=0;
            sqBinding="select a.standard_value from er_invitro_activities a, er_invitro_assays b " +
                    "where a.assay_id=b.assay_id AND b.assay_type=\"Binding\" AND molregno="+molregno+";";
            ResultSet sqB=st.executeQuery(sqBinding);
            while (sqB.next()){
                if (Float.valueOf(sqB.getString("a.standard_value")) < -9999) {
                    inactive++;
                } else{
                    active++;
                }
            }
            Double cd_binding=Math.abs((active - inactive) * 1.0)/(active+inactive);
            int an_binding=(active+inactive);
            Double aI_binding=active*1.0/(active+inactive);



            active=0;
            inactive=0;
            sqReporter="select a.standard_value from er_invitro_activities a, er_invitro_assays b " +
                    "where a.assay_id=b.assay_id AND b.assay_type=\"Reporter Gene\" AND molregno="+molregno+";";
            ResultSet sqR=st.executeQuery(sqReporter);
            while (sqR.next()){
                if (Float.valueOf(sqR.getString("a.standard_value")) < -9999) {
                    inactive++;
                } else{
                    active++;
                }
            }
            Double cd_rg=Math.abs((active-inactive)*1.0)/(active+inactive);
            int an_rg=(active+inactive);
            Double aI_rg=active*1.0/(active+inactive);

            active=0;
            inactive=0;
            sqCell="select a.standard_value from er_invitro_activities a, er_invitro_assays b " +
                    "where a.assay_id=b.assay_id AND b.assay_type=\"Cell Proliferation\" AND molregno="+molregno+";";
            ResultSet sqC=st.executeQuery(sqCell);
            while (sqC.next()){
                if (Float.valueOf(sqC.getString("a.standard_value")) < -9999) {
                    inactive++;
                } else{
                    active++;
                }
            }
            Double cd_cp=Math.abs((active-inactive)*1.0)/(active+inactive);
            int an_cp=(active+inactive);
            Double aI_cp=active*1.0/(active+inactive);

            active=0;
            inactive=0;
            sqVivo="select a.standard_value from er_invitro_activities a, er_invitro_assays b " +
                    "where a.assay_id=b.assay_id AND b.assay_type=\"In Vivo\" AND molregno="+molregno+";";
            ResultSet sqV=st.executeQuery(sqVivo);
            while (sqV.next()){
                if (Float.valueOf(sqV.getString("a.standard_value")) < -9999) {
                    inactive++;
                } else{
                    active++;
                }
            }
            Double cd_iv=Math.abs((active-inactive)*1.0)/(active+inactive);
            int an_iv=(active+inactive);
            Double aI_iv=active*1.0/(active+inactive);

            String[] nameList=getNames(m); //{IUPAC_NAME, Traditional_Name}
            m.setName(Integer.toString(i));
            m.setProperty("id",Integer.toString(i));
            m.setProperty("traditional_name",nameList[1]);
            m.setProperty("iupac_name",nameList[0]);
            m.setProperty("inchi",inchi);
            m.setProperty("inchikey",inchi_key);
            m.setProperty("canonical_smiles",smiles);
            m.setProperty("molformula",mf);
            m.setProperty("aI_b",Double.toString(cd_binding));
            m.setProperty("aI_rg",Double.toString(cd_rg));
            m.setProperty("aI_cp",Double.toString(cd_cp));
            m.setProperty("aI_iv",Double.toString(cd_iv));

            me.write(m);
            if (aI_binding<0.001){
                meBI.write(m);
            }
            if (aI_binding>0.99){
                meBA.write(m);
            }
            if (aI_rg<0.001){
                meRI.write(m);
            }
            if (aI_rg>0.99){
                meRA.write(m);
            }
            if (aI_cp<0.001){
                meCI.write(m);
            }
            if (aI_cp>0.99){
                meCA.write(m);
            }
            if (aI_iv<0.001){
                meVI.write(m);
            }
            if (aI_iv>0.99){
                meVA.write(m);
            }
            pw.printf("%d\t%s\t%s\t%s\t%f\t%d\t%f\t%d\t%f\t%d\t%f\t%d\n",i,molregno,nameList[1],smiles,
                    cd_binding,an_binding,cd_rg,an_rg,cd_cp,an_cp,cd_iv,an_iv);
        }
    }
}
