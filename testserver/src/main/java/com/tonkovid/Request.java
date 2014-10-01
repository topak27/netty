package com.tonkovid;

import io.netty.handler.codec.http.HttpRequest;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.sql.Timestamp;

/**
 * @author Alexey Tonkovid 2014
 */
class Request {
	final Timestamp timeStarted;
	final HttpRequest request;
	final InetSocketAddress ip;
	int sent, received;
	double speed;

	public Request(HttpRequest request, SocketAddress ip) {
		super();
		this.timeStarted = new Timestamp(System.currentTimeMillis());
		this.request = request;
		this.ip = (InetSocketAddress)ip;
	}

	@Override
	public String toString() {
		StringBuilder answer = new StringBuilder();
		answer.append(timeStarted + "\t" + "   IP: " + ip.getHostString() + "\t" 
				+ request.getUri() + "\t\t" + "received: " + received + "\t" 
				+ "sent: " + sent + "\t" + "   speed, KB/s: ");
		if (speed == 0) {
			answer.append("time < 1ms");
		} else {
			answer.append(speed);
		}
		return answer.toString();
	}	
}