package com.example.appcontrolpaucartambo

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Calendar
import java.util.Locale

class ListarMovimientosActivity : AppCompatActivity() {

    private lateinit var rvMovimientos: RecyclerView
    private lateinit var spFiltroTipoMovimiento: Spinner
    private lateinit var spFiltroCategoria: Spinner
    private lateinit var etFiltroFecha: TextInputEditText
    private lateinit var btnLimpiarFiltro: MaterialButton
    private lateinit var btnNuevoMovimiento: MaterialButton

    private lateinit var adapter: MovimientosAdapter
    private var listaCompleta: List<Movimiento> = emptyList()
    private var listaFiltradaActual: List<Movimiento> = emptyList()
    private var fechaSeleccionadaFiltro: String? = null // Formato "YYYY-MM"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_listar_movimientos)

        rvMovimientos = findViewById(R.id.rvMovimientos)
        spFiltroTipoMovimiento = findViewById(R.id.spFiltroTipoMovimiento)
        spFiltroCategoria = findViewById(R.id.spFiltroCategoria)
        etFiltroFecha = findViewById(R.id.etFiltroFecha)
        btnLimpiarFiltro = findViewById(R.id.btnLimpiarFiltro)
        btnNuevoMovimiento = findViewById(R.id.btnNuevoMovimiento)

        rvMovimientos.layoutManager = LinearLayoutManager(this)
        adapter = MovimientosAdapter(emptyList()) { movimiento ->
            confirmarEliminacionMovimiento(movimiento)
        }
        rvMovimientos.adapter = adapter

        // Configurar Spinner de Tipo de Movimiento (TODOS, INGRESO, EGRESO)
        val opcionesTipo = listOf("TODOS", "INGRESO", "EGRESO")
        val adapterTipo = ArrayAdapter(this, R.layout.spinner_item_selected, opcionesTipo)
        adapterTipo.setDropDownViewResource(R.layout.spinner_item_dropdown)
        spFiltroTipoMovimiento.adapter = adapterTipo

        // Configurar Spinner de Categorías inicial
        val adapterCatInicial = ArrayAdapter(this, R.layout.spinner_item_selected, listOf("TODAS LAS CATEGORÍAS"))
        adapterCatInicial.setDropDownViewResource(R.layout.spinner_item_dropdown)
        spFiltroCategoria.adapter = adapterCatInicial

        // Listeners para los spinners
        spFiltroTipoMovimiento.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            private var primerDisparo = true
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (primerDisparo) {
                    primerDisparo = false
                    return
                }
                aplicarFiltros()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        spFiltroCategoria.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            private var primerDisparo = true
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (primerDisparo) {
                    primerDisparo = false
                    return
                }
                aplicarFiltros()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        etFiltroFecha.setOnClickListener {
            mostrarPickerMesAnioFiltro()
        }

        btnLimpiarFiltro.setOnClickListener {
            fechaSeleccionadaFiltro = null
            etFiltroFecha.setText("")
            spFiltroTipoMovimiento.setSelection(0)
            if (spFiltroCategoria.adapter != null && spFiltroCategoria.adapter.count > 0) {
                spFiltroCategoria.setSelection(0)
            }
            aplicarFiltros()
        }

        btnNuevoMovimiento.setOnClickListener {
            val intent = Intent(this, MovimientosActivity::class.java)
            startActivity(intent)
        }

        configurarSwipeComprobante()
    }

    override fun onResume() {
        super.onResume()
        cargarMovimientosDesdeApi()
    }

    private fun cargarMovimientosDesdeApi() {
        val apiService = RetrofitClient.getApiService()
        apiService.obtenerMovimientos().enqueue(object : Callback<List<Movimiento>> {
            override fun onResponse(call: Call<List<Movimiento>>, response: Response<List<Movimiento>>) {
                if (response.isSuccessful && response.body() != null) {
                    // Ordenar de forma descendente por id_movimiento para que el más actual aparezca arriba
                    listaCompleta = response.body()!!.sortedByDescending { it.idMovimiento }
                    actualizarSpinnerCategorias()
                    aplicarFiltros()
                } else {
                    Toast.makeText(this@ListarMovimientosActivity, "Error al cargar movimientos", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<Movimiento>>, t: Throwable) {
                Toast.makeText(this@ListarMovimientosActivity, "Error de red: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun actualizarSpinnerCategorias() {
        val posPrevio = spFiltroCategoria.selectedItemPosition
        val catPrevio = if (posPrevio > 0) spFiltroCategoria.selectedItem?.toString() else null

        // Extraer categorías únicas sin repeticiones
        val categoriasUnicas = listaCompleta
            .mapNotNull { it.categoria?.trim() }
            .filter { it.isNotEmpty() }
            .distinctBy { it.uppercase(Locale.ROOT) }
            .sorted()

        val opcionesCategorias = mutableListOf("TODAS LAS CATEGORÍAS")
        opcionesCategorias.addAll(categoriasUnicas)

        val adapterCat = ArrayAdapter(
            this,
            R.layout.spinner_item_selected,
            opcionesCategorias
        )
        adapterCat.setDropDownViewResource(R.layout.spinner_item_dropdown)
        spFiltroCategoria.adapter = adapterCat

        // Restaurar selección previa si aún existe en la lista
        if (!catPrevio.isNullOrEmpty()) {
            val nuevaPos = opcionesCategorias.indexOfFirst { it.equals(catPrevio, ignoreCase = true) }
            if (nuevaPos != -1) {
                spFiltroCategoria.setSelection(nuevaPos)
            }
        }
    }

    private fun aplicarFiltros() {
        val tipoSeleccionado = spFiltroTipoMovimiento.selectedItem?.toString() ?: "TODOS"
        val posCategoria = spFiltroCategoria.selectedItemPosition
        val categoriaSeleccionada = if (posCategoria > 0) spFiltroCategoria.selectedItem?.toString() else null

        listaFiltradaActual = listaCompleta.filter { mov ->
            // 1. Filtro por Tipo de Movimiento (TODOS, INGRESO, EGRESO)
            val cumpleTipo = when {
                tipoSeleccionado.uppercase(Locale.ROOT).contains("INGRESO") -> mov.tipoMovimiento.equals("INGRESO", ignoreCase = true)
                tipoSeleccionado.uppercase(Locale.ROOT).contains("EGRESO") -> mov.tipoMovimiento.equals("EGRESO", ignoreCase = true)
                else -> true
            }

            // 2. Filtro por Categoría
            val cumpleCategoria = if (!categoriaSeleccionada.isNullOrEmpty()) {
                mov.categoria?.trim()?.equals(categoriaSeleccionada.trim(), ignoreCase = true) == true
            } else {
                true
            }

            // 3. Filtro por Mes y Año (YYYY-MM)
            val cumpleFecha = if (!fechaSeleccionadaFiltro.isNullOrEmpty()) {
                mov.fechaMovimiento?.startsWith(fechaSeleccionadaFiltro!!) == true
            } else {
                true
            }

            cumpleTipo && cumpleCategoria && cumpleFecha
        }

        adapter.actualizarLista(listaFiltradaActual)
    }

    private fun confirmarEliminacionMovimiento(movimiento: Movimiento) {
        val simbolo = if (movimiento.idCuenta == 4) "$" else "S/"
        val detalle = movimiento.concepto ?: movimiento.categoria ?: "Movimiento"
        val mensaje = "¿Desea confirmar el borrado del movimiento?\n\n" +
                "${movimiento.tipoMovimiento.uppercase()}: $detalle\n" +
                "Monto: $simbolo ${String.format(Locale.getDefault(), "%.2f", movimiento.monto)}"

        AlertDialog.Builder(this)
            .setTitle("Confirmar Borrado")
            .setMessage(mensaje)
            .setPositiveButton("Sí, Borrar") { _, _ ->
                eliminarMovimientoEnServidor(movimiento.idMovimiento)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun eliminarMovimientoEnServidor(idMovimiento: Int) {
        val apiService = RetrofitClient.getApiService()
        apiService.eliminarMovimiento(idMovimiento).enqueue(object : Callback<RespuestaMovimiento> {
            override fun onResponse(call: Call<RespuestaMovimiento>, response: Response<RespuestaMovimiento>) {
                if (response.isSuccessful && response.body() != null) {
                    val res = response.body()!!
                    if (res.status == "success" || res.status?.uppercase() == "SUCCESS") {
                        Toast.makeText(this@ListarMovimientosActivity, "Movimiento borrado correctamente", Toast.LENGTH_SHORT).show()
                        cargarMovimientosDesdeApi()
                    } else {
                        Toast.makeText(this@ListarMovimientosActivity, res.error ?: res.message ?: "Error al borrar movimiento", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(this@ListarMovimientosActivity, "Error al procesar eliminación en el servidor", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<RespuestaMovimiento>, t: Throwable) {
                Toast.makeText(this@ListarMovimientosActivity, "Error de red: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun configurarSwipeComprobante() {
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                rvMovimientos.adapter?.notifyItemChanged(position)

                if (position != RecyclerView.NO_POSITION && position < listaFiltradaActual.size) {
                    val movimientoSeleccionado = listaFiltradaActual[position]
                    val intent = Intent(this@ListarMovimientosActivity, ComprobanteMovimientoActivity::class.java)
                    intent.putExtra("MOVIMIENTO_EXTRA", movimientoSeleccionado)
                    startActivity(intent)
                }
            }
        }
        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(rvMovimientos)
    }

    private fun mostrarPickerMesAnioFiltro() {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH)

        var initialYear = currentYear
        var initialMonth = currentMonth

        if (!fechaSeleccionadaFiltro.isNullOrEmpty() && fechaSeleccionadaFiltro!!.contains("-")) {
            val partes = fechaSeleccionadaFiltro!!.split("-")
            if (partes.size >= 2) {
                initialYear = partes[0].toIntOrNull() ?: currentYear
                initialMonth = (partes[1].toIntOrNull() ?: (currentMonth + 1)) - 1
            }
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(40, 30, 40, 10)
        }

        val meses = arrayOf(
            "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
            "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
        )

        val monthPicker = NumberPicker(this).apply {
            minValue = 0
            maxValue = 11
            displayedValues = meses
            value = initialMonth.coerceIn(0, 11)
            wrapSelectorWheel = true
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val yearPicker = NumberPicker(this).apply {
            minValue = currentYear - 10
            maxValue = currentYear + 10
            value = initialYear.coerceIn(currentYear - 10, currentYear + 10)
            wrapSelectorWheel = false
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        layout.addView(monthPicker)
        layout.addView(yearPicker)

        AlertDialog.Builder(this)
            .setTitle("Filtrar por Mes y Año")
            .setView(layout)
            .setPositiveButton("Aceptar") { _, _ ->
                val selectedMonth = monthPicker.value
                val selectedYear = yearPicker.value

                // Guardar filtro en formato YYYY-MM
                fechaSeleccionadaFiltro = String.format(Locale.US, "%04d-%02d", selectedYear, selectedMonth + 1)

                // Texto amigable para mostrar, ej: "Octubre 2026"
                val textoDisplay = "${meses[selectedMonth]} $selectedYear"
                etFiltroFecha.setText(textoDisplay)

                aplicarFiltros()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
