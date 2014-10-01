package com.tonkovid;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.group.DefaultChannelGroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Alexey Tonkovid 2014
 */
class StatusResponse {
	ChannelHandlerContext ctx;
	List<Request> totalRequestList;

	public StatusResponse(ChannelHandlerContext ctx, List<Request> totalRequestList) {
		this.ctx = ctx;
		this.totalRequestList = totalRequestList;
	}

	public String getLogTable() {
		List<Request> sublist;
		StringBuilder content = new StringBuilder();
		int size = totalRequestList.size();

		if (size > 16) {
			sublist = totalRequestList.subList(size-16, size);
		} else {
			sublist = totalRequestList;
		}

		for (Iterator<Request> iterator = sublist.iterator(); iterator.hasNext();) {
			Request request = iterator.next();
			content.append(totalRequestList.indexOf(request) + 1).append('\t')
			.append(request.toString()).append('\n');
		}
		return content.toString();
	}

	public int getActiveChannels(DefaultChannelGroup activeChannels) {
		return activeChannels.size();
	}

	public String getRedirectTable(Map<String, Integer> redirectMap) {
		StringBuilder content = new StringBuilder();

		for (Map.Entry<String, Integer> entry : redirectMap.entrySet()) {
			content.append(entry.getValue()).append('\t')
			.append("to\t").append(entry.getKey()).append('\n');
		}
		return content.toString();
	}

	public String getRequestToIPtable() {
		StringBuilder content = new StringBuilder();
		Map<String, List<Request>> requestToIPmap = new HashMap<>();

		for (Iterator<Request> iterator = totalRequestList.iterator(); iterator.hasNext();) {
			Request request = iterator.next();
			String ip = request.ip.getHostString();

			if (requestToIPmap.containsKey(ip)) {
				requestToIPmap.get(ip).add(request);
			} else {
				List<Request> list = new ArrayList<Request>();
				list.add(request);
				requestToIPmap.put(ip, list);
			}
		}

		for (Map.Entry<String, List<Request>> entry : requestToIPmap.entrySet()) {
			content.append(entry.getKey()).append("\t\t").append("total: ")
			.append(entry.getValue().size()).append('\t').append("last: ")
			.append(entry.getValue().get(entry.getValue().size() - 1).timeStarted)
			.append('\n');
		}
		return content.toString();
	}

	public int getTotalUniqueRequests() {
		Set<String> ipSet = new HashSet<>(10);

		for (Request request : totalRequestList) {
			ipSet.add(request.ip.getHostString());
		}
		return ipSet.size();
	}

	public int getTotalRequests() {
		return totalRequestList.size();
	}

}