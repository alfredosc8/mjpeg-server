package mjpeg.framesource;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

public class JPEGReader {
	private ByteArrayOutputStream buffer;
	
	public JPEGReader() {
		buffer = new ByteArrayOutputStream();
	}

	public byte[] next(InputStream is) throws IOException {
		buffer.reset();
		
		// SOI marker
		int soi = readUint(is);
		if (soi != 0xFFD8) {
			throw new IOException("not JPEG: " + String.format("%x", soi));
		};
		
		mainLoop:
		while(true) {
			int uint = readUint(is);
			
			// EOI-Segment
			if (uint == 0xFFD9) {
				break;
			}
			
			// SOS-Segment
			if (uint == 0xFFDA) {
				while (true) {
					if (readByte(is) == 0xFF) {
						int unsigned = readByte(is);
						if (unsigned == 0x00) {
							continue;
						}
						
						if (unsigned < 0xD0 || unsigned > 0xD7) {
							if (unsigned == 0xD9) {
								break mainLoop;
							}
						}
					}
				}
			}
			
			int length = readUint(is);
			skip(is, length - 2);
		}

		byte[] data = buffer.toByteArray();
		return data;
	}

	private void skip(InputStream fis, int length) throws IOException {
		for (int i = 0; i != length; ++i) {
			int c;
			
			if ((c = fis.read()) == -1) {
				throw new EOFException("Unexpected EOF");
			}
			
			buffer.write(c);		
		}
	}

	private int readUint(InputStream fis) throws IOException {
		int hi = fis.read();
		int lo = fis.read();
		
		if (hi == -1 && lo == -1) {
			throw new EOFException("Unexpected EOF");
		}
		
		buffer.write(hi);
		buffer.write(lo);
		
		return ((hi & 0xFF) << 8) + (lo & 0xFF);
	}

	private int readByte(InputStream is) throws IOException {
		int data = is.read();
		if (data == -1) {
			throw new EOFException("Unexpected EOF");
		}
		
		buffer.write(data);
		return data & 0xFF;
	}
}
