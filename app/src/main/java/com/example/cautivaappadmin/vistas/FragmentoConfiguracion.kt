package com.example.cautivaappadmin.vistas

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.cautivaappadmin.databinding.FragmentoConfiguracionBinding
import com.example.cautivaappadmin.datos.ClienteSupabase
import com.example.cautivaappadmin.datos.GestorSesion
import kotlinx.coroutines.launch

class FragmentoConfiguracion : Fragment() {

    private var _enlace: FragmentoConfiguracionBinding? = null
    private val enlace get() = _enlace!!

    private lateinit var gestorSesion: GestorSesion

    private val opcionesFrecuencia = listOf(5, 10, 15, 30)

    override fun onCreateView(
        inflador: LayoutInflater,
        contenedor: ViewGroup?,
        estadoGuardado: Bundle?
    ): View {
        _enlace = FragmentoConfiguracionBinding.inflate(inflador, contenedor, false)
        return enlace.root
    }

    override fun onViewCreated(vista: View, estadoGuardado: Bundle?) {
        super.onViewCreated(vista, estadoGuardado)

        gestorSesion = GestorSesion(requireContext())

        // Cargar datos de administrador actual
        enlace.textoNombreAdmin.text = gestorSesion.obtenerNombre()
        enlace.textoCorreoAdmin.text = gestorSesion.obtenerCorreo()
        enlace.textoRolAdmin.text = "ROL: ${gestorSesion.obtenerRol()}"

        // Configurar Spinner de Frecuencia de Mapa
        val opcionesTexto = opcionesFrecuencia.map { "$it segundos" }
        val adaptadorSpinner = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            opcionesTexto
        )
        adaptadorSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        enlace.selectorFrecuenciaMapa.adapter = adaptadorSpinner

        // Seleccionar frecuencia guardada
        val frecuenciaActual = gestorSesion.frecuenciaMapa
        val indiceFrecuencia = opcionesFrecuencia.indexOf(frecuenciaActual)
        if (indiceFrecuencia >= 0) {
            enlace.selectorFrecuenciaMapa.setSelection(indiceFrecuencia)
        }

        enlace.selectorFrecuenciaMapa.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val nuevaFrecuencia = opcionesFrecuencia[position]
                gestorSesion.frecuenciaMapa = nuevaFrecuencia
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Configurar Switches
        enlace.interruptorSoloMovimiento.isChecked = gestorSesion.soloEnMovimiento
        enlace.interruptorSoloMovimiento.setOnCheckedChangeListener { _, checked ->
            gestorSesion.soloEnMovimiento = checked
        }

        enlace.interruptorAlertaBateria.isChecked = gestorSesion.alertaBateriaBaja
        enlace.interruptorAlertaBateria.setOnCheckedChangeListener { _, checked ->
            gestorSesion.alertaBateriaBaja = checked
        }

        // Botón Cerrar Sesión
        enlace.botonCerrarSesion.setOnClickListener {
            confirmarCierreSesion()
        }
    }

    private fun confirmarCierreSesion() {
        AlertDialog.Builder(requireContext())
            .setTitle("Cerrar Sesión")
            .setMessage("¿Estás seguro de que deseas cerrar sesión?")
            .setPositiveButton("Sí, salir") { _, _ ->
                cerrarSesion()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun cerrarSesion() {
        lifecycleScope.launch {
            try {
                ClienteSupabase.cerrarSesion()
            } catch (e: Exception) {
                // Si falla el cierre en servidor, limpia local igualmente
            } finally {
                gestorSesion.cerrarSesion()
                Toast.makeText(requireContext(), "Sesión cerrada correctamente", Toast.LENGTH_SHORT).show()

                val intencion = Intent(requireContext(), LoginActivity::class.java)
                intencion.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intencion)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _enlace = null
    }
}