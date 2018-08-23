package com.ihomefnt.sunfire.agent.handler;

import com.ihomefnt.sunfire.agent.event.LoggerEvent;
import com.lmax.disruptor.EventHandler;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
public class LoggerEventHandler implements EventHandler <LoggerEvent> {



    @Override
    public void onEvent(LoggerEvent loggerEvent, long l, boolean b) throws Exception {

    }


}
