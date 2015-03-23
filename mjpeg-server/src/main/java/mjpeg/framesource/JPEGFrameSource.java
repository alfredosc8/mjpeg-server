package mjpeg.framesource;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

public class JPEGFrameSource {
	private JPEGReader parser;
	private InputStream is;

	public JPEGFrameSource(InputStream is) {
		parser = new JPEGReader();
		this.is = is;
	}

	public byte[] nextImage() throws IOException {
		while (true) {
			int c = is.read();
			if (c == -1) {
				throw new EOFException();
			}
		
			if (parser.feed((byte) c)) {
				return parser.getData();
			}
		}
	}
}