
public class ChristianTimeSim {

	public static void main(String[] args) {
		ClockThread ct = new ClockThread(Thread.currentThread());
		String serverLoc;
		
		if(args.length == 0) {
			System.out.println("You must have an argument for client/server");
			return;	
		}
		
		if(args[0].equals("server")) {
			if(args.length < 3) {
				System.out.println("You must have 3 argument for server");
				return;
			}
			System.out.println("Server option chosen");
			ct.isServer = true;
			try{
				ct.currTick = Integer.parseInt(args[1]);
				System.out.println("Current tick time set to " + ct.currTick);
			}
			catch(NumberFormatException e) {
				System.out.println("Second argument must be an integer");
				e.printStackTrace();
				return;
			}
			//Delay in tick
			try{
				ct.tickDelayMS = Integer.parseInt(args[2]);
			}
			catch(NumberFormatException e) {
				System.out.println("Third argument must be an integer");
				e.printStackTrace();
				return;
			}
		}
		else if(args[0].equals("client")) {
			if(args.length > 4) {
				System.out.println("You must have 3 argument for client");
				return;	
			}
			System.out.println("Client option chosen");
			ct.isServer = false;
			
			//Insert Server to connect to
			serverLoc = args[1];

			try{
				ct.currTick = Integer.parseInt(args[2]);
				System.out.println("Current tick time set to " + ct.currTick);
			}
			catch(NumberFormatException e) {
				System.out.println("Third argument must be an integer");
				e.printStackTrace();
				return;
			}
			//Delay in tick
			try{
				ct.tickDelayMS = Integer.parseInt(args[3]);
			}
			catch(NumberFormatException e) {
				System.out.println("Fourth argument must be an integer");
				e.printStackTrace();
				return;
			}
		}
		else {
			System.out.println("First argument must be either client or server.");
			return;
		}
		ct.start();
		if(ct.isServer) {
			//loop for replying to queries 
		}
		else {
			//loop for querying server
		}
		
	}

}
