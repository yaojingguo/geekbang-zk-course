package org.yao;

import com.google.common.collect.ImmutableList;
import org.apache.zookeeper.AsyncCallback;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.Op;
import org.apache.zookeeper.OpResult;
import org.apache.zookeeper.Transaction;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;

import static com.google.common.truth.Truth.assertThat;

public class ZooKeeperTests {
  private String pathPrefix = "/multi";
  private ZooKeeper zk;
  private CountDownLatch startLatch;
  private CountDownLatch closeLatch;
  private AsyncCallback.MultiCallback callback;

  private String path1 = pathPrefix + "1";
  private String path2 = pathPrefix + "2";
  private byte[] data1 = {0x1};
  private byte[] data2 = {0x2};

  @Before
  public void setUp() throws Exception {
    startLatch = new CountDownLatch(1);
    callback =
        (int rc, String path, Object ctx, List<OpResult> opResults) -> {
          assertThat(rc).isEqualTo(KeeperException.Code.OK.intValue());
          System.out.printf("delete multi executed");
          closeLatch.countDown();
        };
    zk = new ZooKeeper("localhost", 2181, new DefaultWatcher());
    startLatch.await();
  }

  @After
  public void tearDown() throws Exception {
    closeLatch.await();
    zk.close();
  }

  @Test
  public void testMulti() throws Exception {
    closeLatch = new CountDownLatch(1);

    // Create two znodes
    Op createOp1 = Op.create(path1, data1, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
    Op createOp2 = Op.create(path2, data2, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);

    // Synchronous API
    zk.multi(ImmutableList.of(createOp1, createOp2));
    System.out.println("create multi executed");

    assertThat(zk.getData(path1, false, null)).isEqualTo(data1);
    assertThat(zk.getData(path2, false, null)).isEqualTo(data2);

    // Delete two znodes
    Op deleteOp1 = Op.delete(path1, -1);
    Op deleteOp2 = Op.delete(path2, -1);

    // Asynchronous API
    zk.multi(ImmutableList.of(deleteOp1, deleteOp2), callback, null);
  }

  @Test
  public void testTransaction() throws Exception {
    closeLatch = new CountDownLatch(1);

    // Create two znodes
    Transaction tx = zk.transaction();
    tx.create(path1, data1, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
    tx.create(path2, data2, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);

    // Synchronous API
    tx.commit();
    System.out.println("transaction committed");

    assertThat(zk.getData(path1, false, null)).isEqualTo(data1);
    assertThat(zk.getData(path2, false, null)).isEqualTo(data2);

    // Delete two znodes
    tx = zk.transaction();
    tx.delete(path1, -1);
    tx.delete(path2, -1);

    // Asynchronous API
    tx.commit(callback, null);
  }

  @Test
  public void testTransactionWithCheck() throws Exception {
    closeLatch = new CountDownLatch(0);

    {
      Transaction tx = zk.transaction();
      tx.create(path1, data1, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
      tx.create(path2, data2, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
      tx.check(path1, 0);
      tx.check(path2, 0);
      tx.commit();
    }

    {
      Transaction tx = zk.transaction();
      tx.check(path1, 0);
      tx.check(path2, 0);
      tx.delete(path1, 0);
      tx.delete(path2, 0);
      tx.commit();
    }
  }


  /**
   * getChildren does not list descendants recursively.
   */
  @Test
  public void testGetChilren() throws Exception {
    closeLatch = new CountDownLatch(0);
    List<String> paths = zk.getChildren("/a", false);
    System.out.printf("child paths: %s\n", paths);
  }

  class DefaultWatcher implements Watcher {
    @Override
    public void process(WatchedEvent event) {
      if (event.getType() == Event.EventType.None
          && event.getState() == Event.KeeperState.SyncConnected) {
        System.out.println("zookeeper client connected");
        startLatch.countDown();
      }
    }
  }
}
