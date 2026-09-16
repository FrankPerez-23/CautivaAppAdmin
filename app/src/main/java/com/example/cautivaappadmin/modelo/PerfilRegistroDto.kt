package com.example.cautivaappadmin.modelo

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class PerfilRegistroDto(
    val id: String,
    val correo: String,
    val nombre: String,
    val apellido: String,
    val telefono: String? = null,
    val rol: String = "CONDUCTOR",
    val activo: Boolean = true,
    val metadatos: JsonObject? = null
)