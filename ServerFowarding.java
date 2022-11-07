import java.io.*;
import java.util.*;
import java.net.Socket;
import java.nio.file.Paths;

public class ServerFowarding implements Runnable {
    Socket socket;
    DistanceVector dv;
    Log log;
    boolean existe = true;
    Integer sizeArchivoLocal = 0;
    Integer puerto = 1981;
    LinkedList<String> hexDelArchivo = new LinkedList<String>();

    public ServerFowarding(Socket socket, DistanceVector dv, Log log) {
        this.socket = socket;
        this.dv = dv;
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

                    // Separarlo por lineas
                    String[] mensajeSeparadoPorNuevaLinea = mensajeDeOtroServidor.split("\n");
                    // Si el largo
                    if (mensajeSeparadoPorNuevaLinea.length == 5) {
                        peticionRespuestaError = "peticion";
                    } else if (mensajeSeparadoPorNuevaLinea.length == 7) {
                        peticionRespuestaError = "datos";
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
                        if (destinatarioDelMensaje.equals(dv.esteNodo)) {
                            String ruta = dv.vectoresDeDistancia.get(dv.esteNodo).get(receptorDelMensaje).conQuien;
                            log.print("Contestar peticion a " + receptorDelMensaje + " por medio de " + ruta);
                            String ip = dv.ipVecinos.get(ruta).get("ip");
                            fragmenatarArchivo(nombreDelArchivo);
                            Socket socketEnvioArchivo = new Socket(ip, puerto);
                            DataOutputStream out = new DataOutputStream(socketEnvioArchivo.getOutputStream());
                            if (!existe) { // Revisar si existe el archivo
                                String mensajeaenviar = "From:" + dv.esteNodo + "\nTo:" + receptorDelMensaje
                                        + "\nMsg: Favor enviar nombre de archivo correcto \nEOF";
                                out.writeUTF(mensajeaenviar);
                                out.close();
                                socketEnvioArchivo.close();
                                break;
                            } else {
                                for (int i = 0; i < hexDelArchivo.size(); i++) {
                                    log.print("(" + i + 1 + ") Reenviar chunk " + i + " a " + receptorDelMensaje
                                            + " por medio de "
                                            + ruta);
                                    String mensajeaenviar = "From:" + dv.esteNodo + "\nTo:" + receptorDelMensaje
                                            + "\nName:"
                                            + nombreDelArchivo + "\nData:" + hexDelArchivo.get(i) + "\nFrag:" + i + 1
                                            + "\nSize:"
                                            + largoDelArchivo + "\nEOF";
                                    out.writeUTF(mensajeaenviar);
                                }
                                out.close();
                                socketEnvioArchivo.close();
                                break;
                            }
                            // Si no es para este nodo solo reenviamos la peticion
                        } else {
                            String ruta = dv.vectoresDeDistancia.get(dv.esteNodo).get(destinatarioDelMensaje).conQuien;
                            log.print("Reenviar peticion a " + destinatarioDelMensaje + " por medio de " + ruta);
                            String ip = dv.ipVecinos.get(ruta).get("ip");
                            Socket socketRenvioArchivo = new Socket(ip, puerto);
                            DataOutputStream out = new DataOutputStream(socketRenvioArchivo.getOutputStream());
                            String mensajeenviar = "From:" + receptorDelMensaje + "\nTo:" + destinatarioDelMensaje
                                    + "\nName:" + nombreDelArchivo
                                    + "\nSize:" + largoDelArchivo + "\nEOF";
                            out.writeUTF(mensajeenviar);
                            out.close();
                            socketRenvioArchivo.close();
                            break;
                        }
                        // Si son datos
                    } else if (peticionRespuestaError.equals("datos")) {
                        // Obtener el receptor del mensaje
                        receptorDelMensaje = mensajeSeparadoPorNuevaLinea[0].split(":")[1].trim();
                        // Obtener el destinatario
                        destinatarioDelMensaje = mensajeSeparadoPorNuevaLinea[1].split(":")[1].trim();
                        if (destinatarioDelMensaje.equals(dv.esteNodo)) {
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
                                String ruta = dv.vectoresDeDistancia.get(dv.esteNodo)
                                        .get(destinatarioDelMensaje).conQuien;
                                log.print("Reenviar archivo " + nombreDelArchivo + " de " + hexDelArchivo.size()
                                        + " chunks a " + destinatarioDelMensaje + " por medio de " + ruta);
                                String ip = dv.ipVecinos.get(ruta).get("ip");
                                Socket socketRenvioArchivo = new Socket(ip, puerto);
                                DataOutputStream out = new DataOutputStream(socketRenvioArchivo.getOutputStream());
                                int cont = 1;
                                for (int i = 0; i < hexDelArchivo.size(); i++) {
                                    log.print("(" + cont + ") Reenviar chunk " + i + " a " + destinatarioDelMensaje
                                            + " por medio de " + ruta);
                                    String mensaje = "";
                                    mensaje = "From:" + receptorDelMensaje + "\nTo:" + destinatarioDelMensaje
                                            + "\nName:" + nombreDelArchivo + "\nData:" + hexDelArchivo.get(i)
                                            + "\nFrag:" + i + "\nSize:" + largoDelArchivo + "\nEOF";
                                    out.writeUTF(mensaje);
                                    cont++;
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
                        if (destinatarioDelMensaje.equals(dv.esteNodo)) {
                            String mensajeError = mensajeSeparadoPorNuevaLinea[2].split(":")[1].trim();
                            log.print("Ocurrio un error al recibir el archivo: " + mensajeError);
                            break;
                            // Si no es para este nodo lo reenvia
                        } else {
                            String ruta = dv.vectoresDeDistancia.get(dv.esteNodo).get(destinatarioDelMensaje).conQuien;
                            log.print(" Reenviar mensaje a " + destinatarioDelMensaje + " por medio de " + ruta);
                            String ip = dv.ipVecinos.get(ruta).get("ip");
                            Socket socketRenvioError = new Socket(ip, puerto);
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
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void fragmenatarArchivo(String nombreArchivo) {
        try {
            InputStream archivoaenviar = new FileInputStream(new File("ArchivosEnviar/" + nombreArchivo));
            byte almacen[] = new byte[256];
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