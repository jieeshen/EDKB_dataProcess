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
import chemaxon.formats.MolImporter;
import chemaxon.struc.Molecule;
import jxl.Cell;

import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;


public class createMolStructTab_v0 {
    final public static String EXCELFILE="C:\\Documents and Settings\\jshen\\My Documents\\Research\\EDKB\\ER\\ERDB\\Assays.xls";
    final public static String SHEETNAME="compound_records";


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
        String driver="com.mysql.jdbc.Driver";
        String url="jdbc:mysql://localhost:3306/chembl_13";
        String user="root";
        String password="jsh1234";
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

    public static void main(String args[]) throws Exception {
        ArrayList readMatrix = readExcel(EXCELFILE,SHEETNAME);
        ArrayList titleArray=(ArrayList)readMatrix.get(0);
        int ckIndex=titleArray.indexOf("compound_key");
        int cnIndex=titleArray.indexOf("compound_name");

//Convert ArrayList to 2D array:
        int matrixSize=readMatrix.size();
        String[][] outArray=new String[matrixSize][];
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

        for (int i=1;i<matrixSize;i++){
            if (outArray[i][3]==""){
                if (outArray[i][10]!=""){


                } else{
                    String sdFileName="C:\\Documents and Settings\\jshen\\My Documents\\Research\\EDKB\\ER\\ERDB\\sdf\\"+mappingList.get(outArray[i][0])+".sdf";
                    //                String sdFileName="C:\\Temp_data2\\all_EDKB_Xu_named.sdf";
                    MolImporter mi = new MolImporter(sdFileName);
                    //                System.out.print(outArray[i][5]+" ");
                    Molecule mol;
                    Boolean found=false;
                    while((mol=mi.read())!=null){
                        String molName=new String();
                        molName=mol.getProperty("Name");
                        //                    System.out.println(molName);
                        if (outArray[i][5].equals(molName)){
                            //                        System.out.println("got it "+mol.getName()+" "+outArray[i][5]);
                            found=true;
                        }
                    }
                    if (found==false){
                        System.out.println("not found it\t"+outArray[i][0]+"\t"+outArray[i][5]);
                    }
                }
            }

        }
    }
}
