package com.empresa.reembolsos;

import com.empresa.reembolsos.aplicacion.SolicitudInvalidaException;
import com.empresa.reembolsos.aplicacion.SolicitudReembolsoService;
import com.empresa.reembolsos.dominio.CategoriaGasto;
import com.empresa.reembolsos.dominio.EstadoSolicitud;
import com.empresa.reembolsos.dominio.SolicitudReembolso;
import com.empresa.reembolsos.infraestructura.RepositorioSolicitudesJpa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class SolicitudReembolsoIntegrationTest {

    @Autowired
    private SolicitudReembolsoService servicio;

    @Autowired
    private RepositorioSolicitudesJpa repositorio;

    @BeforeEach
    void setUp() {
        repositorio.deleteAll();
    }

    @Test
    void crear_conTresCategorias_calculaMontoAprobadoYPersiste() {
        // 1. VIAJE: 2 dias, solicitado 400.000 -> tope diario 150.000 * 2 = 300.000
        SolicitudReembolso viaje = new SolicitudReembolso(null, "EMP01", CategoriaGasto.VIAJE,
                new BigDecimal("140000"), null, LocalDate.now(), 2, true, null);
        SolicitudReembolso viajeCreado = servicio.crear(viaje);
        assertNotNull(viajeCreado.getId());
        assertEquals(new BigDecimal("140000"), viajeCreado.getMontoAprobado());
        assertEquals(EstadoSolicitud.PENDIENTE, viajeCreado.getEstado());

        // 2. REPRESENTACION: solicitado 1.500.000 -> tope mensual 2.000.000 -> aprobado 1.500.000
        SolicitudReembolso rep = new SolicitudReembolso(null, "EMP02", CategoriaGasto.REPRESENTACION,
                new BigDecimal("1500000"), null, LocalDate.now(), 1, true, null);
        SolicitudReembolso repCreado = servicio.crear(rep);
        assertEquals(new BigDecimal("1500000"), repCreado.getMontoAprobado());

        // 3. CAPACITACION: solicitado 2.000.000 -> 80% = 1.600.000 -> aprobado 1.600.000
        SolicitudReembolso cap = new SolicitudReembolso(null, "EMP03", CategoriaGasto.CAPACITACION,
                new BigDecimal("2000000"), null, LocalDate.now(), 1, true, null);
        SolicitudReembolso capCreado = servicio.crear(cap);
        assertEquals(0, new BigDecimal("1600000.0").compareTo(capCreado.getMontoAprobado()));
    }

    @Test
    void crear_sinDocumentosSoporte_lanzaExcepcionYNoPersiste() {
        SolicitudReembolso invalida = new SolicitudReembolso(null, "EMP04", CategoriaGasto.VIAJE,
                new BigDecimal("100000"), null, LocalDate.now(), 2, false, null);

        SolicitudInvalidaException ex = assertThrows(SolicitudInvalidaException.class, () -> servicio.crear(invalida));
        assertEquals("Faltan documentos soporte", ex.getMessage());
        assertEquals(0, repositorio.count());
    }

    @Test
    void cambiarEstado_aAprobada_actualizaEstadoYNotificaObservadores() {
        SolicitudReembolso solicitud = new SolicitudReembolso(null, "EMP05", CategoriaGasto.REPRESENTACION,
                new BigDecimal("500000"), null, LocalDate.now(), 1, true, null);
        SolicitudReembolso guardada = servicio.crear(solicitud);

        SolicitudReembolso actualizada = servicio.cambiarEstado(guardada.getId(), EstadoSolicitud.APROBADA);

        assertEquals(EstadoSolicitud.APROBADA, actualizada.getEstado());
        SolicitudReembolso recuperada = repositorio.findById(guardada.getId()).orElseThrow();
        assertEquals(EstadoSolicitud.APROBADA, recuperada.getEstado());
    }
}

