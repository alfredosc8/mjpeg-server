package mjpeg.benchmark;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;

import mjpeg.framesource.JPEGReader;

import com.google.common.io.ByteStreams;

import counters.SimpleCounter;

public class Benchmark {
	private ByteBuffer buffer;

	public static void main(String[] args) throws IOException {
		new Benchmark().start();
	}

	private void start() throws FileNotFoundException, IOException {
		prepare();
		startDummyBench();
		
		for (int i = 1; i < 4; ++i) {
			buffer.rewind();
			startReaderBench();
		}
	}

	private void startDummyBench() throws FileNotFoundException, IOException {
		SimpleCounter counter = new SimpleCounter();
		
		counter.begin();
		counter.tick();
		
		while (buffer.hasRemaining()) {
			int c = buffer.get();
		}
		
		counter.end();
		counter.print();
	}

	private void startReaderBench() throws IOException {
		JPEGReader reader = new JPEGReader();

		SimpleCounter counter = new SimpleCounter();
		counter.begin();

		while (reader.feed(buffer)) {
			reader.getData();
			counter.tick();
		}
		
		counter.end();
		counter.print();
	}

	private void prepare() throws FileNotFoundException, IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		FileInputStream is = new FileInputStream("video/video.cgi");
		ByteStreams.copy(is, bos);
		
		buffer = ByteBuffer.wrap(bos.toByteArray());
	}
}
