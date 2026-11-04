package com.empresa.reembolsos.procesamiento.validacion;

import com.empresa.reembolsos.dominio.CategoriaGasto;
import com.empresa.reembolsos.dominio.EstadoSolicitud;
import com.empresa.reembolsos.dominio.SolicitudReembolso;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class ValidadorDocumentosSoporteTest {

    private final ValidadorDocumentosSoporte validador = new ValidadorDocumentosSoporte();

    @Test
    void validarPropio_sinDocumentosSoporte_retornaRechazo() {
        SolicitudReembolso solicitud = new SolicitudReembolso(1L, "EMP01", CategoriaGasto.VIAJE,
                new BigDecimal("100000"), null, LocalDate.now(), 2, false, EstadoSolicitud.PENDIENTE);

        ResultadoValidacion resultado = validador.validarPropio(solicitud, Collections.emptyList());

        assertFalse(resultado.valida());
        assertEquals("Faltan documentos soporte", resultado.motivoRechazo());
    }

    @Test
    void validarPropio_conDocumentosSoporte_retornaOk() {
        SolicitudReembolso solicitud = new SolicitudReembolso(1L, "EMP01", CategoriaGasto.VIAJE,
                new BigDecimal("100000"), null, LocalDate.now(), 2, true, EstadoSolicitud.PENDIENTE);

        ResultadoValidacion resultado = validador.validarPropio(solicitud, Collections.emptyList());

        assertTrue(resultado.valida());
        assertNull(resultado.motivoRechazo());
    }
}
