package com.example.cautivaappadmin

import com.example.cautivaappadmin.datos.ClienteSupabase
import com.example.cautivaappadmin.modelo.PerfilRegistroDto
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant
import kotlin.random.Random

/**
 * SIMULADOR DE FLOTA DE 50 VEHÍCULOS (PRUEBA DE ESTRÉS / STRESS TEST)
 *
 * Este script simula 50 choferes transmitiendo telemetría GPS en tiempo real
 * a la tabla 'public.ubicacion_en_vivo' de Supabase cada 5 segundos.
 *
 * Instrucciones de uso:
 * 1. Abrir la app CautivaAppAdmin en el celular o emulador.
 * 2. Ejecutar esta clase haciendo clic en el botón de reproducción verde ▶️ al lado de 'fun main()'.
 * 3. Observar la actualización y rotación fluida de 50 camiones mixer en la pantalla de la app.
 */
fun main() = runBlocking {
    println("=========================================================")
    println("🚀 INICIANDO SIMULADOR DE FLOTA DE 50 VEHÍCULOS CAUTIVA")
    println("=========================================================")

    val cantidadChoferes = 50
    val latitudBase = -12.046374  // Ubicación inicial base (Lima / Planta Cautiva)
    val longitudBase = -77.042793

    // Estructura interna para mantener el estado de movimiento de cada unidad
    class EstadoChoferSimulado(
        val usuarioId: String,
        val nombre: String,
        var latitud: Double,
        var longitud: Double,
        var rumbo: Double,
        var velocidadKmh: Double,
        val placaVehiculo: String
    )

    // 1. GENERAR O REGISTRAR 50 CHOFERES FICTICIOS
    println("📋 Registrando y verificando perfiles de 50 choferes simulados...")
    val choferes = mutableListOf<EstadoChoferSimulado>()

    val tareasRegistroPerfiles = (1..cantidadChoferes).map { i ->
        async {
            val idFormateado = String.format("%02d", i)
            val usuarioId = "ffffffff-ffff-ffff-ffff-${String.format("%012d", i)}"
            val email = "simulado.$idFormateado@cautiva.com"
            val nombre = "Chofer Simulado $idFormateado"
            val placa = "MIX-$idFormateado"

            // Registrar perfil en la tabla 'perfiles' de Supabase
            val metadatos = buildJsonObject {
                put("clave_provisional", "123456")
            }
            val perfilDto = PerfilRegistroDto(
                id = usuarioId,
                correo = email,
                nombre = "Chofer",
                apellido = "Simulado $idFormateado",
                telefono = "+51 900000$idFormateado",
                rol = "CONDUCTOR",
                activo = true,
                metadatos = metadatos
            )

            try {
                ClienteSupabase.cliente.postgrest["perfiles"].upsert(perfilDto)
            } catch (e: Exception) {
                // Silencioso si ya existe
            }

            // Dispersar coordenadas iniciales en un radio ~2 km alrededor del centro
            val latInicial = latitudBase + (Random.nextDouble(-0.02, 0.02))
            val lngInicial = longitudBase + (Random.nextDouble(-0.02, 0.02))
            val rumboInicial = Random.nextDouble(0.0, 360.0)

            EstadoChoferSimulado(
                usuarioId = usuarioId,
                nombre = nombre,
                latitud = latInicial,
                longitud = lngInicial,
                rumbo = rumboInicial,
                velocidadKmh = Random.nextDouble(25.0, 55.0),
                placaVehiculo = placa
            )
        }
    }

    choferes.addAll(tareasRegistroPerfiles.awaitAll())
    println("✅ 50 choferes inicializados y vinculados a la base de datos de Supabase.")
    println("🔄 Iniciando ciclo de transmisión de telemetría en paralelo cada 5 segundos...")
    println("=========================================================\n")

    var numeroIteracion = 1

    while (true) {
        val inicioMs = System.currentTimeMillis()
        val marcaTiempoIso = Instant.now().toString()

        // 2. TRANSMISIÓN EN PARALELO DE 50 REGISTROS A SUPABASE
        val tareasEnvioTelemetry = choferes.map { chofer ->
            async {
                // Simular movimiento realista: avanzar la coordenada en dirección a su rumbo
                val pasoKm = (chofer.velocidadKmh / 3600.0) * 5.0 // Distancia recorrida en 5 segundos
                val cambioLat = (pasoKm / 111.0) * Math.cos(Math.toRadians(chofer.rumbo))
                val cambioLng = (pasoKm / (111.0 * Math.cos(Math.toRadians(chofer.latitud)))) * Math.sin(Math.toRadians(chofer.rumbo))

                chofer.latitud += cambioLat
                chofer.longitud += cambioLng

                // Variar levemente el rumbo (giro en esquinas de calles) y velocidad
                chofer.rumbo = (chofer.rumbo + Random.nextDouble(-15.0, 15.0) + 360.0) % 360.0
                chofer.velocidadKmh = (chofer.velocidadKmh + Random.nextDouble(-3.0, 3.0)).coerceIn(15.0, 65.0)

                // JSON de telemetría para la tabla 'ubicacion_en_vivo'
                val payloadUbicacion = buildJsonObject {
                    put("usuario_id", chofer.usuarioId)
                    put("latitud", chofer.latitud)
                    put("longitud", chofer.longitud)
                    put("velocidad_kmh", chofer.velocidadKmh)
                    put("direccion", chofer.rumbo)
                    put("nivel_bateria", Random.nextInt(40, 99))
                    put("en_movimiento", true)
                    put("ultima_actualizacion", marcaTiempoIso)
                }

                try {
                    ClienteSupabase.cliente.postgrest["ubicacion_en_vivo"].upsert(payloadUbicacion)
                    true
                } catch (e: Exception) {
                    println("❌ Error enviando telemetría de ${chofer.nombre}: ${e.localizedMessage}")
                    false
                }
            }
        }

        val resultados = tareasEnvioTelemetry.awaitAll()
        val exitosos = resultados.count { it }
        val duracionMs = System.currentTimeMillis() - inicioMs

        println("⏱️ [Iteración #$numeroIteracion] Transmitidas $exitosos/$cantidadChoferes ubicaciones en $duracionMs ms ($marcaTiempoIso)")

        numeroIteracion++

        // Pausa exacta de 5 segundos entre cada ráfaga
        val tiempoEspera = (5000L - duracionMs).coerceAtLeast(1000L)
        delay(tiempoEspera)
    }
}