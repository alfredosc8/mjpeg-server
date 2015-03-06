package nioclient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import nioclient.Worker.ChangeOp;

public class NioServer {
	public static void main(String[] args) {
		try {
			new NioServer().start();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void start() throws IOException, InterruptedException {
		Selector socketSelector = SelectorProvider.provider().openSelector();
		ServerSocketChannel serverChannel = ServerSocketChannel.open();
		serverChannel.configureBlocking(false);
		
		InetSocketAddress isa = new InetSocketAddress(82);
		serverChannel.socket().bind(isa);
		serverChannel.register(socketSelector, SelectionKey.OP_ACCEPT);

		Map<SocketChannel, Worker> workers = new HashMap<SocketChannel, Worker>();
		
		while (true) {
			socketSelector.select();
			Set<SelectionKey> keys = socketSelector.selectedKeys();

			for (SelectionKey key : keys) {
				if (key.isValid() && key.isAcceptable()) {
					ServerSocketChannel server = (ServerSocketChannel) key.channel();
					SocketChannel channel = server.accept();
					registerChannel(socketSelector, channel, SelectionKey.OP_READ);
					
					workers.put(channel, new Worker(channel));
				} else 
				if (key.isValid() && key.isReadable()) {
					SocketChannel server = (SocketChannel) key.channel();
					
					Worker w = (Worker) workers.get(server);
					if (w.read() == ChangeOp.WRITE) {
						SelectionKey writeKey = server.keyFor(socketSelector);
						writeKey.interestOps(SelectionKey.OP_WRITE);
					}					
				}
				if (key.isValid() && key.isWritable()) {
					SocketChannel server = (SocketChannel) key.channel();
					
					Worker w = workers.get(server);
					ChangeOp status = w.write();
					
					if (status == ChangeOp.READ) {
						key.interestOps(SelectionKey.OP_READ);
					} else 
					if (status == ChangeOp.CLOSE) {
						server.close();
					}
				}
			}
			
			keys.clear();
		}
	}

	private void registerChannel(Selector socketSelector,
			SocketChannel channel, int ops) throws IOException {
		
		if (channel == null) {
			return;
		}
		
		channel.configureBlocking(false);
		channel.register(socketSelector, ops);
	}
}
