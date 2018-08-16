import com.ihomefnt.sunfire.agent.event.LoggerData;
import com.ihomefnt.sunfire.agent.event.LoggerEvent;
import com.ihomefnt.sunfire.agent.ringbuffer.LoggerRingBuffer;
import com.ihomefnt.sunfire.agent.store.OpenTSDBLoggerStore;
import java.text.SimpleDateFormat;
import java.util.Date;

public class HBTest {

    // {"appName":"irayTest","bizName":"[background-preinit]","createTime":"2018-08-10 17:41:18",
    // "loggerContent":"17:39:45.073 [background-preinit] INFO  o.h.v.internal.util.Version -HV000001:
    // Hibernate Validator 5.3.6.Final","loggerTime":"17:39:45.073","splitExpress":""}
    public static void main(String[] args) {
        //
        write();
    }

    public static void write() {
        int i = 0;
        while (true) {
            if (i == 20) {
                break;
            }
            i++;
            OpenTSDBLoggerStore store = new OpenTSDBLoggerStore <LoggerEvent>();
            LoggerRingBuffer ringBuffer = new LoggerRingBuffer("sunfire_iraytest");
            LoggerData data = new LoggerData();
            data.setAppName("irayTest");

            data.setBizName("[background-preinit]");
            data.setCreateTime("2018-08-10 17:41:18");
            data.setLoggerContent("test| Validator 5.3.6.Final");
            data.setLoggerTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()));
            data.setSplitExpress("");
            data.setTraceId("333");
            data.set$rowKey("2018-08-15 13:50:03.222");
            data.set$openTSDB("localhost:4242");
            ringBuffer.publish(data);
            System.out.println(i);
        }
    }
}
