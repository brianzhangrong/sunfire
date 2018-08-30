package com.ihomefnt.sunfire.hbase.service;


import static com.ihomefnt.sunfire.config.constant.SunfireConstant.CONFIG_ATTR_VALUE;
import static com.ihomefnt.sunfire.config.constant.SunfireConstant.CONFIG_FAMLIY_BEING_PREFIX;
import static com.ihomefnt.sunfire.config.constant.SunfireConstant.CONFIG_FAMLIY_END_PREFIX;
import static com.ihomefnt.sunfire.config.constant.SunfireConstant.CONFIG_FAMLIY_SPLIT;
import static com.ihomefnt.sunfire.config.constant.SunfireConstant.CONFIG_TABLE;
import static com.ihomefnt.sunfire.config.utils.StringUtils.getHBaseNameByAppId;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.ihomefnt.sunfire.hbase.model.Regular;
import java.util.List;
import java.util.Optional;
import javax.annotation.Resource;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;
import org.springframework.data.hadoop.hbase.HbaseTemplate;
import org.springframework.data.hadoop.hbase.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AppRegularModifyService {

    @Resource
    HbaseTemplate hbaseTemplate;


    public List <Regular> selectAppRegular(String appName) {
        Preconditions.checkNotNull(appName);
        List <Regular> regularList = hbaseTemplate
                .get(getHBaseNameByAppId(CONFIG_TABLE), "" + appName.hashCode(),
                        new RowMapper <List <Regular>>() {
                            @Override
                            public List <Regular> mapRow(Result result, int i) throws Exception {
                                List <Regular> regularList = Lists.newArrayList();
                                //begin_id_firstPosition
                                result.listCells().forEach(cell -> {
                                    //获取app配置的qulifyname，和分割符
                                    String qulifyName = Bytes.toString(cell.getQualifierArray(),
                                            cell.getQualifierOffset(), cell.getQualifierLength());
                                    String splitValue = Bytes
                                            .toString(cell.getValueArray(), cell.getValueOffset(),
                                                    cell.getValueLength());
                                    if (StringUtils.isEmpty(qulifyName) || !qulifyName
                                            .contains(CONFIG_FAMLIY_SPLIT)) {
                                        return;
                                    }
                                    //0-begin,end,1-id,2-position------------attr_value
                                    List <String> qulifyList = Lists.newArrayList(
                                            Splitter.on(CONFIG_FAMLIY_SPLIT).split(qulifyName));
                                    Integer id = org.apache.commons.lang3.StringUtils
                                            .isNumeric(qulifyList.get(1).trim()) ? Integer
                                            .valueOf(qulifyList.get(1).trim()) : null;
                                    Optional <Regular> regularOptional = regularList.stream()
                                            .filter(reg -> id != null && id
                                                    .equals(reg.getId().intValue())).findAny();
                                    Regular regular;
                                    if (!regularOptional.isPresent()) {
                                        regular = new Regular();
                                        regular.setId(id);
                                        regularList.add(regular);
                                    } else {
                                        regular = regularOptional.get();
                                    }

                                    if (CONFIG_FAMLIY_BEING_PREFIX
                                            .equals(qulifyList.get(0).trim())) {
                                        regular.setBeginSplitSymbol(splitValue);
                                        regular.setBeginPosition(
                                                Integer.valueOf(qulifyList.get(2).trim()));
                                    } else if (CONFIG_FAMLIY_END_PREFIX
                                            .equals(qulifyList.get(0).trim())) {
                                        regular.setEndSplitSymbol(splitValue);
                                        regular.setEndPosition(
                                                Integer.valueOf(qulifyList.get(2).trim()));
                                    } else if (qulifyName.contains(CONFIG_ATTR_VALUE)) {
                                        regular.setValue(splitValue);
                                    }
                                });
                                return regularList;
                            }
                        });
        return regularList;
    }
}
