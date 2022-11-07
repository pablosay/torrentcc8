
import java.net.ServerSocket;

public class ServerManager implements Runnable {
    Log log;
    int port = 9080;
    int reconectar;
    ServerSocket socketServer;
    DistanceVector dVector;

    public ServerManager(Log log, DistanceVector dVector) {
        this.log = log;
        this.dVector = dVector;
    }

    public void run() {
        try {
            log.print(" Server corriendo en el puerto " + this.port);
            socketServer = new ServerSocket(this.port);
            while (true) {
                try {
                    Server server = new Server(this.socketServer.accept(), this.dVector, this.log);
                    new Thread(server).start();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
