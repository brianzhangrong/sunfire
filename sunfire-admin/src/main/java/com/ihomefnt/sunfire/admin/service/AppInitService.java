package com.ihomefnt.sunfire.admin.service;


import static com.ihomefnt.sunfire.config.utils.StringUtils.getHBaseNameByAppId;
import static com.ihomefnt.sunfire.config.utils.StringUtils.selectTopicName;

import com.google.common.base.Preconditions;
import com.ihomefnt.sunfire.admin.config.AdminSunfireProperties;
import java.io.IOException;
import java.util.Properties;
import javax.annotation.Resource;
import kafka.admin.AdminUtils;
import kafka.admin.RackAwareMode;
import kafka.utils.ZkUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.kafka.common.security.JaasUtils;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AppInitService {

    @Resource
    AdminSunfireProperties properties;

    @Resource
    HBaseAdmin hBaseAdmin;

    public Boolean init(String appName) {
        //创建topic
        ZkUtils zkUtils = getZkUtils();
        String topicName = selectTopicName(appName);
        createKafkaTopic(zkUtils, topicName);
        //hbase建表
        createHbaseTable(getHBaseNameByAppId(appName), "log_content");
        return Boolean.FALSE;
    }

    private void createKafkaTopic(ZkUtils zkUtils, String topicName) {
        if (!AdminUtils.topicExists(zkUtils, topicName)) {
            AdminUtils.createTopic(zkUtils, topicName, 1, 1, new Properties(), new RackAwareMode.Enforced$());
        } else {
            log.warn("topic has been created before");
        }
    }

    public void createHbaseTable(String hbaseTableName, String columnName) {
        Preconditions.checkNotNull(columnName);
        try {
            if (hBaseAdmin.isTableAvailable(hbaseTableName)) {//判断表是否存在
                hBaseAdmin.disableTable(hbaseTableName);//关闭表
                hBaseAdmin.deleteTable(hbaseTableName);//删除表
            }
            HTableDescriptor t = new HTableDescriptor(hbaseTableName.getBytes());
            HColumnDescriptor cf1 = new HColumnDescriptor(columnName.getBytes());
            t.addFamily(cf1);
            hBaseAdmin.createTable(t);
        } catch (IOException e) {
            log.error("hbase create error:{}", ExceptionUtils.getStackTrace(e));
        }
    }


    public Boolean appOffline(String appName, Boolean deleteHbase) {
        //删除topic
        ZkUtils zkUtils = getZkUtils();
        String topicName = selectTopicName(appName);
        if (AdminUtils.topicExists(zkUtils, topicName)) {
            AdminUtils.deleteTopic(zkUtils, topicName);
        } else {
            log.warn("delete topic,but topic is not exist");
        }
        //表删除
        return Boolean.FALSE;
    }

    private ZkUtils getZkUtils() {
        return ZkUtils.apply(properties.getZookeeperAddr(), 30000, 30000,
                JaasUtils.isZkSecurityEnabled());
    }
}
