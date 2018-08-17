package com.ihomefnt.sunfire.agent.constant;

public class SunfireConstant {

    public static final String LOG_SPLIT = "|";
    public static final String APP_IP = "ip";
    public static final String APP_BIZ = "b";
    public static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";
    public static final String OPENTSDB_REQUEST_URL = "http://%s/api/put?summary";
    public static final String[] DATE_REMOVE_KEY = new String[]{"-", ":", ".", " "};

    public static final Long SEQUENCE = 100000L;
}
