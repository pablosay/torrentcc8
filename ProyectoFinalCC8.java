import java.net.Socket;
import java.util.Scanner;
import java.io.DataOutputStream;;

public class ProyectoFinalCC8 {
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String textoEnNegrita = "\033[0;1m";
    public static final String ANSI_BLACK_BACKGROUND = "\u001B[40m";
    public static final String ANSI_RED_BACKGROUND = "\u001B[41m";
    public static final String ANSI_GREEN_BACKGROUND = "\u001B[42m";
    public static final String ANSI_YELLOW_BACKGROUND = "\u001B[43m";
    public static final String ANSI_BLUE_BACKGROUND = "\u001B[44m";
    public static final String ANSI_PURPLE_BACKGROUND = "\u001B[45m";
    public static final String ANSI_CYAN_BACKGROUND = "\u001B[46m";
    public static final String ANSI_WHITE_BACKGROUND = "\u001B[47m";

    static void info() {
        System.out.println(ANSI_YELLOW + "Input de aplicacion" + ANSI_RESET);
        System.out.println(ANSI_BLUE + "Info de Servidores" + ANSI_RESET);
        System.out.println(ANSI_WHITE + "Info Fowarding Table" + ANSI_RESET);
        System.out.println(ANSI_PURPLE + "Info Routing" + ANSI_RESET);
    }

    public static void main(String[] arg) throws Exception {
        Log log = new Log("./dv.txt");
        String config = "configuracion.txt";
        DistanceVector dVector = new DistanceVector(config, arg[0], log);
        dVector.config();

        Log log1 = new Log("./servidor.txt");
        ServerManager servidorDistanceVector = new ServerManager(log1, dVector, Integer.parseInt(arg[2]));
        new Thread(servidorDistanceVector).start();

        Log log2 = new Log("./cliente.txt");
        ClientManager clienteDistanceVector = new ClientManager(log2, dVector, Integer.parseInt(arg[1]));
        new Thread(clienteDistanceVector).start();

        int forwadPuerto = 9081;
        Log log3 = new Log("./servidorFowarding.txt");
        ServerFowardingManager servidorForward = new ServerFowardingManager(log3, dVector, forwadPuerto);
        new Thread(servidorForward).start();
        Scanner input = new Scanner(System.in);
        System.out.println(ANSI_GREEN + "+-------Proyecto Final CC8-------+");
        System.out.println(ANSI_GREEN + "| 19001434	Pablo Say        |" + ANSI_RESET);
        System.out.println(ANSI_GREEN + "| 19000243	Kevin De Mata    |" + ANSI_RESET);
        System.out.println(ANSI_GREEN + "| 19008451	Eduardo Navarro  |" + ANSI_RESET);
        System.out.println(ANSI_GREEN + "+--------------------------------+" + ANSI_RESET);
        // Hacer solicitud
        while (true) {
            System.out.println(ANSI_YELLOW + "Ingrese Nodo: " + ANSI_RESET);
            String nodo = input.nextLine();
            if (nodo.equals("EXIT")) {
                System.out.println(ANSI_RED + "Desconectando..." + ANSI_RESET);
                break;
            }
            System.out.println(ANSI_YELLOW + "Ingrese nombre de archivo y extension: " + ANSI_RESET);
            String archivo = input.nextLine();
            System.out.println(ANSI_YELLOW + "Ingrese largo de archivo (bytes): " + ANSI_RESET);
            String tamano = input.nextLine();
            System.out.println("");
            System.out.println(ANSI_GREEN_BACKGROUND + "Solicitando " + archivo + " de largo " + tamano
                    + " bytes a nodo " + nodo + ANSI_RESET);
            System.out.println("Continuar? (y/n)");
            String descision = input.nextLine();
            if (descision.equals("y")) {
                Socket smensaje = new Socket("127.0.0.1", 9081); // puerto del server de fowarding
                DataOutputStream out = new DataOutputStream(smensaje.getOutputStream());
                String msj = "From:" + arg[0] + "\nTo:" + nodo + "\nName:" + archivo + "\nSize:" + tamano + "\nEOF";
                out.writeUTF(msj);
                smensaje.close();
            }
            System.out.println("");
        }
    }
}
