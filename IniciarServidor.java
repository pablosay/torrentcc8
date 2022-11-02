

import java.net.ServerSocket;

public class IniciarServidor implements Runnable {
    Log log;
    int port = 9080;
    int reconectar;
    ServerSocket socketServer;
    DistanceVector dVector;

    public IniciarServidor(Log log, int reconectar, DistanceVector dVector) {
        this.log = log;
        this.reconectar = reconectar;
        this.dVector = dVector;
    }

    public void run() {
        try {
            log.print("Servidor " + this.port);
            socketServer = new ServerSocket(this.port);
            while (true) {
                try {
                    Server server = new Server(this.socketServer.accept(), this.dVector, this.reconectar, this.log);
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
