package mjpeg;

public class AdaptiveDelay {
	private final long target;
	private long time;

	public AdaptiveDelay(long targetDelay) {
		this.target = targetDelay;
	}
	
	public void delay() throws InterruptedException {
//		if (delay > 0) {
			Thread.sleep(target);
//		}
	}
}
