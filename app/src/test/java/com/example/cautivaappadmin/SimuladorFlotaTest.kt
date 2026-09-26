package com.example.cautivaappadmin

import com.example.cautivaappadmin.datos.ClienteSupabase
import com.example.cautivaappadmin.modelo.Perfil
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Test
import java.time.Instant
import kotlin.random.Random

/**
 * PRUEBA DE ESTRÉS Y SIMULADOR DE FLOTA EN TACNA, PERÚ (50 VEHÍCULOS CAUTIVA)
 */
class SimuladorFlotaTest {

    private val clienteSupabaseSimulador = createSupabaseClient(
        supabaseUrl = ClienteSupabase.URL_SUPABASE,
        supabaseKey = ClienteSupabase.CLAVE_ANON_SUPABASE
    ) {
        install(Postgrest)
    }

    @Test
    fun simular50ChoferesEnVivo() = runBlocking {
        println("=========================================================")
        println("🚀 INICIANDO SIMULADOR DE FLOTA DE 50 VEHÍCULOS EN TACNA")
        println("=========================================================")

        val cantidadObjetivo = 50
        val latitudBaseTacna = -18.013750  // Tacna, Perú
        val longitudBaseTacna = -70.252300

        class EstadoChoferSimulado(
            val usuarioId: String,
            val nombre: String,
            var latitud: Double,
            var longitud: Double,
            var rumbo: Double,
            var velocidadKmh: Double
        )

        // 1. OBTENER CHOFERES EXISTENTES EN LA BASE DE DATOS
        println("📋 Consultando choferes existentes en Supabase...")
        val choferesExistentes = try {
            clienteSupabaseSimulador.postgrest["perfiles"]
                .select()
                .decodeList<Perfil>()
                .filter { it.rol.uppercase() == "CONDUCTOR" }
        } catch (e: Exception) {
            println("Error consultando perfiles: ${e.localizedMessage}")
            emptyList()
        }

        println("  -> Encontrados ${choferesExistentes.size} choferes reales en la tabla 'perfiles'.")

        val choferesSimulados = mutableListOf<EstadoChoferSimulado>()

        // Agregar choferes existentes
        choferesExistentes.forEach { perfil ->
            val lat = latitudBaseTacna + Random.nextDouble(-0.015, 0.015)
            val lng = longitudBaseTacna + Random.nextDouble(-0.015, 0.015)
            choferesSimulados.add(
                EstadoChoferSimulado(
                    usuarioId = perfil.id,
                    nombre = perfil.nombreCompleto,
                    latitud = lat,
                    longitud = lng,
                    rumbo = Random.nextDouble(0.0, 360.0),
                    velocidadKmh = Random.nextDouble(20.0, 50.0)
                )
            )
        }

        // 2. CREAR CHOFERES ADICIONALES MEDIANTE LA FUNCIÓN RPC SI HACEN FALTA HASTA LLEGAR A 50
        val faltantes = cantidadObjetivo - choferesSimulados.size
        if (faltantes > 0) {
            println("🔨 Creando $faltantes choferes ficticios en Supabase usando RPC 'crear_usuario_sistema'...")
            val tareasCreacion = (1..faltantes).map { i ->
                async {
                    val num = String.format("%02d", i)
                    val email = "tacna.simulado.$num@cautiva.com"
                    val password = "Provisional123!"
                    val nombre = "Chofer Tacna"
                    val apellido = "Simulado $num"
                    val telefono = "+51900000$num"

                    try {
                        val parametrosRpc = buildJsonObject {
                            put("p_email", email)
                            put("p_password", password)
                            put("p_nombre", nombre)
                            put("p_apellido", apellido)
                            put("p_telefono", telefono)
                            put("p_rol", "CONDUCTOR")
                        }
                        val resultado = clienteSupabaseSimulador.postgrest.rpc("crear_usuario_sistema", parametrosRpc)
                        val usuarioId = resultado.data.replace("\"", "").trim()

                        val lat = latitudBaseTacna + Random.nextDouble(-0.015, 0.015)
                        val lng = longitudBaseTacna + Random.nextDouble(-0.015, 0.015)

                        EstadoChoferSimulado(
                            usuarioId = usuarioId,
                            nombre = "$nombre $apellido",
                            latitud = lat,
                            longitud = lng,
                            rumbo = Random.nextDouble(0.0, 360.0),
                            velocidadKmh = Random.nextDouble(20.0, 50.0)
                        )
                    } catch (e: Exception) {
                        println("Error creando usuario RPC $num: ${e.localizedMessage}")
                        null
                    }
                }
            }

            val nuevosChoferes = tareasCreacion.awaitAll().filterNotNull()
            choferesSimulados.addAll(nuevosChoferes)
        }

        println("✅ Flota lista con ${choferesSimulados.size} unidades activas en Tacna, Perú.")
        println("🔄 Transmitiendo telemetría continua en paralelo cada 5 segundos...\n")

        var iteracion = 1

        try {
            // 3. CICLO DE TRANSMISIÓN DE TELEMETRÍA EN PARALELO
            while (true) {
                val inicioMs = System.currentTimeMillis()
                val marcaTiempoIso = Instant.now().toString()

                val tareasEnvio = choferesSimulados.map { chofer ->
                    async {
                        val pasoKm = (chofer.velocidadKmh / 3600.0) * 5.0
                        val cambioLat = (pasoKm / 111.0) * Math.cos(Math.toRadians(chofer.rumbo))
                        val cambioLng = (pasoKm / (111.0 * Math.cos(Math.toRadians(chofer.latitud)))) * Math.sin(Math.toRadians(chofer.rumbo))

                        chofer.latitud += cambioLat
                        chofer.longitud += cambioLng
                        chofer.rumbo = (chofer.rumbo + Random.nextDouble(-15.0, 15.0) + 360.0) % 360.0
                        chofer.velocidadKmh = (chofer.velocidadKmh + Random.nextDouble(-3.0, 3.0)).coerceIn(15.0, 60.0)

                        val payload = buildJsonObject {
                            put("usuario_id", chofer.usuarioId)
                            put("latitud", chofer.latitud)
                            put("longitud", chofer.longitud)
                            put("velocidad_kmh", chofer.velocidadKmh)
                            put("direccion", chofer.rumbo)
                            put("nivel_bateria", Random.nextInt(50, 99))
                            put("en_movimiento", true)
                            put("ultima_actualizacion", marcaTiempoIso)
                        }

                        try {
                            clienteSupabaseSimulador.postgrest["ubicacion_en_vivo"].upsert(payload)
                            true
                        } catch (e: Exception) {
                            println("❌ Error en ${chofer.nombre}: ${e.localizedMessage}")
                            false
                        }
                    }
                }

                val resultados = tareasEnvio.awaitAll()
                val exitosos = resultados.count { it }
                val duracionMs = System.currentTimeMillis() - inicioMs

                println("⏱️ [Iteración #$iteracion] Transmitidas $exitosos/${choferesSimulados.size} telemetrías en $duracionMs ms")
                iteracion++

                val tiempoEspera = (5000L - duracionMs).coerceAtLeast(1000L)
                delay(tiempoEspera)
            }
        } finally {
            println("\n🧹 Limpiando registros de telemetría en Supabase...")
            limpiarTelemetriaInterna()
        }
    }

    /**
     * Prueba individual para limpiar inmediatamente las ubicaciones en vivo y refrescar el mapa en blanco.
     * Ejecútala en 1 clic ▶️ para dejar el mapa limpio.
     */
    @Test
    fun limpiarTelemetriaYSimulados() = runBlocking {
        println("🧹 Limpiando la tabla 'ubicacion_en_vivo' en Supabase...")
        limpiarTelemetriaInterna()
        println("✅ Mapa limpiado con éxito en Supabase.")
    }

    private suspend fun limpiarTelemetriaInterna() {
        try {
            clienteSupabaseSimulador.postgrest["ubicacion_en_vivo"].delete {
                filter {
                    neq("usuario_id", "00000000-0000-0000-0000-000000000000")
                }
            }
            println("✅ Telemetría en vivo borrada correctamente.")
        } catch (e: Exception) {
            println("Error durante la limpieza: ${e.localizedMessage}")
        }
    }
}