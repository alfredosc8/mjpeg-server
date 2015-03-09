package mjpeg;

public class AdaptiveDelay {
	private final long target;
	private long time;

	public AdaptiveDelay(long targetDelay) {
		this.target = targetDelay;
	}
	
	public void delay() throws InterruptedException {
//		long now = System.currentTimeMillis();
//		long delay = target - (now - time);
//		
//		if (delay > 0) {
//			Thread.sleep(delay);
//		}
//		
//		time = System.currentTimeMillis();
	}
}
