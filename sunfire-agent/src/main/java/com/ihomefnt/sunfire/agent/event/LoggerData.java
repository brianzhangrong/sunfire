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

    String traceId;

    String $dbName;

    String $rowKey;

    String $openTSDB;

    public String get$dbName() {
        return $dbName;
    }

    public void set$dbName(String $dbName) {
        this.$dbName = $dbName;
    }

    public String get$rowKey() {
        return $rowKey;
    }

    public void set$rowKey(String $rowKey) {
        this.$rowKey = $rowKey;
    }

    public String get$openTSDB() {
        return $openTSDB;
    }

    public void set$openTSDB(String $openTSDB) {
        this.$openTSDB = $openTSDB;
    }
}
