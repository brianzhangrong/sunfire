package com.ihomefnt.sunfire.admin.controller;

import com.ihomefnt.sunfire.admin.config.AdminSunfireProperties;
import com.ihomefnt.sunfire.admin.constants.SunfireAdminConstants;
import com.ihomefnt.sunfire.admin.service.AppInitService;
import javax.annotation.Resource;
import org.springframework.data.hadoop.hbase.HbaseTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SunfireAdminController {

    @Resource
    HbaseTemplate hbaseTemplate;
    @Resource
    AdminSunfireProperties properties;
    @Resource
    AppInitService appInitService;
//    @Resource
//    AdminClient adminClient;
//
//    @RequestMapping(value = "/init", method = {RequestMethod.GET, RequestMethod.POST})
//    public String hello(String appName) {
//        //1.hbase创建 sunfire_appName 表，2.kafka创建 sunfire_appName topic 3.创建规则引擎
//        //hbaseTemplate.
//        List <NewTopic> topicList = Lists
//                .newArrayList(new NewTopic("sunfire_" + appName.toLowerCase(), 1, (short) 1));
//        adminClient.createTopics(topicList);
//        return "hi";
//    }


    @RequestMapping(value = "/init", method = {RequestMethod.GET, RequestMethod.POST})
    public String init(String appName) {
        appInitService.init(appName);

        return SunfireAdminConstants.SUCCESS;
    }


    @RequestMapping(value = "/updateRegular", method = {RequestMethod.GET, RequestMethod.POST})
    public String updateRegular(String appName) {
        //1.hbase创建 sunfire_appName 表，2.kafka创建 sunfire_appName topic 3.创建规则引擎
        //hbaseTemplate.

        return SunfireAdminConstants.SUCCESS;
    }
}
