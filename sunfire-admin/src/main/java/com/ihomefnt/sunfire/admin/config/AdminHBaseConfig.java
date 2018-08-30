package com.ihomefnt.sunfire.admin.config;

import java.io.IOException;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.hadoop.hbase.HbaseTemplate;

@Configuration
public class AdminHBaseConfig {

    @Bean
    public HbaseTemplate hbaseTemplate() {
        org.apache.hadoop.conf.Configuration configuration = new org.apache.hadoop.conf.Configuration();
        configuration.addResource("hbase.xml");
        return new HbaseTemplate(configuration);
    }

    @Bean
    public HBaseAdmin hBaseAdmin(AdminSunfireProperties properties) throws IOException {
        org.apache.hadoop.conf.Configuration config = HBaseConfiguration.create();
        // 链接zookeeper
        config.set("hbase.zookeeper.quorum", properties.getZookeeperAddr());
        return new HBaseAdmin(config);
    }
}
