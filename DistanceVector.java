

import java.io.*;
import java.util.*;

public class DistanceVector {
	public String esteNodo = "";
	public HashMap<String, HashMap<String, String>> ipVecinos = new HashMap<String, HashMap<String, String>>();
	public HashMap<String, HashMap<String, HashMap<String, String>>> rutas = new HashMap<String, HashMap<String, HashMap<String, String>>>();
	public LinkedList<String> nodos = new LinkedList<String>();
	public HashMap<String, HashMap<String, HashMap<String, String>>> dv = new HashMap<String, HashMap<String, HashMap<String, String>>>();
	public String configuracion = "";
	public Log log;
	public HashMap<String, String> info = new HashMap<String, String>();
	public Boolean cambiosDV = false;

	public HashMap<String, Boolean> informado = new HashMap<String, Boolean>();
	public HashMap<String, Boolean> servers = new HashMap<String, Boolean>();
	public HashMap<String, Boolean> clientes = new HashMap<String, Boolean>();

	public DistanceVector(String configuracion, String esteNodo, Log log) {
		this.configuracion = configuracion;
		this.esteNodo = esteNodo;
		this.log = log;
	}

	/* Configurar el distance vector a partir de el archivo configuracion.txt */
	public void configurar() {
		log.print("Iniciando nodo " + this.esteNodo);
		try {
			File archivo = new File(this.configuracion);
			FileReader fr = new FileReader(archivo);
			BufferedReader br = new BufferedReader(fr);
			info = new HashMap<String, String>();
			String linea = "";
			while ((linea = br.readLine()) != null) {
				String[] split_linea = linea.split("->");
				info.put(split_linea[0], split_linea[1]);
				HashMap<String, String> host = new HashMap<String, String>();
				host.put("ip", split_linea[2]);
				ipVecinos.put(split_linea[0], host);
			}
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		log.print("Vecinos " + this.ipVecinos);
		/* Iniciar con el primer Distance Vector */
		reiniciarDV(this.info, this.esteNodo, true);
		/* Agregar las rutas */
		agregarRuta(this.info, this.esteNodo);
		this.cambiosDV = true;
		log.print("Destinos " + this.nodos);
		log.print("Cambios en el Distance Vector " + cambiosDV);
		printDV();
	}

	/*
	 * Establecer el Distance vector a partir de los datos que se reciben, lo
	 * reestablece
	 */
	public void reiniciarDV(HashMap<String, String> datos, String nodo, Boolean inicializacion) {
		HashMap<String, HashMap<String, String>> costos = new HashMap<String, HashMap<String, String>>();
		HashMap<String, String> atraves = new HashMap<String, String>();
		if (!this.nodos.contains(nodo)) {
			this.nodos.add(nodo);
		}
		atraves.put("atraves", nodo);
		atraves.put("costo", "0");
		costos.put(nodo, atraves);
		for (String nodoi : datos.keySet()) {
			if (!this.nodos.contains(nodoi)) {
				this.nodos.add(nodoi);
			}
			atraves = new HashMap<String, String>();
			if (datos.get(nodoi).contains("99")) {
				atraves.put("atraves", "");
			} else {
				atraves.put("atraves", nodoi);
			}
			atraves.put("costo", datos.get(nodoi));
			costos.put(nodoi, atraves);
			if (inicializacion) {
				this.clientes.put(nodoi, false);
				this.informado.put(nodoi, false);
				this.servers.put(nodoi, false);
			}
		}
		this.dv.put(nodo, costos);
	}

	/* Agregar una nueva ruta */
	public void agregarRuta(HashMap<String, String> datos, String vecino) {
		HashMap<String, HashMap<String, String>> costos = new HashMap<String, HashMap<String, String>>();
		HashMap<String, String> atraves = new HashMap<String, String>();

		if (!this.nodos.contains(vecino)) {
			this.nodos.add(vecino);
		}
		atraves.put("atraves", vecino);
		atraves.put("costo", "0");
		costos.put(vecino, atraves);

		for (String vecinoi : datos.keySet()) {
			if (!this.nodos.contains(vecinoi)) {
				this.nodos.add(vecinoi);
			}
			atraves = new HashMap<String, String>();
			atraves.put("atraves", vecinoi);
			atraves.put("costo", datos.get(vecinoi));
			costos.put(vecinoi, atraves);
		}
		if (this.rutas.containsKey(vecino)) {
			this.rutas.replace(vecino, costos);
		} else {
			this.rutas.put(vecino, costos);
		}

		if (!this.esteNodo.contains(vecino)) {
			if (this.rutas.get(this.esteNodo).get(vecino).get("costo").contains("99")) {
				String nuevoCosto = this.rutas.get(vecino).get(this.esteNodo).get("costo");
				this.rutas.get(this.esteNodo).get(vecino).replace("costo", nuevoCosto);
				this.rutas.get(this.esteNodo).get(vecino).replace("atraves", vecino);
			}
		}
	}

	/* Estimar las rutas minimas para ir a un nodo de la red */
	public void calcular(String vecino) {
		String before = this.dv.toString();
		this.log.print("Recalcular Distance Vector con rutas de " + vecino);
		for (String destino : nodos) {
			HashMap<String, String> atraves = new HashMap<String, String>();
			if (!this.dv.get(this.esteNodo).containsKey(destino)) {
				atraves = new HashMap<String, String>();
				atraves.put("costo", "99");
				atraves.put("atraves", "");
				this.dv.get(this.esteNodo).put(destino, atraves);
			}
			/* Algoritmo de Bellman-Ford */
			int c_me_to_vecino = 99;
			if (this.rutas.get(this.esteNodo).containsKey(vecino)) {
				c_me_to_vecino = Integer.parseInt(this.rutas.get(this.esteNodo).get(vecino).get("costo"));
			}
			int c_vecino_to_dest = 99;
			if (this.rutas.get(vecino).containsKey(destino)) {
				c_vecino_to_dest = Integer.parseInt(this.rutas.get(vecino).get(destino).get("costo"));
			}
			// Total
			int total = (c_me_to_vecino + c_vecino_to_dest) > 99 ? 99 : (c_me_to_vecino + c_vecino_to_dest);
			// Comparar el costo y actualizar si fuera necesario en el DV
			int costoActual = Integer.parseInt(this.dv.get(this.esteNodo).get(destino).get("costo"));
			if (total < costoActual) {
				atraves = new HashMap<String, String>();
				atraves.put("costo", Integer.toString(total));
				atraves.put("atraves", vecino);
				this.dv.get(this.esteNodo).replace(destino, atraves);
			}
		}
		String after = this.dv.toString();
		printDV();
		if (!before.equals(after)) {
			this.cambiosDV = true;
			for (String vecinoinformado : this.informado.keySet()) {
				this.informado.replace(vecinoinformado, false);
			}
		}
	}

	/* Dibujar el Distance vector */
	public void printDV() {
		String encabazado = " " + " |";
		String tabla = this.esteNodo + " |";
		Collections.sort(this.nodos);
		for (int i = 0; i < this.nodos.size(); i++) {
			encabazado += "     " + this.nodos.get(i) + "     " + " |";
		}
		/*
		for (var destino : this.nodos.size()) {
			encabazado += " ".repeat(5) + destino + " ".repeat(5 - destino.length()) + " |";
		}*/
		for (HashMap<String, HashMap<String, String>> i : this.dv.values()) {
			for (String destino : i.keySet()) {
				String texto = i.get(destino).get("costo") + i.get(destino).get("atraves");
				tabla += "     " + texto + "    "+ " |";
			}
		}
		String enviar ="";
		for (int i = 0; i < encabazado.length(); i++) {
			enviar += "=";
		}
		log.print(enviar);
		log.print(encabazado);
		log.print(enviar);
		log.print(tabla);
		log.print(enviar);
	}

	/* Cuando se envia el distance vector a un vecino se actualiza a informado */
	public void updateinformado(String vecino, Boolean notificado) {
		if (this.informado.containsKey(vecino)) {
			this.informado.replace(vecino, notificado);
		}
		this.informado.put(vecino, notificado);
	}

	/* Cuando un cliente se conecto al servidor del distance vector */
	public void updateclientes(String vecino, Boolean conectado) {
		if (this.clientes.containsKey(vecino)) {
			this.clientes.replace(vecino, conectado);
		} else {
			this.clientes.put(vecino, conectado);
		}
	}

	/* Cuando me conecto a un servidor */
	public void updateservers(String vecino, Boolean escuchando) {
		if (this.servers.containsKey(vecino)) {
			this.servers.replace(vecino, escuchando);
		}
		this.servers.put(vecino, escuchando);
	}

	/*
	 * Esta funcion se usa unicamente cuando el vecino se desconecta de la red le
	 * pone un costo de 99
	 */
	public void updateCostoVecino(String vecino) {
		String costo = "99";
		HashMap<String, String> datos = new HashMap<String, String>();
		this.log.print("Inicio updateCostoVecino");
		for (String vecinoi : this.rutas.get(this.esteNodo).keySet()) {
			if (!this.esteNodo.contains(vecinoi)) {
				if (vecinoi.equals(vecino)) {
					datos.put(vecinoi, costo);
				} else {
					String costooriginal = this.rutas.get(this.esteNodo).get(vecinoi).get("costo");
					datos.put(vecinoi, costooriginal);
				}
			}
		}
		this.reiniciarDV(datos, this.esteNodo, false);
		this.agregarRuta(datos, this.esteNodo);
		this.cambiosDV = true;
		for (String vecinoinformado : this.informado.keySet()) {
			this.informado.replace(vecinoinformado, false);
		}
		this.log.print("Fin updateCostoVecino");
	}
}