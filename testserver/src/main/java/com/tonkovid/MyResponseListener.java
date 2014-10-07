package com.tonkovid;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.util.AttributeKey;

public class MyResponseListener implements ChannelFutureListener {
	private AttributeKey<Connection> attrConnection = AttributeKey.valueOf("attrConnection");
	ChannelHandlerContext ctx;
	Connection connection;

	public MyResponseListener(ChannelHandlerContext ctx, FullHttpResponse response) {
		super();
		this.ctx = ctx;
		connection = ctx.attr(attrConnection).get();
		connection.setWriteStarted(System.currentTimeMillis());
		connection.setResponse(response);
	}

	@Override
	public void operationComplete(ChannelFuture future) throws Exception {
		connection.setWriteCompleted(System.currentTimeMillis());
		future.channel().close();
	}

}
