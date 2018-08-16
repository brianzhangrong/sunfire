package com.ihomefnt.sunfire.agent.event;

import com.lmax.disruptor.EventFactory;

public class LoggerEventFactory implements EventFactory<LoggerEvent> {

    @Override
    public LoggerEvent newInstance() {
        return new LoggerEvent();
    }
}
