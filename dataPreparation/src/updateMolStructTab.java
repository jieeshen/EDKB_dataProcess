import chemaxon.formats.MolExporter;
import chemaxon.formats.MolImporter;
import chemaxon.reaction.Standardizer;
import chemaxon.struc.Molecule;
import jxl.Cell;

import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created with IntelliJ IDEA.
 * User: JShen
 * Date: 12/13/12
 * Time: 9:12 AM
 * To change this template use File | Settings | File Templates.
 */
public class updateMolStructTab {

        final public static String EXCELFILE="C:\\JShen\\Research\\EDKB\\ER\\DECA\\121211\\UT_input.xls";
        final public static String SHEETNAME="updated_inchikey";
        final public static String driver="com.mysql.jdbc.Driver";
        final public static String url="jdbc:mysql://localhost:3306/";
        final public static String user="root";
        final public static String password="jsh1234";

        //    public static ArrayList<String[]> addedMolDic = new ArrayList<String[]>();
        public static ArrayList<String[]> addedMolStruct = new ArrayList<String[]>();
        public static int addedNo=5000946;

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

        public static ArrayList searchEmblByIck(String ickstr) {

            String sqlquery="select * from compound_structures where standard_inchi_key=\""+ickstr+"\";";
            ArrayList resultArray = new ArrayList();
            try{
                Class.forName(driver);
                Connection conn = DriverManager.getConnection(url+"chembl_14", user, password);
                Statement st= conn.createStatement();

//            System.out.println(sqlqueryByName);
                ResultSet rs=st.executeQuery(sqlquery);
//            System.out.println(rs.getArray("molregno"));
                while(rs.next()){
                    resultArray.add(rs.getString("molregno"));
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

        public static ArrayList searchDECAByIckstr(String ickstr) {
            String sqlquery="select * from er_invitro_compound_structures where standard_inchi_key=\""+ickstr+"\";";
            ArrayList resultArray = new ArrayList();
            try{
                Class.forName(driver);
                Connection conn = DriverManager.getConnection(url+"erad_120828", user, password);
                Statement st= conn.createStatement();

//            System.out.println(sqlqueryByName);
                ResultSet rs=st.executeQuery(sqlquery);
//            System.out.println(rs.getArray("molregno"));
                while(rs.next()){
                    resultArray.add(rs.getString("molregno"));
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





        public static String addMol(Molecule mol) throws Exception{
            addedNo++;
            System.out.print(" add a mol:"+Integer.toString(addedNo));
            String[] cpmd_struct=new String[6];
            cpmd_struct[0]=Integer.toString(addedNo);
            cpmd_struct[1]=MolExporter.exportToFormat(mol,"mol");
            cpmd_struct[2]=MolExporter.exportToFormat(mol,"inchi").split("\n")[0];
            cpmd_struct[3]=MolExporter.exportToFormat(mol,"inchikey").split("=")[1];
            cpmd_struct[4]=MolExporter.exportToFormat(mol,"smiles:u");
            cpmd_struct[5]=mol.getFormula();

            Class.forName(driver);
            Connection conn1 = DriverManager.getConnection(url+"erad_120828", user, password);
            Statement st1= conn1.createStatement();
            String updatesql="insert into er_invitro_compound_structures values('"+cpmd_struct[0]+"','"+cpmd_struct[1]
                    +"','"+cpmd_struct[2]+"','"+cpmd_struct[3]+"','"+cpmd_struct[4]+"','"+cpmd_struct[5]+"')";

            try{
                st1.executeUpdate(updatesql);
            }catch (Exception e){
                System.out.print(" already in ");

            }
            addedMolStruct.add(cpmd_struct);
            conn1.close();
            return cpmd_struct[0];

        }

        public static void addMolbymrn(String molregno) throws Exception{
            Class.forName(driver);
            Connection conn = DriverManager.getConnection(url+"chembl_14", user, password);
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
                Class.forName(driver);
                Connection conn1 = DriverManager.getConnection(url+"erad_120828", user, password);
                Statement st1= conn1.createStatement();
                String updatesql="insert into er_invitro_compound_structures values('"+cpmd_struct[0]+"','"+cpmd_struct[1]
                        +"','"+cpmd_struct[2]+"','"+cpmd_struct[3]+"','"+cpmd_struct[4]+"','"+cpmd_struct[5]+"')";


                try{
                    st1.executeUpdate(updatesql);
                }catch (Exception e){
                    System.out.print(" already in ");
                }

                addedMolStruct.add(cpmd_struct);
                conn1.close();
            }
            conn.close();
        }


        public static void main(String args[]) throws Exception {

            ArrayList readMatrix = readExcel(EXCELFILE,SHEETNAME);
            ArrayList titleArray=(ArrayList)readMatrix.get(0);
            int ickIndex=titleArray.indexOf("INCHIKEY");
            int mrnIndex=titleArray.indexOf("molregno");
            int mrn2Index=titleArray.indexOf("molregno2");
            int smiIndex=titleArray.indexOf("SMILES");

//Convert ArrayList to 2D array:
            int matrixSize=readMatrix.size();
            String[][] outArray=new String[matrixSize][];
            //transfer ArrayList to 2D array
            for (int i=0;i<matrixSize;i++){

                ArrayList tempArray=(ArrayList)readMatrix.get(i);
                outArray[i]=(String[])tempArray.toArray(new String[0]); // !! Should add "new String[0]"
            }


            PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream("C:\\JShen\\Research\\EDKB\\ER\\DECA\\121211\\newCmpdRecs.txt")),true);


            for (int i=1;i<matrixSize;i++){
//        for (int i=1;i<11;i++){
                System.out.print(Integer.toString(i)+" "+outArray[i][ickIndex]+" "+outArray[i][mrnIndex]+" ");
                if (outArray[i][mrn2Index]!=""){
                    addMolbymrn(outArray[i][mrn2Index]);
                    outArray[i][mrnIndex]=outArray[i][mrn2Index];
                }else{
                    if (outArray[i][mrnIndex] == "") {
                        ArrayList<String> returnList=searchDECAByIckstr(outArray[i][ickIndex]);
                        if (returnList.size()>0){
                            outArray[i][mrnIndex]=returnList.get(0);
                        }else{
                            //update mol structure table
                            Molecule importedMol=MolImporter.importMol(outArray[i][smiIndex]);
                            outArray[i][mrnIndex]=addMol(importedMol);
                        }
                    }
                }
                // write to new file:
                for (int j=0;j<outArray[i].length;j++){
                    pw.print(outArray[i][j]+"\t");
                }
                pw.println();
                System.out.println();
            }


            pw.flush();
            pw.close();


        }
}





