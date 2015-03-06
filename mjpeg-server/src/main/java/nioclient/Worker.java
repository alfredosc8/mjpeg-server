package nioclient;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ConcurrentHashMap;

import mjpeg.framesource.JPEGFrameSource;

public class Worker {
	private SocketChannel server;
	private ByteBuffer buffer = ByteBuffer.allocate(100);
	private RequestParser p = new RequestParser();
	private Request request;
	private FileInputStream fis;
	private JPEGFrameSource frameSource;
	
	private static final ConcurrentHashMap<String, byte[]> fileCache = 
		new ConcurrentHashMap<String, byte[]>();
	
	private State state = State.READ_HEADER;

	private ByteBuffer frameBuffer;
	
	public enum ChangeOp {
		READ,
		WRITE,
		CLOSE
	}
	
	private enum State {
		READ_HEADER,
		WRITE_HEADER,
		WRITE
	}
	
	public Worker(SocketChannel server) {
		this.server = server;
	}
	
	public ChangeOp read() throws IOException {
		buffer.clear();					
		
		int c;
		while ((c = server.read(buffer)) > 0) {
			buffer.flip();
			for (int i = 0; i < c; ++i) {
				if (!p.feed(buffer.get(i))) {
					request = p.getRequest();
					prepareWrite();
					state = State.WRITE_HEADER;
					return ChangeOp.WRITE;
				}
			}
			buffer.clear();
		}
		
		return ChangeOp.READ;
	}
	
	private void prepareWrite() throws IOException {
		String filename = "." + request.getResource();
		if (!fileCache.containsKey(filename)) {
			fis = new FileInputStream(filename);
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			copy(fis, bos);
			
			fileCache.put(filename, bos.toByteArray());
		}
		
		frameSource = new JPEGFrameSource(new ByteArrayInputStream(fileCache.get(filename)));
	}

	public static long copy(InputStream from, OutputStream to)
			throws IOException {
		byte[] buf = new byte[0x1024];
		long total = 0;
		while (true) {
			int r = from.read(buf);
			if (r == -1) {
				break;
			}
			to.write(buf, 0, r);
			total += r;
		}
		return total;
	}
	
	public ChangeOp write() throws IOException {
		if (state == State.WRITE_HEADER) {
			String message = 
				"HTTP/1.1 200\r\n" +
				"Content-type: multipart/x-mixed-replace; boundary=\"myboundary\"\r\n" +
				"\r\n";
			
			buffer.clear();
			buffer.put(message.getBytes());
			buffer.flip();
			
			server.write(buffer);
			
			state = State.WRITE;
		} else {
			if (frameBuffer == null || !frameBuffer.hasRemaining()) {
				byte[] image = frameSource.nextImage();
				
				int desiredCapacity = 1024 + image.length;
				if (frameBuffer == null || frameBuffer.capacity() < desiredCapacity) {
					frameBuffer = ByteBuffer.allocate(desiredCapacity);
				} else {
					frameBuffer.clear();
				}
				
				frameBuffer.put("--myboundary\r\n".getBytes());
				frameBuffer.put("Content-Type: image/jpeg\r\n".getBytes());
				frameBuffer.put("\r\n".getBytes());
				
				frameBuffer.put(image);
				frameBuffer.put("\r\n".getBytes());
				frameBuffer.flip();
			}
			
			server.write(frameBuffer);
			
			try {
				Thread.sleep(100L);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
		}
		
		return ChangeOp.WRITE;
	}
}

