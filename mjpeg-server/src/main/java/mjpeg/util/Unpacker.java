package mjpeg.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;

import mjpeg.framesource.JPEGReader;

import com.google.common.io.ByteStreams;

import counters.SimpleCounter;

public class Unpacker {
	public static void main(String[] args) throws IOException {
		new Unpacker().start();
	}

	private void start() throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		FileInputStream is = new FileInputStream("video/video7.cgi");
		ByteStreams.copy(is, bos);

		ByteBuffer buffer = ByteBuffer.wrap(bos.toByteArray());

		JPEGReader reader = new JPEGReader();
		SimpleCounter counter = new SimpleCounter();

		counter.begin();

		int i = 0;
		while (reader.feed(buffer)) {
			byte[] image = reader.getData();
			ImageIO.read(new ByteArrayInputStream(image));
			FileOutputStream fos = new FileOutputStream(String.format("data/%05d.jpeg", i++));
			fos.write(image);
			fos.close();
			counter.tick();
		}
		
		counter.end();
		counter.print();
	}
}
