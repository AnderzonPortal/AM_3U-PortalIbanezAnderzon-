package com.example.s14_techpoint

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.OnSuccessListener
import kotlin.math.*

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var searchView: SearchView
    private lateinit var storeInfoTextView: TextView
    private var currentUserLocation: LatLng? = null

    // Historial de búsquedas
    private val searchHistory = mutableListOf<String>()
    private val maxHistorySize = 10

    private val LOCATION_PERMISSION_REQUEST_CODE = 1000

    // Tiendas reales de equipos de cómputo en Chimbote
    private val computeStores = listOf(
        // Centro de Chimbote
        ComputeStore("Computer House Chimbote", LatLng(-9.0856, -78.5969),
            "Especializada en equipos de cómputo, laptops y accesorios gaming",
            listOf("laptop", "computadora", "monitor", "teclado", "mouse", "impresora", "scanner", "proyector", "disco duro", "memoria ram", "tarjeta de video", "placa madre", "procesador", "fuente de poder", "case gamer", "webcam", "auriculares", "parlantes", "cable hdmi", "adaptadores")),

        ComputeStore("Compuplaza Chimbote", LatLng(-9.0834, -78.5945),
            "Venta de laptops, PCs y dispositivos móviles con servicio técnico",
            listOf("laptop gamer", "pc escritorio", "all in one", "tablet", "smartphone", "smartwatch", "teclado mecánico", "mouse gamer", "pad mouse", "impresora multifuncional", "impresora láser", "scanner profesional", "webcam hd", "micrófono", "auriculares gaming", "parlantes bluetooth", "cargadores", "cables usb", "hub usb", "disco duro externo")),

        ComputeStore("Mi PC Lista Chimbote", LatLng(-9.0798, -78.5934),
            "Computadoras armadas, componentes y periféricos de primeras marcas",
            listOf("computadora gamer", "laptop hp", "laptop dell", "laptop lenovo", "monitor led", "monitor curvo", "teclado inalámbrico", "mouse inalámbrico", "impresora epson", "impresora canon", "multifuncional", "memoria usb", "tarjeta sd", "disco ssd", "memoria ram ddr4", "tarjeta gráfica", "motherboard", "cpu intel", "cpu amd", "cooler")),

        // Nuevo Chimbote
        ComputeStore("TechnoWorld Nuevo Chimbote", LatLng(-9.1085, -78.5147),
            "Tecnología avanzada, equipos gaming y dispositivos inteligentes",
            listOf("laptop acer", "laptop asus", "pc gamer", "monitor samsung", "monitor lg", "teclado gaming", "mouse gaming", "headset gamer", "silla gamer", "escritorio gamer", "impresora 3d", "filamento 3d", "drone", "cámara digital", "cámara seguridad", "router wifi", "repetidor wifi", "switch ethernet", "cable de red", "estabilizador")),

        ComputeStore("Infotech Solutions", LatLng(-9.1034, -78.5234),
            "Soluciones empresariales, servidores y equipos de red profesionales",
            listOf("servidor", "nas", "ups", "rack", "switch administrable", "firewall", "access point", "antena wifi", "cable fibra óptica", "patch panel", "gabinete rack", "ventilador rack", "pdu", "kvm switch", "monitor industrial", "mini pc", "raspberry pi", "arduino", "sensor", "actuador")),

        ComputeStore("Digital Store Chimbote", LatLng(-9.0945, -78.5198),
            "Dispositivos digitales, consolas de videojuegos y accesorios multimedia",
            listOf("chromebook", "macbook", "ipad", "surface", "kindle", "tv box", "chromecast", "fire stick", "nintendo switch", "playstation", "xbox", "joystick", "control gamer", "realidad virtual", "cámara deportiva", "action cam", "gimbal", "trípode", "luz led", "micrófono profesional")),

        // Zona comercial
        ComputeStore("MegaTech Chimbote", LatLng(-9.0723, -78.5812),
            "Equipos profesionales, workstations y soluciones gráficas especializadas",
            listOf("workstation", "laptop workstation", "monitor 4k", "monitor ultrawide", "teclado ergonómico", "mouse ergonómico", "trackball", "tableta gráfica", "stylus", "escáner 3d", "impresora gran formato", "plotter", "cortadora laser", "grabadora blu-ray", "lector código barras", "impresora térmica", "balanza electrónica", "pos", "cajón monedero", "terminal punto venta")),

        ComputeStore("ElectroTech Ancash", LatLng(-9.0667, -78.5756),
            "Sistemas de seguridad, cámaras IP y soluciones de control de acceso",
            listOf("sistema pos", "cámara ip", "dvr", "nvr", "cámara domo", "cámara bullet", "intercomunicador", "control acceso", "biométrico", "tarjeta rfid", "lector huella", "alarma", "sensor movimiento", "sensor puerta", "sirena", "panel alarma", "batería respaldo", "transformador", "inversor", "regulador voltaje")),

        // Zona periférica
        ComputeStore("CompuCenter Garatea", LatLng(-9.0534, -78.6123),
            "Repuestos, refacciones y componentes para reparación de equipos",
            listOf("refacciones pc", "ventilador cpu", "pasta térmica", "cable sata", "cable ide", "fuente atx", "batería cmos", "slot ram", "puerto usb", "conector hdmi", "flex laptop", "teclado laptop", "pantalla laptop", "batería laptop", "cargador laptop", "bisagra laptop", "webcam laptop", "wifi laptop", "bluetooth", "tarjeta red")),

        ComputeStore("Servicios Informáticos SAC", LatLng(-9.0612, -78.6045),
            "Servicios técnicos, mantenimiento y desarrollo de software personalizado",
            listOf("mantenimiento pc", "instalación software", "recuperación datos", "formateo", "instalación windows", "instalación linux", "configuración red", "soporte técnico", "capacitación", "consultoría ti", "desarrollo software", "página web", "hosting", "dominio", "certificado ssl", "backup", "antivirus", "office", "autocad", "photoshop"))
    )

    data class ComputeStore(
        val name: String,
        val location: LatLng,
        val description: String,
        val products: List<String>
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializar el cliente de ubicación
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Inicializar TextView para información de tienda
        storeInfoTextView = findViewById(R.id.store_info)

        // Configurar el mapa
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Cargar historial de búsquedas
        loadSearchHistory()

        // Inicializar la barra de búsqueda
        setupSearchView()

        // Verificar permisos de ubicación
        checkLocationPermissions()
    }

    private fun setupSearchView() {
        searchView = findViewById(R.id.search_view)

        // Configurar el color del texto del SearchView
        val searchAutoComplete = searchView.findViewById<androidx.appcompat.widget.SearchView.SearchAutoComplete>(androidx.appcompat.R.id.search_src_text)
        searchAutoComplete?.setTextColor(Color.BLACK)
        searchAutoComplete?.setHintTextColor(Color.GRAY)

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (query != null && currentUserLocation != null) {
                    addToSearchHistory(query)
                    searchComputeDevices(query)
                } else {
                    Toast.makeText(this@MainActivity, "Esperando ubicación actual...", Toast.LENGTH_SHORT).show()
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })

        searchView.setOnSuggestionListener(object : SearchView.OnSuggestionListener {
            override fun onSuggestionSelect(position: Int): Boolean {
                return false
            }

            override fun onSuggestionClick(position: Int): Boolean {
                if (position < searchHistory.size) {
                    val suggestion = searchHistory[position]
                    searchView.setQuery(suggestion, true)
                    return true
                }
                return false
            }
        })
    }

    private fun loadSearchHistory() {
        val sharedPref = getSharedPreferences("search_history", Context.MODE_PRIVATE)
        val historyString = sharedPref.getString("history", "")
        if (!historyString.isNullOrEmpty()) {
            searchHistory.addAll(historyString.split(",").filter { it.isNotBlank() })
        }
    }

    private fun saveSearchHistory() {
        val sharedPref = getSharedPreferences("search_history", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("history", searchHistory.joinToString(","))
            apply()
        }
    }

    private fun addToSearchHistory(query: String) {
        val normalizedQuery = query.trim().lowercase()

        // Remover si ya existe
        searchHistory.removeAll { it.lowercase() == normalizedQuery }

        // Añadir al principio
        searchHistory.add(0, query.trim())

        // Mantener tamaño máximo
        if (searchHistory.size > maxHistorySize) {
            searchHistory.removeAt(searchHistory.size - 1)
        }

        // Guardar historial
        saveSearchHistory()
    }

    private fun checkLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permisos concedidos, reinicializar mapa si es necesario
                if (::mMap.isInitialized) {
                    setupUserLocation()
                }
            } else {
                Toast.makeText(this, "Se necesitan permisos de ubicación para usar la app", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isZoomControlsEnabled = true

        // Configurar ubicación del usuario
        setupUserLocation()
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun setupUserLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {

            fusedLocationClient.lastLocation.addOnSuccessListener(this, OnSuccessListener<Location> { location ->
                if (location != null) {
                    currentUserLocation = LatLng(location.latitude, location.longitude)

                    // Centrar mapa en ubicación actual
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentUserLocation!!, 15f))

                    // Mostrar marcador de ubicación actual con ícono diferente
                    mMap.addMarker(
                        MarkerOptions()
                            .position(currentUserLocation!!)
                            .title("Tu Ubicación Actual")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                    )

                    // Mostrar información de ubicación actual en Chimbote
                    storeInfoTextView.text = """
                        Ubicación actual detectada en Chimbote
                        Coordenadas: ${String.format("%.4f", location.latitude)}, ${String.format("%.4f", location.longitude)}
                        
                        Mostrando ${computeStores.size} tiendas de cómputo disponibles
                        Busca cualquier dispositivo: laptop, monitor, impresora, etc.
                    """.trimIndent()

                    // Mostrar todas las tiendas disponibles
                    showAllStores()

                } else {
                    // Si no se puede obtener ubicación, usar ubicación predeterminada en Chimbote
                    currentUserLocation = LatLng(-9.0856, -78.5969) // Centro de Chimbote
                    val chimboteCenter = LatLng(-9.0856, -78.5969)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(chimboteCenter, 13f))

                    mMap.addMarker(
                        MarkerOptions()
                            .position(currentUserLocation!!)
                            .title("Ubicación en Chimbote")
                            .snippet("Centro de Chimbote - Ubicación predeterminada")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN))
                    )

                    storeInfoTextView.text = """
                        Mostrando tiendas de cómputo en Chimbote
                        Ubicación predeterminada: Centro de Chimbote
                        
                        ${computeStores.size} establecimientos disponibles
                        Busca tu dispositivo ideal
                    """.trimIndent()

                    showAllStores()
                }
            })
        }
    }

    private fun showAllStores() {
        // Mostrar todas las tiendas de Chimbote en el mapa
        computeStores.forEach { store ->
            val storeLocation = when(store.name) {
                "Computer House Chimbote" -> "Centro Comercial"
                "Compuplaza Chimbote" -> "Av. José Pardo"
                "Mi PC Lista Chimbote" -> "Jr. Tumbes"
                "TechnoWorld Nuevo Chimbote" -> "Av. Pacífico"
                "Infotech Solutions" -> "Urb. Buenos Aires"
                "Digital Store Chimbote" -> "Av. País de los Incas"
                "MegaTech Chimbote" -> "Av. José Gálvez"
                "ElectroTech Ancash" -> "Jr. Leoncio Prado"
                "CompuCenter Garatea" -> "Urb. Garatea"
                else -> "Chimbote"
            }

            mMap.addMarker(
                MarkerOptions()
                    .position(store.location)
                    .title(store.name)
                    .snippet("$storeLocation • ${store.products.size} productos")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
            )
        }
    }

    private fun searchComputeDevices(query: String) {
        val searchQuery = query.lowercase().trim()

        // Limpiar mapa de marcadores anteriores de búsqueda
        mMap.clear()

        // Volver a mostrar la ubicación actual
        currentUserLocation?.let { userLocation ->
            mMap.addMarker(
                MarkerOptions()
                    .position(userLocation)
                    .title("Tu Ubicación Actual")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
            )
        }

        // Buscar tiendas que tengan el producto
        val storesWithProduct = computeStores.filter { store ->
            store.products.any { product -> product.lowercase().contains(searchQuery) }
        }

        if (storesWithProduct.isNotEmpty() && currentUserLocation != null) {
            // Encontrar la tienda más cercana
            val nearestStore = storesWithProduct.minByOrNull { store ->
                calculateDistance(currentUserLocation!!, store.location)
            }

            if (nearestStore != null) {
                val distance = calculateDistance(currentUserLocation!!, nearestStore.location)

                // Mostrar información detallada de la tienda más cercana
                storeInfoTextView.text = """
                    Producto encontrado: "$query"
                    
                    TIENDA MAS CERCANA:
                    ${nearestStore.name}
                    Distancia: ${String.format("%.2f", distance)} km
                    
                    ${nearestStore.description}
                    
                    Ruta marcada en azul en el mapa
                """.trimIndent()

                // Agregar marcador de la tienda más cercana
                mMap.addMarker(
                    MarkerOptions()
                        .position(nearestStore.location)
                        .title("${nearestStore.name} - MAS CERCANA")
                        .snippet("Distancia: ${String.format("%.2f", distance)} km\nTiene: $query")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                )

                // Mostrar otras tiendas que también tienen el producto
                storesWithProduct.filter { it != nearestStore }.forEach { store ->
                    val dist = calculateDistance(currentUserLocation!!, store.location)
                    mMap.addMarker(
                        MarkerOptions()
                            .position(store.location)
                            .title(store.name)
                            .snippet("Distancia: ${String.format("%.2f", dist)} km\nTiene: $query")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
                    )
                }

                // Dibujar ruta a la tienda más cercana
                mMap.addPolyline(
                    PolylineOptions()
                        .add(currentUserLocation!!, nearestStore.location)
                        .color(android.graphics.Color.BLUE)
                        .width(5f)
                )

                // Centrar cámara para mostrar ambos puntos
                val builder = com.google.android.gms.maps.model.LatLngBounds.Builder()
                builder.include(currentUserLocation!!)
                builder.include(nearestStore.location)
                val bounds = builder.build()
                mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))

            }
        } else {
            storeInfoTextView.text = """
                No se encontró: "$query"
                
                Intenta buscar: laptop, monitor, teclado, mouse, impresora
                
                ${computeStores.size} tiendas disponibles en Chimbote
                Productos disponibles: computadoras, periféricos, accesorios
            """.trimIndent()
            Toast.makeText(this, "Producto '$query' no encontrado. Intenta: laptop, monitor, etc.", Toast.LENGTH_LONG).show()

            // Mostrar todas las tiendas disponibles
            showAllStores()
        }
    }

    // Calcular distancia entre dos puntos usando fórmula de Haversine
    private fun calculateDistance(point1: LatLng, point2: LatLng): Double {
        val earthRadius = 6371.0 // Radio de la Tierra en km

        val lat1Rad = Math.toRadians(point1.latitude)
        val lat2Rad = Math.toRadians(point2.latitude)
        val deltaLat = Math.toRadians(point2.latitude - point1.latitude)
        val deltaLng = Math.toRadians(point2.longitude - point1.longitude)

        val a = sin(deltaLat / 2).pow(2) +
                cos(lat1Rad) * cos(lat2Rad) * sin(deltaLng / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return earthRadius * c
    }
}