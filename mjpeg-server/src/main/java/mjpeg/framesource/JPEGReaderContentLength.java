package mjpeg.framesource;

import java.io.IOException;
import java.util.Arrays;

import parser.AbstractParser;

public class JPEGReaderContentLength extends AbstractParser {
	private static final String CONTENT_LENGTH = "Content-Length: ";

	private byte[] separator;
	
	private int separatorPos = 0;
	private byte prev = 0;
	private int contentLength = -1;
	
	private boolean soi = false;
	
	private byte[] buffer = new byte[16 * 1024];
	private int pos;
	
	private StringBuilder headerLine;
	private byte[] frame;
	
	public JPEGReaderContentLength() {
		this.headerLine = new StringBuilder();
	}

	@Override
	public boolean feed(byte b) {
		if (soi) {
			if (contentLength != -1) {
				appendByte(b);
				
				if (pos == contentLength) {
					processChunk(Arrays.copyOf(buffer, pos-1));
					reset();
					
					return true;
				}
			} else {
				if (b != separator[separatorPos]) {
					if (separatorPos != 0) {
						
						int c = 0;
						while (c++ < separatorPos) {
							appendByte(separator[c]);
						}
						separatorPos = 0;
					}
					
					appendByte(b);
				} else {
					if (separatorPos++ >= separator.length - 1) {
						processChunk(Arrays.copyOf(buffer, pos-1));
						reset();
						
						return true;
					}
				}
			}
		} else {
			if (prev == (byte) 0xFF && b == (byte) 0xD8) {
				soi = true;
				appendByte(prev);
				appendByte(b);
			} else  {
				char ch = (char) (b & 0xFF);
				
				if (ch != '\r' && ch != '\n') {
					headerLine.append(ch);
				} else
				if (ch == '\n') {
					String line = headerLine.toString();
					headerLine = new StringBuilder();
					
					if (separator == null && line.startsWith("--")) {
						separator = line.getBytes();
					} else 
					if(contentLength == -1 && line.startsWith(CONTENT_LENGTH)) {
						contentLength = Integer.parseInt(line.substring(CONTENT_LENGTH.length()));
					}
				}
				prev = b;
			}
		}
		return false;
	}

	private void appendByte(byte b) {
		if (pos == buffer.length - 1) {
			buffer = Arrays.copyOf(buffer, buffer.length << 1);
		}
		
		buffer[pos++] = b;
	}

	private void processChunk(byte[] byteArray) {
		this.frame = byteArray;
	}

	private void reset() {
		soi = false;
		prev = 0;
		separatorPos = 0;
		pos = 0;
		contentLength = -1;
	}

	public byte[] getData() throws IOException {
		return frame;
	}
}
