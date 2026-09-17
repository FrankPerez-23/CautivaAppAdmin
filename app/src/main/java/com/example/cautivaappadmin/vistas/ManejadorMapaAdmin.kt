package com.example.cautivaappadmin.vistas

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.animation.LinearInterpolator
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.cautivaappadmin.R
import com.example.cautivaappadmin.modelo.Perfil
import com.example.cautivaappadmin.modelo.UbicacionEnVivo
import com.example.cautivaappadmin.modelo.Vehiculo
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import java.time.Instant
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class ManejadorMapaAdmin(
    private val contexto: Context,
    private val mapaGoogle: GoogleMap
) {

    // Colección de marcadores activos en memoria
    val mapaMarcadores = mutableMapOf<String, Marker>()

    // Colección de animadores activos para interpolación fluida (Efecto InDrive / Uber)
    private val mapaAnimadores = mutableMapOf<String, ValueAnimator>()

    /**
     * Sincroniza los marcadores en el mapa con los datos actuales de Supabase.
     * Realiza limpieza de choferes desconectados y animación suave de coordenadas y rumbo.
     */
    fun sincronizarChoferes(
        listaTrabajadores: List<Perfil>,
        mapaVehiculos: Map<String, Vehiculo>,
        mapaUbicaciones: Map<String, UbicacionEnVivo>,
        soloEnMovimiento: Boolean,
        alertaBateriaBaja: Boolean
    ) {
        val tiempoActualMs = System.currentTimeMillis()

        // 1. LIMPIEZA DE MARCADORES HUÉRFANOS O INACTIVOS
        val idsMarcadoresEnMapa = mapaMarcadores.keys.toList()
        for (usuarioId in idsMarcadoresEnMapa) {
            val ubicacion = mapaUbicaciones[usuarioId]
            val trabajador = listaTrabajadores.find { it.id == usuarioId }

            val debeEliminar = ubicacion == null || 
                               trabajador == null || 
                               !trabajador.activo || 
                               (soloEnMovimiento && !ubicacion.enMovimiento)

            if (debeEliminar) {
                mapaAnimadores[usuarioId]?.cancel()
                mapaAnimadores.remove(usuarioId)
                mapaMarcadores[usuarioId]?.remove()
                mapaMarcadores.remove(usuarioId)
            }
        }

        // 2. ACTUALIZACIÓN O CREACIÓN DE MARCADORES CON ANIMACIÓN E INTERPOLACIÓN
        for (trabajador in listaTrabajadores) {
            val ubicacion = mapaUbicaciones[trabajador.id] ?: continue
            if (!trabajador.activo) continue
            if (soloEnMovimiento && !ubicacion.enMovimiento) continue

            val destinoLatLng = LatLng(ubicacion.latitud, ubicacion.longitud)
            val vehiculo = trabajador.vehiculoAsignadoId?.let { mapaVehiculos[it] }
            val placa = vehiculo?.placa ?: "Sin vehículo"

            // Validar antigüedad de señal (Umbral de 10 minutos = 600,000 ms)
            val fechaReporteMs = parsearFechaToEpochMs(ubicacion.ultimaActualizacion)
            val diferenciaMinutos = if (fechaReporteMs > 0) (tiempoActualMs - fechaReporteMs) / (1000 * 60) else 0L
            val esSenalReciente = diferenciaMinutos < 10

            val estadoSenalTexto = if (esSenalReciente) {
                "Vel: ${ubicacion.velocidadKmh} km/h"
            } else {
                "⚠️ Sin señal reciente (Última vez: hace ${diferenciaMinutos} min)"
            }

            val colorIconoResId = when {
                !esSenalReciente -> R.color.gris_medio
                ubicacion.enMovimiento -> R.color.estado_activo
                else -> R.color.naranja_cautiva
            }

            val rumboFinal = ubicacion.direccion?.toFloat() ?: 0f
            val marcadorExistente = mapaMarcadores[trabajador.id]

            if (marcadorExistente != null) {
                // Actualizar título y snippet (estado / velocidad / señal)
                marcadorExistente.title = trabajador.nombreCompleto
                marcadorExistente.snippet = "Placa: $placa | $estadoSenalTexto"
                marcadorExistente.setIcon(crearIconoCamion(colorIconoResId))

                // Animar interpolación suave de posición y rotación (Efecto InDrive)
                animarMovimientoMarcador(trabajador.id, marcadorExistente, destinoLatLng, rumboFinal)
            } else {
                // Instanciar nuevo marcador personalizado para chofer nuevo
                val opcionesMarcador = MarkerOptions()
                    .position(destinoLatLng)
                    .title(trabajador.nombreCompleto)
                    .snippet("Placa: $placa | $estadoSenalTexto")
                    .icon(crearIconoCamion(colorIconoResId))
                    .rotation(rumboFinal)
                    .flat(true) // Hace que el icono rote plano sobre el mapa simulando dirección vehicular

                val nuevoMarcador = mapaGoogle.addMarker(opcionesMarcador)
                if (nuevoMarcador != null) {
                    nuevoMarcador.tag = trabajador.id
                    mapaMarcadores[trabajador.id] = nuevoMarcador
                }
            }

            // Alerta opcional de batería baja (< 15%)
            val nivelBateria = ubicacion.nivelBateria ?: 100
            if (alertaBateriaBaja && nivelBateria < 15 && nivelBateria > 0) {
                Toast.makeText(
                    contexto,
                    "⚠️ ¡Alerta! Batería baja (${nivelBateria}%) en unidad de ${trabajador.nombreCompleto}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /**
     * Anima la transición de posición y rotación de un marcador de forma suave e interpolada (Efecto InDrive / Uber).
     */
    private fun animarMovimientoMarcador(
        usuarioId: String,
        marcador: Marker,
        posicionDestino: LatLng,
        rumboDestino: Float
    ) {
        // Cancelar animación previa en curso para evitar conflictos de interpolación
        mapaAnimadores[usuarioId]?.cancel()

        val posicionInicio = marcador.position
        val rotacionInicio = marcador.rotation

        // Calcular la diferencia de rumbo más corta para evitar giros largos de 360° innecesarios
        val diferenciaRotacion = (rumboDestino - rotacionInicio + 540) % 360 - 180

        val animador = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 3500L // 3500 ms sincronizados con la frecuencia de sondeo para desplazamiento continuo
            interpolator = LinearInterpolator()
            addUpdateListener { animator ->
                val fraccion = animator.animatedFraction

                // Interpolar latitud y longitud linealmente
                val latitudInterpolada = posicionInicio.latitude + (posicionDestino.latitude - posicionInicio.latitude) * fraccion
                val longitudInterpolada = posicionInicio.longitude + (posicionDestino.longitude - posicionInicio.longitude) * fraccion
                marcador.position = LatLng(latitudInterpolada, longitudInterpolada)

                // Interpolar rotación (rumbo/bearing) suavemente
                val rotacionInterpolada = rotacionInicio + diferenciaRotacion * fraccion
                marcador.rotation = rotacionInterpolada
            }
        }

        mapaAnimadores[usuarioId] = animador
        animador.start()
    }

    /**
     * Convierte una cadena de fecha ISO o UTC a milisegundos Epoch.
     */
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

    /**
     * Genera un BitmapDescriptor con el icono de camión tintado según el estado de la unidad.
     */
    private fun crearIconoCamion(colorResId: Int): BitmapDescriptor {
        val drawable = ContextCompat.getDrawable(contexto, R.drawable.ic_camion) ?: return BitmapDescriptorFactory.defaultMarker()
        drawable.setTint(ContextCompat.getColor(contexto, colorResId))
        drawable.setBounds(0, 0, 80, 80)
        val bitmap = Bitmap.createBitmap(80, 80, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    /**
     * Libera recursos y cancela animadores activos al destruir la vista.
     */
    fun limpiarRecursos() {
        for (animador in mapaAnimadores.values) {
            animador.cancel()
        }
        mapaAnimadores.clear()
        for (marcador in mapaMarcadores.values) {
            marcador.remove()
        }
        mapaMarcadores.clear()
    }
}
