package com.empresa.reembolsos.procesamiento.calculo;

import com.empresa.reembolsos.dominio.CategoriaGasto;
import com.empresa.reembolsos.dominio.SolicitudReembolso;
import java.math.BigDecimal;

/**
 * Define el contrato para las estrategias de cálculo de reembolso (Strategy Pattern).
 * Cada implementación encapsula la fórmula y topes aplicables a una categoría de gasto específica.
 */
public interface CalculadoraReembolso {
    CategoriaGasto getCategoria();
    BigDecimal calcular(SolicitudReembolso solicitud);
}
