public class IniciarCliente implements Runnable {
    int reconectar;
    Log log;
    DistanceVector dVector;
    int port = 9080;

    public IniciarCliente(int reconectar, Log log, DistanceVector dVector) {
        this.reconectar = reconectar;
        this.log = log;
        this.dVector = dVector;
    }

    public void run() {
        try {
            for (String vecino : dVector.ipVecinos.keySet()) {
                if (!dVector.servers.get(vecino)) {
                    String ip = this.dVector.ipVecinos.get(vecino).get("ip");
                    this.log.print(" Primera conexion con " + vecino);
                    Cliente cliente = new Cliente(ip, this.port, this.dVector, this.log, vecino, this.reconectar);
                    new Thread(cliente).start();
                }
            }

            while (true) {
                Thread.sleep(10 * 1000);
                for (String vecino : dVector.ipVecinos.keySet()) {
                    if (dVector.clientes.get(vecino) && !dVector.servers.get(vecino)) {
                        String ip = this.dVector.ipVecinos.get(vecino).get("ip");
                        this.log.print(" Reconexion con " + vecino);
                        Cliente client = new Cliente(ip, this.port, this.dVector, this.log, vecino, this.reconectar);
                        new Thread(client).start();
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
