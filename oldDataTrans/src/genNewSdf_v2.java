/**
 * Created with IntelliJ IDEA.
 * By: Jie Shen @ NCTR, FDA
 * Date: 6/6/12
 * Time: 9:49 PM
 * <p/>
 *
 * Description: This class is designed for transfer old sdf file generated from Xu's EDKB. The purpose is to
 * generate a new sdf file, which includes all the reference information and re-organized the sdf file in
 * non-redundant way. In addition, index will be added to mark the different assay data.
 */

import chemaxon.formats.MolExporter;
import chemaxon.formats.MolImporter;
import chemaxon.marvin.io.MPropHandler;
import chemaxon.reaction.Standardizer;
import chemaxon.struc.Molecule;
import jxl.Cell;

import java.io.*;
import java.util.*;


public class genNewSdf_v2 {
    final public static String INPUTSDFILE="C:\\Temp_data2\\all_EDKB_Xu_named.sdf";
    final public static String OUTPUTSDFILE="C:\\Temp_data2\\all_EDKB_Xu_named_NEW_120607.sdf";
    final public static String FULL_ERA="C:\\Temp_data2\\lists\\full_ERa.txt";
    final public static String FULL_ERB="C:\\Temp_data2\\lists\\full_ERb.txt";
    final public static String LBD_ERA="C:\\Temp_data2\\lists\\LBD_ERa.txt";
    final public static String LBD_ERB="C:\\Temp_data2\\lists\\LBD_ERb.txt";
    final public static String ER_UNKNOWN="C:\\Temp_data2\\lists\\ER_unknown.txt";
    final public static String REPORT_GENE="C:\\Temp_data2\\lists\\RGA.txt";
    final public static String CELL_PROL="C:\\Temp_data2\\lists\\CPA.txt";
    final public static String NON_USE="C:\\Temp_data2\\lists\\non_use.txt";
    final public static String HUMAN="C:\\Temp_data2\\lists\\human.txt";
    final public static String MOUSE="C:\\Temp_data2\\lists\\mouse.txt";
    final public static String RAT="C:\\Temp_data2\\lists\\rat.txt";
    final public static String CALF="C:\\Temp_data2\\lists\\calf.txt";
    final public static String LAMB="C:\\Temp_data2\\lists\\lamb.txt";
    final public static String RABBIT="C:\\Temp_data2\\lists\\rabbit.txt";
    final public static String TROUT="C:\\Temp_data2\\lists\\fish.txt";
    final public static String LIZARD="C:\\Temp_data2\\lists\\Lizard.txt";
    final public static String CHICKEN="C:\\Temp_data2\\lists\\chicken.txt";

    final public static String SUMMARYFILE="C:\\Temp_data2\\Field summar_JS120426.xls";
    final public static String SHEETNAME="Sheet1";

    public static ArrayList readList(String fileName) throws Exception {
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(fileName)));
        String data = null;
        ArrayList outList = new ArrayList();
        while((data=br.readLine())!=null){
            outList.add(data);
        }
        return outList;
    }

    public static ArrayList readExcel(String fileName, String sheetName){
        try {
            InputStream is = new FileInputStream(new File(fileName));
            jxl.Workbook rwb = jxl.Workbook.getWorkbook(is);
            jxl.Sheet rs = rwb.getSheet(sheetName);
            ArrayList<String> tableTitle = new ArrayList<String>();
            ArrayList<ArrayList<String>> readMatrix=new ArrayList<ArrayList<String>>();
            int colN=rs.getColumns();
            int rowN=rs.getRows();
            for (int j=0;j<colN;j++){
                Cell cell =rs.getCell(j,0);
                tableTitle.add(cell.getContents());
            }
            readMatrix.add(tableTitle);
            for (int i=1;i<rowN;i++){
                ArrayList<String> tempArray=new ArrayList<String>();
                for (int j=0;j<colN;j++){
                    Cell cell = rs.getCell(j,i);
                    tempArray.add(cell.getContents());
                }
                readMatrix.add(tempArray);

            }
            return readMatrix;

        } catch (Exception e){
            e.printStackTrace();
            System.exit( 1 );
        }
        return null;
    }

    public static ArrayList htSort(final HashMap ht){ //Sort the hashtable by key values and return the sorted LIST of keys
        ArrayList<String> v = new ArrayList<String>(ht.keySet());
//        System.out.println(ht);
//        System.out.println(v);

        Collections.sort(v, new Comparator<Object>() {
            public int compare(Object arg0, Object arg1) {
//               System.out.println(ht.get(arg1).toString().compareTo(ht.get(arg0).toString()));
//                System.out.println(ht.get(arg1).toString()+ht.get(arg0).toString());
                Double pd=Double.parseDouble(ht.get(arg1).toString())-Double.parseDouble(ht.get(arg0).toString());
//                System.out.println(pd);
                if (pd>0) return 1;
                else if (pd<0) return -1;
                else return 0;
            }
        });
//        System.out.println(ht);
//        System.out.println(v);
//        System.out.println();
        return v;
    }

    public static HashMap<String, Double> sortHashMap(HashMap<String,Double> input){
        Map<String, Double> tempMap = new HashMap<String,Double>();
        for (String wsState : input.keySet()){
            tempMap.put(wsState,input.get(wsState));
        }

        List<String> mapKeys = new ArrayList<String>(tempMap.keySet());
        List<Double> mapValues = new ArrayList<Double>(tempMap.values());
        HashMap<String, Double> sortedMap = new LinkedHashMap<String, Double>();
        TreeSet<Double> sortedSet = new TreeSet<Double>(mapValues);
        Object[] sortedArray = sortedSet.toArray();
        int size = sortedArray.length;
        for (int i=0; i<size; i++){
            sortedMap.put(mapKeys.get(mapValues.indexOf(sortedArray[i])),
                    (Double)sortedArray[i]);
        }
        return sortedMap;
    }


    public static void main(String args[]) throws Exception {
        MolImporter mi = new MolImporter(INPUTSDFILE);
        MolExporter me = new MolExporter(OUTPUTSDFILE, "sdf");

        ArrayList fullERa = readList(FULL_ERA);
        ArrayList fullERb = readList(FULL_ERB);
        ArrayList lbdERa = readList(LBD_ERA);
        ArrayList lbdERb = readList(LBD_ERB);
        ArrayList unknownER = readList(ER_UNKNOWN );
        ArrayList rpa = readList(REPORT_GENE);
        ArrayList cpa = readList(CELL_PROL);
        ArrayList human = readList(HUMAN);
        ArrayList mouse = readList(MOUSE);
        ArrayList rat = readList(RAT);
        ArrayList calf = readList(CALF);
        ArrayList lamb = readList(LAMB);
        ArrayList rabbit = readList(RABBIT);
        ArrayList trout = readList(TROUT);
        ArrayList lizard = readList(LIZARD);
        ArrayList chicken = readList(CHICKEN);
        ArrayList nonUse = readList(NON_USE);

        Molecule mol;
        Standardizer standardizer = new Standardizer(new File("C:\\Temp_data2\\standardlize_config.xml"));
        standardizer.setFinalClean();

        ArrayList sumMatrix = readExcel(SUMMARYFILE,SHEETNAME);
        //Convert ArrayList to HashMap with key of "Field" and value of Array of other information:
        int matrixSize=sumMatrix.size();
        HashMap<String, String[]> sumArray = new HashMap<String, String[]>();
        for (int i=0;i<matrixSize;i++){
            ArrayList tempArray=(ArrayList)sumMatrix.get(i);
            sumArray.put(tempArray.get(0).toString(), (String[])tempArray.toArray(new String[0])); // !! Should add "new String[0]"
        }

        int i=0;
        while((mol=mi.read())!=null && i<300000){
            i++;
            standardizer.standardize(mol);
            String propKeys[] = mol.properties().getKeys();
            HashMap ht_fullERa=new HashMap<String, Double>();   //ht_xxxx is unsorted hashmap, while hm_xxx is sorted one.
            HashMap ht_fullERb=new HashMap<String, Double>();
            HashMap ht_lbdERa=new HashMap<String, Double>();
            HashMap ht_lbdERb=new HashMap<String, Double>();
            HashMap ht_unknownER=new HashMap<String, Double>();
            HashMap ht_rpa=new HashMap<String, Double>();
            HashMap ht_cpa=new HashMap<String, Double>();
/*
            int i_at_1=0, i_at_2=0, i_at_3=0; // index of assay type: 1:binding, 2:reporter gene assay, 3:cell proliferation assay
            int i_pt_1=0, i_pt_2=0, i_pt_0=0; // index of protein type: 1: ERa, 2: ERb, 0: unknown
            int i_pl_1=0, i_pl_2=0, i_pl_0=0; // index of protein length: 1: Full-length; 2: Ligand binding domain; 0: unknown
            int i_sp_1=0, i_sp_2=0, i_sp_3=0, i_sp_4=0, i_sp_5=0, i_sp_6=0, i_sp_7=0, i_sp_8=0;
            //index of species
*/
            HashSet at = new HashSet();
            HashSet pt = new HashSet();
            HashSet pl = new HashSet();
            HashSet sp = new HashSet();


            for (String propKey:propKeys){
                if (fullERa.contains(propKey)) {
                    String prop = MPropHandler.convertToString(mol.properties(), propKey);
                    ht_fullERa.put(propKey,prop);
                    at.add(1);
                    pt.add(1);
                    pl.add(1);
                    mol.properties().remove(mol.properties().get(propKey));
                }
                if (fullERb.contains(propKey)) {
                    String prop = MPropHandler.convertToString(mol.properties(), propKey);
                    ht_fullERb.put(propKey,prop);
                    at.add(1);
                    pt.add(2);
                    pl.add(1);
                    mol.properties().remove(mol.properties().get(propKey));
                }
                if (lbdERa.contains(propKey)) {
                    String prop = MPropHandler.convertToString(mol.properties(), propKey);
                    ht_lbdERa.put(propKey,prop);
                    at.add(1);
                    pt.add(1);
                    pl.add(2);
                    mol.properties().remove(mol.properties().get(propKey));
                }
                if (lbdERb.contains(propKey)) {
                    String prop = MPropHandler.convertToString(mol.properties(), propKey);
                    ht_lbdERb.put(propKey,prop);
                    at.add(1);
                    pt.add(2);
                    pl.add(2);
                    mol.properties().remove(mol.properties().get(propKey));
                }
                if (unknownER.contains(propKey)) {
                    String prop = MPropHandler.convertToString(mol.properties(), propKey);
                    ht_unknownER.put(propKey,prop);
                    at.add(1);
                    pt.add(0);
                    pl.add(0);
                    mol.properties().remove(mol.properties().get(propKey));
                }
                if (rpa.contains(propKey)) {
                    String prop = MPropHandler.convertToString(mol.properties(), propKey);
                    ht_rpa.put(propKey,prop);
                    at.add(2);
                    mol.properties().remove(mol.properties().get(propKey));
                }
                if (cpa.contains(propKey)) {
                    String prop = MPropHandler.convertToString(mol.properties(), propKey);
                    ht_cpa.put(propKey,prop);
                    at.add(3);
                    mol.properties().remove(mol.properties().get(propKey));
                }
                if (human.contains(propKey)) sp.add(1);
                if (mouse.contains(propKey)) sp.add(2);
                if (calf.contains(propKey)) sp.add(3);
                if (lamb.contains(propKey)) sp.add(4);
                if (rabbit.contains(propKey)) sp.add(5);
                if (trout.contains(propKey)) sp.add(6);
                if (lizard.contains(propKey)) sp.add(7);
                if (chicken.contains(propKey)) sp.add(8);
                if (rat.contains(propKey)) sp.add(9);
                if (nonUse.contains(propKey)) mol.properties().remove(mol.properties().get(propKey));
            }

            //Traversal of each hashtable and set the properties in the new SDF file.
            List<String> ls_fullERa = htSort(ht_fullERa);
            for (int j=0;j<ls_fullERa.size();j++){
                String no=Integer.toString(j);
                String keyName="fullERa_"+no;
                String keyValue="fullERa_value_"+no;
                String[] keyNameList=sumArray.get(ls_fullERa.get(j));
                String keyNameIs=keyNameList[0]+","+keyNameList[2]+","+keyNameList[3]+","+keyNameList[9]+","+keyNameList[10]+","+keyNameList[11];
//                mol.setProperty(keyName,ls_fullERa.get(j));
                mol.setProperty(keyName,keyNameIs);
                mol.setProperty(keyValue,ht_fullERa.get(ls_fullERa.get(j)).toString());
            }
            List<String> ls_fullERb = htSort(ht_fullERb);
            for (int j=0;j<ls_fullERb.size();j++){
                String no=Integer.toString(j);
                String keyName="fullERb_"+no;
                String keyValue="fullERb_value_"+no;
                String[] keyNameList=sumArray.get(ls_fullERb.get(j));
                String keyNameIs=keyNameList[0]+","+keyNameList[2]+","+keyNameList[3]+","+keyNameList[9]+","+keyNameList[10]+","+keyNameList[11];
//                mol.setProperty(keyName,ls_fullERb.get(j));
                mol.setProperty(keyName,keyNameIs);
                mol.setProperty(keyValue,ht_fullERb.get(ls_fullERb.get(j)).toString());
            }
            List<String> ls_lbdERa = htSort(ht_lbdERa);
            for (int j=0;j<ls_lbdERa.size();j++){
                String no=Integer.toString(j);
                String keyName="lbdERa_"+no;
                String keyValue="lbdERa_value_"+no;
                String[] keyNameList=sumArray.get(ls_lbdERa.get(j));
                String keyNameIs=keyNameList[0]+","+keyNameList[2]+","+keyNameList[3]+","+keyNameList[9]+","+keyNameList[10]+","+keyNameList[11];
//               mol.setProperty(keyName,ls_lbdERa.get(j));
                mol.setProperty(keyName,keyNameIs);
                mol.setProperty(keyValue,ht_lbdERa.get(ls_lbdERa.get(j)).toString());
            }
            List<String> ls_lbdERb = htSort(ht_lbdERb);
            for (int j=0;j<ls_lbdERb.size();j++){
                String no=Integer.toString(j);
                String keyName="lbdERb_"+no;
                String keyValue="lbdERb_value_"+no;
                String[] keyNameList=sumArray.get(ls_lbdERb.get(j));
                String keyNameIs=keyNameList[0]+","+keyNameList[2]+","+keyNameList[3]+","+keyNameList[9]+","+keyNameList[10]+","+keyNameList[11];
//               mol.setProperty(keyName,ls_lbdERb.get(j));
                mol.setProperty(keyName,keyNameIs);
                mol.setProperty(keyValue,ht_lbdERb.get(ls_lbdERb.get(j)).toString());
            }
            List<String> ls_unknowER = htSort(ht_unknownER);
            for (int j=0;j<ls_unknowER.size();j++){
                String no=Integer.toString(j);
                String keyName="unkER_"+no;
                String keyValue="unkER_value_"+no;
                String[] keyNameList=sumArray.get(ls_unknowER.get(j));
                String keyNameIs=keyNameList[0]+","+keyNameList[2]+","+keyNameList[3]+","+keyNameList[9]+","+keyNameList[10]+","+keyNameList[11];
//               mol.setProperty(keyName,ls_unknowER.get(j));
                mol.setProperty(keyName,keyNameIs);
                mol.setProperty(keyValue,ht_unknownER.get(ls_unknowER.get(j)).toString());
            }
            List<String> ls_rpa = htSort(ht_rpa);
            for (int j=0;j<ls_rpa.size();j++){
                String no=Integer.toString(j);
                String keyName="rpa_"+no;
                String keyValue="rpa_value_"+no;
                String[] keyNameList=sumArray.get(ls_rpa.get(j));
                String keyNameIs=keyNameList[0]+","+keyNameList[2]+","+keyNameList[3]+","+keyNameList[9]+","+keyNameList[10]+","+keyNameList[11];
//               mol.setProperty(keyName,ls_rpa.get(j));
                mol.setProperty(keyName,keyNameIs);

                mol.setProperty(keyValue,ht_rpa.get(ls_rpa.get(j)).toString());
            }
            List<String> ls_cpa = htSort(ht_cpa);
            for (int j=0;j<ls_cpa.size();j++){
                String no=Integer.toString(j);
                String keyName="cpa_"+no;
                String keyValue="cpa_value_"+no;
                String[] keyNameList=sumArray.get(ls_cpa.get(j));
                String keyNameIs=keyNameList[0]+","+keyNameList[2]+","+keyNameList[3]+","+keyNameList[9]+","+keyNameList[10]+","+keyNameList[11];
//               mol.setProperty(keyName,ls_cpa.get(j));
                mol.setProperty(keyName,keyNameIs);

                mol.setProperty(keyValue,ht_cpa.get(ls_cpa.get(j)).toString());
            }

//            System.out.println(ht_fullERa);
//            System.out.println(ls_fullERa);
//            System.out.println(ht_lbdERa);
//            System.out.println(ls_lbdERa);
//            System.out.println(ht_lbdERb);
//            System.out.println(ls_lbdERb);
//            System.out.println(ht_cpa);
//            System.out.println(ls_cpa);

//            mol.aromatize();
//            MolHandler mh= new MolHandler(mol);
//            mh.clean(false,"O1e");
//            me.write(mh.getMolecule());
//            System.out.println(ht_cpa.get("Soto95"));
//            System.out.println(ht_cpa.toString()+at.toString() +pt.toString() +pl.toString() +sp.toString() );
//            System.out.println(mol.properties().get("PK"));        \

            // Set index
            mol.setProperty("AT_index",at.toString());
            mol.setProperty("PT_index",pt.toString());
            mol.setProperty("PL_index",pl.toString());
            mol.setProperty("SP_index",sp.toString());

//           System.out.println(MolExporter.exportToFormat(mol,"inchikey").split("=")[1]);// InchiKey
//           System.out.println(MolExporter.exportToFormat(mol,"smiles:u"));// canonical SMILES
            mol.setProperty("canonical_smiles", MolExporter.exportToFormat(mol,"smiles:u"));
            mol.setProperty("standard_inchi_key", MolExporter.exportToFormat(mol,"inchikey").split("=")[1] );

            me.write(mol);
        }


    }
}

