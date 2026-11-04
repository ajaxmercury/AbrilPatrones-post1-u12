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

*(La documentación de la implementación, arquitectura, decisiones de diseño, ADR-001 y análisis de sobreingeniería se completará en los commits subsecuentes)*
