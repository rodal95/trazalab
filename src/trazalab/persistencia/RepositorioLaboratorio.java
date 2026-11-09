package trazalab.persistencia;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import trazalab.dominio.EstadoMuestra;
import trazalab.dominio.Estudio;
import trazalab.dominio.EventoTraza;
import trazalab.dominio.Muestra;
import trazalab.dominio.Orden;
import trazalab.dominio.Paciente;
import trazalab.dominio.Profesional;
import trazalab.dominio.Resultado;
import trazalab.dominio.Turno;
import trazalab.excepciones.PersistenciaException;

/**
 * Persistencia completa del laboratorio (version web). Lee el estado de la base
 * y registra cada operacion del circuito, cada una en su propia transaccion.
 *
 * El numero de cada turno, orden y muestra sale de su clave primaria
 * (T-0001, O-0001, M-SUE-0001): la base garantiza que no se repita aunque
 * varias sesiones operen a la vez.
 *
 * @author Alday, Rodrigo Matias
 */
public class RepositorioLaboratorio {

    private final ConexionBD baseDeDatos;
    private Connection conexion;
    private EstudioDAO estudioDAO;
    private MuestraDAO muestraDAO;

    // Claves primarias de cada entidad, por su identificacion de negocio.
    private final Map<String, Integer> idPaciente = new HashMap<>();
    private final Map<String, Integer> idEstudio = new HashMap<>();
    private final Map<String, Integer> idOrden = new HashMap<>();
    private final Map<String, Integer> idMuestra = new HashMap<>();
    private final Map<String, Integer> idTurno = new HashMap<>();
    private final Map<String, Integer> idProfesional = new HashMap<>();

    public RepositorioLaboratorio(ConexionBD baseDeDatos) {
        this.baseDeDatos = baseDeDatos;
    }

    /** Toma una conexion valida antes de cada operacion (la anterior pudo caducar). */
    private void asegurarConexion() throws PersistenciaException {
        Connection actual = baseDeDatos.obtenerConexionValida();
        if (actual != conexion) {
            conexion = actual;
            estudioDAO = new EstudioDAO(conexion);
            muestraDAO = new MuestraDAO(conexion);
        }
    }

    /** Conexion en uso, para el modulo persistente (opcion 10). */
    public Connection getConexion() throws PersistenciaException {
        asegurarConexion();
        return conexion;
    }

    /** Clave primaria del profesional, para firmar los eventos del modulo persistente. */
    public int idDe(Profesional profesional) throws PersistenciaException {
        return idProfesionalObligatorio(profesional.getNombreCompleto());
    }

    /** Fotografia del estado del laboratorio leida de la base. */
    public static class Estado {
        public Estudio[] catalogo;
        public final List<Paciente> pacientes = new ArrayList<>();
        public final List<Turno> turnos = new ArrayList<>();
        /** Turnos presentes, en el orden en que llegaron (la cola de la sala de espera). */
        public final List<Turno> salaDeEspera = new ArrayList<>();
        public final List<Orden> ordenes = new ArrayList<>();
        public final List<Muestra> muestras = new ArrayList<>();
        public final Map<String, List<EventoTraza>> eventos = new LinkedHashMap<>();
        public final List<Resultado> resultados = new ArrayList<>();
    }

    // ================================================================ lectura

    /** Lee todo el estado del laboratorio desde la base. */
    public Estado cargar() throws PersistenciaException {
        asegurarConexion();
        Estado e = new Estado();
        try {
            e.catalogo = estudioDAO.listarComoArreglo();
            cargarIdsSimples();
            Map<Integer, Paciente> pacientes = cargarPacientes(e);
            cargarTurnos(e, pacientes);
            Map<Integer, Orden> ordenes = cargarOrdenes(e, pacientes);
            Map<Integer, Muestra> muestras = cargarMuestras(e, ordenes);
            cargarEventos(e, muestras);
            cargarResultados(e, muestras);
        } catch (SQLException ex) {
            throw new PersistenciaException("No se pudo leer el estado del laboratorio: " + ex.getMessage(), ex);
        }
        return e;
    }

    private void cargarIdsSimples() throws SQLException {
        idEstudio.clear();
        idProfesional.clear();
        try (Statement st = conexion.createStatement();
             ResultSet rs = st.executeQuery("SELECT estudio_id, codigo FROM estudios")) {
            while (rs.next()) {
                idEstudio.put(rs.getString(2).toUpperCase(), rs.getInt(1));
            }
        }
        try (Statement st = conexion.createStatement();
             ResultSet rs = st.executeQuery("SELECT profesional_id, CONCAT(apellido, ', ', nombre) FROM profesionales")) {
            while (rs.next()) {
                idProfesional.put(rs.getString(2), rs.getInt(1));
            }
        }
    }

    private Map<Integer, Paciente> cargarPacientes(Estado e) throws SQLException {
        idPaciente.clear();
        Map<Integer, Paciente> porId = new HashMap<>();
        String sql = "SELECT p.paciente_id, p.dni, p.apellido, p.nombre, p.fecha_nacimiento, p.telefono, "
                + "TRIM(CONCAT(COALESCE(os.nombre, 'Particular'), "
                + "  CASE WHEN os.plan IS NULL OR os.plan = '-' THEN '' ELSE CONCAT(' ', os.plan) END)) AS obra "
                + "FROM pacientes p LEFT JOIN obras_sociales os ON os.obra_social_id = p.obra_social_id "
                + "ORDER BY p.paciente_id";
        try (Statement st = conexion.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Paciente p = new Paciente(rs.getString("dni"), rs.getString("apellido"), rs.getString("nombre"),
                        rs.getDate("fecha_nacimiento").toLocalDate(), rs.getString("obra"), rs.getString("telefono"));
                porId.put(rs.getInt("paciente_id"), p);
                idPaciente.put(p.getDni(), rs.getInt("paciente_id"));
                e.pacientes.add(p);
            }
        }
        return porId;
    }

    private void cargarTurnos(Estado e, Map<Integer, Paciente> pacientes) throws SQLException {
        idTurno.clear();
        String sql = "SELECT turno_id, paciente_id, fecha_hora, box, estado, hora_llegada FROM turnos ORDER BY turno_id";
        List<Object[]> presentes = new ArrayList<>();
        try (Statement st = conexion.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                String numero = numeroTurno(rs.getInt("turno_id"));
                Turno t = new Turno(numero, pacientes.get(rs.getInt("paciente_id")),
                        rs.getTimestamp("fecha_hora").toLocalDateTime(), rs.getInt("box"));
                t.setEstado(rs.getString("estado"));
                idTurno.put(numero, rs.getInt("turno_id"));
                e.turnos.add(t);
                if ("Presente".equals(t.getEstado())) {
                    Timestamp llegada = rs.getTimestamp("hora_llegada");
                    presentes.add(new Object[] {llegada == null ? t.getFechaHora() : llegada.toLocalDateTime(), t});
                }
            }
        }
        // COLA: el primero que llego es el primero en ser llamado.
        presentes.sort((a, b) -> ((LocalDateTime) a[0]).compareTo((LocalDateTime) b[0]));
        for (Object[] p : presentes) {
            e.salaDeEspera.add((Turno) p[1]);
        }
    }

    private Map<Integer, Orden> cargarOrdenes(Estado e, Map<Integer, Paciente> pacientes) throws SQLException {
        idOrden.clear();
        Map<Integer, Orden> porId = new LinkedHashMap<>();
        Map<String, Estudio> catalogo = new HashMap<>();
        for (Estudio estudio : e.catalogo) {
            catalogo.put(estudio.getCodigo(), estudio);
        }
        String sql = "SELECT orden_id, numero, paciente_id, medico_solicitante, fecha, estado FROM ordenes ORDER BY orden_id";
        try (Statement st = conexion.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Orden o = new Orden(rs.getString("numero"), pacientes.get(rs.getInt("paciente_id")),
                        rs.getString("medico_solicitante"), rs.getDate("fecha").toLocalDate());
                o.setEstado(rs.getString("estado"));
                porId.put(rs.getInt("orden_id"), o);
                idOrden.put(o.getNumero().toUpperCase(), rs.getInt("orden_id"));
            }
        }
        String det = "SELECT d.orden_id, e.codigo FROM orden_detalle d JOIN estudios e ON e.estudio_id = d.estudio_id "
                + "ORDER BY d.detalle_id";
        try (Statement st = conexion.createStatement(); ResultSet rs = st.executeQuery(det)) {
            while (rs.next()) {
                Orden o = porId.get(rs.getInt(1));
                Estudio estudio = catalogo.get(rs.getString(2));
                if (o != null && estudio != null) {
                    o.agregarEstudio(estudio);
                }
            }
        }
        e.ordenes.addAll(porId.values());
        return porId;
    }

    private Map<Integer, Muestra> cargarMuestras(Estado e, Map<Integer, Orden> ordenes) throws SQLException {
        idMuestra.clear();
        Map<Integer, Muestra> porId = new LinkedHashMap<>();
        String sql = "SELECT muestra_id, codigo_barra, orden_id, tipo_muestra, estado, fecha_extraccion "
                + "FROM muestras ORDER BY muestra_id";
        try (Statement st = conexion.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Muestra m = new Muestra(rs.getString("codigo_barra"), rs.getString("tipo_muestra"),
                        ordenes.get(rs.getInt("orden_id")));
                m.setEstado(EstadoMuestra.valueOf(rs.getString("estado")));
                Timestamp extraccion = rs.getTimestamp("fecha_extraccion");
                if (extraccion != null) {
                    m.setFechaExtraccion(extraccion.toLocalDateTime());
                }
                porId.put(rs.getInt("muestra_id"), m);
                idMuestra.put(m.getCodigoBarra().toUpperCase(), rs.getInt("muestra_id"));
                e.muestras.add(m);
            }
        }
        return porId;
    }

    private void cargarEventos(Estado e, Map<Integer, Muestra> muestras) throws SQLException {
        String sql = "SELECT t.muestra_id, t.estado_anterior, t.estado_nuevo, t.fecha_hora, t.observacion, "
                + "CONCAT(p.apellido, ', ', p.nombre) AS responsable "
                + "FROM trazas_muestra t LEFT JOIN profesionales p ON p.profesional_id = t.profesional_id "
                + "ORDER BY t.muestra_id, t.fecha_hora, t.traza_id";
        try (Statement st = conexion.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Muestra m = muestras.get(rs.getInt("muestra_id"));
                if (m == null) {
                    continue;
                }
                String anterior = rs.getString("estado_anterior");
                EventoTraza ev = new EventoTraza(anterior == null ? null : EstadoMuestra.valueOf(anterior),
                        EstadoMuestra.valueOf(rs.getString("estado_nuevo")), rs.getString("responsable"),
                        rs.getString("observacion"), rs.getTimestamp("fecha_hora").toLocalDateTime());
                e.eventos.computeIfAbsent(m.getIdentificacion(), k -> new ArrayList<>()).add(ev);
            }
        }
    }

    private void cargarResultados(Estado e, Map<Integer, Muestra> muestras) throws SQLException {
        String sql = "SELECT r.muestra_id, es.codigo, r.valor, r.unidad, r.ref_min, r.ref_max, r.fecha_carga, "
                + "r.validado, CONCAT(p.apellido, ', ', p.nombre, ' (Mat. ', COALESCE(p.matricula, '-'), ')') AS validador "
                + "FROM resultados r JOIN orden_detalle d ON d.detalle_id = r.detalle_id "
                + "JOIN estudios es ON es.estudio_id = d.estudio_id "
                + "LEFT JOIN profesionales p ON p.profesional_id = r.validado_por ORDER BY r.resultado_id";
        try (Statement st = conexion.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Muestra m = muestras.get(rs.getInt("muestra_id"));
                if (m == null) {
                    continue;
                }
                Estudio estudio = null;
                for (Estudio candidato : m.getOrden().getEstudios()) {
                    if (candidato.getCodigo().equals(rs.getString("codigo"))) {
                        estudio = candidato;
                    }
                }
                if (estudio == null) {
                    continue;
                }
                Resultado r = new Resultado(m, estudio, rs.getDouble("valor"), rs.getString("unidad"),
                        rs.getDouble("ref_min"), rs.getDouble("ref_max"),
                        rs.getTimestamp("fecha_carga").toLocalDateTime());
                if (rs.getBoolean("validado")) {
                    r.setValidado(true);
                    r.setValidadoPor(rs.getString("validador"));
                }
                e.resultados.add(r);
            }
        }
    }

    // ============================================================== escritura

    /** Alta de paciente. La obra social se busca por nombre (y plan); si no existe, se da de alta. */
    public void insertarPaciente(Paciente p) throws PersistenciaException {
        asegurarConexion();
        enTransaccion("registrar el paciente", () -> {
            Integer obra = buscarOCrearObraSocial(p.getObraSocial());
            String sql = "INSERT INTO pacientes (dni, apellido, nombre, fecha_nacimiento, sexo, telefono, obra_social_id) "
                    + "VALUES (?, ?, ?, ?, 'X', ?, ?)";
            try (PreparedStatement ps = conexion.prepareStatement(sql)) {
                ps.setString(1, p.getDni());
                ps.setString(2, p.getApellido());
                ps.setString(3, p.getNombre());
                ps.setDate(4, java.sql.Date.valueOf(p.getFechaNacimiento()));
                ps.setString(5, p.getTelefono());
                if (obra == null) {
                    ps.setNull(6, java.sql.Types.INTEGER);
                } else {
                    ps.setInt(6, obra);
                }
                ps.executeUpdate();
            }
        });
    }

    private Integer buscarOCrearObraSocial(String texto) throws SQLException {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        String sql = "SELECT obra_social_id FROM obras_sociales WHERE UPPER(nombre) = UPPER(?) "
                + "OR UPPER(CONCAT(nombre, ' ', plan)) = UPPER(?) ORDER BY obra_social_id LIMIT 1";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, texto.trim());
            ps.setString(2, texto.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        String alta = "INSERT INTO obras_sociales (nombre, plan, cobertura_pct) VALUES (?, '-', 0)";
        try (PreparedStatement ps = conexion.prepareStatement(alta, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, texto.trim().length() > 80 ? texto.trim().substring(0, 80) : texto.trim());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    /** Alta de turno. Devuelve el numero asignado (T-0001) a partir de la clave primaria. */
    public String insertarTurno(Paciente paciente, LocalDateTime fechaHora, int box) throws PersistenciaException {
        asegurarConexion();
        String[] numero = new String[1];
        enTransaccion("otorgar el turno", () -> {
            String sql = "INSERT INTO turnos (paciente_id, fecha_hora, box, estado) VALUES (?, ?, ?, 'Otorgado')";
            try (PreparedStatement ps = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, id(idPaciente, paciente.getDni(), "paciente"));
                ps.setTimestamp(2, Timestamp.valueOf(fechaHora));
                ps.setInt(3, box);
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    rs.next();
                    numero[0] = numeroTurno(rs.getInt(1));
                    idTurno.put(numero[0], rs.getInt(1));
                }
            }
        });
        return numero[0];
    }

    /** Cambia el estado de un turno; al pasar a Presente guarda la hora de llegada (cola). */
    public void actualizarTurno(Turno turno) throws PersistenciaException {
        asegurarConexion();
        enTransaccion("actualizar el turno", () -> {
            String sql = "UPDATE turnos SET estado = ?"
                    + ("Presente".equals(turno.getEstado()) ? ", hora_llegada = NOW()" : "")
                    + " WHERE turno_id = ?";
            try (PreparedStatement ps = conexion.prepareStatement(sql)) {
                ps.setString(1, turno.getEstado());
                ps.setInt(2, id(idTurno, turno.getNumero(), "turno"));
                ps.executeUpdate();
            }
        });
    }

    /**
     * Alta de la orden con su detalle y su total, en una sola transaccion. El numero
     * (O-0001) sale de la clave primaria.
     */
    public String insertarOrden(Paciente paciente, String medico, List<Estudio> estudios, double total)
            throws PersistenciaException {
        asegurarConexion();
        String[] numero = new String[1];
        enTransaccion("registrar la orden", () -> {
            int ordenId;
            String sql = "INSERT INTO ordenes (numero, paciente_id, medico_solicitante, fecha, estado, total) "
                    + "VALUES (CONCAT('#', CONNECTION_ID()), ?, ?, CURDATE(), 'Abierta', ?)";
            try (PreparedStatement ps = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, id(idPaciente, paciente.getDni(), "paciente"));
                ps.setString(2, medico);
                ps.setDouble(3, total);
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    rs.next();
                    ordenId = rs.getInt(1);
                }
            }
            numero[0] = String.format("O-%04d", ordenId);
            try (PreparedStatement ps = conexion.prepareStatement("UPDATE ordenes SET numero = ? WHERE orden_id = ?")) {
                ps.setString(1, numero[0]);
                ps.setInt(2, ordenId);
                ps.executeUpdate();
            }
            String det = "INSERT INTO orden_detalle (orden_id, estudio_id, precio) VALUES (?, ?, ?)";
            try (PreparedStatement ps = conexion.prepareStatement(det)) {
                for (Estudio estudio : estudios) {
                    ps.setInt(1, ordenId);
                    ps.setInt(2, id(idEstudio, estudio.getCodigo(), "estudio"));
                    ps.setDouble(3, estudio.calcularPrecio());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            idOrden.put(numero[0], ordenId);
        });
        return numero[0];
    }

    /** Estados de orden que admite la base; "Informada en parte" se guarda como "En proceso". */
    public void actualizarEstadoOrden(Orden orden) throws PersistenciaException {
        asegurarConexion();
        String estado = "Informada en parte".equals(orden.getEstado()) ? "En proceso" : orden.getEstado();
        enTransaccion("actualizar la orden", () -> {
            try (PreparedStatement ps = conexion.prepareStatement("UPDATE ordenes SET estado = ? WHERE orden_id = ?")) {
                ps.setString(1, estado);
                ps.setInt(2, id(idOrden, orden.getNumero(), "orden"));
                ps.executeUpdate();
            }
        });
    }

    /**
     * Alta de una muestra con su primer evento (GENERADA), en una sola transaccion.
     * Devuelve el codigo de barra, que sale de la clave primaria (M-SUE-0001).
     */
    public String insertarMuestra(Orden orden, String tipoMuestra, String responsable, String observacion)
            throws PersistenciaException {
        asegurarConexion();
        String[] codigo = new String[1];
        enTransaccion("generar la muestra", () -> {
            int muestraId;
            String sql = "INSERT INTO muestras (codigo_barra, orden_id, tipo_muestra, estado) VALUES (CONCAT('#', CONNECTION_ID()), ?, ?, 'GENERADA')";
            try (PreparedStatement ps = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, id(idOrden, orden.getNumero(), "orden"));
                ps.setString(2, tipoMuestra);
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    rs.next();
                    muestraId = rs.getInt(1);
                }
            }
            codigo[0] = String.format("M-%s-%04d", tipoMuestra.substring(0, 3).toUpperCase(), muestraId);
            try (PreparedStatement ps = conexion.prepareStatement("UPDATE muestras SET codigo_barra = ? WHERE muestra_id = ?")) {
                ps.setString(1, codigo[0]);
                ps.setInt(2, muestraId);
                ps.executeUpdate();
            }
            insertarTraza(muestraId, null, EstadoMuestra.GENERADA, responsable, observacion);
            idMuestra.put(codigo[0], muestraId);
        });
        return codigo[0];
    }

    /** Cambio de estado de una muestra con su traza (transaccion de MuestraDAO.cambiarEstado). */
    public void registrarTransicion(Muestra muestra, EstadoMuestra anterior, EstadoMuestra nuevo,
                                    String responsable, String observacion) throws PersistenciaException {
        asegurarConexion();
        muestraDAO.cambiarEstado(id(idMuestra, muestra.getCodigoBarra(), "muestra"), anterior, nuevo,
                idProfesionalObligatorio(responsable), observacion);
    }

    /** Carga de un resultado sobre la muestra, vinculado al detalle de la orden. */
    public void insertarResultado(Resultado r, String cargadoPor) throws PersistenciaException {
        asegurarConexion();
        enTransaccion("guardar el resultado", () -> {
            String sql = "INSERT INTO resultados (detalle_id, muestra_id, valor, unidad, ref_min, ref_max, cargado_por) "
                    + "SELECT d.detalle_id, ?, ?, ?, ?, ?, ? FROM orden_detalle d "
                    + "WHERE d.orden_id = ? AND d.estudio_id = ?";
            try (PreparedStatement ps = conexion.prepareStatement(sql)) {
                ps.setInt(1, id(idMuestra, r.getMuestra().getCodigoBarra(), "muestra"));
                ps.setDouble(2, r.getValor());
                ps.setString(3, r.getUnidad());
                ps.setDouble(4, r.getReferenciaMin());
                ps.setDouble(5, r.getReferenciaMax());
                if (cargadoPor == null) {
                    ps.setNull(6, java.sql.Types.INTEGER);
                } else {
                    ps.setInt(6, idProfesionalObligatorio(cargadoPor));
                }
                ps.setInt(7, id(idOrden, r.getMuestra().getOrden().getNumero(), "orden"));
                ps.setInt(8, id(idEstudio, r.getEstudio().getCodigo(), "estudio"));
                if (ps.executeUpdate() != 1) {
                    throw new SQLException("El estudio " + r.getEstudio().getCodigo() + " no figura en la orden.");
                }
            }
        });
    }

    /**
     * Validacion de los resultados de una muestra y su paso a INFORMADA, en una sola
     * transaccion: nunca queda un resultado validado con la muestra sin informar.
     */
    public void validarMuestra(Muestra muestra, EstadoMuestra anterior, String bioquimico, String observacion)
            throws PersistenciaException {
        asegurarConexion();
        int muestraId = id(idMuestra, muestra.getCodigoBarra(), "muestra");
        int profesionalId = idProfesionalObligatorio(bioquimico);
        enTransaccion("validar los resultados", () -> {
            String sql = "UPDATE resultados SET validado = 1, fecha_validacion = NOW(), validado_por = ? "
                    + "WHERE muestra_id = ? AND validado = 0";
            try (PreparedStatement ps = conexion.prepareStatement(sql)) {
                ps.setInt(1, profesionalId);
                ps.setInt(2, muestraId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conexion.prepareStatement("UPDATE muestras SET estado = 'INFORMADA' WHERE muestra_id = ?")) {
                ps.setInt(1, muestraId);
                ps.executeUpdate();
            }
            insertarTraza(muestraId, anterior, EstadoMuestra.INFORMADA, bioquimico, observacion);
        });
    }

    private void insertarTraza(int muestraId, EstadoMuestra anterior, EstadoMuestra nuevo, String responsable,
                               String observacion) throws SQLException, PersistenciaException {
        String sql = "INSERT INTO trazas_muestra (muestra_id, estado_anterior, estado_nuevo, profesional_id, observacion) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, muestraId);
            if (anterior == null) {
                ps.setNull(2, java.sql.Types.VARCHAR);
            } else {
                ps.setString(2, anterior.name());
            }
            ps.setString(3, nuevo.name());
            ps.setInt(4, idProfesionalObligatorio(responsable));
            ps.setString(5, observacion);
            ps.executeUpdate();
        }
    }

    // =========================================================== usuarios (RF01)

    /**
     * Verifica usuario y contrasenia contra la tabla usuarios (hash SHA-256, RNF03).
     * Devuelve el profesional asociado, o null si las credenciales no son validas.
     */
    public Profesional autenticar(String usuario, char[] clave) throws PersistenciaException {
        asegurarConexion();
        String sql = "SELECT u.usuario_id, u.password_hash, p.dni, p.apellido, p.nombre, p.matricula, p.rol "
                + "FROM usuarios u JOIN profesionales p ON p.profesional_id = u.profesional_id "
                + "WHERE u.username = ? AND p.activo = 1";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, usuario);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next() || !MessageDigest.isEqual(
                        sha256(clave).getBytes(StandardCharsets.US_ASCII),
                        rs.getString("password_hash").getBytes(StandardCharsets.US_ASCII))) {
                    return null;
                }
                try (PreparedStatement up = conexion.prepareStatement(
                        "UPDATE usuarios SET ultimo_acceso = NOW() WHERE usuario_id = ?")) {
                    up.setInt(1, rs.getInt("usuario_id"));
                    up.executeUpdate();
                }
                return new Profesional(rs.getString("dni"), rs.getString("apellido"), rs.getString("nombre"),
                        rs.getString("matricula"), rs.getString("rol"));
            }
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo verificar el usuario: " + e.getMessage(), e);
        }
    }

    /** Primer profesional activo con el rol indicado (por ejemplo, la extraccionista de turno). */
    public Profesional buscarProfesionalPorRol(String rol) throws PersistenciaException {
        asegurarConexion();
        String sql = "SELECT dni, apellido, nombre, matricula, rol FROM profesionales "
                + "WHERE rol = ? AND activo = 1 ORDER BY profesional_id LIMIT 1";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, rol);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? new Profesional(rs.getString(1), rs.getString(2), rs.getString(3),
                        rs.getString(4), rs.getString(5)) : null;
            }
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo buscar el profesional: " + e.getMessage(), e);
        }
    }

    private static String sha256(char[] clave) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(new String(clave).getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("La JVM no ofrece SHA-256", e);
        }
    }

    // ================================================================ apoyo

    /** Bloque de trabajo que puede fallar con SQLException. */
    private interface Trabajo {
        void ejecutar() throws SQLException, PersistenciaException;
    }

    /** Ejecuta el trabajo en una transaccion: commit si termina, rollback si falla. */
    private void enTransaccion(String accion, Trabajo trabajo) throws PersistenciaException {
        boolean autoCommit = true;
        try {
            autoCommit = conexion.getAutoCommit();
            conexion.setAutoCommit(false);
            trabajo.ejecutar();
            conexion.commit();
        } catch (SQLException | PersistenciaException e) {
            try {
                conexion.rollback();
            } catch (SQLException ex) {
                throw new PersistenciaException("No se pudo " + accion + " ni revertir la operacion: " + ex.getMessage(), ex);
            }
            if (e instanceof PersistenciaException pe) {
                throw pe;
            }
            throw new PersistenciaException("No se pudo " + accion + ": " + e.getMessage(), e);
        } finally {
            try {
                conexion.setAutoCommit(autoCommit);
            } catch (SQLException e) {
                System.err.println("Aviso: no se pudo restaurar el modo de confirmacion. " + e.getMessage());
            }
        }
    }

    private int id(Map<String, Integer> ids, String clave, String entidad) throws PersistenciaException {
        Integer valor = ids.get(clave.toUpperCase());
        if (valor == null) {
            valor = ids.get(clave);
        }
        if (valor == null) {
            throw new PersistenciaException("No se encontro en la base el " + entidad + " " + clave + ".");
        }
        return valor;
    }

    private int idProfesionalObligatorio(String nombreCompleto) throws PersistenciaException {
        Integer valor = idProfesional.get(nombreCompleto);
        if (valor == null) {
            throw new PersistenciaException("El profesional " + nombreCompleto + " no figura en la base.");
        }
        return valor;
    }

    private static String numeroTurno(int turnoId) {
        return String.format("T-%04d", turnoId);
    }
}
