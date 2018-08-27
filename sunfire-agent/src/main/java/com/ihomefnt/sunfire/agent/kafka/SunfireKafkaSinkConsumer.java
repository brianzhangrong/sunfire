package com.ihomefnt.sunfire.agent.kafka;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.ihomefnt.sunfire.admin.constants.SunfireClientContants;
import com.ihomefnt.sunfire.agent.constant.SunfireConstant;
import com.ihomefnt.sunfire.agent.event.LoggerData;
import com.ihomefnt.sunfire.agent.store.OpenTSDBMetricStore;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.data.hadoop.hbase.HbaseTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
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

    @KafkaListener(topics = {"sunfire_irayproxy"})
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

            LoggerData data = new LoggerData();
            data.setAppName(appName);
            data.setCreateTime(com.ihomefnt.sunfire.agent.utils.StringUtils.now());
            data.setSplitExpress("");
            data.setIp(ip);
            //rowkey作为reginserver的分区管理  日志发生时间+6位随机数
            String rowName = "";
            insertHBaseLogContent(message, appName, data);
            openTSDBMetricStore.put(appName, ip, data);
        }

    }

    private void insertHBaseLogContent(String message, String appName, LoggerData data) {
        String rowName;
        List <String> bodyList = Lists
                .newArrayList(Splitter.on(SunfireConstant.LOG_SPLIT).split(message));
        if (!CollectionUtils.isEmpty(bodyList) && bodyList.size() > 1) {
            //yyyy-MM-dd  hh:mm:ss.SSS 作为rowkey +6位的数字，不足位补0
            rowName = com.ihomefnt.sunfire.agent.utils.StringUtils.dateToRowkey(bodyList.get(1));
            data.setLoggerTime(bodyList.get(1));
            //线程名
            data.setBizName(bodyList.get(2));
            List <String> contentList = Lists.newArrayList(Splitter.on("->>>").split(message));
            if (!CollectionUtils.isEmpty(contentList) && contentList.size() > 1) {
                data.setLoggerContent(contentList.get(1));
            }
            data.setTraceId(bodyList.get(5));
            Field[] fields = data.getClass().getDeclaredFields();

            rowName = com.ihomefnt.sunfire.agent.utils.StringUtils
                    .join(rowName, org.apache.commons.lang.StringUtils
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
                String qualifierName = com.ihomefnt.sunfire.agent.utils.StringUtils
                        .humpToUnderline(fieldName);
                Object value = ReflectionUtils.getField(field, data);
                String colValue = value == null ? "" : String.valueOf(value);
                hbaseTemplate.put(com.ihomefnt.sunfire.agent.utils.StringUtils
                                .getHBaseNameByAppId(appName), rowName, getFamilyName(), qualifierName,
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
