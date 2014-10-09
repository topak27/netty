package com.tonkovid;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.traffic.ChannelTrafficShapingHandler;

/**
 * @author Alexey Tonkovid 2014
 */
public class MyServerInitializer extends ChannelInitializer<SocketChannel> {

	@Override
	protected void initChannel(SocketChannel ch) throws Exception {
		ChannelPipeline p = ch.pipeline();

		p.addLast("codec", new HttpServerCodec());
		p.addLast("trafficHandler", new ChannelTrafficShapingHandler(60));
		p.addLast("handler", new MyHttpHandler());	
	}
}
