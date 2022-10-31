

import java.net.ServerSocket;

public class IniciarServidor implements Runnable {
    Log log;
    Integer puerto = 9080;
    Integer reconexion;
    ServerSocket socketServer;
    DistanceVector dv;

    public IniciarServidor(Log log, Integer reconexion, DistanceVector dv) {
        this.log = log;
        this.reconexion = reconexion;
        this.dv = dv;
    }

    public void run() {
        try {
            log.print("Servidor " + this.puerto);
            socketServer = new ServerSocket(this.puerto); // levantar el servidor en el puerto 9080
            while (true) {
                try {
                    Server server = new Server(this.socketServer.accept(), this.dv, this.reconexion, this.log);
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
