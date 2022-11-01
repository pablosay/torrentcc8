import java.util.Collection;
import java.util.HashMap;

public class InformacionVecino {
    public String conQuien;
    public int costo;

    public InformacionVecino(String conQuien, int costo) {
        this.conQuien = conQuien;
        this.costo = costo;
    }

    public String toString() {
        return conQuien + "," + costo;
    }

    public static void main(String[] args) {
        HashMap<String, HashMap<String, InformacionVecino>> dv1 = new HashMap<String, HashMap<String, InformacionVecino>>();
        HashMap<String, InformacionVecino> a = new HashMap<String, InformacionVecino>();
        a.put("hola", new InformacionVecino("A", 0));
        a.put("pez", new InformacionVecino("B", 3));
        dv1.put("G", a);

        HashMap<String, HashMap<String, InformacionVecino>> dv2 = new HashMap<String, HashMap<String, InformacionVecino>>();
        HashMap<String, InformacionVecino> b = new HashMap<String, InformacionVecino>();
        b.put("pez", new InformacionVecino("B", 3));
        b.put("hola", new InformacionVecino("A", 0));
        dv2.put("G", b);
        System.out.println(dv1.toString());
        System.out.println(dv2.toString());
        /*
         * if (dv1.keySet().equals(dv2.keySet())) {
         * Collection<HashMap<String, InformacionVecino>> avalues = dv1.values();
         * Collection<HashMap<String, InformacionVecino>> bvalues = dv2.values();
         * boolean prueba = false;
         * for (HashMap<String,InformacionVecino> hashMap : avalues) {
         * if(bvalues.contains(hashMap) == false){
         * prueba = true;
         * }
         * }
         * if(!prueba){
         * 
         * }
         * }
         */
    }

}