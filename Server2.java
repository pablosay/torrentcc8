import java.io.*;
import java.util.*;
import java.net.Socket;
import java.nio.file.Paths;

public class Server2 implements Runnable {
    Socket socket;
    DistanceVector dv;
    Log log;
    boolean existe = true;
    Integer sizeArchivoLocal = 0;
    Integer puerto = 1981;
    LinkedList<String> ll = new LinkedList<String>();

    public Server2(Socket socket, DistanceVector dv, Log log) {
        this.socket = socket;
        this.dv = dv;
        this.log = log;
    }

    public void run() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String de = "";
            String para = "";
            while (true) {
                boolean leidoelto = true;
                while (leidoelto) {
                    String msg = in.readLine();
                    if (msg == null) {
                        break;
                    }
                    log.print(" " + msg);
                    String tokens[] = msg.split(":");
                    if (msg.contains("From:")) {
                        de = tokens[1].trim();
                        continue;
                    } else if (msg.contains("To")) {
                        para = tokens[1].trim();
                        leidoelto = false;
                    }
                }
                if (para.equals(dv.esteNodo)) {
                    String msg = in.readLine();
                    log.print(" " + msg);
                    if (msg != null) {
                        if (msg.contains("Name")) {
                            String nombreArchivo = msg.split(":")[1].trim();
                            msg = in.readLine();
                            log.print(" " + msg);

                            if (msg.contains("Size")) {
                                String sizeArchivo = msg.split(":")[1].trim();
                                msg = in.readLine();
                                log.print(" " + msg);
                                String ruta = dv.vectoresDeDistancia.get(dv.esteNodo).get(de).conQuien;
                                log.print("Contestar peticion a " + de + " por medio de " + ruta);
                                String ip = dv.ipVecinos.get(ruta).get("ip");
                                fragmenatarArchivo(nombreArchivo);
                                Socket socketReenvio = new Socket(ip, puerto);
                                PrintWriter outSocket = new PrintWriter(socketReenvio.getOutputStream(), true);
                                if (!existe) {
                                    String mensajeaenviar = "From:" + dv.esteNodo + "\nTo:" + de + "\nMsg: Favor enviar nombre de archivo correcto \nEOF";
                                    outSocket.println(mensajeaenviar);
                                    outSocket.close();
                                    socketReenvio.close();
                                    break;
                                    //si deciden alternar regresar a lo que se tenia
                                } else {
                                    for (int i = 0; i < ll.size(); i++) {
                                        log.print("(" + i+1 + ") Reenviar chunk " + i + " a " + de + " por medio de " + ruta);
                                        String mensajeaenviar = "From:" + dv.esteNodo +"\nTo:" + de +"\nName:" + nombreArchivo + "\nData:" + ll.get(i)+ "\nFrag:" + i+1 +"\nSize:" + sizeArchivo + "\nEOF";
                                        outSocket.println(mensajeaenviar);
                                    }
                                }
                                outSocket.close();
                                socketReenvio.close();
                                break;
                            } else if (msg.contains("Data")) {
                                String data = msg.split(":")[1].trim();
                                msg = in.readLine();
                                log.print(" " + msg);
                                String frag = msg.split(":")[1].trim();
                                msg = in.readLine();
                                log.print(" " + msg);
                                String size = msg.split(":")[1].trim();
                                msg = in.readLine();
                                log.print(" " + msg);
                                ll.add(data);
                                sizeArchivoLocal += data.length() / 2;
                                if (sizeArchivoLocal >= Integer.parseInt(size)) {
                                    log.print(" Se han recibido los " + ll.size() + " chunks del arvhivo "+ nombreArchivo + " enviados por " + de);
                                    String archivoStr = "";
                                    for (int i = 0; i < ll.size(); i++) {
                                        archivoStr += ll.get(i);
                                    }
                                    byte archivo[] = hexStringToByteArray(archivoStr);

                                    String guardar = Paths.get(nombreArchivo).getFileName().toString();

                                    File archivoaescribir = new File("./Recibidos/" + guardar);
                                    OutputStream os = new FileOutputStream(archivoaescribir);
                                    os.write(archivo);
                                    os.close();
                                    log.print("Archivo guardado : " + nombreArchivo);
                                    break;
                                }
                            }
                        } else if (msg.contains("Msg")) {
                            String tokens[] = msg.split(":");
                            String msgdeerror = tokens[1];
                            msg = in.readLine();
                            log.print(" " + msg);
                            log.print("Ocurrion un error al recibir el archivo: " + msgdeerror);
                            break;
                        } else {
                            log.print("El cliente " + de + " envio una peticion incorrecta");
                        }
                    } else {
                        log.print("El cliente " + de + " mando null");
                    }
                } else {
                    String msg = in.readLine();
                    log.print(" " + msg);
                    if (msg != null) {
                        msg.replaceAll("\\s+", "");
                        if (msg.contains("Name:")) {
                            String nombreArchivo = msg.split(":")[1].trim();
                            msg = in.readLine();
                            log.print(" " + msg);
                            if (msg.contains("Size")) {
                                String size = msg.split(":")[1].trim();
                                msg = in.readLine();
                                log.print(" " + msg);
                                String ruta = dv.vectoresDeDistancia.get(dv.esteNodo).get(para).conQuien;
                                log.print("Reenviar peticion a " + para + " por medio de " + ruta);
                                String ip = dv.ipVecinos.get(ruta).get("ip");
                                Socket conesocenviar = new Socket(ip, puerto);
                                PrintWriter out = new PrintWriter(conesocenviar.getOutputStream(), true);
                                String mensajeenviar = "From:" + de + "\nTo:" + para + "\nName:" + nombreArchivo +"\nSize:" + size +"\nEOF";
                                out.println(mensajeenviar);
                                out.close();
                                conesocenviar.close();
                                break;
                            } else {
                                String dataArchivo = msg.split(":")[1].trim();
                                msg = in.readLine();
                                log.print(" " + msg);
                                String fragArchivo = msg.split(":")[1].trim();
                                msg = in.readLine();
                                log.print(" " + msg);
                                String sizeArchivo = msg.split(":")[1].trim();
                                msg = in.readLine();
                                log.print(" " + msg);
                                ll.add(dataArchivo);
                                sizeArchivoLocal += dataArchivo.length() / 2;
                                if (sizeArchivoLocal >= Integer.parseInt(sizeArchivo)) {
                                    String ruta = dv.vectoresDeDistancia.get(dv.esteNodo).get(para).conQuien;
                                    log.print("Reenviar archivo " + nombreArchivo + " de " + ll.size() + " chunks a " + para + " por medio de " + ruta);
                                    String ip = dv.ipVecinos.get(ruta).get("ip");
                                    Socket conexionnuevocliente = new Socket(ip, puerto);
                                    PrintWriter out = new PrintWriter(conexionnuevocliente.getOutputStream(), true);
                                    int cont = 1;

                                    for (int i = 0; i < ll.size(); i++) {
                                        log.print("(" + cont + ") Reenviar chunk " + i + " a " + para + " por medio de " + ruta);
                                        String mensaje = "";
                                        mensaje = "From:" + de +"\nTo:" + para +"\nName:" + nombreArchivo +"\nData:" + ll.get(i) + "\nFrag:" + i +"\nSize:" + sizeArchivo +"\nEOF";
                                        out.println(mensaje);
                                        cont++;
                                    }
                                    out.close();
                                    conexionnuevocliente.close();
                                    break;
                                }
                            }
                        } else if (msg.contains("Msg:")) {
                            String error = msg.split(":")[1].trim();
                            msg = in.readLine();
                            log.print(" " + msg);
                            String ruta = dv.vectoresDeDistancia.get(dv.esteNodo).get(para).conQuien;
                            log.print(" Reenviar mensaje a " + para + " por medio de " + ruta);
                            String ip = dv.ipVecinos.get(ruta).get("ip");
                            Socket conexionnuevocliente = new Socket(ip, puerto);
                            PrintWriter out = new PrintWriter(conexionnuevocliente.getOutputStream(), true);
                            String mensaje = "From:" + de +"\nTo:" + para + "\nMsg:" + error +"\nEOF";
                            out.println(mensaje);
                            out.close();
                            conexionnuevocliente.close();
                            break;
                        } else {
                            log.print(" El cliente " + de + " envio una peticion incorrecta");
                            break;
                        }
                    } else {
                        log.print(" El cliente " + de + " mando null");
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
            InputStream archivoaenviar = new FileInputStream(new File("ArchivosEnviar/" + nombreArchivo));
            byte almacen[] = new byte[256];
            ll.clear();
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
                ll.add(s);
                lel = archivoaenviar.read(almacen);
            }
            archivoaenviar.close();
        } catch (Exception e) {
            existe = false;
            ll.clear();
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
