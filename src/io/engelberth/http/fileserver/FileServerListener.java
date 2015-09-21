package io.engelberth.http.fileserver;

import io.engelberth.http.HttpRequestListener;
import io.engelberth.http.Server;
import io.engelberth.http.HttpHeader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
public class FileServerListener implements HttpRequestListener {
	private File mRoot = null;
	private String mIndexFile = null;
	private String index;
	
	public static String cleanRequest(String request) {
		return request.replace("..", "").replace("./", "/").replace(";", "");
	}
	public FileServerListener(File root, String indexFile) throws IOException {
		mRoot = root;
		mIndexFile = indexFile;
		loadIndexFile();
	}
	private void loadIndexFile() throws IOException {
		index = new String(Files.readAllBytes(Paths.get(mIndexFile)));
	}
	public void handleRequest(Server.Connection connection, HttpHeader header) throws IOException {
		header.request = cleanRequest(header.request);
		if (header.request.equals("/")) {
			connection.send("text/html", index);
			return;
		}
		File requestedFile = new File(mRoot, header.request);
		if (requestedFile.exists() == false) {
			connection.send404("File does not exist");
		} else if (requestedFile.isDirectory()) {
			File[] files = requestedFile.listFiles();
			String fileList = new String();
			for (int i = 0; i < files.length; i++) {
				if (files[i].isDirectory()) {
					fileList = fileList + "<a href=\"" + header.request + files[i].getName() + "/\">" + files[i].getName() + "/</a><br/>";
				} else {
					fileList = fileList + "<a href=\"" + header.request + files[i].getName() + "\">" + files[i].getName() + "</a><br/>";
				}
			}
			connection.send("text/html", "<!doctype html><html><body><h1>Directory: " + header.request + "</h1>" + fileList + "</body></html>");
				
		} else {
			connection.send(requestedFile);
		}
	}
}
