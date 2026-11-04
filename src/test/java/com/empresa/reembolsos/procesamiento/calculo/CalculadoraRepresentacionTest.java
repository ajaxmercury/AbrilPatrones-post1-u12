package com.empresa.reembolsos.procesamiento.calculo;

import com.empresa.reembolsos.dominio.CategoriaGasto;
import com.empresa.reembolsos.dominio.EstadoSolicitud;
import com.empresa.reembolsos.dominio.SolicitudReembolso;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CalculadoraRepresentacionTest {

    private final CalculadoraRepresentacion calculadora = new CalculadoraRepresentacion();

    @Test
    void calcular_montoMenorQueTopeMensual_retornaMontoSolicitado() {
        // Tope mensual = 2000000. Monto = 1200000
        SolicitudReembolso solicitud = new SolicitudReembolso(1L, "EMP01", CategoriaGasto.REPRESENTACION,
                new BigDecimal("1200000"), null, LocalDate.now(), 1, true, EstadoSolicitud.PENDIENTE);

        BigDecimal resultado = calculadora.calcular(solicitud);

        assertEquals(new BigDecimal("1200000"), resultado);
    }

    @Test
    void calcular_montoMayorQueTopeMensual_retornaTopeMensual() {
        // Tope mensual = 2000000. Monto = 2500000
        SolicitudReembolso solicitud = new SolicitudReembolso(1L, "EMP01", CategoriaGasto.REPRESENTACION,
                new BigDecimal("2500000"), null, LocalDate.now(), 1, true, EstadoSolicitud.PENDIENTE);

        BigDecimal resultado = calculadora.calcular(solicitud);

        assertEquals(new BigDecimal("2000000"), resultado);
    }
}
