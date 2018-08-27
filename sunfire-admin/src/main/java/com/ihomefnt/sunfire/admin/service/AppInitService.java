package com.ihomefnt.sunfire.admin.service;

import static com.ihomefnt.sunfire.admin.constants.SunfireAdminConstants.TOPIC_JOIN;
import static com.ihomefnt.sunfire.admin.constants.SunfireAdminConstants.TOPIC_PREFIX;
import static org.apache.hadoop.yarn.util.StringHelper.join;

import com.ihomefnt.sunfire.admin.config.AdminSunfireProperties;
import java.util.Properties;
import javax.annotation.Resource;
import kafka.admin.AdminUtils;
import kafka.admin.RackAwareMode;
import kafka.utils.ZkUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.security.JaasUtils;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AppInitService {

    @Resource
    AdminSunfireProperties properties;

    public Boolean init(String appName) {
        //创建topic
        ZkUtils zkUtils = getZkUtils();
        String topicName = selectTopicName(appName);
        if (!AdminUtils.topicExists(zkUtils, topicName)) {
            AdminUtils.createTopic(zkUtils, topicName, 1, 1, new Properties(),
                    new RackAwareMode.Enforced$());
        } else {
            log.warn("topic has been created before");
        }
        //hbase建表

        return Boolean.FALSE;
    }

    private String selectTopicName(String appName) {
        return join(TOPIC_PREFIX, TOPIC_JOIN, appName.toLowerCase());
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
