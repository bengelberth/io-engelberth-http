package io.engelberth.http;

public class HttpHeader {
	public static final int TYPE_GET = 1;
	public static final int TYPE_POST = 2;
	public static final int TYPE_HEAD = 3;
	
	public int type = -1;
	public String request = null;
	public String userAgent = null;
	public boolean range = false;
}
