import java.text.SimpleDateFormat;
import java.util.Date;

public class TimeTest {

    public static void main(String[] args) {
        //
        System.out.println(
                String.valueOf(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())));
    }
}
