package io.engelberth.http;

import java.io.IOException;

public interface HttpRequestListener {
	// Return null to close connection
	public void handleRequest(Server.Connection connection, HttpHeader header) throws IOException;
}
