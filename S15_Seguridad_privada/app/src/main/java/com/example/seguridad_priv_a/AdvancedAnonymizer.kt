package com.example.seguridad_priv_a

import android.content.Context
import android.content.SharedPreferences
import kotlin.math.*
import kotlin.random.Random
import java.text.SimpleDateFormat
import java.util.*
import java.security.MessageDigest

// 3.3 Framework de Anonimización Avanzado
class AdvancedAnonymizer(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("anonymizer_config", Context.MODE_PRIVATE)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // Data classes para el framework
    data class PersonalData(
        val id: String,
        val age: Int,
        val zipCode: String,
        val gender: String,
        val salary: Double,
        val disease: String,
        val sensitiveAttributes: Map<String, Any> = emptyMap()
    )

    data class AnonymizedData(
        val id: String,
        val ageRange: String,
        val zipPrefix: String,
        val gender: String,
        val salaryRange: String,
        val disease: String,
        val anonymityLevel: Int,
        val diversityLevel: Int
    )

    data class NumericData(
        val values: List<Double>,
        val metadata: Map<String, Any> = emptyMap()
    )

    // 3.3.4 Sistema de políticas de retención
    data class RetentionPolicy(
        val dataType: DataType,
        val retentionDays: Int,
        val anonymizationLevel: AnonymizationLevel,
        val autoDelete: Boolean = true
    )

    enum class DataType {
        PERSONAL_IDENTIFIER, QUASI_IDENTIFIER, SENSITIVE_ATTRIBUTE, NUMERIC_DATA, BIOMETRIC_DATA
    }

    enum class AnonymizationLevel {
        NONE, BASIC, MODERATE, STRONG, COMPLETE
    }

    // 3.3.3 Políticas de enmascaramiento por tipo de dato
    data class MaskingPolicy(
        val dataType: DataType,
        val maskingTechnique: MaskingTechnique,
        val preserveFormat: Boolean = true,
        val customPattern: String? = null
    )

    enum class MaskingTechnique {
        HASH, GENERALIZATION, SUPPRESSION, PERTURBATION, SUBSTITUTION, TOKENIZATION
    }

    // 3.3.1 Implementación de k-anonymity y l-diversity
    fun anonymizeWithKAnonymity(data: List<PersonalData>, k: Int): List<AnonymizedData> {
        if (data.isEmpty() || k <= 1) return data.map { convertToAnonymized(it, 1, 1) }

        // Agrupar por quasi-identifiers (age, zipCode, gender)
        val groups = groupByQuasiIdentifiers(data)
        val anonymizedGroups = mutableListOf<List<PersonalData>>()

        for (group in groups) {
            if (group.size >= k) {
                // Grupo cumple k-anonymity
                anonymizedGroups.add(group)
            } else {
                // Generalizar hasta cumplir k-anonymity
                val generalizedGroup = generalizeGroup(group, data, k)
                anonymizedGroups.add(generalizedGroup)
            }
        }

        // Aplicar l-diversity a cada grupo
        return anonymizedGroups.flatMap { group ->
            val lDiversity = calculateLDiversity(group)
            group.map { convertToAnonymized(it, k, lDiversity) }
        }
    }

    private fun groupByQuasiIdentifiers(data: List<PersonalData>): List<List<PersonalData>> {
        return data.groupBy {
            "${getAgeRange(it.age)}_${getZipPrefix(it.zipCode)}_${it.gender}"
        }.values.toList()
    }

    private fun generalizeGroup(smallGroup: List<PersonalData>, allData: List<PersonalData>, k: Int): List<PersonalData> {
        // Encontrar registros similares para completar el grupo
        val needed = k - smallGroup.size
        val similar = findSimilarRecords(smallGroup, allData, needed)

        val combinedGroup = smallGroup + similar

        // Generalizar atributos para hacer el grupo homogéneo
        return combinedGroup.map { record ->
            record.copy(
                age = getGeneralizedAge(combinedGroup.map { it.age }),
                zipCode = getGeneralizedZip(combinedGroup.map { it.zipCode })
            )
        }
    }

    private fun calculateLDiversity(group: List<PersonalData>): Int {
        // L-diversity: número de valores distintos en atributos sensibles
        val distinctDiseases = group.map { it.disease }.distinct().size
        return maxOf(1, distinctDiseases)
    }

    // 3.3.2 Differential Privacy para datos numéricos
    fun applyDifferentialPrivacy(data: NumericData, epsilon: Double): NumericData {
        val sensitivity = calculateSensitivity(data.values)
        val scale = sensitivity / epsilon

        val noisyValues = data.values.map { value ->
            value + generateLaplaceNoise(scale)
        }

        return NumericData(
            values = noisyValues,
            metadata = data.metadata + mapOf(
                "epsilon" to epsilon,
                "noise_scale" to scale,
                "privacy_budget_used" to epsilon
            )
        )
    }

    private fun generateLaplaceNoise(scale: Double): Double {
        val u1 = Random.nextDouble()
        val u2 = Random.nextDouble()

        return if (u1 <= 0.5) {
            scale * ln(2 * u1)
        } else {
            -scale * ln(2 * (1 - u1))
        }
    }

    private fun calculateSensitivity(values: List<Double>): Double {
        // Sensibilidad global: máxima diferencia posible
        return if (values.isEmpty()) 1.0 else {
            val max = values.maxOrNull() ?: 0.0
            val min = values.minOrNull() ?: 0.0
            maxOf(1.0, max - min)
        }
    }

    // 3.3.3 Técnicas de enmascaramiento por tipo de dato
    fun maskByDataType(data: Any, maskingPolicy: MaskingPolicy): Any {
        return when (maskingPolicy.maskingTechnique) {
            MaskingTechnique.HASH -> hashData(data)
            MaskingTechnique.GENERALIZATION -> generalizeData(data, maskingPolicy.dataType)
            MaskingTechnique.SUPPRESSION -> suppressData(data, maskingPolicy.dataType)
            MaskingTechnique.PERTURBATION -> perturbData(data)
            MaskingTechnique.SUBSTITUTION -> substituteData(data, maskingPolicy.dataType)
            MaskingTechnique.TOKENIZATION -> tokenizeData(data)
        }
    }

    private fun hashData(data: Any): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(data.toString().toByteArray())
        return hash.joinToString("") { "%02x".format(it) }.take(8)
    }

    private fun generalizeData(data: Any, dataType: DataType): Any {
        return when (dataType) {
            DataType.PERSONAL_IDENTIFIER -> when (data) {
                is String -> if (data.matches(Regex("\\d+"))) getAgeRange(data.toIntOrNull() ?: 0) else data.take(1) + "***"
                is Int -> getAgeRange(data)
                else -> "GENERALIZED"
            }
            DataType.QUASI_IDENTIFIER -> when (data) {
                is String -> if (data.length >= 5) data.take(3) + "**" else data
                else -> data
            }
            DataType.NUMERIC_DATA -> when (data) {
                is Double -> roundToRange(data, 1000.0)
                is Int -> (data / 10) * 10
                else -> data
            }
            else -> data
        }
    }

    private fun suppressData(data: Any, dataType: DataType): Any {
        return when (dataType) {
            DataType.SENSITIVE_ATTRIBUTE -> "***SUPPRESSED***"
            DataType.PERSONAL_IDENTIFIER -> "*****"
            else -> data
        }
    }


    private fun perturbData(data: Any): Any {
        return when (data) {
            is Double -> {
                // Generar ruido gaussiano manualmente usando Box-Muller transform
                val u1 = Random.nextDouble()
                val u2 = Random.nextDouble()
                val gaussianNoise = sqrt(-2.0 * ln(u1)) * cos(2.0 * PI * u2)
                data + gaussianNoise * (data * 0.05) // 5% noise
            }
            is Int -> data + Random.nextInt(-2, 3) // ±2 variation
            is String -> if (data.matches(Regex("\\d+"))) {
                (data.toIntOrNull()?.plus(Random.nextInt(-1, 2)) ?: data).toString()
            } else data
            else -> data
        }
    }

    private fun substituteData(data: Any, dataType: DataType): Any {
        return when (dataType) {
            DataType.PERSONAL_IDENTIFIER -> generateSubstitute(data)
            DataType.QUASI_IDENTIFIER -> generateQuasiSubstitute(data)
            else -> data
        }
    }

    private fun tokenizeData(data: Any): String {
        val token = "TOK_${Random.nextInt(100000, 999999)}"
        // En producción, almacenar mapping token->data de forma segura
        storeTokenMapping(token, data.toString())
        return token
    }

    // 3.3.4 Sistema de políticas de retención configurables
    fun applyRetentionPolicy(data: Any, policy: RetentionPolicy): Any? {
        val creationTime = getDataCreationTime(data)
        val currentTime = System.currentTimeMillis()
        val retentionMs = policy.retentionDays * 24 * 60 * 60 * 1000L

        return if (currentTime - creationTime > retentionMs) {
            if (policy.autoDelete) {
                null // Data should be deleted
            } else {
                // Apply anonymization based on level
                applyAnonymizationLevel(data, policy.anonymizationLevel)
            }
        } else {
            data // Data is within retention period
        }
    }

    private fun applyAnonymizationLevel(data: Any, level: AnonymizationLevel): Any {
        return when (level) {
            AnonymizationLevel.NONE -> data
            AnonymizationLevel.BASIC -> maskByDataType(data, MaskingPolicy(DataType.PERSONAL_IDENTIFIER, MaskingTechnique.GENERALIZATION))
            AnonymizationLevel.MODERATE -> maskByDataType(data, MaskingPolicy(DataType.QUASI_IDENTIFIER, MaskingTechnique.HASH))
            AnonymizationLevel.STRONG -> maskByDataType(data, MaskingPolicy(DataType.SENSITIVE_ATTRIBUTE, MaskingTechnique.SUPPRESSION))
            AnonymizationLevel.COMPLETE -> "***ANONYMIZED***"
        }
    }

    fun configureRetentionPolicies(policies: List<RetentionPolicy>) {
        val policiesJson = policies.map { policy ->
            "${policy.dataType.name}:${policy.retentionDays}:${policy.anonymizationLevel.name}:${policy.autoDelete}"
        }.joinToString(";")

        prefs.edit().putString("retention_policies", policiesJson).apply()
    }

    fun getConfiguredPolicies(): List<RetentionPolicy> {
        val policiesStr = prefs.getString("retention_policies", "") ?: ""
        if (policiesStr.isEmpty()) return getDefaultPolicies()

        return policiesStr.split(";").mapNotNull { policyStr ->
            val parts = policyStr.split(":")
            if (parts.size >= 4) {
                RetentionPolicy(
                    DataType.valueOf(parts[0]),
                    parts[1].toIntOrNull() ?: 30,
                    AnonymizationLevel.valueOf(parts[2]),
                    parts[3].toBoolean()
                )
            } else null
        }
    }

    // Métodos auxiliares
    private fun convertToAnonymized(data: PersonalData, k: Int, l: Int): AnonymizedData {
        return AnonymizedData(
            id = hashData(data.id),
            ageRange = getAgeRange(data.age),
            zipPrefix = getZipPrefix(data.zipCode),
            gender = data.gender,
            salaryRange = getSalaryRange(data.salary),
            disease = data.disease,
            anonymityLevel = k,
            diversityLevel = l
        )
    }

    private fun getAgeRange(age: Int): String {
        return when {
            age < 18 -> "<18"
            age < 30 -> "18-29"
            age < 50 -> "30-49"
            age < 65 -> "50-64"
            else -> "65+"
        }
    }

    private fun getGeneralizedAge(ages: List<Int>): Int {
        return ages.average().toInt()
    }

    private fun getZipPrefix(zipCode: String): String {
        return if (zipCode.length >= 3) zipCode.take(3) + "**" else zipCode
    }

    private fun getGeneralizedZip(zipCodes: List<String>): String {
        val commonPrefix = zipCodes.reduce { acc, zip ->
            acc.zip(zip).takeWhile { (a, b) -> a == b }.map { it.first }.joinToString("")
        }
        return if (commonPrefix.length >= 2) commonPrefix + "*".repeat(5 - commonPrefix.length) else "****"
    }

    private fun getSalaryRange(salary: Double): String {
        return when {
            salary < 30000 -> "<30K"
            salary < 50000 -> "30K-50K"
            salary < 75000 -> "50K-75K"
            salary < 100000 -> "75K-100K"
            else -> "100K+"
        }
    }

    private fun roundToRange(value: Double, rangeSize: Double): Double {
        return (value / rangeSize).toInt() * rangeSize
    }

    private fun findSimilarRecords(group: List<PersonalData>, allData: List<PersonalData>, needed: Int): List<PersonalData> {
        val remaining = allData - group.toSet()
        return remaining.sortedBy { candidate ->
            group.minOf { existing ->
                calculateSimilarity(candidate, existing)
            }
        }.take(needed)
    }

    private fun calculateSimilarity(a: PersonalData, b: PersonalData): Double {
        val ageDiff = abs(a.age - b.age) / 100.0
        val zipSim = if (a.zipCode.take(2) == b.zipCode.take(2)) 0.0 else 1.0
        val genderSim = if (a.gender == b.gender) 0.0 else 1.0
        return ageDiff + zipSim + genderSim
    }

    private fun generateSubstitute(data: Any): String {
        return when (data.toString().length) {
            in 1..5 -> "SUB${Random.nextInt(10, 99)}"
            in 6..10 -> "SUBSTITUTE${Random.nextInt(100, 999)}"
            else -> "SUBSTITUTE_${Random.nextInt(1000, 9999)}"
        }
    }

    private fun generateQuasiSubstitute(data: Any): Any {
        return when (data) {
            is String -> if (data.matches(Regex("\\d{5}"))) {
                "${Random.nextInt(10, 99)}${data.takeLast(3)}"
            } else data.take(1) + "***"
            else -> data
        }
    }

    private fun storeTokenMapping(token: String, originalData: String) {
        // En producción: usar almacenamiento seguro con encriptación
        prefs.edit().putString("token_$token", originalData).apply()
    }

    private fun getDataCreationTime(data: Any): Long {
        // En producción: obtener timestamp real de creación
        return prefs.getLong("creation_time_${data.hashCode()}", System.currentTimeMillis())
    }

    private fun getDefaultPolicies(): List<RetentionPolicy> {
        return listOf(
            RetentionPolicy(DataType.PERSONAL_IDENTIFIER, 30, AnonymizationLevel.STRONG),
            RetentionPolicy(DataType.QUASI_IDENTIFIER, 90, AnonymizationLevel.MODERATE),
            RetentionPolicy(DataType.SENSITIVE_ATTRIBUTE, 7, AnonymizationLevel.COMPLETE),
            RetentionPolicy(DataType.NUMERIC_DATA, 365, AnonymizationLevel.BASIC),
            RetentionPolicy(DataType.BIOMETRIC_DATA, 1, AnonymizationLevel.COMPLETE)
        )
    }

    // API pública para obtener estadísticas de anonimización
    fun getAnonymizationStats(): Map<String, Any> {
        return mapOf(
            "configured_policies" to getConfiguredPolicies().size,
            "total_anonymizations" to prefs.getInt("total_anonymizations", 0),
            "k_anonymity_level" to prefs.getInt("default_k", 3),
            "l_diversity_level" to prefs.getInt("default_l", 2),
            "differential_privacy_budget" to prefs.getFloat("privacy_budget", 1.0f)
        )
    }
}