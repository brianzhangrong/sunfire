package com.ihomefnt.sunfire.agent.utils;

import com.ihomefnt.sunfire.agent.constant.SunfireConstant;
import java.text.SimpleDateFormat;
import java.util.Date;

public class StringUtils {

    public static String join(String... a) {
        StringBuilder builder = new StringBuilder();
        for (String tmp : a) {
            builder.append(tmp);
        }
        return builder.toString();
    }

    public static String dateToRowkey(String date) {
        if (org.apache.commons.lang.StringUtils.isNotBlank(date)) {
            for (String remove : SunfireConstant.DATE_REMOVE_KEY) {
                date.replaceAll(remove, "");
            }
            return date;
        }
        return "";
    }

    public static String now() {
        return String.valueOf(new SimpleDateFormat(SunfireConstant.DATE_FORMAT).format(new Date()));
    }

}
