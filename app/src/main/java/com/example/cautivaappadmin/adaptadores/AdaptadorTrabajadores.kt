package com.example.cautivaappadmin.adaptadores

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.cautivaappadmin.R
import com.example.cautivaappadmin.databinding.ItemTrabajadorBinding
import com.example.cautivaappadmin.modelo.Perfil
import com.example.cautivaappadmin.modelo.Vehiculo

class AdaptadorTrabajadores(
    private var listaOriginal: List<Perfil>,
    private var mapaVehiculos: Map<String, Vehiculo>,
    private var mapaTurnosActivos: Map<String, String> = emptyMap(),
    private val alCambiarEstado: (Perfil, Boolean) -> Unit,
    private val alEditar: (Perfil) -> Unit
) : RecyclerView.Adapter<AdaptadorTrabajadores.VistaModeloTrabajador>() {

    private var listaFiltrada: MutableList<Perfil> = mutableListOf()
    private val clavesVisibles = mutableSetOf<String>()

    init {
        actualizarListaInterna(listaOriginal)
    }

    inner class VistaModeloTrabajador(val enlace: ItemTrabajadorBinding) :
        RecyclerView.ViewHolder(enlace.root)

    override fun onCreateViewHolder(padre: ViewGroup, tipoVista: Int): VistaModeloTrabajador {
        val enlace = ItemTrabajadorBinding.inflate(
            LayoutInflater.from(padre.context),
            padre,
            false
        )
        return VistaModeloTrabajador(enlace)
    }

    override fun onBindViewHolder(holedor: VistaModeloTrabajador, posicion: Int) {
        val trabajador = listaFiltrada[posicion]
        val enlace = holedor.enlace
        val contexto = enlace.root.context

        enlace.textoNombreTrabajador.text = trabajador.nombreCompleto
        enlace.textoCorreoTrabajador.text = trabajador.correo ?: "Rol: ${trabajador.rol}"
        enlace.textoTelefonoTrabajador.text = trabajador.telefono ?: "Sin teléfono"

        // Mostrar vehículo asignado durante jornada (priorizar turno activo de BD y luego metadatos)
        val vehiculoIdDirecto = trabajador.vehiculoAsignadoId
        val vehiculoIdTurno = mapaTurnosActivos[trabajador.id]
        val vehiculoId = if (!vehiculoIdTurno.isNullOrEmpty()) vehiculoIdTurno else vehiculoIdDirecto
        val vehiculo = if (!vehiculoId.isNullOrEmpty()) mapaVehiculos[vehiculoId] else null

        enlace.textoVehiculoTrabajador.text = if (vehiculo != null) {
            "Vehículo: Placa ${vehiculo.placa}"
        } else {
            "Vehículo: Sin asignación"
        }

        // Estado Activo/Inactivo (Los deshabilitados van al final de la lista)
        enlace.interruptorEstadoTrabajador.setOnCheckedChangeListener(null)
        enlace.interruptorEstadoTrabajador.isChecked = trabajador.activo
        enlace.interruptorEstadoTrabajador.setOnCheckedChangeListener { _, estaActivo ->
            alCambiarEstado(trabajador, estaActivo)
        }

        // Contraseña
        val claveProv = if (!trabajador.claveProvisional.isNullOrEmpty()) trabajador.claveProvisional!! else "••••••••"
        val esVisible = clavesVisibles.contains(trabajador.id)

        if (esVisible) {
            enlace.textoClaveProvisional.text = claveProv
            enlace.botonVerClave.setImageResource(R.drawable.ic_ojo_cerrado)
        } else {
            enlace.textoClaveProvisional.text = "••••••••"
            enlace.botonVerClave.setImageResource(R.drawable.ic_ojo)
        }

        // Botón Ver/Ocultar Clave
        enlace.botonVerClave.setOnClickListener {
            if (esVisible) {
                clavesVisibles.remove(trabajador.id)
            } else {
                clavesVisibles.add(trabajador.id)
            }
            notifyItemChanged(posicion)
        }

        // Botón Copiar Clave
        enlace.botonCopiarClave.setOnClickListener {
            val portapapeles = contexto.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Contraseña", claveProv)
            portapapeles.setPrimaryClip(clip)
            Toast.makeText(contexto, "Contraseña copiada al portapapeles", Toast.LENGTH_SHORT).show()
        }

        // Botón Editar Datos
        enlace.botonEditarTrabajador.setOnClickListener {
            alEditar(trabajador)
        }
    }

    override fun getItemCount(): Int = listaFiltrada.size

    fun actualizarDatos(
        nuevaLista: List<Perfil>,
        nuevoMapaVehiculos: Map<String, Vehiculo>,
        nuevoMapaTurnosActivos: Map<String, String> = emptyMap()
    ) {
        this.mapaVehiculos = nuevoMapaVehiculos
        this.mapaTurnosActivos = nuevoMapaTurnosActivos
        actualizarListaInterna(nuevaLista)
        notifyDataSetChanged()
    }

    private fun actualizarListaInterna(lista: List<Perfil>) {
        // Ordenar trabajadores: primero activos, al final deshabilitados
        val listaOrdenada = lista.sortedWith(
            compareByDescending<Perfil> { it.activo }
                .thenBy { it.nombreCompleto }
        )
        this.listaOriginal = listaOrdenada
        this.listaFiltrada = listaOrdenada.toMutableList()
    }

    fun filtrar(texto: String) {
        val consulta = texto.trim().lowercase()
        listaFiltrada = if (consulta.isEmpty()) {
            listaOriginal.toMutableList()
        } else {
            listaOriginal.filter { perfil ->
                val vehiculoIdDirecto = perfil.vehiculoAsignadoId
                val vehiculoIdTurno = mapaTurnosActivos[perfil.id]
                val vehiculoId = if (!vehiculoIdTurno.isNullOrEmpty()) vehiculoIdTurno else vehiculoIdDirecto
                val vehiculo = if (!vehiculoId.isNullOrEmpty()) mapaVehiculos[vehiculoId] else null

                perfil.nombreCompleto.lowercase().contains(consulta) ||
                        (perfil.correo?.lowercase()?.contains(consulta) == true) ||
                        (vehiculo != null && vehiculo.placa.lowercase().contains(consulta))
            }.toMutableList()
        }
        notifyDataSetChanged()
    }
}