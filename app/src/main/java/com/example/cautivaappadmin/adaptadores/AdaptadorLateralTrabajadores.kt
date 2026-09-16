package com.example.cautivaappadmin.adaptadores

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.cautivaappadmin.R
import com.example.cautivaappadmin.databinding.ItemLateralTrabajadorBinding
import com.example.cautivaappadmin.modelo.Perfil
import com.example.cautivaappadmin.modelo.UbicacionEnVivo
import com.example.cautivaappadmin.modelo.Vehiculo

class AdaptadorLateralTrabajadores(
    private var listaTrabajadores: List<Perfil>,
    private var mapaVehiculos: Map<String, Vehiculo>,
    private var mapaUbicaciones: Map<String, UbicacionEnVivo>,
    private val alSeleccionar: (Perfil) -> Unit
) : RecyclerView.Adapter<AdaptadorLateralTrabajadores.VistaModeloLateral>() {

    inner class VistaModeloLateral(val enlace: ItemLateralTrabajadorBinding) :
        RecyclerView.ViewHolder(enlace.root)

    override fun onCreateViewHolder(padre: ViewGroup, tipoVista: Int): VistaModeloLateral {
        val enlace = ItemLateralTrabajadorBinding.inflate(
            LayoutInflater.from(padre.context),
            padre,
            false
        )
        return VistaModeloLateral(enlace)
    }

    override fun onBindViewHolder(holedor: VistaModeloLateral, posicion: Int) {
        val trabajador = listaTrabajadores[posicion]
        val enlace = holedor.enlace
        val contexto = enlace.root.context

        enlace.textoNombreLateral.text = trabajador.nombreCompleto

        // Vehículo asignado
        val vehiculo = trabajador.vehiculoAsignadoId?.let { mapaVehiculos[it] }
        enlace.textoCamionLateral.text = if (vehiculo != null) {
            "Camión: ${vehiculo.placa}"
        } else {
            "Sin camión asignado"
        }

        // Estado en vivo (punto verde si activo y tiene reporte reciente / punto gris si inactivo)
        val estaActivoEnRuta = trabajador.activo && mapaUbicaciones.containsKey(trabajador.id)
        if (estaActivoEnRuta) {
            enlace.indicadorEstadoLateral.setColorFilter(
                ContextCompat.getColor(contexto, R.color.estado_activo)
            )
        } else {
            enlace.indicadorEstadoLateral.setColorFilter(
                ContextCompat.getColor(contexto, R.color.gris_medio)
            )
        }

        enlace.root.setOnClickListener {
            alSeleccionar(trabajador)
        }
    }

    override fun getItemCount(): Int = listaTrabajadores.size

    fun actualizarDatos(
        nuevaLista: List<Perfil>,
        nuevoMapaVehiculos: Map<String, Vehiculo>,
        nuevoMapaUbicaciones: Map<String, UbicacionEnVivo>
    ) {
        this.listaTrabajadores = nuevaLista.sortedBy { it.nombreCompleto.lowercase() }
        this.mapaVehiculos = nuevoMapaVehiculos
        this.mapaUbicaciones = nuevoMapaUbicaciones
        notifyDataSetChanged()
    }
}