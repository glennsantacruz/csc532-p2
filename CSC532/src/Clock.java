
class Clock implements Runnable {
	protected int tick = 0;
	protected int tickGranule = 1;
	protected int msDelay = 10;

	public Clock(int tick, int msDelay) {
		setTick(tick);
		setDelayMillis(msDelay);
	}

	protected void addTick(int toAdj) {
		synchronized (this) {
			this.tick += toAdj;
			// now that we have ticked, notify any blocking
			// threads so they can take action
			notify();
		}
	}

	protected void setTick(int newTick) {
		synchronized (this) {
			this.tick = newTick;
		}
	}

	public int getTick() {
		synchronized (this) {
			return this.tick;
		}
	}

	public int getDelayMillis() {
		synchronized (this) {
			return this.msDelay;
		}
	}

	protected void setDelayMillis(int newMillis) {
		synchronized (this) {
			this.msDelay = newMillis;
			// will not allow our delay to become negative nor stop clock
			// altogether
			if (this.msDelay <= 0) {
				this.msDelay = 1;
			}
		}
	}

	public void run() {
		boolean isRunning = true;
		while (isRunning) {
			try {
				Thread.sleep(this.msDelay);
			} catch (InterruptedException ire) {
				isRunning = false;
			}
			// we have slept long enough; add our tick
			addTick(this.tickGranule);
		}
	}
}
