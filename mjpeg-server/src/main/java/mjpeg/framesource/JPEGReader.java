package mjpeg.framesource;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

public class JPEGReader {
	private byte[] separator;
	private DataInputStream dis;
	
	private int separatorPos = 0;
	private byte prev = 0;
	private boolean soi = false;
	
	private ByteArrayOutputStream bos;
	private StringBuilder separatorBuilder;
	
	private byte[] frame;
	
	public JPEGReader(InputStream is) {
		this.dis = new DataInputStream(new BufferedInputStream(is, 0x1024));
		this.separatorBuilder = new StringBuilder();
		this.bos = new ByteArrayOutputStream();
	}

	private boolean feed(byte b) {
		boolean frameReady = false;
		
		if (separator == null) {
			char ch = (char) (b & 0xFF);
			
			if (ch != '\r' && ch != '\n') {
				separatorBuilder.append(ch);
			} else
			if (ch == '\n') {
				separator = separatorBuilder.toString().getBytes();
			}
		} else {
			if (!soi && prev == (byte) 0xFF && b == (byte) 0xD8) {
				soi = true;
				bos.write(prev);
			}
			if (b == separator[separatorPos]) {
				if (separatorPos < separator.length - 1) {
					separatorPos++;
				} else {
					if (soi) {
						processChunk(bos.toByteArray());
						reset();
						
						frameReady = true;
					}
				}
			} else {
				if (separatorPos > 0) {
					if (soi) {
						bos.write(separator, 0, separatorPos);
					}
					
					separatorPos = 0;
				}
				
				if (soi) {
					bos.write(b);
				}
			}
		}
		
		prev = b;
		return frameReady;
	}

	private void processChunk(byte[] byteArray) {
		this.frame = byteArray;
	}

	private void reset() {
		soi = false;
		prev = 0;
		separatorPos = 0;
		bos.reset();
	}

	public byte[] next() throws IOException {
		while (!feed(dis.readByte())) {
			//
		}
		
		
		return frame;
	}
}
