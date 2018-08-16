package com.ihomefnt.sunfire.agent.handler;

import com.ihomefnt.sunfire.agent.event.LoggerEvent;
import com.ihomefnt.sunfire.agent.generator.Generator;
import com.ihomefnt.sunfire.agent.generator.RowNameGenerator;
import com.ihomefnt.sunfire.agent.store.OpenTSDBLoggerStore;
import com.ihomefnt.sunfire.agent.store.Store;
import com.lmax.disruptor.EventHandler;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

@Slf4j
@Data
public class LoggerEventHandler implements EventHandler <LoggerEvent> {

    Store <LoggerEvent> store = new OpenTSDBLoggerStore <>();
    Generator rowNameGenerator = new RowNameGenerator();


    @Override
    public void onEvent(LoggerEvent loggerEvent, long l, boolean b) throws Exception {
        if (!StringUtils.isEmpty(loggerEvent.getData().get$rowKey()) && !StringUtils
                .isEmpty(loggerEvent.getData().getTraceId())) {
            store.store(loggerEvent.getData().get$dbName(), loggerEvent.getData().get$rowKey(),
                    loggerEvent);
        }
    }

    private String rowNameGenerator(String rowKey) {
        return StringUtils.isEmpty(rowKey) ? rowNameGenerator.generate() : rowKey;
    }
}
