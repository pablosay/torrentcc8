import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class mainTorrent {
    public static void main(String[] arg) throws Exception {
        Scanner leer = new Scanner(System.in);

        Log log = new Log("./Log_DistaneVector.txt");
        String configuracion = "configuracion.txt";
        String esteNodo = "G";
        DistanceVector dv = new DistanceVector(configuracion, esteNodo, log);
        dv.config();

        // servidor
        Log log1 = new Log("./Log_Servidor.txt");
        int reconexion = 90;
        IniciarServidor servidorDistanceVector = new IniciarServidor(log1, reconexion, dv);
        new Thread(servidorDistanceVector).start();

        // clientes
        Log log2 = new Log("./Log_Cliente.txt");
        int retransmitir = 20;
        IniciarCliente clienteDistanceVector = new IniciarCliente(retransmitir, log2, dv);
        new Thread(clienteDistanceVector).start();

        // servidor forward
        int forwadPuerto = 1981;
        Log log3 = new Log("./Log_Servidor_F.txt");
        IniciarServidorF servidorForward = new IniciarServidorF(log3, dv, forwadPuerto);
        new Thread(servidorForward).start();

        // Hacer solicitud
        String consola = "";
        while (true) {
            System.out.println("Ingrese (1) para solicitar archivo");
            consola = leer.nextLine();
            if (consola.contains("1")) {
                System.out.println("Ingrese nodo a solicitar: ");
                String nodo = leer.nextLine();
                System.out.println("Ingrese archivo a solicitar: ");
                String archivo = leer.nextLine();
                System.out.println("Ingrese tamanio a solicitar: ");
                String tamanio = leer.nextLine();
                System.out.println("Se va a solicitar el archivo " + archivo + " de " + tamanio + " bytes al nodo "
                        + nodo + " Esta seguro? (S/N)");
                String seguro = leer.nextLine();
                if (seguro.contains("S")) {
                    System.out.println("Ahora si se va enviar la solicitud");
                    // conectarse con el servidor para reenviar la informacion
                    Socket socketEnvio = new Socket("localhost", forwadPuerto);
                    PrintWriter outSocket = new PrintWriter(socketEnvio.getOutputStream(), true);
                    String mensaje = "";
                    mensaje = "From:" + esteNodo;
                    mensaje += "\nTo:" + nodo;
                    mensaje += "\nName:" + archivo;
                    mensaje += "\nSize:" + tamanio;
                    mensaje += "\nEOF";
                    outSocket.println(mensaje);
                    outSocket.close();
                    socketEnvio.close();
                } else {
                    continue;
                }
            } else {
                System.out.println("Opcion no valida, intente de nuevo");
                continue;
            }
        }
    }
}
