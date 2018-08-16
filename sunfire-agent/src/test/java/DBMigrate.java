import com.alibaba.fastjson.JSON;
import com.ihomefnt.sunfire.SunfireAgent;
import com.ihomefnt.sunfire.agent.config.CuratorZookeeperClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.Watcher.Event.EventType;

@Slf4j
public class DBMigrate {

    public static CuratorZookeeperClient client;

    public static void main(String[] args) throws InterruptedException {
        //
        String appName = "irayTest1";
        DBMigrate.client = new CuratorZookeeperClient("127.0.0.1:2181");
        DBMigrate.createIfNodeNotExist(SunfireAgent.SUNFIRE_PATH + SunfireAgent.SPLIT + appName);

        DBMigrate.createIfNodeNotExist(
                SunfireAgent.SUNFIRE_PATH + SunfireAgent.SPLIT + appName + SunfireAgent.SPLIT
                        + SunfireAgent.HBASE_NAME);
        DBMigrate.client.setData(
                SunfireAgent.SUNFIRE_PATH + SunfireAgent.SPLIT + appName + SunfireAgent.SPLIT
                        + SunfireAgent.HBASE_NAME, SunfireAgent
                        .selectHBTableName(SunfireAgent.SUNFIRE_HBASE_TABLE_NAME_PREFIX,
                                appName.toLowerCase()));
        //SunfireAgent.SPLIT+SunfireAgent.HBASE_NAME
        DBMigrate.client.watchData(
                SunfireAgent.SUNFIRE_PATH + SunfireAgent.SPLIT + appName + SunfireAgent.SPLIT
                        + SunfireAgent.HBASE_NAME, (event) -> {
                    event.getType();
                    if (event.getType() == EventType.NodeDataChanged && (SunfireAgent.SUNFIRE_PATH
                            + SunfireAgent.SPLIT + appName + SunfireAgent.SPLIT
                            + SunfireAgent.HBASE_NAME).equals(event.getPath())) {

                    }
                    System.out.println("-----" + JSON.toJSONString(event));
                });
        synchronized (DBMigrate.class) {
            DBMigrate.class.wait();
        }
    }

    public static void createIfNodeNotExist(String path) {
        if (!DBMigrate.client.checkExists(path)) {
            DBMigrate.client.createPersistent(path);
        }
    }
}
