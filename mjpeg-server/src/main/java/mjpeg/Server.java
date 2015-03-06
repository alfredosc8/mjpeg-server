package mjpeg;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mjpeg.config.Config;
import mjpeg.framesource.JPEGFrameSource;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class Server {
	private static final Logger logger = LogManager.getLogger(Server.class.getName());
	private final static String BOUNDARY =  "boundary";
	private AtomicLong connecitonCounter = new AtomicLong();
	
	private Map<String, byte[]> data = new HashMap<String, byte[]>();
	
	public static void main(String[] args) throws IOException {
		BasicConfigurator.configure();
		new Server().start();
	}

	private void start() throws IOException {
		Config config = Config.load();
		
		ServerSocket socket = new ServerSocket(config.getPort());
		ExecutorService pool = Executors.newFixedThreadPool(config.getWorkers());
		
		while (true) {
			final Socket connection = socket.accept();
			
			Runnable task = new Runnable() {
				
				@Override
				public void run() {
					try {
						handleRequest(connection);
					} catch (IOException e) {
						logger.error(e);
					}
				}
			};
			
			pool.execute(task);
		}
	}

	protected void handleRequest(Socket connection) throws IOException {			
		long id = connecitonCounter.incrementAndGet();

		String webServerAddress = connection.getInetAddress().toString();
		logger.info("New Connection:" + webServerAddress + " id: " + id);
		
		BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		String request = in.readLine();
		
		logger.info("--- Client request: " + request);
		OutputStream os = connection.getOutputStream();
		PrintWriter out = new PrintWriter(os, true);
		
		Param param = new Param(request);
		if (!new File(param.getFile()).exists()) {
			out.printf("HTTP/1.0 404\r\n");
			out.printf("\r\n");
			out.printf("File '" + param.getFile() + "' not found\r\n");
			out.flush();
			connection.close();
			return;
		}
		
		AdaptiveDelay delay;
		if (param.getFps() != 0) {
			delay = new AdaptiveDelay((long) (1000f / param.getFps()));
		} else {
			delay = new AdaptiveDelay(0L);
		}
		
		out.printf("HTTP/1.0 200" + "\r\n");
		out.printf(String.format("Content-type: multipart/x-mixed-replace; boundary=\"%s\"\r\n", BOUNDARY));
		out.printf("\r\n");
		out.flush();
		
		JPEGFrameSource source = new JPEGFrameSource(loadFile(param.getFile()));
		while (true) {
			byte[] image = null;
			
			try {
				delay.delay();
				image = source.nextImage();
			} catch (EOFException e) {
				logger.error("eof", e);
				source.close();
				source = new JPEGFrameSource(loadFile(param.getFile()));
				
				continue;
			} catch (IOException e) {
				logger.error("error", e);
				break;
			} catch (InterruptedException e) {
				logger.error(e);
				break;
			}
			
			out.print("--" + BOUNDARY + "\r\n");
			out.print("Content-type: image/jpeg" + "\r\n");
			out.print("Content-Length: " + image.length + "\r\n");
			out.print("\r\n");
			out.flush();
			
			os.write(image);
			os.flush();
			
			out.print("\r\n");
			out.flush(); 
		}
		
		logger.info("disconnected: " + id);
		
		os.close();
		out.close();
		connection.close();
	}

	private InputStream loadFile(String file) throws IOException {
		synchronized (data) {
			if (!data.containsKey(file)) {
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				FileInputStream fis = new FileInputStream(file);
				copy(fis, bos);
				fis.close();
				
				data.put(file, bos.toByteArray());
			}
		}
		
		return new ByteArrayInputStream(data.get(file));
	}
	
	public static long copy(InputStream from, OutputStream to) throws IOException {
		byte[] buf = new byte[64 * 1024];
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
	
	private class Param {
		private String file;
		private Float fps = 0f;

		public Float getFps() {
			return fps;
		}

		public String getFile() {
			return file;
		}

		public Param(String request) {
			Pattern p = Pattern.compile("GET /([^?]+)(\\??)(.*) HTTP.*");
			Matcher m = p.matcher(request);
			if (m.matches()) {
				file = m.group(1);
				String args = m.group(3);
				
				Map<String, String> data = new HashMap<String, String>();
				if (!args.isEmpty()) {
					for (String s : args.split("&")) {
						data.put(s.split("=")[0], s.split("=")[1]);
					}
				}
				
				if (data.containsKey("fps")) {
					fps = Float.valueOf(data.get("fps"));
				}
			}
		}
	}
}
