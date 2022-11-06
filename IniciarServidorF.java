import java.io.*;
import java.net.*;

public class IniciarServidorF implements Runnable {
    Log log;
    int puerto = 1981;
    DistanceVector dv;
    
    public IniciarServidorF(Log log, DistanceVector dv, int puerto) {
        this.log = log;
        this.dv = dv;
        this.puerto = puerto;
    }

    public void run() {
        try {
            log.print(" Servidor Envia del Puerto " + puerto);
            ServerSocket socket = new ServerSocket(puerto);
            while (true) {
                Server2 server = new Server2(socket.accept(), dv, log);
                new Thread(server).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
