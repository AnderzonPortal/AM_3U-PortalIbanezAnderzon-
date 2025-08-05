package com.example.seguridad_priv_a

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import android.util.Log
import org.json.JSONObject
import org.json.JSONArray
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*

class SecurityAuditManager(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("security_audit", Context.MODE_PRIVATE)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    companion object {
        private const val MAX_ATTEMPTS = 5
        private const val TIME_WINDOW_MS = 60000L // 1 minuto
        private const val RATE_LIMIT_WINDOW = 10000L // 10 segundos
    }

    // 2.2.1 Detectar intentos de acceso sospechosos
    fun detectSuspiciousAccess(userId: String, operation: String): Boolean {
        val currentTime = System.currentTimeMillis()
        val key = "attempts_${userId}_${operation}"
        val attempts = getRecentAttempts(key, currentTime)

        attempts.add(currentTime)
        saveAttempts(key, attempts)

        val isSuspicious = attempts.size > MAX_ATTEMPTS

        if (isSuspicious) {
            generateAlert("SUSPICIOUS_ACCESS", "Usuario $userId: ${attempts.size} intentos en ${operation}")
            logAuditEvent("SECURITY_ALERT", "Acceso sospechoso detectado: $userId - $operation")
        }

        return isSuspicious
    }

    // 2.2.2 Rate limiting para operaciones sensibles
    fun checkRateLimit(userId: String, operation: String): Boolean {
        val currentTime = System.currentTimeMillis()
        val key = "rate_limit_${userId}_${operation}"
        val lastAccess = prefs.getLong(key, 0)

        val withinLimit = (currentTime - lastAccess) >= RATE_LIMIT_WINDOW

        if (withinLimit) {
            prefs.edit().putLong(key, currentTime).apply()
            logAuditEvent("RATE_LIMIT_OK", "Operación permitida: $userId - $operation")
        } else {
            logAuditEvent("RATE_LIMIT_BLOCKED", "Operación bloqueada por rate limit: $userId - $operation")
            generateAlert("RATE_LIMIT_EXCEEDED", "Usuario $userId bloqueado en $operation")
        }

        return withinLimit
    }

    // 2.2.3 Generar alertas por patrones anómalos
    fun generateAlert(type: String, message: String) {
        val alert = JSONObject().apply {
            put("timestamp", dateFormat.format(Date()))
            put("type", type)
            put("message", message)
            put("severity", getSeverityLevel(type))
        }

        saveAlert(alert)
        Log.w("SecurityAudit", "ALERTA: $type - $message")
    }

    // 2.2.4 Exportar logs en formato JSON firmado digitalmente
    fun exportSignedLogs(): String {
        val logs = getAllAuditLogs()
        val alerts = getAllAlerts()

        val exportData = JSONObject().apply {
            put("export_timestamp", dateFormat.format(Date()))
            put("logs", JSONArray(logs))
            put("alerts", JSONArray(alerts))
            put("total_logs", logs.size)
            put("total_alerts", alerts.size)
        }

        val signature = generateDigitalSignature(exportData.toString())
        exportData.put("digital_signature", signature)

        logAuditEvent("LOG_EXPORT", "Logs exportados con firma digital")
        return exportData.toString(2)
    }

    private fun getRecentAttempts(key: String, currentTime: Long): MutableList<Long> {
        val attemptsStr = prefs.getString(key, "") ?: ""
        val attempts = if (attemptsStr.isEmpty()) {
            mutableListOf()
        } else {
            attemptsStr.split(",").mapNotNull { it.toLongOrNull() }.toMutableList()
        }

        // Filtrar intentos dentro de la ventana de tiempo
        return attempts.filter { currentTime - it <= TIME_WINDOW_MS }.toMutableList()
    }

    private fun saveAttempts(key: String, attempts: List<Long>) {
        val attemptsStr = attempts.joinToString(",")
        prefs.edit().putString(key, attemptsStr).apply()
    }

    private fun logAuditEvent(type: String, message: String) {
        val logEntry = JSONObject().apply {
            put("timestamp", dateFormat.format(Date()))
            put("type", type)
            put("message", message)
        }

        val currentLogs = prefs.getString("audit_logs", "") ?: ""
        val newLogs = if (currentLogs.isEmpty()) {
            logEntry.toString()
        } else {
            "$currentLogs\n${logEntry}"
        }

        prefs.edit().putString("audit_logs", newLogs).apply()
    }

    private fun saveAlert(alert: JSONObject) {
        val currentAlerts = prefs.getString("security_alerts", "") ?: ""
        val newAlerts = if (currentAlerts.isEmpty()) {
            alert.toString()
        } else {
            "$currentAlerts\n$alert"
        }

        prefs.edit().putString("security_alerts", newAlerts).apply()
    }

    private fun getSeverityLevel(type: String): String {
        return when (type) {
            "SUSPICIOUS_ACCESS", "RATE_LIMIT_EXCEEDED" -> "HIGH"
            "RATE_LIMIT_BLOCKED" -> "MEDIUM"
            else -> "LOW"
        }
    }

    private fun getAllAuditLogs(): List<String> {
        val logs = prefs.getString("audit_logs", "") ?: ""
        return if (logs.isEmpty()) emptyList() else logs.split("\n")
    }

    private fun getAllAlerts(): List<String> {
        val alerts = prefs.getString("security_alerts", "") ?: ""
        return if (alerts.isEmpty()) emptyList() else alerts.split("\n")
    }

    private fun generateDigitalSignature(data: String): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(data.toByteArray())
            Base64.encodeToString(hash, Base64.DEFAULT).trim()
        } catch (e: Exception) {
            "SIGNATURE_ERROR"
        }
    }

    // Métodos de utilidad para consultas
    fun getAuditSummary(): Map<String, Any> {
        val logs = getAllAuditLogs()
        val alerts = getAllAlerts()

        return mapOf(
            "total_logs" to logs.size,
            "total_alerts" to alerts.size,
            "last_export" to (prefs.getString("last_export", "Nunca") ?: "Nunca"),
            "status" to "Activo"
        )
    }

    fun clearAuditData() {
        prefs.edit().clear().apply()
        logAuditEvent("AUDIT_CLEARED", "Datos de auditoría borrados")
    }
}