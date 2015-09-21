package io.engelberth.http;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * A class to assist in printing information to screen in a nice format
 */
public class Print {
	public static final int VERSION = 1;
	public static final int SILENT = 1;	// Print nothing
	public static final int ERROR = 2;	// Print errors
	public static final int NORMAL = 3;	// Print normal information
	public static final int LOUD = 4;// Print everything
	public static final int DEBUG = LOUD;
	private int mVerbose;
	
	public Print() {
		this(ERROR);
	}
	public Print(int v) {
		setVerbose(v);
	}
	
	public void setVerbose(int v) {
		if (v < SILENT) mVerbose = SILENT;
		else if (v > LOUD) mVerbose = LOUD;
		else mVerbose = v;
	}
  // Print a line of text
  // Set the stack to the line of code where this method is called.
	public void println(int v, String s) {
		println(3, v, s);
	}
  // Only useful if you want to push the stack back beyound where you are calling.
	public void println(int stack, int v, String s) {
		if (v <= mVerbose) {
			SimpleDateFormat sdf = new SimpleDateFormat("MMddyyyy HH:mm:ss");
			StackTraceElement[] st = Thread.currentThread().getStackTrace();
			
			System.out.println(sdf.format(new Date()) + "(" + st[stack].getClassName() + "." + st[stack].getMethodName() + ":" + st[stack].getLineNumber() + "): " + s);
		}
	}

	// Simplification methods
	public void println(String s) {
		println(3, NORMAL, s);
	}
	public void errorln(String s) {
		println(3, ERROR, s);
	}
	public void silenceln(String s) {
		println(3, SILENT, s);
	}
	public void debugln(String s) {
		println(3, LOUD, s);
	}
}
