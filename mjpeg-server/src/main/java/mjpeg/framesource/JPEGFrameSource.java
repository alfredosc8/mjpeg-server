package mjpeg.framesource;

import java.io.IOException;
import java.io.InputStream;

public class JPEGFrameSource {
	private ImprovedJPEGReader parser;

	public JPEGFrameSource(InputStream is) {
		parser = new ImprovedJPEGReader(is);
	}

	public byte[] nextImage() throws IOException {
		return parser.next();
	}
}