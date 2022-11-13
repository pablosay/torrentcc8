import java.net.Socket;
import java.io.DataInputStream;
import java.io.DataOutputStream;

public class Cliente implements Runnable {

    protected Socket socket;
    protected Thread runningThread;
    protected String ip;
    protected Integer puerto;
    protected DistanceVector dv;
    protected Log log;
    protected String vecino;
    protected int tiempoT;

    public Cliente(String ip, Integer puerto, DistanceVector dv, Log log, String vecino, int tiempoT) {
        this.ip = ip;
        this.puerto = puerto;
        this.dv = dv;
        this.log = log;
        this.vecino = vecino;
        this.tiempoT = tiempoT;
    }

    public void run() {
        // Sincronizar el thread
        synchronized (this) {
            this.runningThread = Thread.currentThread();
        }
        try {
            try {
                // Intentar la conexion con el server
                this.socket = new Socket(this.ip, this.puerto);
            } catch (Exception e) {
            }
            if (this.socket != null) {
                // DataInput y DataOutput
                DataInputStream in = new DataInputStream(this.socket.getInputStream());
                DataOutputStream out = new DataOutputStream(this.socket.getOutputStream());
                this.log.print(" HELLO a: " + this.vecino);
                // Mensaje a a enviar al servidor
                String mensaje = "From:" + dv.esteNodo + "\nType:HELLO";
                // Enviar mensaje
                out.writeUTF(mensaje);
                // Recibir mensaje del servidor
                String respuestaServidor = in.readUTF();
                // Imprimir en el log la respuesta del servidor
                this.log.print(" Servidor responde: " + respuestaServidor);
                // Tokeninzar el mensaje del servidor
                String[] mensajeServerTokenizado = respuestaServidor.split("\n");
                // Verificar que nos hayan enviado el welcome
                if (mensajeServerTokenizado[1].contains("WELCOME")) {
                    // Estamos escuchando al servidor del vecino
                    this.dv.updateservers(vecino, true);
                }
                // Enviamos por primera vez nuestro DV
                mensaje = "From:" + dv.esteNodo + "\nType:DV" + "\nLen:"
                        + (dv.vectoresDeDistancia.get(dv.esteNodo).size() - 1);
                for (String vecinoi : dv.vectoresDeDistancia.get(dv.esteNodo).keySet()) {
                    if (!vecinoi.equals(dv.esteNodo)) {
                        mensaje += "\n" + vecinoi + ":"
                                + dv.vectoresDeDistancia.get(dv.esteNodo).get(vecinoi).costo;
                    }
                }
                // Enviar mensaje
                out.writeUTF(mensaje);
                this.log.print(" DV enviado a " + this.vecino);
                dv.updateinformado(this.vecino, true);
                while (true) {
                    Thread.sleep(1000 * tiempoT);
                    if (dv.info.clientes.get(this.vecino)) {
                        if (dv.info.servers.get(this.vecino)) {
                            if (dv.cambiosDV && !dv.info.informado.get(this.vecino)) {
                                mensaje = "From:" + dv.esteNodo + "\nType:DV" + "\nLen:"
                                        + (dv.vectoresDeDistancia.get(dv.esteNodo).size() - 1);
                                for (String vecinoi : dv.vectoresDeDistancia.get(dv.esteNodo).keySet()) {
                                    if (!vecinoi.equals(dv.esteNodo)) {
                                        mensaje = "\n" + vecinoi + ":"
                                                + dv.vectoresDeDistancia.get(dv.esteNodo).get(vecinoi).costo;
                                    }
                                }
                                out.writeUTF(mensaje);
                                this.log.print(" Se envio el DV" + this.vecino);
                                dv.updateinformado(this.vecino, true);
                            } else {
                                log.print(" KeepAlive a " + this.vecino);
                                mensaje = "From:" + dv.esteNodo;
                                mensaje += "\nType:KeepAlive";
                                out.writeUTF(mensaje);
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
                    int acumulador = 0;
                    for (Boolean status : dv.info.informado.values()) {
                        if (status) {
                            acumulador++;
                        }
                    }
                    if (acumulador == dv.info.informado.size()) {
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