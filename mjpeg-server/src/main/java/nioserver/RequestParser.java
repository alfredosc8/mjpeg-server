package nioserver;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class RequestParser {
	private enum State {
		INIT,
		READ_LINE,
		READ_LF,
		END
	}
	
	private State state = State.INIT;
	private StringBuilder sb;
	
	private List<String> headers = new LinkedList<String>();
	
	public boolean feed(byte b) {
		char ch = (char) (b & 0xFF);
		
		switch (state) {
		case INIT:
			state = State.READ_LINE;
			sb = new StringBuilder();
		case READ_LINE:
			if (ch == '\r') {
				state = State.READ_LF;
				return true;
			}
			
			sb.append(ch);
			return true;
		case READ_LF:
			if (ch == '\n') {
				String line = sb.toString();
				if (line.isEmpty()) {
					state = State.END;
					return false;
				} else {
					headers.add(line);
				}
			}
			state = State.INIT;
			return true;
		case END:
			return false;
		}
		
		throw new IllegalStateException();
	}
	
	public Request getRequest() {
		Iterator<String> it = headers.iterator();
		
		String[] firstLine = it.next().split(" ");

		Request request = new Request();
		request.setQuery(firstLine[1]);
		
		Map<String, List<String>> headers = new HashMap<String, List<String>>();
		while (it.hasNext()) {
			String line = it.next();
			
			int separator = line.indexOf(':');
			
			String key = line.substring(0, separator).trim();
			String value = line.substring(separator + 1);
			
			List<String> values = new LinkedList<String>();
			for (String v : value.split(";")) {
				values.add(v.trim());
			}
			
			headers.put(key, values);
		}
		
		request.setHeaders(headers);
		return request;
	}
}
