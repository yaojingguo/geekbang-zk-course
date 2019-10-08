package org.yao.socket;

import com.google.common.io.Closeables;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * <pre>
 * java -cp build/classes/java/main org.yao.socket.EchoServer 6000
 * </pre>
 */
public class EchoServer {
  public static void main(String[] args) throws IOException {
    if (args.length != 1) {
      System.err.println("Usage: java EchoServer <port number>");
      System.exit(1);
    }
    int port = Integer.parseInt(args[0]);
    ServerSocket serverSocket = null;
    Socket socket = null;
    PrintWriter out = null;
    BufferedReader in = null;
    try {
      // Method 1
      // serverSocket = new ServerSocket(Integer.parseInt(args[0]));

      // Method 2
      serverSocket = new ServerSocket();
      serverSocket.setReuseAddress(true);
      InetSocketAddress address = new InetSocketAddress(port);
      serverSocket.bind(address);

      socket = serverSocket.accept();
      out = new PrintWriter(socket.getOutputStream(), true);
      in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      String msg;
      while ((msg = in.readLine()) != null) {
        out.println(msg);
      }
    } finally {
      close(out);
      close(in);
      close(socket);
      close(serverSocket);
    }
  }

  private static void close(Closeable closeable) {
    if (closeable == null) {
      return;
    }
    try {
      closeable.close();
    } catch (Throwable th) {
      throw new RuntimeException(th);
    }
  }
}
