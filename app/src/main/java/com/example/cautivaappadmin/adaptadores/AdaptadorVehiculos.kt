package com.example.cautivaappadmin.adaptadores

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.cautivaappadmin.R
import com.example.cautivaappadmin.databinding.ItemVehiculoBinding
import com.example.cautivaappadmin.modelo.Vehiculo

class AdaptadorVehiculos(
    private var listaOriginal: List<Vehiculo>,
    private val alCambiarEstado: (Vehiculo, String) -> Unit,
    private val alEditar: (Vehiculo) -> Unit,
    private val alEliminar: (Vehiculo) -> Unit
) : RecyclerView.Adapter<AdaptadorVehiculos.VistaModeloVehiculo>() {

    private var listaFiltrada: MutableList<Vehiculo> = listaOriginal.toMutableList()

    inner class VistaModeloVehiculo(val enlace: ItemVehiculoBinding) :
        RecyclerView.ViewHolder(enlace.root)

    override fun onCreateViewHolder(padre: ViewGroup, tipoVista: Int): VistaModeloVehiculo {
        val enlace = ItemVehiculoBinding.inflate(
            LayoutInflater.from(padre.context),
            padre,
            false
        )
        return VistaModeloVehiculo(enlace)
    }

    override fun onBindViewHolder(holedor: VistaModeloVehiculo, posicion: Int) {
        val vehiculo = listaFiltrada[posicion]
        val enlace = holedor.enlace
        val contexto = enlace.root.context

        enlace.textoPlacaVehiculo.text = vehiculo.placa
        enlace.textoDetalleVehiculo.text = vehiculo.resumenDetalle

        // Estado y Badge
        enlace.textoEstadoVehiculo.text = vehiculo.estado
        when (vehiculo.estado.uppercase()) {
            "ACTIVO" -> {
                enlace.textoEstadoVehiculo.setBackgroundResource(R.drawable.fondo_insignia_activo)
                enlace.textoEstadoVehiculo.setTextColor(ContextCompat.getColor(contexto, R.color.estado_activo))
            }
            "MANTENIMIENTO" -> {
                enlace.textoEstadoVehiculo.setBackgroundResource(R.drawable.fondo_insignia_mantenimiento)
                enlace.textoEstadoVehiculo.setTextColor(ContextCompat.getColor(contexto, R.color.estado_mantenimiento))
            }
            else -> {
                enlace.textoEstadoVehiculo.setBackgroundResource(R.drawable.fondo_insignia_inactivo)
                enlace.textoEstadoVehiculo.setTextColor(ContextCompat.getColor(contexto, R.color.estado_inactivo))
            }
        }

        // Capacidad del Mixer
        val capacidad = vehiculo.capacidadM3
        if (capacidad != null && capacidad > 0) {
            enlace.contenedorCapacidadVehiculo.visibility = View.VISIBLE
            enlace.textoCapacidadVehiculo.text = "Capacidad Mixer: $capacidad m³"
        } else {
            enlace.contenedorCapacidadVehiculo.visibility = View.GONE
        }

        // Menú de opciones de 3 puntos
        enlace.botonOpcionesVehiculo.setOnClickListener { vista ->
            mostrarMenuOpciones(contexto, vista, vehiculo)
        }
    }

    private fun mostrarMenuOpciones(contexto: Context, vistaAnchor: View, vehiculo: Vehiculo) {
        val menu = PopupMenu(contexto, vistaAnchor)
        menu.menu.add("Marcar ACTIVO")
        menu.menu.add("Marcar MANTENIMIENTO")
        menu.menu.add("Marcar INACTIVO")
        menu.menu.add("Editar Datos")
        menu.menu.add("Eliminar Vehículo")

        menu.setOnMenuItemClickListener { elemento ->
            when (elemento.title) {
                "Marcar ACTIVO" -> alCambiarEstado(vehiculo, "ACTIVO")
                "Marcar MANTENIMIENTO" -> alCambiarEstado(vehiculo, "MANTENIMIENTO")
                "Marcar INACTIVO" -> alCambiarEstado(vehiculo, "INACTIVO")
                "Editar Datos" -> alEditar(vehiculo)
                "Eliminar Vehículo" -> alEliminar(vehiculo)
            }
            true
        }
        menu.show()
    }

    override fun getItemCount(): Int = listaFiltrada.size

    fun actualizarDatos(nuevaLista: List<Vehiculo>) {
        this.listaOriginal = nuevaLista
        this.listaFiltrada = nuevaLista.toMutableList()
        notifyDataSetChanged()
    }

    fun filtrar(texto: String) {
        val consulta = texto.trim().lowercase()
        listaFiltrada = if (consulta.isEmpty()) {
            listaOriginal.toMutableList()
        } else {
            listaOriginal.filter { vehiculo ->
                vehiculo.placa.lowercase().contains(consulta) ||
                        vehiculo.descripcionVehiculo.lowercase().contains(consulta)
            }.toMutableList()
        }
        notifyDataSetChanged()
    }
}