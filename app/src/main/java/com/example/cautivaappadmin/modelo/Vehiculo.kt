package com.example.cautivaappadmin.modelo

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class Vehiculo(
    val id: String? = null,
    val placa: String,
    val marca: String? = null,
    val modelo: String? = null,
    val anio: Int? = null,
    val estado: String = "ACTIVO",
    val metadatos: JsonObject? = null,
    @SerialName("creado_el") val creadoEl: String? = null
) {
    val capacidadM3: Double?
        get() = metadatos?.get("capacidad_m3")?.jsonPrimitive?.doubleOrNull

    val descripcionVehiculo: String
        get() = if (!marca.isNullOrEmpty() && !modelo.isNullOrEmpty()) {
            "$marca $modelo"
        } else {
            marca ?: "Sin especificación"
        }

    val resumenDetalle: String
        get() = buildString {
            append(descripcionVehiculo)
            if (anio != null && anio > 0) {
                append(" • ").append(anio)
            }
        }

    override fun toString(): String {
        return "$placa - $descripcionVehiculo".trim()
    }
}