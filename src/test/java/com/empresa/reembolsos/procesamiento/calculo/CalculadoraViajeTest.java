package com.empresa.reembolsos.procesamiento.calculo;

import com.empresa.reembolsos.dominio.CategoriaGasto;
import com.empresa.reembolsos.dominio.EstadoSolicitud;
import com.empresa.reembolsos.dominio.SolicitudReembolso;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CalculadoraViajeTest {

    private final CalculadoraViaje calculadora = new CalculadoraViaje();

    @Test
    void calcular_montoMenorQueTopeDiario_retornaMontoSolicitado() {
        // 3 dias * 150000 = 450000 tope. Monto solicitado = 300000
        SolicitudReembolso solicitud = new SolicitudReembolso(1L, "EMP01", CategoriaGasto.VIAJE,
                new BigDecimal("300000"), null, LocalDate.now(), 3, true, EstadoSolicitud.PENDIENTE);

        BigDecimal resultado = calculadora.calcular(solicitud);

        assertEquals(new BigDecimal("300000"), resultado);
    }

    @Test
    void calcular_montoMayorQueTopeDiario_retornaTopeCalculado() {
        // 2 dias * 150000 = 300000 tope. Monto solicitado = 400000
        SolicitudReembolso solicitud = new SolicitudReembolso(1L, "EMP01", CategoriaGasto.VIAJE,
                new BigDecimal("400000"), null, LocalDate.now(), 2, true, EstadoSolicitud.PENDIENTE);

        BigDecimal resultado = calculadora.calcular(solicitud);

        assertEquals(new BigDecimal("300000"), resultado);
    }
}
