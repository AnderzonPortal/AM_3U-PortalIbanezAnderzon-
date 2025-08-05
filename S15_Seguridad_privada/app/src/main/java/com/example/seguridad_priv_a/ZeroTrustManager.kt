package com.example.seguridad_priv_a

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.util.Base64
import android.util.Log
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class ZeroTrustManager(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("zero_trust", Context.MODE_PRIVATE)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    companion object {
        private const val TOKEN_VALIDITY_MS = 15 * 60 * 1000L // 15 minutos
        private const val HMAC_ALGORITHM = "HmacSHA256"

        // 3.1.2 Privilegios por contexto
        enum class SecurityContext(val level: Int, val privileges: List<String>) {
            GUEST(0, listOf("VIEW_PUBLIC")),
            USER(1, listOf("VIEW_PUBLIC", "VIEW_LOGS")),
            ADMIN(2, listOf("VIEW_PUBLIC", "VIEW_LOGS", "CLEAR_DATA", "EXPORT_LOGS")),
            SYSTEM(3, listOf("VIEW_PUBLIC", "VIEW_LOGS", "CLEAR_DATA", "EXPORT_LOGS", "ROTATE_KEYS"))
        }

        enum class SensitiveOperation {
            VIEW_LOGS, CLEAR_DATA, EXPORT_LOGS, ROTATE_KEYS, ACCESS_BIOMETRIC
        }
    }

    // 3.1.3 Token temporal de sesión
    data class SecurityToken(
        val token: String,
        val context: SecurityContext,
        val issuedAt: Long,
        val expiresAt: Long,
        val signature: String
    )

    // 3.1.1 Validación independiente de cada operación sensible
    fun validateOperation(operation: SensitiveOperation, token: SecurityToken?): ValidationResult {
        // Verificar token válido
        if (token == null || !isTokenValid(token)) {
            logSecurityEvent("OPERATION_DENIED", "Token inválido para $operation")
            return ValidationResult(false, "Token de seguridad inválido o expirado")
        }

        // Verificar privilegios por contexto
        val requiredPrivilege = mapOperationToPrivilege(operation)
        if (!hasPrivilege(token.context, requiredPrivilege)) {
            logSecurityEvent("PRIVILEGE_DENIED", "Privilegios insuficientes: ${token.context} para $operation")
            return ValidationResult(false, "Privilegios insuficientes para esta operación")
        }

        // 3.1.4 Attestation de integridad
        if (!verifyAppIntegrity()) {
            logSecurityEvent("INTEGRITY_FAILURE", "Fallo en attestation de integridad para $operation")
            return ValidationResult(false, "Integridad de la aplicación comprometida")
        }

        // Validación adicional por contexto
        if (!validateContextualSecurity(operation, token)) {
            logSecurityEvent("CONTEXTUAL_FAILURE", "Fallo en validación contextual para $operation")
            return ValidationResult(false, "Contexto de seguridad inválido")
        }

        logSecurityEvent("OPERATION_APPROVED", "Operación $operation aprobada para ${token.context}")
        return ValidationResult(true, "Operación autorizada")
    }

    // 3.1.3 Generación de tokens temporales
    fun generateSecurityToken(context: SecurityContext): SecurityToken {
        val currentTime = System.currentTimeMillis()
        val expiresAt = currentTime + TOKEN_VALIDITY_MS

        val tokenData = JSONObject().apply {
            put("context", context.name)
            put("level", context.level)
            put("issued_at", currentTime)
            put("expires_at", expiresAt)
            put("device_id", getDeviceFingerprint())
            put("app_signature", getAppSignature())
        }

        val tokenString = Base64.encodeToString(tokenData.toString().toByteArray(), Base64.DEFAULT)
        val signature = generateTokenSignature(tokenString)

        val token = SecurityToken(tokenString, context, currentTime, expiresAt, signature)

        // Almacenar token activo
        storeActiveToken(token)
        logSecurityEvent("TOKEN_GENERATED", "Token generado para contexto $context")

        return token
    }

    // 3.1.2 Principio de menor privilegio
    private fun hasPrivilege(context: SecurityContext, privilege: String): Boolean {
        return context.privileges.contains(privilege)
    }

    private fun mapOperationToPrivilege(operation: SensitiveOperation): String {
        return when (operation) {
            SensitiveOperation.VIEW_LOGS -> "VIEW_LOGS"
            SensitiveOperation.CLEAR_DATA -> "CLEAR_DATA"
            SensitiveOperation.EXPORT_LOGS -> "EXPORT_LOGS"
            SensitiveOperation.ROTATE_KEYS -> "ROTATE_KEYS"
            SensitiveOperation.ACCESS_BIOMETRIC -> "VIEW_LOGS"
        }
    }

    // 3.1.4 Attestation de integridad de la aplicación
    private fun verifyAppIntegrity(): Boolean {
        return try {
            val currentSignature = getAppSignature()
            if (currentSignature == "NO_SIGNATURE" || currentSignature == "UNKNOWN_SIGNATURE") {
                return false
            }

            val storedSignature = prefs.getString("app_signature", null)

            // Primera ejecución: almacenar firma
            if (storedSignature == null) {
                prefs.edit().putString("app_signature", currentSignature).apply()
                return true
            }

            // Verificar que la firma no haya cambiado
            val isValid = currentSignature == storedSignature

            if (!isValid) {
                logSecurityEvent("INTEGRITY_BREACH", "Firma de aplicación modificada")
            }

            isValid
        } catch (e: Exception) {
            logSecurityEvent("INTEGRITY_ERROR", "Error en verificación de integridad: ${e.message}")
            false
        }
    }

    private fun isTokenValid(token: SecurityToken): Boolean {
        // Verificar expiración
        if (System.currentTimeMillis() > token.expiresAt) {
            return false
        }

        // Verificar firma
        val expectedSignature = generateTokenSignature(token.token)
        if (token.signature != expectedSignature) {
            return false
        }

        // Verificar que el token esté activo
        val activeToken = getActiveToken()
        return activeToken?.token == token.token
    }

    private fun validateContextualSecurity(operation: SensitiveOperation, token: SecurityToken): Boolean {
        // Validaciones adicionales por operación
        when (operation) {
            SensitiveOperation.CLEAR_DATA -> {
                // Requiere confirmación reciente
                val lastAuth = prefs.getLong("last_auth_time", 0)
                if (System.currentTimeMillis() - lastAuth > 300000) { // 5 minutos
                    return false
                }
            }
            SensitiveOperation.EXPORT_LOGS -> {
                // Verificar que no se haya exportado recientemente
                val lastExport = prefs.getLong("last_export_time", 0)
                if (System.currentTimeMillis() - lastExport < 60000) { // 1 minuto
                    return false
                }
            }
            else -> { /* Otras validaciones */ }
        }

        return true
    }

    private fun generateTokenSignature(tokenData: String): String {
        return try {
            val key = getSigningKey()
            val mac = Mac.getInstance(HMAC_ALGORITHM)
            mac.init(SecretKeySpec(key.toByteArray(), HMAC_ALGORITHM))
            Base64.encodeToString(mac.doFinal(tokenData.toByteArray()), Base64.DEFAULT).trim()
        } catch (e: Exception) {
            "SIGNATURE_ERROR"
        }
    }

    private fun getSigningKey(): String {
        var key = prefs.getString("signing_key", null)
        if (key == null) {
            val random = SecureRandom()
            val keyBytes = ByteArray(32)
            random.nextBytes(keyBytes)
            key = Base64.encodeToString(keyBytes, Base64.DEFAULT)
            prefs.edit().putString("signing_key", key).apply()
        }
        return key
    }

    private fun getDeviceFingerprint(): String {
        return try {
            val deviceInfo = "${Build.BRAND}_${Build.MODEL}_${Build.VERSION.SDK_INT}"
            val digest = MessageDigest.getInstance("SHA-256")
            Base64.encodeToString(digest.digest(deviceInfo.toByteArray()), Base64.DEFAULT).trim()
        } catch (e: Exception) {
            "UNKNOWN_DEVICE"
        }
    }

    private fun getAppSignature(): String {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
            }

            val signature = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.apkContentsSigners?.get(0)
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures?.get(0)
            }

            if (signature != null) {
                val digest = MessageDigest.getInstance("SHA-256")
                Base64.encodeToString(digest.digest(signature.toByteArray()), Base64.DEFAULT).trim()
            } else {
                "NO_SIGNATURE"
            }
        } catch (e: Exception) {
            "UNKNOWN_SIGNATURE"
        }
    }

    private fun storeActiveToken(token: SecurityToken) {
        val tokenJson = JSONObject().apply {
            put("token", token.token)
            put("context", token.context.name)
            put("issued_at", token.issuedAt)
            put("expires_at", token.expiresAt)
            put("signature", token.signature)
        }
        prefs.edit().putString("active_token", tokenJson.toString()).apply()
    }

    private fun getActiveToken(): SecurityToken? {
        return try {
            val tokenStr = prefs.getString("active_token", null) ?: return null
            val tokenJson = JSONObject(tokenStr)

            val context = SecurityContext.valueOf(tokenJson.getString("context"))
            SecurityToken(
                tokenJson.getString("token"),
                context,
                tokenJson.getLong("issued_at"),
                tokenJson.getLong("expires_at"),
                tokenJson.getString("signature")
            )
        } catch (e: Exception) {
            null
        }
    }

    fun updateAuthTime() {
        prefs.edit().putLong("last_auth_time", System.currentTimeMillis()).apply()
    }

    fun updateExportTime() {
        prefs.edit().putLong("last_export_time", System.currentTimeMillis()).apply()
    }

    fun revokeActiveToken() {
        prefs.edit().remove("active_token").apply()
        logSecurityEvent("TOKEN_REVOKED", "Token activo revocado")
    }

    private fun logSecurityEvent(event: String, details: String) {
        val timestamp = dateFormat.format(Date())
        val logEntry = "$timestamp - ZERO_TRUST_$event: $details"

        val currentLogs = prefs.getString("security_logs", "") ?: ""
        val newLogs = if (currentLogs.isEmpty()) {
            logEntry
        } else {
            "$currentLogs\n$logEntry"
        }

        prefs.edit().putString("security_logs", newLogs).apply()
        Log.d("ZeroTrust", logEntry)
    }

    fun getSecurityLogs(): List<String> {
        val logs = prefs.getString("security_logs", "") ?: ""
        return if (logs.isEmpty()) emptyList() else logs.split("\n")
    }

    fun getSecurityStatus(): Map<String, Any> {
        val activeToken = getActiveToken()
        return mapOf(
            "has_active_token" to (activeToken != null),
            "token_context" to (activeToken?.context?.name ?: "NONE"),
            "token_expires_at" to (activeToken?.expiresAt ?: 0),
            "app_integrity" to verifyAppIntegrity(),
            "device_fingerprint" to getDeviceFingerprint(),
            "total_security_events" to getSecurityLogs().size
        )
    }
}

data class ValidationResult(
    val isValid: Boolean,
    val message: String
)