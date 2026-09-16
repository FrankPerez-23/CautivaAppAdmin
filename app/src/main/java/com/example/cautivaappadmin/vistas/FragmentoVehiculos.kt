package com.example.cautivaappadmin.vistas

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.cautivaappadmin.R
import com.example.cautivaappadmin.adaptadores.AdaptadorVehiculos
import com.example.cautivaappadmin.databinding.FragmentoVehiculosBinding
import com.example.cautivaappadmin.datos.ClienteSupabase
import com.example.cautivaappadmin.modelo.Vehiculo
import kotlinx.coroutines.launch

class FragmentoVehiculos : Fragment() {

    private var _enlace: FragmentoVehiculosBinding? = null
    private val enlace get() = _enlace!!

    private var adaptador: AdaptadorVehiculos? = null
    private var listaVehiculos = listOf<Vehiculo>()

    override fun onCreateView(
        inflador: LayoutInflater,
        contenedor: ViewGroup?,
        estadoGuardado: Bundle?
    ): View {
        _enlace = FragmentoVehiculosBinding.inflate(inflador, contenedor, false)
        return enlace.root
    }

    override fun onViewCreated(vista: View, estadoGuardado: Bundle?) {
        super.onViewCreated(vista, estadoGuardado)

        // Escuchar cambios en la pila de sub-fragmentos hijos
        childFragmentManager.addOnBackStackChangedListener {
            if (childFragmentManager.backStackEntryCount == 0) {
                enlace.contenedorRegistroVehiculo.visibility = View.GONE
                cargarVehiculos()
            } else {
                enlace.contenedorRegistroVehiculo.visibility = View.VISIBLE
            }
        }

        enlace.recyclerVehiculos.layoutManager = LinearLayoutManager(requireContext())
        adaptador = AdaptadorVehiculos(
            listaVehiculos,
            alCambiarEstado = { vehiculo, nuevoEstado ->
                cambiarEstadoVehiculo(vehiculo, nuevoEstado)
            },
            alEditar = { vehiculo ->
                abrirFormularioRegistro(vehiculo)
            },
            alEliminar = { vehiculo ->
                confirmarEliminacion(vehiculo)
            }
        )
        enlace.recyclerVehiculos.adapter = adaptador

        // Filtro de búsqueda por placa o marca
        enlace.campoBusquedaVehiculos.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                adaptador?.filtrar(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Botón Flotante para Agregar Nuevo Vehículo
        enlace.botonAgregarVehiculo.setOnClickListener {
            abrirFormularioRegistro(null)
        }

        cargarVehiculos()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (hidden) {
            // Si el usuario cambia a otra pestaña (Mapa, Trabajadores, etc.), cerrar el registro si estaba abierto
            if (childFragmentManager.backStackEntryCount > 0) {
                childFragmentManager.popBackStackImmediate()
            }
        } else {
            cargarVehiculos()
        }
    }

    override fun onResume() {
        super.onResume()
        cargarVehiculos()
    }

    private fun cargarVehiculos() {
        enlace.barraCargandoVehiculos.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                listaVehiculos = ClienteSupabase.obtenerVehiculos()
                enlace.barraCargandoVehiculos.visibility = View.GONE

                if (listaVehiculos.isEmpty()) {
                    enlace.textoListaVaciaVehiculos.visibility = View.VISIBLE
                    enlace.recyclerVehiculos.visibility = View.GONE
                } else {
                    enlace.textoListaVaciaVehiculos.visibility = View.GONE
                    enlace.recyclerVehiculos.visibility = View.VISIBLE
                    adaptador?.actualizarDatos(listaVehiculos)
                }

            } catch (e: Exception) {
                enlace.barraCargandoVehiculos.visibility = View.GONE
                Toast.makeText(requireContext(), "Error al cargar flota vehicular: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun cambiarEstadoVehiculo(vehiculo: Vehiculo, nuevoEstado: String) {
        val vehiculoId = vehiculo.id ?: return
        lifecycleScope.launch {
            try {
                ClienteSupabase.cambiarEstadoVehiculo(vehiculoId, nuevoEstado)
                Toast.makeText(requireContext(), "Estado cambiado a $nuevoEstado", Toast.LENGTH_SHORT).show()
                cargarVehiculos()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error al cambiar estado: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun abrirFormularioRegistro(vehiculo: Vehiculo?) {
        val fragmentoRegistro = FragmentoRegistroVehiculo()
        if (vehiculo != null) {
            fragmentoRegistro.establecerVehiculoAEditar(vehiculo)
        }

        enlace.contenedorRegistroVehiculo.visibility = View.VISIBLE
        childFragmentManager.beginTransaction()
            .replace(R.id.contenedor_registro_vehiculo, fragmentoRegistro)
            .addToBackStack("registro_vehiculo")
            .commit()
    }

    private fun confirmarEliminacion(vehiculo: Vehiculo) {
        val vehiculoId = vehiculo.id ?: return
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar Vehículo")
            .setMessage("¿Estás seguro de que deseas eliminar la unidad ${vehiculo.placa}?")
            .setPositiveButton("Sí, eliminar") { _, _ ->
                eliminarVehiculo(vehiculoId)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun eliminarVehiculo(vehiculoId: String) {
        lifecycleScope.launch {
            try {
                ClienteSupabase.eliminarVehiculo(vehiculoId)
                Toast.makeText(requireContext(), "Vehículo eliminado correctamente", Toast.LENGTH_SHORT).show()
                cargarVehiculos()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error al eliminar vehículo: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _enlace = null
    }
}