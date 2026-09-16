package com.example.cautivaappadmin.modelo

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class Perfil(
    val id: String,
    val correo: String? = null,
    val nombre: String,
    val apellido: String,
    val telefono: String? = null,
    val rol: String = "CONDUCTOR",
    val activo: Boolean = true,
    val metadatos: JsonObject? = null,
    @SerialName("creado_el") val creadoEl: String? = null,
    @SerialName("actualizado_el") val actualizadoEl: String? = null
) {
    val nombreCompleto: String
        get() = "$nombre $apellido".trim()

    val claveProvisional: String?
        get() = metadatos?.get("clave_provisional")?.jsonPrimitive?.content

    val vehiculoAsignadoId: String?
        get() = metadatos?.get("vehiculo_id")?.jsonPrimitive?.content
}