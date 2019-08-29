package org.yao;

import com.google.common.io.Closeables;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.retry.RetryOneTime;
import org.apache.curator.utils.CloseableUtils;
import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class PathChildrenCacheTests {
  private String connectString = "localhost:2181";

  @Test
  public void testBasics() throws Exception {
    {
      CuratorFramework client =
          CuratorFrameworkFactory.newClient(connectString, new RetryOneTime(1));
      PathChildrenCache cache = null;
      client.start();
      String basePath = "/path-children-cache-tests";
      String child1Path = basePath + "/one";
      String child2Path = basePath + "/two";

      try {
        client.create().forPath(basePath);

        final BlockingQueue<PathChildrenCacheEvent.Type> events = new LinkedBlockingQueue<>();

        cache = new PathChildrenCache(client, basePath, true);
        cache
            .getListenable()
            .addListener(
                (c, event) -> {
                  if (event.getData().getPath().startsWith(basePath)) {
                    events.offer(event.getType());
                  }
                });
        cache.start();

        String child1Version0Data = "hey there";
        client.create().forPath(child1Path, child1Version0Data.getBytes());
        assertThat(events.poll(1, TimeUnit.SECONDS))
            .isEqualTo(PathChildrenCacheEvent.Type.CHILD_ADDED);

        String child1Version1Data = "sup!";
        client.setData().forPath(child1Path, child1Version1Data.getBytes());
        assertThat(events.poll(1, TimeUnit.SECONDS))
            .isEqualTo(PathChildrenCacheEvent.Type.CHILD_UPDATED);
        assertThat(cache.getCurrentData(child1Path).getData())
            .isEqualTo(child1Version1Data.getBytes());

        String child2Version0Data = "foo";
        client.create().forPath(child2Path, child2Version0Data.getBytes());
        assertThat(events.poll(1, TimeUnit.SECONDS))
            .isEqualTo(PathChildrenCacheEvent.Type.CHILD_ADDED);
        assertThat(cache.getCurrentData(child2Path).getData())
            .isEqualTo(child2Version0Data.getBytes());

        assertThat(cache.getCurrentData().size()).isEqualTo(2);

        client.delete().forPath(child1Path);
        assertThat(events.poll(1, TimeUnit.SECONDS))
            .isEqualTo(PathChildrenCacheEvent.Type.CHILD_REMOVED);

        client.delete().deletingChildrenIfNeeded().forPath(basePath);

      } finally {
        CloseableUtils.closeQuietly(cache);
        CloseableUtils.closeQuietly(client);
      }
    }
  }
}
