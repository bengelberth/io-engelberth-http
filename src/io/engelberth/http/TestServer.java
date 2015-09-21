package io.engelberth.http;

import java.io.File;
import java.io.IOException;


public class TestServer {
	public static void main(String[] args) {
		Server server = new Server(3333);
		server.start();
		server.setHttpRequestListener(new HttpRequestListener() {
			@Override
			public void handleRequest(Server.Connection connection, HttpHeader header) throws IOException {
				if (header.request.equals("/stop")) {
					try {
						connection.send("text/plain", "Shutting down");
						connection.getServer().stop();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					return;
				}
				//System.out.println("request=" + request);
				//return new Server.Response().setContent("Hellow Request: " + request);
				connection.send(new File(header.request));
			}
			
		});
	}
}
