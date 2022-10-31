import java.net.ServerSocket;

public class IniciarServidorF implements Runnable {
    Log log;
    Integer puerto = 1981;
    ServerSocket socketServer;
    DistanceVector dv;
    
    public IniciarServidorF(Log log, DistanceVector dv, Integer puerto) {
        this.log = log;
        this.dv = dv;
        this.puerto = puerto;
    }

    public void run() {
        try {
            log.print("Servidor Forward " + this.puerto);
            socketServer = new ServerSocket(this.puerto);
            while (true) {
                try {
                    Server2 server = new Server2(this.socketServer.accept(), this.dv, this.log);
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
