package com.ihomefnt.sunfire.config.constant;

public class SunfireConstant {

    public static final String LOG_SPLIT = "|";
    //kafka  head  key  ip=；appname=
    public static final String APP_IP = "ip";
    public static final String APP_BIZ = "b";
    public static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";
    //opentsdb metrics上传地址
    public static final String OPENTSDB_REQUEST_URL = "http://%s/api/put?summary";
    //hbase rowkey要删除的字符串
    public static final String[] DATE_REMOVE_KEY = new String[]{"-", ":", ".", " "};


    public static final String TRACE_ID = "traceId";
    //app 日志切分配置表
    public static final String APPCONFIG_COLUME_FAMLIY = "regular_config";
    public static final String CONFIG_TABLE = "app_config";
    public static final String CONFIG_FAMLIY_BEING_PREFIX = "begin";
    public static final String CONFIG_FAMLIY_END_PREFIX = "end";
    public static final String CONFIG_FAMLIY_SPLIT = "_";
    public static final String CONFIG_ATTR_VALUE = "attribute";
    //hbase rowkey尾缀长度
    public static final Long SEQUENCE = 100000L;
}
