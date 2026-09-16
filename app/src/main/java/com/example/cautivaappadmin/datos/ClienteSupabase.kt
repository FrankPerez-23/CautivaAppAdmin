package com.example.cautivaappadmin.datos

import com.example.cautivaappadmin.modelo.Perfil
import com.example.cautivaappadmin.modelo.PerfilRegistroDto
import com.example.cautivaappadmin.modelo.UbicacionEnVivo
import com.example.cautivaappadmin.modelo.Vehiculo
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.realtime.Realtime
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object ClienteSupabase {

    // Configuración por defecto de la instancia de Supabase de Cautiva
    const val URL_SUPABASE = "https://zhtufypmgbgaoqnojwas.supabase.co"
    const val CLAVE_ANON_SUPABASE = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InpodHVmeXBtZ2JnYW9xbm9qd2FzIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODkzOTQ2MjMsImV4cCI6MjEwNDk3MDYyM30.9iepsOEdeYu1C1hB7voqBEXqyeY72yrZLfCF9gH2oII"

    val cliente = createSupabaseClient(
        supabaseUrl = URL_SUPABASE,
        supabaseKey = CLAVE_ANON_SUPABASE
    ) {
        install(Auth)
        install(Postgrest)
        install(Realtime)
    }

    // Autenticación de administrador
    suspend fun autenticarAdministrador(correo: String, contrasena: String) {
        cliente.auth.signInWith(Email) {
            email = correo
            password = contrasena
        }
    }

    // Obtener sesión actual
    fun obtenerUsuarioActualId(): String? {
        return cliente.auth.currentUserOrNull()?.id
    }

    // Obtener perfil del usuario por ID
    suspend fun obtenerPerfil(usuarioId: String): Perfil {
        return cliente.postgrest["perfiles"]
            .select {
                filter {
                    eq("id", usuarioId)
                }
            }
            .decodeSingle<Perfil>()
    }

    // Obtener lista completa de trabajadores (CONDUCTOR, SUPERVISOR, ADMINISTRADOR)
    suspend fun obtenerTrabajadores(): List<Perfil> {
        return cliente.postgrest["perfiles"]
            .select()
            .decodeList<Perfil>()
    }

    // Obtener lista de vehículos
    suspend fun obtenerVehiculos(): List<Vehiculo> {
        return cliente.postgrest["vehiculos"]
            .select()
            .decodeList<Vehiculo>()
    }

    // Registrar un nuevo vehículo en la tabla 'vehiculos'
    suspend fun insertarVehiculo(
        placa: String,
        marca: String?,
        modelo: String?,
        anio: Int?,
        estado: String,
        capacidadM3: Double?
    ) {
        val metadatosJson = buildJsonObject {
            if (capacidadM3 != null && capacidadM3 > 0) {
                put("capacidad_m3", capacidadM3)
            }
            put("tipo_unidad", "MIXER")
        }

        val nuevoVehiculo = buildJsonObject {
            put("placa", placa)
            if (!marca.isNullOrEmpty()) put("marca", marca)
            if (!modelo.isNullOrEmpty()) put("modelo", modelo)
            if (anio != null && anio > 0) put("anio", anio)
            put("estado", estado)
            put("metadatos", metadatosJson)
        }

        cliente.postgrest["vehiculos"].insert(nuevoVehiculo)
    }

    // Actualizar estado operativo del vehículo (ACTIVO, MANTENIMIENTO, INACTIVO)
    suspend fun cambiarEstadoVehiculo(vehiculoId: String, nuevoEstado: String) {
        cliente.postgrest["vehiculos"]
            .update({
                set("estado", nuevoEstado)
            }) {
                filter {
                    eq("id", vehiculoId)
                }
            }
    }

    // Actualizar datos de un vehículo existente
    suspend fun actualizarVehiculo(
        vehiculoId: String,
        placa: String,
        marca: String?,
        modelo: String?,
        anio: Int?,
        estado: String,
        capacidadM3: Double?
    ) {
        val metadatosJson = buildJsonObject {
            if (capacidadM3 != null && capacidadM3 > 0) {
                put("capacidad_m3", capacidadM3)
            }
            put("tipo_unidad", "MIXER")
        }

        cliente.postgrest["vehiculos"]
            .update({
                set("placa", placa)
                set("marca", marca)
                set("modelo", modelo)
                set("anio", anio)
                set("estado", estado)
                set("metadatos", metadatosJson)
            }) {
                filter {
                    eq("id", vehiculoId)
                }
            }
    }

    // Eliminar vehículo por ID
    suspend fun eliminarVehiculo(vehiculoId: String) {
        cliente.postgrest["vehiculos"]
            .delete {
                filter {
                    eq("id", vehiculoId)
                }
            }
    }

    // Obtener todas las ubicaciones en vivo
    suspend fun obtenerUbicacionesEnVivo(): List<UbicacionEnVivo> {
        return cliente.postgrest["ubicacion_en_vivo"]
            .select()
            .decodeList<UbicacionEnVivo>()
    }

    // Actualizar estado activo/inactivo del perfil de un trabajador
    suspend fun cambiarEstadoTrabajador(usuarioId: String, activo: Boolean) {
        cliente.postgrest["perfiles"]
            .update({
                set("activo", activo)
            }) {
                filter {
                    eq("id", usuarioId)
                }
            }
    }

    // Actualizar datos del trabajador
    suspend fun actualizarDatosTrabajador(
        usuarioId: String,
        nombre: String,
        apellido: String,
        telefono: String,
        vehiculoId: String? = null,
        claveProvisional: String? = null
    ) {
        val metadatosJson = buildJsonObject {
            if (!claveProvisional.isNullOrEmpty()) {
                put("clave_provisional", claveProvisional)
            }
            if (!vehiculoId.isNullOrEmpty()) {
                put("vehiculo_id", vehiculoId)
            }
        }

        cliente.postgrest["perfiles"]
            .update({
                set("nombre", nombre)
                set("apellido", apellido)
                set("telefono", telefono)
                set("metadatos", metadatosJson)
            }) {
                filter {
                    eq("id", usuarioId)
                }
            }
    }

    // Crear un nuevo chofer en la base de datos usando la función RPC
    suspend fun registrarNuevoChofer(
        email: String,
        password: String,
        nombre: String,
        apellido: String,
        telefono: String
    ): String {
        val parametrosRpc = buildJsonObject {
            put("p_email", email)
            put("p_password", password)
            put("p_nombre", nombre)
            put("p_apellido", apellido)
            put("p_telefono", telefono)
            put("p_rol", "CONDUCTOR")
        }

        // Llamada a la función RPC SQL 'crear_usuario_sistema'
        val resultadoRpc = cliente.postgrest.rpc("crear_usuario_sistema", parametrosRpc)
        val usuarioId = resultadoRpc.data.replace("\"", "").trim()

        // Guardar metadatos adicionales como clave provisional y actualizar columna correo
        val metadatosJson = buildJsonObject {
            put("clave_provisional", password)
        }

        val perfilDto = PerfilRegistroDto(
            id = usuarioId,
            correo = email,
            nombre = nombre,
            apellido = apellido,
            telefono = telefono,
            rol = "CONDUCTOR",
            activo = true,
            metadatos = metadatosJson
        )

        cliente.postgrest["perfiles"].upsert(perfilDto)

        return usuarioId
    }

    // Cierre de sesión en Supabase Auth
    suspend fun cerrarSesion() {
        cliente.auth.signOut()
    }
}