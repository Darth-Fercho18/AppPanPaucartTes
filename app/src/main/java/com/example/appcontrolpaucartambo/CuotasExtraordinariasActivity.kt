package com.example.appcontrolpaucartambo

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Calendar
import java.util.Locale

data class AsociadoIdPayload(
    @SerializedName("id_asociado") val idAsociado: Int
)

data class CuotaExtraordinariaPayload(
    @SerializedName("id_tipo_cuota") val idTipoCuota: Int = 2,
    @SerializedName("concepto") val concepto: String,
    @SerializedName("monto_original") val montoOriginal: Double,
    @SerializedName("mes") val mes: Int,
    @SerializedName("anio") val anio: Int,
    @SerializedName("id_actividad") val idActividad: Int? = null,
    @SerializedName("asociados") val asociados: List<AsociadoIdPayload>
)

class CuotasExtraordinariasActivity : AppCompatActivity() {

    private lateinit var spCuotasExistentes: Spinner
    private lateinit var etConceptoExtra: TextInputEditText
    private lateinit var etMontoExtra: TextInputEditText
    private lateinit var spFiltroTipoExtra: Spinner
    private lateinit var etBuscarAsociadoExtra: TextInputEditText
    private lateinit var cbSeleccionarTodos: CheckBox
    private lateinit var rvAsociadosExtra: RecyclerView
    private lateinit var btnAsignarExtra: Button

    private var listaTodosAsociados: List<Asociado> = emptyList()
    private var listaCuotasExistentes: List<DeudaPendiente> = emptyList()
    private var listaItemsAdaptador: List<CuotaExtraordinariaItem> = emptyList()
    private lateinit var adapter: CuotasExtraordinariasAdapter

    private var spinnersListos = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cuotas_extraordinarias)

        spCuotasExistentes = findViewById(R.id.spCuotasExistentes)
        etConceptoExtra = findViewById(R.id.etConceptoExtra)
        etMontoExtra = findViewById(R.id.etMontoExtra)
        spFiltroTipoExtra = findViewById(R.id.spFiltroTipoExtra)
        etBuscarAsociadoExtra = findViewById(R.id.etBuscarAsociadoExtra)
        cbSeleccionarTodos = findViewById(R.id.cbSeleccionarTodos)
        rvAsociadosExtra = findViewById(R.id.rvAsociadosExtra)
        btnAsignarExtra = findViewById(R.id.btnAsignarExtra)

        rvAsociadosExtra.layoutManager = LinearLayoutManager(this)
        adapter = CuotasExtraordinariasAdapter(emptyList()) { _, _ ->
            actualizarEstadoCheckboxTodos()
        }
        rvAsociadosExtra.adapter = adapter

        etBuscarAsociadoExtra.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                filtrarPorBusqueda(s.toString())
            }
        })

        cbSeleccionarTodos.setOnCheckedChangeListener { _, isChecked ->
            if (spinnersListos) {
                listaItemsAdaptador.forEach { it.isSelected = isChecked }
                adapter.notifyDataSetChanged()
            }
        }

        spFiltroTipoExtra.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            private var primerDisparo = true
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (primerDisparo) {
                    primerDisparo = false
                    return
                }
                filtrarAsociadosPorTipo()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        spCuotasExistentes.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            private var primerDisparo = true
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (primerDisparo) {
                    primerDisparo = false
                    return
                }
                if (position > 0 && (position - 1) < listaCuotasExistentes.size) {
                    val cuota = listaCuotasExistentes[position - 1]
                    etConceptoExtra.setText(cuota.concepto)
                    etMontoExtra.setText(cuota.montoOriginal?.toString() ?: cuota.monto.toString())
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        btnAsignarExtra.setOnClickListener {
            asignarCuotasExtraordinarias()
        }

        cargarDatosIniciales()
    }

    private fun cargarDatosIniciales() {
        val apiService = RetrofitClient.getApiService()

        // 1. Cargar Asociados
        apiService.getAsociados().enqueue(object : Callback<List<Asociado>> {
            override fun onResponse(call: Call<List<Asociado>>, response: Response<List<Asociado>>) {
                if (response.isSuccessful && response.body() != null) {
                    listaTodosAsociados = response.body()!!
                    filtrarAsociadosPorTipo()
                    spinnersListos = true
                }
            }
            override fun onFailure(call: Call<List<Asociado>>, t: Throwable) {
                Toast.makeText(this@CuotasExtraordinariasActivity, "Error al cargar integrantes", Toast.LENGTH_SHORT).show()
            }
        })

        // 2. Cargar Cuotas Extraordinarias Existentes (id_tipo_cuota = 2)
        apiService.obtenerCuotasAsociado(0, 2, null).enqueue(object : Callback<List<DeudaPendiente>> {
            override fun onResponse(call: Call<List<DeudaPendiente>>, response: Response<List<DeudaPendiente>>) {
                if (response.isSuccessful && response.body() != null) {
                    val cuotasCargadas = response.body()!!
                    // Filtrar para mostrar únicamente conceptos únicos (evitando repeticiones por cada socio)
                    listaCuotasExistentes = cuotasCargadas.distinctBy {
                        "${it.concepto.trim().lowercase(Locale.ROOT)}_${it.montoOriginal ?: it.monto}"
                    }
                    val opciones = mutableListOf("Seleccione una cuota existente o ingrese manual...")
                    opciones.addAll(listaCuotasExistentes.map { "${it.concepto} (S/ ${it.montoOriginal ?: it.monto})" })

                    val spinnerAdapter = ArrayAdapter(this@CuotasExtraordinariasActivity, android.R.layout.simple_spinner_item, opciones)
                    spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spCuotasExistentes.adapter = spinnerAdapter
                }
            }
            override fun onFailure(call: Call<List<DeudaPendiente>>, t: Throwable) {
                // Opcional
            }
        })
    }

    private fun filtrarAsociadosPorTipo() {
        filtrarPorBusqueda(etBuscarAsociadoExtra.text.toString())
        cbSeleccionarTodos.isChecked = false
    }

    private fun filtrarPorBusqueda(query: String) {
        val q = query.trim().lowercase(Locale.ROOT)
        val posicionFiltro = spFiltroTipoExtra.selectedItemPosition
        val tipoSocio = if (posicionFiltro > 0) spFiltroTipoExtra.selectedItem.toString() else "FILA"

        val idTipoBuscado = when (tipoSocio.uppercase(Locale.ROOT)) {
            "FILA" -> 1
            "NIÑOS" -> 2
            "EX INTEGRANTE" -> 3
            else -> 1
        }

        val filtradosBase = listaTodosAsociados.filter { it.idTipo == idTipoBuscado }

        val listaFiltrada = if (q.isEmpty()) {
            filtradosBase.map { CuotaExtraordinariaItem(it, false) }
        } else {
            filtradosBase.filter { asociado ->
                val nombreCompleto = "${asociado.nombres} ${asociado.apellidoPaterno} ${asociado.apellidoMaterno}".lowercase(Locale.ROOT)
                val dni = asociado.dni.lowercase(Locale.ROOT)
                nombreCompleto.contains(q) || dni.contains(q)
            }.map { CuotaExtraordinariaItem(it, false) }
        }

        listaItemsAdaptador = listaFiltrada
        adapter.actualizarLista(listaItemsAdaptador)
    }

    private fun actualizarEstadoCheckboxTodos() {
        if (listaItemsAdaptador.isNotEmpty()) {
            val todosSeleccionados = listaItemsAdaptador.all { it.isSelected }
            cbSeleccionarTodos.setOnCheckedChangeListener(null)
            cbSeleccionarTodos.isChecked = todosSeleccionados
            cbSeleccionarTodos.setOnCheckedChangeListener { _, isChecked ->
                if (spinnersListos) {
                    listaItemsAdaptador.forEach { it.isSelected = isChecked }
                    adapter.notifyDataSetChanged()
                }
            }
        }
    }

    private fun asignarCuotasExtraordinarias() {
        val concepto = etConceptoExtra.text.toString().trim()
        val montoStr = etMontoExtra.text.toString().trim()
        val monto = montoStr.toDoubleOrNull() ?: 0.0

        if (concepto.isEmpty()) {
            etConceptoExtra.error = "Ingrese el concepto"
            return
        }
        if (monto <= 0) {
            etMontoExtra.error = "Ingrese un monto válido"
            return
        }

        val seleccionados = listaItemsAdaptador.filter { it.isSelected }
        if (seleccionados.isEmpty()) {
            Toast.makeText(this, "Seleccione al menos un integrante", Toast.LENGTH_SHORT).show()
            return
        }

        val cal = Calendar.getInstance()
        val mesActual = cal.get(Calendar.MONTH) + 1
        val anioActual = cal.get(Calendar.YEAR)

        val asociadosPayload = seleccionados.map { AsociadoIdPayload(it.asociado.idAsociado) }
        val payload = CuotaExtraordinariaPayload(
            idTipoCuota = 2,
            concepto = concepto,
            montoOriginal = monto,
            mes = mesActual,
            anio = anioActual,
            idActividad = null,
            asociados = asociadosPayload
        )

        btnAsignarExtra.isEnabled = false
        val apiService = RetrofitClient.getApiService()
        apiService.guardarExtraordinariaJson(payload).enqueue(object : Callback<RespuestaBase> {
            override fun onResponse(call: Call<RespuestaBase>, response: Response<RespuestaBase>) {
                btnAsignarExtra.isEnabled = true
                if (response.isSuccessful && (response.body()?.isSuccess == true || response.body()?.success == true)) {
                    Toast.makeText(this@CuotasExtraordinariasActivity, "¡Cuotas extraordinarias asignadas con éxito!", Toast.LENGTH_LONG).show()
                    finish()
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Código ${response.code()}"
                    Toast.makeText(this@CuotasExtraordinariasActivity, "Error al asignar cuotas: $errorMsg", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<RespuestaBase>, t: Throwable) {
                btnAsignarExtra.isEnabled = true
                Toast.makeText(this@CuotasExtraordinariasActivity, "Error de red al asignar cuotas: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }
}
