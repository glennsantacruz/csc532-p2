import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;


public class Client {
	protected final int OBSERVE_DELAY = 10;
	protected Clock clock = null;
	protected Thread clockThread = null;
	protected String serverLocation = null;
	protected int lastServerTick = 0, lastClientTick = 0, lastNetDelay = 0;
	
	public Client(String serverLoc, int tick, int msDelay) {
		this.clock = new Clock(tick, msDelay);
		this.clockThread = new Thread(this.clock);
		this.clockThread.start();
		this.serverLocation = serverLoc;
	}

	public void syncClock() {
		// making use of our own clock, we will synchronize on the object
		// and wait for it to notify us of each tick. Every this.OBSERVE_DELAY tick
		// (or greater, since we may lose granularity) of
		// our clock, we will observe the server and make clock adjustment
		boolean isRunning = true;
		int currTick = 0, lastTick = 0;
		while (isRunning) {
			// first we behave nice with others
			Thread.yield();

			// sync on the clock instance, so that we can wait for a tick
			synchronized (this.clock) {
				try {
					this.clock.wait();
					currTick = this.clock.getTick();
					System.out.println( "client sync tick " + currTick  );
				} catch (InterruptedException ignored) {
				}
			}
			if (currTick - lastTick >= this.OBSERVE_DELAY) {
				System.out.println( "client sync observing " + currTick  );
				observeServer();
				lastTick = currTick;
			}
		}
	}

	protected void observeServer() {
		Socket cs = null;
		try {
			System.out.println( "client requesting from server"  );
			// setup socket to talk with server
			cs = new Socket(this.serverLocation, Server.SERVERPORT);
			DataInputStream dis = new DataInputStream(cs.getInputStream());
			DataOutputStream dos = new DataOutputStream(cs.getOutputStream());

			// make request to server for current time
			int tickBegin = this.clock.getTick();
			dos.writeUTF(Server.REQUESTTICK);
			int srvTick = dis.readInt();
			int tickEnd = this.clock.getTick();

			System.out.println( "server response " + srvTick + ",tickBegin " + tickBegin + ",tickEnd " + tickEnd  );
			// record observation and adjust as needed
			observeAdjust(srvTick, tickBegin, tickEnd);
		} catch (Throwable t) {
			// someting bad happened; cleanup
			try {
				cs.close();
			} catch (Throwable ignored) {
			}
		}
	}

	protected void observeAdjust(int srvTick, int tickBegin, int tickEnd) {
		// first we calculate the apparent network delay in the request,
		// as seen from our own clock perspective. the delay is halved, to
		// approximate the time involved in a single leg of the message request
		int netDelay = (tickEnd - tickBegin) / 2;
		
		// note that the current netDelay is added to the current server tick, as a correction
		// likewise, the prior netDelay is added to the prior server tick, as a correction
				 
		// calculate rate of change between server and client
		System.out.println( "  client obs: srvTick/netDelay,lastSrvTick/lastNetdelay " + srvTick + "/" + netDelay + "," + lastServerTick + "/" + lastNetDelay );
		double rate = (((double)srvTick + netDelay) - (lastServerTick + lastNetDelay)) / (tickEnd - lastClientTick);

		// calculate a variance in ticks between server and client
		int tickVariance = (srvTick - tickEnd);

		// our current delay
		int cliDelayMS = this.clock.getDelayMillis();

		// calculate an adjustment factor (by default 1, no change):
		double adj = 1;
		
		if( rate == 1 ) {
			if( tickVariance > 0 ) {			
				// server same rate, but ahead: slight speedup ( adj < 1 )
				adj = 0.99 - (tickVariance/(2*this.OBSERVE_DELAY * cliDelayMS));
			} else if( tickVariance < 0 ) {		
				// server same rate, but behind: slight slowdown (adj > 1)
				adj = 1.01 + (Math.abs(tickVariance)/(this.OBSERVE_DELAY * cliDelayMS));
			} else {							
				// server same rate, same tick: full convergence (best case)
				System.out.println("client obs: converged!!");
			}
		} else if( rate > 1 ) {
			if( tickVariance > 0 ) {			
				// server faster, but ahead: large speedup ( adj << 1 )
				adj = 1/rate;
			} else if( tickVariance < 0 ) {		
				// server faster, but behind: slight slowdown (adj > 1)
				adj = 1.01 + (Math.abs(tickVariance)/(2*this.OBSERVE_DELAY * cliDelayMS));
			} else {							
				// server faster, same tick: slight speedup (adj < 1)
				adj = 0.99 - (tickVariance/(2*this.OBSERVE_DELAY * cliDelayMS));
			}			
		} else if( rate < 1 ) {
			if( tickVariance > 0 ) {			
				// server slower, but ahead: slight speedup (adj < 1)
				adj = 0.99 + (this.OBSERVE_DELAY/(rate * cliDelayMS));
			} else if( tickVariance < 0 ) {		
				// server slower, but behind: slight slowdown (adj > 1)
				//adj = 1.01 + (double)Math.abs(tickVariance)/(.5*this.OBSERVE_DELAY * cliDelayMS);
				adj = (double)1/Math.abs(rate);
			} else {							
				// server slower, but matched: no action. This situation will tend
				// to resolve itself as part of the algorithm
			}
		}
		// the adjusted target delay
		int targetMS =  (int)(cliDelayMS * adj);
		
		double d = cliDelayMS * adj;
		System.out.println( "adj: " + adj + ", cliDelayMs " + cliDelayMS + ", targetMS " + targetMS + ", d=" + d);
		
		// now we adjust our own clock's msDelay, according to the above
		// observation:
		System.out.println( "1 client obs: adj      " + adj + ", rate " + rate );
		System.out.println( "2 client obs: cliDelay " + cliDelayMS + ", variance " + tickVariance );
		System.out.println( "3 client obs: targetMS " + targetMS + ", srvTick " + srvTick + ", clientTick " + tickEnd  );
		this.clock.setDelayMillis(targetMS);

		// store the last observed values, for use in the next observation
		this.lastServerTick = srvTick;
		this.lastClientTick = tickEnd;
		this.lastNetDelay = netDelay;
	}
}