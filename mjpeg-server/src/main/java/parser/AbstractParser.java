package parser;

import java.nio.ByteBuffer;

/**
 * Парсер
 * @author vzhilin
 */
public abstract class AbstractParser {
	/**
	 * Обрабатывает байт
	 * @param b
	 * @return true, если разбор окончен
	 */
	public abstract boolean feed(byte b);
	
	/**
	 * Обрабатывает байты в буфере
	 * @param buffer
	 * @return true, когда разбор окончен
	 */
	public boolean feed(ByteBuffer buffer) {
		for (int i = 0; i < buffer.remaining(); ++i) {
			if (feed(buffer.get())) {
				return true;
			}
		}
		
		return false;
	}
}
