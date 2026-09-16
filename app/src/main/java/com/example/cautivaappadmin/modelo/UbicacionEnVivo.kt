package com.example.cautivaappadmin.modelo

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UbicacionEnVivo(
    @SerialName("usuario_id") val usuarioId: String,
    @SerialName("turno_id") val turnoId: String? = null,
    val latitud: Double,
    val longitud: Double,
    @SerialName("velocidad_kmh") val velocidadKmh: Double = 0.0,
    val direccion: Double? = null,
    @SerialName("nivel_bateria") val nivelBateria: Int? = null,
    @SerialName("en_movimiento") val enMovimiento: Boolean = false,
    @SerialName("ultima_actualizacion") val ultimaActualizacion: String? = null
)