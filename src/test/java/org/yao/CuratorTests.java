package org.yao;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.CuratorEvent;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

import static com.google.common.truth.Truth.assertThat;

/**
 * Example code to demonstrate the usage of Curator client and framework.
 */
public class CuratorTests {
  private CuratorFramework client;
  private String connectString = "localhost:2181";
  private RetryPolicy retryPolicy;

  @Before
  public void setUp() {
    retryPolicy = new ExponentialBackoffRetry(1000, 3);
    client = CuratorFrameworkFactory.newClient(connectString, retryPolicy);

    /*
    // Fluent style
    client =
        CuratorFrameworkFactory.builder()
            .connectString(connectString)
            .retryPolicy(retryPolicy)
            .build();
    */

    // Start client
    client.start();
  }

  @After
  public void tearDown() {
    client.close();
  }

  // create -> getData -> delete in synchronous mode
  @Test
  public void testSyncOp() throws Exception {
    String path = "/one";
    byte[] data = {'1'};
    client.create().withMode(CreateMode.PERSISTENT).forPath(path, data);

    byte[] actualData = client.getData().forPath(path);
    assertThat(data).isEqualTo(actualData);

    client.delete().forPath(path);

    client.close();
  }


  // create -> getData -> delete in asynchronous mode
  @Test
  public void testAsyncOp() throws Exception {
    String path = "/two";
    final byte[] data = {'2'};
    final CountDownLatch latch = new CountDownLatch(1);

    // Use listener only for callbacks
    client
        .getCuratorListenable()
        .addListener(
            (CuratorFramework c, CuratorEvent event) -> {
              switch (event.getType()) {
                case CREATE:
                  System.out.printf("znode '%s' created\n", event.getPath());
                  // 2. getData
                  c.getData().inBackground().forPath(event.getPath());
                  break;
                case GET_DATA:
                  System.out.printf("got the data of znode '%s'\n", event.getPath());
                  assertThat(event.getData()).isEqualTo(data);
                  // 3. Delete
                  c.delete().inBackground().forPath(path);
                  break;
                case DELETE:
                  System.out.printf("znode '%s' deleted\n", event.getPath());
                  latch.countDown();
                  break;
              }
            });

    // 1. create
    client.create().withMode(CreateMode.PERSISTENT).inBackground().forPath(path, data);

    latch.await();

    client.close();
  }

  @Test
  public void testWatch() throws Exception {
    String path = "/three";
    byte[] data = {'3'};
    byte[] newData = {'4'};
    CountDownLatch latch = new CountDownLatch(1);

    // Use listener only for watches
    client
        .getCuratorListenable()
        .addListener(
            (CuratorFramework c, CuratorEvent event) -> {
              switch (event.getType()) {
                case WATCHED:
                  WatchedEvent we = event.getWatchedEvent();
                  System.out.println("watched event: " + we);
                  if (we.getType() == Watcher.Event.EventType.NodeDataChanged
                      && we.getPath().equals(path)) {
                    // 4. watch triggered
                    System.out.printf("got the event for the triggered watch\n");
                    byte[] actualData = c.getData().forPath(path);
                    assertThat(actualData).isEqualTo(newData);
                  }
                  latch.countDown();
                  break;
              }
            });

    // 1. create
    client.create().withMode(CreateMode.PERSISTENT).forPath(path, data);
    // 2. getData and register a watch
    byte[] actualData = client.getData().watched().forPath(path);
    assertThat(actualData).isEqualTo(data);

    // 3. setData
    client.setData().forPath(path, newData);
    latch.await();

    // 5. delete
    client.delete().forPath(path);
  }

  @Test
  public void testCallbackAndWatch() throws Exception {
    String path = "/four";
    byte[] data = {'4'};
    byte[] newData = {'5'};
    CountDownLatch latch = new CountDownLatch(2);

    // Use listener for both callbacks and watches
    client
        .getCuratorListenable()
        .addListener(
            (CuratorFramework c, CuratorEvent event) -> {
              switch (event.getType()) {
                case CREATE:
                  // 2. callback for create
                  System.out.printf("znode '%s' created\n", event.getPath());
                  // 3. getData and register a watch
                  assertThat(client.getData().watched().forPath(path)).isEqualTo(data);
                  // 4. setData
                  client.setData().forPath(path, newData);
                  latch.countDown();
                  break;
                case WATCHED:
                  WatchedEvent we = event.getWatchedEvent();
                  System.out.println("watched event: " + we);
                  if (we.getType() == Watcher.Event.EventType.NodeDataChanged
                      && we.getPath().equals(path)) {
                    // 5. watch triggered
                    System.out.printf("got the event for the triggered watch\n");
                    assertThat(c.getData().forPath(path)).isEqualTo(newData);
                  }
                  latch.countDown();
                  break;
              }
            });

    // 1. create
    client.create().withMode(CreateMode.PERSISTENT).inBackground().forPath(path, data);

    latch.await();

    // 6. delete
    client.delete().forPath(path);
  }
}
