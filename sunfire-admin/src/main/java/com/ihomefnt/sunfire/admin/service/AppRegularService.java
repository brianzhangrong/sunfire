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
import com.ihomefnt.sunfire.admin.model.BaseParams;
import com.ihomefnt.sunfire.admin.model.UpdateRegularParams;
import com.ihomefnt.sunfire.hbase.model.Regular;
import com.ihomefnt.sunfire.hbase.service.AppRegularModifyService;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.hadoop.hbase.HbaseTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
@Slf4j
public class AppRegularService {

    @Resource
    HbaseTemplate hbaseTemplate;
    @Resource
    AppRegularModifyService appRegularModifyService;

    public List <Regular> selectRegular(BaseParams params) {
        return appRegularModifyService.selectAppRegular(params.getAppName());

    }

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

    public void deleteRegular(UpdateRegularParams params) {
        params.getRegularList().stream().forEach(regular -> {
//delete all
//            hbaseTemplate
//                    .delete(getHBaseNameByAppId(CONFIG_TABLE), join(params.getAppName().hashCode()),
//                            APPCONFIG_COLUME_FAMLIY);
            hbaseTemplate
                    .delete(getHBaseNameByAppId(CONFIG_TABLE), join(params.getAppName().hashCode()),
                            APPCONFIG_COLUME_FAMLIY,
                            join(CONFIG_FAMLIY_BEING_PREFIX, CONFIG_FAMLIY_SPLIT, regular.getId(),
                                    CONFIG_FAMLIY_SPLIT, regular.getBeginPosition()));
            log.info("delete begin:{}",
                    join(CONFIG_FAMLIY_BEING_PREFIX, CONFIG_FAMLIY_SPLIT, regular.getId(),
                            CONFIG_FAMLIY_SPLIT, regular.getBeginPosition()));
            hbaseTemplate
                    .delete(getHBaseNameByAppId(CONFIG_TABLE), join(params.getAppName().hashCode()),
                            APPCONFIG_COLUME_FAMLIY,
                            join(CONFIG_FAMLIY_END_PREFIX, CONFIG_FAMLIY_SPLIT, regular.getId(),
                                    CONFIG_FAMLIY_SPLIT, regular.getEndPosition()));
            log.info("delete end:{}",
                    join(CONFIG_FAMLIY_END_PREFIX, CONFIG_FAMLIY_SPLIT, regular.getId(),
                            CONFIG_FAMLIY_SPLIT, regular.getEndPosition()));
            hbaseTemplate
                    .delete(getHBaseNameByAppId(CONFIG_TABLE), join(params.getAppName().hashCode()),
                            APPCONFIG_COLUME_FAMLIY,
                            join(CONFIG_ATTR_VALUE, CONFIG_FAMLIY_SPLIT, regular.getId()));
            log.info("delete value:{}", join(params.getAppName().hashCode()),
                    APPCONFIG_COLUME_FAMLIY,
                    join(CONFIG_ATTR_VALUE, CONFIG_FAMLIY_SPLIT, regular.getId()));
        });


    }

    private void sleep() {
        try {
            TimeUnit.MILLISECONDS.sleep(100L);
        } catch (InterruptedException e) {
        }
    }

}
