package com.ihomefnt.sunfire.agent.ringbuffer;

import com.ihomefnt.sunfire.agent.event.LoggerData;
import com.ihomefnt.sunfire.agent.event.LoggerEventFactory;
import com.ihomefnt.sunfire.agent.handler.LoggerEventHandler;
import lombok.Data;
import org.springframework.util.StringUtils;

@Data
public class LoggerRingBuffer extends AbstractRingBufferPublish <LoggerData> {


    String dbName;

    public LoggerRingBuffer(String dbName) {
        this.dbName = dbName;
        eventHandler = new LoggerEventHandler();
        TRANSLATOR = new LoggerEventTranslator();
        eventFactory = new LoggerEventFactory();
        init();

    }

    @Override
    public void publish(LoggerData data) {
        //        RingBuffer<LoggerEvent> ringBuffer = disruptor.getRingBuffer();
        //        long sequence = ringBuffer.next();//请求下一个事件序号；
        //
        //        try {
        //            LoggerEvent event = ringBuffer.get(sequence);//获取该序号对应的事件对象；
        //            event.setData(data);
        //        } finally {
        //            ringBuffer.publish(sequence);//发布事件；
        //        }
        if (StringUtils.isEmpty(data.get$dbName())) {
            data.set$dbName(dbName);
        }
        super.publish(data);

    }


}
