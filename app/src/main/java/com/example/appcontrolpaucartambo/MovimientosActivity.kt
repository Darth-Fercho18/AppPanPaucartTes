package com.example.appcontrolpaucartambo

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MovimientosActivity : AppCompatActivity() {

    private lateinit var spinnerTipoMovimiento: Spinner
    private lateinit var spinnerCuentaBancaria: Spinner
    private lateinit var etCategoria: TextInputEditText
    private lateinit var etConcepto: TextInputEditText
    private lateinit var tilMonto: TextInputLayout
    private lateinit var etMonto: TextInputEditText
    private lateinit var actvAsociado: AutoCompleteTextView
    private lateinit var btnRegistrarMovimiento: Button

    private var listaSocios: List<Asociado> = emptyList()
    private var idAsociadoSeleccionado: Int? = null

    private val cuentasPorDefecto = listOf(
        CuentaBancaria(1, "Efectivo"),
        CuentaBancaria(2, "BCP"),
        CuentaBancaria(3, "Interbank"),
        CuentaBancaria(4, "Dólares")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_movimientos)

        spinnerTipoMovimiento = findViewById(R.id.spinnerTipoMovimiento)
        spinnerCuentaBancaria = findViewById(R.id.spinnerCuentaBancaria)
        etCategoria = findViewById(R.id.etCategoria)
        etConcepto = findViewById(R.id.etConcepto)
        tilMonto = findViewById(R.id.tilMonto)
        etMonto = findViewById(R.id.etMonto)
        actvAsociado = findViewById(R.id.actvAsociado)
        btnRegistrarMovimiento = findViewById(R.id.btnRegistrarMovimiento)

        // Configurar Spinner de Tipo de Movimiento
        val tipos = listOf("INGRESO", "EGRESO")
        val tipoAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, tipos)
        tipoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerTipoMovimiento.adapter = tipoAdapter

        // Configurar Spinner de Cuenta Bancaria inicialmente con cuentas por defecto
        configurarSpinnerCuentas(cuentasPorDefecto)

        // Cargar cuentas desde API si están disponibles
        cargarCuentasBancarias()

        // Cambiar la etiqueta del monto según la moneda/cuenta seleccionada
        spinnerCuentaBancaria.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val cuenta = parent?.getItemAtPosition(position) as? CuentaBancaria
                val esDolares = cuenta?.idCuenta == 4 ||
                        cuenta?.nombreCuenta?.contains("Dólar", ignoreCase = true) == true ||
                        cuenta?.nombre?.contains("Dólar", ignoreCase = true) == true

                tilMonto.hint = if (esDolares) "Monto ($)" else "Monto (S/)"
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Cargar socios para autocompletar
        cargarSociosParaBuscador()

        btnRegistrarMovimiento.setOnClickListener {
            ejecutarRegistroMovimiento()
        }
    }

    private fun configurarSpinnerCuentas(listaCuentas: List<CuentaBancaria>) {
        val cuentaAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, listaCuentas)
        cuentaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCuentaBancaria.adapter = cuentaAdapter
    }

    private fun cargarCuentasBancarias() {
        val apiService = RetrofitClient.getApiService()
        apiService.obtenerCuentas().enqueue(object : Callback<List<CuentaBancaria>> {
            override fun onResponse(call: Call<List<CuentaBancaria>>, response: Response<List<CuentaBancaria>>) {
                if (response.isSuccessful && response.body() != null && response.body()!!.isNotEmpty()) {
                    var lista = response.body()!!
                    if (lista.none { it.idCuenta == 4 }) {
                        lista = lista + CuentaBancaria(4, "Dólares")
                    }
                    configurarSpinnerCuentas(lista)
                }
            }

            override fun onFailure(call: Call<List<CuentaBancaria>>, t: Throwable) {
                // Mantiene cuentas por defecto
            }
        })
    }

    private fun cargarSociosParaBuscador() {
        val apiService = RetrofitClient.getApiService()
        apiService.getAsociados().enqueue(object : Callback<List<Asociado>> {
            override fun onResponse(call: Call<List<Asociado>>, response: Response<List<Asociado>>) {
                if (response.isSuccessful && response.body() != null) {
                    listaSocios = response.body()!!
                    val listaNombres = listaSocios.map { "${it.nombres} ${it.apellidoPaterno} ${it.apellidoMaterno}" }

                    val adapter = ArrayAdapter(
                        this@MovimientosActivity,
                        android.R.layout.simple_dropdown_item_1line,
                        listaNombres
                    )
                    actvAsociado.setAdapter(adapter)

                    actvAsociado.setOnItemClickListener { _, _, position, _ ->
                        val itemSeleccionado = adapter.getItem(position)
                        val socio = listaSocios.find {
                            "${it.nombres} ${it.apellidoPaterno} ${it.apellidoMaterno}" == itemSeleccionado
                        }
                        idAsociadoSeleccionado = socio?.idAsociado
                    }
                }
            }

            override fun onFailure(call: Call<List<Asociado>>, t: Throwable) {
                // Silencioso o Toast opcional
            }
        })
    }

    private fun ejecutarRegistroMovimiento() {
        val tipoMovimiento = spinnerTipoMovimiento.selectedItem.toString()
        val cuentaSeleccionada = spinnerCuentaBancaria.selectedItem as CuentaBancaria
        val idCuenta = cuentaSeleccionada.idCuenta

        // Formato para enviar al API en formato YYYY-MM-DD (fecha del sistema)
        val formatoApi = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val fechaMovimiento = formatoApi.format(Date())

        val categoriaStr = etCategoria.text.toString().trim()
        val categoria = if (categoriaStr.isEmpty()) "GENERAL" else categoriaStr

        val concepto = etConcepto.text.toString().trim()
        val montoStr = etMonto.text.toString().trim()
        val monto = montoStr.toDoubleOrNull() ?: 0.0

        val textoAsociado = actvAsociado.text.toString().trim()
        val socioEncontrado = listaSocios.find {
            "${it.nombres} ${it.apellidoPaterno} ${it.apellidoMaterno}" == textoAsociado
        }
        val idAsociadoFinal = socioEncontrado?.idAsociado ?: idAsociadoSeleccionado

        if (monto <= 0) {
            Toast.makeText(this, "El monto debe ser mayor a cero", Toast.LENGTH_SHORT).show()
            etMonto.requestFocus()
            return
        }

        btnRegistrarMovimiento.isEnabled = false

        val apiService = RetrofitClient.getApiService()
        apiService.registrarMovimiento(
            idCuenta = idCuenta,
            tipoMovimiento = tipoMovimiento,
            categoria = categoria,
            concepto = if (concepto.isEmpty()) null else concepto,
            monto = monto,
            numComprobante = null,
            idAsociado = idAsociadoFinal,
            fechaMovimiento = fechaMovimiento
        ).enqueue(object : Callback<RespuestaMovimiento> {
            override fun onResponse(call: Call<RespuestaMovimiento>, response: Response<RespuestaMovimiento>) {
                btnRegistrarMovimiento.isEnabled = true
                if (response.isSuccessful && response.body() != null) {
                    val res = response.body()!!
                    if (res.status == "success") {
                        Toast.makeText(this@MovimientosActivity, "¡Movimiento registrado correctamente!", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this@MovimientosActivity, res.error ?: "Error al registrar movimiento", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(this@MovimientosActivity, "Error en la respuesta del servidor", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<RespuestaMovimiento>, t: Throwable) {
                btnRegistrarMovimiento.isEnabled = true
                Toast.makeText(this@MovimientosActivity, "Error de red: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
