import java.io.*;
import java.util.*;
import java.net.Socket;
import java.nio.file.Paths;

public class ServerFowarding implements Runnable {
    Socket socket;
    DistanceVector distanceVector;
    Log log;
    boolean existe = true;
    Integer sizeArchivoLocal = 0;
    Integer puerto = 4500;
    LinkedList<String> hexDelArchivo = new LinkedList<String>();

    public ServerFowarding(Socket socket, DistanceVector distanceVector, Log log) {
        this.socket = socket;
        this.distanceVector = distanceVector;
        this.log = log;
    }

    public void run() {
        try {
            DataInputStream in = new DataInputStream(this.socket.getInputStream());
            String receptorDelMensaje = "";
            String destinatarioDelMensaje = "";
            String peticionRespuestaError = "";
            String mensajeDeOtroServidor = "";
            while (true) {
                try {
                    // Obtener mensaje
                    try {
                        mensajeDeOtroServidor = in.readUTF();
                    } catch (EOFException e) {
                        continue;
                    }
                    this.log.print(mensajeDeOtroServidor);
                    // Separarlo por lineas
                    String[] mensajeSeparadoPorNuevaLinea = mensajeDeOtroServidor.split("\n");

                    for (int i = 0; i < mensajeSeparadoPorNuevaLinea.length; i++) {
                        this.log.print(" " + mensajeSeparadoPorNuevaLinea[i]);
                    }
                    // Si el largo
                    if (mensajeSeparadoPorNuevaLinea.length == 5) {
                        peticionRespuestaError = "peticion";
                    } else if (mensajeSeparadoPorNuevaLinea.length == 7) {
                        peticionRespuestaError = "datos";
                        this.log.print("SI lee que van a caer datoss");
                    } else if (mensajeSeparadoPorNuevaLinea.length == 4) {
                        peticionRespuestaError = "error";
                    }
                    // Si es peticion
                    if (peticionRespuestaError.equals("peticion")) {
                        // Obtener el receptor del mensaje
                        receptorDelMensaje = mensajeSeparadoPorNuevaLinea[0].split(":")[1].trim();
                        // Obtener el destinatario
                        destinatarioDelMensaje = mensajeSeparadoPorNuevaLinea[1].split(":")[1].trim();
                        // Nombre del archivo
                        String nombreDelArchivo = mensajeSeparadoPorNuevaLinea[2].split(":")[1].trim();
                        // Largo del archivo
                        String largoDelArchivo = mensajeSeparadoPorNuevaLinea[3].split(":")[1].trim();
                        // Si es para este nodo solo contestamos la peticion
                        if (destinatarioDelMensaje.equals(distanceVector.esteNodo)) {
                            String ruta = distanceVector.vectoresDeDistancia.get(distanceVector.esteNodo)
                                    .get(receptorDelMensaje).conQuien;
                            log.print(" Contestar peticion a " + receptorDelMensaje + " por medio de " + ruta);
                            String ip = distanceVector.ipVecinos.get(ruta);
                            fragmentacionArchivo(nombreDelArchivo);
                            //Socket socketEnvioArchivo = new Socket(ip, this.puerto);
                            DataOutputStream out = new DataOutputStream(this.socket.getOutputStream());
                            if (!existe) { // Revisar si existe el archivo
                                String mensajeaenviar = "From:" + distanceVector.esteNodo + "\nTo:" + receptorDelMensaje
                                        + "\nMsg: Favor enviar nombre de archivo correcto \nEOF";
                                out.writeUTF(mensajeaenviar);
                                out.close();
                                //socketEnvioArchivo.close();
                                break;
                            } else {
                                for (int i = 0; i < hexDelArchivo.size(); i++) {
                                    log.print(" Reenviar chunk " + i + " a " + receptorDelMensaje
                                            + " por medio de "
                                            + ruta);
                                    String mensajeaenviar = "From:" + distanceVector.esteNodo + "\nTo:"
                                            + receptorDelMensaje
                                            + "\nName:"
                                            + nombreDelArchivo + "\nData:" + hexDelArchivo.get(i) + "\nFrag:" + i
                                            + "\nSize:"
                                            + largoDelArchivo + "\nEOF";
                                    out.writeUTF(mensajeaenviar);
                                }
                                out.close();
                                //socketEnvioArchivo.close();
                                break;
                            }
                            // Si no es para este nodo solo reenviamos la peticion
                        } else {
                            String ruta = distanceVector.vectoresDeDistancia.get(distanceVector.esteNodo)
                                    .get(destinatarioDelMensaje).conQuien;
                            log.print(" Reenviar peticion a " + destinatarioDelMensaje + " por medio de " + ruta);
                            String ip = distanceVector.ipVecinos.get(ruta);
                            Socket socketRenvioArchivo = new Socket(ip, this.puerto);
                            DataOutputStream out = new DataOutputStream(socketRenvioArchivo.getOutputStream());
                            DataInputStream in2 =  new DataInputStream(socketRenvioArchivo.getInputStream());
                            String mensajeenviar = "From:" + receptorDelMensaje + "\nTo:" + destinatarioDelMensaje
                                    + "\nName:" + nombreDelArchivo
                                    + "\nSize:" + largoDelArchivo + "\nEOF";
                            out.writeUTF(mensajeenviar);
                            out.flush();
                            String x;
                            while (true) {
                                try {
                                    x = in2.readUTF();
                                } catch (EOFException e) {
                                    continue;
                                }
                                String[] mensajeSeparadoPorNuevaLinea2 = x.split("\n");
                                
                                for (int i = 0; i < mensajeSeparadoPorNuevaLinea2.length; i++) {
                                    this.log.print(" " + mensajeSeparadoPorNuevaLinea2[i]);
                                }
                            }
                            //this.socket.close();
                            //break;
                        }
                        // Si son datos
                    } else if (peticionRespuestaError.equals("datos")) {
                        // Obtener el receptor del mensaje
                        receptorDelMensaje = mensajeSeparadoPorNuevaLinea[0].split(":")[1].trim();
                        // Obtener el destinatario
                        destinatarioDelMensaje = mensajeSeparadoPorNuevaLinea[1].split(":")[1].trim();
                        if (destinatarioDelMensaje.equals(distanceVector.esteNodo)) {
                            this.log.print("llego ponele");
                            // Nombre del archivo
                            String nombreDelArchivo = mensajeSeparadoPorNuevaLinea[2].split(":")[1].trim();
                            // Datos del archivo
                            String datosDelArchivo = mensajeSeparadoPorNuevaLinea[3].split(":")[1].trim();
                            // Largo del archivo
                            String largoDelArchivo = mensajeSeparadoPorNuevaLinea[5].split(":")[1].trim();
                            // Agregar los datos
                            hexDelArchivo.add(datosDelArchivo);
                            // Se va incrementando el largo del archivo
                            sizeArchivoLocal += datosDelArchivo.length() / 2;
                            // Verificamos si ya estan todos los chunks del archivo
                            if (sizeArchivoLocal >= Integer.parseInt(largoDelArchivo)) {
                                log.print(" Se han recibido los " + hexDelArchivo.size() + " chunks del arvhivo "
                                        + nombreDelArchivo + " enviados por " + receptorDelMensaje);
                                String archivoStr = "";
                                for (int i = 0; i < hexDelArchivo.size(); i++) {
                                    archivoStr += hexDelArchivo.get(i);
                                }
                                // Obtenemos el arreglo de bytes para guardar el archivo
                                byte archivo[] = hexStringToByteArray(archivoStr);
                                String guardar = Paths.get(nombreDelArchivo).getFileName().toString();
                                File archivoaescribir = new File("./Recibidos/" + guardar);
                                OutputStream os = new FileOutputStream(archivoaescribir);
                                os.write(archivo);
                                os.close();
                                log.print(" Archivo almacenado");
                                break;
                            }
                            // Si los archivos no son para este nodo reenviamos los datos
                        } else {
                            // Nombre del archivo
                            String nombreDelArchivo = mensajeSeparadoPorNuevaLinea[2].split(":")[1].trim();
                            // Datos del archivo
                            String datosDelArchivo = mensajeSeparadoPorNuevaLinea[3].split(":")[1].trim();
                            // Largo del archivo
                            String largoDelArchivo = mensajeSeparadoPorNuevaLinea[5].split(":")[1].trim();
                            hexDelArchivo.add(datosDelArchivo);
                            sizeArchivoLocal += datosDelArchivo.length() / 2;
                            if (sizeArchivoLocal >= Integer.parseInt(largoDelArchivo)) {
                                String ruta = distanceVector.vectoresDeDistancia.get(distanceVector.esteNodo)
                                        .get(destinatarioDelMensaje).conQuien;
                                log.print(" Reenviar archivo " + nombreDelArchivo + " de " + hexDelArchivo.size()
                                        + " chunks a " + destinatarioDelMensaje + " por medio de " + ruta);
                                String ip = distanceVector.ipVecinos.get(ruta);
                                Socket socketRenvioArchivo = new Socket(ip, this.puerto);
                                DataOutputStream out = new DataOutputStream(socketRenvioArchivo.getOutputStream());
                                for (int i = 0; i < hexDelArchivo.size(); i++) {
                                    log.print(" Reenviar chunk " + (i + 1) + " a " + destinatarioDelMensaje
                                            + " por medio de " + ruta);
                                    String mensaje = "";
                                    mensaje = "From:" + receptorDelMensaje + "\nTo:" + destinatarioDelMensaje
                                            + "\nName:" + nombreDelArchivo + "\nData:" + hexDelArchivo.get(i)
                                            + "\nFrag:" + i + "\nSize:" + largoDelArchivo + "\nEOF";
                                    out.writeUTF(mensaje);
                                }
                                out.close();
                                socketRenvioArchivo.close();
                                break;
                            }
                        }

                    } else if (peticionRespuestaError.equals("error")) {
                        // Obtener el receptor del mensaje
                        receptorDelMensaje = mensajeSeparadoPorNuevaLinea[0].split(":")[1].trim();
                        // Obtener el destinatario
                        destinatarioDelMensaje = mensajeSeparadoPorNuevaLinea[1].split(":")[1].trim();
                        // Ver si el error es para este nodo
                        if (destinatarioDelMensaje.equals(distanceVector.esteNodo)) {
                            String mensajeError = mensajeSeparadoPorNuevaLinea[2].split(":")[1].trim();
                            log.print(" Ocurrio un error al recibir el archivo: " + mensajeError);
                            break;
                            // Si no es para este nodo lo reenvia
                        } else {
                            String ruta = distanceVector.vectoresDeDistancia.get(distanceVector.esteNodo)
                                    .get(destinatarioDelMensaje).conQuien;
                            log.print(" Reenviar mensaje a " + destinatarioDelMensaje + " por medio de " + ruta);
                            String ip = distanceVector.ipVecinos.get(ruta);
                            Socket socketRenvioError = new Socket(ip, this.puerto);
                            DataOutputStream out = new DataOutputStream(socketRenvioError.getOutputStream());
                            String mensajeError = mensajeSeparadoPorNuevaLinea[2].split(":")[1].trim();
                            String mensaje = "From:" + receptorDelMensaje + "\nTo:" + destinatarioDelMensaje + "\nMsg:"
                                    + mensajeError + "\nEOF";
                            out.writeUTF(mensaje);
                            out.close();
                            socketRenvioError.close();
                            break;
                        }
                    } else {
                        log.print(" El nodo " + receptorDelMensaje + " envio una peticion incorrecta");
                        break;
                    }
                } catch (Exception e) {
                    
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void fragmentacionArchivo(String nombreArchivo) {
        try {
            InputStream archivoaenviar = new FileInputStream(new File("ArchivosEnviar/" + nombreArchivo));
            byte almacen[] = new byte[1460];
            hexDelArchivo.clear();
            int lel = archivoaenviar.read(almacen);
            while (lel > 0) {
                byte almacen2[] = new byte[lel];
                for (int i = 0; i < lel; i++) {
                    almacen2[i] = almacen[i];
                }
                String s = "";
                for (int i = 0; i < almacen2.length; i++) {
                    s += String.format("%02x", almacen2[i]);
                }
                hexDelArchivo.add(s);
                lel = archivoaenviar.read(almacen);
            }
            archivoaenviar.close();
        } catch (Exception e) {
            existe = false;
            hexDelArchivo.clear();
        }
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte data[] = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }
}