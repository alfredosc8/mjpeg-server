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

public class Unpacker {
	public void start(String ifile) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		FileInputStream is = new FileInputStream(ifile);
		ByteStreams.copy(is, bos);

		ByteBuffer buffer = ByteBuffer.wrap(bos.toByteArray());
		JPEGReader reader = new JPEGReader();

		int i = 0;
		while (reader.feed(buffer)) {
			byte[] image = reader.getData();
			ImageIO.read(new ByteArrayInputStream(image));
			FileOutputStream fos = new FileOutputStream(String.format("./%06d.jpeg", i++));
			fos.write(image);
			fos.close();
		}
	}
}
