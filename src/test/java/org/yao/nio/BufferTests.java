package org.yao.nio;

import org.junit.Before;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Paths;

import static com.google.common.truth.Truth.assertThat;

public class BufferTests {
  private int CAP = 10;
  private ByteBuffer buf;
  private byte[] chars = {'h', 'e', 'l', 'l', 'o'};
  private String pathname = "channel-data";

  @Before
  public void setUp() {
    buf = ByteBuffer.allocate(CAP);
    verifyBufferState(buf, 0, 10);

    // Put
    for (byte ch : chars) {
      buf.put(ch);
    }
    verifyBufferState(buf, 5, 10);
  }

  @Test
  public void testBasics() {
    // Get
    buf.flip();
    verifyGet(buf);

    // Get again
    buf.rewind();
    verifyGet(buf);

    buf.clear();
    verifyBufferState(buf, 0, 10);
  }

  @Test
  public void testWrap() {
    byte[] world = {'w', 'o', 'r', 'l', 'd'};
    ByteBuffer buf = ByteBuffer.wrap(world);
    verifyBufferState(buf, 0, 5);
  }

  @Test
  public void testDuplicate() {
    buf.flip();
    ByteBuffer clone = buf.duplicate();
    verifyGet(buf);
    verifyBufferState(clone, 0, 5);
    verifyGet(clone);
  }

  @Test
  public void testWithChannel() throws Exception {
    Files.deleteIfExists(Paths.get(pathname));
    try (FileChannel channel = new FileOutputStream(pathname).getChannel()) {
      buf.flip();
      verifyBufferState(buf, 0, 5);
      // Channel write is equivalent to a series of gets.
      channel.write(buf);
      verifyBufferState(buf, 5, 5);
    }

    ByteBuffer buf2 = ByteBuffer.allocate(CAP);
    try (FileChannel channel = new FileInputStream(pathname).getChannel()) {
      // Channel read is equivalent to a series of puts.
      channel.read(buf2);
      verifyBufferState(buf2, 5, 10);
      buf2.flip();
      verifyBufferState(buf2, 0, 5);
      verifyGet(buf2);
    }
  }

  private void verifyGet(ByteBuffer buffer) {
    int i = 0;
    verifyBufferState(buffer, 0, 5);
    while (buffer.hasRemaining()) {
      byte ch = buffer.get();
      assertThat(ch).isEqualTo(chars[i++]);
    }
    verifyBufferState(buffer, 5, 5);
  }

  private void verifyBufferState(ByteBuffer buf, int position, int limit) {
    assertThat(buf.position()).isEqualTo(position);
    assertThat(buf.limit()).isEqualTo(limit);
  }
}
