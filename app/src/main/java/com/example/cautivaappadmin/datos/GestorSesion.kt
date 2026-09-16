package com.example.cautivaappadmin.datos

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class GestorSesion(contexto: Context) {

    private val preferencias: SharedPreferences = try {
        val masterKey = MasterKey.Builder(contexto)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            contexto,
            NOMBRE_PREFERENCIAS,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        contexto.getSharedPreferences(NOMBRE_PREFERENCIAS, Context.MODE_PRIVATE)
    }

    companion object {
        private const val NOMBRE_PREFERENCIAS = "cautiva_sesion_admin_pref"
        private const val CLAVE_TOKEN = "token_sesion"
        private const val CLAVE_ID_USUARIO = "id_usuario"
        private const val CLAVE_CORREO = "correo_usuario"
        private const val CLAVE_NOMBRE = "nombre_usuario"
        private const val CLAVE_ROL = "rol_usuario"
        private const val CLAVE_FRECUENCIA_MAPA = "frecuencia_mapa"
        private const val CLAVE_FILTRO_MOVIMIENTO = "filtro_movimiento"
        private const val CLAVE_ALERTA_BATERIA = "alerta_bateria"
    }

    fun guardarSesion(
        token: String,
        idUsuario: String,
        correo: String,
        nombre: String,
        rol: String
    ) {
        preferencias.edit()
            .putString(CLAVE_TOKEN, token)
            .putString(CLAVE_ID_USUARIO, idUsuario)
            .putString(CLAVE_CORREO, correo)
            .putString(CLAVE_NOMBRE, nombre)
            .putString(CLAVE_ROL, rol)
            .apply()
    }

    fun estaAutenticado(): Boolean {
        val token = preferencias.getString(CLAVE_TOKEN, null)
        val rol = preferencias.getString(CLAVE_ROL, null)
        return !token.isNull_or_Empty() && (rol == "ADMINISTRADOR" || rol == "SUPERVISOR")
    }

    private fun String?.isNull_or_Empty(): Boolean = this == null || this.trim().isEmpty()

    fun obtenerIdUsuario(): String? = preferencias.getString(CLAVE_ID_USUARIO, null)
    fun obtenerCorreo(): String? = preferencias.getString(CLAVE_CORREO, "")
    fun obtenerNombre(): String? = preferencias.getString(CLAVE_NOMBRE, "Administrador")
    fun obtenerRol(): String? = preferencias.getString(CLAVE_ROL, "ADMINISTRADOR")

    var frecuenciaMapa: Int
        get() = preferencias.getInt(CLAVE_FRECUENCIA_MAPA, 10)
        set(valor) = preferencias.edit().putInt(CLAVE_FRECUENCIA_MAPA, valor).apply()

    var soloEnMovimiento: Boolean
        get() = preferencias.getBoolean(CLAVE_FILTRO_MOVIMIENTO, false)
        set(valor) = preferencias.edit().putBoolean(CLAVE_FILTRO_MOVIMIENTO, valor).apply()

    var alertaBateriaBaja: Boolean
        get() = preferencias.getBoolean(CLAVE_ALERTA_BATERIA, true)
        set(valor) = preferencias.edit().putBoolean(CLAVE_ALERTA_BATERIA, valor).apply()

    fun cerrarSesion() {
        preferencias.edit()
            .remove(CLAVE_TOKEN)
            .remove(CLAVE_ID_USUARIO)
            .remove(CLAVE_CORREO)
            .remove(CLAVE_NOMBRE)
            .remove(CLAVE_ROL)
            .apply()
    }
}