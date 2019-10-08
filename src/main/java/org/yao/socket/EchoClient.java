package org.yao.socket;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * <pre>
 * java -cp build/classes/java/main org.yao.socket.EchoClient 127.0.0.1 6000
 * </pre>
 */
public class EchoClient {
  public static void main(String[] args) throws Exception {
    if (args.length != 2) {
      System.err.println("Usage: java EchoClient <ip> <port number>");
      System.exit(1);
    }

    String ip = args[0];
    int port = Integer.parseInt(args[1]);

    try (Socket socket = new Socket(ip, port);
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in))) {
      String msg;
      while ((msg = stdin.readLine()) != null) {
        out.println(msg);
        System.out.println("echo: " + in.readLine());
      }
    }
  }
}
