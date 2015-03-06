package nioclient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Request {

	private String query;
	public String getQuery() {
		return query;
	}

	private Map<String, List<String>> headers;
	private Map<String, String> params = new HashMap<String, String>();
	private String resource;
	
	public String getResource() {
		return resource;
	}

	public String getParam(String key) {
		return params.get(key);
	}
	
	public void setQuery(String query) {
		this.query = query;
		
		if (query.indexOf('?') != -1) {
			String paramsLine = query.substring(query.indexOf('?')+1);
			for (String param : paramsLine.split("&")) {
				String key = param.split("=")[0];
				String value = param.split("=")[1];
				
				params.put(key, value);
			}
			
			resource = query.substring(0, query.indexOf('?'));
		} else {
			resource = query;
		}
	}

	public void setHeaders(Map<String, List<String>> headers) {
		this.headers = headers;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(query);
		sb.append("\n");
		
		for (String key : headers.keySet()) {
			sb.append(key + ": " + headers.get(key));
			sb.append("\n");
		}
		
		return sb.toString();
	}
}
