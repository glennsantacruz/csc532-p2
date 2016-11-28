import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Objects;

public class Server {
	public final static int SERVERPORT = 16532; // Fall 16, CSC 532
	// protocol message for request
	public final static String REQUESTTICK = "reqServerTick";

	protected Clock clock = null;
	protected Thread clockThread = null;

	public Server(int tick, int msDelay) {
		this.clock = new Clock(tick, msDelay);
		this.clockThread = new Thread(this.clock);
		this.clockThread.start();
	}

	public void listen() {
		boolean isListening = true;
		ServerSocket servSock = null;

		// Open a new socket, listening for client connections
		try {
			servSock = new ServerSocket(Server.SERVERPORT);
			System.out.println( "server listening..." );

			while (isListening) {
				Socket clientSock = servSock.accept();
				// Send the request to a thread to handle, and go back to
				// listening immediately
				System.out.println( "server accepted client" );

				new ClientRequest(clientSock, this.clock);
			}
		} catch (IOException ie) {
			// must have been an error in creating the socket or elsewhere;
			// should stop the loop
			isListening = false;
			ie.printStackTrace(System.out);
		} finally {
			try {
				servSock.close();
			} catch (Throwable ignored) {
			}
		}
	}

	class ClientRequest extends Thread {
		DataInputStream inStream;
		DataOutputStream outStream;
		Socket clientSocket;
		Clock clock;

		ClientRequest(Socket clientSocket, Clock serverClock) {
			System.out.println( "ClientRequest constructor:");

			try {
				this.inStream = new DataInputStream(clientSocket.getInputStream());
				this.outStream = new DataOutputStream(clientSocket.getOutputStream());
				this.clock = serverClock;
				this.start();
			} catch (Throwable t) {
				try {
					clientSocket.close();
				} catch (Throwable ignored) {
				}
			}
		}

		public void run() {
			try {
				String request = inStream.readUTF();
				
				if (Objects.equals(Server.REQUESTTICK, request)) {
					// client is requesting our time; return it as quickly as
					// possible
					int tick = this.clock.getTick();
					outStream.writeInt( tick );
					System.out.println( "server sent client response: " + tick  );
				}
			} catch (Throwable ignored) {
			}
		}
	}
}
