import com.ihomefnt.sunfire.agent.event.LoggerEvent;
import com.ihomefnt.sunfire.agent.generator.FamilyNameGenerator;
import com.ihomefnt.sunfire.agent.generator.Generator;
import java.lang.reflect.Field;

public class LoggerDOTest {

    public static void main(String[] args) {

        LoggerEvent loggerEvent = new LoggerEvent();
        Field[] fields = loggerEvent.getClass().getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            //
            System.out.println(familyNameGenerator(field.getName()).generate());
        }
    }

    public static Generator familyNameGenerator(String field) {
        return new FamilyNameGenerator(field);
    }
}
