package com.example.cautivaappadmin

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.example.cautivaappadmin.databinding.ActivityMainBinding
import com.example.cautivaappadmin.vistas.FragmentoConfiguracion
import com.example.cautivaappadmin.vistas.FragmentoMapa
import com.example.cautivaappadmin.vistas.FragmentoRegistro
import com.example.cautivaappadmin.vistas.FragmentoTrabajadores
import com.example.cautivaappadmin.vistas.FragmentoVehiculos

class MainActivity : AppCompatActivity() {

    private lateinit var enlace: ActivityMainBinding

    private val fragmentoMapa = FragmentoMapa()
    private val fragmentoTrabajadores = FragmentoTrabajadores()
    private val fragmentoVehiculos = FragmentoVehiculos()
    private val fragmentoRegistro = FragmentoRegistro()
    private val fragmentoConfiguracion = FragmentoConfiguracion()

    private var fragmentoActual: Fragment = fragmentoMapa

    override fun onCreate(estadoGuardado: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(estadoGuardado)
        enlace = ActivityMainBinding.inflate(layoutInflater)
        setContentView(enlace.root)

        // Configurar fragmentos iniciales
        if (estadoGuardado == null) {
            supportFragmentManager.beginTransaction()
                .add(R.id.contenedor_fragmentos, fragmentoConfiguracion, "5").hide(fragmentoConfiguracion)
                .add(R.id.contenedor_fragmentos, fragmentoRegistro, "4").hide(fragmentoRegistro)
                .add(R.id.contenedor_fragmentos, fragmentoVehiculos, "3").hide(fragmentoVehiculos)
                .add(R.id.contenedor_fragmentos, fragmentoTrabajadores, "2").hide(fragmentoTrabajadores)
                .add(R.id.contenedor_fragmentos, fragmentoMapa, "1")
                .commit()
        }

        // Listener para la barra de navegación inferior
        enlace.barraNavegacionInferior.setOnItemSelectedListener { elemento ->
            when (elemento.itemId) {
                R.id.opcion_mapa -> {
                    cambiarFragmento(fragmentoMapa)
                    true
                }
                R.id.opcion_trabajadores -> {
                    cambiarFragmento(fragmentoTrabajadores)
                    true
                }
                R.id.opcion_vehiculos -> {
                    cambiarFragmento(fragmentoVehiculos)
                    true
                }
                R.id.opcion_registro -> {
                    cambiarFragmento(fragmentoRegistro)
                    true
                }
                R.id.opcion_configuracion -> {
                    cambiarFragmento(fragmentoConfiguracion)
                    true
                }
                else -> false
            }
        }
    }

    private fun cambiarFragmento(nuevoFragmento: Fragment) {
        if (fragmentoActual != nuevoFragmento) {
            supportFragmentManager.beginTransaction()
                .hide(fragmentoActual)
                .show(nuevoFragmento)
                .commit()
            fragmentoActual = nuevoFragmento
        }
    }
}