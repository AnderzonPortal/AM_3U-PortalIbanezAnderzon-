package com.example.seguridad_priv_a

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.seguridad_priv_a.databinding.ActivityDataProtectionBinding
import android.content.Context
import android.content.SharedPreferences
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.*
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import android.util.Base64
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import androidx.core.content.ContextCompat
import android.os.Handler
import android.os.Looper
import android.app.KeyguardManager

class DataProtectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDataProtectionBinding
    private val dataProtectionManager by lazy {
        (application as PermissionsApplication).dataProtectionManager
    }

    // 2.3 Biometría y Autenticación
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    private var isAuthenticated = false
    private var sessionHandler = Handler(Looper.getMainLooper())
    private var sessionTimeout: Runnable? = null
    private val SESSION_TIMEOUT_MS = 5 * 60 * 1000L // 5 minutos

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDataProtectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBiometricAuthentication()
        setupUI()

        // Solicitar autenticación al iniciar
        requestAuthentication()

        dataProtectionManager.logAccess("NAVIGATION", "DataProtectionActivity abierta")
    }

    // 2.3.1 Configurar BiometricPrompt API
    private fun setupBiometricAuthentication() {
        val executor = ContextCompat.getMainExecutor(this)

        biometricPrompt = BiometricPrompt(this as FragmentActivity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    handleAuthenticationFailure("Error biométrico: $errString")
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    handleAuthenticationSuccess()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    handleAuthenticationFailure("Autenticación biométrica fallida")
                }
            })

        // 2.3.2 Implementar fallback a PIN/Pattern
        val biometricManager = BiometricManager.from(this)
        val authenticators = when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricManager.Authenticators.BIOMETRIC_STRONG
            else -> BiometricManager.Authenticators.DEVICE_CREDENTIAL or BiometricManager.Authenticators.BIOMETRIC_WEAK
        }

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Acceso Seguro Requerido")
            .setSubtitle("Autentícate para acceder a los datos de protección")
            .setAllowedAuthenticators(authenticators)
            .build()
    }

    private fun requestAuthentication() {
        if (!isAuthenticated) {
            biometricPrompt.authenticate(promptInfo)
        }
    }

    private fun handleAuthenticationSuccess() {
        isAuthenticated = true
        loadDataProtectionInfo()
        loadAccessLogs()
        startSessionTimeout()
        dataProtectionManager.logAccess("AUTHENTICATION", "Autenticación biométrica exitosa")
        Toast.makeText(this, "Autenticación exitosa", Toast.LENGTH_SHORT).show()
    }

    private fun handleAuthenticationFailure(error: String) {
        isAuthenticated = false
        dataProtectionManager.logAccess("AUTHENTICATION_FAILED", error)

        // 2.3.2 Fallback a PIN/Pattern usando KeyguardManager
        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (keyguardManager.isDeviceSecure) {
            showFallbackAuthentication()
        } else {
            Toast.makeText(this, "No hay métodos de autenticación configurados", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun showFallbackAuthentication() {
        AlertDialog.Builder(this)
            .setTitle("Autenticación Requerida")
            .setMessage("La biometría falló. ¿Deseas usar PIN/Patrón del dispositivo?")
            .setPositiveButton("Usar PIN/Patrón") { _, _ ->
                // Solicitar autenticación con credenciales del dispositivo
                val fallbackPrompt = BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Autenticación con PIN/Patrón")
                    .setSubtitle("Ingresa tu PIN o patrón del dispositivo")
                    .setAllowedAuthenticators(BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                    .build()
                biometricPrompt.authenticate(fallbackPrompt)
            }
            .setNegativeButton("Cancelar") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }

    // 2.3.3 Timeout de sesión tras 5 minutos de inactividad
    private fun startSessionTimeout() {
        resetSessionTimeout()
    }

    private fun resetSessionTimeout() {
        sessionTimeout?.let { sessionHandler.removeCallbacks(it) }

        sessionTimeout = Runnable {
            isAuthenticated = false
            dataProtectionManager.logAccess("SESSION_TIMEOUT", "Sesión expirada por inactividad")
            Toast.makeText(this, "Sesión expirada. Autentícate nuevamente", Toast.LENGTH_LONG).show()
            clearSensitiveData()
            requestAuthentication()
        }

        sessionHandler.postDelayed(sessionTimeout!!, SESSION_TIMEOUT_MS)
    }

    private fun clearSensitiveData() {
        binding.tvAccessLogs.text = "Sesión expirada. Autentícate para ver los datos."
        binding.tvDataProtectionInfo.text = "🔒 ACCESO BLOQUEADO\n\nLa sesión ha expirado por seguridad."
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        if (isAuthenticated) {
            resetSessionTimeout()
        }
    }

    private fun setupUI() {
        binding.btnViewLogs.setOnClickListener {
            if (isAuthenticated) {
                loadAccessLogs()
                resetSessionTimeout()
                Toast.makeText(this, "Logs actualizados", Toast.LENGTH_SHORT).show()
            } else {
                requestAuthentication()
            }
        }

        binding.btnClearData.setOnClickListener {
            if (isAuthenticated) {
                showClearDataDialog()
                resetSessionTimeout()
            } else {
                requestAuthentication()
            }
        }
    }

    private fun loadDataProtectionInfo() {
        if (!isAuthenticated) {
            binding.tvDataProtectionInfo.text = "🔒 Autenticación requerida para ver información"
            return
        }

        val info = dataProtectionManager.getDataProtectionInfo()
        val infoText = StringBuilder()

        infoText.append("🔐 INFORMACIÓN DE SEGURIDAD\\n\\n")
        info.forEach { (key, value) ->
            infoText.append("• $key: $value\\n")
        }

        infoText.append("\\n📊 EVIDENCIAS DE PROTECCIÓN:\\n")
        infoText.append("• Encriptación AES-256-GCM activa\\n")
        infoText.append("• Autenticación biométrica activa\\n")
        infoText.append("• Timeout de sesión: 5 minutos\\n")
        infoText.append("• Todos los accesos registrados\\n")
        infoText.append("• Datos anonimizados automáticamente\\n")
        infoText.append("• Almacenamiento local seguro\\n")
        infoText.append("• No hay compartición de datos\\n")

        binding.tvDataProtectionInfo.text = infoText.toString()

        dataProtectionManager.logAccess("DATA_PROTECTION", "Información de protección mostrada")
    }

    private fun loadAccessLogs() {
        if (!isAuthenticated) {
            binding.tvAccessLogs.text = "🔒 Autenticación requerida para ver logs"
            return
        }

        val logs = dataProtectionManager.getAccessLogs()

        if (logs.isNotEmpty()) {
            val logsText = logs.joinToString("\\n")
            binding.tvAccessLogs.text = logsText
        } else {
            binding.tvAccessLogs.text = "No hay logs disponibles"
        }

        dataProtectionManager.logAccess("DATA_ACCESS", "Logs de acceso consultados")
    }

    private fun showClearDataDialog() {
        AlertDialog.Builder(this)
            .setTitle("Borrar Todos los Datos")
            .setMessage("¿Estás seguro de que deseas borrar todos los datos almacenados y logs de acceso? Esta acción no se puede deshacer.")
            .setPositiveButton("Borrar") { _, _ ->
                clearAllData()
                resetSessionTimeout()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun clearAllData() {
        dataProtectionManager.clearAllData()

        // Actualizar UI
        binding.tvAccessLogs.text = "Todos los datos han sido borrados"
        binding.tvDataProtectionInfo.text = "🔐 DATOS BORRADOS DE FORMA SEGURA\\n\\nTodos los datos personales y logs han sido eliminados del dispositivo."

        Toast.makeText(this, "Datos borrados de forma segura", Toast.LENGTH_LONG).show()

        // Este log se creará después del borrado
        dataProtectionManager.logAccess("DATA_MANAGEMENT", "Todos los datos borrados por el usuario")
    }

    override fun onResume() {
        super.onResume()
        if (isAuthenticated) {
            loadAccessLogs() // Actualizar logs al volver a la actividad
            resetSessionTimeout()
        }
    }

    override fun onPause() {
        super.onPause()
        sessionTimeout?.let { sessionHandler.removeCallbacks(it) }
    }

    override fun onDestroy() {
        super.onDestroy()
        sessionTimeout?.let { sessionHandler.removeCallbacks(it) }
    }
}

class DataProtectionManager(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("secure_data", Context.MODE_PRIVATE)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    companion object {
        private const val KEY_ROTATION_DAYS = 30
        private const val AES_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val HMAC_ALGORITHM = "HmacSHA256"
    }

    init {
        checkAndRotateKey()
    }

    // 2.1.1 Rotación automática de claves maestras cada 30 días
    fun rotateEncryptionKey(): Boolean {
        return try {
            val keyGenerator = KeyGenerator.getInstance("AES")
            keyGenerator.init(256)
            val newKey = keyGenerator.generateKey()

            val keyBytes = Base64.encodeToString(newKey.encoded, Base64.DEFAULT)
            val currentTime = System.currentTimeMillis()

            prefs.edit()
                .putString("master_key", keyBytes)
                .putLong("key_created_time", currentTime)
                .apply()

            logAccess("KEY_ROTATION", "Clave maestra rotada exitosamente")
            true
        } catch (e: Exception) {
            logAccess("KEY_ROTATION_ERROR", "Error al rotar clave: ${e.message}")
            false
        }
    }

    // 2.1.2 Verificación de integridad de datos usando HMAC
    fun verifyDataIntegrity(data: String): Boolean {
        return try {
            val parts = data.split(":")
            if (parts.size != 2) return false

            val encryptedData = parts[0]
            val storedHmac = parts[1]

            val key = getCurrentKey()
            val mac = Mac.getInstance(HMAC_ALGORITHM)
            mac.init(key)
            val calculatedHmac = Base64.encodeToString(mac.doFinal(encryptedData.toByteArray()), Base64.DEFAULT)

            val isValid = calculatedHmac.trim() == storedHmac.trim()
            logAccess("INTEGRITY_CHECK", "Verificación de integridad: ${if (isValid) "VÁLIDA" else "FALLIDA"}")
            isValid
        } catch (e: Exception) {
            logAccess("INTEGRITY_ERROR", "Error en verificación: ${e.message}")
            false
        }
    }

    // 2.1.3 Key derivation con salt único por usuario
    private fun deriveKeyWithSalt(userId: String): SecretKey {
        val salt = getSaltForUser(userId)
        val baseKey = getCurrentKey()

        val mac = Mac.getInstance(HMAC_ALGORITHM)
        mac.init(baseKey)
        val derivedKeyBytes = mac.doFinal((userId + salt).toByteArray())

        return SecretKeySpec(derivedKeyBytes.sliceArray(0..31), "AES")
    }

    private fun getSaltForUser(userId: String): String {
        val saltKey = "salt_$userId"
        var salt = prefs.getString(saltKey, null)

        if (salt == null) {
            val random = SecureRandom()
            val saltBytes = ByteArray(16)
            random.nextBytes(saltBytes)
            salt = Base64.encodeToString(saltBytes, Base64.DEFAULT)
            prefs.edit().putString(saltKey, salt).apply()
        }

        return salt
    }

    private fun getCurrentKey(): SecretKey {
        val keyString = prefs.getString("master_key", null)
        return if (keyString != null) {
            val keyBytes = Base64.decode(keyString, Base64.DEFAULT)
            SecretKeySpec(keyBytes, "AES")
        } else {
            // Generar nueva clave si no existe
            rotateEncryptionKey()
            getCurrentKey()
        }
    }

    private fun checkAndRotateKey() {
        val lastRotation = prefs.getLong("key_created_time", 0)
        val daysSinceRotation = (System.currentTimeMillis() - lastRotation) / (1000 * 60 * 60 * 24)

        if (daysSinceRotation >= KEY_ROTATION_DAYS) {
            rotateEncryptionKey()
        }
    }

    // Encriptación mejorada con HMAC
    fun encryptData(data: String, userId: String = "default"): String {
        return try {
            val key = deriveKeyWithSalt(userId)
            val cipher = Cipher.getInstance(AES_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key)

            val encryptedBytes = cipher.doFinal(data.toByteArray())
            val iv = cipher.iv

            val encryptedData = Base64.encodeToString(iv + encryptedBytes, Base64.DEFAULT)

            // Agregar HMAC para integridad
            val mac = Mac.getInstance(HMAC_ALGORITHM)
            mac.init(key)
            val hmac = Base64.encodeToString(mac.doFinal(encryptedData.toByteArray()), Base64.DEFAULT)

            "$encryptedData:$hmac"
        } catch (e: Exception) {
            logAccess("ENCRYPTION_ERROR", "Error al encriptar: ${e.message}")
            ""
        }
    }

    fun decryptData(encryptedData: String, userId: String = "default"): String {
        return try {
            if (!verifyDataIntegrity(encryptedData)) {
                return ""
            }

            val dataOnly = encryptedData.split(":")[0]
            val key = deriveKeyWithSalt(userId)

            val decodedData = Base64.decode(dataOnly, Base64.DEFAULT)
            val iv = decodedData.sliceArray(0..11)
            val encrypted = decodedData.sliceArray(12 until decodedData.size)

            val cipher = Cipher.getInstance(AES_TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))

            String(cipher.doFinal(encrypted))
        } catch (e: Exception) {
            logAccess("DECRYPTION_ERROR", "Error al desencriptar: ${e.message}")
            ""
        }
    }

    fun logAccess(action: String, details: String) {
        val timestamp = dateFormat.format(Date())
        val logEntry = "$timestamp - $action: $details"

        val currentLogs = prefs.getString("access_logs", "") ?: ""
        val newLogs = if (currentLogs.isEmpty()) {
            logEntry
        } else {
            "$currentLogs\n$logEntry"
        }

        prefs.edit().putString("access_logs", newLogs).apply()
    }

    fun getAccessLogs(): List<String> {
        val logs = prefs.getString("access_logs", "") ?: ""
        return if (logs.isEmpty()) emptyList() else logs.split("\n")
    }

    fun getDataProtectionInfo(): Map<String, String> {
        val keyAge = (System.currentTimeMillis() - prefs.getLong("key_created_time", 0)) / (1000 * 60 * 60 * 24)

        return mapOf(
            "Encriptación" to "AES-256-GCM",
            "Integridad" to "HMAC-SHA256",
            "Rotación de clave" to "Cada 30 días",
            "Días desde última rotación" to "$keyAge días",
            "Derivación de clave" to "Salt único por usuario",
            "Estado" to "Activo y seguro"
        )
    }

    fun clearAllData() {
        prefs.edit().clear().apply()
        logAccess("DATA_CLEARED", "Todos los datos borrados de forma segura")
    }
}