package trazalab.persistencia;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import trazalab.excepciones.PersistenciaException;

/**
 * Administra la conexion con la base de datos MySQL mediante JDBC.
 *
 * PATRON SINGLETON: una unica instancia de conexion para toda la aplicacion,
 * complementario al patron MVC que estructura la aplicacion. Evita abrir una
 * conexion por operacion, que es el error de rendimiento mas frecuente.
 *
 * La conexion se configura en config/trazalab.properties (claves db.url,
 * db.usuario y db.clave), sin recompilar: cada puesto apunta al servidor del
 * laboratorio con el usuario de aplicacion. Otra ruta se indica con
 * -Dtrazalab.config=ruta. Sin archivo se usan los valores por defecto, que son
 * los de desarrollo: MySQL en el mismo equipo.
 *
 * @author Alday, Rodrigo Matias
 */
public final class ConexionBD {

    private static final String DRIVER = "com.mysql.cj.jdbc.Driver";
    private static final String ARCHIVO_CONFIGURACION = "config/trazalab.properties";

    // Valores por defecto, solo para desarrollo.
    private static final String URL_BASE = "jdbc:mysql://localhost:3306/trazalab"
            + "?useSSL=false&serverTimezone=America/Argentina/Cordoba&allowPublicKeyRetrieval=true";
    private static final String USUARIO = "root";
    private static final String CLAVE = "";

    /** Unica instancia de la clase (Singleton). */
    private static ConexionBD instancia;

    private Connection conexion;
    private final String url;
    private final String usuario;
    private final String clave;

    /** Constructor privado: nadie puede instanciar la clase desde afuera. */
    private ConexionBD(String url, String usuario, String clave) {
        this.url = url;
        this.usuario = usuario;
        this.clave = clave;
    }

    /** Devuelve la instancia unica, con la configuracion del archivo o la de desarrollo. */
    public static synchronized ConexionBD getInstancia() {
        if (instancia == null) {
            Properties config = leerConfiguracion();
            instancia = new ConexionBD(config.getProperty("db.url", URL_BASE),
                    config.getProperty("db.usuario", USUARIO),
                    config.getProperty("db.clave", CLAVE));
        }
        return instancia;
    }

    /** Lee el archivo de configuracion; si no existe devuelve una configuracion vacia. */
    private static Properties leerConfiguracion() {
        String indicada = System.getProperty("trazalab.config");
        Path ruta = Path.of(indicada != null ? indicada : ARCHIVO_CONFIGURACION);
        Properties config = new Properties();
        if (!Files.exists(ruta)) {
            if (indicada != null) {
                System.err.println("Aviso: no existe " + ruta + "; se usa la configuracion de desarrollo.");
            }
            return config;
        }
        try (Reader lector = Files.newBufferedReader(ruta, StandardCharsets.UTF_8)) {
            config.load(lector);
        } catch (IOException e) {
            System.err.println("Aviso: no se pudo leer " + ruta + " (" + e.getMessage()
                    + "); se usa la configuracion de desarrollo.");
        }
        return config;
    }

    /** Permite reconfigurar el destino (util para pruebas o para otro servidor). */
    public static synchronized ConexionBD configurar(String url, String usuario, String clave) {
        cerrarSilencioso();
        instancia = new ConexionBD(url, usuario, clave);
        return instancia;
    }

    /**
     * Abre la conexion si todavia no esta disponible y la devuelve.
     *
     * @throws PersistenciaException si el driver no se encuentra o el servidor
     *                               no responde. La SQLException queda como causa.
     */
    public Connection obtenerConexion() throws PersistenciaException {
        try {
            if (conexion == null || conexion.isClosed()) {
                Class.forName(DRIVER);
                conexion = DriverManager.getConnection(url, usuario, clave);
            }
            return conexion;
        } catch (ClassNotFoundException e) {
            throw new PersistenciaException(
                    "No se encontro el driver JDBC de MySQL. Verifique que el conector "
                    + "este agregado al classpath del proyecto.", e);
        } catch (SQLException e) {
            throw new PersistenciaException(
                    "No fue posible conectar con la base de datos: " + e.getMessage(), e);
        }
    }

    /** Comprueba que la conexion este viva enviando una instruccion al servidor. */
    public boolean estaDisponible() {
        try {
            return conexion != null && !conexion.isClosed() && conexion.isValid(2);
        } catch (SQLException e) {
            return false;
        }
    }

    /** Cierra la conexion liberando los recursos del servidor. */
    public void cerrar() throws PersistenciaException {
        try {
            if (conexion != null && !conexion.isClosed()) {
                conexion.close();
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Error al cerrar la conexion: " + e.getMessage(), e);
        }
    }

    private static void cerrarSilencioso() {
        if (instancia != null) {
            try {
                instancia.cerrar();
            } catch (PersistenciaException e) {
                System.err.println("Aviso: " + e.getMessage());
            }
        }
    }

    public String getUrl() {
        return url;
    }
}
