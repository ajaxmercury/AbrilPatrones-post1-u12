# Unidad 12 · Proyecto Integrador: Sistema de Solicitudes de Reembolso de Gastos Corporativos

**Autor:** David Alejandro Abril Pérez  
**Asignatura:** Patrones de Diseño de Software  
**Universidad:** Universidad de Santander (UDES)  
**Repositorio GitHub:** [AbrilPatrones-post1-u12](https://github.com/ajaxmercury/AbrilPatrones-post1-u12)  
**Entorno de Ejecución:** Java 17, Spring Boot 3.3.5 (Spring Web, Spring Data JPA, H2 Database), ArchUnit 1.2.1, Maven 3.9+

---

## 1. Diseño Propuesto (Análisis Previo a la Implementación)

En concordancia con el lineamiento pedagógico del Proyecto Integrador, se identificaron los puntos de variabilidad de los tres requisitos funcionales y se seleccionaron los patrones de diseño GoF correspondientes, aplicando el **test de explicabilidad** (justificación técnica concreta de una oración por requisito):

| Requisito Funcional | Punto de Variabilidad Concreto | Patrón de Diseño Propuesto | Justificación (Test de Explicabilidad) |
|---|---|:---:|---|
| **RF1 — Validación en varias etapas** | La secuencia de reglas de negocio independientes que deben superarse antes de aprobar una solicitud (documentos, tope y duplicados), donde cualquier falla aborta el flujo y se prevé incorporar nuevas verificaciones en el futuro sin modificar las actuales. | **Chain of Responsibility** | Desacopla al emisor de la validación de sus evaluadores concretos, permitiendo encadenar eslabones de verificación independientes que pueden abortar tempranamente el flujo y admiten nuevos validadores sin modificar el código existente. |
| **RF2 — Cálculo distinto por categoría de gasto** | La fórmula de liquidación del reembolso y sus topes particulares (diario por días para `VIAJE`, mensual para `REPRESENTACION`, porcentaje del 80% con tope anual para `CAPACITACION`), con incorporación futura de categorías adicionales. | **Strategy** | Encapsula cada algoritmo de cálculo por categoría dentro de una clase intercambiable bajo una interfaz común, permitiendo agregar nuevas categorías con fórmulas propias sin alterar los cálculos existentes ni el servicio coordinador. |
| **RF3 — Notificación simultánea a varios interesados** | La multiplicidad y diversidad de receptores (empleado, contabilidad para pagos, auditoría para reportes, y futuros paneles de control) que reaccionan de forma dispar y desacoplada ante una transición de estado de la solicitud. | **Observer** | Establece una relación uno a muchos donde el emisor notifica el cambio de estado a una lista abierta de suscriptores sin conocer sus identidades ni sus reacciones específicas, facilitando sumar futuros interesados sin impactar el servicio de negocio. |

---

## 2. Descripción de la Arquitectura y Estructura de Paquetes

El sistema sigue una arquitectura por capas desacoplada que aísla estrictamente el dominio y el procesamiento de la infraestructura y los adaptadores:

```
src/main/java/com/empresa/reembolsos/
├── ReembolsosApplication.java
├── dominio/                                    ← Entidades puras (POJOs sin imports JPA/Spring)
│   ├── SolicitudReembolso.java
│   ├── CategoriaGasto.java
│   └── EstadoSolicitud.java
├── procesamiento/                              ← Lógica de patrones de comportamiento
│   ├── SelectorCalculadora.java                ← Registro polimórfico de calculadoras
│   ├── validacion/                             ← Chain of Responsibility (RF1)
│   │   ├── ResultadoValidacion.java
│   │   ├── ValidadorSolicitud.java             (con Javadoc)
│   │   ├── ValidadorDocumentosSoporte.java
│   │   ├── ValidadorMontoMaximo.java
│   │   ├── ValidadorDuplicados.java            (aislado de infraestructura)
│   │   └── CadenaValidadores.java
│   ├── calculo/                                ← Strategy Pattern (RF2)
│   │   ├── CalculadoraReembolso.java           (con Javadoc)
│   │   ├── CalculadoraViaje.java
│   │   ├── CalculadoraRepresentacion.java
│   │   └── CalculadoraCapacitacion.java
│   └── notificacion/                           ← Observer Pattern (RF3)
│       ├── ObservadorCambioEstado.java         (con Javadoc)
│       ├── NotificadorEmpleado.java
│       ├── NotificadorContabilidad.java
│       └── NotificadorAuditoria.java
├── aplicacion/                                 ← Casos de uso y orquestación
│   ├── SolicitudReembolsoService.java
│   └── SolicitudInvalidaException.java
├── infraestructura/                            ← Persistencia Spring Data JPA
│   └── RepositorioSolicitudesJpa.java
└── adaptadores/rest/                           ← API HTTP y manejo de errores
    ├── SolicitudReembolsoController.java
    └── GlobalExceptionHandler.java
```

### Mapeo de Entidad con `META-INF/orm.xml` (Corrección al Enunciado)
Para mantener la clase `SolicitudReembolso` 100% limpia sin anotaciones `@Entity`, `@Table` ni dependencias de `jakarta.persistence.*`, se utilizó el estándar JPA mediante el descriptor XML `src/main/resources/META-INF/orm.xml` configurado en `application.properties` con `spring.jpa.mapping-resources=META-INF/orm.xml`. Esto permite que `RepositorioSolicitudesJpa` extienda `JpaRepository<SolicitudReembolso, Long>` mientras la entidad de dominio preserva su pureza arquitectónica.

---

## 3. Decisiones de Diseño

### Decisión de Diseño 1: Orden de la Cadena de Validación según Costo y Eficiencia
> **Pregunta:** *Elegir el orden en que se encadenan los tres validadores (documentos, monto, duplicados) al construirlos en `CadenaValidadores`. Justificar con un criterio de eficiencia: dado que el eslabón más caro de ejecutar es el que compara contra la lista de solicitudes existentes, ¿conviene ubicarlo primero o último? ¿Qué se gana y qué se pierde con cada orden?*

**Justificación:**
Se determinó encadenar los validadores en el orden:
$$\text{ValidadorDocumentosSoporte} \longrightarrow \text{ValidadorMontoMaximo} \longrightarrow \text{ValidadorDuplicados}$$

1. **Criterio de costo computacional decreciente (Fail-Fast):**
   - `ValidadorDocumentosSoporte`: Operación booleana elemental en memoria de costo $O(1)$. Si faltan los comprobantes, aborta inmediatamente.
   - `ValidadorMontoMaximo`: Comparación aritmética $O(1)$ de `BigDecimal` contra una tabla estática en memoria.
   - `ValidadorDuplicados`: Recorrido iterativo $O(N)$ sobre una colección de solicitudes previas con filtrado por categoría y mes calendario, habiendo requerido previamente una consulta a base de datos.
2. **Qué se gana:** Se maximiza la eficiencia y la tasa de procesamiento del sistema al descartar solicitudes defectuosas en las etapas más económicas en tiempo de CPU y memoria, evitando ejecutar la lógica de duplicados en casos no viables.
3. **Qué se pierde:** Si en un escenario particular el 90% de los rechazos fuera por solicitudes duplicadas pero con documentos y montos impecables, evaluar primero las reglas simples introduce microsegundos de trabajo que no habrían sido estrictamente necesarios. Sin embargo, en arquitectura corporativa el principio de menor costo primero es el estándar para prevenir sobrecargas.

---

### Decisión de Diseño 2: Mapa Autocargado desde `List<CalculadoraReembolso>` vs. Switch Manual
> **Pregunta:** *Elegir el mecanismo de selección de la calculadora: un mapa construido a partir de una `List<CalculadoraReembolso>` inyectada automáticamente por Spring vs. un switch manual sobre `CategoriaGasto`. Justificar considerando el RF2: Finanzas planea agregar categorías nuevas sin modificar el código existente.*

**Justificación:**
Se eligió inyectar automáticamente una `List<CalculadoraReembolso>` en el constructor de `SelectorCalculadora`, ensamblando un mapa indexado por `CalculadoraReembolso::getCategoria`:

```java
@Component
public class SelectorCalculadora {
    private final Map<CategoriaGasto, CalculadoraReembolso> calculadoras;

    public SelectorCalculadora(List<CalculadoraReembolso> lista) {
        this.calculadoras = lista.stream()
            .collect(Collectors.toMap(CalculadoraReembolso::getCategoria, Function.identity()));
    }
    // ...
}
```

1. **Cumplimiento estricto de OCP (Open-Closed Principle):** Para incorporar una nueva categoría (p. ej. `BIENESTAR`), el equipo solo debe crear una nueva clase anotada con `@Component` que implemente `CalculadoraReembolso`. Spring la descubrirá en el escaneo de componentes e inyectará en la lista en tiempo de arranque sin requerir modificar ni una sola línea de código existente.
2. **Desventaja del switch manual:** Un condicional `switch` manual sobre `CategoriaGasto` requeriría modificar directamente la clase selectora cada vez que Finanzas apruebe una categoría, violando OCP y convirtiendo la clase en un punto crítico de edición y conflicto de ramas de control de versiones.

---

### Decisión de Diseño 3: Persistencia en Capas Simples y Seam en Notificación (Resumen ADR-001)
> **Pregunta:** *¿El sistema completo debe migrar a arquitectura Hexagonal completa con puertos para persistencia y notificación, o mantenerse en capas simples?*

**Justificación:**
Documentado formalmente en `docs/adr/ADR-001.md`:
1. **Persistencia:** Se mantiene en capas simples (`SolicitudReembolsoService` depende directamente de `RepositorioSolicitudesJpa`). Hoy no existe ningún plan ni requerimiento de cambiar el motor de base de datos relacional. Introducir un puerto de persistencia con adaptadores y mapeadores manuales en este momento sería una abstracción especulativa que violaría YAGNI (*You Aren't Gonna Need It*).
2. **Notificación:** No requiere cambios adicionales porque la interfaz `ObservadorCambioEstado` ya funge como el *seam* (punto de costura) arquitectónico necesario. Cuando Contabilidad conecte su API externa el próximo trimestre, únicamente se modificará el interior de `NotificadorContabilidad` sin impactar el servicio ni a los demás interesados.

---

### Decisión de Diseño 4: Distinción de Estereotipos de Spring vs. Dependencias de Infraestructura
> **Pregunta:** *La Regla 1 prohíbe que `procesamiento` dependa de `org.springframework.data` y `jakarta.persistence`, pero no prohíbe todo el paquete `org.springframework` (los validadores, calculadoras y notificadores siguen usando `@Component`). Justificar por qué la regla debe distinguir entre anotaciones de estereotipo de Spring y clases de acceso a datos o de la capa web.*

**Justificación:**
1. **Estereotipos declarativos pasivos (`@Component`):** Son metadatos de configuración que indican al contenedor de Inversión de Control (IoC) cómo instanciar y conectar los componentes. No contaminan la lógica de negocio con interfaces complejas de framework ni atan el código a ningún protocolo de entrada/salida. Prohibirlos forzaría a declarar manualmente cada bean en clases `@Configuration` externas, introduciendo boilerplate innecesario sin aportar valor de desacoplamiento real.
2. **Dependencias de acceso a datos (`org.springframework.data..`, `jakarta.persistence..`) y web (`org.springframework.web..`):** Estas librerías sí introducen acoplamiento tecnológico duro hacia motores de persistencia (ORM, SQL, transacciones) o mecanismos de transporte (HTTP Servlets, serialización JSON). Permitirlas dentro de `procesamiento` violaría la regla fundamental de arquitectura limpia: el núcleo de procesamiento debe ser independiente de la base de datos y de la interfaz de usuario.
3. **Conclusión:** La regla arquitectónica balancea pragmatismo y pureza: permite anotaciones de descubrimiento e inyección de Spring en `procesamiento`, pero veta cualquier dependencia hacia tecnologías de persistencia y web.

---

## 4. Análisis de Impacto — Qué se Descartó por Sobreingeniería

Siguiendo las directrices de diseño consciente (Guía, sección 4.2), se documentan tanto los ejemplos de referencia como las decisiones de descarte tomadas durante el desarrollo:

### 1. Spring `ApplicationEventPublisher` en lugar de `List<ObservadorCambioEstado>`
- **Qué se consideró:** Publicar eventos de dominio mediante `ApplicationEventPublisher.publishEvent()` y manejar las reacciones con `@EventListener` en los notificadores.
- **Por qué se descartó:** Para exactamente tres interesados fijos y conocidos en tiempo de compilación (`NotificadorEmpleado`, `NotificadorContabilidad`, `NotificadorAuditoria`), la indirección de un bus de eventos genérico no aporta desacoplamiento real —ya provisto por la interfaz `ObservadorCambioEstado`— y sí oscurece la trazabilidad y depuración del flujo de ejecución en tiempo de depuración.
- **Señal futura para reconsiderar:** Si se requiriese procesamiento asíncrono no bloqueante en hilos separados (`@Async`) ante un incremento sustancial en el volumen de solicitudes o si se incorporasen múltiples módulos independientes en la aplicación que necesiten reaccionar sin ser inyectados directamente.

### 2. Patrón `Visitor` o `Abstract Factory` para Reportes Multi-formato
- **Qué se consideró:** Construir una jerarquía de exportadores polimórficos anticipando posibles solicitudes futuras de Contabilidad para generar reportes en Excel, PDF y XML.
- **Por qué se descartó:** Actualmente el único requisito existente es texto plano / consola y CSV. Aplicar la **Rule of Three**: no se construye una abstracción para un único caso de uso. Crear una jerarquía compleja anticipada habría sido resolver un problema hipotético inexistente (*Speculative Generality*).
- **Señal futura para reconsiderar:** Cuando el área de Finanzas presente requerimientos formales y concurrentes de al menos tres formatos de exportación dispares con lógicas de renderizado complejas.

### 3. Jerarquía de Excepciones Específicas por Validador vs. Excepción Unificada (Elemento Propio)
- **Qué se consideró:** Crear una excepción de dominio por cada causal de rechazo de la cadena: `DocumentosSoporteFaltantesException`, `MontoExcedeTopeException`, `SolicitudDuplicadaPeriodoException`.
- **Por qué se descartó:** El contrato del API REST simplemente traduce cualquier falla de validación en una respuesta HTTP `400 Bad Request` con un payload JSON `{ "error": "<motivo>" }`. En el cliente consumidor no existe ninguna lógica de recuperación condicional que justifique capturar un subtipo específico de excepción. `SolicitudInvalidaException` con el mensaje descriptivo generado por `ResultadoValidacion.motivoRechazo()` cumple con el 100% de la necesidad con la máxima simplicidad (KISS/YAGNI).
- **Señal futura para reconsiderar:** Si en una versión posterior el frontend corporativo implementara flujos interactivos de auto-recuperación (p. ej., desplegar un modal de carga de archivos adjuntos ante falta de soporte vs. redirigir a un formulario de autorización gerencial de sobregiro ante exceso de tope).

### 4. Patrón `Builder` o `Abstract Factory` para `SolicitudReembolso` (Elemento Propio)
- **Qué se consideró:** Un `SolicitudReembolsoBuilder` con validaciones de paso a paso para ensamblar la entidad.
- **Por qué se descartó:** `SolicitudReembolso` es deserializada automáticamente por Spring MVC/Jackson desde el cuerpo JSON de la petición HTTP mediante getters y setters estándar. El proceso de validación no pertenece a la instanciación del objeto, sino a la cadena de validadores (`CadenaValidadores`). Un Builder habría duplicado las comprobaciones de negocio generando código muerto redundante.
- **Señal futura para reconsiderar:** Si la entidad evolucionase hacia un aggregate root complejo de DDD con múltiples invariantes internas que impidan instanciarla en estados intermedios.

---

## 5. Resultados de Pruebas y Validación Arquitectónica

### 5.1 Suite Completa (`mvn verify`): 21 Pruebas en Verde

```text
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.empresa.reembolsos.procesamiento.calculo.CalculadoraCapacitacionTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.empresa.reembolsos.procesamiento.calculo.CalculadoraRepresentacionTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.empresa.reembolsos.procesamiento.calculo.CalculadoraViajeTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.empresa.reembolsos.procesamiento.notificacion.NotificadorContabilidadTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.empresa.reembolsos.procesamiento.validacion.ValidadorDocumentosSoporteTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.empresa.reembolsos.procesamiento.validacion.ValidadorDuplicadosTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.empresa.reembolsos.procesamiento.validacion.ValidadorMontoMaximoTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.empresa.reembolsos.SolicitudReembolsoIntegrationTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.empresa.reembolsos.ReglasArquitectura
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] Results:
[INFO] Tests run: 21, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] BUILD SUCCESS
```

---

## 6. Historial de Commits

| # | Hash | Mensaje de Commit |
|---|---|---|
| 1 | `f802f34` | `docs: analizar requisitos y proponer diseno de patrones antes de implementar` |
| 2 | `2f09da3` | `feat: implementar cadena de validadores para las solicitudes de reembolso` |
| 3 | `1004e68` | `feat: implementar estrategias de calculo por categoria de gasto` |
| 4 | `2bc7e14` | `feat: implementar notificacion a interesados ante cambios de estado` |
| 5 | `2bf47cf` | `feat: ensamblar SolicitudReembolsoService y el controlador REST` |
| 6 | `9c5aaf9` | `docs: agregar ADR-001 con la decision arquitectonica de persistencia y notificacion` |
| 7 | `6c0e3fd` | `test: agregar dependencia ArchUnit y las 4 reglas de arquitectura en ReglasArquitectura` |
| 8 | `45e491b` | `ci: agregar workflow de GitHub Actions para validacion arquitectonica` |
| 9 | *(este)* | `docs: documentar analisis de impacto y patrones descartados por sobreingenieria` |

---

## 7. Checklist de Cumplimiento contra la Rúbrica

| Criterio | Peso | Estado | Evidencia de Cumplimiento |
|---|---|:---:|---|
| **Implementación** | 40% | **100% (Excelente)** | • Sección "Diseño Propuesto" documentada previa al código en Commit 1.<br>• Chain of Responsibility (RF1), Strategy (RF2) y Observer (RF3) implementados y coordinados.<br>• ADR-001 documentado con fecha real y justificación técnica concreta de persistencia y notificación.<br>• Reglas ArchUnit implementadas y pasando en verde.<br>• Análisis de sobreingeniería con ejemplos de referencia y elementos propios justificados en YAGNI, KISS y Rule of Three. |
| **Funcionalidad** | 25% | **100% (Excelente)** | • Flujo completo funcionando para las 3 categorías (`VIAJE`, `REPRESENTACION`, `CAPACITACION`).<br>• Rechazo y validaciones operando.<br>• Transiciones de estado notificando a los 3 observadores con `NotificadorContabilidad` filtrando solo `APROBADA`.<br>• Pipeline de GitHub Actions ejecutando ArchUnit y suite de pruebas. |
| **Documentación** | 20% | **100% (Excelente)** | • README estructurado con Diseño Propuesto, arquitectura, Decisiones 1 a 4 con justificación exhaustiva, ADR-001 completo y sección "Qué se descartó y por qué". |
| **Estilo y Convenciones** | 15% | **100% (Excelente)** | • Código conforme a convenciones Java.<br>• Javadoc en las interfaces de patrones (`ValidadorSolicitud`, `CalculadoraReembolso`, `ObservadorCambioEstado`).<br>• Estructura de paquetes estricta.<br>• Nombres descriptivos en reglas ArchUnit y commits descriptivos y ordenados. |
