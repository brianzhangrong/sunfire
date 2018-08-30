package com.ihomefnt.sunfire.agent.store;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Maps;
import com.ihomefnt.sunfire.agent.bootConfig.SunfireProperties;
import com.ihomefnt.sunfire.agent.event.LoggerData;
import com.ihomefnt.sunfire.agent.store.OpenTSDBParams.OpenTSDBSummary;
import com.ihomefnt.sunfire.config.constant.SunfireConstant;
import com.ihomefnt.sunfire.hbase.model.Regular;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class OpenTSDBMetricStore {

    @Resource
    SunfireProperties sunfireProperties;

    public void put(String apppName, String ip, LoggerData data, List <Regular> regularList) {
            OpenTSDBParams <OpenTSDBSummary> params = new OpenTSDBParams <>();
            OpenTSDBSummary summary = new OpenTSDBSummary();
            Map <String, String> tagMap = Maps.newHashMap();
        String message = data.getLoggerContent();
        if (!StringUtils.isEmpty(message)) {

            String valueJoin = regularList.stream().map(regular -> regular.getValue())
                    .collect(Collectors.joining("."));
            // metrics
            summary.setMetric(com.ihomefnt.sunfire.config.utils.StringUtils
                    .join(apppName.toLowerCase(), ".", valueJoin));

            try {
                // tag
                tagMap.put(SunfireConstant.APP_IP, ip);
                summary.setTags(tagMap);
                summary.setTimestamp(String.valueOf(
                        new SimpleDateFormat(SunfireConstant.DATE_FORMAT)
                                .parse(data.getLoggerTime()).getTime()));
                summary.setValue("1");
                params.getList().add(summary);
                // 发送Json格式的数据请求  ,http://localhost:4242/api/put?summary
                postDataToTSDB(params);
            } catch (Exception e) {
                e.printStackTrace();
                log.error("ex:{}", ExceptionUtils.getStackTrace(e));
            }
            }

    }


    private void postDataToTSDB(OpenTSDBParams <OpenTSDBSummary> params) throws IOException {

        String tsdbRequestUrl = String
                .format(SunfireConstant.OPENTSDB_REQUEST_URL, sunfireProperties.getOpenTSDB());
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .connectTimeout(3000, TimeUnit.MILLISECONDS)
                .readTimeout(3000, TimeUnit.MILLISECONDS).retryOnConnectionFailure(true)
                .writeTimeout(3000, TimeUnit.MILLISECONDS).build();
        RequestBody requestBody = FormBody
                .create(MediaType.parse("application/json; charset=utf-8"),
                        JSON.toJSONString(params.getList()));
        Request tsdbRequest = new Request.Builder().url(tsdbRequestUrl).post(requestBody).build();
        Call call = httpClient.newCall(tsdbRequest);

        call.enqueue(new Callback() {
            //请求错误回调方法
            @Override
            public void onFailure(Call call, IOException e) {
                log.error("insert tsdb error:{}", ExceptionUtils.getStackTrace(e));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                log.info(response.body().string());
            }
        });
    }
}
