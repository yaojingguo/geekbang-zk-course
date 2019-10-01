package org.yao;

import com.sun.tools.internal.ws.wsdl.document.Output;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * In C, listening on a port needs to use socket, bind, listen and accept API. In Java, listen API
 * does not exist.
 */
public class EchoServer {

  public static void main(String[] args) throws Exception {
    int port = 4444;

    // Method 1
    // ServerSocket serverSocket = new ServerSocket(port);

    // Method 2
    ServerSocket serverSocket = new ServerSocket();
    InetSocketAddress address = new InetSocketAddress(port);
    serverSocket.bind(address);
    System.out.printf("started server on port %d\n", port);

    while (true) {
      Socket clientSocket = serverSocket.accept();
      System.out.println("accepted connection from client");

      InputStream in = clientSocket.getInputStream();
      OutputStream out = clientSocket.getOutputStream();

      int ch;
      while ((ch = in.read()) != -1) {
        //        System.out.printf("byte: %d\n", ch);
        out.write(ch);
      }

      System.out.println("closing connection with client");
      out.close();
      in.close();
      clientSocket.close();
    }
  }
}
