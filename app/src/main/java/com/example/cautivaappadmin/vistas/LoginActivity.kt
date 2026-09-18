package com.example.cautivaappadmin.vistas

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import com.example.cautivaappadmin.MainActivity
import com.example.cautivaappadmin.datos.ClienteSupabase
import com.example.cautivaappadmin.datos.GestorSesion
import com.example.cautivaappadmin.databinding.ActivityLoginBinding
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var enlace: ActivityLoginBinding
    private lateinit var gestorSesion: GestorSesion

    override fun onCreate(estadoGuardado: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(estadoGuardado)
        enlace = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(enlace.root)

        gestorSesion = GestorSesion(this)

        // Verificar si ya existe una sesión guardada de administrador
        if (gestorSesion.estaAutenticado()) {
            irAMainActivity()
            return
        }

        enlace.botonIniciarSesion.setOnClickListener {
            procesarInicioSesion()
        }
    }

    private fun procesarInicioSesion() {
        val correo = enlace.campoCorreo.text.toString().trim()
        val contrasena = enlace.campoContrasena.text.toString().trim()

        if (correo.isEmpty() || contrasena.isEmpty()) {
            mostrarError("Por favor, ingresa tu correo y contraseña.")
            return
        }

        ocultarError()
        mostrarCargando(true)

        lifecycleScope.launch {
            try {
                // 1. Autenticar con Supabase Auth
                ClienteSupabase.autenticarAdministrador(correo, contrasena)
                val usuarioId = ClienteSupabase.obtenerUsuarioActualId()

                if (usuarioId == null) {
                    mostrarError("No se pudo obtener la sesión del usuario.")
                    mostrarCargando(false)
                    return@launch
                }

                // 2. Verificar rol en la tabla 'perfiles'
                val perfil = ClienteSupabase.obtenerPerfil(usuarioId)

                if (perfil.rol == "ADMINISTRADOR" || perfil.rol == "SUPERVISOR") {
                    // Guardar sesión segura localmente
                    gestorSesion.guardarSesion(
                        token = usuarioId,
                        idUsuario = usuarioId,
                        correo = correo,
                        nombre = perfil.nombreCompleto,
                        rol = perfil.rol
                    )

                    mostrarCargando(false)
                    Toast.makeText(this@LoginActivity, "Bienvenido, ${perfil.nombre}", Toast.LENGTH_SHORT).show()
                    irAMainActivity()
                } else {
                    // Si el rol es CONDUCTOR u otro no autorizado
                    ClienteSupabase.cerrarSesion()
                    gestorSesion.cerrarSesion()
                    mostrarCargando(false)
                    mostrarError("Acceso denegado: esta app es exclusiva para administradores")
                }

            } catch (e: Exception) {
                mostrarCargando(false)
                val mensajeError = e.localizedMessage ?: "Error de autenticación"
                if (mensajeError.contains("Invalid login credentials")) {
                    mostrarError("Credenciales incorrectas. Verifique correo y contraseña.")
                } else {
                    mostrarError("Error: $mensajeError")
                }
            }
        }
    }

    private fun mostrarCargando(cargando: Boolean) {
        if (cargando) {
            enlace.barraProgresoLogin.visibility = View.VISIBLE
            enlace.botonIniciarSesion.isEnabled = false
        } else {
            enlace.barraProgresoLogin.visibility = View.GONE
            enlace.botonIniciarSesion.isEnabled = true
        }
    }

    private fun mostrarError(mensaje: String) {
        enlace.textoErrorLogin.text = mensaje
        enlace.textoErrorLogin.visibility = View.VISIBLE
    }

    private fun ocultarError() {
        enlace.textoErrorLogin.visibility = View.GONE
    }

    private fun irAMainActivity() {
        val intencion = Intent(this, MainActivity::class.java)
        startActivity(intencion)
        finish()
    }
}