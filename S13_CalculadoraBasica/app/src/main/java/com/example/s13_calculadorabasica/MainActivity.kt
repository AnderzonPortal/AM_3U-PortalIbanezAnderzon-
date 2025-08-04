package com.example.s13_calculadorabasica

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.example.s13_calculadorabasica.databinding.ActivityMainBinding
import com.example.s13_calculadorabasica.viewmodel.CalculatorViewModel

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: CalculatorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Observa el cambio en el texto del display
        viewModel.display.observe(this, Observer {
            binding.textDisplay.text = it
        })

        // Configuración de los botones
        val numberButtons = listOf(
            binding.btn0, binding.btn1, binding.btn2, binding.btn3, binding.btn4,
            binding.btn5, binding.btn6, binding.btn7, binding.btn8, binding.btn9, binding.btnDot
        )

        // Asignar funcionalidad a los botones de números y punto
        numberButtons.forEach { button ->
            button.setOnClickListener {
                viewModel.appendSymbol(button.text.toString())
            }
        }

        // Asignar funcionalidad a los botones de operaciones
        binding.btnAdd.setOnClickListener { viewModel.appendSymbol("+") }
        binding.btnSubtract.setOnClickListener { viewModel.appendSymbol("-") }
        binding.btnMultiply.setOnClickListener { viewModel.appendSymbol("*") }
        binding.btnDivide.setOnClickListener { viewModel.appendSymbol("/") }
        binding.btnSqrt.setOnClickListener { viewModel.appendSymbol("√") }
        binding.btnOpenParen.setOnClickListener { viewModel.appendSymbol("(") }
        binding.btnCloseParen.setOnClickListener { viewModel.appendSymbol(")") }

        // Comandos
        binding.btnCE.setOnClickListener { viewModel.clearAll() }
        binding.btnAC.setOnClickListener { viewModel.backspace() }
        binding.btnEquals.setOnClickListener { viewModel.appendSymbol("=") }
    }
}
