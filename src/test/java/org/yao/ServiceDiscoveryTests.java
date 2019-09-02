package org.yao;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryOneTime;
import org.apache.curator.utils.CloseableUtils;
import org.apache.curator.x.discovery.ServiceDiscovery;
import org.apache.curator.x.discovery.ServiceDiscoveryBuilder;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.ServiceProvider;
import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;

public class ServiceDiscoveryTests {
  private String connectString = "localhost:2181";

  /** Shows the basic usage for curator-x-discovery. */
  @Test
  public void testBasics() throws Exception {
    CuratorFramework client = null;
    ServiceDiscovery<String> discovery = null;
    ServiceProvider<String> provider = null;
    String serviceName = "test";
    String basePath = "/services";

    try {
      client = CuratorFrameworkFactory.newClient(connectString, new RetryOneTime(1));
      client.start();

      ServiceInstance<String> instance1 =
          ServiceInstance.<String>builder().payload("plant").name(serviceName).port(10064).build();
      ServiceInstance<String> instance2 =
          ServiceInstance.<String>builder().payload("animal").name(serviceName).port(10065).build();

      System.out.printf("instance1 id: %s\n", instance1.getId());
      System.out.printf("instance2 id: %s\n", instance2.getId());

      discovery =
          ServiceDiscoveryBuilder.builder(String.class)
              .basePath(basePath)
              .client(client)
              .thisInstance(instance1)
              .build();
      discovery.start();
      discovery.registerService(instance2);

      provider = discovery.serviceProviderBuilder().serviceName(serviceName).build();
      provider.start();

      assertThat(provider.getInstance().getId()).isNotEmpty();
      assertThat(provider.getAllInstances()).containsExactly(instance1, instance2);

      client.delete().deletingChildrenIfNeeded().forPath(basePath);
    } finally {
      CloseableUtils.closeQuietly(provider);
      CloseableUtils.closeQuietly(discovery);
      CloseableUtils.closeQuietly(client);
    }
  }
}
