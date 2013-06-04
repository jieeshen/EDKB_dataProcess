

/**
 * Created with IntelliJ IDEA.
 * By: Jie Shen @ NCTR, FDA
 * Date: 6/18/12
 * Time: 8:29 AM
 * <p/>
 * Description: This class is used to get the CAS numbers of the compound and generate a mapping list table. It reads
 *              the molecule inchi from erdb mysql database and searches from http://cactus.nci.nih.gov. Then it writes
 *              a table containing three columns: casno, cas_rn, molregno.
 *
 */

import java.io.*;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

public class getCAS {
    final public static String MOLLISTFILE="C:\\Temp_data2\\sorted_molregno.txt";
    final public static String OUTPUTSDFILE="C:\\Temp_data2\\molcas.txt";
    final public static String driver="com.mysql.jdbc.Driver";
    final public static String url="jdbc:mysql://localhost:3306/chembl_13";
    final public static String user="root";
    final public static String password="jsh1234";


    public static void main(String args[]) throws Exception {
        ArrayList molList=writeSdfFromDb.readList(MOLLISTFILE);
        Class.forName(driver);
        Connection conn = DriverManager.getConnection(url, user, password);
        Statement st= conn.createStatement();
        PrintWriter pw=new PrintWriter(new OutputStreamWriter(new FileOutputStream(OUTPUTSDFILE)),true);
        pw.println("casno\tcas_rn\tmolregno");
        for(int i=1;i<=molList.size();i++){
            String molregno=molList.get(i-1).toString(); //got the molregno
            System.out.print(i+":"+molregno+"\t");
            // got the molecule information
            String sqlquery_Mol="select molregno, standard_inchi from er_invitro_compound_structures where molregno="+molregno+";";
            ResultSet rsM=st.executeQuery(sqlquery_Mol);
            if (rsM.next()){
                String inchi = rsM.getString("standard_inchi");
                String urlstr = "http://cactus.nci.nih.gov/chemical/structure/"+inchi+"/cas";
                URL url = new URL(urlstr);
                try {
                    BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
                    String s;
                    while((s=br.readLine())!=null && s.indexOf("not found")<0){
                        System.out.print(s+"\t");
                        pw.println(s.replaceAll("-","")+"\t"+s+"\t"+molregno);
                    }
                } catch (Exception e){
                    System.out.print("not found");
                }

            }
            System.out.println();
        }



    }

}
