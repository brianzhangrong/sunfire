package com.ihomefnt.sunfire.agent.event;

import com.alibaba.fastjson.JSON;
import lombok.Data;

@Data
public class LoggerEvent {


    LoggerData data;

    @Override
    public String toString() {
        return JSON.toJSONString(this);
    }
}
