package com.empresa.reembolsos.procesamiento.calculo;

import com.empresa.reembolsos.dominio.CategoriaGasto;
import com.empresa.reembolsos.dominio.EstadoSolicitud;
import com.empresa.reembolsos.dominio.SolicitudReembolso;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CalculadoraCapacitacionTest {

    private final CalculadoraCapacitacion calculadora = new CalculadoraCapacitacion();

    @Test
    void calcular_ochentaPorCientoBajoTopeAnual_retornaOchentaPorCiento() {
        // Monto = 2000000 -> 80% = 1600000 (menor a tope anual 3000000)
        SolicitudReembolso solicitud = new SolicitudReembolso(1L, "EMP01", CategoriaGasto.CAPACITACION,
                new BigDecimal("2000000"), null, LocalDate.now(), 1, true, EstadoSolicitud.PENDIENTE);

        BigDecimal resultado = calculadora.calcular(solicitud);

        assertEquals(0, new BigDecimal("1600000.0").compareTo(resultado));
    }

    @Test
    void calcular_ochentaPorCientoExcedeTopeAnual_retornaTopeAnual() {
        // Monto = 5000000 -> 80% = 4000000 (mayor a tope anual 3000000) -> retorna 3000000
        SolicitudReembolso solicitud = new SolicitudReembolso(1L, "EMP01", CategoriaGasto.CAPACITACION,
                new BigDecimal("5000000"), null, LocalDate.now(), 1, true, EstadoSolicitud.PENDIENTE);

        BigDecimal resultado = calculadora.calcular(solicitud);

        assertEquals(0, new BigDecimal("3000000").compareTo(resultado));
    }
}
