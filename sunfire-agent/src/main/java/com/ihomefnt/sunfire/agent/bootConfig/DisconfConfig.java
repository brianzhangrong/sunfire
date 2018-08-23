package com.ihomefnt.sunfire.agent.bootConfig;

import com.baidu.disconf.client.DisconfMgrBean;
import com.baidu.disconf.client.DisconfMgrBeanSecond;
import com.baidu.disconf.client.addons.properties.ReloadablePropertiesFactoryBean;
import com.baidu.disconf.client.addons.properties.ReloadingPropertyPlaceholderConfigurer;
import com.baidu.disconf.client.config.DisClientConfig;
import com.baidu.disconf.client.config.DisClientSysConfig;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

@Configuration
@Slf4j
public class DisconfConfig implements EnvironmentAware {

    public static final String PRD_ENV = "prd";
    public static final String SIT_ENV = "sit";
    public static final String SIT_DISCONF_URL = "http://disconf.sit.ihomefnt.org/";
    public static final String PRD_DISCONF_URL = "http://disconf.ihomefnt.com/";
    private ConfigurableEnvironment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = (ConfigurableEnvironment) environment;
        // 加载Disconf 系统自带的配置
        try {
            loadDisClientSysConfig();
        } catch (Exception e) {
            log.error("disconfconfig error:{}", ExceptionUtils.getStackTrace(e));
        }
    }

    @Bean(name = "disconfMgrBean")
    public DisconfMgrBean disconfMgrBean() {
        DisClientConfig.getInstance().ENABLE_DISCONF = true;
        DisconfMgrBean disconfMgrBean = new DisconfMgrBean();
        disconfMgrBean.setScanPackage("com.ihomefnt");
        return disconfMgrBean;
    }

    @Bean(initMethod = "init", destroyMethod = "destroy")
    public DisconfMgrBeanSecond disconfMgrBeanSecond() {
        return new DisconfMgrBeanSecond();
    }

    @Bean("disconfReloadablePropertiesFactoryBean")
    @ConditionalOnClass(DisconfMgrBean.class)
    public ReloadablePropertiesFactoryBean reloadFactoryBean() {
        ReloadablePropertiesFactoryBean bean = new ReloadablePropertiesFactoryBean();
        bean.setLocations(Lists.newArrayList("classpath*:irayCloud.properties"));
        return bean;
    }

    @Bean
    @ConditionalOnClass(ReloadablePropertiesFactoryBean.class)
    public ReloadingPropertyPlaceholderConfigurer reloadConfig(
            @Qualifier("disconfReloadablePropertiesFactoryBean") ReloadablePropertiesFactoryBean factoryBean)
            throws IOException {
        ReloadingPropertyPlaceholderConfigurer configure = new ReloadingPropertyPlaceholderConfigurer();
        configure.setIgnoreResourceNotFound(true);
        configure.setIgnoreUnresolvablePlaceholders(true);
        final List <Resource> resourceLst = Lists.newArrayList();
        resourceLst.add(new ClassPathResource("irayCloud.properties"));
        configure.setLocations(resourceLst.toArray(new Resource[]{}));
        configure.setProperties(factoryBean.getObject());
        addPropertiesPropertySource("disconfReloadableProperties", factoryBean.getObject());
        return configure;
    }

    private void addPropertiesPropertySource(String name, Properties source) {
        PropertiesPropertySource propertiesPropertySource = new PropertiesPropertySource(name,
                source);
        environment.getPropertySources().addLast(propertiesPropertySource);
    }

    private void loadDisClientSysConfig() throws Exception {
        // 加载Disconf 系统自带的配置
        DisClientSysConfig disClientSysConfig = DisClientSysConfig.getInstance();
        DisClientConfig disClientConfig = DisClientConfig.getInstance();

        String serverHost = System.getProperty("spring.disconf.conf-server-host");
        String env = System.getProperty("spring.disconf.env");
        String envirment = environment.getProperty("spring.profiles.active");
        if (StringUtils.isEmpty(envirment)) {
            envirment = SIT_ENV;
        }
        //TODO: 上生产前注释调
        //envirment=SIT_ENV;
        log.info("---------current profile:{}", envirment);
        if (StringUtils.isNotEmpty(envirment) && envirment.equals("prd")) {
            serverHost = PRD_DISCONF_URL;
            env = PRD_ENV;
        } else {
            serverHost = SIT_DISCONF_URL;
            env = SIT_ENV;
        }
        log.info("current envirment is ----------------> activeprofile:{},disconf:{},env:{}",
                envirment, serverHost, env);
        disClientConfig.loadConfig(null);
        disClientSysConfig.loadConfig(null);
        if (StringUtils.isNoneEmpty(env)) {
            disClientConfig.ENV = env;
        }
        if (StringUtils.isNotEmpty(serverHost)) {
            disClientConfig.CONF_SERVER_HOST = serverHost;
        }
    }


}
