public class ClientManager implements Runnable {
    Log log;
    DistanceVector dVector;

    public ClientManager(Log log, DistanceVector dVector) {
        this.log = log;
        this.dVector = dVector;
    }

    public void run() {
        try {
            for (String vecino : dVector.ipVecinos.keySet()) {
                if (!dVector.info.servers.get(vecino)) {
                    String ip = this.dVector.ipVecinos.get(vecino).get("ip");
                    this.log.print(" Conexion con: " + vecino);
                    Cliente cliente = new Cliente(ip, 9080, this.dVector, this.log, vecino);
                    new Thread(cliente).start();
                }
            }

            while (true) {
                Thread.sleep(10 * 1000);
                for (String vecino : dVector.ipVecinos.keySet()) {
                    if (dVector.info.clientes.get(vecino) && !dVector.info.servers.get(vecino)) {
                        String ip = this.dVector.ipVecinos.get(vecino).get("ip");
                        this.log.print(" Reconexion con: " + vecino);
                        Cliente client = new Cliente(ip, 9080, this.dVector, this.log, vecino);
                        new Thread(client).start();
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
