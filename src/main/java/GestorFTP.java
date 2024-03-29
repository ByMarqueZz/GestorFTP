import java.io.*;
import java.net.SocketException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTP;

public class GestorFTP {
    private FTPClient clienteFTP;
    private static final String SERVIDOR = "localhost";
    private static final int PUERTO = 21;
    private static final String USUARIO = "PruebaFTP";
    private static final String PASSWORD = "";

    public GestorFTP() {
        clienteFTP = new FTPClient();
    }

    private void conectar() throws SocketException, IOException {
        clienteFTP.connect(SERVIDOR, PUERTO);
        int respuesta = clienteFTP.getReplyCode();
        if (!FTPReply.isPositiveCompletion(respuesta)) {
            clienteFTP.disconnect();
            throw new IOException("Error al conectar con el servidor FTP");
        }
        boolean credencialesOK = clienteFTP.login(USUARIO, PASSWORD);
        if (!credencialesOK) {
            throw new IOException("Error al conectar con el servidor FTP. Credenciales incorrectas.");
        }
        clienteFTP.setFileType(FTP.BINARY_FILE_TYPE);
    }

    private void desconectar() throws IOException {
        clienteFTP.disconnect();
    }

    private boolean subirFichero(String nombreArchivo) throws IOException {
        File ficheroLocal = new File(nombreArchivo);
        InputStream is = new FileInputStream(ficheroLocal);
        boolean enviado = clienteFTP.storeFile(ficheroLocal.getName(), is);
        is.close();
        return enviado;
    }

    private boolean descargarFichero(String ficheroRemoto, String pathLocal) throws IOException {
        OutputStream os = new BufferedOutputStream(new FileOutputStream(pathLocal));
        boolean recibido = clienteFTP.retrieveFile(ficheroRemoto, os);
        os.close();
        return recibido;
    }

    public static void main(String[] args) {
        String fichero_descarga = "perro.jpg";
        System.out.println("¿Qué carpeta deseas asegurar? (ruta absoluta)");
        Scanner sc = new Scanner(System.in);
        String path = sc.nextLine();
        sc.close();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        String nombreZip = sdf.format(new Date()) + ".zip";
        try {
            Process comprimir = new ProcessBuilder()
                    .command("7z", "a", nombreZip, "-r", path)
                    .start();
            comprimir.waitFor();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        File archivoZip = new File(nombreZip);

        GestorFTP gestorFTP = new GestorFTP();
        try {
            gestorFTP.conectar();
            System.out.println("Conectado");
            boolean subido = gestorFTP.subirFichero(archivoZip.getAbsolutePath());
            if (subido) {
                System.out.println("Fichero subido correctamente");
            } else {
                System.err.println("Ha ocurrido un error al intentar subir el fichero");
            }
            // Borramos el zip una vez subido
            boolean borrado = archivoZip.delete();
            if (borrado) {
                System.out.println("Fichero borrado correctamente");
            } else {
                System.err.println("Ha ocurrido un error al intentar borrar el fichero");
            }
            boolean descargado = gestorFTP.descargarFichero(fichero_descarga, "/Users/bymarquezz/Desktop/perrete.jpg");
            if (descargado) {
                System.out.println("Fichero descargado correctamente");
            } else {
                System.err.println("Ha ocurrido un error al intentar descargar el fichero");
            }
            gestorFTP.desconectar();
            System.out.println("Desconectado");
        } catch (Exception e) {
            System.err.println("Ha ocurrido un error:" + e.getMessage());
        }
    }
}