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
 * Modified on 7/19/2012
 * Updated: do not write year in references
 *
 *
 * Modified on 7/18/2012
 * Updated: Using cleaned mysql database from ERAD_120718, it will generate compact sdf file. In this version,
 *          the assay description is deleted. only the references are left. It also counts the number of activities
 *          in each category.

 * Modified on 7/3/2012
 * Updated: This is for E2 only, it will merge the same activities into one field
 * updated: This program will separate reference by 200.
 *
 *
 * Created with IntelliJ IDEA.
 * By: Jie Shen @ NCTR, FDA
 * Date: 6/14/12
 * Time: 9:47 AM
 * <p/>
 * Description: This class is used to generate a sdf file for SYMYX import. It queries data from the mysql database
 *              (erdb_12) and write out the sdf file with proper format.
 *              First, it query all the assays and activities of each molecule, then write properties
 */
public class writeSdfFromDb_v6 {
    final public static String MOLLISTFILE="C:\\Documents and Settings\\jshen\\My Documents\\Research\\EDKB\\" +
            "ER\\Symyx\\120717\\molreglist.txt";
    final public static String OUTPUTSDFILE="C:\\Documents and Settings\\jshen\\My Documents\\Research\\EDKB\\" +
            "ER\\Symyx\\120717\\erad_120719_compact.sdf";
    final public static String driver="com.mysql.jdbc.Driver";
    final public static String url="jdbc:mysql://localhost:3306/erad_120718";
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
        System.out.printf("id\tmolregno\t#data\t#binding\t#report gene\t#cell proliferation\t#alpha\t#beta\t#unknown\t" +
                "#human\t#mouse\t#rat\t#calf\t#monkey\t#Ecoli\t#chicken\t#lazard\t#rabbit\t#lamp\t#trout\n");

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
            String[] descPool={"","",""};
            int i_activity=0;
            int count_ref=0;
            int rid;

            // got activity information
            String sqlquery_Activities="select c.molregno, c.molfile, a.standard_type, a.standard_value, " +
                    "a.standard_units, d.assay_type, d.assay_id, d.target, d.length, d.assay_organism, " +
                    "d.description, e.ref_type, e.url, e.journal, e.year, e.volume, e.first_page, a.ref_id from " +
                    "er_invitro_activities a, er_invitro_compound_structures c, er_invitro_assays d, er_invitro_refs e " +
                    "where a.molregno=c.molregno and a.assay_id=d.assay_id and e.ref_id=a.ref_id " +
                    "and a.molregno="+molregno+";";
            ResultSet rsA=st.executeQuery(sqlquery_Activities);

            ArrayList<String> b_a_f = new ArrayList<String>();
            ArrayList<String> b_b_f = new ArrayList<String>();
            ArrayList<String> b_a_l = new ArrayList<String>();
            ArrayList<String> b_b_l = new ArrayList<String>();
            ArrayList<String> b_a_u = new ArrayList<String>();
            ArrayList<String> b_b_u = new ArrayList<String>();
            ArrayList<String> b_u = new ArrayList<String>();
            ArrayList<String> r_a = new ArrayList<String>();
            ArrayList<String> r_b = new ArrayList<String>();
            ArrayList<String> r_u = new ArrayList<String>();
            ArrayList<String> c = new ArrayList<String>();
            ArrayList<String> refpool = new ArrayList<String>();

            ArrayList<ArrayList<Integer>> b_a_f_ref = new ArrayList<ArrayList<Integer>>();
            ArrayList<ArrayList<Integer>> b_b_f_ref = new ArrayList<ArrayList<Integer>>();
            ArrayList<ArrayList<Integer>> b_a_l_ref = new ArrayList<ArrayList<Integer>>();
            ArrayList<ArrayList<Integer>> b_b_l_ref = new ArrayList<ArrayList<Integer>>();
            ArrayList<ArrayList<Integer>> b_a_u_ref = new ArrayList<ArrayList<Integer>>();
            ArrayList<ArrayList<Integer>> b_b_u_ref = new ArrayList<ArrayList<Integer>>();
            ArrayList<ArrayList<Integer>> b_u_ref = new ArrayList<ArrayList<Integer>>();
            ArrayList<ArrayList<Integer>> r_a_ref = new ArrayList<ArrayList<Integer>>();
            ArrayList<ArrayList<Integer>> r_b_ref = new ArrayList<ArrayList<Integer>>();
            ArrayList<ArrayList<Integer>> r_u_ref = new ArrayList<ArrayList<Integer>>();
            ArrayList<ArrayList<Integer>> c_ref = new ArrayList<ArrayList<Integer>>();

            // count the activities
            int count_sp_human=0;
            int count_sp_mouse=0;
            int count_sp_rat=0;
            int count_sp_calf=0;
            int count_sp_monkey=0;
            int count_sp_ecoli=0;
            int count_sp_chicken=0;
            int count_sp_lizard=0;
            int count_sp_rabbit=0;
            int count_sp_lamb=0;
            int count_sp_trout=0;


            while(rsA.next()){
                i_activity++;



                String value=rsA.getString("standard_value");
                String value_desciption=rsA.getString("standard_type")+":"+rsA.getString("standard_units");
                //String value_desciption=rsA.getString("standard_type")+":"+rsA.getString("standard_units")+" ["
                //        +Integer.toString(i_activity)+"]";



                String ref_id=rsA.getString("ref_id"); //get ref_id of the reference
                if (refpool.contains(ref_id)){
                    rid=refpool.indexOf(ref_id);
                }else{
                    count_ref++;
                    rid=count_ref;
                    refpool.add(ref_id);
                    // format acivity reference
                    String reference=new String();
                    switch (rsA.getString("ref_type").charAt(0)){
                        case 'j':
                            reference=rsA.getString("journal")+", "+rsA.getString("volume")
                                    +": "+rsA.getString("first_page")+"\n";
                            break;
                        case 'd':
                        case 'w':
                            reference=rsA.getString("url")+"\n";
                            break;
                        case 'p':
                            reference=rsA.getString("journal")+"\n";
                            break;
                        default:
                            System.out.print("can not recognize the reference type...");
                    }
                    int refsection=(rid-1)/500;
                    descPool[refsection]=descPool[refsection]+"["+Integer.toString(rid)+"] "+reference;
                }


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
                        count_sp_human++;
                        break;
                    case 2:
                        index_sp_mouse=1;
                        count_sp_mouse++;
                        break;
                    case 3:
                        index_sp_rat=1;
                        count_sp_rat++;
                        break;
                    case 4:
                        index_sp_calf=1;
                        count_sp_calf++;
                        break;
                    case 5:
                        index_sp_monkey=1;
                        count_sp_monkey++;
                        break;
                    case 6:
                        index_sp_ecoli=1;
                        count_sp_ecoli++;
                        break;
                    case 7:
                        index_sp_chicken=1;
                        count_sp_chicken++;
                        break;
                    case 8:
                        index_sp_lizard=1;
                        count_sp_lizard++;
                        break;
                    case 9:
                        index_sp_rabbit=1;
                        count_sp_rabbit++;
                        break;
                    case 10:
                        index_sp_lamb=1;
                        count_sp_lamb++;
                        break;
                    case 11:
                        index_sp_trout=1;
                        count_sp_trout++;
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

                                        String activityinfof=new String();
                                        activityinfof=value+"\t"+value_desciption;
                                        if (b_a_f.contains(activityinfof)){
                                            int index=b_a_f.indexOf(activityinfof);
                                            ArrayList ref=b_a_f_ref.get(index);
                                            if (ref.contains(rid)==false) ref.add(rid);
                                            b_a_f_ref.set(index,ref);
                                        }else{
                                            b_a_f.add(activityinfof);
                                            ArrayList ref=new ArrayList();
                                            if (ref.contains(rid)==false) ref.add(rid);
                                            b_a_f_ref.add(ref);
                                        }


                                        //i_b_a_f++;
                                        //valueTitle="b_a_f_value_"+Integer.toString(i_b_a_f);
                                        //descTitle="b_a_f_description_"+Integer.toString(i_b_a_f);

                                        //m.setProperty(valueTitle,value);
                                        //m.setProperty(descTitle,value_desciption);

                                        break;
                                    case 'L':
                                        index_pl_l=1;

                                        String activityinfol=new String();
                                        activityinfol=value+"\t"+value_desciption;
                                        if (b_a_l.contains(activityinfol)){
                                            int index=b_a_l.indexOf(activityinfol);
                                            ArrayList ref=b_a_l_ref.get(index);
                                            if (ref.contains(rid)==false) ref.add(rid);
                                            b_a_l_ref.set(index,ref);
                                        }else{
                                            b_a_l.add(activityinfol);
                                            ArrayList ref=new ArrayList();
                                            if (ref.contains(rid)==false) ref.add(rid);
                                            b_a_l_ref.add(ref);
                                        }


                                        //i_b_a_l++;
                                        //valueTitle="b_a_l_value_"+Integer.toString(i_b_a_l);
                                        //descTitle="b_a_l_description_"+Integer.toString(i_b_a_l);
                                        ////refTitle="b_a_l_ref_"+Integer.toString(i_b_a_l);
                                        //m.setProperty(valueTitle,value);
                                        //m.setProperty(descTitle,value_desciption);
                                        ////m.setProperty(refTitle,Integer.toString(i_activity));
                                        break;
                                    default:
                                        index_pl_u=1;

                                        String activityinfou=new String();
                                        activityinfou=value+"\t"+value_desciption;
                                        if (b_a_u.contains(activityinfou)){
                                            int index=b_a_u.indexOf(activityinfou);
                                            ArrayList ref=b_a_u_ref.get(index);
                                            if (ref.contains(rid)==false) ref.add(rid);
                                            b_a_u_ref.set(index,ref);
                                        }else{
                                            b_a_u.add(activityinfou);
                                            ArrayList ref=new ArrayList();
                                            if (ref.contains(rid)==false) ref.add(rid);
                                            b_a_u_ref.add(ref);
                                        }

                                        //i_b_a_u++;
                                        //valueTitle="b_a_u_value_"+Integer.toString(i_b_a_u);
                                        //descTitle="b_a_u_description_"+Integer.toString(i_b_a_u);
                                        ////refTitle="b_a_l_ref_"+Integer.toString(i_b_a_l);
                                        //m.setProperty(valueTitle,value);
                                        //m.setProperty(descTitle,value_desciption);
                                        ////m.setProperty(refTitle,Integer.toString(i_activity));
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

                                        String activityinfof=new String();
                                        activityinfof=value+"\t"+value_desciption;
                                        if (b_b_f.contains(activityinfof)){
                                            int index=b_b_f.indexOf(activityinfof);
                                            ArrayList ref=b_b_f_ref.get(index);
                                            if (ref.contains(rid)==false) ref.add(rid);
                                            b_b_f_ref.set(index,ref);
                                        }else{
                                            b_b_f.add(activityinfof);
                                            ArrayList ref=new ArrayList();
                                            if (ref.contains(rid)==false) ref.add(rid);
                                            b_b_f_ref.add(ref);
                                        }

                                        //i_b_b_f++;
                                        //valueTitle="b_b_f_value_"+Integer.toString(i_b_b_f);
                                        //descTitle="b_b_f_description_"+Integer.toString(i_b_b_f);
                                        ////String refTitle="b_a_f_ref_"+Integer.toString(i_b_a_f);
                                        //m.setProperty(valueTitle,value);
                                        //m.setProperty(descTitle,value_desciption);
                                        ////m.setProperty(refTitle,Integer.toString(i_activity));
                                        break;
                                    case 'L':
                                        index_pl_l=1;

                                        String activityinfol=new String();
                                        activityinfol=value+"\t"+value_desciption;
                                        if (b_b_l.contains(activityinfol)){
                                            int index=b_b_l.indexOf(activityinfol);
                                            ArrayList ref=b_b_l_ref.get(index);
                                            if (ref.contains(rid)==false) ref.add(rid);
                                            b_b_l_ref.set(index,ref);
                                        }else{
                                            b_b_l.add(activityinfol);
                                            ArrayList ref=new ArrayList();
                                            if (ref.contains(rid)==false) ref.add(rid);
                                            b_b_l_ref.add(ref);
                                        }

                                        //i_b_b_l++;
                                        //valueTitle="b_b_l_value_"+Integer.toString(i_b_b_l);
                                        //descTitle="b_b_l_description_"+Integer.toString(i_b_b_l);
                                        ////refTitle="b_a_l_ref_"+Integer.toString(i_b_a_l);
                                        //m.setProperty(valueTitle,value);
                                        //m.setProperty(descTitle,value_desciption);
                                        ////m.setProperty(refTitle,Integer.toString(i_activity));
                                        break;
                                    default:
                                        index_pl_u=1;

                                        String activityinfou=new String();
                                        activityinfou=value+"\t"+value_desciption;
                                        if (b_b_u.contains(activityinfou)){
                                            int index=b_b_u.indexOf(activityinfou);
                                            ArrayList ref=b_b_u_ref.get(index);
                                            if (ref.contains(rid)==false) ref.add(rid);
                                            b_b_u_ref.set(index,ref);
                                        }else{
                                            b_b_u.add(activityinfou);
                                            ArrayList ref=new ArrayList();
                                            if (ref.contains(rid)==false) ref.add(rid);
                                            b_b_u_ref.add(ref);
                                        }




                                        //i_b_b_u++;
                                        //valueTitle="b_b_u_value_"+Integer.toString(i_b_b_u);
                                        //descTitle="b_b_u_description_"+Integer.toString(i_b_b_u);
                                        ////refTitle="b_a_l_ref_"+Integer.toString(i_b_a_l);
                                        //m.setProperty(valueTitle,value);
                                        //m.setProperty(descTitle,value_desciption);
                                        ////m.setProperty(refTitle,Integer.toString(i_activity));
                                        break;
                                }
                                break;
                            default: // ER type unspecified
                                index_pt_u=1;

                                String activityinfo=new String();
                                activityinfo=value+"\t"+value_desciption;
                                if (b_u.contains(activityinfo)){
                                    int index=b_u.indexOf(activityinfo);
                                    ArrayList ref=b_u_ref.get(index);
                                    if (ref.contains(rid)==false) ref.add(rid);
                                    b_u_ref.set(index,ref);
                                }else{
                                    b_u.add(activityinfo);
                                    ArrayList ref=new ArrayList();
                                    if (ref.contains(rid)==false) ref.add(rid);
                                    b_u_ref.add(ref);
                                }

                                //i_b_u++;
                                //valueTitle="b_u_value_"+Integer.toString(i_b_u);
                                //descTitle="b_u_description_"+Integer.toString(i_b_u);
                                ////refTitle="b_a_l_ref_"+Integer.toString(i_b_a_l);
                                //m.setProperty(valueTitle,value);
                                //m.setProperty(descTitle,value_desciption);
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

                                String activityinfoa=new String();
                                activityinfoa=value+"\t"+value_desciption;
                                if (r_a.contains(activityinfoa)){
                                    int index=r_a.indexOf(activityinfoa);
                                    ArrayList ref=r_a_ref.get(index);
                                    if (ref.contains(rid)==false) ref.add(rid);
                                    r_a_ref.set(index,ref);
                                }else{
                                    r_a.add(activityinfoa);
                                    ArrayList ref=new ArrayList();
                                    if (ref.contains(rid)==false) ref.add(rid);
                                    r_a_ref.add(ref);
                                }
                                //i_r_a++;
                                //valueTitle="r_a_value_"+Integer.toString(i_r_a);
                                //descTitle="r_a_description_"+Integer.toString(i_r_a);
                                ////String refTitle="b_a_f_ref_"+Integer.toString(i_b_a_f);
                                //m.setProperty(valueTitle,value);
                                //m.setProperty(descTitle,value_desciption);
                                break;
                            case 'b': // ER beta
                                index_pt_b=1;

                                String activityinfob=new String();
                                activityinfoa=value+"\t"+value_desciption;
                                if (r_b.contains(activityinfoa)){
                                    int index=r_b.indexOf(activityinfoa);
                                    ArrayList ref=r_b_ref.get(index);
                                    if (ref.contains(rid)==false) ref.add(rid);
                                    r_b_ref.set(index,ref);
                                }else{
                                    r_b.add(activityinfoa);
                                    ArrayList ref=new ArrayList();
                                    if (ref.contains(rid)==false) ref.add(rid);
                                    r_b_ref.add(ref);
                                }

                                //i_r_b++;
                                //valueTitle="r_b_value_"+Integer.toString(i_r_b);
                                //descTitle="r_b_description_"+Integer.toString(i_r_b);
                                //String refTitle="b_a_f_ref_"+Integer.toString(i_b_a_f);
                                //m.setProperty(valueTitle,value);
                                //m.setProperty(descTitle,value_desciption);
                                break;
                            default: // ER type unspecified
                                index_pt_u=1;

                                String activityinfou=new String();
                                activityinfou=value+"\t"+value_desciption;
                                if (r_u.contains(activityinfou)){
                                    int index=r_u.indexOf(activityinfou);
                                    ArrayList ref=r_u_ref.get(index);
                                    if (ref.contains(rid)==false) ref.add(rid);
                                    r_u_ref.set(index,ref);
                                }else{
                                    r_u.add(activityinfou);
                                    ArrayList ref=new ArrayList();
                                    if (ref.contains(rid)==false) ref.add(rid);
                                    r_u_ref.add(ref);
                                }

                                //i_r_u++;
                                //valueTitle="r_u_value_"+Integer.toString(i_r_u);
                                //descTitle="r_u_description_"+Integer.toString(i_r_u);
                                ////refTitle="b_a_l_ref_"+Integer.toString(i_b_a_l);
                                //m.setProperty(valueTitle,value);
                                //m.setProperty(descTitle,value_desciption);
                                break;
                        }
                        break;
                    case 'C': // cell proliferation assay
                        index_at_c=1;

                        String activityinfoc=new String();
                        activityinfoc=value+"\t"+value_desciption;
                        if (c.contains(activityinfoc)){
                            int index=c.indexOf(activityinfoc);
                            ArrayList ref=c_ref.get(index);
                            if (ref.contains(rid)==false) ref.add(rid);
                            c_ref.set(index,ref);
                        }else{
                            c.add(activityinfoc);
                            ArrayList ref=new ArrayList();
                            if (ref.contains(rid)==false) ref.add(rid);
                            c_ref.add(ref);
                        }
                        //i_c++;
                        //valueTitle="c_value_"+Integer.toString(i_c);
                        //descTitle="c_description_"+Integer.toString(i_c);
                        ////refTitle="b_a_l_ref_"+Integer.toString(i_b_a_l);
                        //m.setProperty(valueTitle,value);
                        //m.setProperty(descTitle,value_desciption);
                        break;
                    default:
                        System.out.print("can not recognize the assay type");
                        break;
                }
            }
            // set activities

            for(int j=0;j<b_a_f.size();j++){
                String[] activities=b_a_f.get(j).split("\t");
                String valueTitle="b_a_f_value_"+Integer.toString(j+1);
                String descTitle="b_a_f_description_"+Integer.toString(j+1);

                m.setProperty(valueTitle,activities[0]);
                String value_description=activities[1]+" [";
                for (int refid:b_a_f_ref.get(j)){
                    value_description=value_description+Integer.toString(refid)+",";
                }
                value_description=value_description+"]";
                String formatted_vd=value_description.replaceAll(",]","]");

                m.setProperty(descTitle,formatted_vd);
            }

            for(int j=0;j<b_b_f.size();j++){
                String[] activities=b_b_f.get(j).split("\t");
                String valueTitle="b_b_f_value_"+Integer.toString(j+1);
                String descTitle="b_b_f_description_"+Integer.toString(j+1);

                m.setProperty(valueTitle,activities[0]);
                String value_description=activities[1]+" [";
                for (int refid:b_b_f_ref.get(j)){
                    value_description=value_description+Integer.toString(refid)+",";
                }
                value_description=value_description+"]";
                String formatted_vd=value_description.replaceAll(",]","]");

                m.setProperty(descTitle,formatted_vd);
            }

            for(int j=0;j<b_a_l.size();j++){
                String[] activities=b_a_l.get(j).split("\t");
                String valueTitle="b_a_l_value_"+Integer.toString(j+1);
                String descTitle="b_a_l_description_"+Integer.toString(j+1);

                m.setProperty(valueTitle,activities[0]);
                String value_description=activities[1]+" [";
                for (int refid:b_a_l_ref.get(j)){
                    value_description=value_description+Integer.toString(refid)+",";
                }
                value_description=value_description+"]";
                String formatted_vd=value_description.replaceAll(",]","]");

                m.setProperty(descTitle,formatted_vd);
            }


            for(int j=0;j<b_b_l.size();j++){
                String[] activities=b_b_l.get(j).split("\t");
                String valueTitle="b_b_l_value_"+Integer.toString(j+1);
                String descTitle="b_b_l_description_"+Integer.toString(j+1);

                m.setProperty(valueTitle,activities[0]);
                String value_description=activities[1]+" [";
                for (int refid:b_b_l_ref.get(j)){
                    value_description=value_description+Integer.toString(refid)+",";
                }
                value_description=value_description+"]";
                String formatted_vd=value_description.replaceAll(",]","]");

                m.setProperty(descTitle,formatted_vd);
            }
            for(int j=0;j<b_a_u.size();j++){
                String[] activities=b_a_u.get(j).split("\t");
                String valueTitle="b_a_u_value_"+Integer.toString(j+1);
                String descTitle="b_a_u_description_"+Integer.toString(j+1);

                m.setProperty(valueTitle,activities[0]);
                String value_description=activities[1]+" [";
                for (int refid:b_a_u_ref.get(j)){
                    value_description=value_description+Integer.toString(refid)+",";
                }
                value_description=value_description+"]";
                String formatted_vd=value_description.replaceAll(",]","]");

                m.setProperty(descTitle,formatted_vd);
            }


            for(int j=0;j<b_b_u.size();j++){
                String[] activities=b_b_u.get(j).split("\t");
                String valueTitle="b_b_u_value_"+Integer.toString(j+1);
                String descTitle="b_b_u_description_"+Integer.toString(j+1);

                m.setProperty(valueTitle,activities[0]);
                String value_description=activities[1]+" [";
                for (int refid:b_b_u_ref.get(j)){
                    value_description=value_description+Integer.toString(refid)+",";
                }
                value_description=value_description+"]";
                String formatted_vd=value_description.replaceAll(",]","]");

                m.setProperty(descTitle,formatted_vd);
            }


            for(int j=0;j<b_u.size();j++){
                String[] activities=b_u.get(j).split("\t");
                String valueTitle="b_u_value_"+Integer.toString(j+1);
                String descTitle="b_u_description_"+Integer.toString(j+1);

                m.setProperty(valueTitle,activities[0]);
                String value_description=activities[1]+" [";
                for (int refid:b_u_ref.get(j)){
                    value_description=value_description+Integer.toString(refid)+",";
                }
                value_description=value_description+"]";
                String formatted_vd=value_description.replaceAll(",]","]");

                m.setProperty(descTitle,formatted_vd);
            }


            for(int j=0;j<r_a.size();j++){
                String[] activities=r_a.get(j).split("\t");
                String valueTitle="r_a_value_"+Integer.toString(j+1);
                String descTitle="r_a_description_"+Integer.toString(j+1);

                m.setProperty(valueTitle,activities[0]);
                String value_description=activities[1]+" [";
                for (int refid:r_a_ref.get(j)){
                    value_description=value_description+Integer.toString(refid)+",";
                }
                value_description=value_description+"]";
                String formatted_vd=value_description.replaceAll(",]","]");

                m.setProperty(descTitle,formatted_vd);
            }
            for(int j=0;j<r_b.size();j++){
                String[] activities=r_b.get(j).split("\t");
                String valueTitle="r_b_value_"+Integer.toString(j+1);
                String descTitle="r_b_description_"+Integer.toString(j+1);

                m.setProperty(valueTitle,activities[0]);
                String value_description=activities[1]+" [";
                for (int refid:r_b_ref.get(j)){
                    value_description=value_description+Integer.toString(refid)+",";
                }
                value_description=value_description+"]";
                String formatted_vd=value_description.replaceAll(",]","]");

                m.setProperty(descTitle,formatted_vd);
            }


            for(int j=0;j<r_u.size();j++){
                String[] activities=r_u.get(j).split("\t");
                String valueTitle="r_u_value_"+Integer.toString(j+1);
                String descTitle="r_u_description_"+Integer.toString(j+1);

                m.setProperty(valueTitle,activities[0]);
                String value_description=activities[1]+" [";
                for (int refid:r_u_ref.get(j)){
                    value_description=value_description+Integer.toString(refid)+",";
                }
                value_description=value_description+"]";
                String formatted_vd=value_description.replaceAll(",]","]");

                m.setProperty(descTitle,formatted_vd);
            }


            for(int j=0;j<c.size();j++){
                String[] activities=c.get(j).split("\t");
                String valueTitle="c_value_"+Integer.toString(j+1);
                String descTitle="c_description_"+Integer.toString(j+1);

                m.setProperty(valueTitle,activities[0]);
                String value_description=activities[1]+" [";
                for (int refid:c_ref.get(j)){
                    value_description=value_description+Integer.toString(refid)+",";
                }
                value_description=value_description+"]";
                String formatted_vd=value_description.replaceAll(",]","]");

                m.setProperty(descTitle,formatted_vd);
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
            for (int ic=0;ic<3;ic++){
                if (descPool[ic]!=""){
                    m.setProperty("references"+ic,descPool[ic]);
                }
            }
            System.out.printf("%d\t%s\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\n",
                    i,molregno,i_activity,
                    b_a_f.size()+b_a_l.size()+b_a_u.size()+b_b_f.size()+b_b_l.size()+b_b_u.size()+b_u.size(),
                    r_a.size()+r_b.size()+r_u.size(), c.size(), b_a_f.size()+b_a_l.size()+b_a_u.size()+r_a.size(),
                    b_b_f.size()+b_b_l.size()+b_b_u.size()+r_b.size(),b_u.size()+r_u.size(),
                    count_sp_human, count_sp_mouse, count_sp_rat, count_sp_calf, count_sp_monkey,
                    count_sp_ecoli, count_sp_chicken, count_sp_lizard, count_sp_rabbit, count_sp_lamb, count_sp_trout);
            me.write(m);
            //System.out.println(i+":"+i_b_a_f+" "+i_b_a_l+" "+i_b_a_u+" "+i_b_b_f+" "+i_b_b_l+" "+i_b_b_u+" "+i_b_u+" "+
            //i_r_a+" "+i_r_b+" "+i_r_u+" "+i_c);
        }
    }


}
