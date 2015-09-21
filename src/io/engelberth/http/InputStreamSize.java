package io.engelberth.http;

import java.io.InputStream;

public class InputStreamSize  {
	private int size;
	private InputStream input;
	
	public InputStreamSize(InputStream in, int s) {
		size = s;
		input = in;
	}
	
	public int size() { return size; }
	public InputStream inputStream() { return input; }

}
