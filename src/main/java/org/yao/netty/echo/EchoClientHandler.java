package org.yao.netty.echo;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * Handler implementation for the echo client. It initiates the ping-pong traffic between the echo
 * client and server by sending the first message to the server.
 */
public class EchoClientHandler extends ChannelInboundHandlerAdapter {

  private final ByteBuf firstMessage;

  /** Creates a client-side handler. */
  public EchoClientHandler() {
    firstMessage = Unpooled.buffer(11);
    for (int i = 0; i < firstMessage.capacity() - 1; i++) {
      firstMessage.writeByte((byte) i + '0');
    }
    firstMessage.writeByte('\n');
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) {
    // System.out.printf("channelActive\n");
    ctx.writeAndFlush(firstMessage);
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) {
    // System.out.printf("channelRead\n");
    ByteBuf in = (ByteBuf) msg;

    while (in.isReadable()) {
      System.out.print((char) in.readByte());
    }
    System.out.flush();

    // Equivalent to ByteBuffer's flip
    in.readerIndex(0);
    ctx.write(in);
  }

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) {
    // System.out.printf("channelReadComplete\n");
    ctx.flush();
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    // Close the connection when an exception is raised.
    cause.printStackTrace();
    ctx.close();
  }
}
