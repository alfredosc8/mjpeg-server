package mjpeg.benchmark;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import mjpeg.framesource.JPEGReader;

import com.google.common.io.ByteStreams;

import counters.SimpleCounter;

public class Benchmark {
	private static final byte FF = (byte) 0xFF;
	private static final byte D8 = (byte) 0xD8;
	private static final byte D9 = (byte) 0xD9;
	
	private ByteBuffer buffer;

	public void start(String path) throws FileNotFoundException, IOException {
		prepare(path);
		startDummyBench();
		
		for (int i = 1; i < 20; ++i) {
			buffer.rewind();
			startBench2();
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
	
	private void startBench2() {
		SimpleCounter counter = new SimpleCounter("JPEGReader");
		counter.begin();

		byte[] buf = new byte[512 * 1024];
		int pos = 0;
		boolean soi = false, prev = false;
		byte[] arrayBuffer = buffer.array();
		
		final int limit = buffer.limit();
		byte b;
		for (int i = buffer.position(); i < limit; ++i) {
			b = arrayBuffer[i];
			if (soi) {
				buf[pos++] = b;
			}
			
			if (prev) {
				if (soi) {
					if (b == D9) {
						Arrays.copyOf(buf, pos);
						pos = 0;
						soi = false;

						counter.tick();
						
						continue;
					}
				} else {
					if (b == D8) {
						soi = true;
						buf[pos++] = FF;
						buf[pos++] = D8;
					}
				}
			}
			
			
			prev = b == FF;
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
