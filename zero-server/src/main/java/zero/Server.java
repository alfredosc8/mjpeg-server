package zero;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class Server {
	private static final Logger logger = LogManager.getLogger(Server.class.getName());
	private final static byte[] zeroBuffer = new byte[1024 * 128];
	
	public static void main(String[] args) throws ParseException {
		try {
			Options options = new Options();
			options.addOption("p", true, "server port");
			
			CommandLineParser parser = new BasicParser();
			CommandLine cmd = parser.parse( options, args);
			
			if (!cmd.hasOption("p")) {
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp( "zero-server.jar", options );
				return;
			}
			
			new Server().start(Integer.parseInt(cmd.getOptionValue("p")));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void start(int port) throws IOException {
		BasicConfigurator.configure();
		ServerSocket socket = new ServerSocket(port);
		ExecutorService service = Executors.newFixedThreadPool(4);
		
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
			
			service.execute(task);
		}
	}

	protected void handleRequest(Socket connection) throws IOException {
		String webServerAddress = connection.getInetAddress().toString();
		logger.info("New Connection:" + webServerAddress);
		
		BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		String request = in.readLine();
		
		logger.info("--- Client request: " + request);
		OutputStream os = connection.getOutputStream();
		
		PrintWriter out = new PrintWriter(os, true);
		out.printf("HTTP/1.0 200" + "\r\n");
		out.printf("Content-type: \"application/octet-stream\"" + "\r\n");
		out.printf("\r\n");
		out.flush();
		
		while (true) {
			os.write(zeroBuffer);
		}
	}
}
