import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

public class Server2 implements Runnable {
    protected Socket socket;
    protected DistanceVector dv;
    protected Log log;
    protected Integer puerto = 1981;
    protected HashMap<Integer, String> archivo = new HashMap<Integer, String>();
    protected Integer sizeArchivoLocal = 0;
    protected String mensaje = "";

    public Server2(Socket socket, DistanceVector dv, Log log) {
        this.socket = socket;
        this.dv = dv;
        this.log = log;
    }

    public void run() {
        try {
            BufferedReader inSocket = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String de = "";
            String para = "";
            while (true) {
                Boolean leerEncabezado = false;
                while (!leerEncabezado) {
                    String mensajeCliente = inSocket.readLine();
                    if (mensajeCliente == null) {
                        break;
                    }
                    this.log.print("Cliente solicita " + mensajeCliente);
                    String[] tokens = mensajeCliente.split(":");
                    if (mensajeCliente.contains("From:")) {
                        de = tokens[1].trim();
                        continue;
                    } else if (mensajeCliente.contains("To")) {
                        para = tokens[1].trim();
                        leerEncabezado = true;
                    }
                }
                // Saber si el mensaje viene para mi o es para reenviar
                if (para.equals(dv.esteNodo)) {
                    String mensajeCliente = inSocket.readLine();
                    this.log.print("Cliente solicita " + mensajeCliente);
                    if (mensajeCliente != null) {
                        if (mensajeCliente.contains("Name:")) {
                            // es una petcion o una respuesta
                            String[] tokens = mensajeCliente.split(":");
                            String nombreArchivo = tokens[1].trim();
                            mensajeCliente = inSocket.readLine();
                            this.log.print("Cliente solicita " + mensajeCliente);

                            if (mensajeCliente.contains("Size")) {
                                // es una peticion
                                tokens = mensajeCliente.split(":");
                                String sizeArchivo = tokens[1].trim();
                                mensajeCliente = inSocket.readLine();
                                this.log.print("Cliente solicita " + mensajeCliente);
                                // saber cual es la ruta minima a partir del Distance Vector para enviar el
                                // mensaje
                                String rutaMinima = this.dv.vectoresDeDistancia.get(this.dv.esteNodo).get(de).conQuien;
                                this.log.print("Contestar peticion a " + de + " por medio de " + rutaMinima);
                                String ipRutaMinima = this.dv.ipVecinos.get(rutaMinima).get("ip");
                                // conectarse con el servidor para reenviar la informacion
                                fragmenatarArchivo(nombreArchivo); // chunks del archivo (archivo)
                                // abrir conexion con el destinatario
                                Socket socketReenvio = new Socket(ipRutaMinima, this.puerto);
                                PrintWriter outSocket = new PrintWriter(socketReenvio.getOutputStream(), true);
                                if (this.mensaje != "") {
                                    String mensaje = "";
                                    mensaje = "From:" + dv.esteNodo;
                                    mensaje += "\nTo:" + de;
                                    mensaje += "\nMsg:" + this.mensaje;
                                    mensaje += "\nEOF";
                                    outSocket.println(mensaje);
                                    outSocket.close();
                                    socketReenvio.close();
                                    break;
                                }
                                // enviar archivos alternados
                                Integer comienzo = 1;
                                Integer finaal = archivo.size();
                                boolean alternar = true;
                                Integer j = 1;
                                for (Integer i : archivo.keySet()) {
                                    if (alternar) {
                                        this.log.print("(" + j + ") Reenviar chunk " + i.toString() + " a " + de
                                                + " por medio de " + rutaMinima);
                                        String mensaje = "";
                                        mensaje = "From:" + this.dv.esteNodo;
                                        mensaje += "\nTo:" + de;
                                        mensaje += "\nName:" + nombreArchivo;
                                        mensaje += "\nData:" + this.archivo.get(comienzo);
                                        mensaje += "\nFrag:" + comienzo.toString();
                                        mensaje += "\nSize:" + sizeArchivo;
                                        mensaje += "\nEOF";
                                        outSocket.println(mensaje);
                                        alternar = false;
                                        comienzo++;
                                    } else {
                                        this.log.print("(" + j + ") Reenviar chunk " + i.toString() + " a " + de
                                                + " por medio de " + rutaMinima);
                                        String mensaje = "";
                                        mensaje = "From:" + this.dv.esteNodo;
                                        mensaje += "\nTo:" + de;
                                        mensaje += "\nName:" + nombreArchivo;
                                        mensaje += "\nData:" + this.archivo.get(finaal);
                                        mensaje += "\nFrag:" + finaal.toString();
                                        mensaje += "\nSize:" + sizeArchivo;
                                        mensaje += "\nEOF";
                                        outSocket.println(mensaje);
                                        alternar = true;
                                        finaal--;
                                    }
                                    j++;
                                }
                                // cerrar conexion con el destinatario
                                outSocket.close();
                                socketReenvio.close();
                                break;
                            } else {
                                // es una respuesta
                                tokens = mensajeCliente.split(":");
                                String dataArchivo = tokens[1].trim();
                                // leer el numero de frag
                                mensajeCliente = inSocket.readLine();
                                this.log.print("Cliente solicita " + mensajeCliente);
                                tokens = mensajeCliente.split(":");
                                String fragArchivo = tokens[1].trim();
                                // leer el size
                                mensajeCliente = inSocket.readLine();
                                this.log.print("Cliente solicita " + mensajeCliente);
                                tokens = mensajeCliente.split(":");
                                String sizeArchivo = tokens[1].trim();
                                // leer eof
                                mensajeCliente = inSocket.readLine();
                                this.log.print("Cliente solicita " + mensajeCliente);
                                // agregar el fragmento a mi temporal
                                archivo.put(Integer.parseInt(fragArchivo), dataArchivo);
                                // darle valor a sizeArchivoLocal
                                sizeArchivoLocal += dataArchivo.length() / 2;
                                if (sizeArchivoLocal >= Integer.parseInt(sizeArchivo)) {
                                    this.log.print(archivo.size() + " chunks recibidos de archivo " + nombreArchivo
                                            + " de " + de + " para mi ");
                                    String archivoStr = "";
                                    for (Integer i : archivo.keySet()) {
                                        archivoStr = archivoStr + archivo.get(i); // obtener todos los chunks
                                    }
                                    // System.out.println("archivo : " + archivoStr);
                                    byte[] archivo = hexStringToByteArray(archivoStr);

                                    Path path = Paths.get(nombreArchivo);
                                    String file = path.getFileName().toString();

                                    File archivoFile = new File("./Recibidos/" + file);
                                    OutputStream os = new FileOutputStream(archivoFile);
                                    os.write(archivo);
                                    os.close();
                                    this.log.print("Archivo guardado : " + nombreArchivo);
                                    break;
                                }
                            }
                        } else if (mensajeCliente.contains("Msg:")) {
                            // si es respuesta de error
                            String[] tokens = mensajeCliente.split(":");
                            String msg = tokens[1].trim();
                            mensajeCliente = inSocket.readLine();
                            this.log.print("Cliente solicita " + mensajeCliente);
                            this.log.print("Ocurrion un error al recibir el archivo: " + msg);
                            break;
                        } else {
                            log.print("El cliente " + de + " envio una peticion incorrecta");
                            break;
                        }
                    } else {
                        log.print("El cliente " + de + " mando null");
                        break;
                    }
                } else {
                    // Area del reenvio
                    String mensajeCliente = inSocket.readLine();
                    this.log.print("Cliente solicita " + mensajeCliente);
                    if (mensajeCliente != null) {
                        if (mensajeCliente.contains("Name:")) {
                            // es una petcion o una respuesta
                            String[] tokens = mensajeCliente.split(":");
                            String nombreArchivo = tokens[1].trim();
                            mensajeCliente = inSocket.readLine();
                            this.log.print("Cliente solicita " + mensajeCliente);

                            if (mensajeCliente.contains("Size")) {
                                // es una peticion
                                tokens = mensajeCliente.split(":");
                                String tamanioArchivo = tokens[1].trim();
                                mensajeCliente = inSocket.readLine();
                                this.log.print("Cliente solicita " + mensajeCliente);
                                // saber cual es la ruta minima a partir del Distance Vector para enviar el
                                // mensaje
                                String rutaMinima = this.dv.vectoresDeDistancia.get(this.dv.esteNodo)
                                        .get(para).conQuien;
                                this.log.print("Reenviar peticion a " + para + " por medio de " + rutaMinima);
                                String ipRutaMinima = this.dv.ipVecinos.get(rutaMinima).get("ip");
                                // conectarse con el servidor para reenviar la informacion
                                Socket socketReenvio = new Socket(ipRutaMinima, this.puerto);
                                PrintWriter outSocket = new PrintWriter(socketReenvio.getOutputStream(), true);
                                String mensaje = "";
                                mensaje = "From:" + de;
                                mensaje += "\nTo:" + para;
                                mensaje += "\nName:" + nombreArchivo;
                                mensaje += "\nSize:" + tamanioArchivo;
                                mensaje += "\nEOF";
                                outSocket.println(mensaje);
                                outSocket.close();
                                socketReenvio.close();
                                break;
                            } else {
                                // es una respuesta
                                tokens = mensajeCliente.split(":");
                                String dataArchivo = tokens[1].trim();
                                // leer el numero de frag
                                mensajeCliente = inSocket.readLine();
                                this.log.print("Cliente solicita " + mensajeCliente);
                                tokens = mensajeCliente.split(":");
                                String fragArchivo = tokens[1].trim();
                                // leer el size
                                mensajeCliente = inSocket.readLine();
                                this.log.print("Cliente solicita " + mensajeCliente);
                                tokens = mensajeCliente.split(":");
                                String sizeArchivo = tokens[1].trim();
                                // leer eof
                                mensajeCliente = inSocket.readLine();
                                this.log.print("Cliente solicita " + mensajeCliente);
                                // agregar el fragmento a mi temporal
                                archivo.put(Integer.parseInt(fragArchivo), dataArchivo);
                                // darle valor a sizeArchivoLocal
                                sizeArchivoLocal += dataArchivo.length() / 2;
                                if (sizeArchivoLocal >= Integer.parseInt(sizeArchivo)) {
                                    String rutaMinima = this.dv.vectoresDeDistancia.get(this.dv.esteNodo)
                                            .get(para).conQuien;
                                    this.log.print("Reenviar archivo " + nombreArchivo + " de " + archivo.size()
                                            + " chunks a " + para + " por medio de " + rutaMinima);
                                    String ipRutaMinima = this.dv.ipVecinos.get(rutaMinima).get("ip");
                                    Socket socketReenvio = new Socket(ipRutaMinima, this.puerto);
                                    PrintWriter outSocket = new PrintWriter(socketReenvio.getOutputStream(), true);
                                    Integer index = 1;
                                    for (Integer fragmento : this.archivo.keySet()) {
                                        this.log.print("(" + index + ") Reenviar chunk " + fragmento + " a " + para
                                                + " por medio de " + rutaMinima);
                                        String mensaje = "";
                                        mensaje = "From:" + de;
                                        mensaje += "\nTo:" + para;
                                        mensaje += "\nName:" + nombreArchivo;
                                        mensaje += "\nData:" + this.archivo.get(fragmento);
                                        mensaje += "\nFrag:" + fragmento;
                                        mensaje += "\nSize:" + sizeArchivo;
                                        mensaje += "\nEOF";
                                        outSocket.println(mensaje);
                                        index++;
                                    }
                                    outSocket.close();
                                    socketReenvio.close();
                                    break;
                                }
                            }
                        } else if (mensajeCliente.contains("Msg:")) {
                            // si es respuesta de error
                            String[] tokens = mensajeCliente.split(":");
                            String msg = tokens[1].trim();
                            mensajeCliente = inSocket.readLine();
                            this.log.print("Cliente solicita " + mensajeCliente);
                            // saber cual es la ruta minima a partir del Distance Vector para enviar el
                            // mensaje
                            String rutaMinima = this.dv.vectoresDeDistancia.get(this.dv.esteNodo).get(para).conQuien;
                            this.log.print("Reenviar mensaje a " + para + " por medio de " + rutaMinima);
                            String ipRutaMinima = this.dv.ipVecinos.get(rutaMinima).get("ip");
                            /* Conectarme al servidor forward del vecino */
                            Socket socketReenvio = new Socket(ipRutaMinima, this.puerto);
                            PrintWriter outSocket = new PrintWriter(socketReenvio.getOutputStream(), true);
                            /* Reenviar el mensaje que va para ese vecino */
                            String mensaje = "";
                            mensaje = "From:" + de;
                            mensaje += "\nTo:" + para;
                            mensaje += "\nMsg:" + msg;
                            mensaje += "\nEOF";
                            outSocket.println(mensaje);
                            outSocket.close();
                            socketReenvio.close();
                            break;
                        } else {
                            log.print("El cliente " + de + " envio una peticion incorrecta");
                            break;
                        }
                    } else {
                        log.print("El cliente " + de + " mando null");
                        break;
                    }

                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void fragmenatarArchivo(String nombreArchivo) {
        try {
            File file = new File("./Enviados/" + nombreArchivo);
            byte[] arreglo = new byte[256];
            int tamano = 0;
            InputStream is = new FileInputStream(file);
            this.archivo = new HashMap<Integer, String>();
            int indiceChunk = 1;
            while ((tamano = is.read(arreglo)) > 0) {
                byte[] arregloAux = new byte[tamano];
                for (int i = 0; i < tamano; i++) {
                    arregloAux[i] = arreglo[i];
                    // System.out.println(arregloAux[i]);
                }
                StringBuilder sb = new StringBuilder();
                for (byte b : arregloAux) { // pasar los bytes a un string manejable
                    sb.append(String.format("%02x", b));
                }
                // System.out.println(sb.toString());
                archivo.put(indiceChunk, sb.toString());
                indiceChunk++;
            }
            is.close();
        } catch (Exception e) {
            this.mensaje = "No se encontro el archivo solicitado";
            this.archivo = new HashMap<>();
        }
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }
}
