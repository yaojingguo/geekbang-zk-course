package org.yao.net;

import org.junit.Test;

import java.net.InetAddress;


import static com.google.common.truth.Truth.assertThat;

public class InetAddressTests {

  @Test
  public void testGetByName() throws Exception {
    InetAddress addr1 = InetAddress.getByName("www.baidu.com");
    // Output: www.baidu.com/61.135.169.121
    System.out.println(addr1);

    // etc/hosts:
    //   47.95.28.76 leci
    InetAddress addr2 = InetAddress.getByName("47.95.28.76");

    String hostName = addr2.getHostName();
    // Output: leci
    System.out.println(hostName);
    // Output: leci/47.95.28.76
    System.out.println(addr2);
  }

  @Test
  public void testGetAllByName() throws Exception {
    InetAddress[] addrs = InetAddress.getAllByName("www.baidu.com");
    // Output:
    //   www.baidu.com/61.135.169.121
    //   www.baidu.com/61.135.169.125
    for (InetAddress addr : addrs) System.out.println(addr);
  }

  @Test
  public void testGetAddress() throws Exception {
    InetAddress addr = InetAddress.getByName("192.168.1.9");
    byte[] expected = {(byte)192, (byte)168, 1, 9};
    assertThat(addr.getAddress()).isEqualTo(expected);
  }
}
