package mjpeg.framesource;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

public class URLJpegFrameSource extends JPEGFrameSource {
	private String connectionString;
	
	public URLJpegFrameSource(String connecionString) {
		this.connectionString = connecionString;
	}
	
	public void close() {
		try {
			if (stream != null) {
				stream.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected void setupConnection() throws IOException {
		URL url = new URL(connectionString);
		URLConnection conn = url.openConnection();
		stream = conn.getInputStream();
	}
	
    public void setupConnection(URLConnection urlConnection) throws IOException {
        stream = urlConnection.getInputStream();
    }

	@Override
	public byte[] nextImage() throws IOException {
		if (stream == null) {
			setupConnection();
		}
		
		return super.nextImage();
	}
}
