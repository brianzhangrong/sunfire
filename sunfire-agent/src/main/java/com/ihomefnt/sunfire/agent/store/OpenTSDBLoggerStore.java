package com.ihomefnt.sunfire.agent.store;

import com.alibaba.fastjson.JSON;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ihomefnt.sunfire.agent.constant.SunfireConstant;
import com.ihomefnt.sunfire.agent.event.LoggerData;
import com.ihomefnt.sunfire.agent.event.LoggerEvent;
import com.ihomefnt.sunfire.agent.store.OpenTSDBParams.OpenTSDBSummary;
import com.ihomefnt.sunfire.agent.utils.NetUtils;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
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
import org.springframework.util.StringUtils;

@Slf4j
public class OpenTSDBLoggerStore<LoggerDO> extends AbstraceHbaseStore <LoggerDO> {

    private static final String URL = "http://%s/api/put?summary";

    public static void main(String[] args) {
        //
    }

    @Override
    public void preCondition(LoggerDO loggerDO) {
        Preconditions.checkNotNull(loggerDO);
    }

    @Override
    public boolean useOpenTSDB() {
        return true;
    }

    //http://localhost:4242/api/put?summary
    @Override
    public void process(LoggerDO loggerDO) {
        if (loggerDO instanceof LoggerEvent) {
            LoggerEvent event = (LoggerEvent) loggerDO;
            LoggerData data = event.getData();
            String openTSDB = data.get$openTSDB();
            OpenTSDBParams <OpenTSDBSummary> params = new OpenTSDBParams <>();
            OpenTSDBSummary summary = new OpenTSDBSummary();
            String content = data.getLoggerContent();
            Map <String, String> tagMap = Maps.newHashMap();
            if (!StringUtils.isEmpty(content) && content.contains(SunfireConstant.LOG_SPLIT)) {
                List <String> contentList = Lists
                        .newArrayList(Splitter.on(SunfireConstant.LOG_SPLIT).split(content));
                summary.setMetric(data.getAppName());

                try {
                    tagMap.put(contentList.get(0).toLowerCase(), "1");
                    tagMap.put("ip", NetUtils.getLocalHostLANAddress().getHostAddress());
                    summary.setTags(tagMap);
                    summary.setTimestamp(String.valueOf(
                            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
                                    .parse(data.getLoggerTime()).getTime()));
                    summary.setValue("1");
                    params.getList().add(summary);
                    // 发送Json格式的数据请求
                    String tsdbRequestUrl = String.format(URL, openTSDB);
                    OkHttpClient httpClient = new OkHttpClient.Builder()
                            .connectTimeout(3000, TimeUnit.MILLISECONDS)
                            .readTimeout(3000, TimeUnit.MILLISECONDS).retryOnConnectionFailure(true)
                            .writeTimeout(3000, TimeUnit.MILLISECONDS).build();
                    RequestBody requestBody = FormBody
                            .create(MediaType.parse("application/json; charset=utf-8"),
                                    JSON.toJSONString(params.getList()));
                    Request tsdbRequest = new Request.Builder().url(tsdbRequestUrl)
                            .post(requestBody).build();
                    Call call = httpClient.newCall(tsdbRequest);

                    call.enqueue(new Callback() {
                        //请求错误回调方法
                        @Override
                        public void onFailure(Call call, IOException e) {
                            log.error("连接失败");
                        }

                        @Override
                        public void onResponse(Call call, Response response) throws IOException {
                            log.info(response.body().string());
                        }
                    });


                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("ex:{}", ExceptionUtils.getStackTrace(e));
                }

            }

        }
    }
}
