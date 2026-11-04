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

## 3. Decisiones de Diseño (Parte 1)

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
3. **Qué se pierde:** Si en un escenario particular el 90% de los rechazos fuera por solicitudes duplicadas pero con documentos y montos impecables, evaluar primero las reglas simples introduce microsegundos de trabajo que no habrían sido estrictamente necesarios. Sin embargo, en arquitectura de software corporativo el principio de menor costo primero es el estándar para prevenir sobrecargas.

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

## 4. Resultados de Pruebas Unitarias y de Integración (Parte 1)

Se construyó una suite con **17 pruebas automatizadas** que cubren exhaustivamente cada eslabón y el flujo completo:

```text
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.empresa.reembolsos.procesamiento.calculo.CalculadoraCapacitacionTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0 (calculo 80% y tope anual)
[INFO] Running com.empresa.reembolsos.procesamiento.calculo.CalculadoraRepresentacionTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0 (monto menor y tope mensual)
[INFO] Running com.empresa.reembolsos.procesamiento.calculo.CalculadoraViajeTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0 (dias * tope diario)
[INFO] Running com.empresa.reembolsos.procesamiento.notificacion.NotificadorContabilidadTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0 (solo actua en APROBADA)
[INFO] Running com.empresa.reembolsos.procesamiento.validacion.ValidadorDocumentosSoporteTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0 (rechazo sin docs y ok con docs)
[INFO] Running com.empresa.reembolsos.procesamiento.validacion.ValidadorDuplicadosTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0 (duplicado en mes y sin duplicados)
[INFO] Running com.empresa.reembolsos.procesamiento.validacion.ValidadorMontoMaximoTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0 (exceso de tope y dentro de tope)
[INFO] Running com.empresa.reembolsos.SolicitudReembolsoIntegrationTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0 (flujo completo 3 categorias, rechazo sin docs, notificacion observadores)
[INFO] 
[INFO] Results:
[INFO] Tests run: 17, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

---

*(La Parte 2 se completará con el ADR-001, las reglas de arquitectura ArchUnit, el workflow de CI y el análisis de impacto de lo descartado)*
