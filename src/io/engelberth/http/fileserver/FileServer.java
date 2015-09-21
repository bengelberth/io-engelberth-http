package io.engelberth.http.fileserver;

import io.engelberth.http.Print;
import io.engelberth.http.Server;
import java.io.File;

public class FileServer {
	public static void main(String[] args) throws Exception {
		Print p = new Print();
		if (args.length != 2) {
			p.errorln("Must give root dir and index file on command line");
			return;
		}
		
		Server server = new Server(8080);
		server.p.setVerbose(Print.DEBUG);
		server.setHttpRequestListener(new FileServerListener(new File(args[0]), args[1]));
		server.start();
		
	}
}
