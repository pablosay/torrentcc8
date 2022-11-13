import java.io.*;
import java.util.*;

public class DistanceVector {

	public String esteNodo;
	public String archivoConfiguracion;
	public Log log;
	// Vectores de distancia del programa
	public HashMap<String, HashMap<String, InformacionVecino>> vectoresDeDistancia = new HashMap<String, HashMap<String, InformacionVecino>>();
	// Nodo , IP
	public HashMap<String, String> ipVecinos = new HashMap<String, String>();
	// Rutas para fowarding
	public HashMap<String, HashMap<String, InformacionVecino>> rutas = new HashMap<String, HashMap<String, InformacionVecino>>();
	// Nodos de la red
	public LinkedList<String> nodosDeLaRed = new LinkedList<String>();
	// Nodos vecinos con sus costos
	public HashMap<String, String> vecinosCosto = new HashMap<String, String>();
	// Verificacion si hay cambios en los vectores de distancia
	public Boolean cambio = false;
	// Informacion de estado de los servidores, clientes, nodos informados
	Informacion info = new Informacion();

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
			// Leemos el archivo de configuracion
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
				// Ingresar el nodo con su IP.
				ipVecinos.put(nodo, ip);
			}
			bufferreader.close();
		} catch (Exception e) {
			System.out.println("Error al iniciar la configuracion del servidor, DistanceVector.java, configurar");
		}
		// Imprimimos los adyacentes que encontremos
		log.print("Adyacentes: " + this.ipVecinos);
		// Reiniciamos el distance vector, le damos true porque viene de la
		// inicializacion
		reiniciar(true);
		// Agregamos las rutas que encontramos
		nuevaRuta(this.vecinosCosto, this.esteNodo);
		// Si hubieron cambios
		this.cambio = true;
		// Nodos de la red
		log.print(" Vecinos (Adyacentes) : " + this.nodosDeLaRed);
		// Imprimir el distance vector
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
		// Vamos a obtener los costos
		HashMap<String, InformacionVecino> costos = new HashMap<String, InformacionVecino>();
		// Con quien debe ir y el costo
		InformacionVecino infovecino = new InformacionVecino(this.esteNodo, 0);
		// Si no esta contenido el propio nodo lo agregamos
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
				this.info.CliPut(nodo);
				this.info.AvisoPut(nodo);
				this.info.ServePut(nodo);
			}
		}
		// Agregamos al dv los nuevos valores
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
		// Nodos con la informacion de quien debe ir y sus costos
		HashMap<String, InformacionVecino> costos = new HashMap<String, InformacionVecino>();
		InformacionVecino infovecino = new InformacionVecino(this.esteNodo, 0);
		// Si no esta este nodo se agrega a los nodos de la red
		if (!this.nodosDeLaRed.contains(vecino)) {
			this.nodosDeLaRed.add(vecino);
		}
		// Agregamos la informacion de este nodo
		infovecino.conQuien = vecino;
		infovecino.costo = 0;
		costos.put(vecino, infovecino);

		// Ver si hay un nuevo nodo y lo agregamos
		for (String i : datos.keySet()) {
			if (!this.nodosDeLaRed.contains(i)) {
				this.nodosDeLaRed.add(i);
			}
			// actualizamos la informacion de los vecinos
			infovecino = new InformacionVecino(i, Integer.parseInt(datos.get(i)));
			costos.put(i, infovecino);
		}

		// Si no esta el vecino se agrega y si se encuentra se reemplaza el valor
		this.rutas.put(vecino, costos);

		if (!this.esteNodo.contains(vecino)) {
			// Si el nodo es unreachable
			if (this.rutas.get(this.esteNodo).get(vecino).costo == 99) {
				// Ya le podemos cambiar su costo
				int nuevoCosto = this.rutas.get(vecino).get(this.esteNodo).costo;
				// Nuevo costo
				this.rutas.get(this.esteNodo).get(vecino).costo = nuevoCosto;
				// Con este vecino
				this.rutas.get(this.esteNodo).get(vecino).conQuien = vecino;
			}
		}
	}

	/* Estimar las rutas minimas para ir a un nodo de la red */
	public void calcular(String vecino) {
		// Verificar si hubieron cambios en los vectores de distancia
		String antesDePosibleCambio = this.vectoresDeDistancia.toString();
		// Recalculando ruta
		this.log.print("Recalculando rutas: " + vecino);
		for (String destino : nodosDeLaRed) {
			InformacionVecino infovecino;
			// Si el nodo no es vecino
			if (!this.vectoresDeDistancia.get(this.esteNodo).containsKey(destino)) {
				// Agregamos el costo de 99
				infovecino = new InformacionVecino("", 99);
				this.vectoresDeDistancia.get(this.esteNodo).put(destino, infovecino);
			}
			// Si no es ejecutamos el
			// Algoritmo de Bellman-Ford
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
		// Si hubi cambios
		if (!antesDePosibleCambio.equals(despuesDePosibleCambio)) {
			this.cambio = true;
			for (String vecinoinformado : this.info.informado.keySet()) {
				// No se les ha enviado los nuevos distance vector
				this.info.informado.replace(vecinoinformado, false);
			}
		} else {
			this.log.print(" No hubieron cambios en de vectores de distancia ");
			print();
		}
	}

	// Imprimir en el log el distance vector
	public void print() {
		log.print(" DV: " + this.vectoresDeDistancia.toString());
	}

	/*
	 * Esta funcion se usa unicamente cuando el vecino se desconecta de la red le
	 * pone un costo de 99
	 */
	public void updateCostoVecino(String vecino) {
		// Costo para unreachable
		String costo = "99";
		// Datos del vecino
		HashMap<String, String> datos = new HashMap<String, String>();
		for (String vecinoi : this.rutas.get(this.esteNodo).keySet()) {
			if (!this.esteNodo.contains(vecinoi)) {
				if (vecinoi.equals(vecino)) {
					// Unreachable
					datos.put(vecinoi, costo);
				} else {
					// Costo original
					String costooriginal = String.valueOf(this.rutas.get(this.esteNodo).get(vecinoi).costo);
					datos.put(vecinoi, costooriginal);
				}
			}
		}
		this.reiniciar(false);
		this.nuevaRuta(datos, this.esteNodo);
		// Hay cambio en el distance vector
		this.cambio = true;
		// No se les han informado a los vecinos de nuestros cambios
		for (String vecinoinformado : this.info.informado.keySet()) {
			this.info.informado.replace(vecinoinformado, false);
		}
		this.log.print(" Hubieron cambios en el distance vector");
		print();
	}
}