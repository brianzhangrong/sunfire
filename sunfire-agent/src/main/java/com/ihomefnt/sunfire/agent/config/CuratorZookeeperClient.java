package com.ihomefnt.sunfire.agent.config;

import com.google.common.base.Charsets;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.CuratorWatcher;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.curator.retry.RetryNTimes;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.apache.zookeeper.KeeperException.NodeExistsException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;

public class CuratorZookeeperClient extends AbstractZookeeperClient<CuratorWatcher> {

  private final CuratorFramework client;
  String connectStr = "zk-01.ihomefnt.com:2181,zk-02.ihomefnt.com:2182,zk-03.ihomefnt.com:2183";
  String authority = "";

  public CuratorZookeeperClient(String connectStr) {
    this.connectStr = connectStr;
    try {
      CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory.builder()
              .connectString(connectStr).retryPolicy(new RetryNTimes(1, 1000))
              .connectionTimeoutMs(5000);
      if (authority != null && authority.length() > 0) {
        builder = builder.authorization("digest", authority.getBytes());
      }
      client = builder.build();
      client.getConnectionStateListenable().addListener(new ConnectionStateListener() {
        @Override
        public void stateChanged(CuratorFramework client, ConnectionState state) {
          if (state == ConnectionState.LOST) {
            CuratorZookeeperClient.this.stateChanged(StateListener.DISCONNECTED);
          } else if (state == ConnectionState.CONNECTED) {
            CuratorZookeeperClient.this.stateChanged(StateListener.CONNECTED);
          } else if (state == ConnectionState.RECONNECTED) {
            CuratorZookeeperClient.this.stateChanged(StateListener.RECONNECTED);
          }
        }
      });
      client.start();
    } catch (Exception e) {
      throw new IllegalStateException(e.getMessage(), e);
    }
  }

  public CuratorFramework getClient() {
    return client;
  }

  @Override
  public void createPersistent(String path) {
    try {
      client.create().forPath(path);
    } catch (NodeExistsException e) {
    } catch (Exception e) {
      throw new IllegalStateException(e.getMessage(), e);
    }
  }

  @Override
  public void createEphemeral(String path) {
    try {
      client.create().withMode(CreateMode.EPHEMERAL).forPath(path);
    } catch (NodeExistsException e) {
    } catch (Exception e) {
      throw new IllegalStateException(e.getMessage(), e);
    }
  }

  @Override
  public void setData(String path, String data) {
    try {
      client.setData().forPath(path, data.getBytes());
    } catch (Exception e) {
      throw new IllegalStateException(e.getMessage(), e);
    }
  }

  public void watchData(String path, Watcher watcher) {
    try {
      client.getData().usingWatcher(watcher).forPath(path);
    } catch (Exception e) {
      throw new IllegalStateException(e.getMessage(), e);
    }
  }

  public String getData(String path) {
    try {
      return StringUtils.toEncodedString(client.getData().forPath(path), Charsets.UTF_8);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }


  @Override
  public void delete(String path) {
    try {
      client.delete().forPath(path);
    } catch (NoNodeException e) {
    } catch (Exception e) {
      throw new IllegalStateException(e.getMessage(), e);
    }
  }

  @Override
  public List<String> getChildren(String path) {
    try {
      return client.getChildren().forPath(path);
    } catch (Exception e) {
      throw new IllegalStateException(e.getMessage(), e);
    }
  }

  @Override
  public boolean checkExists(String path) {
    try {
      if (client.checkExists().forPath(path) != null) {
        return true;
      }
    } catch (Exception e) {
    }
    return false;
  }

  @Override
  public boolean isConnected() {
    return client.getZookeeperClient().isConnected();
  }

  @Override
  public void doClose() {
    client.close();
  }

  @Override
  public CuratorWatcher createTargetChildListener(String path, ChildListener listener) {
    return new CuratorWatcherImpl(listener);
  }

  @Override
  public List<String> addTargetChildListener(String path, CuratorWatcher listener) {
    try {
      return client.getChildren().usingWatcher(listener).forPath(path);
    } catch (NoNodeException e) {
      return null;
    } catch (Exception e) {
      throw new IllegalStateException(e.getMessage(), e);
    }
  }

  @Override
  public void removeTargetChildListener(String path, CuratorWatcher listener) {
    ((CuratorWatcherImpl) listener).unwatch();
  }

  private class CuratorWatcherImpl implements CuratorWatcher {

    private volatile ChildListener listener;

    public CuratorWatcherImpl(ChildListener listener) {
      this.listener = listener;
    }

    public void unwatch() {
      listener = null;
    }

    @Override
    public void process(WatchedEvent event) throws Exception {
      if (listener != null) {
        String path = event.getPath() == null ? "" : event.getPath();
        listener.childChanged(path,
                // if path is null, curator using watcher will throw NullPointerException.
                // if client connect or disconnect to server, zookeeper will queue
                // watched event(Watcher.Event.EventType.None, .., path = null).
                StringUtils.isNotEmpty(path) ? client.getChildren().usingWatcher(this).forPath(path)
                        : Collections.<String>emptyList());
      }
    }
  }
}
