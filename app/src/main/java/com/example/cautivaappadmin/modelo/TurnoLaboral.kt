package com.example.cautivaappadmin.modelo

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TurnoLaboral(
    @SerialName("id") val id: String? = null,
    @SerialName("usuario_id") val usuarioId: String,
    @SerialName("vehiculo_id") val vehiculoId: String? = null,
    @SerialName("estado") val estado: String? = null
)