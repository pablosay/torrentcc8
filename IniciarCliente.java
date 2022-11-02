public class IniciarCliente implements Runnable {
    int retransmitir;
    Log log;
    DistanceVector dv;
    int puerto = 9080;

    public IniciarCliente(int retransmitir, Log log, DistanceVector dv) {
        this.retransmitir = retransmitir;
        this.log = log;
        this.dv = dv;
    }

    public void run() {
        try {
            for (String vecino : dv.ipVecinos.keySet()) {
                if (!dv.servers.get(vecino)) {
                    String ip = this.dv.ipVecinos.get(vecino).get("ip");
                    this.log.print(" Primera conexion con " + vecino);
                    Cliente cliente = new Cliente(ip, this.puerto, this.dv, this.log, vecino, this.retransmitir);
                    new Thread(cliente).start();
                }
            }

            while (true) {
                Thread.sleep(10 * 1000);
                for (String vecino : dv.ipVecinos.keySet()) {
                    if (dv.clientes.get(vecino) && !dv.servers.get(vecino)) {
                        String ip = this.dv.ipVecinos.get(vecino).get("ip");
                        this.log.print(" Reconexion con " + vecino);
                        Cliente client = new Cliente(ip, this.puerto, this.dv, this.log, vecino, this.retransmitir);
                        new Thread(client).start();
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
