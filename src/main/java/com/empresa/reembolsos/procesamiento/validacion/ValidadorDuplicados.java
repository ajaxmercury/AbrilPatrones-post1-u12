package com.empresa.reembolsos.procesamiento.validacion;

import com.empresa.reembolsos.dominio.SolicitudReembolso;
import com.empresa.reembolsos.infraestructura.RepositorioSolicitudesJpa; // Violacion intencional de Regla 1
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class ValidadorDuplicados extends ValidadorSolicitud {

    @SuppressWarnings("unused")
    private RepositorioSolicitudesJpa repositorioSolicitudesJpa; // Dependencia prohibida hacia infraestructura

    @Override
    protected ResultadoValidacion validarPropio(SolicitudReembolso solicitud, List<SolicitudReembolso> pendientes) {
        boolean existeDuplicado = pendientes != null && pendientes.stream()
            .anyMatch(p -> p.getCategoria() == solicitud.getCategoria()
                        && p.getFechaGasto() != null && solicitud.getFechaGasto() != null
                        && p.getFechaGasto().getMonth() == solicitud.getFechaGasto().getMonth());
        return existeDuplicado
            ? ResultadoValidacion.rechazar("Ya existe una solicitud pendiente de la misma categoria en el periodo")
            : ResultadoValidacion.ok();
    }
}
