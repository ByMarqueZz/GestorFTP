import java.io.*;
import java.net.SocketException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

public class EjercicioB {
    private static final String SERVIDOR = "cloudinghub.com";
    private static final int PUERTO = 21;
    private static final String USUARIO = "pruebaFTP";
    private static final String PASSWORD = "root";
    private static final String CARPETA_LOCAL = "/Users/bymarquezz/Desktop/FTP";
    private static final String CARPETA_REMOTA = "C:\\Users\\Administrador\\Desktop\\pruebaFTPB";

    private FTPClient clienteFTP;

    public EjercicioB() {
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

    private void sincronizar() throws IOException {
        while (true) {
            try {
                FTPFile[] archivosRemotos = clienteFTP.listFiles(CARPETA_REMOTA);
                File carpetaLocal = new File(CARPETA_LOCAL);
                File[] archivosLocales = carpetaLocal.listFiles();

                // Sincronizar archivos remotos con locales
                sincronizarArchivos(archivosLocales, archivosRemotos);

                // Esperar antes de volver a sincronizar
                TimeUnit.SECONDS.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void sincronizarArchivos(File[] archivosLocales, FTPFile[] archivosRemotos) {
        // Crear una lista de nombres de archivos remotos para facilitar la búsqueda
        Set<String> nombresRemotos = new HashSet<>();
        for (FTPFile archivoRemoto : archivosRemotos) {
            nombresRemotos.add(archivoRemoto.getName());
        }

        // Sincronizar archivos locales con remotos
        for (File archivoLocal : archivosLocales) {
            String nombreArchivoLocal = archivoLocal.getName();
            String rutaArchivoLocal = archivoLocal.getAbsolutePath();

            if (!nombresRemotos.contains(nombreArchivoLocal)) {
                // Archivo local no existe en remoto, subirlo
                try {
                    FileInputStream fis = new FileInputStream(archivoLocal);
                    boolean subido = clienteFTP.storeFile(nombreArchivoLocal, fis);
                    fis.close();
                    if (subido) {
                        System.out.println("Archivo subido: " + nombreArchivoLocal);
                    } else {
                        System.err.println("Error al subir archivo: " + nombreArchivoLocal);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                // Archivo local existe en remoto, verificar si ha sido modificado
                FTPFile archivoRemoto = null;
                for (FTPFile ftpFile : archivosRemotos) {
                    if (ftpFile.getName().equals(nombreArchivoLocal)) {
                        archivoRemoto = ftpFile;
                        break;
                    }
                }

                if (archivoRemoto != null) {
                    long tamañoLocal = archivoLocal.length();
                    long tamañoRemoto = archivoRemoto.getSize();

                    if (tamañoLocal != tamañoRemoto) {
                        // Archivo local ha sido modificado, subir versión actualizada
                        try {
                            FileInputStream fis = new FileInputStream(archivoLocal);
                            boolean subido = clienteFTP.storeFile(nombreArchivoLocal, fis);
                            fis.close();
                            if (subido) {
                                System.out.println("Archivo modificado subido: " + nombreArchivoLocal);
                            } else {
                                System.err.println("Error al subir archivo modificado: " + nombreArchivoLocal);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        // Archivo local no ha sido modificado
                        System.out.println("Archivo no ha cambiado: " + nombreArchivoLocal);
                    }
                }
            }
        }

        // Verificar archivos remotos que no existen localmente (han sido borrados)
        for (FTPFile archivoRemoto : archivosRemotos) {
            String nombreArchivoRemoto = archivoRemoto.getName();
            boolean encontrado = false;
            for (File archivoLocal : archivosLocales) {
                if (archivoLocal.getName().equals(nombreArchivoRemoto)) {
                    encontrado = true;
                    break;
                }
            }
            if (!encontrado) {
                // Archivo remoto no existe localmente, eliminarlo
                try {
                    boolean eliminado = clienteFTP.deleteFile(nombreArchivoRemoto);
                    if (eliminado) {
                        System.out.println("Archivo remoto borrado: " + nombreArchivoRemoto);
                    } else {
                        System.err.println("Error al borrar archivo remoto: " + nombreArchivoRemoto);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    public static void main(String[] args) {
        EjercicioB sincronizador = new EjercicioB();
        try {
            sincronizador.conectar();
            System.out.println("Conectado al servidor FTP");
            sincronizador.sincronizar();
        } catch (Exception e) {
            System.err.println("Ha ocurrido un error:" + e.getMessage());
        } finally {
            try {
                sincronizador.desconectar();
            } catch (IOException e) {
                System.err.println("Error al desconectar del servidor FTP: " + e.getMessage());
            }
        }
    }
}
