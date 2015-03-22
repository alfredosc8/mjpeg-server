package parser;

import java.nio.ByteBuffer;
import java.util.LinkedList;

import com.google.common.primitives.Bytes;

public abstract class MarkableParser extends AbstractParser {
	private LinkedList<ByteBuffer> bufferChain;
	
	private boolean active;
	private ByteBuffer buffer;
	private int offset;
	
	public MarkableParser() {
		active = false;
		bufferChain = new LinkedList<ByteBuffer>();
	}

	@Override
	public boolean feed(ByteBuffer buffer) {
		this.buffer = buffer;
		if (active) {
			offset = buffer.position();
			
			ByteBuffer buf = buffer.slice();
			bufferChain.add(buf);
		}
		
		return super.feed(buffer);
	}
	
	protected void begin() {
		if (active) {
			throw new IllegalStateException();
		}
		
		active = true;
		bufferChain.clear();
		
		buffer.position(buffer.position() - 1);
		offset = buffer.position();
		bufferChain.add(buffer.slice());
		buffer.position(buffer.position() + 1);
	}
	
	protected void end() {
		if (!active) {
			throw new IllegalStateException();
		}
		
		active = false;
		bufferChain.getLast().limit(buffer.position() - offset);
	}
	
	protected byte[] data() {
		byte[][] data = new byte[bufferChain.size()][];
		int i = 0;
		
		for (ByteBuffer buffer : bufferChain) {
			byte[] bytes = new byte[buffer.remaining()];
			buffer.get(bytes);
			
			data[i++] = bytes;
		}
		
		return Bytes.concat(data);
	}
}
