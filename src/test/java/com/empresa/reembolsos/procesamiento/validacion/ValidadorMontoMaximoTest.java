package com.empresa.reembolsos.procesamiento.validacion;

import com.empresa.reembolsos.dominio.CategoriaGasto;
import com.empresa.reembolsos.dominio.EstadoSolicitud;
import com.empresa.reembolsos.dominio.SolicitudReembolso;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class ValidadorMontoMaximoTest {

    private final ValidadorMontoMaximo validador = new ValidadorMontoMaximo();

    @Test
    void validarPropio_montoExcedeTope_retornaRechazo() {
        SolicitudReembolso solicitud = new SolicitudReembolso(1L, "EMP01", CategoriaGasto.VIAJE,
                new BigDecimal("200000"), null, LocalDate.now(), 2, true, EstadoSolicitud.PENDIENTE);

        ResultadoValidacion resultado = validador.validarPropio(solicitud, Collections.emptyList());

        assertFalse(resultado.valida());
        assertTrue(resultado.motivoRechazo().contains("Monto excede el tope de la categoria VIAJE"));
    }

    @Test
    void validarPropio_montoDentroDelTope_retornaOk() {
        SolicitudReembolso solicitud = new SolicitudReembolso(1L, "EMP01", CategoriaGasto.VIAJE,
                new BigDecimal("150000"), null, LocalDate.now(), 2, true, EstadoSolicitud.PENDIENTE);

        ResultadoValidacion resultado = validador.validarPropio(solicitud, Collections.emptyList());

        assertTrue(resultado.valida());
        assertNull(resultado.motivoRechazo());
    }
}
