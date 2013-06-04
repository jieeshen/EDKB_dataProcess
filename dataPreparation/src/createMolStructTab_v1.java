/**
 * Created with IntelliJ IDEA.
 * By: Jie Shen @ NCTR, FDA
 * Date: 6/10/12
 * Time: 8:28 AM
 * <p/>
 * Description:
 */
/**
 * Created with IntelliJ IDEA.
 * By: Jie Shen @ NCTR, FDA
 * Date: 5/30/12
 * Time: 11:18 AM
 * <p/>
 * Description: This class is used to create the molecule_structures table from the molecule_records.
 *              It reads the list in the molecule_records, for those do not have molregno associated,
 *              the program will search the molecules from the sdf files and make comparison by standard_inchi_key
 *              to localized its molregno in the EMBL database. If there is no such records in the EMBL, create it
 */
import chemaxon.formats.MolExporter;
import chemaxon.formats.MolImporter;
import chemaxon.reaction.Standardizer;
import chemaxon.struc.Molecule;
import jxl.Cell;

import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;


public class createMolStructTab_v1 {
    final public static String EXCELFILE="C:\\Documents and Settings\\jshen\\My Documents\\Research\\EDKB\\ER\\ERDB\\Assays.xls";
    final public static String SHEETNAME="compound_records";
    final public static String driver="com.mysql.jdbc.Driver";
    final public static String url="jdbc:mysql://localhost:3306/chembl_13";
    final public static String user="root";
    final public static String password="jsh1234";

    //    public static ArrayList<String[]> addedMolDic = new ArrayList<String[]>();
    public static ArrayList<String[]> addedMolStruct = new ArrayList<String[]>();
    public static int addedNo=5000000;


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

    public static ArrayList searchEmblByName(String molName) {

        String sqlqueryByName="select * from molecule_dictionary where pref_name like \""+molName+"\";";
        String sqlqueryBySyno="select * from molecule_synonyms where synonyms like \""+molName+"\";";
        ArrayList resultArray = new ArrayList();
        try{
            Class.forName(driver);
            Connection conn = DriverManager.getConnection(url, user, password);
            Statement st= conn.createStatement();

//            System.out.println(sqlqueryByName);
            ResultSet rs=st.executeQuery(sqlqueryByName);
//            System.out.println(rs.getArray("molregno"));
            while(rs.next()){
                resultArray.add(rs.getString("molregno"));
            }
            if (resultArray.isEmpty()){
//                System.out.println(sqlqueryBySyno);
                ResultSet rs2=st.executeQuery(sqlqueryBySyno);
                while(rs2.next()){
                    resultArray.add(rs2.getString("molregno"));
                }
            }
        } catch ( ClassNotFoundException cnfex ) {
            System.err.println("fail in loading JDBC/ODBC driver." );
            cnfex.printStackTrace();
            System.exit( 1 ); // terminate program
        } catch ( SQLException sqlex ) {
            System.err.println( "cannot connect to database." );
            sqlex.printStackTrace();
            System.exit( 1 ); // terminate program
        }
        return resultArray;
    }

    public static ArrayList searchEmblByName(String[] molName) {
        String driver="com.mysql.jdbc.Driver";
        String url="jdbc:mysql://localhost:3306/chembl_13";
        String user="root";
        String password="jsh1234";

        ArrayList resultArray = new ArrayList();
        try{
            Class.forName(driver);
            Connection conn = DriverManager.getConnection(url, user, password);
            Statement st= conn.createStatement();

//            System.out.println(sqlqueryByName);
            for (int i=0;i<molName.length;i++){
                String sqlqueryByName="select * from molecule_dictionary where pref_name like \""+molName[i]+"\";";
                String sqlqueryBySyno="select * from molecule_synonyms where synonyms like \""+molName[i]+"\";";
                ArrayList resultLine = new ArrayList();
                resultLine.add(molName[i]);
                ResultSet rs=st.executeQuery(sqlqueryByName);
//            System.out.println(rs.getArray("molregno"));
                while(rs.next()){
                    resultLine.add(rs.getString("molregno"));
                }

//                System.out.println(sqlqueryBySyno);
                ResultSet rs2=st.executeQuery(sqlqueryBySyno);
                while(rs2.next()){
                    resultLine.add(rs2.getString("molregno"));
                }
                resultArray.add(resultLine);
            }

        } catch ( ClassNotFoundException cnfex ) {
            System.err.println("fail in loading JDBC/ODBC driver." );
            cnfex.printStackTrace();
            System.exit( 1 ); // terminate program
        } catch ( SQLException sqlex ) {
            System.err.println( "cannot connect to database." );
            sqlex.printStackTrace();
            System.exit( 1 ); // terminate program
        }
        return resultArray;
    }

    public static HashMap<String, String> getMapping(String filename) throws Exception {
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filename)));
        String data = null;
        HashMap<String, String> mappingDic = new HashMap<String, String>();
        while((data = br.readLine())!=null)  {
            String[] dataList=data.split("\t");
            mappingDic.put(dataList[1],dataList[0]);
        }
        return mappingDic;

    }

    public static String hasMol(Molecule mol) throws Exception{
        String inchikey= MolExporter.exportToFormat(mol, "inchikey").split("=")[1];
        return inchikey;
/*
//changed due to it will take extremely long time to search the database. So now make it pending by returning the inchikey.
//Then they can be queried in bach.
        System.out.print(inchikey);
        Class.forName(driver);
        Connection conn = DriverManager.getConnection(url, user, password);
        Statement st= conn.createStatement();
        String sqlqueryByik="select * from compound_structures where standard_inchi_key=\""+inchikey+"\";";
        ResultSet rs=st.executeQuery(sqlqueryByik);
        if(rs.next()){
            return rs.getString("molregno");
        } else{
            System.out.println(" NotInEMBL ");
            if (addedMolStruct.size()>0){
                for(String[] al:addedMolStruct ){
                    if (al[3].equals(inchikey)){
                        System.out.println(" foundedInAddedMolStruct ");
                        return al[0];
                    }
                }
            }
        }
        return "not found";
*/
    }

    public static void addMol(Molecule mol) throws Exception{
        addedNo++;
        System.out.print(" add a mol:"+Integer.toString(addedNo));
        String[] cpmd_struct=new String[6];
        cpmd_struct[0]=Integer.toString(addedNo);
        cpmd_struct[1]=MolExporter.exportToFormat(mol,"mol");
        cpmd_struct[2]=MolExporter.exportToFormat(mol,"inchi").split("=")[1];
        cpmd_struct[3]=MolExporter.exportToFormat(mol,"inchikey").split("=")[1];
        cpmd_struct[4]=MolExporter.exportToFormat(mol,"smiles:u");
        cpmd_struct[5]=mol.getFormula();
        addedMolStruct.add(cpmd_struct);
    }

    public static void addMol(String molregno) throws Exception{
        Class.forName(driver);
        Connection conn = DriverManager.getConnection(url, user, password);
        Statement st= conn.createStatement();
        String sqlqueryBychemblid="select * from compound_structures where molregno="+molregno+";";
        ResultSet rs=st.executeQuery(sqlqueryBychemblid);
        String[] cpmd_struct=new String[6];
        if (rs.next()){
            cpmd_struct[0]=rs.getString("molregno");
            cpmd_struct[1]=rs.getString("molfile");
            cpmd_struct[2]=rs.getString("standard_inchi");
            cpmd_struct[3]=rs.getString("standard_inchi_key");
            cpmd_struct[4]=rs.getString("canonical_smiles");
            cpmd_struct[5]=rs.getString("molformula");
            addedMolStruct.add(cpmd_struct);
        }
    }

    public static void main(String args[]) throws Exception {
        Molecule mol;
        boolean found;

        ArrayList readMatrix = readExcel(EXCELFILE,SHEETNAME);
        ArrayList titleArray=(ArrayList)readMatrix.get(0);
        int ckIndex=titleArray.indexOf("compound_key");
        int cnIndex=titleArray.indexOf("compound_name");

//Convert ArrayList to 2D array:
        int matrixSize=readMatrix.size();
        String[][] outArray=new String[matrixSize][];
        //transfer ArrayList to 2D array
        for (int i=0;i<matrixSize;i++){

            ArrayList tempArray=(ArrayList)readMatrix.get(i);
            outArray[i]=(String[])tempArray.toArray(new String[0]); // !! Should add "new String[0]"
        }

//
//        String[] molNameList=new String[matrixSize];
//        for (int i=0;i<matrixSize;i++){
//            molNameList[i]=outArray[i][3].trim();
//        }
//
//        ArrayList returnList=searchEmblByName(molNameList);

//        System.out.println(outArray[3][6].trim());
//        System.out.println(outArray[3][6]=="");
//
//        System.out.println(searchEmblByName(outArray[3][5].trim()));
//
// process the data, search from EMBL
//        for (int i=1;i<matrixSize;i++){
//           ArrayList molregnoList=searchEmblByName(outArray[i][5].trim());
//            if (outArray[i][6]!=""){
//                molregnoList.addAll(searchEmblByName(outArray[i][6].trim()));
//            }
//            System.out.println(outArray[i][5]+"\t"+outArray[i][6]+"\t"+molregnoList);
//        }

// get a mapping dictionary from a text file
        HashMap<String, String> mappingList=getMapping("C:\\Temp_data2\\refMapping.txt");
//        System.out.println(mappingList);
//        System.out.println(mappingList.get("647"));

        PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream("C:\\Temp_data2\\newCmpdRecs.txt")),true);

        PrintWriter pw_cpmd_struct = new PrintWriter(new OutputStreamWriter(new FileOutputStream("C:\\Temp_data2\\newCmpdStructs.txt")),true);

        for (int i=1;i<matrixSize;i++){
//        for (int i=1;i<11;i++){
            System.out.print(Integer.toString(i)+" "+outArray[i][3]+" ");
            if (outArray[i][3] != "") {
                addMol(outArray[i][3]);
            } else {
                if (outArray[i][10]!=""){
                    System.out.print(" HASMOLINCHEMBLID "+outArray[i][10]+" ");
                    Class.forName(driver);
                    Connection conn = DriverManager.getConnection(url, user, password);
                    Statement st= conn.createStatement();
                    String sqlqueryBychemblid="select * from molecule_dictionary where chembl_id=\""+outArray[i][10]+"\";";
                    ResultSet rs=st.executeQuery(sqlqueryBychemblid);
                    if(rs.next()){
                        outArray[i][3]=rs.getString("molregno");
                        addMol(outArray[i][3]);
                    }
                } else{
                    String sdFileName="C:\\Documents and Settings\\jshen\\My Documents\\Research\\EDKB\\ER\\ERDB\\sdf\\"+mappingList.get(outArray[i][0])+".sdf";
//                String sdFileName="C:\\Temp_data2\\all_EDKB_Xu_named.sdf";
                    MolImporter mi = new MolImporter(sdFileName);
//                System.out.print(outArray[i][5]+" ");

                    found=false;
                    while((mol=mi.read())!=null){
                        String molName=new String();
                        molName=mol.getProperty("Name");
//                    System.out.println(molName);
                        if (outArray[i][5].equals(molName)){
//                        System.out.println("got it "+mol.getName()+" "+outArray[i][5]);
                            found=true;
                            // standardize the molecule
                            Standardizer standardizer = new Standardizer(new File("C:\\Temp_data2\\standardlize_config.xml"));
                            standardizer.setFinalClean();
                            standardizer.standardize(mol);
                            String molinchikey=MolExporter.exportToFormat(mol,"inchikey").split("=")[1];
                            System.out.print(molinchikey.equals(outArray[i][14]));
                            if (molinchikey.equals(outArray[i][14])){
                                System.out.print(" found in table");
                                outArray[i][3]=outArray[i][15];
                                addMol(outArray[i][3]);
                            } else{
                                System.out.print(" Searching in DB... ");
                                String hasmol=hasMol(mol);
                                System.out.print(" HASMOLINSDF "+hasmol);
                                if (hasmol.equals("not found")){
                                    addMol(mol);
                                    outArray[i][3]=Integer.toString(addedNo);
                                } else {
                                    outArray[i][3]=hasmol;
//                                    addMol(outArray[i][3]);
                                }
                            }

                        }
                    }
                    if (found == false) {
                        if (outArray[i][9]!=""){
                            System.out.print(" GET From SMILES ");
                            Molecule importedMol=MolImporter.importMol(outArray[i][9]);

                            Standardizer standardizer = new Standardizer(new File("C:\\Temp_data2\\standardlize_config.xml"));
                            standardizer.setFinalClean();
                            standardizer.standardize(importedMol);
                            String hasmol=hasMol(importedMol);
                            if (hasmol.equals("not found")){

                                addMol(importedMol);
                                outArray[i][3]=Integer.toString(addedNo);
                            } else {
                                outArray[i][3]=hasmol;
//                                addMol(outArray[i][3]);
                            }
                        } else{
                            System.out.println("not found it\t"+outArray[i][0]+"\t"+outArray[i][5]);
                        }
                    }

                }


            }

            // write to new file:
            for (int j=0;j<outArray[i].length;j++){
                pw.print(outArray[i][j]+"\t");
            }
            pw.println();
            System.out.println(" "+outArray[i][3]);
        }

        for(String[] strings:addedMolStruct){
            for(String st:strings){
                pw_cpmd_struct.print(st.replace("\n","\\n")+"\t");
            }
            pw_cpmd_struct.println();
        }

        pw.close();
        pw_cpmd_struct.close();

    }
}

