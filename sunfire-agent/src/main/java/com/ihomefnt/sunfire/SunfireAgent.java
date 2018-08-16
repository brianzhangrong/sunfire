package com.ihomefnt.sunfire;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.ihomefnt.sunfire.agent.config.CuratorZookeeperClient;
import com.ihomefnt.sunfire.agent.config.StateListener;
import com.ihomefnt.sunfire.agent.constant.SunfireConstant;
import com.ihomefnt.sunfire.agent.event.LoggerData;
import com.ihomefnt.sunfire.agent.ringbuffer.LoggerRingBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.flume.Channel;
import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.EventDeliveryException;
import org.apache.flume.Transaction;
import org.apache.flume.conf.Configurable;
import org.apache.flume.sink.AbstractSink;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

public class SunfireAgent extends AbstractSink  implements Configurable {

    /*
    * conf 配置agent监控那个appid
    * */
    public static final String APP_NAME = "appname";
    /*
    * conf配置agent的zookeeper地址
    * */
    public static final String ZOOKEEPR_ADDR = "zookeeper";
    /**
     * 分割符
     */
    public static final String SPLIT="/";
    /**
     * 根目录
     */
    public static final String SUNFIRE="sunfire";
    /*
    * zk root目录
    * */
    public static final String SUNFIRE_PATH = join(SunfireAgent.SPLIT, SunfireAgent.SUNFIRE);

    /*
     * opentsdb地址，zk nodename
     * */
    public static final String OPENTSDB = "tsdb";
    /**
     * zk tsdb zk路径
     */
    public static final String SUNFIRE_OPENSTSDB = join(SUNFIRE_PATH, SunfireAgent.SPLIT, OPENTSDB);
    /*
    *
    * zk hbase存放路径
    * */
    public static final String HBASE_NAME = "hb";
    /*
    *
    * zk hbase table name 前缀
    * */
    public static final String SUNFIRE_HBASE_TABLE_NAME_PREFIX = join(SunfireAgent.SUNFIRE, "_");
    /**
     * 日志内容分割符，在zk中可配置
     */
    public static final String BODYSPLIT = "bodysplit";
    private static final Logger logger = LoggerFactory.getLogger(SunfireAgent.class);
    /**
     * habse rowkey 并发防重
     */
    private static final AtomicLong sequence = new AtomicLong(0);
    /**
     * hbase  表名
     */
    private static String tbName;
    /**
     * appid
     */
    private static String appName;
    /**
     * zkclient
     */
    CuratorZookeeperClient client;
    /**
     * ringbuffer
     */
    LoggerRingBuffer ringBuffer;
    /**
     * opentsdb地址，zk中，或者是flume.conf的配置文件中配置
     */
    String openTSDBUrl = "localhost:4242";

    /**
     * 日志内容分割符
     */
    String bodySplit = "->>>";

    public SunfireAgent() {

    }

    public static String selectHBTableName(String sunfireHbaseTableNamePrefix, String s) {
        if (StringUtils.isNotBlank(SunfireAgent.tbName)) {
            return SunfireAgent.tbName;
        }
        return SunfireAgent.join(sunfireHbaseTableNamePrefix, s);
    }

    private static String join(String... a) {
        StringBuilder builder = new StringBuilder();
        for (String tmp : a) {
            builder.append(tmp);
        }
        return builder.toString();
    }

    /**
     * @see org.apache.flume.conf.Configurable#configure(org.apache.flume.Context)
     */
    @Override
    public void configure(Context context) {
        appName = context.getString(SunfireAgent.APP_NAME);
        openTSDBUrl = context.getString(OPENTSDB);
        ringBuffer = new LoggerRingBuffer(initAppHbName());
        String zookeeperAddr = context.getString(SunfireAgent.ZOOKEEPR_ADDR);
        client =new CuratorZookeeperClient(zookeeperAddr);
        addStateListeners();
        addSunfireRootZKPath();
    }

    /**
     * zk 应用配置变更，watch监控， hbasename，日志内容分割符，opentsdb地址
     */
    private void addSunfireRootZKPath() {
        Preconditions.checkNotNull(appName);
        createIfNodeNotExist(SunfireAgent.SUNFIRE_PATH);
        //appid注册
        createIfNodeNotExist(appId());
        //appid对应hb节点
        createIfNodeNotExist(appHbNode());
        //appid对应的日志body分割符
        createIfNodeNotExist(appBodySplit());
        //日志内容分割符
        client.setData(appBodySplit(), bodySplit);
        client.watchData(appBodySplit(), (event) -> {
            if (event.getType() == EventType.NodeDataChanged && (appBodySplit())
                    .equals(event.getPath())) {
                bodySplit = client.getData(event.getPath());
                SunfireAgent.logger
                        .info("opentsdb changed:{},path:{}", SunfireAgent.tbName, event.getPath());

            }
        });
        //opentsdb地址
        createIfNodeNotExist(SUNFIRE_OPENSTSDB);
        client.setData(SUNFIRE_OPENSTSDB, openTSDBUrl);
        client.watchData(SUNFIRE_OPENSTSDB, (event) -> {
            if (event.getType() == EventType.NodeDataChanged && (SUNFIRE_OPENSTSDB).equals(event.getPath())) {
                openTSDBUrl = client.getData(event.getPath());
                SunfireAgent.logger.info("opentsdb changed:{},path:{}", SunfireAgent.tbName, event.getPath());

            }
        });
        //appid对应hb内容
        client.setData(appHbNode(), initAppHbName());
        client.watchData(appHbNode(), (event) -> {
            if (event.getType() == EventType.NodeDataChanged && (appHbNode()).equals(event.getPath())) {
                SunfireAgent.tbName = client.getData(event.getPath());
                ringBuffer.setDbName(initAppHbName());
                SunfireAgent.logger.info("hbase table changed:{},path:{}", SunfireAgent.tbName,
                        event.getPath());

            }
        });
    }

    private String initAppHbName() {
        return SunfireAgent.selectHBTableName(SunfireAgent.SUNFIRE_HBASE_TABLE_NAME_PREFIX,
                appName.toLowerCase());
    }

    private String appId() {
        return SunfireAgent.SUNFIRE_PATH + SunfireAgent.SPLIT + appName;
    }

    private String appHbNode() {
        return appId() + SunfireAgent.SPLIT + SunfireAgent.HBASE_NAME;
    }

    private String appBodySplit() {
        return appId() + SunfireAgent.SPLIT + BODYSPLIT;
    }

    private void createIfNodeNotExist(String path) {
        if( !client.checkExists(path)){
            client.createPersistent(path);
        }
    }

    private void addStateListeners() {
        client.addStateListener((state)->{
            if(state== StateListener.CONNECTED){
                SunfireAgent.logger.warn("iray cloud zookeeper connected");
            }else if(state==StateListener.DISCONNECTED){
                SunfireAgent.logger.warn("iray cloud zookeeper disconnected");
            }else if(state ==StateListener.RECONNECTED){
                SunfireAgent.logger.warn("iray cloud zookeeper reconnected");
            }else{
                SunfireAgent.logger.warn("iray cloud zookeeper unkown state");
            }
        });
    }

    /**
     * @see org.apache.flume.Sink#process()
     *|2018-08-13 17:28:42.548|http-nio-9999-exec-1|INFO |c.i.i.controller
     * .sunfireController|58f8ea8578ad486fa18a6a5102c3b8c8|-hear end,null
     */
    @Override
    public Status process() throws EventDeliveryException {
        Channel ch = getChannel();
        //get the transaction
        Transaction txn = ch.getTransaction();
        Event event =null;
        //begin the transaction
        txn.begin();
        while(true){
            event = ch.take();
            if (event!=null) {
                break;
            }
        }
        try {
            String body = new String(event.getBody());
            if (StringUtils.isNotBlank(body)) {
                LoggerData data = new LoggerData();
                data.setAppName(appName);
                data.setCreateTime(now());
                data.setSplitExpress("");
                //traceId==rowkey作为reginserver的分区管理
                List <String> bodyList = Lists
                        .newArrayList(Splitter.on(SunfireConstant.LOG_SPLIT).split(body));
                if (!CollectionUtils.isEmpty(bodyList) && bodyList.size() > 1) {
                    //traceId
                    data.set$rowKey(
                            bodyList.get(1).replace("-", "").replace(":", "").replace(".", "")
                                    .replace(" ", "") + sequence.getAndIncrement());
                    data.setLoggerTime(bodyList.get(1));
                    data.setBizName(bodyList.get(2));
                    List <String> contentList = Lists
                            .newArrayList(Splitter.on(bodySplit).split(body));
                    if (!CollectionUtils.isEmpty(contentList) && contentList.size() > 1) {
                        data.setLoggerContent(contentList.get(1));
                    }
                    data.setTraceId(bodyList.get(5));
                    if (sequence.compareAndSet(SunfireConstant.SEQUENCE, 0)) {
                        logger.info("sequence set zeror");
                    }
                }
                data.set$openTSDB(openTSDBUrl);
                data.set$dbName(initAppHbName());
                ringBuffer.publish(data);
            }
            txn.commit();
            return Status.READY;
        } catch (Throwable th) {
            SunfireAgent.logger.error("event process error:{}", ExceptionUtils.getStackTrace(th));
            txn.rollback();
            if (th instanceof Error) {
                throw (Error) th;
            } else {
                throw new EventDeliveryException(th);
            }
        } finally {
            txn.close();
        }
    }

    private String now() {
        return String.valueOf(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
    }


}
