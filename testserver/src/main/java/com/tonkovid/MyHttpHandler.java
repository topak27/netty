package com.tonkovid;

import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpResponseStatus.TEMPORARY_REDIRECT;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @author Alexey Tonkovid 2014
 */
public class MyHttpHandler extends SimpleChannelInboundHandler<HttpRequest> {
	static final Set<String> ipSet = new HashSet<>();
	static final Map<String, Integer> redirectMap = new HashMap<>();
	private static final DefaultEventExecutorGroup statusResponseExecutor = new DefaultEventExecutorGroup(8);
	static final List<Connection> totalConnectionList = new ArrayList<Connection>(10000);
	static final DefaultChannelGroup activeChannels  = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
	private Connection connection;

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		activeChannels.add(ctx.channel());
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		connection = new Connection(ctx);
		connection.setReadStarted(System.nanoTime());
		boolean autoRelease = true;
		boolean release = true;
		try {
			if (acceptInboundMessage(msg)) {
				HttpRequest imsg = (HttpRequest) msg;
				messageReceived(ctx, imsg);
			} else {
				release = false;
				ctx.fireChannelRead(msg);
			}
		} finally {
			if (autoRelease && release) {
				ReferenceCountUtil.release(msg);
			}
		}
	}

	@Override
	protected void messageReceived(ChannelHandlerContext ctx, HttpRequest req) throws Exception {
		QueryStringDecoder queryStringDecoder = new QueryStringDecoder(req.getUri());
		connection.setReadCompleted(System.nanoTime());
		connection.setRequest(req);

		synchronized (totalConnectionList) {
			totalConnectionList.add(connection);	
		}
		switch (queryStringDecoder.path()) {
		case "/hello":
			helloResponse(ctx);
			break;
		case "/status":
			statusResponse(ctx);
			break;
		case "/redirect":
			redirectResponse(ctx, queryStringDecoder);
			break;
		default:
			_404Response(ctx);
		}

	}

	/**
	 * Sends "Hello, World!" string after 10 sec.
	 * @throws InterruptedException 
	 */
	private void helloResponse(ChannelHandlerContext ctx) throws InterruptedException {
		FullHttpResponse response; 

		response = new DefaultFullHttpResponse (HTTP_1_1, OK, Unpooled.copiedBuffer("Hello world!", CharsetUtil.UTF_8));
		connection.setResponse(response);
		ctx.channel().eventLoop().schedule(new Runnable() {              
			@Override
			public void run() {
				ctx.writeAndFlush(response).addListener(new MyResponseListener(ctx, connection));
			}
		}, 10, TimeUnit.SECONDS);
	}

	/**
	 * Redirects to the first asked URL
	 */
	private void redirectResponse(ChannelHandlerContext ctx, QueryStringDecoder queryStringDecoder) {
		List<String> urlList = queryStringDecoder.parameters().get("url");
		FullHttpResponse response = new DefaultFullHttpResponse (HTTP_1_1, TEMPORARY_REDIRECT);

		if (urlList != null && !urlList.isEmpty()) {
			String url = urlList.get(0);

			if (redirectMap.containsKey(url)) {
				Integer i = (Integer)redirectMap.get(url);
				redirectMap.replace(url, ++i);
			} else {
				redirectMap.put(url, 1);
			}

			response.headers().set("LOCATION", url);
		}
		connection.setResponse(response);
		ctx.writeAndFlush(response).addListener(new MyResponseListener(ctx, connection));
	}

	/**
	 * Shows the Status page
	 * @throws Exception 
	 */
	private void statusResponse(ChannelHandlerContext ctx) throws Exception {
		statusResponseExecutor.submit(new StatusResponse(ctx, connection));
	}

	/**
	 * Returns 404 as default
	 */
	private void _404Response(ChannelHandlerContext ctx) {
		FullHttpResponse response = new DefaultFullHttpResponse (HTTP_1_1, NOT_FOUND);
		connection.setResponse(response);
		ctx.writeAndFlush(response).addListener(new MyResponseListener(ctx, connection));
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		cause.printStackTrace();
		ctx.close();
	}
}
