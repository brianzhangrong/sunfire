package com.ihomefnt.sunfire.agent.bootConfig;

import com.baidu.disconf.client.common.annotations.DisconfFile;
import com.baidu.disconf.client.common.annotations.DisconfFileItem;
import com.baidu.disconf.client.common.annotations.DisconfUpdateService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration
@Scope("singleton")
@DisconfFile(filename = "sunfire.properties")
@DisconfUpdateService(classes = SunfireProperties.class)
@Slf4j
@Data
public class SunfireProperties {

    String openTSDB;

    @DisconfFileItem(name = "openTSDB", associateField = "openTSDB")
    public String getOpenTSDB() {
        return openTSDB;
    }
}
