package com.example.appcontrolpaucartambo

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnListar = findViewById<Button>(R.id.btnListar)
        val btnAsistencia = findViewById<Button>(R.id.btnAsistencia)
        val btnIngresosEgresos = findViewById<Button>(R.id.btnIngresosEgresos)
        val btnPlaceholder = findViewById<Button>(R.id.btnPlaceholder)

        btnListar.setOnClickListener {
            startActivity(Intent(this, ListarActivity::class.java))
        }

        btnAsistencia.setOnClickListener {
            startActivity(Intent(this, AsistenciaActivity::class.java))
        }

        btnIngresosEgresos.setOnClickListener {
            Toast.makeText(this, "Módulo de Ingresos y Egresos en desarrollo", Toast.LENGTH_SHORT).show()
        }

        btnPlaceholder.setOnClickListener {
            Toast.makeText(this, "Opción en desarrollo", Toast.LENGTH_SHORT).show()
        }
    }
}