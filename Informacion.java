import java.util.*;
public class Informacion {
    public HashMap<String, Boolean> informado = new HashMap<String, Boolean>();
	public HashMap<String, Boolean> servers = new HashMap<String, Boolean>();
	public HashMap<String, Boolean> clientes = new HashMap<String, Boolean>();

/* Para los  avisos */
    public void Aviso(){
        System.out.println(informado);
    }

    public void AvisoPut(String nodo ){
        informado.put(nodo, false);
    }
/* Replace */
    public void AvisoRep(String nodo ){
        informado.put(nodo, false);
    }


/* Para los Servers  */
    public void Serve() {
        System.out.println(servers);
    }
    public void ServePut(String nodo){
        servers.put(nodo, false);
    }

    public void updateServe(String vecino, Boolean escuchando) {
		this.servers.put(vecino, escuchando);
	}

/* Para los Clientes  */
    public void Cli() {
        System.out.println(clientes);
    }

    public void CliPut(String nodo){
        clientes.put(nodo, false);
    }


}
