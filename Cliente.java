import java.net.Socket;
import java.io.DataInputStream;
import java.io.DataOutputStream;

public class Cliente implements Runnable {

    protected Socket socket;
    protected Thread runningThread;
    protected String ip;
    protected Integer puerto;
    protected DistanceVector distanceVector;
    protected Log log;
    protected String vecino;
    protected int tiempoT;

    public Cliente(String ip, Integer puerto, DistanceVector distanceVector, Log log, String vecino, int tiempoT) {
        this.ip = ip;
        this.puerto = puerto;
        this.distanceVector = distanceVector;
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
                String mensaje = "From:" + distanceVector.esteNodo + "\nType:HELLO";
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
                    this.distanceVector.info.servers.put(vecino, true);
                }
                // Enviamos por primera vez nuestro distanceVector
                mensaje = "From:" + distanceVector.esteNodo + "\nType:DV" + "\nLen:"
                        + (distanceVector.vectoresDeDistancia.get(distanceVector.esteNodo).size() - 1);
                for (String vecinoi : distanceVector.vectoresDeDistancia.get(distanceVector.esteNodo).keySet()) {
                    if (!vecinoi.equals(distanceVector.esteNodo)) {
                        mensaje += "\n" + vecinoi + ":"
                                + distanceVector.vectoresDeDistancia.get(distanceVector.esteNodo).get(vecinoi).costo;
                    }
                }
                // Enviar mensaje
                out.writeUTF(mensaje);
                this.log.print(" distanceVector enviado a " + this.vecino);
                distanceVector.info.informado.put(this.vecino, true);
                while (true) {
                    // Realizar el envio cada tiempo T
                    Thread.sleep(1000 * tiempoT);
                    // Si el vecino esta informadio
                    if (distanceVector.info.clientes.get(this.vecino)) {
                        // Si estamos conectados al servidor
                        if (distanceVector.info.servers.get(this.vecino)) {
                            // Si hubo cambio y esta no esta informado el vecino
                            if (distanceVector.cambio && !distanceVector.info.informado.get(this.vecino)) {
                                mensaje = "From:" + distanceVector.esteNodo + "\nType:DV" + "\nLen:"
                                        + (distanceVector.vectoresDeDistancia.get(distanceVector.esteNodo).size() - 1);
                                for (String vecinoi : distanceVector.vectoresDeDistancia.get(distanceVector.esteNodo)
                                        .keySet()) {
                                    if (!vecinoi.equals(distanceVector.esteNodo)) {
                                        mensaje = "\n" + vecinoi + ":"
                                                + distanceVector.vectoresDeDistancia.get(distanceVector.esteNodo)
                                                        .get(vecinoi).costo;
                                    }
                                }
                                // Enviamos el mensaje
                                out.writeUTF(mensaje);
                                this.log.print(" Se envio el DV a: " + this.vecino);
                                // Ya se marca como informado
                                distanceVector.info.informado.put(this.vecino, true);
                            } else {
                                // Mantiene abierta la conexion con KeepAlive
                                log.print(" KeepAlive a " + this.vecino);
                                mensaje = "From:" + distanceVector.esteNodo;
                                mensaje += "\nType:KeepAlive";
                                out.writeUTF(mensaje);
                            }
                        } else {
                            // No se puede conectar con el servidor
                            log.print(" No se puede enviar informacion al servidor de  " + this.vecino);
                            this.distanceVector.info.servers.put(vecino, false);
                            break;
                        }
                    } else {
                        log.print(" No se puede enviar informacion al servidor de " + this.vecino);
                        this.distanceVector.info.servers.put(vecino, false);
                        break;
                    }
                    int contadorVecinosInformados = 0;
                    for (Boolean status : distanceVector.info.informado.values()) {
                        if (status) {
                            contadorVecinosInformados++;
                        }
                    }
                    if (contadorVecinosInformados == distanceVector.info.informado.size()) {
                        distanceVector.cambio = false;
                        // Como no hay cambios seguimos recorriendo el loop
                    }
                }
            } else {
                this.log.print(" No se logro conectar con " + this.vecino);
                distanceVector.info.informado.put(this.vecino, true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}