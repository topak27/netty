package com.tonkovid;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.util.CharsetUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * @author Alexey Tonkovid 2014
 */
class StatusResponse implements Callable<ChannelFuture>{
	private List<Connection> totalConnectionList = MyHttpHandler.totalConnectionList;
	private DefaultChannelGroup activeChannels = MyHttpHandler.activeChannels;
	private Map<String, Integer> redirectMap = MyHttpHandler.redirectMap;
	private Set<String> ipSet = MyHttpHandler.ipSet;
	private ChannelHandlerContext ctx;
	private Connection connection;

	public StatusResponse(ChannelHandlerContext ctx, Connection connection) {
		this.ctx = ctx;
		this.connection = connection;
	}

	@Override
	public ChannelFuture call() throws Exception {
		StringBuilder content = new StringBuilder();

		content.append(getActiveChannels())
		.append(getTotalRequests())
		.append(getTotalUniqueRequests())
		.append(getRequestToIPtable())
		.append(getRedirectTable())
		.append(getLogTable());	

		FullHttpResponse response = new DefaultFullHttpResponse (HTTP_1_1, OK, Unpooled.copiedBuffer(content, CharsetUtil.UTF_8));
		connection.setResponse(response);
		return ctx.writeAndFlush(response).addListener(new MyResponseListener(ctx, connection));
	}

	private String getLogTable() {
		List<Connection> sublist = new ArrayList<Connection>(16);
		StringBuilder content = new StringBuilder();
		int size = totalConnectionList.size();

		synchronized (totalConnectionList) {
			if (size > 16) {
				sublist.addAll(totalConnectionList.subList(size-16, size));
			} else {
				sublist.addAll(totalConnectionList);
			}
		}

		content.append("\"Log of last 16\" table\n");

		for (Iterator<Connection> iterator = sublist.iterator(); iterator.hasNext();) {
			Connection connection = iterator.next();
			content.append(totalConnectionList.indexOf(connection) + 1).append('\t')
			.append(connection.toString()).append('\n');
		}	
		return content.toString();
	}

	private String getActiveChannels() {
		StringBuilder content = new StringBuilder();
		content.append("Active channels: ").append(activeChannels.size()).append("\n");
		return content.toString();
	}

	String getRedirectTable() {
		StringBuilder content = new StringBuilder();
		content.append("\"Redirect\" table\n");

		for (Map.Entry<String, Integer> entry : redirectMap.entrySet()) {
			content.append(entry.getValue()).append('\t')
			.append("to\t").append(entry.getKey()).append('\n');
		}
		content.append("\n\n");
		return content.toString();
	}

	private String getRequestToIPtable() {
		StringBuilder content = new StringBuilder();
		List<Connection> totalList = new ArrayList<Connection>();
		Map<String, List<Connection>> requestToIPmap = new HashMap<>();

		totalList.addAll(totalConnectionList);

		for (Iterator<Connection> iterator = totalList.iterator(); iterator.hasNext();) {
			Connection request = iterator.next();
			String ip = request.getIp().getHostString();

			if (requestToIPmap.containsKey(ip)) {
				requestToIPmap.get(ip).add(request);
			} else {
				List<Connection> list = new ArrayList<Connection>();
				list.add(request);
				requestToIPmap.put(ip, list);
			}
		}

		content.append("\"Request - to - IP\" table\n");

		for (Map.Entry<String, List<Connection>> entry : requestToIPmap.entrySet()) {
			content.append(entry.getKey()).append("\t\t").append("total: ")
			.append(entry.getValue().size()).append('\t').append("last: ")
			.append(entry.getValue().get(entry.getValue().size() - 1).getTimeStarted())
			.append('\n');
		}
		content.append("\n\n");
		return content.toString();
	}

	private String getTotalUniqueRequests() {
		StringBuilder content = new StringBuilder();
		content.append("Total unique requests: ").append(ipSet.size()).append("\n\n");
		return content.toString();
	}

	private String getTotalRequests() {
		StringBuilder content = new StringBuilder();
		content.append("Total requests: ").append(totalConnectionList.size()).append("\n");
		return content.toString();
	}

}