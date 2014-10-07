package com.tonkovid;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.MessageSizeEstimator;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;

import java.net.InetSocketAddress;
import java.sql.Timestamp;

/**
 * @author Alexey Tonkovid 2014
 */
class Connection {
	private MessageSizeEstimator sizeEstimator;
	private Timestamp timeStarted;
	private int bytesSent, bytesReceived;
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
		sizeEstimator = ctx.channel().config().getMessageSizeEstimator();
	}

	public Timestamp getTimeStarted() {
		return timeStarted;
	}

	public int getBytesSent() {
		return bytesSent;
	}

	private void setBytesSent(int bytesSent) {
		this.bytesSent = bytesSent;
	}

	public int getBytesReceived() {
		return bytesReceived;
	}

	private void setBytesReceived(int bytesReceived) {
		this.bytesReceived = bytesReceived;
	}

	public HttpRequest getRequest() {
		return request;
	}

	public void setRequest(HttpRequest request) {
		this.request = request;
		timeStarted = new Timestamp(System.currentTimeMillis());
		setBytesReceived(sizeEstimator.newHandle().size(request));
	}

	public HttpResponse getResponse() {
		return response;
	}

	public void setResponse(HttpResponse response) {
		this.response = response;
		setBytesSent(sizeEstimator.newHandle().size(response));
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

	public double getSpeed() {
		long time = (readCompleted - readStarted) + (writeCompleted - writeStarted);
		if (time != 0) {
			return (bytesReceived + bytesSent)/time;
		} else {
			return 0;
		}
	}

	@Override
	public String toString() {
		StringBuilder answer = new StringBuilder();
		answer.append(timeStarted + "\t" + "   IP: " + ip.getHostString() + "\t" 
				+ request.getUri() + "\t\t" + "received: " + bytesReceived + "\t" 
				+ "sent: " + bytesSent + "\t" + "   speed, KB/s: ");
		if (getSpeed() == 0) {
			answer.append("time < 1ms");
		} else {
			answer.append(getSpeed());
		}
		return answer.toString();
	}	
}