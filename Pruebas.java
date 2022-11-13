public class Pruebas {
    public static void main(String[] args) {
        String mensaje = "From:B" + "\n" + "To:A" + "\n" + "Name: galileo.jpg" + "\n" + "Size:4553" + "\n" + "EOF";
        String[] tokens = mensaje.split("\n");
        String destinatarioDelMensaje = tokens[1].split(":")[1].trim();
        System.out.println(destinatarioDelMensaje + "hola");
    }
}

