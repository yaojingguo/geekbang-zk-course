package org.yao;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.NodeCache;
import org.apache.curator.retry.RetryOneTime;
import org.apache.curator.utils.CloseableUtils;
import org.junit.Test;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static com.google.common.truth.Truth.assertThat;

public class NodeCacheTests {
  private String connectString = "localhost:2181";

  @Test
  public void testBasics() throws Exception {
    NodeCache cache = null;
    CuratorFramework client = CuratorFrameworkFactory.newClient(connectString, new RetryOneTime(1));
    client.start();

    try {
      String basePath = "/test";
      client.create().forPath(basePath);

      String nodePath = basePath + "/node";
      cache = new NodeCache(client, nodePath);
      cache.start(true);

      final Semaphore semaphore = new Semaphore(0);

      cache.getListenable().addListener(() -> semaphore.release());

      assertThat(cache.getCurrentData()).isNull();

      String version0Data = "a";
      client.create().forPath(nodePath, version0Data.getBytes());
      assertThat(semaphore.tryAcquire(1, TimeUnit.SECONDS)).isTrue();
      assertThat(cache.getCurrentData().getData()).isEqualTo(version0Data.getBytes());

      String version1Data = "b";
      client.setData().forPath(nodePath, version1Data.getBytes());
      assertThat(semaphore.tryAcquire(1, TimeUnit.SECONDS)).isTrue();
      assertThat(cache.getCurrentData().getData()).isEqualTo(version1Data.getBytes());

      client.delete().forPath(nodePath);
      assertThat(semaphore.tryAcquire(1, TimeUnit.SECONDS)).isTrue();
      assertThat(cache.getCurrentData()).isNull();

      client.delete().forPath(basePath);
    } finally {
      CloseableUtils.closeQuietly(cache);
      CloseableUtils.closeQuietly(client);
    }
  }
}
