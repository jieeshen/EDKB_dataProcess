import chemaxon.formats.MolExporter;
import chemaxon.formats.MolImporter;
import chemaxon.marvin.calculations.IUPACNamingPlugin;
import chemaxon.marvin.calculations.TopologyAnalyserPlugin;
import chemaxon.marvin.calculations.logPPlugin;
import chemaxon.struc.Molecule;
import chemaxon.util.MolHandler;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

/**
 * Created with IntelliJ IDEA.
 * By: Jie Shen @ NCTR, FDA
 * Date: 6/14/12
 * Time: 9:47 AM
 * <p/>
 * Description: This class is used to generate a sdf file for SYMYX import. It queries data from the mysql database
 *              (erdb_12) and write out the sdf file with proper format.
 *              First, it query all the assays and activities of each molecule, then write properties
 */
public class writeSdfFromDb {
    final public static String MOLLISTFILE="C:\\Temp_data2\\sorted_molregno.txt";
    final public static String OUTPUTSDFILE="C:\\Temp_data2\\erdb_12_symyx.sdf";
    final public static String driver="com.mysql.jdbc.Driver";
    final public static String url="jdbc:mysql://localhost:3306/chembl_13";
    final public static String user="root";
    final public static String password="jsh1234";
    final public static HashMap<String,Integer> sp = new HashMap<String, Integer>(){
        {
            put("Homo sapiens",1);//human
            put("Mus musculus",2);//mouse
            put("Rattus norvegicus",3);//rat
            put("Bos taurus",4);//calf
            put("monkey",5); //
            put("Escherichia coli",6);//
            put("Gallus gallus",7);//chicken
            put("Lizard",8);//
            put("Oryctolagus cuniculus",9);//rabit
            put("Ovis aries",10);//lamb
            put("RainbowTrout",11);//trout
            put("unknown",0);
        }
    };




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

    public static int calRotBonds (Molecule m) throws Exception {
        TopologyAnalyserPlugin plugin = new TopologyAnalyserPlugin();
        plugin.setMolecule(m);
        plugin.run();
        return plugin.getRotatableBondCount();
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

    public static String clob2string(Clob cb) throws Exception{
        StringBuffer sb = new StringBuffer();
        String string;

        BufferedReader br = new BufferedReader(cb.getCharacterStream());

        while ((string=br.readLine())!=null){
            sb.append(string);
        }
        return sb.toString();
    }

    public static void main(String args[]) throws Exception {

        MolExporter me = new MolExporter(OUTPUTSDFILE, "sdf");// define the sdf output file

        ArrayList molList=readList(MOLLISTFILE); // read sorded molregno from the list

        // set connection to mysql db.
        Class.forName(driver);
        Connection conn = DriverManager.getConnection(url, user, password);
        Statement st= conn.createStatement();

        // hander molecule one by one
        for(int i=1;i<=molList.size();i++){
//        for(int i=72;i<=74;i++){
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

            // got calculated properties
            double logp=physpropLopP(m);
            double mw=calMolWeight(m);
            int nrb=calRotBonds(m);
            String[] nameList=getNames(m); //{IUPAC_NAME, Traditional_Name}
            m.setProperty("id",Integer.toString(i));
            m.setProperty("traditional_name",nameList[1]);
            m.setProperty("iupac_name",nameList[0]);
            m.setProperty("inchi",inchi);
            m.setProperty("inchikey",inchi_key);
            m.setProperty("canonical_smiles",smiles);
            m.setProperty("molformula",mf);
            m.setProperty("mw",Double.toString(mw));
            m.setProperty("logp",Double.toString(logp));
            m.setProperty("number_of_rotatable_bonds", Integer.toString(nrb));

            // initialize the index variables
            int index_at_b=0, index_at_r=0, index_at_c=0; // assay type
            int index_pt_a=0, index_pt_b=0, index_pt_u=0; // protein type
            int index_pl_f=0, index_pl_l=0, index_pl_u=0; // protein length
            int index_sp_human=0, index_sp_mouse=0, index_sp_rat=0, index_sp_calf=0;
            int index_sp_monkey=0, index_sp_ecoli=0, index_sp_chicken=0;
            int index_sp_lizard=0, index_sp_rabbit=0, index_sp_lamb=0;
            int index_sp_trout=0; // species

            // initialize the count of each activity categories
            int i_b_a_f=0, i_b_a_l=0, i_b_a_u=0; // binding assay, ER alpha, protein length
            int i_b_b_f=0, i_b_b_l=0, i_b_b_u=0; // binding assay, ER beta, protein length
            int i_b_u=0; // binding assay, unknown ER subtype
            int i_r_a=0, i_r_b=0, i_r_u=0; // reporter gene assay, ER subytpes
            int i_c=0; // cell proliferation assay

            //initialize the description of activity (assay and reference) pools
            String descPool=new String();
            int i_activity=0;

            // got activity information
            String sqlquery_Activities="select c.molregno, c.molfile, a.standard_type, a.standard_value, " +
                    "a.standard_units, d.assay_type, d.assay_id, d.target, d.length, d.assay_organism, " +
                    "d.description, e.ref_type, e.url, e.journal, e.year, e.volume, e.first_page from " +
                    "er_invitro_activities a, er_invitro_compound_structures c, er_invitro_assays d, er_invitro_refs e " +
                    "where a.molregno=c.molregno and a.assay_id=d.assay_id and e.ref_id=a.ref_id " +
                    "and a.molregno="+molregno+";";
            ResultSet rsA=st.executeQuery(sqlquery_Activities);
            while(rsA.next()){
                i_activity++;

                String value=rsA.getString("standard_value");
                String value_desciption=rsA.getString("standard_type")+":"+rsA.getString("standard_units")+" ["
                        +Integer.toString(i_activity)+"]";

                // format acivity reference
                String reference=rsA.getString("description");
                switch (rsA.getString("ref_type").charAt(0)){
                    case 'j':
                        reference=reference+".  "+rsA.getString("journal")+" "+rsA.getString("year")+","+
                                rsA.getString("volume")+":"+rsA.getString("first_page")+".\n";
                        break;
                    case 'd':
                    case 'w':
                        reference=reference+"."+rsA.getString("url")+" "+rsA.getString("year")+".\n";
                        break;
                    case 'p':
                        reference=reference+"."+rsA.getString("journal")+" "+rsA.getString("year")+".\n";
                        break;
                    default:
                        System.out.print("can not recognize the reference type...");
                }
                descPool=descPool+"["+Integer.toString(i_activity)+"] "+reference;

                // initialize the property titles of the sdf file
                String valueTitle=new String();
                String descTitle=new String();

                String pl = new String();
                String pt = new String();

                String spString = new String();
                //String refTitle=new String();


                // set species index
                spString=rsA.getString("assay_organism");
                if (spString == null) spString = "unknown";
                //System.out.println(spString+" "+sp.get(spString));
                switch (sp.get(spString)){
                    case 1:
                        index_sp_human=1;
                        break;
                    case 2:
                        index_sp_mouse=1;
                        break;
                    case 3:
                        index_sp_rat=1;
                        break;
                    case 4:
                        index_sp_calf=1;
                        break;
                    case 5:
                        index_sp_monkey=1;
                        break;
                    case 6:
                        index_sp_ecoli=1;
                        break;
                    case 7:
                        index_sp_chicken=1;
                        break;
                    case 8:
                        index_sp_lizard=1;
                        break;
                    case 9:
                        index_sp_rabbit=1;
                        break;
                    case 10:
                        index_sp_lamb=1;
                        break;
                    case 11:
                        index_sp_trout=1;
                        break;
                }



                switch (rsA.getString("assay_type").charAt(0)) {
                    case 'B': // binding assay
                        index_at_b=1;
                        pt= rsA.getString("target");
                        if (pt==null) pt="unknown";
                        switch (pt.charAt(0)){
                            case 'a': // ER alpha
                                index_pt_a=1;
                                pl=rsA.getString("length");
                                if (pl==null) pl="unknown";
                                switch(pl.charAt(0)){
                                    case 'F':
                                        index_pl_f=1;
                                        i_b_a_f++;
                                        valueTitle="b_a_f_value_"+Integer.toString(i_b_a_f);
                                        descTitle="b_a_f_description_"+Integer.toString(i_b_a_f);
                                        //String refTitle="b_a_f_ref_"+Integer.toString(i_b_a_f);
                                        m.setProperty(valueTitle,value);
                                        m.setProperty(descTitle,value_desciption);
                                        //m.setProperty(refTitle,Integer.toString(i_activity));
                                        break;
                                    case 'L':
                                        index_pl_l=1;
                                        i_b_a_l++;
                                        valueTitle="b_a_l_value_"+Integer.toString(i_b_a_l);
                                        descTitle="b_a_l_description_"+Integer.toString(i_b_a_l);
                                        //refTitle="b_a_l_ref_"+Integer.toString(i_b_a_l);
                                        m.setProperty(valueTitle,value);
                                        m.setProperty(descTitle,value_desciption);
                                        //m.setProperty(refTitle,Integer.toString(i_activity));
                                        break;
                                    default:
                                        index_pl_u=1;
                                        i_b_a_u++;
                                        valueTitle="b_a_u_value_"+Integer.toString(i_b_a_u);
                                        descTitle="b_a_u_description_"+Integer.toString(i_b_a_u);
                                        //refTitle="b_a_l_ref_"+Integer.toString(i_b_a_l);
                                        m.setProperty(valueTitle,value);
                                        m.setProperty(descTitle,value_desciption);
                                        //m.setProperty(refTitle,Integer.toString(i_activity));
                                        break;
                                }
                                break;
                            case 'b': // ER beta
                                index_pt_b=1;
                                pl=rsA.getString("length");
                                if (pl==null) pl="unknown";
                                switch(pl.charAt(0)){
                                    case 'F':
                                        index_pl_f=1;
                                        i_b_b_f++;
                                        valueTitle="b_b_f_value_"+Integer.toString(i_b_b_f);
                                        descTitle="b_b_f_description_"+Integer.toString(i_b_b_f);
                                        //String refTitle="b_a_f_ref_"+Integer.toString(i_b_a_f);
                                        m.setProperty(valueTitle,value);
                                        m.setProperty(descTitle,value_desciption);
                                        //m.setProperty(refTitle,Integer.toString(i_activity));
                                        break;
                                    case 'L':
                                        index_pl_l=1;
                                        i_b_b_l++;
                                        valueTitle="b_b_l_value_"+Integer.toString(i_b_b_l);
                                        descTitle="b_b_l_description_"+Integer.toString(i_b_b_l);
                                        //refTitle="b_a_l_ref_"+Integer.toString(i_b_a_l);
                                        m.setProperty(valueTitle,value);
                                        m.setProperty(descTitle,value_desciption);
                                        //m.setProperty(refTitle,Integer.toString(i_activity));
                                        break;
                                    default:
                                        index_pl_u=1;
                                        i_b_b_u++;
                                        valueTitle="b_b_u_value_"+Integer.toString(i_b_b_u);
                                        descTitle="b_b_u_description_"+Integer.toString(i_b_b_u);
                                        //refTitle="b_a_l_ref_"+Integer.toString(i_b_a_l);
                                        m.setProperty(valueTitle,value);
                                        m.setProperty(descTitle,value_desciption);
                                        //m.setProperty(refTitle,Integer.toString(i_activity));
                                        break;
                                }
                                break;
                            default: // ER type unspecified
                                index_pt_u=1;
                                i_b_u++;
                                valueTitle="b_u_value_"+Integer.toString(i_b_u);
                                descTitle="b_u_description_"+Integer.toString(i_b_u);
                                //refTitle="b_a_l_ref_"+Integer.toString(i_b_a_l);
                                m.setProperty(valueTitle,value);
                                m.setProperty(descTitle,value_desciption);
                                break;
                        }
                        break;
                    case 'R': // reporter gene assay
                        index_at_r=1;
                        pt= rsA.getString("target");
                        if (pt==null) pt="unknown";
                        switch (pt.charAt(0)){
                            case 'a': // ER alpha
                                index_pt_a=1;
                                i_r_a++;
                                valueTitle="r_a_value_"+Integer.toString(i_r_a);
                                descTitle="r_a_description_"+Integer.toString(i_r_a);
                                //String refTitle="b_a_f_ref_"+Integer.toString(i_b_a_f);
                                m.setProperty(valueTitle,value);
                                m.setProperty(descTitle,value_desciption);
                                break;
                            case 'b': // ER beta
                                index_pt_b=1;
                                i_r_b++;
                                valueTitle="r_b_value_"+Integer.toString(i_r_b);
                                descTitle="r_b_description_"+Integer.toString(i_r_b);
                                //String refTitle="b_a_f_ref_"+Integer.toString(i_b_a_f);
                                m.setProperty(valueTitle,value);
                                m.setProperty(descTitle,value_desciption);
                                break;
                            default: // ER type unspecified
                                index_pt_u=1;
                                i_r_u++;
                                valueTitle="r_u_value_"+Integer.toString(i_r_u);
                                descTitle="r_u_description_"+Integer.toString(i_r_u);
                                //refTitle="b_a_l_ref_"+Integer.toString(i_b_a_l);
                                m.setProperty(valueTitle,value);
                                m.setProperty(descTitle,value_desciption);
                                break;
                        }
                        break;
                    case 'C': // cell proliferation assay
                        index_at_c=1;
                        i_c++;
                        valueTitle="c_value_"+Integer.toString(i_c);
                        descTitle="c_description_"+Integer.toString(i_c);
                        //refTitle="b_a_l_ref_"+Integer.toString(i_b_a_l);
                        m.setProperty(valueTitle,value);
                        m.setProperty(descTitle,value_desciption);
                        break;
                    default:
                        System.out.print("can not recognize the assay type");
                        break;
                }
            }
            // set index properties

            m.setProperty("index_at_b",Integer.toString(index_at_b));
            m.setProperty("index_at_r",Integer.toString(index_at_r));
            m.setProperty("index_at_c",Integer.toString(index_at_c));

            m.setProperty("index_pt_a",Integer.toString(index_pt_a));
            m.setProperty("index_pt_b",Integer.toString(index_pt_b));
            m.setProperty("index_pt_u",Integer.toString(index_pt_u));

            m.setProperty("index_pl_f",Integer.toString(index_at_b));
            m.setProperty("index_pl_l",Integer.toString(index_at_b));
            m.setProperty("index_pl_u",Integer.toString(index_at_b));

            m.setProperty("index_sp_human",Integer.toString(index_sp_human));
            m.setProperty("index_sp_mouse",Integer.toString(index_sp_mouse));
            m.setProperty("index_sp_rat",Integer.toString(index_sp_rat));
            m.setProperty("index_sp_calf",Integer.toString(index_sp_calf));
            m.setProperty("index_sp_monkey",Integer.toString(index_sp_monkey));
            m.setProperty("index_sp_ecoli",Integer.toString(index_sp_ecoli));
            m.setProperty("index_sp_chicken",Integer.toString(index_sp_chicken));
            m.setProperty("index_sp_lizard",Integer.toString(index_sp_lizard));
            m.setProperty("index_sp_rabbit",Integer.toString(index_sp_rabbit));
            m.setProperty("index_sp_lamb",Integer.toString(index_sp_lamb));
            m.setProperty("index_sp_trout",Integer.toString(index_sp_trout));

            // set activity references
            m.setProperty("activity_references",descPool);
            me.write(m);
            System.out.println(i+":"+i_b_a_f+" "+i_b_a_l+" "+i_b_a_u+" "+i_b_b_f+" "+i_b_b_l+" "+i_b_b_u+" "+i_b_u+" "+
            i_r_a+" "+i_r_b+" "+i_r_u+" "+i_c);
        }
    }


}
