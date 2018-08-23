package com.ihomefnt.sunfire.agent.config;

import static com.ihomefnt.sunfire.agent.utils.StringUtils.join;

import com.google.common.collect.Maps;
import com.ihomefnt.sunfire.agent.config.WatchDataNotify.DataEvent;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.zookeeper.Watcher.Event.EventType;

@Slf4j
public class CuratorRegistry {

    /**
     * 分割符
     */
    public static final String SPLIT = "/";
    /**
     * 根目录
     */
    public static final String SUNFIRE = "sunfire";
    /*
     * opentsdb地址，zk nodename
     * */
    public static final String OPENTSDB = "tsdb";
    /*
     * zk root目录
     * */
    public static final String SUNFIRE_PATH = join(SPLIT, SUNFIRE);

    /**
     * zk tsdb zk路径
     */
    public static final String SUNFIRE_OPENSTSDB = join(SUNFIRE_PATH, SPLIT, OPENTSDB);
    /*
     *
     * zk hbase存放路径
     * */
    public static final String HBASE_NAME = "hb";
    /*
     *
     * zk hbase table name 前缀
     * */
    public static final String SUNFIRE_HBASE_TABLE_NAME_PREFIX = join(SUNFIRE, "_");
    /**
     * 日志内容分割符，在zk中可配置
     */
    public static final String BODYSPLIT = "bodysplit";
    /**
     * hbase  表名
     */
    private static String tbName;
    /**
     * appid
     */
    CuratorZookeeperClient client;
    /**
     * opentsdb地址，zk中，或者是flume.conf的配置文件中配置
     */
    String openTSDBUrl = "localhost:4242";

    /**
     * 日志内容分割符
     */
    String bodySplit = "->>>";

    Map <String, ZkDataConfig> appConfig = Maps.newHashMap();

    DataChangedNotify dataChangedNotify = new DataChangedNotify();

    public CuratorRegistry(String appName, String zookeeperAddr, String openTSDBUrl) {
        if (!appConfig.containsKey(appName)) {
            appConfig.put(appName, new ZkDataConfig());
        }
        ZkDataConfig config = appConfig.get(appName);
        this.openTSDBUrl = openTSDBUrl;
        client = new CuratorZookeeperClient(zookeeperAddr);
        addStateListeners();
        addSunfireRootZKPath(appName);
    }

    public static String selectHBTableName(String sunfireHbaseTableNamePrefix, String s) {
        if (StringUtils.isNotBlank(tbName)) {
            return tbName;
        }
        return join(sunfireHbaseTableNamePrefix, s);
    }

    public void addDataChangedNotify(DataEvent event, WatchDataNotify notify) {
        dataChangedNotify.addWatchDataNotify(event, notify);
    }


    /**
     * zk 应用配置变更，watch监控， hbasename，日志内容分割符，opentsdb地址
     */
    private void addSunfireRootZKPath(String appName) {

        createIfNodeNotExist(SUNFIRE_PATH);
        //appid注册
        createIfNodeNotExist(appId(appName));
        //appid对应hb节点

        //opentsdb地址
        createIfNodeNotExist(appOSTBNode());
        client.setData(appOSTBNode(), openTSDBUrl);
        client.watchData(appOSTBNode(), (event) -> {
            if ((event.getType() == EventType.NodeDataChanged) && (appOSTBNode())
                    .equals(event.getPath())) {
                openTSDBUrl = client.getData(event.getPath());
                dataChangedNotify.notify(DataEvent.OSTB_CHANGED_EVENT, openTSDBUrl);
                log.info("opentsdb changed:{},path:{}", tbName, event.getPath());

            }
        });

        createIfNodeNotExist(appHbNode(appName));
        //appid对应hb内容
        client.setData(appHbNode(appName), initAppHbName(appName));
        client.watchData(appHbNode(appName), (event) -> {
            if ((event.getType() == EventType.NodeDataChanged) && (appHbNode(appName))
                    .equals(event.getPath())) {
                tbName = client.getData(event.getPath());
                dataChangedNotify.notify(DataEvent.HBTABLE_CHANGED_EVENT, initAppHbName(appName));
                log.info("hbase table changed:{},path:{}", tbName, event.getPath());

            }
        });
        //appid对应的日志body分割符
        createIfNodeNotExist(appBodySplitNode(appName));
        //日志内容分割符
        client.setData(appBodySplitNode(appName), bodySplit);
        client.watchData(appBodySplitNode(appName), (event) -> {
            if (event.getType() == EventType.NodeDataChanged && (appBodySplitNode(appName))
                    .equals(event.getPath())) {
                bodySplit = client.getData(event.getPath());
                log.info("bodysplit changed:{},path:{}", tbName, event.getPath());

            }
        });
    }

    public String appOSTBNode() {
        return SUNFIRE_OPENSTSDB;
    }

    private void createIfNodeNotExist(String path) {
        if (!client.checkExists(path)) {
            client.createPersistent(path);
        }
    }

    private void createNodeAndWatch(String path, String data, DataEvent event,
            WatchDataNotify watchDataNotify) {
        createIfNodeNotExist(path);
        client.setData(path, data);
        client.watchData(path, (e) -> {
            if (e.getType() == EventType.NodeDataChanged && path.equals(e.getPath())) {
                String nodeData = client.getData(e.getPath());
                dataChangedNotify.notify(event, nodeData);
                log.info("data changed:{},path:{}", nodeData, e.getPath());

            }
        });
    }

    private void addStateListeners() {
        client.addStateListener((state) -> {
            if (state == StateListener.CONNECTED) {
                log.warn("iray cloud zookeeper connected");
            } else if (state == StateListener.DISCONNECTED) {
                log.warn("iray cloud zookeeper disconnected");
            } else if (state == StateListener.RECONNECTED) {
                log.warn("iray cloud zookeeper reconnected");
            } else {
                log.warn("iray cloud zookeeper unkown state");
            }
        });
    }

    public String initAppHbName(String appName) {
        return selectHBTableName(SUNFIRE_HBASE_TABLE_NAME_PREFIX, appName.toLowerCase());
    }

    private String appId(String appName) {
        return join(SUNFIRE_PATH, SPLIT, appName);
    }

    private String appHbNode(String appName) {
        return join(appId(appName), SPLIT, HBASE_NAME);
    }

    private String appBodySplitNode(String appName) {
        return join(appId(appName), SPLIT, BODYSPLIT);
    }
}
