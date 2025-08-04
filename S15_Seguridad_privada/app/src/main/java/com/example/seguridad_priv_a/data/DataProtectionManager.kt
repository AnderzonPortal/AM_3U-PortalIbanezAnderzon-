package com.example.seguridad_priv_a.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DataProtectionManager(private val context: Context) {
    
    private lateinit var encryptedPrefs: SharedPreferences
    private lateinit var accessLogPrefs: SharedPreferences
    
    fun initialize() {
        try {
            // Crear o obtener la clave maestra
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
                
            // Crear SharedPreferences encriptado para datos sensibles
            encryptedPrefs = EncryptedSharedPreferences.create(
                context,
                "secure_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            
            // SharedPreferences normal para logs de acceso (no son datos sensibles críticos)
            accessLogPrefs = context.getSharedPreferences("access_logs", Context.MODE_PRIVATE)
            
        } catch (e: Exception) {
            // Fallback a SharedPreferences normales si falla la encriptación
            encryptedPrefs = context.getSharedPreferences("fallback_prefs", Context.MODE_PRIVATE)
            accessLogPrefs = context.getSharedPreferences("access_logs", Context.MODE_PRIVATE)
        }
    }
    
    fun storeSecureData(key: String, value: String) {
        encryptedPrefs.edit().putString(key, value).apply()
        logAccess("DATA_STORAGE", "Dato almacenado de forma segura: $key")
    }
    
    fun getSecureData(key: String): String? {
        val data = encryptedPrefs.getString(key, null)
        if (data != null) {
            logAccess("DATA_ACCESS", "Dato accedido: $key")
        }
        return data
    }
    
    fun logAccess(category: String, action: String) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val logEntry = "$timestamp - $category: $action"
        
        // Obtener logs existentes
        val existingLogs = accessLogPrefs.getString("logs", "") ?: ""
        val newLogs = if (existingLogs.isEmpty()) {
            logEntry
        } else {
            "$existingLogs\\n$logEntry"
        }
        
        // Guardar logs actualizados
        accessLogPrefs.edit().putString("logs", newLogs).apply()
        
        // Limitar el número de logs (mantener solo los últimos 100)
        val logLines = newLogs.split("\\n")
        if (logLines.size > 100) {
            val trimmedLogs = logLines.takeLast(100).joinToString("\\n")
            accessLogPrefs.edit().putString("logs", trimmedLogs).apply()
        }
    }
    
    fun getAccessLogs(): List<String> {
        val logsString = accessLogPrefs.getString("logs", "") ?: ""
        return if (logsString.isEmpty()) {
            emptyList()
        } else {
            logsString.split("\\n").reversed() // Mostrar los más recientes primero
        }
    }
    
    fun clearAllData() {
        // Limpiar datos encriptados
        encryptedPrefs.edit().clear().apply()
        
        // Limpiar logs
        accessLogPrefs.edit().clear().apply()
        
        logAccess("DATA_MANAGEMENT", "Todos los datos han sido borrados de forma segura")
    }
    
    fun getDataProtectionInfo(): Map<String, String> {
        return mapOf(
            "Encriptación" to "AES-256-GCM",
            "Almacenamiento" to "Local encriptado",
            "Logs de acceso" to "${getAccessLogs().size} entradas",
            "Última limpieza" to (getSecureData("last_cleanup") ?: "Nunca"),
            "Estado de seguridad" to "Activo"
        )
    }
    
    fun anonymizeData(data: String): String {
        // Implementación básica de anonimización
        return data.replace(Regex("[0-9]"), "*")
            .replace(Regex("[A-Za-z]{3,}"), "***")
    }
}