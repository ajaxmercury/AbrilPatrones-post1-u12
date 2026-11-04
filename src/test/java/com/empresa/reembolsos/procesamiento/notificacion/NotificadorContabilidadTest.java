package com.empresa.reembolsos.procesamiento.notificacion;

import com.empresa.reembolsos.dominio.CategoriaGasto;
import com.empresa.reembolsos.dominio.EstadoSolicitud;
import com.empresa.reembolsos.dominio.SolicitudReembolso;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificadorContabilidadTest {

    private final NotificadorContabilidad notificador = new NotificadorContabilidad();
    private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    private PrintStream originalOut;

    @BeforeEach
    void setUp() {
        originalOut = System.out;
        System.setOut(new PrintStream(outputStream));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

    @Test
    void onCambioEstado_cuandoEstadoEsAprobada_programaPago() {
        SolicitudReembolso solicitud = new SolicitudReembolso(1L, "EMP01", CategoriaGasto.VIAJE,
                new BigDecimal("150000"), new BigDecimal("150000"), LocalDate.now(), 1, true, EstadoSolicitud.APROBADA);

        notificador.onCambioEstado(solicitud, EstadoSolicitud.PENDIENTE);

        String output = outputStream.toString();
        assertTrue(output.contains("Contabilidad: programar pago de 150000 para EMP01"));
    }

    @Test
    void onCambioEstado_cuandoEstadoEsRechazada_noProgramaPago() {
        SolicitudReembolso solicitud = new SolicitudReembolso(1L, "EMP01", CategoriaGasto.VIAJE,
                new BigDecimal("150000"), null, LocalDate.now(), 1, true, EstadoSolicitud.RECHAZADA);

        notificador.onCambioEstado(solicitud, EstadoSolicitud.PENDIENTE);

        String output = outputStream.toString();
        assertFalse(output.contains("Contabilidad: programar pago"));
    }
}
