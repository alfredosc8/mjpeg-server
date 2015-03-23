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

	public void start(String path) throws FileNotFoundException, IOException {
		prepare(path);
		startDummyBench();
		
		for (int i = 1; i < 4; ++i) {
			buffer.rewind();
			startReaderBench();
		}
	}

	private void startDummyBench() throws FileNotFoundException, IOException {
		SimpleCounter counter = new SimpleCounter("dummy");
		
		counter.begin();
		counter.tick();
		
		while (buffer.hasRemaining()) {
			buffer.get();
		}
		
		counter.end();
		counter.print();
	}

	private void startReaderBench() throws IOException {
		JPEGReader reader = new JPEGReader();

		SimpleCounter counter = new SimpleCounter("JPEGReader");
		counter.begin();

		while (reader.feed(buffer)) {
			reader.getData();
			counter.tick();
		}
		
		counter.end();
		counter.print();
	}

	private void prepare(String path) throws FileNotFoundException, IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		FileInputStream is = new FileInputStream(path);
		ByteStreams.copy(is, bos);
		
		buffer = ByteBuffer.wrap(bos.toByteArray());
	}
}
