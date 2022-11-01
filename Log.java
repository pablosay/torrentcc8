
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Log {

    private BufferedWriter buffered;
    private String ruta;

    public Log(String ruta) {
        try {
            this.ruta = ruta;
            File archivo = new File(ruta);
            if (archivo.exists()) {
                archivo.delete();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void print(String data) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z");
            String formatoFecha = sdf.format(new Date());
            this.buffered = new BufferedWriter(new FileWriter(this.ruta, true));
            this.buffered.write("[" + formatoFecha + "]" + data + "\n");
            this.buffered.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}