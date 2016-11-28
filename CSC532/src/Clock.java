
public class ClockThread implements Runnable{
	Thread t;
	Thread hostThread;
	int currTick = 0;
	int tickDelayMS = 0;
	boolean isServer = false;
	boolean isRunning = false;
	
	ClockThread(Thread ht) {
		hostThread = ht;
	}
	
	public void start() {
		if(t == null) {
			isRunning = true;
			t= new Thread(this);
			t.start();
		}
	}
	
	public void addTick(int toAdd) {
		currTick += toAdd;
	}
	
	public void addDelay(int toAdd) {
		tickDelayMS += toAdd;
	}

	@Override
	public void run() {
		int initTick = currTick;
		while(isRunning) {
			currTick++;
			System.out.println("Thread Tick: " + currTick);
				try {
					Thread.sleep(tickDelayMS);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					isRunning = false;
					return;
				}
			//Testing if the thread is still alive is probably not necessary
			//Added just in case
			if(currTick == initTick + 10 || !hostThread.isAlive()) {
				System.out.println("Main method thread is not alive, exiting");
				return;
			}
		}
	}
}
