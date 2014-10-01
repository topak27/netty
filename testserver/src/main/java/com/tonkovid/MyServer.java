package com.tonkovid;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

/**
 * @author Alexey Tonkovid 2014
 */
public class MyServer {

	private final String domName;
	private final int port;

	public MyServer(String domName, int port) {
		this.domName = domName;
		this.port = port;
	}

	public void run() throws Exception {
		EventLoopGroup bossGroup = new NioEventLoopGroup();
		EventLoopGroup workerGroup = new NioEventLoopGroup();	

		try {
			ServerBootstrap b = new ServerBootstrap();
			b.localAddress(new LocalAddress(domName))
			.group(bossGroup, workerGroup)			
			.channel(NioServerSocketChannel.class)
			.childHandler(new MyServerInitializer())
			.bind(port).sync().channel().closeFuture().sync();		
		} finally {
			workerGroup.shutdownGracefully();
			bossGroup.shutdownGracefully();
		}
	}

	public static void main(String[] args) throws Exception {
		new MyServer("somedomain", 8080).run();
	}
}

