
import java.io.*;
import java.util.*;

public class DistanceVector {

	public String esteNodo;
	public String archivoConfiguracion;
	public Log log;

	public HashMap<String, HashMap<String, InformacionVecino>> vectoresDeDistancia = new HashMap<String, HashMap<String, InformacionVecino>>();
	// Aqui quiza podas reemplazar Hashmap<String, Hashmap<String,String> por
	// Hashmap<String,String>
	public HashMap<String, HashMap<String, String>> ipVecinos = new HashMap<String, HashMap<String, String>>();
	public HashMap<String, HashMap<String, InformacionVecino>> rutas = new HashMap<String, HashMap<String, InformacionVecino>>();
	public LinkedList<String> nodosDeLaRed = new LinkedList<String>();
	public HashMap<String, String> vecinosCosto = new HashMap<String, String>();
	public Boolean cambiosDV = false;
	// Meterlos en una clase
	public HashMap<String, Boolean> informado = new HashMap<String, Boolean>();
	public HashMap<String, Boolean> servers = new HashMap<String, Boolean>();
	public HashMap<String, Boolean> clientes = new HashMap<String, Boolean>();

	/**
	 * 
	 * @param archivoConfiguracion Archivo para el log
	 * @param esteNodo             Nodo que identifica este proyecto
	 * @param log                  Log asignado para el distance vector.
	 */
	public DistanceVector(String archivoConfiguracion, String esteNodo, Log log) {
		this.archivoConfiguracion = archivoConfiguracion;
		this.esteNodo = esteNodo;
		this.log = log;
	}

	/**
	 * Configuracion de la clase de los vectores de distancia.
	 */
	public void config() {
		try {
			File archivo = new File(this.archivoConfiguracion);
			FileReader filereader = new FileReader(archivo);
			BufferedReader bufferreader = new BufferedReader(filereader);
			// Nodo vecino, su costo
			vecinosCosto = new HashMap<String, String>();
			String lineaArchivo = "";
			while ((lineaArchivo = bufferreader.readLine()) != null) {
				String[] token = lineaArchivo.split("-");
				String nodo = token[0];
				String costo = token[1];
				String ip = token[2];
				// Ingresar su nodo y los costos
				vecinosCosto.put(nodo, costo);
				HashMap<String, String> ipNumero = new HashMap<String, String>();
				// Ingresar las IP con sus numeros de IP
				ipNumero.put("ip", ip);
				// Ingresar el nodo con su IP.
				ipVecinos.put(nodo, ipNumero);
				// Nodo (H) -> (G, ipNumero - > ("ip", 127.0.0.1))
			}
			bufferreader.close();
		} catch (Exception e) {
			System.out.println("Error al iniciar la configuracion del servidor, DistanceVector.java, configurar");
		}
		log.print("Adyacentes: " + this.ipVecinos);
		reiniciar(true);
		nuevaRuta(this.vecinosCosto, this.esteNodo);
		this.cambiosDV = true;
		log.print(" Vecinos (Adyacentes) : " + this.nodosDeLaRed);
		print();
	}

	/**
	 * Reiniciar, se reinicia el distance vector
	 * 
	 * @param costosnodos    Costos Nodos y sus costos asignados
	 * @param nodo           Nodo de este proyecto
	 * @param inicializacion Verificar si ya se inicializo
	 */
	public void reiniciar(Boolean inicializacion) {
		HashMap<String, InformacionVecino> costos = new HashMap<String, InformacionVecino>();
		InformacionVecino infovecino = new InformacionVecino(this.esteNodo, 0);
		if (!this.nodosDeLaRed.contains(this.esteNodo)) {
			this.nodosDeLaRed.add(this.esteNodo);
		}

		// Soy mi propio vecino
		costos.put(this.esteNodo, infovecino);

		// Agregar ruta de los vecinos con sus costos
		for (String nodo : this.vecinosCosto.keySet()) {
			if (this.nodosDeLaRed.contains(nodo) == false) {
				this.nodosDeLaRed.add(nodo);
			}
			infovecino = new InformacionVecino("", 0);
			// Si el costo es 99 es porque esta desconectado
			if (this.vecinosCosto.get(nodo).contains("99")) {
				// Se cambia con quien
				infovecino.conQuien = "";
			} else {
				infovecino.conQuien = nodo;
			}
			infovecino.costo = Integer.parseInt(this.vecinosCosto.get(nodo));
			costos.put(nodo, infovecino);
			// Si estamos inicializando
			if (inicializacion) {
				this.clientes.put(nodo, false);
				this.informado.put(nodo, false);
				this.servers.put(nodo, false);
			}
		}
		this.vectoresDeDistancia.put(this.esteNodo, costos);
	}

	/**
	 * Cuando se revisa los vecinos y sus costos que nos envian ,verificamos si hay
	 * o no una mejor ruta para cada uno
	 * 
	 * @param datos
	 * @param nuevoVecino
	 */
	public void nuevaRuta(HashMap<String, String> datos, String vecino) {
		HashMap<String, InformacionVecino> costos = new HashMap<String, InformacionVecino>();
		InformacionVecino infovecino = new InformacionVecino(this.esteNodo, 0);

		if (!this.nodosDeLaRed.contains(vecino)) {
			this.nodosDeLaRed.add(vecino);
		}
		infovecino.conQuien = vecino;
		infovecino.costo = 0;
		costos.put(vecino, infovecino);

		for (String i : datos.keySet()) {
			if (!this.nodosDeLaRed.contains(i)) {
				this.nodosDeLaRed.add(i);
			}
			infovecino = new InformacionVecino(i, Integer.parseInt(datos.get(i)));
			costos.put(i, infovecino);
		}
		if (this.rutas.containsKey(vecino)) {
			this.rutas.replace(vecino, costos);
		} else {
			this.rutas.put(vecino, costos);
		}

		if (!this.esteNodo.contains(vecino)) {
			if (this.rutas.get(this.esteNodo).get(vecino).costo == 99) {
				int nuevoCosto = this.rutas.get(vecino).get(this.esteNodo).costo;
				this.rutas.get(this.esteNodo).get(vecino).costo = nuevoCosto;
				this.rutas.get(this.esteNodo).get(vecino).conQuien = vecino;
			}
		}
	}

	/* Estimar las rutas minimas para ir a un nodo de la red */
	public void calcular(String vecino) {

		String antesDePosibleCambio = this.vectoresDeDistancia.toString();
		this.log.print("Recalculando rutas: " + vecino);
		for (String destino : nodosDeLaRed) {
			InformacionVecino infovecino;
			if (!this.vectoresDeDistancia.get(this.esteNodo).containsKey(destino)) {
				infovecino = new InformacionVecino("", 99);
				this.vectoresDeDistancia.get(this.esteNodo).put(destino, infovecino);
			}
			/* Algoritmo de Bellman-Ford */
			int a = 99;
			if (this.rutas.get(this.esteNodo).containsKey(vecino)) {
				a = this.rutas.get(this.esteNodo).get(vecino).costo;
			}
			int b = 99;
			if (this.rutas.get(vecino).containsKey(destino)) {
				b = this.rutas.get(vecino).get(destino).costo;
			}
			// Total
			int total = 0;
			if ((a + b) > 99) {
				total = 99;
			} else {
				total = a + b;
			}
			// Comparar el costo y actualizar si fuera necesario en el DV
			int costoActual = this.vectoresDeDistancia.get(this.esteNodo).get(destino).costo;
			if (total < costoActual) {
				infovecino = new InformacionVecino(vecino, total);
				this.vectoresDeDistancia.get(this.esteNodo).replace(destino, infovecino);
			}
		}
		String despuesDePosibleCambio = this.vectoresDeDistancia.toString();
		print();
		if (!antesDePosibleCambio.equals(despuesDePosibleCambio)) {
			this.cambiosDV = true;
			for (String vecinoinformado : this.informado.keySet()) {
				this.informado.replace(vecinoinformado, false);
			}
		}
	}

	/* Dibujar el Distance vector */
	public void print() {
		log.print(" DV: " + this.vectoresDeDistancia.toString());
	}

	/* Cuando se envia el distance vector a un vecino se actualiza a informado */
	public void updateinformado(String vecino, Boolean notificado) {
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
					String costooriginal = String.valueOf(this.rutas.get(this.esteNodo).get(vecinoi).costo);
					datos.put(vecinoi, costooriginal);
				}
			}
		}
		this.reiniciar(false);
		this.nuevaRuta(datos, this.esteNodo);
		this.cambiosDV = true;
		for (String vecinoinformado : this.informado.keySet()) {
			this.informado.replace(vecinoinformado, false);
		}
		this.log.print("Fin updateCostoVecino");
	}
}