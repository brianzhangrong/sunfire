package com.ihomefnt.sunfire.agent.ringbuffer;

import com.ihomefnt.sunfire.agent.event.LoggerData;
import com.ihomefnt.sunfire.agent.event.LoggerEvent;
import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.EventTranslatorOneArg;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.YieldingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AbstractRingBufferPublish<T> implements RingBufferPublish <T> {

    int ringBufferSize = 1024 * 1024; // RingBuffer 大小，必须是 2 的 N 次方；过小会积压造成内存OOM
    EventFactory eventFactory;
    ExecutorService executor = Executors
            .newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);
    Disruptor <LoggerEvent> disruptor;
    EventHandler eventHandler;
    EventTranslatorOneArg TRANSLATOR;

    public AbstractRingBufferPublish() {

    }

    public void init() {
        disruptor = new Disruptor <>(eventFactory, ringBufferSize, executor, ProducerType.SINGLE,
                new YieldingWaitStrategy());
        disruptor.handleEventsWith(eventHandler);
        disruptor.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            disruptor.shutdown();//关闭 disruptor，方法会堵塞，直至所有的事件都得到处理；
            executor.shutdown();//关闭 disruptor 使用的线程池；如果需要的话，必须手动关闭， disruptor 在 shutdown 时不会自动关闭；
        }));
    }

    @Override
    public void publish(T data) {
        RingBuffer <LoggerEvent> ringBuffer = disruptor.getRingBuffer();
        ringBuffer.publishEvent(TRANSLATOR, data);
    }


    static class LoggerEventTranslator implements EventTranslatorOneArg <LoggerEvent, LoggerData> {

        @Override
        public void translateTo(LoggerEvent event, long l, LoggerData data) {
            event.setData(data);
        }
    }
}
