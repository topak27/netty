package com.tonkovid;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.traffic.ChannelTrafficShapingHandler;

public class MyResponseListener implements ChannelFutureListener {
	private ChannelTrafficShapingHandler trafficHandler;
	private Connection connection;

	public MyResponseListener(ChannelHandlerContext ctx, Connection conn) {
		super();
		this.connection = conn;
		trafficHandler = (ChannelTrafficShapingHandler) ctx.channel().pipeline().toMap().get("trafficHandler");
		connection.setBytesReceived(trafficHandler.trafficCounter().cumulativeReadBytes());
		connection.setWriteStarted(System.nanoTime());
	}

	@Override
	public void operationComplete(ChannelFuture future) throws Exception {
		connection.setWriteCompleted(System.nanoTime());
		connection.setBytesSent(trafficHandler.trafficCounter().cumulativeWrittenBytes());
		future.channel().close();
	}

}
