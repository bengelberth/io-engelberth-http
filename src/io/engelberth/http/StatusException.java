package io.engelberth.http;

public class StatusException extends Exception {

	private static final long serialVersionUID = 1L;
	
	private int mStatus = -1;
	
	private StatusException() { super(); }
	
	public StatusException(int status) {
		this();
		mStatus = status;
	}
	
	public int getStatus() { return mStatus; }
}
