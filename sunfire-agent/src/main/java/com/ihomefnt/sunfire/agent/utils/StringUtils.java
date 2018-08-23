package com.ihomefnt.sunfire.agent.utils;

import com.google.common.base.Preconditions;
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
        String ret = date;
        if (org.apache.commons.lang.StringUtils.isNotBlank(date)) {
            for (String remove : SunfireConstant.DATE_REMOVE_KEY) {
                ret = ret.replaceAll(remove, "");
            }
            return ret;
        }
        return "";
    }

    public static String now() {
        return String.valueOf(new SimpleDateFormat(SunfireConstant.DATE_FORMAT).format(new Date()));
    }

    public static String getHBaseNameByAppId(String appName) {
        return join("sunfire", "_", appName).toLowerCase();
    }


    public static String humpToUnderline(String para) {
        Preconditions.checkNotNull(para);
        StringBuilder sb = new StringBuilder(para);
        int temp = 0;//定位
        for (int i = 0; i < para.length(); i++) {
            if (Character.isUpperCase(para.charAt(i))) {
                sb.insert(i + temp, "_");
                temp += 1;
            }
        }
        return sb.toString().toLowerCase();
    }

    public static void main(String[] args) {
        //
        System.out.println(org.apache.commons.lang.StringUtils
                .leftPad("1", String.valueOf(SunfireConstant.SEQUENCE).length(), '0'));
    }
}
