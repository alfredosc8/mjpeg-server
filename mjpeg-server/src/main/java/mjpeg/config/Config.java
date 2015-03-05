package mjpeg.config;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.google.gson.Gson;

public class Config {
	private static final Logger logger = LogManager.getLogger(Config.class.getName());
	
	private int port;
	private int workers;
	
	public Config() {
	}
	
	public int getPort() {
		return port;
	}

	public int getWorkers() {
		return workers;
	}
	
	public static Config load() {
		return new Gson().fromJson(readConfig(), Config.class);
	}

	private static String readConfig() {
		try {			
		    String fileName = "config.json";
		    
		    String define = System.getProperty("CONFIG");
		    if (define != null) {
		        fileName = define;
		    }
			
            Scanner sc = new Scanner(new File(fileName));
			String text = sc.useDelimiter("\\A").next();
			sc.close();
			
			return text;
		} catch (FileNotFoundException e) {
			logger.error(e);
		}
		
		return null;
	}
}
