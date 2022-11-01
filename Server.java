
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class Server implements Runnable {

    protected Socket socket;
    protected DistanceVector dv;
    protected Integer reconexion;
    protected Log log;
    protected String vecino;

    public Server(Socket socket, DistanceVector dv, Integer reconexion, Log log) {
        this.socket = socket;
        this.dv = dv;
        this.reconexion = reconexion;
        this.log = log;
    }

    public void run() {
        try {
            PrintWriter outSocket = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader inSocket = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            while (true) {
                // Recibir mensaje del cliente
                String mensajeCliente = inSocket.readLine();
                if (mensajeCliente == null) {
                    break;
                }
                // Imprimir mensaje del cliente al log
                this.log.print(mensajeCliente);
                // Verificar si tiene un From
                if (mensajeCliente.contains("From:")) {
                    String[] tokens = mensajeCliente.split(":");
                    this.vecino = tokens[1];
                    continue;
                } else if (mensajeCliente.contains("Type")) {
                    String[] tokens = mensajeCliente.split(":");
                    String type = tokens[1];
                    if (type.contains("HELLO")) {
                        String test = "From:" + dv.esteNodo;
                        test += "\n" + "Type:WELCOME";
                        outSocket.println(test);
                        /* Agregar al pool de clientes */
                        this.dv.updateclientes(this.vecino, true);
                    } else if (type.contains("DV")) {
                        boolean leerDV = false;
                        HashMap<String, String> datos = new HashMap<String, String>();
                        Integer len = 0;
                        Integer locallen = 1;
                        while (!leerDV) {
                            mensajeCliente = inSocket.readLine();
                            this.log.print("Cliente solicita " + mensajeCliente);
                            if (mensajeCliente.contains("Len")) {
                                tokens = mensajeCliente.split(":");
                                len = Integer.parseInt(tokens[1]);
                            } else {
                                tokens = mensajeCliente.split(":");
                                datos.put(tokens[0], tokens[1]);
                                if (locallen == len) {
                                    leerDV = true;
                                }
                                locallen++;
                            }
                        }
                        this.log.print(vecino + " envio -> " + datos);
                        dv.nuevaRuta(datos, vecino); // agregar la nueva ruta, para despues calcular
                        dv.calcular(vecino); // establecer los costos minimos nuevamente
                    } else if (type.contains("KeepAlive")) {
                        // No se hace nada en el keepalive, solo se hace print
                        this.log.print("KeepAlive de " + this.vecino);
                    }
                    continue;
                }
            }

            /* Si llega aqui se desconecto */
            this.log.print("Se perdio conexion con " + this.vecino);
            this.dv.updateclientes(this.vecino, false);
            this.dv.updateinformado(this.vecino, true);
            while (true) {
                Thread.sleep(this.reconexion * 1000);
                if (!this.dv.clientes.get(this.vecino)) {
                    log.print(this.vecino + " ya no se conecto");
                    this.dv.updateCostoVecino(this.vecino);
                    break;
                } else {
                    log.print(this.vecino + " se conecto de nuevo");
                    break;
                }
            }
        } catch (SocketException e) {

            if (e.toString().contains("Connection reset")) {
                /* Si entra a esta excepcion es por que se desconecto */
                try {
                    this.dv.updateclientes(this.vecino, false);
                    this.dv.updateinformado(this.vecino, true);
                    while (true) {
                        Thread.sleep(this.reconexion * 1000);
                        if (!this.dv.clientes.get(this.vecino)) {
                            log.print(this.vecino + " ya no se conecto");
                            this.dv.updateCostoVecino(this.vecino);
                            break;
                        } else {
                            log.print(this.vecino + " se conecto de nuevo");
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
