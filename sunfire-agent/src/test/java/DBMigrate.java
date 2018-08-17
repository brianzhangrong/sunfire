import com.alibaba.fastjson.JSON;
import com.ihomefnt.sunfire.agent.config.CuratorRegistry;
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
        DBMigrate.createIfNodeNotExist(
                CuratorRegistry.SUNFIRE_PATH + CuratorRegistry.SPLIT + appName);

        DBMigrate.createIfNodeNotExist(
                CuratorRegistry.SUNFIRE_PATH + CuratorRegistry.SPLIT + appName
                        + CuratorRegistry.SPLIT + CuratorRegistry.HBASE_NAME);
        DBMigrate.client.setData(CuratorRegistry.SUNFIRE_PATH + CuratorRegistry.SPLIT + appName
                        + CuratorRegistry.SPLIT + CuratorRegistry.HBASE_NAME,
                CuratorRegistry.selectHBTableName(CuratorRegistry.SUNFIRE_HBASE_TABLE_NAME_PREFIX,
                                appName.toLowerCase()));
        //SunfireAgent.SPLIT+SunfireAgent.HBASE_NAME
        DBMigrate.client.watchData(CuratorRegistry.SUNFIRE_PATH + CuratorRegistry.SPLIT + appName
                + CuratorRegistry.SPLIT + CuratorRegistry.HBASE_NAME, (event) -> {
                    event.getType();
            if (event.getType() == EventType.NodeDataChanged && (CuratorRegistry.SUNFIRE_PATH
                    + CuratorRegistry.SPLIT + appName + CuratorRegistry.SPLIT
                    + CuratorRegistry.HBASE_NAME).equals(event.getPath())) {

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
