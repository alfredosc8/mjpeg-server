package nioclient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import mjpeg.config.Config;
import nioclient.Worker.ChangeOp;

public class NioServer {
	private Map<SocketChannel, Worker> workers = new HashMap<SocketChannel, Worker>();
	private Selector socketSelector = null;
	
	public static void main(String[] args) throws InterruptedException {
		new NioServer().start();
	}

	private void start() throws InterruptedException {
		try {
			socketSelector = SelectorProvider.provider().openSelector();
			ServerSocketChannel serverChannel = ServerSocketChannel.open();
			serverChannel.configureBlocking(false);
			
			InetSocketAddress isa = new InetSocketAddress(Config.load().getPort());
			serverChannel.socket().bind(isa);
			serverChannel.register(socketSelector, SelectionKey.OP_ACCEPT);
		} catch (IOException ex) {
			error(ex);
		}

		Set<SelectionKey> keys = null;

		while (true) {
			try {
				socketSelector.select();
				keys = socketSelector.selectedKeys();
			} catch (IOException ex) {
				error(ex);
			}
			
			List<SelectionKey> orderKeys = orderKeys(keys);
			if (!orderKeys.isEmpty()) {
				SelectionKey k = orderKeys.get(0);
				Worker w;
				if ((w = workers.get(k.channel())) != null) {
					long delta = w.timeWait() - System.currentTimeMillis();
					if (delta > 0) {
						Thread.sleep(delta);
					}
				}
				processKey(k);
			}
			keys.clear();
		}
	}

	private void processKey(SelectionKey key) {
		if (key.isValid() && key.isAcceptable()) {
			ServerSocketChannel server = (ServerSocketChannel) key.channel();
			SocketChannel channel;
			try {
				channel = server.accept();
				registerChannel(socketSelector, channel, SelectionKey.OP_READ);
				workers.put(channel, new Worker(channel));
			} catch (IOException e) {
				logError(e);
			}
			
		} else 
		if (key.isValid() && key.isReadable()) {
			SocketChannel server = (SocketChannel) key.channel();
			
			Worker w = (Worker) workers.get(server);
			
			try {
				if (w.read() == ChangeOp.WRITE) {
					SelectionKey writeKey = server.keyFor(socketSelector);
					writeKey.interestOps(SelectionKey.OP_WRITE);
				}
			} catch (IOException ex) {
				logError(ex);
				key.cancel();
				try {
					server.close();
				} catch (IOException e) {
					logError(e);
				}
			}
		} else
		if (key.isValid() && key.isWritable()) {
			SocketChannel server = (SocketChannel) key.channel();
			
			try {
				Worker w = workers.get(server);
				ChangeOp status = w.write();
				
				if (status == ChangeOp.READ) {
					key.interestOps(SelectionKey.OP_READ);
				} else 
				if (status == ChangeOp.CLOSE) {
					workers.remove(w);
					server.close();
				}
			} catch (IOException ex) {
				logError(ex);
				key.cancel();
				try {
					server.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}		
	}
	
	private List<SelectionKey> orderKeys(Set<SelectionKey> keys) {
		List<SelectionKey> k = new ArrayList<SelectionKey>(keys);
		
		Collections.sort(k, new Comparator<SelectionKey>() {

			@Override
			public int compare(SelectionKey o1, SelectionKey o2) {
				long r1 = 0;
				long r2 = 0;
				
				Worker w1, w2;
				if ((w1 = workers.get(o1.channel())) != null) {
					r1 = w1.timeWait();
				}
				
				if ((w2 = workers.get(o2.channel())) != null) {
					r2 = w2.timeWait();
				}
				
				return r1 < r2 ? -1 : (r1 > r2 ? 1 : 0);
			}
		});
		
		return k;
	}

	private void logError(IOException e) {
		e.printStackTrace();
	}

	private void error(IOException ex) {
		ex.printStackTrace();
		System.exit(-1);
	}

	private void registerChannel(Selector socketSelector, SocketChannel channel, int ops) throws IOException {
		if (channel == null) {
			return;
		}
		
		channel.configureBlocking(false);
		channel.register(socketSelector, ops);
	}
}