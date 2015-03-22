package mjpeg.framesource;

import java.io.IOException;

import parser.MarkableParser;

public class MarkableJPEGReader extends MarkableParser {
	private byte[] separator;
	
	private int separatorPos = 0;
	private byte prev = 0;
	
	private boolean soi = false;
	
	private StringBuilder separatorBuilder;
	private byte[] frame;
	
	public MarkableJPEGReader() {
		this.separatorBuilder = new StringBuilder();
	}

	@Override
	public boolean feed(byte b) {
		if (soi) {
			if (b == separator[separatorPos]) {
				if (separatorPos++ >= separator.length - 1) {
					end();
					reset();
					
					return true;
				}
			} else {
				int c = 0;
				
				if (separatorPos > 0) {
					while (c < separatorPos) {
						c++;
					}
					separatorPos = 0;
				}
			}
		} else {
			if (separator == null) {
				char ch = (char) (b & 0xFF);
				
				if (ch != '\r' && ch != '\n') {
					separatorBuilder.append(ch);
				} else
				if (ch == '\n') {
					separator = separatorBuilder.toString().getBytes();
				}
			} else {
				if (prev == (byte) 0xFF && b == (byte) 0xD8) {
					soi = true;
					begin();
				}
				prev = b;
			}
		}
		return false;
	}

	private void reset() {
		soi = false;
		prev = 0;
		separatorPos = 0;
	}

	public byte[] getData() throws IOException {
		return data();
	}
}
