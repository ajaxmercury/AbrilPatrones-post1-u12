package com.empresa.reembolsos.procesamiento.validacion;

import com.empresa.reembolsos.dominio.CategoriaGasto;
import com.empresa.reembolsos.dominio.EstadoSolicitud;
import com.empresa.reembolsos.dominio.SolicitudReembolso;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ValidadorDuplicadosTest {

    private final ValidadorDuplicados validador = new ValidadorDuplicados();

    @Test
    void validarPropio_conDuplicadoMismaCategoriaYMes_retornaRechazo() {
        LocalDate fecha = LocalDate.of(2026, 5, 10);
        SolicitudReembolso pendienteExistente = new SolicitudReembolso(1L, "EMP01", CategoriaGasto.VIAJE,
                new BigDecimal("100000"), null, fecha, 2, true, EstadoSolicitud.PENDIENTE);
        SolicitudReembolso nuevaSolicitud = new SolicitudReembolso(2L, "EMP01", CategoriaGasto.VIAJE,
                new BigDecimal("120000"), null, fecha.plusDays(3), 1, true, EstadoSolicitud.PENDIENTE);

        ResultadoValidacion resultado = validador.validarPropio(nuevaSolicitud, List.of(pendienteExistente));

        assertFalse(resultado.valida());
        assertEquals("Ya existe una solicitud pendiente de la misma categoria en el periodo", resultado.motivoRechazo());
    }

    @Test
    void validarPropio_sinDuplicados_retornaOk() {
        LocalDate fecha = LocalDate.of(2026, 5, 10);
        SolicitudReembolso pendienteDiferenteCategoria = new SolicitudReembolso(1L, "EMP01", CategoriaGasto.REPRESENTACION,
                new BigDecimal("100000"), null, fecha, 2, true, EstadoSolicitud.PENDIENTE);
        SolicitudReembolso nuevaSolicitud = new SolicitudReembolso(2L, "EMP01", CategoriaGasto.VIAJE,
                new BigDecimal("120000"), null, fecha, 1, true, EstadoSolicitud.PENDIENTE);

        ResultadoValidacion resultado = validador.validarPropio(nuevaSolicitud, List.of(pendienteDiferenteCategoria));

        assertTrue(resultado.valida());
        assertNull(resultado.motivoRechazo());
    }
}
