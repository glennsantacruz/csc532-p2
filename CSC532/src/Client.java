import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

public class Client {
	protected Clock clock = null;
	protected Thread clockThread = null;
	protected String serverLocation = null;
	protected int lastServerTick = 0, lastClientTick = 0;

	public Client(String serverLoc, int tick, int msDelay) {
		this.clock = new Clock(tick, msDelay);
		this.clockThread = new Thread(this.clock);
		this.clockThread.start();
		this.serverLocation = serverLoc;
	}

	public void syncClock() {
		// making use of our own clock, we will synchronize on the object
		// and wait for it to notify us of each tick. Every 10th tick
		// (or greater, since we may lose granularity) of
		// our clock, we will observe the server and make clock adjustment
		boolean isRunning = true;
		int currTick = 0, lastTick = 0;
		while (isRunning) {
			System.out.println( "client sync - yield"  );
			// first we behave nice with others
			Thread.yield();

			// sync on the clock instance, so that we can wait for a tick
			synchronized (this.clock) {
				try {
					System.out.println( "client sync waiting on clock"  );
					this.clock.wait();
					currTick = this.clock.getTick();
					System.out.println( "client sync got tick " + currTick  );
				} catch (InterruptedException ignored) {
				}
			}
			if (currTick - lastTick >= 10) {
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

			System.out.println( "server response received"  );
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

		// the received server time should be adjusted according to the
		// calculated netDelay; since the serverTick will represent time
		// at the server prior to network transmission, we adjust the time
		// by adding the netDelay:
		srvTick += netDelay;

		// calculate an approximation of the perceived server msDelay, by
		// determining the server change interval, and our local change
		// interval.
		// the ratio of server to client can be applied to our local msDelay,
		// thus approximating the server msDelay
		int cliDelayMS = this.clock.getDelayMillis();
		int srvDelayMS = ((srvTick - lastServerTick) / (tickEnd - lastClientTick)) * cliDelayMS;

		// calculate a variance in ticks between server and client
		int tickVariance = (srvTick - tickEnd);

		// now we know an approximation of the server msDelay, which is a
		// threshold
		// to use in targeting our own msDelay adjustment; if we need to speed
		// up,
		// we should adjust to be faster than the server. Likewise, if we need
		// to
		// slow down, we should run slower than the server.
		int targetMS = 0;

		if (tickVariance > 1) {
			// server is ahead; we need to speed up, by decreasing our msDelay
			// to a value less than the perceived server msDelay.

			// the adjustment is made by dividing the perceived server delay by
			// the variance. Since the variance is a positive number, this has
			// the
			// effect of decreasing the perceived server delay, which we will
			// then
			// use as a reduction to the perceived delay, getting us our target:
			targetMS = srvDelayMS - (srvDelayMS / tickVariance);

		} else if (tickVariance < 1) {
			// server is behind; we need to slow down, by increasing our msDelay
			// to a value greater than the perceived server msDelay.

			// the adjustment is made by dividing the absolute value of our
			// variance
			// (since the variance is negative) by the perceived server delay;
			// similar to above, this will increase our delay beyond the server,
			// allowing the server to approach our time (since it will then be
			// running faster than us)

			// note also that we are subtracting a negative, which is desired
			targetMS = srvDelayMS - (tickVariance / srvDelayMS);

		} else if (tickVariance == 1 || tickVariance == -1) {
			// this is a special case, since we are either ahead or behind, but
			// only
			// slightly. we should adjust our targetMS to match the perceived
			// server,
			// with only a slight adjustment to the msDelay

			// note that this should be rare case, but we handle nonetheless
			targetMS = srvDelayMS + tickVariance;

		} else {
			// variance must be zero. clocks are currently in sync, but there's
			// a chance of
			// drift, so future adjustments could occur. no need to perform
			// actions here
			// other than logging to indicate we are in sync.
		}
		// now we adjust our own clock's msDelay, according to the above
		// observation:
		System.out.println( "client obs: targetMS " + targetMS + ", srvTick " + srvTick + ", clientTick " + tickEnd  );
		this.clock.setDelayMillis(targetMS);

		// store the last observed values, for use in the next observation
		this.lastServerTick = srvTick;
		this.lastClientTick = tickEnd;

	}
}