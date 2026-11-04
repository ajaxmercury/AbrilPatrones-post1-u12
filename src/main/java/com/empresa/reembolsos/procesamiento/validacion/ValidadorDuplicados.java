package com.empresa.reembolsos.procesamiento.validacion;

import com.empresa.reembolsos.dominio.SolicitudReembolso;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class ValidadorDuplicados extends ValidadorSolicitud {
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
