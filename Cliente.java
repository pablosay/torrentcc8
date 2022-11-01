
import java.net.Socket;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class Cliente implements Runnable {

    protected Socket socket;
    protected Thread runningThread;
    protected String ip;
    protected Integer puerto;
    protected DistanceVector dv;
    protected Log log;
    protected String vecino;
    protected Integer retransmitir;

    public Cliente(String ip, Integer puerto, DistanceVector dv, Log log, String vecino, Integer retransmitir) {
        this.ip = ip;
        this.puerto = puerto;
        this.dv = dv;
        this.log = log;
        this.vecino = vecino;
        this.retransmitir = retransmitir;
    }

    public void run() {
        synchronized (this) {
            this.runningThread = Thread.currentThread();
        }
        try {
            try {
                this.socket = new Socket(this.ip, this.puerto);
            } catch (Exception e) {
            }
            if (this.socket != null) {
                PrintWriter outSocket = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader inSocket = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                this.log.print(" HELLO: " + this.vecino);
                String mensaje = "From:" + dv.esteNodo;
                mensaje += "\nType:HELLO";
                outSocket.println(mensaje);
                Boolean welcome = false;
                String vecino = "";
                /* estoy esperando el welcome */
                while (!welcome) {
                    String mensajeServidor = inSocket.readLine();
                    this.log.print(" Servidor (" + this.vecino + ") " + mensajeServidor);
                    if (mensajeServidor.contains("From")) {
                        String[] tokens = mensajeServidor.split(":");
                        vecino = tokens[1];
                        continue;
                    } else if (mensajeServidor.contains("Type")) {
                        String[] tokens = mensajeServidor.split(":");
                        if (tokens[1].contains("WELCOME")) {
                            welcome = true;
                            this.dv.updateservers(vecino, true);
                        }
                    }
                }

                /* Enviar el distance vector */
                mensaje = "From:" + dv.esteNodo;
                mensaje += "\nType:DV";
                mensaje += "\nLen:" + (dv.vectoresDeDistancia.get(dv.esteNodo).size() - 1);
                for (String vecinoi : dv.vectoresDeDistancia.get(dv.esteNodo).keySet()) {
                    if (!vecinoi.equals(dv.esteNodo)) {
                        mensaje += "\n" + vecinoi + ":"
                                + dv.vectoresDeDistancia.get(dv.esteNodo).get(vecinoi).costo;
                    }
                }
                outSocket.println(mensaje);
                this.log.print(" DV enviado a " + this.vecino);
                /* actualizar que ya le envie el distance vector */
                dv.updateinformado(this.vecino, true);

                /* Retransmitir informacion cada x segundos */
                while (true) {
                    Thread.sleep(this.retransmitir * 1000);
                    if (dv.cambiosDV) {
                        log.print(" Cambios DV: " + dv.cambiosDV);
                    } else {
                        log.print(" No hay cambios DV " + dv.cambiosDV);
                    }
                    if (dv.clientes.get(this.vecino)) {
                        if (dv.servers.get(this.vecino)) {
                            if (dv.cambiosDV && !dv.informado.get(this.vecino)) {
                                mensaje = "From:" + dv.esteNodo;
                                mensaje += "\nType:DV";
                                mensaje += "\nLen:" + (dv.vectoresDeDistancia.get(dv.esteNodo).size() - 1);
                                for (String vecinoi : dv.vectoresDeDistancia.get(dv.esteNodo).keySet()) {
                                    if (!vecinoi.equals(dv.esteNodo)) {
                                        mensaje += "\n" + vecinoi + ":"
                                                + dv.vectoresDeDistancia.get(dv.esteNodo).get(vecinoi).costo;
                                    }
                                }
                                outSocket.println(mensaje);
                                this.log.print(" Se envio el DV" + this.vecino);
                                dv.updateinformado(this.vecino, true);
                            } else {
                                log.print(" Enviar KeepAlive a " + this.vecino);
                                mensaje = "From:" + dv.esteNodo;
                                mensaje += "\nType:KeepAlive";
                                outSocket.println(mensaje);
                            }
                        } else {
                            log.print(" No se puede enviar informacion a " + this.vecino);
                            this.dv.updateservers(this.vecino, false);
                            break;
                        }
                    } else {
                        log.print(" No se puede enviar informacion a (2) " + this.vecino);
                        this.dv.updateservers(this.vecino, false);
                        break;
                    }
                    /*
                     * Si todos los vecinos ya fueron informados, regresar a false la variable
                     * cambiosDV
                     */
                    Integer acumulador = 0;
                    for (Boolean status : dv.informado.values()) {
                        if (status) {
                            acumulador++;
                        }
                    }
                    if (acumulador == dv.informado.size()) {
                        dv.cambiosDV = false;
                    }
                }
            } else {
                this.log.print(" No se logro conectar con " + this.vecino);
                dv.updateinformado(this.vecino, true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}