package com.empresa.reembolsos.procesamiento.notificacion;

import com.empresa.reembolsos.dominio.EstadoSolicitud;
import com.empresa.reembolsos.dominio.SolicitudReembolso;

/**
 * Define el contrato de suscripción para los interesados en cambios de estado (Observer Pattern).
 * Permite reaccionar asíncrona o sincrónicamente ante la transición de estado de una solicitud.
 */
public interface ObservadorCambioEstado {
    void onCambioEstado(SolicitudReembolso solicitud, EstadoSolicitud estadoAnterior);
}
