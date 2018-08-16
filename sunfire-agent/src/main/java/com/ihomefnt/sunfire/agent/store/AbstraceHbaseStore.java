package com.ihomefnt.sunfire.agent.store;

import com.ihomefnt.sunfire.agent.event.LoggerData;
import com.ihomefnt.sunfire.agent.event.LoggerEvent;
import com.ihomefnt.sunfire.agent.generator.FamilyNameGenerator;
import com.ihomefnt.sunfire.agent.generator.Generator;
import com.ihomefnt.sunfire.agent.generator.QualifierGenerator;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.hadoop.hbase.HbaseTemplate;
import org.springframework.data.hadoop.hbase.RowMapper;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

public abstract class AbstraceHbaseStore<T> implements Store<T> {
    public final static String HBASE_PROPERTIES= "hbase.xml";
    private static final Logger logger = LoggerFactory.getLogger(AbstraceHbaseStore.class);
    HbaseTemplate hbaseTemplate;

    public AbstraceHbaseStore(){
        Configuration configuration = new Configuration();
        configuration.addResource(AbstraceHbaseStore.HBASE_PROPERTIES);
        hbaseTemplate = new HbaseTemplate(configuration);
    }


    @Override
    public void store(String tableName, String rowName, T t) {
        preCondition(t);
        if (t instanceof LoggerEvent) {
            LoggerData loggerData = ((LoggerEvent) t).getData();
            insertAppIdHb(tableName, rowName, loggerData);
        }
        if (useOpenTSDB()) {
            process(t);
        }

    }

    private void insertAppIdHb(String tableName, String rowName, LoggerData loggerEvent) {
        Field[] fields = loggerEvent.getClass().getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            String fieldName = field.getName();
            if (StringUtils.isEmpty(fieldName) || fieldName.contains("$")) {
                continue;
            }
            String familName = familyNameGenerator(fieldName).generate();
            String qualifierName = qualifierGenerator(fieldName).generate();
            Object value = ReflectionUtils.getField(field, loggerEvent);
            String colValue = value == null ? "" : String.valueOf(value);
            logger.warn("store data familName:{},qualifierName:{},insert colValue:{}",
                    new Object[]{familName, qualifierName, colValue});
            qualifierName = getQualifierNameSequence(tableName, rowName, familName, qualifierName);
            hbaseTemplate.put(tableName, rowName, familName, qualifierName, colValue.getBytes());
        }
    }

    private String getQualifierNameSequence(String tableName, String rowName, String familName,
            String qualifierName) {
        Map <String, Object> result = hbaseTemplate
                .get(tableName, rowName, familName, new RowMapper <Map <String, Object>>() {
                    @Override
                    public Map <String, Object> mapRow(Result result, int rowNum) throws Exception {
                        List <Cell> ceList = result.listCells();
                        Map <String, Object> map = new HashMap <>();
                        if (ceList != null && ceList.size() > 0) {
                            for (Cell cell : ceList) {
                                map.put(Bytes.toString(cell.getQualifierArray(),
                                        cell.getQualifierOffset(), cell.getQualifierLength()),
                                        Bytes.toString(cell.getValueArray(), cell.getValueOffset(),
                                                cell.getValueLength()));
                            }
                        }
                        return map;
                    }
                });
        Integer sequence = 0;
        if (!CollectionUtils.isEmpty(result)) {
            //faimly:qualify 不为空，traceId的值需要串联 "create_time_create_time" -> "2018-02-10
            // 17:41:18"
            sequence = result.keySet().stream().map(qulifyName -> Integer
                    .valueOf(qulifyName.substring(qulifyName.lastIndexOf("_") + 1)))
                    .max(Integer::compareTo).get();

        }
        sequence += 1;
        qualifierName = qualifierName + "_" + sequence;
        return qualifierName;
    }

    Generator familyNameGenerator(String field) {
        return new FamilyNameGenerator(field);
    }

    Generator qualifierGenerator(String field) {
        return new QualifierGenerator(field);
    }


    public abstract void preCondition(T t);

    public abstract boolean useOpenTSDB();

    public abstract void process(T t);

}
