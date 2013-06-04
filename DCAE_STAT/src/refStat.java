/**
 * Created with IntelliJ IDEA.
 * User: JShen
 * Date: 11/5/12
 * Time: 3:22 PM
 * To change this template use File | Settings | File Templates.
 */


import java.io.*;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.util.Calendar;
import java.util.Map;

public class refStat {
    final public static String OUTPUTFILE="C:\\JShen\\Research\\EDKB\\ER\\DEAC\\130429\\ref_stats.txt";
    final public static String driver="com.mysql.jdbc.Driver";
    final public static String url="jdbc:mysql://localhost:3306/deac_130429";
    final public static String user="root";
    final public static String password="";

    public static void main(String args[]) throws Exception {
        // set connection to mysql db.
        Class.forName(driver);
        Connection conn = DriverManager.getConnection(url, user, password);
        Statement st= conn.createStatement();
        Statement stinner= conn.createStatement();

        //set output file
        PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(OUTPUTFILE)),true);
        pw.printf("ref_id\tJournal\tYear\tVolume\tIssue\tFirst Page\tLast Page\t#Binding\t#Reporter Gene\t" +
                "#Cell Proliferation\t#In Vivo\t#Total\n");

        String sqRef="select * from er_invitro_refs order by year,journal;";
        ResultSet sqRs=st.executeQuery(sqRef);

        String refid=new String();
        String sqBinding=new String();
        String sqReporter=new String();
        String sqCell=new String();
        String sqTotal=new String();
        String sqVivo=new String();

        while (sqRs.next()){
            refid=sqRs.getString("ref_id");
            pw.print( refid + "\t" + sqRs.getString("journal") + "\t" + sqRs.getString("year") + "\t" +
                    sqRs.getString("volume") + "\t" + sqRs.getString("issue") + "\t" + sqRs.getString("first_page") +
                    "\t" + sqRs.getString("last_page")+"\t");
            sqBinding="select COUNT(a.activity_id) from er_invitro_activities a, er_invitro_assays b " +
                    "where a.assay_id=b.assay_id AND b.assay_type=\"Binding\" AND a.ref_id="+refid + ";";
            ResultSet sqB=stinner.executeQuery(sqBinding);
            if (sqB.next()){

            } else {
                System.out.println("not found in db... Something wrong....");
            }
            pw.print(sqB.getString("COUNT(a.activity_id)"));

            sqReporter="select COUNT(a.activity_id) from er_invitro_activities a, er_invitro_assays b " +
                    "where a.assay_id=b.assay_id AND b.assay_type=\"Reporter Gene\" AND a.ref_id="+refid + ";";
            ResultSet sqR=stinner.executeQuery(sqReporter);
            if (sqR.next()){

            } else {
                System.out.print("not found in db... Something wrong....");
            }
            pw.print("\t" + sqR.getString("COUNT(a.activity_id)"));

            sqCell="select COUNT(a.activity_id) from er_invitro_activities a, er_invitro_assays b " +
                    "where a.assay_id=b.assay_id AND b.assay_type=\"Cell Proliferation\" AND a.ref_id="+refid + ";";
            ResultSet sqC=stinner.executeQuery(sqCell);
            if (sqC.next()){

            } else {
                System.out.println("not found in db... Something wrong....");
            }
            pw.print("\t" + sqC.getString("COUNT(a.activity_id)"));

            sqVivo="select COUNT(a.activity_id) from er_invitro_activities a, er_invitro_assays b " +
                    "where a.assay_id=b.assay_id AND b.assay_type=\"In Vivo\" AND a.ref_id="+refid + ";";
            ResultSet sqV=stinner.executeQuery(sqVivo);
            if (sqV.next()){

            } else {
                System.out.println("not found in db... Something wrong....");
            }
            pw.print("\t" + sqV.getString("COUNT(a.activity_id)"));

            sqTotal="select COUNT(a.activity_id) from er_invitro_activities a " +
                    "where a.ref_id="+refid + ";";
            ResultSet sqT=stinner.executeQuery(sqTotal);
            if (sqT.next()){

            } else {
                System.out.println("not found in db... Something wrong....");
            }
            pw.print("\t" + sqT.getString("COUNT(a.activity_id)") + "\n");

        }
        pw.flush();
        pw.close();
    }

}
