package mjpeg.framesource;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

public class JPEGFrameSource {
	protected InputStream stream;
	private JPEGReader parser = new JPEGReader();
    private HashMap<String, String> headers;

	public JPEGFrameSource(InputStream is) {
	    this();
		this.stream = new BufferedInputStream(is);
	}
	
	public JPEGFrameSource() {
	    headers = new HashMap<String, String>();
	}

	public void close() throws IOException {
		stream.close();
	}

	public byte[] nextImage() throws IOException {
		readHeaderChunk(stream);
		
		int contentLength = -1;
		if (headers.containsKey("Content-Length")) {
		    contentLength = Integer.parseInt(headers.get("Content-Length"));
		}
		
		byte[] frame = parser.next(stream, contentLength);
		
		skipEmptyLine();
		return frame;
	}

	private void skipEmptyLine() throws IOException {
		while (true) {
			stream.mark(2);
			if (stream.read() != '\r' || stream.read() != '\n') {
				stream.reset();
				break;
			}
		}
	}
	
    private void readHeaderChunk(InputStream bis) throws IOException {
        headers.clear();
        String line;
        
        int c;
        do {
            line = readLine(bis);
            if ((c = line.indexOf(':')) != -1) {
                String key = line.substring(0, c);
                String value = line.substring(c+1);
                
                headers.put(key.trim(), value.trim());
            }
        } while (!line.isEmpty());
    }
    
    private String readLine(InputStream bis) throws IOException {
        StringBuilder sb = new StringBuilder();
        
        int ch;
        while ((ch = bis.read()) != -1) {
            if (ch == '\r') {
                ch = bis.read();
                
                if (ch == '\n') {
                    break;
                }
                    
                sb.append('\r');
            } 
            
            sb.append((char) (ch & 0xFF));
        }
        
        return sb.toString();
    }
}