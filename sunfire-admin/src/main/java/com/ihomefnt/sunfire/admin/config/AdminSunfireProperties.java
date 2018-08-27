package com.ihomefnt.sunfire.admin.config;

import com.baidu.disconf.client.common.annotations.DisconfFile;
import com.baidu.disconf.client.common.annotations.DisconfFileItem;
import com.baidu.disconf.client.common.annotations.DisconfUpdateService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration
@Scope("singleton")
@DisconfFile(filename = "sunfire-admin.properties")
@DisconfUpdateService(classes = AdminSunfireProperties.class)
@Slf4j
@Data
public class AdminSunfireProperties {

    String brokenList;

    String zookeeperAddr;


    @DisconfFileItem(name = "kafka.brokenlist", associateField = "brokenList")
    public String getBrokenList() {
        return brokenList;
    }

    @DisconfFileItem(name = "zookeep.addr", associateField = "zookeeperAddr")
    public String getZookeeperAddr() {
        return zookeeperAddr;
    }
}
