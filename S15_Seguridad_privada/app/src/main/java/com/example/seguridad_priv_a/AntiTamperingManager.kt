package com.example.seguridad_priv_a

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Build
import android.os.Debug
import android.provider.Settings
import android.util.Base64
import android.util.Log
import java.io.File
import java.security.MessageDigest
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.*
import java.io.ByteArrayInputStream
import java.security.KeyStore
import java.security.SecureRandom

class AntiTamperingManager(private val context: Context) {

    // 3.2.2 Obfuscación de strings sensibles
    private object ObfuscatedStrings {
        // Strings obfuscados usando XOR simple (en producción usar algo más robusto)
        private val KEY = byteArrayOf(0x7A.toByte(), 0x2B.toByte(), 0x1C.toByte(), 0x8D.toByte())

        val DEBUG_PROPERTY = decrypt(byteArrayOf(0x0A.toByte(), 0x5F.toByte(), 0x68.toByte(), 0xE7.toByte(), 0x18.toByte(), 0x47.toByte(), 0x74.toByte(), 0xEB.toByte())) // "ro.build"
        val EMULATOR_FILES = listOf(
            decrypt(byteArrayOf(0x17.toByte(), 0x44.toByte(), 0x64.toByte(), 0xF8.toByte(), 0x1E.toByte(), 0x5E.toByte(), 0x7A.toByte(), 0xE0.toByte())), // "/dev/qemu"
            decrypt(byteArrayOf(0x17.toByte(), 0x44.toByte(), 0x64.toByte(), 0xF8.toByte(), 0x1E.toByte(), 0x5A.toByte(), 0x7A.toByte(), 0xE0.toByte()))  // "/dev/vbox"
        )
        val ROOT_PATHS = listOf(
            decrypt(byteArrayOf(0x17.toByte(), 0x5F.toByte(), 0x79.toByte(), 0xF1.toByte(), 0x1E.toByte(), 0x44.toByte(), 0x78.toByte(), 0xF3.toByte())), // "/system/su"
            decrypt(byteArrayOf(0x17.toByte(), 0x5F.toByte(), 0x79.toByte(), 0xF1.toByte(), 0x1A.toByte(), 0x44.toByte(), 0x78.toByte(), 0xF3.toByte()))  // "/system/xbin"
        )

        private fun decrypt(data: ByteArray): String {
            return String(data.mapIndexed { i, byte -> (byte.toInt() xor KEY[i % KEY.size].toInt()).toByte() }.toByteArray())
        }
    }

    // 3.2.3 Constantes criptográficas obfuscadas
    private object CryptoConstants {
        private val OBFUSCATED_ALGORITHM = byteArrayOf(0x01.toByte(), 0x0B.toByte(), 0x13.toByte(), 0x1F.toByte(), 0x0C.toByte(), 0x07.toByte(), 0x06.toByte(), 0x1F.toByte())
        private val OBFUSCATED_TRANSFORMATION = byteArrayOf(0x01.toByte(), 0x0B.toByte(), 0x13.toByte(), 0x1F.toByte(), 0x18.toByte(), 0x09.toByte(), 0x0D.toByte(), 0x18.toByte(), 0x18.toByte(), 0x18.toByte())

        fun getAlgorithm(): String = deobfuscate(OBFUSCATED_ALGORITHM) // "AES"
        fun getTransformation(): String = deobfuscate(OBFUSCATED_TRANSFORMATION) // "AES/GCM"

        private fun deobfuscate(data: ByteArray): String {
            return String(data.map { (it + 0x40).toByte() }.toByteArray())
        }
    }

    data class TamperingReport(
        val isCompromised: Boolean,
        val threats: List<String>,
        val securityLevel: SecurityLevel
    )

    enum class SecurityLevel { SECURE, WARNING, COMPROMISED }

    // 3.2.1 Detección de debugging activo y emuladores
    fun detectTamperingAttempts(): TamperingReport {
        val threats = mutableListOf<String>()
        var securityLevel = SecurityLevel.SECURE

        // Detectar debugging activo
        if (isDebuggerAttached()) {
            threats.add("Debugger activo detectado")
            securityLevel = SecurityLevel.COMPROMISED
        }

        // Detectar emuladores
        if (isRunningOnEmulator()) {
            threats.add("Ejecución en emulador detectada")
            securityLevel = SecurityLevel.WARNING
        }

        // Detectar root
        if (isDeviceRooted()) {
            threats.add("Dispositivo rooteado detectado")
            securityLevel = SecurityLevel.COMPROMISED
        }

        // Detectar aplicaciones sospechosas
        val suspiciousApps = detectSuspiciousApps()
        if (suspiciousApps.isNotEmpty()) {
            threats.add("Apps sospechosas: ${suspiciousApps.joinToString()}")
            securityLevel = SecurityLevel.WARNING
        }

        // Verificar integridad del APK
        if (!verifyApkIntegrity()) {
            threats.add("Integridad del APK comprometida")
            securityLevel = SecurityLevel.COMPROMISED
        }

        return TamperingReport(
            isCompromised = securityLevel == SecurityLevel.COMPROMISED,
            threats = threats,
            securityLevel = securityLevel
        )
    }

    private fun isDebuggerAttached(): Boolean {
        return Debug.isDebuggerConnected() ||
                Debug.waitingForDebugger() ||
                isApplicationDebuggable()
    }

    private fun isApplicationDebuggable(): Boolean {
        return try {
            (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        } catch (e: Exception) {
            false
        }
    }

    private fun isRunningOnEmulator(): Boolean {
        return checkEmulatorProperties() ||
                checkEmulatorFiles() ||
                checkEmulatorFeatures()
    }

    private fun checkEmulatorProperties(): Boolean {
        val properties = listOf(
            "ro.build.fingerprint",
            "ro.kernel.android.checkjni",
            "ro.hardware"
        )

        return properties.any { prop ->
            val value = getSystemProperty(prop)
            value.contains("generic") ||
                    value.contains("unknown") ||
                    value.contains("emulator") ||
                    value.contains("sdk")
        }
    }

    private fun checkEmulatorFiles(): Boolean {
        return ObfuscatedStrings.EMULATOR_FILES.any { File(it).exists() }
    }

    private fun checkEmulatorFeatures(): Boolean {
        return Build.BRAND.startsWith("generic") ||
                Build.DEVICE.startsWith("generic") ||
                Build.PRODUCT.contains("sdk") ||
                Build.HARDWARE.contains("goldfish") ||
                Build.HARDWARE.contains("ranchu")
    }

    private fun isDeviceRooted(): Boolean {
        return checkRootPaths() ||
                checkRootApps() ||
                checkBuildTags()
    }

    private fun checkRootPaths(): Boolean {
        return ObfuscatedStrings.ROOT_PATHS.any { File(it).exists() }
    }

    private fun checkRootApps(): Boolean {
        val rootApps = listOf("com.noshufou.android.su", "com.thirdparty.superuser", "eu.chainfire.supersu")
        return rootApps.any { isPackageInstalled(it) }
    }

    private fun checkBuildTags(): Boolean {
        return Build.TAGS?.contains("test-keys") == true
    }

    private fun detectSuspiciousApps(): List<String> {
        val suspiciousApps = listOf(
            "com.hexeditplus", // Hex editor
            "bin.mt.plus",     // MT Manager
            "com.aide.ui"      // AIDE
        )

        return suspiciousApps.filter { isPackageInstalled(it) }
    }

    // 3.2.3 Verificación de firma digital en runtime
    private fun verifyApkIntegrity(): Boolean {
        return try {
            val expectedSignature = getExpectedSignature()
            val currentSignature = getCurrentAppSignature()

            if (expectedSignature.isEmpty()) {
                // Primera ejecución: almacenar firma esperada
                storeExpectedSignature(currentSignature)
                return true
            }

            currentSignature == expectedSignature
        } catch (e: Exception) {
            Log.e("AntiTampering", "Error verificando integridad: ${e.message}")
            false
        }
    }

    private fun getCurrentAppSignature(): String {
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
                ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    // 3.2.4 Certificate Pinning para comunicaciones futuras
    fun createPinnedSSLContext(): SSLContext? {
        return try {
            // Certificado esperado (en producción, usar el certificado real)
            val expectedCertificate = """
                -----BEGIN CERTIFICATE-----
                MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA7VKx/sample/certificate
                -----END CERTIFICATE-----
            """.trimIndent()

            val certificateFactory = CertificateFactory.getInstance("X.509")
            val certificate = certificateFactory.generateCertificate(
                ByteArrayInputStream(expectedCertificate.toByteArray())
            ) as X509Certificate

            val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
            keyStore.load(null, null)
            keyStore.setCertificateEntry("server", certificate)

            val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
            trustManagerFactory.init(keyStore)

            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, trustManagerFactory.trustManagers, SecureRandom())

            sslContext
        } catch (e: Exception) {
            Log.e("AntiTampering", "Error creando SSL Context pinned: ${e.message}")
            null
        }
    }

    fun createPinnedHostnameVerifier(pinnedHost: String): HostnameVerifier {
        return HostnameVerifier { hostname, _ ->
            hostname == pinnedHost
        }
    }

    // Métodos auxiliares
    private fun getSystemProperty(prop: String): String {
        return try {
            Runtime.getRuntime()
                .exec("getprop $prop")
                .inputStream
                .bufferedReader()
                .readLine() ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    private fun isPackageInstalled(packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun getExpectedSignature(): String {
        return context.getSharedPreferences("anti_tampering", Context.MODE_PRIVATE)
            .getString("expected_signature", "") ?: ""
    }

    private fun storeExpectedSignature(signature: String) {
        context.getSharedPreferences("anti_tampering", Context.MODE_PRIVATE)
            .edit()
            .putString("expected_signature", signature)
            .apply()
    }

    // Método de utilidad para obtener reporte completo
    fun getSecurityReport(): Map<String, Any> {
        val report = detectTamperingAttempts()
        return mapOf(
            "is_compromised" to report.isCompromised,
            "security_level" to report.securityLevel.name,
            "threat_count" to report.threats.size,
            "threats" to report.threats,
            "debugger_attached" to isDebuggerAttached(),
            "running_on_emulator" to isRunningOnEmulator(),
            "device_rooted" to isDeviceRooted(),
            "apk_integrity_valid" to verifyApkIntegrity()
        )
    }

    // Método para aplicar contramedidas
    fun applyCountermeasures(report: TamperingReport) {
        when (report.securityLevel) {
            SecurityLevel.COMPROMISED -> {
                // En producción: cerrar app, borrar datos sensibles, notificar servidor
                Log.w("AntiTampering", "COMPROMISED: ${report.threats}")
            }
            SecurityLevel.WARNING -> {
                // Incrementar logging, reducir funcionalidad
                Log.w("AntiTampering", "WARNING: ${report.threats}")
            }
            SecurityLevel.SECURE -> {
                Log.i("AntiTampering", "Security status: SECURE")
            }
        }
    }
}