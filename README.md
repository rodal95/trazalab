# TRAZALAB — Prototipo de la Actividad Práctica 3

Sistema de gestión de turnos y trazabilidad de muestras del Laboratorio Bioquímico San Rafael.
Esta versión resuelve todo el circuito **en memoria**, sin base de datos.

## Requisitos
- JDK 17 o superior (desarrollado en Visual Studio Code y probado con JDK 21).

## Compilar
```bash
javac -encoding UTF-8 -d build $(find src -name "*.java")
```

## Ejecutar
```bash
java -cp build trazalab.app.MenuPrincipal
```

## Estructura
```
src/trazalab/
  dominio/       13 archivos — entidades, enum EstadoMuestra, interfaz Trazable
  excepciones/    4 archivos — excepciones propias verificadas
  servicio/       2 archivos — Laboratorio (fachada) y AgendaTurnos
  util/           3 archivos — Ordenamiento, Busqueda, Consola
  app/            1 archivo  — MenuPrincipal (main)
```

## Recorrido sugerido para probarlo
1. Opción **2**: otorgar un turno al DNI `30111222`, registrar su llegada (el paciente
   ingresa a la **cola** de la sala de espera) y llamar al siguiente paciente.
2. Opción **3**: registrar una orden para el DNI `30111222` con las prácticas `GLU01`, `COL01` y `ORI01`.
3. Opción **4**: intentar la extracción de la orden `O-0001` declarando **4** horas de ayuno
   → se lanza `AyunoInsuficienteException`.
4. Opción **4** otra vez, declarando **12** horas → la extraccionista genera dos muestras
   (suero y orina). Una tercera vez se rechaza: la orden ya tiene sus muestras.
5. Opción **5**: avanzar `M-SUE-0001` al estado 3 (EN_PROCESO). Probar además un retroceso:
   se rechaza la transición.
6. Opción **6**: cargar los resultados de la muestra (por ejemplo 178 mg/dL con rango 70–110).
   Si se repite, los resultados ya cargados se omiten.
7. Opción **7**: la validación se rechaza mientras la muestra no esté ANALIZADA.
8. Opción **5**: avanzar `M-SUE-0001` al estado 4 (ANALIZADA) y volver a la opción **7**:
   se valida el informe y la muestra pasa a INFORMADA.
9. Opción **9**: ver los indicadores de gestión y las últimas acciones de la **pila**.

También puede escribirse una letra donde se espera un número: el menú captura
`InputMismatchException` y continúa.

Pacientes precargados: `30111222` (Gómez), `45222333` (Suárez, pediátrico) y `12888999` (Ríos).
