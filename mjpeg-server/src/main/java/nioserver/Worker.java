package nioserver;

import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import mjpeg.framesource.JPEGFrameSource;

public class Worker {
	private static final Map<String, List<byte[]>> frameCache = 
			new HashMap<String, List<byte[]>>();
	
	private State state = State.READ_HEADER;
	private SocketChannel server;
	private ByteBuffer readBuffer = ByteBuffer.allocate(100);
	private RequestParser p = new RequestParser();
	private ByteBuffer frameBuffer;
	private Iterator<byte[]> it;
	private String filename;
	private long timeWait = 0;
	private String fps;
	
	public enum ChangeOp {
		READ,
		WRITE,
		CLOSE
	}
	
	private enum State {
		READ_HEADER,
		WRITE_HEADER,
		WRITE_BODY
	}
	
	public Worker(SocketChannel server) {
		this.server = server;
	}
	
	public ChangeOp read() throws IOException {
		readBuffer.clear();					
		
		int c;
		while ((c = server.read(readBuffer)) > 0) {
			readBuffer.flip();
			for (int i = 0; i < c; ++i) {
				if (!p.feed(readBuffer.get(i))) {
					Request request = p.getRequest();
					filename = "." + request.getResource();
					fps = request.getParam("fps");
					
					prepareWrite();
					state = State.WRITE_HEADER;
					return ChangeOp.WRITE;
				}
			}
			readBuffer.clear();
		}
		
		if (c == -1) {
			throw new EOFException();
		}
		
		return ChangeOp.READ;
	}
	
	private synchronized void prepareWrite() throws IOException {
		if (!frameCache.containsKey(filename)) {
			FileInputStream fis = new FileInputStream(filename);
			JPEGFrameSource frameSource = new JPEGFrameSource(fis);
			
			List<byte[]> images = new ArrayList<byte[]>();
			try {
				while (true) {
					images.add(frameSource.nextImage());
				}
			} catch (EOFException ex) {
				// 
			}
			
			frameCache.put(filename, Collections.unmodifiableList(images));
		}
		
		it = frameCache.get(filename).iterator();
	}

	public ChangeOp write() throws IOException {
		if (state == State.WRITE_HEADER) {
			String message = 
				"HTTP/1.1 200\r\n" +
				"Content-type: multipart/x-mixed-replace; boundary=\"myboundary\"\r\n" +
				"\r\n";
			
			readBuffer.clear();
			readBuffer.put(message.getBytes());
			readBuffer.flip();
			server.write(readBuffer);
			
			state = State.WRITE_BODY;
		} else {
			if (frameBuffer == null || !frameBuffer.hasRemaining()) {
				if (!it.hasNext()) {
					prepareWrite();
				}
				
				byte[] image = it.next();
				
				int desiredCapacity = 1024 + image.length;
				if (frameBuffer == null || frameBuffer.capacity() < desiredCapacity) {
					frameBuffer = ByteBuffer.allocate(desiredCapacity);
				} else {
					frameBuffer.clear();
				}
				
				frameBuffer.put("--myboundary\r\n".getBytes());
				frameBuffer.put("Content-Type: image/jpeg\r\n".getBytes());
				frameBuffer.put(("Content-Length: " + image.length + "\r\n").getBytes());
				frameBuffer.put("\r\n".getBytes());
				
				frameBuffer.put(image);
				frameBuffer.put("\r\n".getBytes());
				frameBuffer.flip();
			}
			
			server.write(frameBuffer);
			
			if (!frameBuffer.hasRemaining()) {
				if (fps != null && !fps.equals("0")) {
					timeWait = System.currentTimeMillis() + (long) (1000.0f / Float.valueOf(fps));
				}
			}
		}
		
		return ChangeOp.WRITE;
	}
	
	public long timeWait() {
		return timeWait;
	}
}

