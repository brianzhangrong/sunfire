package com.ihomefnt.sunfire.admin.service;

import static com.ihomefnt.sunfire.config.constant.SunfireConstant.APPCONFIG_COLUME_FAMLIY;
import static com.ihomefnt.sunfire.config.constant.SunfireConstant.CONFIG_ATTR_VALUE;
import static com.ihomefnt.sunfire.config.constant.SunfireConstant.CONFIG_FAMLIY_BEING_PREFIX;
import static com.ihomefnt.sunfire.config.constant.SunfireConstant.CONFIG_FAMLIY_END_PREFIX;
import static com.ihomefnt.sunfire.config.constant.SunfireConstant.CONFIG_FAMLIY_SPLIT;
import static com.ihomefnt.sunfire.config.constant.SunfireConstant.CONFIG_TABLE;
import static com.ihomefnt.sunfire.config.utils.StringUtils.getHBaseNameByAppId;
import static org.apache.hadoop.yarn.util.StringHelper.join;

import com.google.common.base.Preconditions;
import com.ihomefnt.sunfire.admin.model.UpdateRegularParams;
import javax.annotation.Resource;
import org.springframework.data.hadoop.hbase.HbaseTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
public class AppRegularService {

    @Resource
    HbaseTemplate hbaseTemplate;

    public void appRegularUpdate(UpdateRegularParams params) {
        Preconditions.checkNotNull(params);
        Preconditions.checkState(!CollectionUtils.isEmpty(params.getRegularList()));
        params.getRegularList().forEach(regular -> {
            hbaseTemplate
                    .put(getHBaseNameByAppId(CONFIG_TABLE), join(params.getAppName().hashCode()),
                            APPCONFIG_COLUME_FAMLIY,
                            join(CONFIG_FAMLIY_BEING_PREFIX, CONFIG_FAMLIY_SPLIT, regular.getId(),
                                    CONFIG_FAMLIY_SPLIT, regular.getBeginPosition()),
                            regular.getBeginSplitSymbol().getBytes());

            hbaseTemplate
                    .put(getHBaseNameByAppId(CONFIG_TABLE), join(params.getAppName().hashCode()),
                            APPCONFIG_COLUME_FAMLIY,
                            join(CONFIG_FAMLIY_END_PREFIX, CONFIG_FAMLIY_SPLIT, regular.getId(),
                                    CONFIG_FAMLIY_SPLIT, regular.getEndPosition()),
                            regular.getEndSplitSymbol().getBytes());
            hbaseTemplate
                    .put(getHBaseNameByAppId(CONFIG_TABLE), join(params.getAppName().hashCode()),
                            APPCONFIG_COLUME_FAMLIY,
                            join(CONFIG_ATTR_VALUE, CONFIG_FAMLIY_SPLIT, regular.getId()),
                            regular.getValue().getBytes());
        });

    }

}
