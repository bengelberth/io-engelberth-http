package io.engelberth.http;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.NetworkInterface;
import java.net.InetAddress;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.GZIPOutputStream;


public class Server {
	public static final int VERSION = 7;
	
	public static final int MAX_HEADER_READ = 1024 * 5;
	public static final int MAX_CONNECTIONS = 25;
	public static final int READ_TIMEOUT = 2500;	// milliseconds
	public static final int OUTPUT_BUFFER_SIZE = 3000;
	public static final int MINIMUM_COMPRESS_SIZE = 150;	// Smallest amount of data to compress
	public static final int REQUEST_BUFFER = 10485760;	// 10 MB (Buffer size for compressed output
	//public static final int LIMIT = 5000;
	//public static Random rand = new Random();
	private ServerRunnable mServer;
	private int mPort;
	private HttpRequestListener mListener;
	public Print p;
	private int mPoolSize;
	
	public Server(int port) {
		this(port, MAX_CONNECTIONS);
		//mDefaultPage = defaultPage;
	}
	
	public Server(int port, int poolSize) {
		mPoolSize = poolSize;
		p = new Print(Print.ERROR);
		mPort = port;
		setHttpRequestListener(null);
	}
	public void setHttpRequestListener(HttpRequestListener listener) {
		if (listener == null) {
			mListener = new HttpRequestListener() {
				@Override
				public void handleRequest(Connection connection, HttpHeader request) throws IOException {
						
				} 
			};
		} else
			mListener = listener;
	}
	
	public HttpRequestListener getHttpRequestListener() { 
		return mListener;
	}
	public void start() {
		if (mServer != null) return;
		mServer = new ServerRunnable(this, mPort, mPoolSize);
		Thread t = new Thread(mServer);
		t.start();
	}
	
	public void stop() throws IOException {
		mServer.stop();
	}
	
	public static class Connection implements Runnable {
		private Socket mSocket;
		private Server mServer;
		private InputStream mInput;
		private OutputStream mOutput;
		private boolean mCompress;
		private String mIp;
		public Connection(Server server, Socket socket) {
			mSocket = socket;
			mServer = server;
			mInput = null;
			mOutput = null;
			
		}
		public Server getServer() { return mServer; }
		public String getIp() { return mSocket.getInetAddress().getHostAddress(); }
		//public byte[] getMac() throws IOException { return NetworkInterface.getByInetAddress(InetAddress.getByName(getIp())).getHardwareAddress(); }
        public Socket getSocket() { return mSocket; }
		public InputStream getInputStream() {
        		return mInput;
        }
        public OutputStream getOutputStream() {
        		return mOutput;
        }
        private void readAndHandleRequest() throws IOException {
        		HttpHeader httpHeader = new HttpHeader();
				
				
            		// Read header
                int d;
                int t =0;
                String request = "";
                while ((d = mInput.read()) > -1) {
                		t++;
                    request += ((char)d);
                    if (t > MAX_HEADER_READ) {
                    	mServer.p.errorln("Max header size(" + MAX_HEADER_READ + ") reached from: " + mIp);
						break;
					}
                    if (request.lastIndexOf(Http.HTTP_NEW_LINE + Http.HTTP_NEW_LINE, request.length()) > -1) break;
                }
                
                // Figure out type
                if (request.substring(0, 3).equals("GET")) {
                		httpHeader.type = HttpHeader.TYPE_GET;
                	}
                	
				mServer.p.debugln("Socket=" + mSocket + ", header=" + request);
				//if (l == -1) System.out.println("l = -1, " + t);
				// Parse requested file
				mCompress = request.contains("gzip");
				int index = request.indexOf("/");
				//String requestedFile = "/";
				httpHeader.request = new String("/");
				if (index > -1) {
					httpHeader.request = request.substring(index).trim();
					httpHeader.request = httpHeader.request.substring(0, httpHeader.request.indexOf(" "));
				} else
					mServer.p.errorln("ERROR: " + request);
				mServer.p.println("(" + mIp + "): Request for: " + httpHeader.request);
				// Run callback
				mServer.getHttpRequestListener().handleRequest(this, httpHeader);
				/*
				if (action == null) return;
				if (action.getFile() != null)
					sendFile(action.getFile());
				else
					sendResponse(action.getType(), action.getContent());
				*/
				// Check header if "close"
				// if close then return.
			readAndHandleRequest();
        }
		@Override
		public void run() {
			mIp = mSocket.getInetAddress().getHostAddress();
			//println("New Connection from: " +ip);
			//BufferedInputStream in = null;
			//BufferedOutputStream out = null;
			mServer.p.debugln("Socket=" + mSocket);
			try {
				mSocket.setSoTimeout(READ_TIMEOUT);
				mInput = new BufferedInputStream(mSocket.getInputStream());
				mOutput = new BufferedOutputStream(mSocket.getOutputStream());
				readAndHandleRequest();
				
			} catch (SocketTimeoutException e) {
				mServer.p.errorln("Socket=" + mSocket + ", Connection timeout error");
			} catch (IOException e) {
				mServer.p.errorln(e.toString());
				e.printStackTrace();
			} finally {
				try {
					if (mInput != null) mInput.close();
				} catch (IOException e) { }
				try {
					if (mOutput != null) mOutput.close();
				} catch (IOException e) { }
				try {
					mSocket.close();
				} catch (IOException e) { }
			}
		}

		public void send(File file) throws IOException {
			mServer.p.println("Sending file: " + file.getAbsolutePath());
			if (!file.exists()) {
				sendError("File does not exists");
				throw new IOException(file.getAbsolutePath() + " does not exist");
			}

			String extension = "";
			int index = file.getName().lastIndexOf(".");
			if (index > -1)
				extension = file.getName().substring(index+1);
			String dataType = "";
			if (extension.equals("jpg") || extension.equals("jpeg"))
				dataType = "image/jpeg";
			else if (extension.equals("html"))
				dataType = "text/html";
			else if (extension.equals("png"))
				dataType = "image/png";
			else if (extension.equals("gif"))
				dataType = "image/gif";
			else if (extension.equals("json"))
				dataType = "application/json";
			else if (extension.equals("js"))
				dataType = "application/javascript";
			else if (extension.equals("css"))
				dataType = "text/css";
			else if (extension.equals("html"))
				dataType = "text/html";
			else if (extension.equals("mp4"))
				dataType = "video/mp4";
			try (FileInputStream in = new FileInputStream(file)) {
				send(dataType, in, (int)file.length());
			} catch (IOException e) {
				throw e;
			}
			
		}

		public void sendJSON(String json) throws IOException {
			send("application/json", json);
		}
		public void send(String type, String data) throws IOException {
			try (ByteArrayInputStream in = new ByteArrayInputStream(data.getBytes())) {
				send(type, in, data.length());
			}
		}
		public void send(String type, InputStream in, int size) throws IOException {
			send(type, in, size, 200, "OK");
		}
		public void send(String type, InputStream in, int size, int status, String mesg) throws IOException {
			if (size < 0) throw new IOException("Chunked sending not supported");
			boolean compress = (REQUEST_BUFFER > size && mCompress && size > MINIMUM_COMPRESS_SIZE && (type.contains("json") || type.contains("text"))?true:false);
			byte[] compressed = null;
			byte[] buffer;
			if (size < OUTPUT_BUFFER_SIZE) buffer = new byte[size];
			else buffer = new byte[OUTPUT_BUFFER_SIZE];
			if (compress) {
				try (	ByteArrayOutputStream compressedStream = new ByteArrayOutputStream();
						GZIPOutputStream gzipOut = new GZIPOutputStream(compressedStream)) {
					int l;
					int t = 0;
					
					// TODO this should be reworked
					while ((l = in.read(buffer)) > -1 && t < size) {
						t = t + l;
						gzipOut.write(buffer, 0, l);
					}
					gzipOut.finish();
					compressed = compressedStream.toByteArray();
					size = compressed.length;
				}
			
			}
			mOutput.write(("HTTP/1.1 " + status + " " + mesg + Http.HTTP_NEW_LINE + 
					"Connection: close" + Http.HTTP_NEW_LINE + 
					"Content-Type: " + type + Http.HTTP_NEW_LINE +
					"Content-Length: " + size + Http.HTTP_NEW_LINE +
					(compress?"Content-Encoding: gzip" + Http.HTTP_NEW_LINE:"") +
					Http.HTTP_NEW_LINE).getBytes());
			if (compress)
				mOutput.write(compressed);
			else {
				int l;
				int t = 0;
				// TODO Confirm this works....
				mServer.p.debugln("Socket=" + mSocket + ", size=" + size);
				while (t < size && (l = in.read(buffer, 0, size - t > buffer.length?buffer.length:size - t)) > -1) {
					t = t + l;
					if (l > 0)
						mOutput.write(buffer, 0, l);
				}
			}
			//if (out instanceof GZIPOutputStream) ((GZIPOutputStream)out).finish();
			mOutput.flush();
			
		}
		/*
		private void sendResponse(String type, byte[] data) throws IOException {
			mOutput.write(("HTTP/1.1 200 OK" + Http.HTTP_NEW_LINE + 
							"Content-Type: " + type + Http.HTTP_NEW_LINE +
							"Content-Length: " + data.length + Http.HTTP_NEW_LINE +
							Http.HTTP_NEW_LINE).getBytes());
			mOutput.write(data);
			mOutput.flush();
		}
		*/
		// TODO implement
		public void send204() throws IOException {
			
		}
		public void send404(String mesg) throws IOException {
			try (InputStream in = new ByteArrayInputStream(mesg.getBytes())) {
				send("text/plain", in, mesg.length(), 404, "Not Found");
			}
		}
		public void sendError(String mesg) throws IOException {
			send404(mesg);
		}
	}
	
	private static class ServerRunnable implements Runnable {
		private int mPort;
		private ServerSocket mServerSocket;
		private boolean mAlive;
		private Server mServer;
		private int mPoolSize;
		//private Print p;
		public ServerRunnable(Server server, int port, int poolSize) {
			mPort = port;
			mAlive = true;
			mServer = server;
			mPoolSize = poolSize;
		}
		
		public void stop() throws IOException {
			mAlive = false;
			mServerSocket.close();
		}
		
		@Override
		public void run() {
			mServer.p.println("Server started: version=" + VERSION + ", port=" + mPort + ", pool=" + mPoolSize);
			//Vector<Thread> pool = new Vector<Thread>();
			ExecutorService threadPool = Executors.newFixedThreadPool(mPoolSize);
			try {
				mServerSocket = new ServerSocket(mPort);
				//Thread t;
				while (mAlive) {
					try {	//TODO why are we sleeping for 5 milli?
						Thread.sleep(5);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
					try {
						Socket socket = mServerSocket.accept();
						threadPool.execute(new Connection(mServer, socket));
						//t.start();
						
						//pool.add(t);
						/*
						while (pool.size() > MAX_CONNECTIONS) {
							mServer.p.println("Pool size is > max " + MAX_CONNECTIONS + ".  cleaning...");
							for (Iterator<Thread> iterator = pool.iterator(); iterator.hasNext();) {
								t = iterator.next();
								if (!t.isAlive()) iterator.remove();
							}
							mServer.p.println("Pool size is now: " + pool.size());
							if (pool.size() > MAX_CONNECTIONS)
								try {
									mServer.p.errorln("Wow!  Must be busy.... sleeping for a second");
									Thread.sleep(1000);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
						}
						*/
					} catch (SocketException e) {
						mServer.p.errorln(e.toString());
					}
					
					
				}
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				mServer.p.println("Exiting");
				threadPool.shutdown();
			}
		}
		
	}
}
