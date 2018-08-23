package com.ihomefnt.sunfire.agent.config;

import com.ihomefnt.sunfire.agent.ringbuffer.LoggerRingBuffer;
import lombok.Data;

@Data
public class ZkDataConfig {


    LoggerRingBuffer ringBuffer;
}
