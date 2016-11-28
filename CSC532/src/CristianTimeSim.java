import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.*;

public class CristianTimeSim {
	static final LogManager logMan = LogManager.getLogManager();
	static final Logger LOGGER = Logger.getLogger( CristianTimeSim.class.getName() );
	static FileHandler fh;
	
	static{
		try{
			logMan.readConfiguration(new FileInputStream("./res/logging.properties"));
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		boolean isServer = false;
		int ticks = 0;
		int msDelay = 0;
		String serverLoc = "";

		System.out.println( "arguments:" + args.length );
		
		if (args.length == 0) {
			System.out.println("You must have an argument for client/server");
			return;
		}

		if (args[0].equals("server")) {
			if (args.length != 3) {
				System.out.println("You must have 3 argument for server");
				return;
			}
			System.out.println("Server option chosen");
			isServer = true;
			try {
				ticks = Integer.parseInt(args[1]);
				System.out.println("Current tick time set to " + ticks);
			} catch (NumberFormatException e) {
				System.out.println("Second argument must be an integer");
				e.printStackTrace();
				return;
			}
			// delay in milliseconds
			try {
				msDelay = Integer.parseInt(args[2]);
			} catch (NumberFormatException e) {
				System.out.println("Third argument must be an integer");
				e.printStackTrace();
				return;
			}
		} else if (args[0].equals("client")) {
			if (args.length != 4) {
				System.out.println("You must have 3 argument for client");
				return;
			}
			System.out.println("Client option chosen");
			isServer = false;

			// Insert Server to connect to
			serverLoc = args[1];

			try {
				ticks = Integer.parseInt(args[2]);
				System.out.println("Current tick time set to " + ticks);
			} catch (NumberFormatException e) {
				System.out.println("Third argument must be an integer");
				e.printStackTrace();
				return;
			}
			// Delay in tick
			try {
				msDelay = Integer.parseInt(args[3]);
			} catch (NumberFormatException e) {
				System.out.println("Fourth argument must be an integer");
				e.printStackTrace();
				return;
			}
		} else {
			System.out.println("First argument must be either client or server.");
			return;
		}

		// at this point, we have enough information to run either mode:
		if (isServer) {
			try {
				fh = new FileHandler("Server.log");
				CristianTimeSim.LOGGER.addHandler(fh);
				SimpleFormatter sf = new SimpleFormatter();
				fh.setFormatter(sf);
				CristianTimeSim.LOGGER.setUseParentHandlers(false);
			} catch (SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Server srv = new Server(ticks, msDelay);
			srv.listen();
		} else {
			try {
				fh = new FileHandler("Client.log");
				CristianTimeSim.LOGGER.addHandler(fh);
				SimpleFormatter sf = new SimpleFormatter();
				fh.setFormatter(sf);
				CristianTimeSim.LOGGER.setUseParentHandlers(false);
			} catch (SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Client clt = new Client(serverLoc, ticks, msDelay);
			clt.syncClock();
		}
	}
}
