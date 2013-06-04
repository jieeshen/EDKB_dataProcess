import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: jshen
 * Date: 7/3/12
 * Time: 9:14 PM
 * To change this template use File | Settings | File Templates.
 */
public class test {
    public static void main(String[] args){
        String a="qwerqew213,23,4,]";
        String b=a.replaceAll(",]","]");
        System.out.print(b);
    }
}
