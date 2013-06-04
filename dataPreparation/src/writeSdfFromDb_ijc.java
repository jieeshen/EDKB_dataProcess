import chemaxon.formats.MolExporter;
import chemaxon.formats.MolImporter;
import chemaxon.marvin.calculations.IUPACNamingPlugin;
import chemaxon.marvin.calculations.TopologyAnalyserPlugin;
import chemaxon.marvin.calculations.logPPlugin;
import chemaxon.reaction.Standardizer;
import chemaxon.struc.Molecule;
import chemaxon.util.MolHandler;

import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

/**
 *
 * Created with IntelliJ IDEA.
 * By: Jie Shen @ NCTR, FDA
 * Date: 7/23/12
 * Time: 3:10 PM
 * Description: This class is used to generate a sdf file for Instant JChem import. It queries data from the mysql database
 *              (erdb_12) and write out the sdf file with proper format.
 *              First, it query all the assays and activities of each molecule, then write properties
 */
public class writeSdfFromDb_ijc {
    final public static String MOLLISTFILE="C:\\JShen\\Research\\EDKB\\ER\\DEAC\\120213\\for_IJC\\molregnoList.txt";
    final public static String OUTPUTSDFILE="C:\\JShen\\Research\\EDKB\\ER\\DEAC\\120213\\for_IJC\\deac_130213.sdf";
    final public static String MOLINFOFILE="C:\\JShen\\Research\\EDKB\\ER\\DEAC\\120213\\for_IJC\\deac_130213_molinfo.txt";
    final public static String driver="com.mysql.jdbc.Driver";
    final public static String url="jdbc:mysql://localhost:3306/deac_130213(withctd)";
    final public static String user="root";
    final public static String password="jsh1234";





    public static ArrayList readList(String fileName) throws Exception {
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(fileName)));
        String data = null;
        ArrayList outList = new ArrayList();
        while((data=br.readLine())!=null){
            outList.add(data);
        }
        return outList;
    }

    public static double physpropLopP (Molecule m) throws Exception {
        Properties params = new Properties();
        params.put("type","logP");
        logPPlugin plugin = new logPPlugin();
        plugin.setlogPMethod(logPPlugin.METHOD_PHYS); //get logP value from physprop database
        plugin.setMolecule(m);
        plugin.run();
        double logp=plugin.getlogPTrue();
        return logp;
    }

    public static double calMolWeight (Molecule m) throws Exception {
        MolHandler mh=new MolHandler(m);
        return mh.calcMolWeight();
    }

    public static Integer[] calCounts (Molecule m) throws Exception {
        TopologyAnalyserPlugin plugin = new TopologyAnalyserPlugin();
        plugin.setMolecule(m);
        plugin.run();
        Integer[] ncounts= new Integer[3];
        ncounts[0]=plugin.getRotatableBondCount();
        ncounts[1]=plugin.getAllAtomCount();
        ncounts[2]=plugin.getBondCount();
        return ncounts;
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

        ArrayList molList=readList(MOLLISTFILE); // read sorded molregno from the list

        PrintWriter pw=new PrintWriter(new OutputStreamWriter(new FileOutputStream(MOLINFOFILE)),true);
        pw.println("id\tmolregno\tname\tiupac_name\tsmiles\tMolFormula\tMW\tLogP\tnAtoms\tnBonds");

        // set connection to mysql db.
        Class.forName(driver);
        Connection conn = DriverManager.getConnection(url, user,"");
        Statement st= conn.createStatement();

        // hander molecule one by one

        for(int i=1;i<=molList.size();i++){
//        for(int i=1;i<=2;i++){
            String molregno=molList.get(i-1).toString(); //got the molregno

            // got the molecule information
            String sqlquery_Mol="select * from er_invitro_compound_structures where molregno="+molregno+";";
            ResultSet rsM=st.executeQuery(sqlquery_Mol);
            if (rsM.next()){

            } else {
                System.out.print(molregno+"not found in db... Something wrong....");
            }
            //Clob molfileClob=rsM.getClob("molfile");
            //String molfileString=clob2string(molfileClob);
            Molecule m = MolImporter.importMol(rsM.getString("molfile").replace("\\n","\n"),"mol");
            String inchi=rsM.getString("standard_inchi");
            String inchi_key=rsM.getString("standard_inchi_key");
            String smiles=rsM.getString("canonical_smiles");
            String mf=rsM.getString("molformula");


            // got CAS information
            String sqlquery_cas="select cas_rn from er_invitro_cas_mapping where molregno="+molregno+";";
            ResultSet rsC=st.executeQuery(sqlquery_cas);
            String casstr=new String();
            while (rsC.next()){
                casstr=casstr+rsC.getString("cas_rn")+"; ";
            }
            //Clob molfileClob=rsM.getClob("molfile");
            //String molfileString=clob2string(molfileClob);
            if (casstr.length()>0) m.setProperty("cas",casstr);

            // got calculated properties
            double logp=physpropLopP(m);
            double mw=calMolWeight(m);
            Integer[] ncounts=calCounts(m);
            String[] nameList=getNames(m); //{IUPAC_NAME, Traditional_Name}
            m.setProperty("molregno",molregno);
            m.setProperty("id",Integer.toString(i));
            m.setProperty("traditional_name",nameList[1]);
            m.setProperty("iupac_name",nameList[0]);
            m.setProperty("inchi",inchi);
            m.setProperty("inchikey",inchi_key);
            m.setProperty("canonical_smiles",smiles);
            m.setProperty("molformula",mf);
            m.setProperty("mw",Double.toString(mw));
            m.setProperty("logp",Double.toString(logp));
            m.setProperty("number_of_rotatable_bonds", Integer.toString(ncounts[0]));
            pw.printf("%d\t%s\t%s\t%s\t%s\t%s\t%f\t%f\t%d\t%d\n",i,molregno,nameList[1],nameList[0],smiles,mf,mw,
                    logp,ncounts[1],ncounts[2]);

            Standardizer standardizer = new Standardizer(new File("C:\\JShen\\Research\\EDKB\\ER\\ijc\\121009\\std.xml"));
            standardizer.setFinalClean();
            standardizer.standardize(m);

            me.write(m);
            System.out.println(i+"\t"+molregno+"\t"+nameList[1]+"\t"+smiles+"\t"+mf+"\t"+mw+"\t"+logp
                    +"\t"+ncounts[1]+"\t"+ncounts[2]);
        }
        me.flush();
        pw.flush();
        me.close();
        pw.close();

    }


}
