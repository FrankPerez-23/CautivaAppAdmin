package com.example.cautivaappadmin.vistas

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.cautivaappadmin.adaptadores.AdaptadorTrabajadores
import com.example.cautivaappadmin.databinding.DialogoEditarTrabajadorBinding
import com.example.cautivaappadmin.databinding.FragmentoTrabajadoresBinding
import com.example.cautivaappadmin.datos.ClienteSupabase
import com.example.cautivaappadmin.modelo.Perfil
import com.example.cautivaappadmin.modelo.Vehiculo
import kotlinx.coroutines.launch

class FragmentoTrabajadores : Fragment() {

    private var _enlace: FragmentoTrabajadoresBinding? = null
    private val enlace get() = _enlace!!

    private var adaptador: AdaptadorTrabajadores? = null
    private var listaTrabajadores = listOf<Perfil>()
    private var mapaVehiculos = mapOf<String, Vehiculo>()
    private var listaVehiculos = listOf<Vehiculo>()

    override fun onCreateView(
        inflador: LayoutInflater,
        contenedor: ViewGroup?,
        estadoGuardado: Bundle?
    ): View {
        _enlace = FragmentoTrabajadoresBinding.inflate(inflador, contenedor, false)
        return enlace.root
    }

    override fun onViewCreated(vista: View, estadoGuardado: Bundle?) {
        super.onViewCreated(vista, estadoGuardado)

        enlace.recyclerTrabajadores.layoutManager = LinearLayoutManager(requireContext())
        adaptador = AdaptadorTrabajadores(
            listaTrabajadores,
            mapaVehiculos,
            alCambiarEstado = { trabajador, estaActivo ->
                cambiarEstadoTrabajador(trabajador, estaActivo)
            },
            alEditar = { trabajador ->
                mostrarDialogoEditar(trabajador)
            }
        )
        enlace.recyclerTrabajadores.adapter = adaptador

        // Buscador por nombre o placa
        enlace.campoBusquedaTrabajadores.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                adaptador?.filtrar(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        cargarDatos()
    }

    override fun onResume() {
        super.onResume()
        cargarDatos()
    }

    private fun cargarDatos() {
        enlace.barraCargandoTrabajadores.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                listaTrabajadores = ClienteSupabase.obtenerTrabajadores()
                listaVehiculos = ClienteSupabase.obtenerVehiculos()
                mapaVehiculos = listaVehiculos.filter { it.id != null }.associateBy { it.id!! }

                enlace.barraCargandoTrabajadores.visibility = View.GONE

                if (listaTrabajadores.isEmpty()) {
                    enlace.textoListaVacia.visibility = View.VISIBLE
                    enlace.recyclerTrabajadores.visibility = View.GONE
                } else {
                    enlace.textoListaVacia.visibility = View.GONE
                    enlace.recyclerTrabajadores.visibility = View.VISIBLE
                    adaptador?.actualizarDatos(listaTrabajadores, mapaVehiculos)
                }

            } catch (e: Exception) {
                enlace.barraCargandoTrabajadores.visibility = View.GONE
                Toast.makeText(requireContext(), "Error al cargar trabajadores: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun cambiarEstadoTrabajador(trabajador: Perfil, estaActivo: Boolean) {
        lifecycleScope.launch {
            try {
                ClienteSupabase.cambiarEstadoTrabajador(trabajador.id, estaActivo)
                val estadoTexto = if (estaActivo) "habilitado" else "deshabilitado"
                Toast.makeText(requireContext(), "Trabajador $estadoTexto", Toast.LENGTH_SHORT).show()
                cargarDatos()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error al cambiar estado: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun mostrarDialogoEditar(trabajador: Perfil) {
        val enlaceDialogo = DialogoEditarTrabajadorBinding.inflate(layoutInflater)
        val constructorDialogo = AlertDialog.Builder(requireContext())
            .setView(enlaceDialogo.root)

        val dialogo = constructorDialogo.create()

        // Prellenar campos
        enlaceDialogo.campoEditarNombre.setText(trabajador.nombre)
        enlaceDialogo.campoEditarApellido.setText(trabajador.apellido)
        enlaceDialogo.campoEditarTelefono.setText(trabajador.telefono ?: "")

        // Spinner de vehículos
        val adaptadorSpinner = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            listaVehiculos
        )
        adaptadorSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        enlaceDialogo.selectorEditarVehiculo.adapter = adaptadorSpinner

        // Seleccionar vehículo actual si existe
        val vehiculoActualId = trabajador.vehiculoAsignadoId
        if (vehiculoActualId != null) {
            val posicion = listaVehiculos.indexOfFirst { it.id == vehiculoActualId }
            if (posicion >= 0) {
                enlaceDialogo.selectorEditarVehiculo.setSelection(posicion)
            }
        }

        enlaceDialogo.botonCancelarEdicion.setOnClickListener {
            dialogo.dismiss()
        }

        enlaceDialogo.botonGuardarEdicion.setOnClickListener {
            val nuevoNombre = enlaceDialogo.campoEditarNombre.text.toString().trim()
            val nuevoApellido = enlaceDialogo.campoEditarApellido.text.toString().trim()
            val nuevoTelefono = enlaceDialogo.campoEditarTelefono.text.toString().trim()

            val vehiculoSeleccionado = enlaceDialogo.selectorEditarVehiculo.selectedItem as? Vehiculo
            val vehiculoId = vehiculoSeleccionado?.id

            if (nuevoNombre.isEmpty() || nuevoApellido.isEmpty()) {
                Toast.makeText(requireContext(), "Nombre y Apellido son obligatorios", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    ClienteSupabase.actualizarDatosTrabajador(
                        usuarioId = trabajador.id,
                        nombre = nuevoNombre,
                        apellido = nuevoApellido,
                        telefono = nuevoTelefono,
                        vehiculoId = vehiculoId,
                        claveProvisional = trabajador.claveProvisional
                    )
                    Toast.makeText(requireContext(), "Datos actualizados correctamente", Toast.LENGTH_SHORT).show()
                    dialogo.dismiss()
                    cargarDatos()
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Error al actualizar: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        dialogo.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _enlace = null
    }
}