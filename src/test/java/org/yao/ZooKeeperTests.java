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
import org.apache.zookeeper.data.Stat;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.truth.Truth.assertThat;

public class ZooKeeperTests {
  private String pathPrefix = "/single";
  private String multiPathPrefix = "/multi";
  private ZooKeeper zk;
  private CountDownLatch startLatch;
  private CountDownLatch closeLatch = new CountDownLatch(0);
  private AsyncCallback.MultiCallback callback;

  private String path1 = multiPathPrefix + "1";
  private String path2 = multiPathPrefix + "2";
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

  /** getChildren does not list descendants recursively. */
  @Test
  public void testGetChilren() throws Exception {
    String path = pathPrefix + "-get-children";
    zk.create(path, new byte[]{'a'} ,ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
    List<String> paths = zk.getChildren(path, false);
    System.out.printf("child paths: %s\n", paths);
    zk.delete(path, -1);
  }

  /*
   * 1. If the znode does not, KeeperException.NoNodeException is thrown for any version value.
   * 2. Otherwise.
   *   1. If version is less than -1, KeeperException.BadVersionException is thrown.
   *   2. Else if version is -1, setData succeeds.
   *   3. Else if version mismatches, KeeperException.BadVersionException is thrown.
   *   4. Else setData succeeds.
   */
  @Test
  public void testSetData() throws InterruptedException, KeeperException {
    String path = multiPathPrefix + "-set";
    byte[] dataV0 = {'o'};

    for (int version : new int[] {-2, -1, 0}) {
      System.out.printf("setting data for version %d...\n", version);
      boolean exceptionThrown = false;
      try {
        zk.setData(path, dataV0, -1);
      } catch (KeeperException.NoNodeException ex) {
        exceptionThrown = true;
        ex.printStackTrace();
      }
      assertThat(exceptionThrown).isTrue();
    }

    assertThat(zk.create(path, data1, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT))
        .isEqualTo(path);
    System.out.printf("znode created\n");

    for (int version : new int[] {-2, 1}) {
      System.out.printf("setting data for version %d...\n", version);
      boolean exceptionThrown = false;
      try {
        zk.setData(path, dataV0, version);
      } catch (KeeperException.BadVersionException ex) {
        exceptionThrown = true;
        ex.printStackTrace();
      }
      assertThat(exceptionThrown).isTrue();
    }

    byte[] dataV1 = {'p'};
    zk.setData(path, dataV1, 0);
    zk.delete(path, 1);
  }

  @Test
  public void testDelete() throws Exception {
    String path = pathPrefix + "-delete";
    boolean exceptionThrown = false;
    try {
      zk.delete(path, 0);
    } catch (KeeperException.NoNodeException ex) {
      exceptionThrown = true;
      ex.printStackTrace();
    }
    assertThat(exceptionThrown).isTrue();

    zk.create(path, new byte[]{'a'}, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
    exceptionThrown = false;
    try {
      zk.delete(path, -2);
    } catch (KeeperException.BadVersionException ex) {
      exceptionThrown = true;
      ex.printStackTrace();
    }
    assertThat(exceptionThrown).isTrue();
    zk.delete(path, 0);
  }

  @Test
  public void testAsyncWithFirstFailture() throws Exception {
    final AtomicInteger counter = new AtomicInteger(0);
    AsyncCallback.StatCallback statCallback =
        (int rc, String path, Object ctx, Stat stat) -> {
          if (rc == KeeperException.Code.BADVERSION.intValue()) {
            assertThat(counter.getAndIncrement()).isEqualTo(0);
            System.out.printf("rc: bad version\n");
          } else if (rc == KeeperException.Code.OK.intValue()) {
            assertThat(counter.getAndIncrement()).isEqualTo(1);
            System.out.printf("rc: ok\n");
          } else {
            throw new IllegalStateException();
          }
        };

    String path1 = pathPrefix + "-1";
    byte[] dataV0 = {'a'};
    byte[] dataV1 = {'b'};
    zk.create(path1, dataV0, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
    // Fail
    zk.setData(path1, dataV1, 1, statCallback, null);
    // Succeed
    zk.setData(path1, dataV1, 0, statCallback, null);

    assertThat(zk.getData(path1, false, null)).isEqualTo(dataV1);
    zk.delete(path1, -1);
  }

  @Test
  public void testAsyncWithSecondFailture() throws Exception {
    final AtomicInteger counter = new AtomicInteger(0);
    AsyncCallback.StatCallback statCallback =
        (int rc, String path, Object ctx, Stat stat) -> {
          if (rc == KeeperException.Code.BADVERSION.intValue()) {
            assertThat(counter.getAndIncrement()).isEqualTo(1);
            System.out.printf("rc: bad version\n");
          } else if (rc == KeeperException.Code.OK.intValue()) {
            assertThat(counter.getAndIncrement()).isEqualTo(0);
            System.out.printf("rc: ok\n");
          } else {
            throw new IllegalStateException();
          }
        };

    String path1 = pathPrefix + "-2";
    byte[] dataV0 = {'a'};
    byte[] dataV1 = {'b'};
    zk.create(path1, dataV0, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
    // Fail
    zk.setData(path1, dataV1, 0, statCallback, null);
    // Succeed
    zk.setData(path1, dataV1, 1, statCallback, null);

    assertThat(zk.getData(path1, false, null)).isEqualTo(dataV1);
    zk.delete(path1, -1);
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
  public void testTx() throws Exception {
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
  public void testCreateAndCheckTx() throws Exception {
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

  @Test
  public void testSetDataAndFailedCheckTx() throws Exception {
    closeLatch = new CountDownLatch(0);
    byte[] data1v0 = {'a'};
    byte[] data2v0 = {'b'};

    {
      Transaction tx = zk.transaction();
      tx.create(path1, data1v0, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
      tx.create(path2, data2v0, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
      tx.check(path1, 0);
      tx.check(path2, 0);
      tx.commit();
    }

    byte[] data1v1 = {'x'};
    byte[] data2v1 = {'y'};
    boolean expectionThrown = false;
    try {
      Transaction setTx = zk.transaction();
      setTx.setData(path1, data1v1, 0);
      setTx.setData(path2, data2v1, 0);
      setTx.check(path1, 1);
      // This check will fail.
      setTx.check(path1, 2);
      setTx.commit();
    } catch (KeeperException.BadVersionException ex) {
      ex.printStackTrace();
      expectionThrown = true;
    }
    assertThat(expectionThrown).isTrue();

    assertThat(zk.getData(path1, false, null)).isEqualTo(data1v0);
    assertThat(zk.getData(path2, false, null)).isEqualTo(data2v0);

    zk.delete(path1, -1);
    zk.delete(path2, -1);
  }

  @Test
  public void testCheckTx() throws Exception {
    String checkPath = multiPathPrefix + "-check";
    boolean exceptionThrown = false;
    try {
      Transaction checkTx = zk.transaction();
      checkTx.check(checkPath, -1);
      checkTx.commit();
    } catch (KeeperException.NoNodeException ex) {
      exceptionThrown = true;
      ex.printStackTrace();
    }

    assertThat(exceptionThrown).isTrue();
    zk.create(checkPath, new byte[] {'a'}, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);

    try {
      Transaction checkTx = zk.transaction();
      // If version is -1, the check passes if the znode exists.
      checkTx.check(checkPath, -1);
      checkTx.commit();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    zk.delete(checkPath, -1);
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
