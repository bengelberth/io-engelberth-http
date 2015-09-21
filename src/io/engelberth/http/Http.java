package io.engelberth.http;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.SSLSocket;
/**
 * This class is used to make HTTP connections as a client.
 * @author brandon
 *
 */

public class Http {
	public static final int VERSION = 7;
	public static final String USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Ubuntu Chromium/34.0.1847.116 Chrome/34.0.1847.116 Safari/537.36";
	public static final String HTTP_NEW_LINE = "\r\n";
	public static final String CONTENT_LENGTH_KEY = "Content-Length:";
	public static final String CONTENT_ENCODING_KEY = "Content-Encoding:";
	public static final String LOCATION_KEY = "Location:";
	public static final int PRINT_SILENT = 1;	// Print nothing
	public static final int PRINT_ERROR = 2;	// Print errors
	public static final int PRINT_NORMAL = 3;	// Print normal information
	public static final int PRINT_LOUD = 4;// Print everything
	public static final int READ_TIMEOUT = 20000;	// milliseconds
	public static final int BUFFER_SIZE = 1024;
	public static final int DEFAULT_SSL_CONNECT_TIMEOUT = 20;	// Seconds
	public static final int DEFAULT_CONCURRENT_CONNECTIONS = 2;
	
	
	//private header_struct mHeader = null;
	//Options
	private boolean mFollowRedirect;
	private int mVerbose;
	//private HashMap<String, String> mCookies;
	private String mCookies;
	private HashMap<String, String> mCookieMap;
	private Proxy mProxy = null;
	private boolean mStrictSSL;
	private int mSSLConnectTimeout;
	private int mConnections;
	private int mMaxConnections;
	//private boolean mUseSSL;
	//private String mPreviousUrl = "";
	/**
	 * Set the defaults
	 */
	public Http() {
		mConnections = 0;
		mMaxConnections = DEFAULT_CONCURRENT_CONNECTIONS;
		mFollowRedirect = false;
		mVerbose = PRINT_ERROR;
		mCookieMap = new HashMap<String, String>();
		//mUseSSL = true;
		setStrictSSL(true);
		setSSLConnectTimeout(DEFAULT_SSL_CONNECT_TIMEOUT);
	}
	public void setConcurrentConnections(int c) {
		mMaxConnections = c;
	}
	public void setSSLConnectTimeout(int timeout) {
		if (timeout < 1) mSSLConnectTimeout = DEFAULT_SSL_CONNECT_TIMEOUT;
		else mSSLConnectTimeout = timeout;
	}
	public void setStrictSSL(boolean s) {
		mStrictSSL = s;
	}
	public void setCookie(String name, String value) {
		mCookies = "";
		mCookieMap.put(name, value);
		for (Map.Entry<String, String> entry : mCookieMap.entrySet()) {
			mCookies = mCookies + " " + entry.getKey() + "=" + entry.getValue() + ";";
		}
	}
	public void followRedirect(boolean f) { mFollowRedirect = f; }
	
	public void setVerbose(int v) {
		mVerbose = v;
		if (v < PRINT_SILENT) mVerbose = 1;
		if (v > PRINT_LOUD) mVerbose = PRINT_LOUD;
	}
	public void setProxy(Proxy proxy) {
		mProxy = proxy;
	}
	public Proxy getProxy() { return mProxy; }
	/*
	public int getStatus() {
		if (mHeader == null) return -1;
		return mHeader.status;
	}
	*/
	protected void println(int v, String s) {
		if (v <= mVerbose) {
			SimpleDateFormat sdf = new SimpleDateFormat("MMddyyyy HH:mm:ss");
			StackTraceElement[] st = Thread.currentThread().getStackTrace();
			
			System.out.println(sdf.format(new Date()) + "(Http." + st[2].getMethodName() + ":" + st[2].getLineNumber() + "): " + s);
		}
	}
	// This should be overriden if you want to look up hostnames a special way
	// if using a proxy.... like tor-resolve
	protected String getProxyHostname(String hostname) {
		return hostname;
	}
	private void deleteTempFile(File f) {
		if (!f.delete()) println(PRINT_ERROR, "error deleting temp file: " + f.getAbsolutePath());
	}
	// For chunked.
	private void readGzip(InputStream input, OutputStream output, int length, File chunked) throws IOException {
		//System.out.println("readGzip()CHUNK");
		int l;
		byte[] buffer = new byte[BUFFER_SIZE];
		int t =0;
		int readAmount = BUFFER_SIZE;
		if (BUFFER_SIZE > length) readAmount = length;
		//File tempGzipBuffer = File.createTempFile("gzip", "buffer");
		if (t < length) {
			try (FileOutputStream gzipOutBuffer = new FileOutputStream(chunked, true)) {
				while (t < length && (l = input.read(buffer, 0, readAmount)) > -1) {
					//System.out.println("pass[in]:  t=" + t + ", l=" + l + ", readAmount=" + readAmount);
					t = t + l;
					gzipOutBuffer.write(buffer, 0, l);	
					readAmount = BUFFER_SIZE;
					if (BUFFER_SIZE > length - t) readAmount = length - t;
					//System.out.println("pass[out]: t=" + t + ", l=" + l + ", readAmount=" + readAmount);
				}
				gzipOutBuffer.flush();
			}
		}
		if (length == 0) {
			try (GZIPInputStream gzip = new GZIPInputStream(new FileInputStream(chunked))) {
				t = 0;
				while ((l = gzip.read(buffer)) > -1) {
					t = t + l;
					output.write(buffer, 0, l);			
				}
			}
			//deleteTempFile(chunked);
		}

	}
	// Read length amount of gzipped data from input and write the uncompressed data to output
	// TODO - utilize DiskBufferedInputStream
	private void readGzip(InputStream input, OutputStream output, int length) throws IOException {
		int l;
		byte[] buffer = new byte[BUFFER_SIZE];
		int t =0;
		File tempGzipBuffer = File.createTempFile("gzip", "buffer");
		try {
			try (FileOutputStream gzipOutBuffer = new FileOutputStream(tempGzipBuffer)) {
				while (t < length && (l = input.read(buffer)) > -1) {
					t = t + l;
					gzipOutBuffer.write(buffer, 0, l);			
				}
			}
			
			try (GZIPInputStream gzip = new GZIPInputStream(new FileInputStream(tempGzipBuffer))) {
				t = 0;
				while ((l = gzip.read(buffer)) > -1) {
					t = t + l;
					output.write(buffer, 0, l);			
				}
			}
		} catch (Exception e) {
			throw e;
		} finally {
			deleteTempFile(tempGzipBuffer);
		}

		
	}
	protected void get(OutputStream stream, String strUrl) throws IOException, StatusException {
		get(stream, strUrl, true);
	}
	protected void get(OutputStream stream, String strUrl, boolean ssl) throws IOException, StatusException {
		println(PRINT_LOUD, "mConnections=" + mConnections + ", mMaxConnections=" + mMaxConnections);

		while (mConnections >= mMaxConnections) {
			try {
				Thread.sleep(2500);
			} catch (InterruptedException e) {
				
			}
		}
		mConnections++;
		try {
			getNoWait(stream, strUrl, ssl);
		} catch (Throwable e) {
			throw e;
		} finally {
			mConnections--;
		}
	}
	protected void getNoWait(OutputStream stream, String strUrl, boolean ssl) throws IOException, StatusException {
		url_struct url = parseUrl(strUrl.replace("http://",  "").replace("https://", ""));
		if (url.hostname.equals("")) throw new MalformedURLException("hostname is null or empty: " + url.hostname);
		if (ssl) url.port = 443;
		Socket httpSocket = null, httpsSocket = null;
		
		InetSocketAddress address;
		println(PRINT_LOUD, "Set URL to: " + url.hostname);
		if (mProxy == null) {
			httpSocket = new Socket();
			address = new InetSocketAddress(url.hostname, url.port);
		} else {
			String ip = getProxyHostname(url.hostname);
			if (ip == null || ip.equals("")) throw new IOException("Proxy: Error resolving hostname: " + url.hostname);
		
			httpSocket = new Socket(mProxy);
			address = new InetSocketAddress(ip, url.port);
			//socket.connect(new InetSocketAddress(ip, url.port));
		}
		
		if (ssl) {
			try {
				httpSocket.connect(address, mSSLConnectTimeout * 1000);
				httpsSocket = ((SSLSocketFactory)SSLSocketFactory.getDefault()).createSocket(httpSocket, address.getHostString(), 443, true);
				((SSLSocket)httpsSocket).startHandshake();
			} catch (IOException e) {
				//println(PRINT_ERROR, "SSL failed: falling back to clear text for: " +url.hostname);
				if (httpsSocket != null) httpsSocket.close();
				httpSocket.close();
				//httpSocket.close();
				httpsSocket = null;
				if (mStrictSSL) {
					//println(PRINT_ERROR, "SSL Strict is on: Failed to make ssl connection to: " + url.hostname);
					
					throw new IOException("SSL Strict is on: Failed to make ssl connection to: " + url.hostname, e);
				} else {
					println(PRINT_NORMAL, "SSL failed: falling back to clear text for: " +url.hostname);
					getNoWait(stream, strUrl, false);
				}
				//socket.close();
				return;
			}
		} else
			httpSocket.connect(address);
		Socket socket;
		if (httpsSocket != null) socket = httpsSocket;
		else socket = httpSocket;
		//socket.setSoTimeout(20 * 1000);
		socket.setSoTimeout(READ_TIMEOUT);
		
		try (	BufferedInputStream in = new BufferedInputStream(socket.getInputStream());
				BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream())) {
		
			byte[] request = ("GET " + url.file + " HTTP/1.1" + HTTP_NEW_LINE +
							"Host: " + url.hostname + (url.port==80||url.port==443?"":(":" + url.port)) + HTTP_NEW_LINE +
							"Accept-Encoding: gzip" + HTTP_NEW_LINE +
							"User-Agent: " + USER_AGENT + HTTP_NEW_LINE 
							(mCookies==null?"":"Cookie:" + mCookies + HTTP_NEW_LINE) +
							HTTP_NEW_LINE).getBytes();
			out.write(request);
			out.flush();
			println(PRINT_LOUD, "Flushed header request to pipe");
			println(PRINT_LOUD, "out header\n" + new String(request));
			StringBuilder headerRaw = new StringBuilder();
			int buf;
			
			while ((buf = in.read()) > -1) {
				headerRaw.append((char)buf);
				if (headerRaw.indexOf(HTTP_NEW_LINE + HTTP_NEW_LINE) > -1)
					break;
			}
			println(PRINT_LOUD, "raw header\n" + headerRaw);
			header_struct header = parseHeader(headerRaw.toString());
			
			if (header == null) {
				in.close();out.close();socket.close();
				throw new IOException("Bad HTTP Header");
			}
			println(PRINT_LOUD, "Received Header (Status: " + header.status + " Length: " + header.length + " gzip: " + header.gzip);
	
			if (header.status != 200) {
				if ((header.status == 302 || header.status == 301) && mFollowRedirect) {
					try {
						String location = header.header.substring(header.header.toLowerCase().indexOf(LOCATION_KEY.toLowerCase()));
						location = location.substring(LOCATION_KEY.length());
						location = location.substring(0, location.indexOf(HTTP_NEW_LINE)).trim();
						location = location.replace("http://", "");
						/*if (location.contains("https://")) {	// Don't follow ssl redirect
							println(PRINT_ERROR, "Not following redirect to ssl: " + location);
						} else*/ if (location.equals(strUrl)) {
							println(PRINT_ERROR, "Not following to self: " + location);
						} else {
							println(PRINT_NORMAL, "Following Redirect: " + location);
							getNoWait(stream, location, ssl);
							return;
						}
					} catch (StringIndexOutOfBoundsException e) {
						println(PRINT_ERROR, "Error in HTTP header while trying to follow redirect");
					} finally {
						in.close();
						out.close();
						socket.close();
						httpSocket.close();
					}
				}
				println(PRINT_ERROR, "Error: Http Status: " + header.status + ": " + strUrl);
				in.close();
				out.close();
				socket.close();
				httpSocket.close();
				throw new StatusException(header.status);
			}
			
			//byte[] content;
			
			//GZIPInputStream gzip = null;
			//if (mHeader.gzip) gzip = new GZIPInputStream(in);
			if (header.length < 0) { // chuncked assume string
				println(PRINT_LOUD, "Incoming data is chunked");
				//String strContent = new String();
				//ByteArrayOutputStream contentStream = new ByteArrayOutputStream();
				
				//byte[] chunk;
				
				File tempChunk = File.createTempFile("chunked", "gzip");
				try {
					while (readChunk(header, stream, in, tempChunk)) { }
				} catch (Exception e) {
					in.close();
					out.close();
					socket.close();
					httpSocket.close();
					throw e;
				} finally {
					deleteTempFile(tempChunk);	// Clean up our mess when things go wrong!
				}
				//content = contentStream.toByteArray();
				//contentStream.close();
			} else {
				println(PRINT_LOUD, "Incoming data is: " + header.length + " bytes");
				int l;
				int total = 0;
				if (header.gzip == false) {
					byte[] pageBuf = new byte[BUFFER_SIZE];
					while (total < header.length && (l = in.read(pageBuf, 0, BUFFER_SIZE)) > -1) {
						total = total + l;
						stream.write(pageBuf, 0, l);
						stream.flush(); // TODO debug
					}
				} else
					readGzip(in, stream, header.length);
				//content = pageBuf;
			}
			//if (gzip != null) gzip.close();
			//out.close();
			//in.close();
		} catch (Exception e) {
			throw e;
		} finally {
			socket.close();
			httpSocket.close();
		}
	
	}
	public void get(File file, String strUrl) throws IOException, StatusException {
		try (FileOutputStream out = new FileOutputStream(file)) {
			get(out, strUrl);
		}
	}
	// example: www.google.com/hello_world
	// NOT: http://www.google.com/hello_world
	public byte[]get(String strUrl) throws IOException, StatusException {
		try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			get(out, strUrl);
			byte[] r = out.toByteArray();
			return r;
		}
	}
	private boolean readChunk(header_struct header, OutputStream out, BufferedInputStream in, File chunkBuffer) throws IOException {
		StringBuilder hexLength = new StringBuilder();
		int buf;
		
		//String chunk = new String();
		while ((buf = in.read()) > -1) {
			hexLength.append((char)buf);
			if (hexLength.indexOf(HTTP_NEW_LINE) > -1) break;
		}
		int size = 0;
		try {
			size = Integer.parseInt(hexLength.toString().trim(), 16);
		} catch (NumberFormatException e) {
			println(PRINT_ERROR, "Chunk error: size=0, buf=" + buf + ", '" + hexLength + "'");
		}
		if (size == 0) {
			if (header.gzip) readGzip(in, out, 0, chunkBuffer);	// Flushes the buffer to the stream
			return false; // Last chunk
		}
		println(PRINT_LOUD, "Chunk size: " + size);
		//ByteArrayOutputStream chunk = new ByteArrayOutputStream();
		if (header.gzip) readGzip(in, out, size, chunkBuffer);
		else {
			for (int i = 0; i < size; i++) {
				out.write(in.read());
			}
		}
		in.read();
		in.read();
		//byte[] r = chunk.toByteArray();
		//chunk.close();
		return true;
	}
	private static header_struct parseHeader(String header) {
		header_struct h = new header_struct();
		h.header = header;
		String status = header.replace("HTTP/", "").replace("1.1",  "").replace("1.0", "");
		if (status.length() < 4) return null;
		status = status.substring(1, 4);
		try {
			h.status = Integer.parseInt(status);
		} catch (NumberFormatException e) {
			return null;
		}
		try {
			String length = header.substring(header.toLowerCase().indexOf(CONTENT_LENGTH_KEY.toLowerCase()));
			length = length.substring(CONTENT_LENGTH_KEY.length());
			length = length.substring(0, length.indexOf(HTTP_NEW_LINE));
			h.length = Integer.parseInt(length.trim());
		} catch (StringIndexOutOfBoundsException e) {
			h.length = -1; // chunked
		}
		// Check to see if the stream is compressed.
		h.gzip = false;
		//h.deflate = false;
		try {
			String encoding = header.substring(header.toLowerCase().indexOf(CONTENT_ENCODING_KEY.toLowerCase()));
			encoding = encoding.substring(CONTENT_ENCODING_KEY.length());
			encoding = encoding.substring(0, encoding.indexOf(HTTP_NEW_LINE));
			encoding = encoding.toLowerCase().trim();
			if (encoding.equals("gzip"))
				h.gzip = true;
			//else if (encoding.equals("deflate"))
			//	h.deflate = true;
				
		} catch (StringIndexOutOfBoundsException e) {
			//h.gzip = false;
		}
		return h;
		
	}
	// Not a safe function
	public static url_struct parseUrl(String a) {
		int defaultPort = 80;
		if (a.contains("https://")) defaultPort = 443;
		String url = a.replace("http://", "").replace("https://", "");
		url_struct u = new url_struct();
		u.firstColon = url.indexOf(":");
		u.firstForwardSlash = url.indexOf("/");
		if (u.firstColon < u.firstForwardSlash && u.firstColon > -1) { // Port number
			u.hostname = url.substring(0, u.firstColon);
			try {
				u.port = Integer.parseInt(url.substring(u.firstColon+1, u.firstForwardSlash));
			} catch (NumberFormatException e) {
				System.err.println(e.toString());
				u.port = defaultPort;
			}
		} else {
			if (u.firstForwardSlash > -1)
				u.hostname = url.substring(0, u.firstForwardSlash);
			else
				u.hostname = url;
			u.port = defaultPort;
		}
		if (u.firstForwardSlash > -1)
			u.file = url.substring(u.firstForwardSlash);
		else
			u.file = "/";
		return u;
	}
	public static class url_struct {
		public int firstColon;
		public int firstForwardSlash;
		public String hostname;
		public int port;
		public String file;
		public String toString() {
			return "Hostname: " + hostname + "\n" +
					"Port: " + port + "\n" +
					"File: " + file;
		}
	}
	private static class header_struct {
		public String header;
		public int status;
		public int length;
		public boolean gzip;
	}
	
}
