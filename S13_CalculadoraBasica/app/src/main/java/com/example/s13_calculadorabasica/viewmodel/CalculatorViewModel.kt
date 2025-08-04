package com.example.s13_calculadorabasica.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlin.math.sqrt

class CalculatorViewModel : ViewModel() {
    private val _display = MutableLiveData<String>("0")
    val display: LiveData<String> = _display

    fun appendSymbol(symbol: String) {
        when (symbol) {
            "=" -> calculate()
            "√" -> handleSquareRoot()
            "(" -> handleOpenParenthesis()
            ")" -> handleCloseParenthesis()
            else -> handleRegularSymbol(symbol)
        }
    }

    private fun handleSquareRoot() {
        val currentValue = _display.value ?: "0"

        // Si el display contiene un solo número, reemplazarlo por la raíz cuadrada
        if (currentValue.isDigitsOnly()) {
            _display.value = "√("
        } else {
            // Si ya hay una operación, añadir "*√("
            if (currentValue != "0") {
                _display.value = currentValue + "*√("
            } else {
                _display.value = "√("
            }
        }
    }

    private fun handleOpenParenthesis() {
        val currentValue = _display.value ?: "0"

        if (currentValue == "0") {
            _display.value = "("
        } else {
            // Si el último carácter es un número, añadir multiplicación implícita
            val lastChar = currentValue.lastOrNull()
            if (lastChar?.isDigit() == true) {
                _display.value = currentValue + "*("
            } else {
                _display.value = currentValue + "("
            }
        }
    }

    private fun handleCloseParenthesis() {
        val currentValue = _display.value ?: "0"

        // Contar paréntesis abiertos y cerrados
        val openCount = currentValue.count { it == '(' }
        val closeCount = currentValue.count { it == ')' }

        // Solo añadir paréntesis de cierre si hay paréntesis abiertos sin cerrar
        if (openCount > closeCount) {
            _display.value = currentValue + ")"
        }
    }

    private fun handleRegularSymbol(symbol: String) {
        val currentValue = _display.value ?: "0"

        // Si el display muestra "0", reemplazarlo con el nuevo símbolo (excepto para operadores)
        if (currentValue == "0" && symbol !in listOf("+", "-", "*", "/", "÷", "×", "−")) {
            _display.value = symbol
        } else {
            _display.value = currentValue + symbol
        }
    }

    fun clearAll() {
        _display.value = "0"
    }

    fun backspace() {
        val currentValue = _display.value ?: "0"
        if (currentValue.length > 1) {
            _display.value = currentValue.dropLast(1)
        } else {
            _display.value = "0"
        }
    }

    fun calculate() {
        try {
            val input = _display.value ?: ""
            if (input.isBlank() || input == "0") return

            // Normalizar símbolos para cálculo
            val normalizedInput = input
                .replace("÷", "/")
                .replace("×", "*")
                .replace("−", "-")

            val result = evaluateExpression(normalizedInput)

            // Formatear el resultado
            _display.value = if (result % 1.0 == 0.0) {
                result.toInt().toString()
            } else {
                String.format("%.8f", result).trimEnd('0').trimEnd('.')
            }
        } catch (e: Exception) {
            _display.value = "Error"
        }
    }

    private fun evaluateExpression(expression: String): Double {
        val sanitizedExpression = expression.replace(" ", "")

        // Primero procesamos las raíces cuadradas
        var processedExpression = sanitizedExpression

        // Manejar √( ... )
        while (processedExpression.contains("√(")) {
            val sqrtIndex = processedExpression.indexOf("√(")
            var parenCount = 1
            var endIndex = sqrtIndex + 2

            // Encontrar el paréntesis de cierre correspondiente
            while (endIndex < processedExpression.length && parenCount > 0) {
                when (processedExpression[endIndex]) {
                    '(' -> parenCount++
                    ')' -> parenCount--
                }
                endIndex++
            }

            if (parenCount == 0) {
                // Extraer y evaluar la expresión dentro del √
                val innerExpression = processedExpression.substring(sqrtIndex + 2, endIndex - 1)
                val innerResult = evaluateBasicExpression(innerExpression)

                if (innerResult < 0) throw ArithmeticException("Raíz cuadrada de número negativo")

                val sqrtResult = sqrt(innerResult)
                processedExpression = processedExpression.substring(0, sqrtIndex) +
                        sqrtResult.toString() +
                        processedExpression.substring(endIndex)
            } else {
                throw ArithmeticException("Paréntesis no balanceados")
            }
        }

        // También manejar √ seguido directamente por números
        val sqrtRegex = Regex("√(\\d+(?:\\.\\d+)?)")
        while (sqrtRegex.containsMatchIn(processedExpression)) {
            processedExpression = sqrtRegex.replace(processedExpression) { matchResult ->
                val number = matchResult.groupValues[1].toDouble()
                if (number < 0) throw ArithmeticException("Raíz cuadrada de número negativo")
                sqrt(number).toString()
            }
        }

        // Ahora evaluamos la expresión matemática restante
        return evaluateBasicExpression(processedExpression)
    }

    private fun evaluateBasicExpression(expression: String): Double {
        if (expression.isEmpty()) return 0.0

        // Verificar paréntesis balanceados
        var parenCount = 0
        for (char in expression) {
            when (char) {
                '(' -> parenCount++
                ')' -> parenCount--
            }
            if (parenCount < 0) throw ArithmeticException("Paréntesis no balanceados")
        }
        if (parenCount != 0) throw ArithmeticException("Paréntesis no balanceados")

        // Resolver paréntesis más internos primero
        var processedExpression = expression
        while (processedExpression.contains("(")) {
            val lastOpen = processedExpression.lastIndexOf("(")
            val firstClose = processedExpression.indexOf(")", lastOpen)

            if (firstClose == -1) throw ArithmeticException("Paréntesis no balanceados")

            val innerExpression = processedExpression.substring(lastOpen + 1, firstClose)
            val innerResult = evaluateWithoutParentheses(innerExpression)

            processedExpression = processedExpression.substring(0, lastOpen) +
                    innerResult.toString() +
                    processedExpression.substring(firstClose + 1)
        }

        return evaluateWithoutParentheses(processedExpression)
    }

    private fun evaluateWithoutParentheses(expression: String): Double {
        if (expression.isEmpty()) return 0.0

        var expr = expression

        // Manejar números negativos al inicio
        var isNegative = false
        if (expr.startsWith("-")) {
            isNegative = true
            expr = expr.substring(1)
        }

        // Evaluamos multiplicaciones y divisiones primero
        expr = evaluateMultiplicationAndDivision(expr)

        // Luego evaluamos sumas y restas
        val result = evaluateAdditionAndSubtraction(expr)

        return if (isNegative) -result else result
    }

    private fun evaluateMultiplicationAndDivision(expression: String): String {
        var expr = expression

        // Procesamos multiplicaciones y divisiones de izquierda a derecha
        while (expr.contains("*") || expr.contains("/")) {
            val multIndex = expr.indexOf("*")
            val divIndex = expr.indexOf("/")

            val operatorIndex = when {
                multIndex == -1 -> divIndex
                divIndex == -1 -> multIndex
                multIndex < divIndex -> multIndex
                else -> divIndex
            }

            val operator = expr[operatorIndex]

            // Encontrar el número a la izquierda
            var leftStart = operatorIndex - 1
            while (leftStart > 0 && (expr[leftStart - 1].isDigit() || expr[leftStart - 1] == '.')) {
                leftStart--
            }

            // Encontrar el número a la derecha
            var rightEnd = operatorIndex + 1
            while (rightEnd < expr.length && (expr[rightEnd].isDigit() || expr[rightEnd] == '.')) {
                rightEnd++
            }

            val leftNum = expr.substring(leftStart, operatorIndex).toDouble()
            val rightNum = expr.substring(operatorIndex + 1, rightEnd).toDouble()

            val result = when (operator) {
                '*' -> leftNum * rightNum
                '/' -> {
                    if (rightNum == 0.0) throw ArithmeticException("División por cero")
                    leftNum / rightNum
                }
                else -> throw ArithmeticException("Operador inválido")
            }

            expr = expr.substring(0, leftStart) + result.toString() + expr.substring(rightEnd)
        }

        return expr
    }

    private fun evaluateAdditionAndSubtraction(expression: String): Double {
        var result = 0.0
        var currentNumber = ""
        var operator = '+'

        for (i in expression.indices) {
            val char = expression[i]

            if (char.isDigit() || char == '.') {
                currentNumber += char
            } else if (char == '+' || char == '-') {
                if (currentNumber.isNotEmpty()) {
                    val num = currentNumber.toDouble()
                    result = when (operator) {
                        '+' -> result + num
                        '-' -> result - num
                        else -> num
                    }
                    currentNumber = ""
                }
                operator = char
            }
        }

        // Procesar el último número
        if (currentNumber.isNotEmpty()) {
            val num = currentNumber.toDouble()
            result = when (operator) {
                '+' -> result + num
                '-' -> result - num
                else -> num
            }
        }

        return result
    }
}

// Función de extensión para verificar si una cadena contiene solo dígitos
fun String.isDigitsOnly(): Boolean {
    return this.all { it.isDigit() }
}