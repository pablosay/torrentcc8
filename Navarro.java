public class Navarro {
    public static void main(String[] args) {

        Informacion info = new Informacion();
        info.informado.put("aviso", true);
        info.clientes.put("clientex", true);
        info.servers.put("Serverx", true);
        info.Aviso();
        info.AvisoPut("Eduardo");
        info.Aviso();
    }
}
/* 
         Informacion test = new Informacion();
        ClienteS cli = new ClienteS();
        ServerS svr = new ServerS();
        test.informado.put("aviso", true);
        cli.clientes.put("clientex", true);
        svr.servers.put("Serverx", true);
 */