package org.yao;

import org.apache.zookeeper.server.auth.DigestAuthenticationProvider;

/**
 * Generate digest for ZooKeeper super user authentication.
 */
public class DigestGenerator {

  public static void main(String[] args) throws Exception {
    System.out.println(DigestAuthenticationProvider.generateDigest("super:jingguo"));
  }
}
