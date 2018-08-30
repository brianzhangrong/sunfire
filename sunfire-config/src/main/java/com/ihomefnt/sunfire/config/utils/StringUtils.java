package com.ihomefnt.sunfire.config.utils;

import com.google.common.base.Preconditions;
import com.ihomefnt.sunfire.config.constant.SunfireConstant;
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

    public static Integer searchIndexInMessage(String msg, String key, Integer position) {
        //第一个出现的索引位置
        int cursor = 1;
        int index = msg.indexOf(key);
        if (cursor == position) {
            return index;
        }
        while (index != -1) {
            index = msg.indexOf(key, index + 1);//*从这个索引往后开始第一个出现的位置
            cursor++;
            if (cursor == position) {
                return index;
            }
        }
        return -1;
    }

    public static String dateToRowkey(String date) {
        String ret = date;
        if (org.apache.commons.lang.StringUtils.isNotBlank(date)) {
            for (String remove : SunfireConstant.DATE_REMOVE_KEY) {
                ret = ret.replace(remove, "");
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

    public static String selectTopicName(String appName) {
        return join("sunfire", appName).toLowerCase();
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
//        System.out.println(org.apache.commons.lang.StringUtils
//                .leftPad("1", String.valueOf(SunfireConstant.SEQUENCE).length(), '0'));
        System.out.println(searchIndexInMessage("a|b|c", "|", 2));

    }
}
