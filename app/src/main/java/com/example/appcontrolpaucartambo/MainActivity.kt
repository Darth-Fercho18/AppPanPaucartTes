package com.example.appcontrolpaucartambo

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var tvTotalGeneralSoles: TextView
    private lateinit var tvTotalGeneralDolares: TextView
    private lateinit var layoutSaldosPorBanco: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvTotalGeneralSoles = findViewById(R.id.tvTotalGeneralSoles)
        tvTotalGeneralDolares = findViewById(R.id.tvTotalGeneralDolares)
        layoutSaldosPorBanco = findViewById(R.id.layoutSaldosPorBanco)

        // Vinculación de botones con la nueva UI
        val btnAsistencia = findViewById<Button>(R.id.btnAsistencia)
        val btnRegistrarCobro = findViewById<Button>(R.id.btnRegistrarCobro)
        val btnAsignarMulta = findViewById<Button>(R.id.btnAsignarMulta)
        val btnListarSocios = findViewById<Button>(R.id.btnListarSocios)
        val btnMovimientos = findViewById<Button>(R.id.btnMovimientos)

        // Módulo de Asistencia
        btnAsistencia.setOnClickListener {
            startActivity(Intent(this, AsistenciaActivity::class.java))
        }

        // Padrón de Socios
        btnListarSocios.setOnClickListener {
            startActivity(Intent(this, ListarActivity::class.java))
        }

        // Módulo de Cobros
        btnRegistrarCobro.setOnClickListener {
            val intent = Intent(this, CobrosActivity::class.java)
            startActivity(intent)
        }

        // Módulo de Ingresos y Egresos
        btnMovimientos.setOnClickListener {
            val intent = Intent(this, ListarMovimientosActivity::class.java)
            startActivity(intent)
        }

        // Módulo de Cuotas Extraordinarias
        btnAsignarMulta.setOnClickListener {
            val intent = Intent(this, CuotasExtraordinariasActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        cargarResumenFinanciero()
    }

    private fun obtenerInfoCuenta(cuenta: CuentaBancaria): Triple<String, Int, Boolean> {
        val rawNombre = (cuenta.nombreCuenta ?: cuenta.nombre ?: "").uppercase(Locale.ROOT)
        val id = cuenta.idCuenta
        val moneda = (cuenta.moneda ?: "").uppercase(Locale.ROOT)
        val esDolar = id == 4 || moneda.contains("USD") || rawNombre.contains("DOLAR") || rawNombre.contains("DÓLAR")

        return when {
            id == 1 || rawNombre.contains("EFECTIVO") || rawNombre.contains("CAJA CHICA") -> Triple("EFECTIVO", 1, false)
            id == 2 || (rawNombre.contains("BCP") && !esDolar) -> Triple("BCP", 2, false)
            id == 3 || (rawNombre.contains("INTERBANK") && !esDolar) -> Triple("INTERBANK", 3, false)
            id == 4 || esDolar -> Triple("DOLARES", 4, true)
            else -> Triple(cuenta.nombreCuenta ?: cuenta.nombre ?: "Cuenta $id", 5, esDolar)
        }
    }

    private fun cargarResumenFinanciero() {
        val apiService = RetrofitClient.getApiService()
        apiService.obtenerCuentas().enqueue(object : Callback<List<CuentaBancaria>> {
            override fun onResponse(call: Call<List<CuentaBancaria>>, response: Response<List<CuentaBancaria>>) {
                if (response.isSuccessful && response.body() != null) {
                    val cuentas = response.body()!!

                    var totalSoles = 0.0
                    var totalDolares = 0.0

                    layoutSaldosPorBanco.removeAllViews()

                    // Ordenar cuentas: 1: EFECTIVO, 2: BCP, 3: INTERBANK, 4: DOLARES (al final)
                    val cuentasOrdenadas = cuentas.sortedBy { cuenta ->
                        obtenerInfoCuenta(cuenta).second
                    }

                    for (cuenta in cuentasOrdenadas) {
                        val saldo = cuenta.saldoActual ?: 0.0
                        val (nombreMostrar, _, esDolar) = obtenerInfoCuenta(cuenta)

                        if (esDolar) {
                            totalDolares += saldo
                        } else {
                            totalSoles += saldo
                        }

                        val rowLayout = LinearLayout(this@MainActivity).apply {
                            orientation = LinearLayout.HORIZONTAL
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply {
                                setMargins(0, 6, 0, 6)
                            }
                            setPadding(12, 10, 12, 10)
                            gravity = Gravity.CENTER_VERTICAL
                        }

                        val tvBanco = TextView(this@MainActivity).apply {
                            text = nombreMostrar
                            setTextColor(resources.getColor(R.color.text_primary, theme))
                            textSize = 14f
                            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                        }

                        val simbolo = if (esDolar) "$" else "S/"
                        val tvMonto = TextView(this@MainActivity).apply {
                            text = "$simbolo ${String.format(Locale.getDefault(), "%.2f", saldo)}"
                            setTextColor(resources.getColor(R.color.primary_green, theme))
                            textSize = 14f
                            setTypeface(null, android.graphics.Typeface.BOLD)
                            gravity = Gravity.END
                        }

                        rowLayout.addView(tvBanco)
                        rowLayout.addView(tvMonto)
                        layoutSaldosPorBanco.addView(rowLayout)
                    }

                    tvTotalGeneralSoles.text = String.format(Locale.getDefault(), "S/ %.2f", totalSoles)
                    tvTotalGeneralDolares.text = String.format(Locale.getDefault(), "$ %.2f", totalDolares)
                }
            }

            override fun onFailure(call: Call<List<CuentaBancaria>>, t: Throwable) {
                Toast.makeText(this@MainActivity, "Error al cargar cuentas bancarias", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
