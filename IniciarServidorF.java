import java.net.*;

public class IniciarServidorF implements Runnable {
    Log log;
    int puerto = 1981;
    ServerSocket socket;
    DistanceVector dv;
    public IniciarServidorF(Log log, DistanceVector dv, int puerto) {
        this.log = log;
        this.dv = dv;
        this.puerto = puerto;
    }
    public void run() {
        try {
            log.print(" Servidor Envia del Puerto " + this.puerto);
            socket = new ServerSocket(this.puerto);
            while (true) {
                try {
                    Server2 server = new Server2(this.socket.accept(), this.dv, this.log);
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
