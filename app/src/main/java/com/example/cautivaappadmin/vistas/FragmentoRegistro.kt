package com.example.cautivaappadmin.vistas

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.cautivaappadmin.databinding.FragmentoRegistroBinding
import com.example.cautivaappadmin.datos.ClienteSupabase
import kotlinx.coroutines.launch

class FragmentoRegistro : Fragment() {

    private var _enlace: FragmentoRegistroBinding? = null
    private val enlace get() = _enlace!!

    override fun onCreateView(
        inflador: LayoutInflater,
        contenedor: ViewGroup?,
        estadoGuardado: Bundle?
    ): View {
        _enlace = FragmentoRegistroBinding.inflate(inflador, contenedor, false)
        return enlace.root
    }

    override fun onViewCreated(vista: View, estadoGuardado: Bundle?) {
        super.onViewCreated(vista, estadoGuardado)

        enlace.botonRegistrarConductor.setOnClickListener {
            procesarRegistroConductor()
        }
    }

    private fun procesarRegistroConductor() {
        val nombre = enlace.campoNombreRegistro.text.toString().trim()
        val apellido = enlace.campoApellidoRegistro.text.toString().trim()
        val correo = enlace.campoCorreoRegistro.text.toString().trim()
        val telefono = enlace.campoTelefonoRegistro.text.toString().trim()
        val claveProvisional = enlace.campoClaveProvisionalRegistro.text.toString().trim()

        if (nombre.isEmpty() || apellido.isEmpty() || correo.isEmpty() || claveProvisional.isEmpty()) {
            Toast.makeText(requireContext(), "Por favor completa todos los campos requeridos", Toast.LENGTH_SHORT).show()
            return
        }

        if (!correo.contains("@cautiva.com") && !correo.contains("@")) {
            Toast.makeText(requireContext(), "El correo debe ser corporativo (@cautiva.com)", Toast.LENGTH_SHORT).show()
            return
        }

        if (claveProvisional.length < 6) {
            Toast.makeText(requireContext(), "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
            return
        }

        mostrarCargando(true)

        lifecycleScope.launch {
            try {
                // Registrar chofer en Supabase (Auth + Registro en perfiles con correo y metadatos)
                ClienteSupabase.registrarNuevoChofer(
                    email = correo,
                    password = claveProvisional,
                    nombre = nombre,
                    apellido = apellido,
                    telefono = telefono
                )

                mostrarCargando(false)
                mostrarDialogoConfirmacion(nombre, apellido, correo, claveProvisional)
                limpiarFormulario()

            } catch (e: Exception) {
                mostrarCargando(false)
                val mensajeError = e.localizedMessage ?: "Error desconocido"
                if (mensajeError.contains("duplicate key") || mensajeError.contains("already registered")) {
                    Toast.makeText(
                        requireContext(),
                        "El correo ingresado ya se encuentra registrado",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Error al registrar chofer: $mensajeError",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun mostrarDialogoConfirmacion(nombre: String, apellido: String, correo: String, clave: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Registro Exitoso")
            .setMessage("Chofer registrado exitosamente con clave temporal: $clave\n\nNombre: $nombre $apellido\nCorreo: $correo")
            .setPositiveButton("Aceptar", null)
            .show()
    }

    private fun limpiarFormulario() {
        enlace.campoNombreRegistro.text?.clear()
        enlace.campoApellidoRegistro.text?.clear()
        enlace.campoCorreoRegistro.text?.clear()
        enlace.campoTelefonoRegistro.text?.clear()
        enlace.campoClaveProvisionalRegistro.text?.clear()
    }

    private fun mostrarCargando(cargando: Boolean) {
        if (cargando) {
            enlace.barraProgresoRegistro.visibility = View.VISIBLE
            enlace.botonRegistrarConductor.isEnabled = false
        } else {
            enlace.barraProgresoRegistro.visibility = View.GONE
            enlace.botonRegistrarConductor.isEnabled = true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _enlace = null
    }
}