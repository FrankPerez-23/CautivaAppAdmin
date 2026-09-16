package com.example.cautivaappadmin.vistas

import android.os.Bundle
import android.text.InputFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.cautivaappadmin.R
import com.example.cautivaappadmin.databinding.FragmentoRegistroVehiculoBinding
import com.example.cautivaappadmin.datos.ClienteSupabase
import com.example.cautivaappadmin.modelo.Vehiculo
import kotlinx.coroutines.launch

class FragmentoRegistroVehiculo : Fragment() {

    private var _enlace: FragmentoRegistroVehiculoBinding? = null
    private val enlace get() = _enlace!!

    private var vehiculoAEditar: Vehiculo? = null

    private val marcasSugeridas = listOf(
        "Volvo", "Mercedes-Benz", "Scania", "Hino", "Mack", "International", "Freightliner", "Volkswagen"
    )

    override fun onCreateView(
        inflador: LayoutInflater,
        contenedor: ViewGroup?,
        estadoGuardado: Bundle?
    ): View {
        _enlace = FragmentoRegistroVehiculoBinding.inflate(inflador, contenedor, false)
        return enlace.root
    }

    override fun onViewCreated(vista: View, estadoGuardado: Bundle?) {
        super.onViewCreated(vista, estadoGuardado)

        // Forzar Placa en MAYÚSCULAS automáticamente
        enlace.campoPlacaVehiculo.filters = arrayOf(InputFilter.AllCaps())

        // Configurar sugerencias de Marca
        val adaptadorMarcas = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            marcasSugeridas
        )
        enlace.campoMarcaVehiculo.setAdapter(adaptadorMarcas)

        // Si se pasa un vehículo para edición
        if (vehiculoAEditar != null) {
            cargarDatosVehiculoEdicion(vehiculoAEditar!!)
        }

        // Botón Cerrar "X" y Botón Cancelar
        enlace.botonCerrarRegistroVehiculo.setOnClickListener {
            cerrarFormulario()
        }

        enlace.botonCancelarRegistroVehiculo.setOnClickListener {
            cerrarFormulario()
        }

        // Botón Guardar
        enlace.botonGuardarVehiculo.setOnClickListener {
            procesarGuardadoVehiculo()
        }
    }

    fun establecerVehiculoAEditar(vehiculo: Vehiculo) {
        this.vehiculoAEditar = vehiculo
    }

    private fun cargarDatosVehiculoEdicion(vehiculo: Vehiculo) {
        enlace.textoTituloRegistroVehiculo.text = "Editar Vehículo"
        enlace.campoPlacaVehiculo.setText(vehiculo.placa)
        enlace.campoMarcaVehiculo.setText(vehiculo.marca ?: "")
        enlace.campoModeloVehiculo.setText(vehiculo.modelo ?: "")
        enlace.campoAnioVehiculo.setText(vehiculo.anio?.toString() ?: "")
        enlace.campoCapacidadM3.setText(vehiculo.capacidadM3?.toString() ?: "")

        when (vehiculo.estado.uppercase()) {
            "MANTENIMIENTO" -> enlace.radioMantenimiento.isChecked = true
            "INACTIVO" -> enlace.radioInactivo.isChecked = true
            else -> enlace.radioActivo.isChecked = true
        }
    }

    private fun procesarGuardadoVehiculo() {
        val placa = enlace.campoPlacaVehiculo.text.toString().trim().uppercase()
        val marca = enlace.campoMarcaVehiculo.text.toString().trim()
        val modelo = enlace.campoModeloVehiculo.text.toString().trim()
        val anioTexto = enlace.campoAnioVehiculo.text.toString().trim()
        val capacidadTexto = enlace.campoCapacidadM3.text.toString().trim()

        val estado = when (enlace.grupoEstadoVehiculo.checkedRadioButtonId) {
            R.id.radio_mantenimiento -> "MANTENIMIENTO"
            R.id.radio_inactivo -> "INACTIVO"
            else -> "ACTIVO"
        }

        if (placa.isEmpty()) {
            Toast.makeText(requireContext(), "Por favor ingresa la placa de la unidad", Toast.LENGTH_SHORT).show()
            return
        }

        val anio = anioTexto.toIntOrNull()
        if (anio != null && (anio < 1980 || anio > 2030)) {
            Toast.makeText(requireContext(), "Ingresa un año válido (ej. 2023)", Toast.LENGTH_SHORT).show()
            return
        }

        val capacidadM3 = capacidadTexto.toDoubleOrNull()

        mostrarCargando(true)

        lifecycleScope.launch {
            try {
                if (vehiculoAEditar != null && vehiculoAEditar?.id != null) {
                    // Modo Edición
                    ClienteSupabase.actualizarVehiculo(
                        vehiculoId = vehiculoAEditar!!.id!!,
                        placa = placa,
                        marca = marca,
                        modelo = modelo,
                        anio = anio,
                        estado = estado,
                        capacidadM3 = capacidadM3
                    )
                    Toast.makeText(requireContext(), "Vehículo actualizado exitosamente", Toast.LENGTH_SHORT).show()
                } else {
                    // Modo Nuevo Registro
                    ClienteSupabase.insertarVehiculo(
                        placa = placa,
                        marca = marca,
                        modelo = modelo,
                        anio = anio,
                        estado = estado,
                        capacidadM3 = capacidadM3
                    )
                    Toast.makeText(requireContext(), "Vehículo registrado con éxito", Toast.LENGTH_SHORT).show()
                }

                mostrarCargando(false)
                cerrarFormulario()

            } catch (e: Exception) {
                mostrarCargando(false)
                val errorMsj = e.localizedMessage ?: ""
                if (errorMsj.contains("duplicate key") || errorMsj.contains("unique constraint") || errorMsj.contains("vehiculos_placa_key")) {
                    Toast.makeText(
                        requireContext(),
                        "La placa ingresada ya se encuentra registrada",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(requireContext(), "Error: $errorMsj", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun cerrarFormulario() {
        parentFragmentManager.popBackStack()
    }

    private fun mostrarCargando(cargando: Boolean) {
        if (cargando) {
            enlace.barraProgresoVehiculo.visibility = View.VISIBLE
            enlace.botonGuardarVehiculo.isEnabled = false
            enlace.botonCancelarRegistroVehiculo.isEnabled = false
        } else {
            enlace.barraProgresoVehiculo.visibility = View.GONE
            enlace.botonGuardarVehiculo.isEnabled = true
            enlace.botonCancelarRegistroVehiculo.isEnabled = true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _enlace = null
    }
}