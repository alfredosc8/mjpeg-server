package mjpeg.framesource;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

public class JPEGFrameSource {
	protected BufferedInputStream stream;
	
	private JPEGReader parser = new JPEGReader();

	public JPEGFrameSource(InputStream is) {
		this.stream = new BufferedInputStream(is, 64 * 1024);
	}
	
	public JPEGFrameSource() {
		// TODO Auto-generated constructor stub
	}

	public void close() throws IOException {
		stream.close();
	}

	public byte[] nextImage() throws IOException {
		readHeaderChunk(stream);
		byte[] frame = parser.next(stream);
		
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
	
	private void readHeaderChunk(BufferedInputStream bis) throws IOException {
		String line;
		do {
			line = readLine(bis);
		} while (!line.isEmpty());
	}
	
	private String readLine(BufferedInputStream bis) throws IOException {
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