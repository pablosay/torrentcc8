
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.io.DataInputStream;
import java.io.DataOutputStream;

public class Server implements Runnable {

    protected Socket socket;
    protected DistanceVector distanceVector;
    protected Log log;
    protected String vecino;
    protected int tiempoU;

    public Server(Socket socket, DistanceVector distanceVector, Log log, int tiempoU) {
        this.socket = socket;
        this.distanceVector = distanceVector;
        this.log = log;
        this.tiempoU = tiempoU;
    }

    public void run() {
        try {
            DataInputStream in = new DataInputStream(this.socket.getInputStream());
            DataOutputStream out = new DataOutputStream(this.socket.getOutputStream());
            while (true) {
                // Recibir mensaje del cliente
                try {
                    String mensajeCliente = in.readUTF();
                    // Imprimir mensaje del cliente al log
                    String[] mensajeClienteTokenizado = mensajeCliente.split("\n");
                    // Imprimir en el log el mensaje del cliente
                    for (int i = 0; i < mensajeClienteTokenizado.length; i++) {
                        this.log.print(" " + mensajeClienteTokenizado[i]);
                    }
                    // Obtener la letra del vecino que nos manda el mensaje
                    String[] tokenDeMensajeFrom = mensajeClienteTokenizado[0].split(":");
                    if (tokenDeMensajeFrom.length >= 2) {
                        this.vecino = tokenDeMensajeFrom[1];
                        // Si es de dos lineas es un HELLO o un keep alive
                        if (mensajeClienteTokenizado.length == 2) {
                            // Si manda un HELLO hay que devolverle un WELCOME
                            if (mensajeClienteTokenizado[1].contains("HELLO")) {
                                String test = "From:" + distanceVector.esteNodo + "\n" + "Type:WELCOME";
                                out.writeUTF(test);
                                this.distanceVector.info.clientes.put(vecino, true);
                                // Si hay un keepAlive solo imprime
                            } else if (mensajeClienteTokenizado[1].contains("KeepAlive")) {
                            }
                            // Si el largo del mensaje tokenizado es mayor a 2 significa que hay cambios en
                            // los vectores de distancia.
                        } else {
                            // HashMap para agregar los datos de los vecinos y sus adyacentes
                            HashMap<String, String> adyacentesDeVecinoYSusCostos = new HashMap<String, String>();
                            // Recorremsos los vecinos del adyacente y los almacenamos
                            for (int i = 3; i < mensajeClienteTokenizado.length; i++) {
                                String[] tokensVecinoCosto = mensajeClienteTokenizado[i].split(":");
                                if (tokensVecinoCosto.length >= 2) {
                                    String adyacente = tokensVecinoCosto[0];
                                    String costo = tokensVecinoCosto[1];
                                    adyacentesDeVecinoYSusCostos.put(adyacente, costo);
                                }
                            }
                            // Imprimos los vectores que nos enviaron
                            this.log.print(
                                    " " + this.vecino + " envia el vector: "
                                            + adyacentesDeVecinoYSusCostos.toString());
                            // Agregar una nueva ruta
                            distanceVector.nuevaRuta(adyacentesDeVecinoYSusCostos, this.vecino);
                            // Calculamos la distancia mas corta al vecino
                            distanceVector.calcular(this.vecino);
                        }
                    }
                } catch (Exception e) {
                    break;
                }
            }
            this.log.print(" Se perdio conexion con " + this.vecino);
            this.distanceVector.info.clientes.put(vecino, false);
            this.distanceVector.info.informado.put(this.vecino, true);
            while (true) {
                // Esperamos tiempo U
                Thread.sleep(this.tiempoU * 1000);
                if (!this.distanceVector.info.clientes.get(this.vecino)) {
                    log.print(" " + this.vecino + " perdio conexion");
                    this.distanceVector.updateCostoVecino(this.vecino);
                    break;
                } else {
                    log.print(" " + this.vecino + " se conecto de nuevo");
                    break;
                }
            }
        } catch (SocketException e) {
            if (e.toString().contains("Connection reset")) {
                try {
                    this.distanceVector.info.clientes.put(this.vecino, false);
                    this.distanceVector.info.informado.put(this.vecino, true);
                    while (true) {
                        // Esperamos tiempo U
                        Thread.sleep(this.tiempoU * 1000);
                        // Si no esta conectado se le pone unreachable
                        if (!this.distanceVector.info.clientes.get(this.vecino)) {
                            log.print(" " + this.vecino + " ya no se conecto");
                            this.distanceVector.updateCostoVecino(this.vecino);
                            break;
                        } else {
                            // Se conecta de nuevo
                            log.print(" " + this.vecino + " se conecto de nuevo");
                            break;
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
