package com.example.cautivaappadmin.vistas

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.cautivaappadmin.R
import com.example.cautivaappadmin.adaptadores.AdaptadorLateralTrabajadores
import com.example.cautivaappadmin.databinding.FragmentoMapaBinding
import com.example.cautivaappadmin.datos.ClienteSupabase
import com.example.cautivaappadmin.datos.GestorSesion
import com.example.cautivaappadmin.modelo.Perfil
import com.example.cautivaappadmin.modelo.UbicacionEnVivo
import com.example.cautivaappadmin.modelo.Vehiculo
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Locale
import java.util.TimeZone

class FragmentoMapa : Fragment(), OnMapReadyCallback {

    private var _enlace: FragmentoMapaBinding? = null
    private val enlace get() = _enlace!!

    private var mapaGoogle: GoogleMap? = null
    private lateinit var gestorSesion: GestorSesion
    private lateinit var clienteUbicacion: FusedLocationProviderClient

    // Colección de marcadores activos en memoria
    private val mapaMarcadores = mutableMapOf<String, Marker>()

    private var adaptadorLateral: AdaptadorLateralTrabajadores? = null

    private var listaTrabajadores = listOf<Perfil>()
    private var mapaVehiculos = mapOf<String, Vehiculo>()
    private var mapaUbicaciones = mapOf<String, UbicacionEnVivo>()

    private var trabajoActualizacionEnVivo: Job? = null

    // Solicitud dinámica de permisos de ubicación
    private val lanzadorPermisosUbicacion = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permisos ->
        val fineGranted = permisos[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permisos[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (fineGranted || coarseGranted) {
            obtenerUbicacionActualYCentrar()
        } else {
            Toast.makeText(
                requireContext(),
                "Permiso de ubicación denegado. Se mostrará vista general.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onCreateView(
        inflador: LayoutInflater,
        contenedor: ViewGroup?,
        estadoGuardado: Bundle?
    ): View {
        _enlace = FragmentoMapaBinding.inflate(inflador, contenedor, false)
        return enlace.root
    }

    override fun onViewCreated(vista: View, estadoGuardado: Bundle?) {
        super.onViewCreated(vista, estadoGuardado)

        gestorSesion = GestorSesion(requireContext())
        clienteUbicacion = LocationServices.getFusedLocationProviderClient(requireActivity())

        // Configurar menú hamburguesa (3 rayas)
        enlace.botonMenuHamburguesa.setOnClickListener {
            enlace.disenoDesplazableMapa.openDrawer(GravityCompat.START)
        }

        // Botón cerrar tarjeta de chofer
        enlace.botonCerrarTarjetaInfo.setOnClickListener {
            enlace.tarjetaInfoChofer.visibility = View.GONE
        }

        // Botón Mi Ubicación para centrar en la posición del administrador
        enlace.botonMiUbicacion.setOnClickListener {
            obtenerUbicacionActualYCentrar()
        }

        // Configurar RecyclerView lateral
        enlace.recyclerListaLateralTrabajadores.layoutManager = LinearLayoutManager(requireContext())
        adaptadorLateral = AdaptadorLateralTrabajadores(
            listaTrabajadores,
            mapaVehiculos,
            mapaUbicaciones
        ) { trabajador ->
            moverCamaraATrabajador(trabajador)
            enlace.disenoDesplazableMapa.closeDrawer(GravityCompat.START)
        }
        enlace.recyclerListaLateralTrabajadores.adapter = adaptadorLateral

        // Inicializar fragmento del mapa de Google
        val mapaFragmento = childFragmentManager.findFragmentById(R.id.mapa_google) as? SupportMapFragment
        mapaFragmento?.getMapAsync(this)
    }

    override fun onMapReady(mapa: GoogleMap) {
        mapaGoogle = mapa
        mapaGoogle?.mapType = GoogleMap.MAP_TYPE_NORMAL
        mapaGoogle?.uiSettings?.isZoomControlsEnabled = true
        mapaGoogle?.uiSettings?.isCompassEnabled = true
        mapaGoogle?.uiSettings?.isMyLocationButtonEnabled = false

        // Reposicionar el logo de Google hacia abajo junto a la barra de navegación
        mapaGoogle?.setPadding(0, 160, 0, 24)

        // Posición por defecto mientras obtiene ubicación GPS (Lima, Perú)
        val posicionInicial = LatLng(-12.046374, -77.042793)
        mapaGoogle?.moveCamera(CameraUpdateFactory.newLatLngZoom(posicionInicial, 12f))

        // Intentar centrar en la ubicación actual del administrador
        obtenerUbicacionActualYCentrar()

        // Click en marcador del mapa
        mapaGoogle?.setOnMarkerClickListener { marcador ->
            val usuarioId = marcador.tag as? String
            if (usuarioId != null) {
                val trabajador = listaTrabajadores.find { it.id == usuarioId }
                val ubicacion = mapaUbicaciones[usuarioId]
                if (trabajador != null) {
                    mostrarTarjetaInfoChofer(trabajador, ubicacion)
                }
            }
            marcador.showInfoWindow()
            true
        }

        iniciarBucleActualizacionEnVivo()
    }

    private fun obtenerUbicacionActualYCentrar() {
        val tieneFineLocation = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val tieneCoarseLocation = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!tieneFineLocation && !tieneCoarseLocation) {
            lanzadorPermisosUbicacion.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
            return
        }

        try {
            mapaGoogle?.isMyLocationEnabled = true
        } catch (_: SecurityException) {}

        clienteUbicacion.lastLocation.addOnSuccessListener { ubicacion ->
            if (ubicacion != null) {
                val miPosicion = LatLng(ubicacion.latitude, ubicacion.longitude)
                mapaGoogle?.animateCamera(CameraUpdateFactory.newLatLngZoom(miPosicion, 15f))
            } else {
                try {
                    clienteUbicacion.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                        .addOnSuccessListener { ubicacionFresca ->
                            if (ubicacionFresca != null) {
                                val miPosicion = LatLng(ubicacionFresca.latitude, ubicacionFresca.longitude)
                                mapaGoogle?.animateCamera(CameraUpdateFactory.newLatLngZoom(miPosicion, 15f))
                            }
                        }
                } catch (_: SecurityException) {}
            }
        }
    }

    private fun iniciarBucleActualizacionEnVivo() {
        trabajoActualizacionEnVivo?.cancel()
        trabajoActualizacionEnVivo = viewLifecycleOwner.lifecycleScope.launch {
            while (isActive) {
                cargarDatosMapa()
                val intervalo = gestorSesion.frecuenciaMapa * 1000L
                delay(if (intervalo < 3000L) 3000L else intervalo)
            }
        }
    }

    private suspend fun cargarDatosMapa() {
        try {
            // Cargar trabajadores, vehículos y ubicaciones en vivo desde Supabase
            val trabajadores = ClienteSupabase.obtenerTrabajadores()
            val vehiculos = ClienteSupabase.obtenerVehiculos().filter { it.id != null }.associateBy { it.id!! }
            val ubicaciones = ClienteSupabase.obtenerUbicacionesEnVivo().associateBy { it.usuarioId }

            this.listaTrabajadores = trabajadores
            this.mapaVehiculos = vehiculos
            this.mapaUbicaciones = ubicaciones

            // Actualizar adaptador de la lista rápida lateral
            adaptadorLateral?.actualizarDatos(trabajadores, vehiculos, ubicaciones)

            // Sincronizar marcadores sobre el mapa
            actualizarMarcadoresEnMapa()

        } catch (e: Exception) {
            // Manejo silencioso de pequeños cortes de red
        }
    }

    private fun actualizarMarcadoresEnMapa() {
        val mapa = mapaGoogle ?: return
        val soloMovimiento = gestorSesion.soloEnMovimiento
        val alertaBateria = gestorSesion.alertaBateriaBaja
        val tiempoActualMs = System.currentTimeMillis()

        // 1. ELIMINAR MARCADORES HUÉRFANOS (Choferes que finalizaron turno y cuya fila fue borrada de 'ubicacion_en_vivo')
        val idsMarcadoresEnMapa = mapaMarcadores.keys.toList()
        for (usuarioId in idsMarcadoresEnMapa) {
            val ubicacion = mapaUbicaciones[usuarioId]
            val trabajador = listaTrabajadores.find { it.id == usuarioId }

            // Si la fila fue borrada de 'ubicacion_en_vivo', o el trabajador está inactivo, o filtrado por movimiento
            if (ubicacion == null || trabajador == null || !trabajador.activo || (soloMovimiento && !ubicacion.enMovimiento)) {
                mapaMarcadores[usuarioId]?.remove()
                mapaMarcadores.remove(usuarioId)
            }
        }

        // 2. ACTUALIZAR O AGREGAR MARCADORES PARA CHOFERES CON TURNO ACTIVO
        for (trabajador in listaTrabajadores) {
            val ubicacion = mapaUbicaciones[trabajador.id] ?: continue
            if (!trabajador.activo) continue
            if (soloMovimiento && !ubicacion.enMovimiento) continue

            val latLng = LatLng(ubicacion.latitud, ubicacion.longitud)
            val vehiculo = trabajador.vehiculoAsignadoId?.let { mapaVehiculos[it] }
            val placa = vehiculo?.placa ?: "Sin vehículo"

            // Evaluar si la señal tiene más de 10 minutos de antigüedad (600,000 ms)
            val fechaReporteMs = parsearFechaToEpochMs(ubicacion.ultimaActualizacion)
            val diferenciaMinutos = if (fechaReporteMs > 0) (tiempoActualMs - fechaReporteMs) / (1000 * 60) else 0L
            val esSenalReciente = diferenciaMinutos < 10

            val estadoSenalTexto = if (esSenalReciente) {
                "Vel: ${ubicacion.velocidadKmh} km/h"
            } else {
                "⚠️ Sin señal reciente (Última vez: hace ${diferenciaMinutos} min)"
            }

            val colorIcono = when {
                !esSenalReciente -> R.color.gris_medio
                ubicacion.enMovimiento -> R.color.estado_activo
                else -> R.color.naranja_cautiva
            }

            val marcadorExistente = mapaMarcadores[trabajador.id]
            if (marcadorExistente != null) {
                marcadorExistente.position = latLng
                marcadorExistente.title = trabajador.nombreCompleto
                marcadorExistente.snippet = "Placa: $placa | $estadoSenalTexto"
                marcadorExistente.setIcon(crearIconoCamion(colorIcono))
            } else {
                val nuevoMarcador = mapa.addMarker(
                    MarkerOptions()
                        .position(latLng)
                        .title(trabajador.nombreCompleto)
                        .snippet("Placa: $placa | $estadoSenalTexto")
                        .icon(crearIconoCamion(colorIcono))
                )
                if (nuevoMarcador != null) {
                    nuevoMarcador.tag = trabajador.id
                    mapaMarcadores[trabajador.id] = nuevoMarcador
                }
            }

            // Alerta de Batería Baja (< 15%)
            val nivelBat = ubicacion.nivelBateria ?: 100
            if (alertaBateria && nivelBat < 15 && nivelBat > 0) {
                Toast.makeText(
                    requireContext(),
                    "⚠️ ¡Alerta! Batería baja (${nivelBat}%) en móvil de ${trabajador.nombreCompleto}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun parsearFechaToEpochMs(fechaTexto: String?): Long {
        if (fechaTexto.isNullOrEmpty()) return 0L
        return try {
            Instant.parse(fechaTexto).toEpochMilli()
        } catch (_: Exception) {
            try {
                val formato = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                formato.timeZone = TimeZone.getTimeZone("UTC")
                formato.parse(fechaTexto)?.time ?: 0L
            } catch (_: Exception) {
                0L
            }
        }
    }

    private fun moverCamaraATrabajador(trabajador: Perfil) {
        val ubicacion = mapaUbicaciones[trabajador.id]
        if (ubicacion != null) {
            val latLng = LatLng(ubicacion.latitud, ubicacion.longitud)
            mapaGoogle?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f))
            mapaMarcadores[trabajador.id]?.showInfoWindow()
            mostrarTarjetaInfoChofer(trabajador, ubicacion)
        } else {
            Toast.makeText(
                requireContext(),
                "${trabajador.nombreCompleto} no tiene turno activo ni ubicación en vivo",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun mostrarTarjetaInfoChofer(trabajador: Perfil, ubicacion: UbicacionEnVivo?) {
        val vehiculo = trabajador.vehiculoAsignadoId?.let { mapaVehiculos[it] }

        enlace.textoNombreChoferMapa.text = trabajador.nombreCompleto
        enlace.textoTelefonoChoferMapa.text = trabajador.telefono ?: "Sin teléfono"
        enlace.textoCamionChoferMapa.text = vehiculo?.placa ?: "Sin asignación"

        if (ubicacion != null) {
            enlace.textoVelocidadChoferMapa.text = "${ubicacion.velocidadKmh} km/h"
            enlace.textoBateriaChoferMapa.text = "${ubicacion.nivelBateria ?: 0}%"
            val fechaFormateada = formatearFechaHora(ubicacion.ultimaActualizacion)
            enlace.textoUltimaActualizacionMapa.text = "Último reporte: $fechaFormateada"
        } else {
            enlace.textoVelocidadChoferMapa.text = "0.0 km/h"
            enlace.textoBateriaChoferMapa.text = "--"
            enlace.textoUltimaActualizacionMapa.text = "Sin datos de reporte"
        }

        enlace.tarjetaInfoChofer.visibility = View.VISIBLE
    }

    private fun formatearFechaHora(fechaTexto: String?): String {
        if (fechaTexto.isNullOrEmpty()) return "Hace un momento"
        return try {
            val offsetDateTime = java.time.OffsetDateTime.parse(fechaTexto)
            val zonedDateTime = offsetDateTime.atZoneSameInstant(java.time.ZoneId.systemDefault())
            val formateador = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy -- HH:mm:ss", Locale.getDefault())
            zonedDateTime.format(formateador)
        } catch (_: Exception) {
            try {
                val instant = Instant.parse(fechaTexto)
                val zonedDateTime = instant.atZone(java.time.ZoneId.systemDefault())
                val formateador = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy -- HH:mm:ss", Locale.getDefault())
                zonedDateTime.format(formateador)
            } catch (_: Exception) {
                try {
                    val formatoEntrada = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                    formatoEntrada.timeZone = TimeZone.getTimeZone("UTC")
                    val fecha = formatoEntrada.parse(fechaTexto)
                    val formatoSalida = SimpleDateFormat("dd/MM/yyyy -- HH:mm:ss", Locale.getDefault())
                    formatoSalida.timeZone = TimeZone.getDefault()
                    if (fecha != null) formatoSalida.format(fecha) else fechaTexto
                } catch (_: Exception) {
                    fechaTexto
                }
            }
        }
    }

    private fun crearIconoCamion(colorResId: Int): BitmapDescriptor {
        val drawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_camion) ?: return BitmapDescriptorFactory.defaultMarker()
        drawable.setTint(ContextCompat.getColor(requireContext(), colorResId))
        drawable.setBounds(0, 0, 80, 80)
        val bitmap = Bitmap.createBitmap(80, 80, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        trabajoActualizacionEnVivo?.cancel()
        _enlace = null
    }
}