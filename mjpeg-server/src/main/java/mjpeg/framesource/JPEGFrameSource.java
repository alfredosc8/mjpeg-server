package mjpeg.framesource;

import java.io.IOException;
import java.io.InputStream;

public class JPEGFrameSource {
	private JPEGReader parser;

	public JPEGFrameSource(InputStream is) {
		parser = new JPEGReader(is);
	}

	public byte[] nextImage() throws IOException {
		return parser.next();
	}
}