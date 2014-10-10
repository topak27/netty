package com.tonkovid;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import java.net.InetSocketAddress;
import java.sql.Timestamp;

/**
 * @author Alexey Tonkovid 2014
 */
class Connection {
	private Timestamp timeStarted;
	private long bytesSent, bytesReceived;
	private HttpRequest request;
	private HttpResponse response;
	private InetSocketAddress ip;
	private long readStarted;
	private long readCompleted;
	private long writeStarted;
	private long writeCompleted;

	public Connection(ChannelHandlerContext ctx) {
		super();
		ip = (InetSocketAddress)ctx.channel().remoteAddress();
		MyHttpHandler.ipSet.add(ip.getHostString());
	}

	public Timestamp getTimeStarted() {
		return timeStarted;
	}

	public long getBytesSent() {
		return bytesSent;
	}

	public void setBytesSent(long bytesSent) {
		this.bytesSent = bytesSent;
	}

	public long getBytesReceived() {
		return bytesReceived;
	}

	public void setBytesReceived(long bytesReceived) {
		this.bytesReceived = bytesReceived;
	}

	public HttpRequest getRequest() {
		return request;
	}

	public void setRequest(HttpRequest request) {
		this.request = request;
		timeStarted = new Timestamp(System.currentTimeMillis());
	}

	public HttpResponse getResponse() {
		return response;
	}

	public void setResponse(HttpResponse response) {
		this.response = response;
	}

	public InetSocketAddress getIp() {
		return ip;
	}

	public void setIp(InetSocketAddress ip) {
		this.ip = ip;
	}

	public void setReadStarted(long readStarted) {
		this.readStarted = readStarted;
	}

	public void setReadCompleted(long readCompleted) {
		this.readCompleted = readCompleted;
	}

	public void setWriteStarted(long writeStarted) {
		this.writeStarted = writeStarted;
	}

	public void setWriteCompleted(long writeCompleted) {
		this.writeCompleted = writeCompleted;
	}

	public long getSpeed() {
		double time = ((readCompleted - readStarted) + (writeCompleted - writeStarted))/1000;
		return (long) ((bytesReceived + bytesSent)*1000000/(time*1024));
	}

	@Override
	public String toString() {
		StringBuilder answer = new StringBuilder();
		answer.append(timeStarted + "\t" + "   IP: " + ip.getHostString() + "\t" 
				+ request.getUri() + "\t\t" + "received: " + bytesReceived + "\t" 
				+ "sent: " + bytesSent + "\t" + "   speed, KB/s: " + getSpeed());
		return answer.toString();
	}	
}