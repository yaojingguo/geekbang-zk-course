package org.yao.watchclient;

/**
 * A simple example program to use DataMonitor to start and stop executables based on a znode. The
 * program watches the specified znode and saves the data that corresponds to the znode in the
 * filesystem. It also starts the specified program with the specified arguments when the znode
 * exists and kills the program if the znode goes away.
 */
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

public class Executor implements Watcher, Runnable, DataMonitor.DataMonitorListener {
  private String znode;
  private DataMonitor dm;
  private ZooKeeper zk;
  private String pathname;
  private String exec[];
  private Process child;

  public Executor(String hostPort, String znode, String filename, String exec[])
      throws KeeperException, IOException {
    this.pathname = filename;
    this.exec = exec;
    zk = new ZooKeeper(hostPort, 3000, this);
    dm = new DataMonitor(zk, znode, this);
  }

  /** @param args */
  public static void main(String[] args) {
    if (args.length < 4) {
      System.err.println("USAGE: Executor hostPort znode pathname program [args ...]");
      System.exit(2);
    }
    String hostPort = args[0];
    String znode = args[1];
    String filename = args[2];
    String exec[] = new String[args.length - 3];
    System.arraycopy(args, 3, exec, 0, exec.length);
    try {
      new Executor(hostPort, znode, filename, exec).run();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  // Watcher
  @Override
  public void process(WatchedEvent event) {
    dm.handle(event);
  }

  // Runnable
  @Override
  public void run() {
    try {
      synchronized (this) {
        while (!dm.dead) {
          wait();
        }
      }
    } catch (InterruptedException e) {
    }
  }

  // DataMonitor.DataMonitorListener
  @Override
  public void closing(int rc) {
    synchronized (this) {
      notifyAll();
    }
  }

  // DataMonitor.DataMonitorListener
  @Override
  public void exists(byte[] data) {
    if (data == null) {
      if (child != null) {
        System.out.println("Killing handle");
        child.destroy();
        try {
          child.waitFor();
        } catch (InterruptedException e) {
        }
      }
      child = null;
    } else {
      if (child != null) {
        System.out.println("Stopping child");
        child.destroy();
        try {
          child.waitFor();
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
      try {
        FileOutputStream fos = new FileOutputStream(new File(pathname));
        fos.write(data);
        fos.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
      try {
        System.out.println("Starting child");
        child = Runtime.getRuntime().exec(exec);
        new StreamWriter(child.getInputStream(), System.out);
        new StreamWriter(child.getErrorStream(), System.err);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  static class StreamWriter extends Thread {
    OutputStream os;

    InputStream is;

    StreamWriter(InputStream is, OutputStream os) {
      this.is = is;
      this.os = os;
      start();
    }

    public void run() {
      byte b[] = new byte[80];
      int rc;
      try {
        while ((rc = is.read(b)) > 0) {
          os.write(b, 0, rc);
        }
      } catch (IOException e) {
      }
    }
  }
}
