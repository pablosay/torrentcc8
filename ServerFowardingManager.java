import java.io.*;
import java.net.*;

public class ServerFowardingManager implements Runnable {
    Log log;
    int puerto = 1981;
    DistanceVector dv;

    public ServerFowardingManager(Log log, DistanceVector dv, int puerto) {
        this.log = log;
        this.dv = dv;
        this.puerto = puerto;
    }

    public void run() {
        try {
            log.print(" Servidor Envia del Puerto " + puerto);
            ServerSocket socket = new ServerSocket(puerto);
            while (true) {
                ServerFowarding server = new ServerFowarding(socket.accept(), dv, log);
                new Thread(server).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
