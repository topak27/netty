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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * @author Alexey Tonkovid 2014
 */
class StatusResponse implements Callable<ChannelFuture>{
	ChannelHandlerContext ctx;
	List<Connection> totalConnectionList;
	Map<String, Integer> redirectMap;
	DefaultChannelGroup activeChannels;


	public StatusResponse(ChannelHandlerContext ctx, List<Connection> totalConnectionList, 
			DefaultChannelGroup activeChannels, Map<String, Integer> redirectMap) {

		this.totalConnectionList = totalConnectionList;
		this.activeChannels = activeChannels;
		this.redirectMap = redirectMap;
		this.ctx = ctx;
	}

	@Override
	public ChannelFuture call() throws Exception {
		StringBuilder content = new StringBuilder();

		synchronized (totalConnectionList) {
			content.append(getActiveChannels(activeChannels))
			.append(getTotalRequests())
			.append(getTotalUniqueRequests())
			.append(getRequestToIPtable())
			.append(getRedirectTable(redirectMap))
			.append(getLogTable());	
		}

		FullHttpResponse response = new DefaultFullHttpResponse (HTTP_1_1, OK, Unpooled.copiedBuffer(content, CharsetUtil.UTF_8));
		return ctx.writeAndFlush(response).addListener(new MyResponseListener(ctx, response));
	}

	public String getLogTable() {
		List<Connection> sublist;
		StringBuilder content = new StringBuilder();
		int size = totalConnectionList.size();

		if (size > 16) {
			sublist = totalConnectionList.subList(size-16, size);
		} else {
			sublist = totalConnectionList;
		}

		content.append("\"Log of last 16\" table\n");

		for (Iterator<Connection> iterator = sublist.iterator(); iterator.hasNext();) {
			Connection request = iterator.next();
			content.append(totalConnectionList.indexOf(request) + 1).append('\t')
			.append(request.toString()).append('\n');
		}	
		return content.toString();
	}

	public String getActiveChannels(DefaultChannelGroup activeChannels) {
		StringBuilder content = new StringBuilder();
		content.append("Active channels: ").append(activeChannels.size()).append("\n");
		return content.toString();
	}

	public String getRedirectTable(Map<String, Integer> redirectMap) {
		StringBuilder content = new StringBuilder();
		content.append("\"Redirect\" table\n");

		for (Map.Entry<String, Integer> entry : redirectMap.entrySet()) {
			content.append(entry.getValue()).append('\t')
			.append("to\t").append(entry.getKey()).append('\n');
		}
		content.append("\n\n");
		return content.toString();
	}

	public String getRequestToIPtable() {
		StringBuilder content = new StringBuilder();
		Map<String, List<Connection>> requestToIPmap = new HashMap<>();

		for (Iterator<Connection> iterator = totalConnectionList.iterator(); iterator.hasNext();) {
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

	public String getTotalUniqueRequests() {
		Set<String> ipSet = new HashSet<>(10);
		StringBuilder content = new StringBuilder();

		for (Connection request : totalConnectionList) {
			ipSet.add(request.getIp().getHostString());
		}
		content.append("Total unique requests: ").append(ipSet.size()).append("\n\n");
		return content.toString();
	}

	public String getTotalRequests() {
		StringBuilder content = new StringBuilder();
		content.append("Total requests: ").append(totalConnectionList.size()).append("\n");
		return content.toString();
	}

}