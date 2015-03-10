package com.vzhilin;

import info.monitorenter.gui.chart.Chart2D;
import info.monitorenter.gui.chart.traces.Trace2DLtdReplacing;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;

public class Monitor {
	private Trace2DLtdReplacing traceLatency;
	private Trace2DLtdReplacing traceBandwith;
	
	public static void main(String[] args) throws IOException {
		new Monitor().start();
	}

	private void start() throws IOException {
	    Chart2D chartLatency   = new Chart2D();
	    Chart2D chartBandwidth = new Chart2D();
	    
	    traceBandwith = new Trace2DLtdReplacing(1000);
	    traceLatency = new Trace2DLtdReplacing(1000);
	    chartLatency.addTrace(traceLatency);
	    chartBandwidth.addTrace(traceBandwith);
	    
	    JFrame frame = new JFrame();
	    JTabbedPane tabs = new JTabbedPane();
	    frame.add(tabs);
	    
	    
		JPanel innerFrame = new JPanel();
		innerFrame.setLayout(new GridLayout(2, 1));
		innerFrame.add(chartLatency);
		innerFrame.add(chartBandwidth);
		innerFrame.setSize(640, 480);
		innerFrame.setMinimumSize(new Dimension(640, 480));
		
		JScrollPane scroll = new JScrollPane(innerFrame, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		tabs.addTab("Latency & Fps", scroll);
		
		frame.setSize(640, 480);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
		startListener();
	}

	private void startListener() throws IOException {
		final DatagramSocket socket = new DatagramSocket(4444);
		byte[] buffer = new byte[256];

		final DatagramPacket p = new DatagramPacket(buffer, 256);
		
		new Thread(new Runnable() {
			public void run() {
				long t1 = 0;
				long startTime = 0;
				
				FrequesncyCounter c = new FrequesncyCounter();
				
				while (true) {
					try {
						socket.receive(p);
						
						DataInputStream dis = new DataInputStream(new ByteArrayInputStream(p.getData()));
						long time = dis.readLong();
						int size = dis.readInt();
						
						c.tick(time);
					
						if (t1 != 0) {
							long delta = time - t1;
							traceLatency.addPoint(time - startTime, delta);
							traceBandwith.addPoint(time - startTime, c.get());
						} else {
							startTime = time;
						}
						
						t1 = time;
						
					} catch (IOException e) {
						e.printStackTrace();
						break;
					}
				}
			}
		}).start();
	}
}
