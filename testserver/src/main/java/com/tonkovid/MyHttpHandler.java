package com.tonkovid;

import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpResponseStatus.TEMPORARY_REDIRECT;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.MessageSizeEstimator;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Alexey Tonkovid 2014
 */
public class MyHttpHandler extends SimpleChannelInboundHandler<HttpRequest> {

	private static final List<Request> totalRequestList = new ArrayList<>(10000);
	private static final Map<String, Integer> redirectMap = new HashMap<>(50);
	private static final DefaultChannelGroup activeChannels  = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
	private final boolean autoRelease = true;
	private long readStarted;
	private long readCompleted;
	private long writeStarted;
	private long writeCompleted;

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		activeChannels.add(ctx.channel());
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		readStarted = System.currentTimeMillis();
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
	protected void messageReceived(ChannelHandlerContext ctx, final HttpRequest req) throws Exception {
		final MessageSizeEstimator sizeEstimator = ctx.channel().config().getMessageSizeEstimator();
		final Request request = new Request(req, ctx.channel().remoteAddress());
		final FullHttpResponse response;
		totalRequestList.add(request);
		readCompleted = System.currentTimeMillis();
		QueryStringDecoder queryStringDecoder = new QueryStringDecoder(req.getUri());

		switch (queryStringDecoder.path()) {
		case "/hello":
			response = helloResponse(ctx);
			break;
		case "/redirect":
			response = redirectResponse(ctx, queryStringDecoder);
			break;
		case "/status":
			response = statusResponse(ctx);
			break;
		default:
			response = _404Response(ctx);
			break;
		}

		writeStarted = System.currentTimeMillis();
		ctx.writeAndFlush(response).addListener(new ChannelFutureListener() {

			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				writeCompleted = System.currentTimeMillis();
				double time = (writeCompleted - writeStarted) + (readCompleted - readStarted);
				request.received = sizeEstimator.newHandle().size(req);
				request.sent = sizeEstimator.newHandle().size(response);
				if (time != 0) request.speed = (request.sent + request.received)/(time);
			}
		});
		ctx.close();
	}

	/**
	 * Sends "Hello, World!" string after 10 sec., 
	 * which waits for at the back thread
	 * @throws InterruptedException 
	 */
	private FullHttpResponse helloResponse(final ChannelHandlerContext ctx) throws InterruptedException {
		final FullHttpResponse response; 

		response = new DefaultFullHttpResponse (HTTP_1_1, OK, Unpooled.copiedBuffer("Hello world!", CharsetUtil.UTF_8));
		Thread t = new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					Thread.sleep(10000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		});
		t.start();
		t.join(); //crutch
		return response;
	}

	/**
	 * Redirects to the first asked URL
	 */
	private FullHttpResponse redirectResponse(ChannelHandlerContext ctx, QueryStringDecoder queryStringDecoder) {
		List<String> urlList = queryStringDecoder.parameters().get("url");
		FullHttpResponse response = null;

		if (!urlList.isEmpty()) {
			String url = urlList.get(0);

			if (redirectMap.containsKey(url)) {
				Integer i = (Integer)redirectMap.get(url);
				redirectMap.replace(url, ++i);
			} else {
				redirectMap.put(url, 1);
			}

			response = new DefaultFullHttpResponse (HTTP_1_1, TEMPORARY_REDIRECT);
			response.headers().set("LOCATION", url);
		}
		return response;
	}

	/**
	 * Shows the Status page
	 */
	private FullHttpResponse statusResponse(ChannelHandlerContext ctx) {
		FullHttpResponse response;
		StringBuilder content = new StringBuilder();
		StatusResponse statusResponse = new StatusResponse(ctx, totalRequestList);

		content.append("Active channels: ").append(statusResponse.getActiveChannels(activeChannels)).append("\n")
		.append("Total requests: ").append(statusResponse.getTotalRequests()).append("\n")
		.append("Total unique requests: ").append(statusResponse.getTotalUniqueRequests()).append("\n\n")
		.append("\"Request - to - IP\" table\n").append(statusResponse.getRequestToIPtable()).append("\n\n")
		.append("\"Redirect\" table\n").append(statusResponse.getRedirectTable(redirectMap)).append("\n\n")
		.append("\"Log of last 16\" table\n").append(statusResponse.getLogTable());

		response = new DefaultFullHttpResponse (HTTP_1_1, OK, Unpooled.copiedBuffer(content, CharsetUtil.UTF_8));
		return response;
	}

	/**
	 * Returns 404 as default
	 */
	private FullHttpResponse _404Response(ChannelHandlerContext ctx) {
		FullHttpResponse response = new DefaultFullHttpResponse (HTTP_1_1, NOT_FOUND);
		return response;
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		cause.printStackTrace();
		ctx.close();
	}
}
