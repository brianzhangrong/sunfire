package com.ihomefnt.sunfire.agent.event;

import lombok.Data;

@Data
public class LoggerData {

    /*
     * 插入hbase时间
     * */

    String createTime;
    /*
     * 日志内容
     * */

    String loggerContent;
    /*
     * 日志时间
     * */

    String loggerTime;
    /*
     * 所属应用
     * */

    String appName;
    /*
     * 应用业务id
     * */

    String bizName;
    /*
     * 分割表达式
     * */

    String splitExpress;
    /*
     * ip:port
     * */

    String addrInfo;

    /**
     * dapper
     */
    String traceId;

    /**
     * 应用上报ip
     */
    String ip;
}
