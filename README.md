# Desarrollo de Aplicaciones Móviles - Android

Repositorio que contiene los proyectos desarrollados durante el curso de programación Android, organizados por semanas de trabajo.

---

# 📅 Semana 13 - S13_CalculadoraBasica

Una aplicación de calculadora básica desarrollada en Android que permite realizar operaciones matemáticas fundamentales con una interfaz de usuario intuitiva y moderna.

## 📱 Descripción del Proyecto

Esta calculadora básica está diseñada para dispositivos Android y proporciona las funcionalidades esenciales de cálculo que los usuarios necesitan en su día a día. La aplicación implementa el patrón arquitectónico MVVM (Model-View-ViewModel) para garantizar una separación clara de responsabilidades y un código mantenible.

## 🎯 Finalidad

El objetivo principal de este proyecto es:

- Crear una calculadora funcional y fácil de usar para operaciones básicas
- Implementar buenas prácticas de desarrollo Android usando ViewModel  
- Proporcionar una interfaz de usuario limpia y responsive
- Demostrar el uso correcto del patrón MVVM en una aplicación práctica

## ⚡ Funcionalidades

### Operaciones Matemáticas
- ✅ **Suma (+)**: Adición de números
- ✅ **Resta (-)**: Sustracción de números  
- ✅ **Multiplicación (×)**: Producto de números
- ✅ **División (÷)**: División de números

### Funciones de Control
- ✅ **Clear Entry (CE)**: Borra la entrada actual
- ✅ **All Clear (AC)**: Limpia toda la operación
- ✅ **Raíz cuadrada (√)**: Cálculo de raíz cuadrada
- ✅ **Paréntesis ( )**: Soporte para agrupación de operaciones

### Características de Interfaz
- ✅ **Botones grandes**: Fáciles de presionar en dispositivos móviles
- ✅ **Pantalla clara**: Visualización nítida de números y resultados
- ✅ **Diseño responsive**: Adaptación a diferentes tamaños de pantalla
- ✅ **Retroalimentación visual**: Indicación clara de las pulsaciones

## 🏗️ Arquitectura MVVM

El proyecto implementa el patrón **Model-View-ViewModel** siguiendo las mejores prácticas de Android:

### Componentes Principales

| Componente | Archivo | Responsabilidad |
|------------|---------|-----------------|
| **Model** | Lógica interna | Operaciones matemáticas y cálculos |
| **View** | `MainActivity` | Interfaz de usuario y eventos |
| **ViewModel** | `CalculatorViewModel.kt` | Estado de la aplicación y lógica de presentación |

## 📸 Capturas de Pantalla

### Pantalla Principal
<img src="S13_CalculadoraBasica/app/src/imagenes/pantalla_principal.jpg" alt="Calculadora Principal" width="40%">
> *Interfaz principal mostrando el teclado numérico y operadores con diseño moderno*

### Operación en Progreso  
<img src="S13_CalculadoraBasica/app/src/imagenes/operacion_progreso.jpg" alt="Operación en Progreso" width="40%">
> *Ejemplo de una operación matemática siendo procesada*

### Resultado Final
<img src="S13_CalculadoraBasica/app/src/imagenes/resultado.jpg" alt="Resultado Final" width="40%">
> *Pantalla mostrando el resultado calculado de una operación*

## 🛠️ Stack Tecnológico

### Desarrollo
- **Lenguaje**: Kotlin
- **IDE**: Android Studio
- **Arquitectura**: MVVM (Model-View-ViewModel)

### Componentes Android
- **ViewModel**: Gestión del estado de la aplicación
- **LiveData/Observable**: Observación reactiva de datos
- **Data Binding**: Enlace bidireccional entre vista y datos

### Interfaz de Usuario
- **Layouts**: XML con diseño responsive
- **Drawables**: Iconos y fondos personalizados
- **Themes**: Esquema de colores moderno

### Testing
- **JUnit**: Pruebas unitarias
- **Espresso**: Pruebas instrumentadas de UI

---
# 📅 Semana 14 - S14_TechPoint

Aplicación móvil que muestra en un mapa interactivo todos los establecimientos de Chimbote donde se venden equipos de cómputo, con funcionalidad de geolocalización y recomendaciones basadas en proximidad.

## 📱 Descripción del Proyecto

TechPoint es una aplicación de mapas especializada que ayuda a los usuarios a encontrar tiendas de equipos de cómputo en la ciudad de Chimbote. La aplicación utiliza Google Maps para mostrar la ubicación de los establecimientos verificados, detecta la posición actual del usuario y proporciona recomendaciones personalizadas basadas en la distancia.

## 🎯 Finalidad

Los objetivos principales de este proyecto son:

- Crear un directorio digital de tiendas de equipos de cómputo en Chimbote
- Implementar funcionalidades de geolocalización y mapas en Android
- Proporcionar recomendaciones inteligentes basadas en proximidad
- Facilitar la búsqueda y navegación hacia establecimientos especializados
- Demostrar el uso de Google Maps API y servicios de ubicación

## ⚡ Funcionalidades

### Mapa Interactivo
- ✅ **Visualización de Google Maps**: Mapa interactivo de la ciudad de Chimbote
- ✅ **Marcadores personalizados**: Identificación visual de tiendas de tecnología
- ✅ **InfoWindows**: Información detallada de cada establecimiento
- ✅ **Zoom y navegación**: Controles intuitivos para explorar el mapa

### Geolocalización
- ✅ **Detección de ubicación**: GPS del usuario en tiempo real
- ✅ **Cálculo de distancias**: Medición precisa a cada establecimiento
- ✅ **Permisos de ubicación**: Manejo seguro de permisos sensibles
- ✅ **Indicador de posición**: Marcador visual de la ubicación actual

### Sistema de Recomendaciones
- ✅ **Establecimientos cercanos**: Lista ordenada por proximidad
- ✅ **Filtrado inteligente**: Clasificación por tipo de productos
- ✅ **Información de contacto**: Teléfonos, direcciones y horarios
- ✅ **Navegación externa**: Integración con apps de mapas del sistema

### Base de Datos Local
- ✅ **Catálogo verificado**: Información actualizada de establecimientos
- ✅ **Categorización**: Clasificación por especialidad tecnológica
- ✅ **Datos offline**: Funcionalidad básica sin conexión a internet

## 🏗️ Arquitectura y Componentes

El proyecto implementa una arquitectura robusta con integración de servicios de Google:

### Componentes Principales

| Componente | Archivo | Responsabilidad |
|------------|---------|-----------------|
| **MainActivity** | `MainActivity.kt` | Interfaz principal y coordinación |
| **Maps Fragment** | Google Maps API | Visualización del mapa interactivo |
| **Location Service** | FusedLocationProvider | Detección de ubicación GPS |
| **Data Model** | Clases de datos | Estructura de establecimientos |

### APIs y Servicios
- **Google Maps SDK**: Renderizado de mapas y marcadores
- **Location Services**: Geolocalización y permisos
- **Places API**: Información adicional de ubicaciones
- **Geocoding**: Conversión de direcciones a coordenadas

## 📸 Capturas de Pantalla

### Ubicación Actual Detectada
<img src="S14_TechPoint/app/src/imagenes/ubicacion_actual.jpg" alt="Ubicación Actual" width="40%">
> *Mapa mostrando la ubicación actual del usuario detectada por GPS*

### Establecimientos Cercanos
<img src="S14_TechPoint/app/src/imagenes/establecimientos.jpg" alt="Establecimientos Cercanos" width="40%">
> *Marcadores de tiendas tecnológicas cercanas a la posición actual*

### Detalle de Establecimiento
<img src="S14_TechPoint/app/src/imagenes/detalle_tienda.jpg" alt="Detalle de Tienda" width="40%">
> *Información detallada de un establecimiento seleccionado*

### Recomendaciones por Proximidad
<img src="S14_TechPoint/app/src/imagenes/tienda_cercana.jpg" alt="Recomendaciones" width="40%">
> *Establecimientos que tienen el mismo dispositivo pero indica el mas cercano*

## 🛠️ Stack Tecnológico

### Desarrollo
- **Lenguaje**: Kotlin
- **IDE**: Android Studio
- **Arquitectura**: MVVM con Repository Pattern

### Google Services
- **Google Maps SDK**: Mapas interactivos
- **Google Play Services**: Servicios de ubicación
- **Places API**: Información de lugares
- **Directions API**: Cálculo de rutas

### Componentes Android
- **FusedLocationProvider**: Geolocalización eficiente
- **RecyclerView**: Lista de establecimientos
- **CardView**: Diseño de tarjetas informativas
- **Material Design**: Interfaz moderna y consistente

### Permisos y Seguridad
- **ACCESS_FINE_LOCATION**: Ubicación precisa
- **ACCESS_COARSE_LOCATION**: Ubicación aproximada
- **INTERNET**: Conectividad para mapas
- **Runtime Permissions**: Manejo dinámico de permisos
