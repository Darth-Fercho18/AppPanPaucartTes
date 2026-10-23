package com.example.appcontrolpaucartambo

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale

class CobrosActivity : AppCompatActivity() {

    private lateinit var actvBuscarSocio: AutoCompleteTextView
    private lateinit var rvDeudasPendientes: RecyclerView
    private lateinit var tvSinDeudas: TextView
    private lateinit var tvMontoTotalCobro: TextView
    private lateinit var spCuentaGeneral: Spinner
    private lateinit var btnProcesarPago: Button

    private var listaSocios: List<Asociado> = emptyList()
    private var listaCuentas: List<CuentaBancaria> = emptyList()
    private var socioSeleccionado: Asociado? = null
    private var deudasObtenidas: MutableList<DeudaPendiente> = mutableListOf()
    private var totalPagar: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cobros)

        actvBuscarSocio = findViewById(R.id.actvBuscarSocio)
        rvDeudasPendientes = findViewById(R.id.rvDeudasPendientes)
        tvSinDeudas = findViewById(R.id.tvSinDeudas)
        tvMontoTotalCobro = findViewById(R.id.tvMontoTotalCobro)
        spCuentaGeneral = findViewById(R.id.spCuentaGeneral)
        btnProcesarPago = findViewById(R.id.btnProcesarPago)

        rvDeudasPendientes.layoutManager = LinearLayoutManager(this)

        cargarCuentasBancarias()

        val idAsociadoDirecto = intent.getIntExtra("id_asociado_directo", -1)

        if (idAsociadoDirecto != -1) {
            actvBuscarSocio.isEnabled = false
            cargarSociosParaBuscador(idAsociadoDirecto)
        } else {
            cargarSociosParaBuscador(-1)
        }

        btnProcesarPago.setOnClickListener {
            procesarPagoBD()
        }
    }

    private fun cargarCuentasBancarias() {
        val apiService = RetrofitClient.getApiService()
        apiService.obtenerCuentas().enqueue(object : Callback<List<CuentaBancaria>> {
            override fun onResponse(call: Call<List<CuentaBancaria>>, response: Response<List<CuentaBancaria>>) {
                if (response.isSuccessful && response.body() != null && response.body()!!.isNotEmpty()) {
                    var cuentas = response.body()!!
                    if (cuentas.none { it.idCuenta == 4 }) {
                        cuentas = cuentas + CuentaBancaria(4, "Dólares")
                    }
                    listaCuentas = cuentas
                } else {
                    listaCuentas = listOf(
                        CuentaBancaria(1, "Efectivo"),
                        CuentaBancaria(2, "BCP"),
                        CuentaBancaria(3, "Interbank"),
                        CuentaBancaria(4, "Dólares")
                    )
                }
                val spinnerAdapter = ArrayAdapter(this@CobrosActivity, android.R.layout.simple_spinner_item, listaCuentas)
                spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spCuentaGeneral.adapter = spinnerAdapter
            }

            override fun onFailure(call: Call<List<CuentaBancaria>>, t: Throwable) {
                listaCuentas = listOf(
                    CuentaBancaria(1, "Efectivo"),
                    CuentaBancaria(2, "BCP"),
                    CuentaBancaria(3, "Interbank"),
                    CuentaBancaria(4, "Dólares")
                )
                val spinnerAdapter = ArrayAdapter(this@CobrosActivity, android.R.layout.simple_spinner_item, listaCuentas)
                spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spCuentaGeneral.adapter = spinnerAdapter
            }
        })
    }

    private fun cargarSociosParaBuscador(idAsociadoDirecto: Int) {
        val apiService = RetrofitClient.getApiService()
        apiService.getAsociados().enqueue(object : Callback<List<Asociado>> {
            override fun onResponse(call: Call<List<Asociado>>, response: Response<List<Asociado>>) {
                if (response.isSuccessful && response.body() != null) {
                    listaSocios = response.body()!!

                    val listaNombres = listaSocios.map { "${it.nombres} ${it.apellidoPaterno} ${it.apellidoMaterno}" }

                    val adapter = ArrayAdapter(
                        this@CobrosActivity,
                        android.R.layout.simple_dropdown_item_1line,
                        listaNombres
                    )
                    actvBuscarSocio.setAdapter(adapter)

                    if (idAsociadoDirecto != -1) {
                        socioSeleccionado = listaSocios.find { it.idAsociado == idAsociadoDirecto }
                        socioSeleccionado?.let {
                            actvBuscarSocio.setText("${it.nombres} ${it.apellidoPaterno} ${it.apellidoMaterno}", false)
                        }
                        consultarDeudasSocio(idAsociadoDirecto)
                    }

                    actvBuscarSocio.setOnItemClickListener { _, _, position, _ ->
                        val itemSeleccionado = adapter.getItem(position)
                        socioSeleccionado = listaSocios.find {
                            "${it.nombres} ${it.apellidoPaterno} ${it.apellidoMaterno}" == itemSeleccionado
                        }

                        socioSeleccionado?.let { consultarDeudasSocio(it.idAsociado) }
                    }
                }
            }

            override fun onFailure(call: Call<List<Asociado>>, t: Throwable) {
                Toast.makeText(this@CobrosActivity, "Error al cargar integrantes", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun consultarDeudasSocio(idAsociado: Int) {
        tvSinDeudas.text = "Cargando deudas..."
        tvSinDeudas.visibility = View.VISIBLE
        rvDeudasPendientes.visibility = View.GONE
        btnProcesarPago.isEnabled = false
        totalPagar = 0.0
        tvMontoTotalCobro.text = "S/ 0.00"

        val apiService = RetrofitClient.getApiService()
        apiService.obtenerDeudasSocio(idAsociado).enqueue(object : Callback<List<DeudaPendiente>> {
            override fun onResponse(call: Call<List<DeudaPendiente>>, response: Response<List<DeudaPendiente>>) {
                if (response.isSuccessful && response.body() != null) {
                    deudasObtenidas = response.body()!!.filter { it.estado?.uppercase() != "PAGADO" }.toMutableList()

                    if (deudasObtenidas.isEmpty()) {
                        tvSinDeudas.text = "El integrante no tiene deudas pendientes."
                        tvSinDeudas.visibility = View.VISIBLE
                        rvDeudasPendientes.visibility = View.GONE
                    } else {
                        tvSinDeudas.visibility = View.GONE
                        rvDeudasPendientes.visibility = View.VISIBLE
                        mostrarListaDeudas()
                    }
                } else {
                    tvSinDeudas.text = "Error al consultar deudas."
                }
            }

            override fun onFailure(call: Call<List<DeudaPendiente>>, t: Throwable) {
                tvSinDeudas.text = "Error de red al traer deudas."
            }
        })
    }

    private fun mostrarListaDeudas() {
        val listaConHeaders = prepararListaConHeaders(deudasObtenidas)
        val adapter = DeudasAdapter(
            listaItems = listaConHeaders,
            onDeudaCheckChanged = { _ ->
                recalcularTotal()
            },
            onItemClick = { cuota ->
                mostrarDialogoDetalleDeuda(cuota)
            }
        )
        rvDeudasPendientes.adapter = adapter
    }

    private fun mostrarDialogoDetalleDeuda(cuota: DeudaPendiente) {
        val nombreSocio = socioSeleccionado?.let { "${it.nombres} ${it.apellidoPaterno} ${it.apellidoMaterno}" } ?: "Integrante"
        val nombreActividad = cuota.nombreActividad ?: "N/A"

        val mensaje = "Integrante: $nombreSocio\n" +
                "Actividad: $nombreActividad\n" +
                "Concepto: ${cuota.concepto}\n" +
                "Monto Original: S/ ${cuota.montoOriginal ?: cuota.monto}\n" +
                "Descuento: S/ ${cuota.descuento ?: 0.0}\n" +
                "Monto Total: S/ ${cuota.montoTotal ?: cuota.monto}\n" +
                "Monto Pagado: S/ ${cuota.montoPagado ?: 0.0}\n" +
                "Saldo Pendiente: S/ ${cuota.monto}\n" +
                "Estado: ${cuota.estado ?: "PENDIENTE"}"

        val titulo = when (cuota.idTipoCuota) {
            3 -> "🔴 Detalle de Multa"
            2 -> "📌 Detalle de Cuota Extraordinaria"
            else -> "📋 Detalle de Cuota"
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(titulo)
            .setMessage(mensaje)
            .setPositiveButton("Editar Descuento") { _, _ ->
                mostrarDialogoEditarDescuentoCobro(cuota)
            }
            .setNeutralButton("Eliminar") { _, _ ->
                confirmarEliminacionCuota(cuota)
            }
            .setNegativeButton("Cerrar", null)
            .show()
    }

    private fun mostrarDialogoEditarDescuentoCobro(cuota: DeudaPendiente) {
        val view = layoutInflater.inflate(R.layout.dialog_editar_descuento, null)
        val etDescuento = view.findViewById<TextInputEditText>(R.id.etDescuentoDialog)
        etDescuento.setText(cuota.descuento?.toString() ?: "0.00")

        MaterialAlertDialogBuilder(this)
            .setTitle("✏️ Modificación Especial (Descuento)")
            .setView(view)
            .setPositiveButton("Aplicar") { _, _ ->
                val nuevoDescuentoStr = etDescuento.text.toString().trim()
                val nuevoDescuento = nuevoDescuentoStr.toDoubleOrNull() ?: 0.0

                val apiService = RetrofitClient.getApiService()
                val payload = ActualizarCuotaPayload(
                    idCuota = cuota.idDeuda,
                    descuento = nuevoDescuento,
                    montoOriginal = cuota.montoOriginal ?: cuota.monto
                )

                apiService.actualizarCuotaJson(payload).enqueue(object : Callback<RespuestaBase> {
                    override fun onResponse(call: Call<RespuestaBase>, response: Response<RespuestaBase>) {
                        if (response.isSuccessful && (response.body()?.isSuccess == true || response.body()?.success == true || response.body()?.status == "success")) {
                            Toast.makeText(this@CobrosActivity, "¡Descuento aplicado con éxito!", Toast.LENGTH_SHORT).show()
                            socioSeleccionado?.let { consultarDeudasSocio(it.idAsociado) }
                        } else {
                            val rawError = response.errorBody()?.string()
                            val msg = response.body()?.error ?: response.body()?.message ?: rawError ?: "Cód: ${response.code()}"
                            Toast.makeText(this@CobrosActivity, "Error al actualizar descuento: $msg", Toast.LENGTH_LONG).show()
                        }
                    }

                    override fun onFailure(call: Call<RespuestaBase>, t: Throwable) {
                        Toast.makeText(this@CobrosActivity, "Error de red: ${t.message}", Toast.LENGTH_LONG).show()
                    }
                })
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun confirmarEliminacionCuota(cuota: DeudaPendiente) {
        val mensaje = "¿Está seguro de que desea eliminar permanentemente esta cuota/deuda?\n\n" +
                "Concepto: ${cuota.concepto}\n" +
                "Monto Pendiente: S/ ${String.format(Locale.getDefault(), "%.2f", cuota.monto)}\n\n" +
                "⚠️ Esta acción eliminará el registro de la base de datos."

        MaterialAlertDialogBuilder(this)
            .setTitle("⚠️ Confirmar Eliminación")
            .setMessage(mensaje)
            .setPositiveButton("Sí, Eliminar") { _, _ ->
                eliminarCuotaEnServidor(cuota.idDeuda)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun eliminarCuotaEnServidor(idCuota: Int) {
        val apiService = RetrofitClient.getApiService()
        val payload = EliminarCuotasPayload(ids = listOf(idCuota))

        apiService.eliminarCuotasJson(payload).enqueue(object : Callback<RespuestaBase> {
            override fun onResponse(call: Call<RespuestaBase>, response: Response<RespuestaBase>) {
                if (response.isSuccessful && (response.body()?.isSuccess == true || response.body()?.success == true)) {
                    Toast.makeText(this@CobrosActivity, "¡Registro eliminado correctamente!", Toast.LENGTH_SHORT).show()
                    socioSeleccionado?.let { consultarDeudasSocio(it.idAsociado) }
                } else {
                    Toast.makeText(this@CobrosActivity, "Error al eliminar registro: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<RespuestaBase>, t: Throwable) {
                Toast.makeText(this@CobrosActivity, "Error de red: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun prepararListaConHeaders(deudas: List<DeudaPendiente>): List<DeudaListItem> {
        val resultado = mutableListOf<DeudaListItem>()

        // 1. Cuotas Mensuales / Ordinarias (idTipoCuota == 1), ordenadas por año y mes descendente
        val mensuales = deudas.filter { it.idTipoCuota == 1 }.sortedWith(compareByDescending<DeudaPendiente> { it.anio ?: 0 }.thenByDescending { it.mes ?: 0 })
        if (mensuales.isNotEmpty()) {
            resultado.add(DeudaListItem.Header("CUOTA ORDINARIA"))
            mensuales.forEach { resultado.add(DeudaListItem.Item(it)) }
        }

        // 2. Cuotas Extraordinarias (idTipoCuota == 2), ordenadas por idDeuda descendente
        val extraordinarias = deudas.filter { it.idTipoCuota == 2 }.sortedByDescending { it.idDeuda }
        if (extraordinarias.isNotEmpty()) {
            resultado.add(DeudaListItem.Header("CUOTA EXTRAORDINARIA"))
            extraordinarias.forEach { resultado.add(DeudaListItem.Item(it)) }
        }

        // 3. Multas (idTipoCuota == 3), ordenadas por idDeuda descendente
        val multas = deudas.filter { it.idTipoCuota == 3 }.sortedByDescending { it.idDeuda }
        if (multas.isNotEmpty()) {
            resultado.add(DeudaListItem.Header("MULTA"))
            multas.forEach { resultado.add(DeudaListItem.Item(it)) }
        }

        // 4. Otros tipos
        val otros = deudas.filter { it.idTipoCuota !in listOf(1, 2, 3) }
        if (otros.isNotEmpty()) {
            resultado.add(DeudaListItem.Header("OTRAS DEUDAS"))
            otros.forEach { resultado.add(DeudaListItem.Item(it)) }
        }

        return resultado
    }

    private fun recalcularTotal() {
        totalPagar = deudasObtenidas.filter { it.estaSeleccionada }.sumOf { it.monto }
        tvMontoTotalCobro.text = String.format("S/ %.2f", totalPagar)
        btnProcesarPago.isEnabled = totalPagar > 0
    }

    private fun procesarPagoBD() {
        val deudasAPagar = deudasObtenidas.filter { it.estaSeleccionada }
        if (deudasAPagar.isEmpty()) return

        val idCuentaSeleccionada = (spCuentaGeneral.selectedItem as? CuentaBancaria)?.idCuenta ?: 1
        val nombreSocio = socioSeleccionado?.let { "${it.nombres} ${it.apellidoPaterno} ${it.apellidoMaterno}" } ?: "Integrante"
        val descripcion = construirDescripcionPago(nombreSocio, deudasAPagar)

        val pagosList = deudasAPagar.map { PagoItemPayload(it.idDeuda, it.monto) }
        val payload = GuardarPagoPayload(
            idCuenta = idCuentaSeleccionada,
            descripcion = descripcion,
            pagos = pagosList
        )

        btnProcesarPago.isEnabled = false
        val apiService = RetrofitClient.getApiService()
        apiService.guardarPagoJson(payload).enqueue(object : Callback<RespuestaBase> {
            override fun onResponse(call: Call<RespuestaBase>, response: Response<RespuestaBase>) {
                btnProcesarPago.isEnabled = true
                if (response.isSuccessful && (response.body()?.isSuccess == true || response.body()?.success == true)) {
                    Toast.makeText(this@CobrosActivity, "¡Pago registrado exitosamente!", Toast.LENGTH_LONG).show()
                    finish()
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Código ${response.code()}"
                    Toast.makeText(this@CobrosActivity, "Error al registrar pago: $errorMsg", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<RespuestaBase>, t: Throwable) {
                btnProcesarPago.isEnabled = true
                Toast.makeText(this@CobrosActivity, "Error de red al registrar pago: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun construirDescripcionPago(nombreSocio: String, deudas: List<DeudaPendiente>): String {
        val mesNombres = arrayOf("", "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre")

        val partes = mutableListOf<String>()

        // 1. Cuotas Mensuales / Ordinarias (idTipoCuota == 1)
        val mensuales = deudas.filter { it.idTipoCuota == 1 }
        if (mensuales.isNotEmpty()) {
            val mesesStr = mensuales.joinToString(", ") { deuda ->
                val nombreMes = if (deuda.mes != null && deuda.mes in 1..12) mesNombres[deuda.mes] else ""
                val anioStr = deuda.anio?.toString() ?: ""
                "$nombreMes $anioStr".trim()
            }
            partes.add("cuotas ordinarias ($mesesStr)")
        }

        // 2. Cuotas Extraordinarias (idTipoCuota == 2)
        val extraordinarias = deudas.filter { it.idTipoCuota == 2 }
        if (extraordinarias.isNotEmpty()) {
            val extrasStr = extraordinarias.joinToString(", ") { it.concepto ?: "extraordinaria" }
            partes.add("cuotas extraordinarias ($extrasStr)")
        }

        // 3. Multas (idTipoCuota == 3)
        val multas = deudas.filter { it.idTipoCuota == 3 }
        if (multas.isNotEmpty()) {
            val multasStr = multas.joinToString(", ") { if (!it.nombreActividad.isNullOrEmpty()) it.nombreActividad else (it.concepto ?: "multa") }
            partes.add("multas ($multasStr)")
        }

        // 4. Otros
        val otros = deudas.filter { it.idTipoCuota !in listOf(1, 2, 3) }
        if (otros.isNotEmpty()) {
            val otrosStr = otros.joinToString(", ") { it.concepto ?: "deuda" }
            partes.add("otros ($otrosStr)")
        }

        val detalle = if (partes.isNotEmpty()) partes.joinToString(", ") else "deudas diversas"
        return "$nombreSocio pago de $detalle"
    }
}
