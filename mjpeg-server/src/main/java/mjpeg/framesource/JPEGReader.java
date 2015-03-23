package mjpeg.framesource;

import java.io.IOException;
import java.util.Arrays;

import parser.AbstractParser;

public class JPEGReader extends AbstractParser {
	private boolean soi = false;
	private byte prev = 0;
	private byte[] buffer = new byte[16 * 1024];
	private int pos;
	
	private byte[] frame;
	
	@Override
	public boolean feed(byte b) {
		if (soi) {
			if (prev == (byte) 0xFF && b == (byte) 0xD9) {
				appendByte((byte) 0xFF);
				appendByte((byte) 0xD9);
				
				processChunk(Arrays.copyOf(buffer, pos));
				reset();
				
				return true;
			} else {
				appendByte(b);
			}
		} else {
			if (prev == (byte) 0xFF && b == (byte) 0xD8) {
				soi = true;
				appendByte((byte) 0xFF);
				appendByte((byte) 0xD8);
			}
		}
		
		prev = b;
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
		pos = 0;
	}

	public byte[] getData() throws IOException {
		return frame;
	}
}
