package com.ihomefnt.sunfire.agent.kafka;

import static com.ihomefnt.sunfire.config.utils.StringUtils.dateToRowkey;
import static com.ihomefnt.sunfire.config.utils.StringUtils.getHBaseNameByAppId;
import static com.ihomefnt.sunfire.config.utils.StringUtils.humpToUnderline;
import static com.ihomefnt.sunfire.config.utils.StringUtils.join;
import static com.ihomefnt.sunfire.hbase.utils.RuleUtils.isFixedRule;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.ihomefnt.sunfire.admin.constants.SunfireClientContants;
import com.ihomefnt.sunfire.agent.event.LoggerData;
import com.ihomefnt.sunfire.agent.store.OpenTSDBMetricStore;
import com.ihomefnt.sunfire.config.constant.SunfireConstant;
import com.ihomefnt.sunfire.hbase.model.Regular;
import com.ihomefnt.sunfire.hbase.service.AppRegularModifyService;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.data.hadoop.hbase.HbaseTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

@Component
@Slf4j
public class SunfireKafkaSinkConsumer {

    private final static AtomicLong sequence = new AtomicLong(0);
    @Resource
    HbaseTemplate hbaseTemplate;
    @Resource
    OpenTSDBMetricStore openTSDBMetricStore;
    @Resource
    AppRegularModifyService appRegularModifyService;

    @KafkaListener(topics = {"sunfireirayproxy"})
    public void listen(ConsumerRecord <?, ?> record) {

        Optional <?> kafkaMessage = Optional.ofNullable(record.value());

        if (kafkaMessage.isPresent()) {

            String message = (String) kafkaMessage.get();
            if (StringUtils.isEmpty(message)) {
                return;
            }

            String key = (String) record.key();
            String ip = getClientIpByHeader(key, SunfireClientContants.IP_HEADER);
            String appName = getClientIpByHeader(key, SunfireClientContants.APP_NAME);

            log.info("msg app:{},ip:{}, in:{}", appName, ip, message);

            LoggerData data = new LoggerData();
            data.setAppName(appName);
            data.setCreateTime(com.ihomefnt.sunfire.config.utils.StringUtils.now());
            data.setSplitExpress("");
            data.setIp(ip);
            //rowkey作为reginserver的分区管理  日志发生时间+6位随机数
            String rowName = "";
            List <Regular> regularList = appRegularModifyService.selectAppRegular(appName);
            Collections.sort(regularList);
            if (isFixedRule(regularList, message)) {
                //按logback特定日志格式，录入hbase
                insertHBaseLogContent(message, appName, data);
                //规则引擎匹配，录入opentsdb
                openTSDBMetricStore.put(appName, ip, data, regularList);
            }
        }

    }

    private void insertHBaseLogContent(String message, String appName, LoggerData data) {
        String rowName;
        if (message.contains(SunfireConstant.LOG_SPLIT)) {

            List <String> bodyList = Lists
                    .newArrayList(Splitter.on(SunfireConstant.LOG_SPLIT).split(message));
            // yyyy-MM-dd  hh:mm:ss.SSS 作为rowkey +6位的数字，不足位补0
            rowName = dateToRowkey(bodyList.get(1));
            data.setLoggerTime(bodyList.get(1));
            // 线程名
            data.setBizName(bodyList.get(2));
            data.setTraceId(bodyList.get(5));
            data.setLoggerContent(message);
            Field[] fields = data.getClass().getDeclaredFields();

            rowName = join(rowName, org.apache.commons.lang.StringUtils
                    .leftPad(String.valueOf(sequence.getAndIncrement()),
                            String.valueOf(SunfireConstant.SEQUENCE).length(), '0'));
            if (sequence.compareAndSet(SunfireConstant.SEQUENCE, 0)) {
                log.info("sequence set zero");
            }
            for (Field field : fields) {
                field.setAccessible(true);
                String fieldName = field.getName();
                if (StringUtils.isEmpty(fieldName) || fieldName.contains("$")) {
                    continue;
                }
                String qualifierName = humpToUnderline(fieldName);
                Object value = ReflectionUtils.getField(field, data);
                String colValue = value == null ? "" : String.valueOf(value);
                hbaseTemplate
                        .put(getHBaseNameByAppId(appName), rowName, getFamilyName(), qualifierName,
                                colValue.getBytes());
            }
        }
    }

    private String getFamilyName() {
        return "log_content";
    }

    private String getClientIpByHeader(String key, String header) {
        String data = "";
        if (!StringUtils.isEmpty(key)) {
            List <String> keyList = Lists
                    .newArrayList(Splitter.on(SunfireClientContants.HEADER_SPLIT).split(key));
            Optional <String> ipHead = keyList.stream().filter(k -> k.contains(header)).findFirst();

            if (ipHead.isPresent()) {
                data = Lists.newArrayList(
                        Splitter.on(SunfireClientContants.VALUE_SPLIT).split(ipHead.get())).get(1);
            }
        }
        return data;
    }
}
