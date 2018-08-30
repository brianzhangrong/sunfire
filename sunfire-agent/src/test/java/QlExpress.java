import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.ihomefnt.sunfire.config.constant.SunfireConstant;
import java.util.List;

public class QlExpress {

    public static void main(String[] args) {
        //
        String log = "|58f8ea8578ad486fa18a6a5102c3b8c8|2018-08-13 17:28:42.548|http-nio-9999-exec-1|INFO |c.i.i.controller.IrayProxyController|-hear end,null";
        List <String> logList = Lists
                .newArrayList(Splitter.on(SunfireConstant.LOG_SPLIT).split(log));
        System.out.println(logList.get(6).substring(1));
    }
}
