package com.example.appcontrolpaucartambo

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AsistenciaActivity : AppCompatActivity() {

    private lateinit var spActividad: Spinner
    private lateinit var spFiltroTipoAsistencia: Spinner
    private lateinit var etBuscarAsistencia: TextInputEditText
    private lateinit var rvAsistencia: RecyclerView
    private lateinit var btnGuardarAsistencia: Button
    private lateinit var btnAgregarActividad: Button
    private lateinit var btnEditarActividad: MaterialButton
    private lateinit var tvEstadoCarga: TextView

    private lateinit var adapter: AsistenciaAdapter
    private var listaActividades: List<Actividad> = emptyList()
    private var listaTodosLosAsociados: List<Asociado> = emptyList()
    private var listaCompletaActual: List<AsistenciaItem> = emptyList()

    // Bandera vital para detener el disparo automático de los Spinners de Android
    private var spinnersListos = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_asistencia)

        spActividad = findViewById(R.id.spActividad)
        spFiltroTipoAsistencia = findViewById(R.id.spFiltroTipoAsistencia)
        etBuscarAsistencia = findViewById(R.id.etBuscarAsistencia)
        rvAsistencia = findViewById(R.id.rvAsistencia)
        btnGuardarAsistencia = findViewById(R.id.btnGuardarAsistencia)
        btnAgregarActividad = findViewById(R.id.btnAgregarActividad)
        btnEditarActividad = findViewById(R.id.btnEditarActividad)
        tvEstadoCarga = findViewById(R.id.tvEstadoCarga)

        rvAsistencia.layoutManager = LinearLayoutManager(this)
        adapter = AsistenciaAdapter(emptyList())
        rvAsistencia.adapter = adapter

        etBuscarAsistencia.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                filtrarPorBusqueda(s.toString())
            }
        })

        btnGuardarAsistencia.setOnClickListener { enviarAsistencia() }
        btnAgregarActividad.setOnClickListener { mostrarDialogoAgregarActividad() }

        // Evento de clic para Editar Actividad
        btnEditarActividad.setOnClickListener {
            val actividadSeleccionada = spActividad.selectedItem as? Actividad
            if (actividadSeleccionada != null) {
                mostrarDialogoEditarActividad(actividadSeleccionada)
            } else {
                Toast.makeText(this, "Seleccione una actividad válida para editar", Toast.LENGTH_SHORT).show()
            }
        }

        // ACTIVAMOS EL GESTO SWIPE EN EL RECYCLERVIEW
        configurarGestoDeslizar()

        // INICIAMOS LA CADENA ORDENADA (Paso 1)
        descargarSociosPrimero()
    }

    // --- PASO 1: Descargar los Socios ---
    private fun descargarSociosPrimero() {
        tvEstadoCarga.text = "1/2 Descargando asociados..."

        RetrofitClient.getApiService().getAsociados().enqueue(object : Callback<List<Asociado>> {
            override fun onResponse(call: Call<List<Asociado>>, response: Response<List<Asociado>>) {
                if (response.isSuccessful && response.body() != null) {

                    // Agregamos el ordenamiento por ID aquí mismo
                    listaTodosLosAsociados = response.body()!!.sortedBy { it.idAsociado }

                    descargarActividadesDespues()
                } else {
                    tvEstadoCarga.text = "Error HTTP al cargar integrantes: ${response.code()}"
                }
            }

            override fun onFailure(call: Call<List<Asociado>>, t: Throwable) {
                tvEstadoCarga.text = "Fallo de red en integrantes: ${t.message}"
            }
        })
    }

    // --- PASO 2: Descargar Actividades ---
    private fun descargarActividadesDespues() {
        tvEstadoCarga.text = "2/2 Descargando actividades..."

        RetrofitClient.getApiService().obtenerActividades().enqueue(object : Callback<List<Actividad>> {
            override fun onResponse(call: Call<List<Actividad>>, response: Response<List<Actividad>>) {
                if (response.isSuccessful && response.body() != null) {
                    listaActividades = response.body()!!

                    val spinnerAdapter = ArrayAdapter(
                        this@AsistenciaActivity,
                        android.R.layout.simple_spinner_dropdown_item,
                        listaActividades
                    )
                    spActividad.adapter = spinnerAdapter

                    if (listaActividades.isEmpty()) {
                        tvEstadoCarga.text = "No hay actividades. Presione '+' para crear una."
                    } else {
                        tvEstadoCarga.text = "Todo listo. Seleccione una opción."
                        if (spFiltroTipoAsistencia.selectedItemPosition <= 0) {
                            spFiltroTipoAsistencia.setSelection(1) // Selecciona "FILA" por defecto
                        }
                    }

                    // SOLO CUANDO TODO ESTÁ DESCARGADO Y DIBUJADO, ACTIVAMOS LOS LECTORES
                    spinnersListos = true
                    configurarFiltroSocio()
                    verificarYTraerDatos()

                } else {
                    tvEstadoCarga.text = "Error HTTP en actividades: ${response.code()}"
                }
            }

            override fun onFailure(call: Call<List<Actividad>>, t: Throwable) {
                tvEstadoCarga.text = "Fallo de red en actividades: ${t.message}"
            }
        })
    }

    // --- PASO 3: Controlar la interacción humana ---
    private fun configurarFiltroSocio() {
        val listener = object : AdapterView.OnItemSelectedListener {

            private var primerDisparo = true

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (primerDisparo || !spinnersListos) {
                    primerDisparo = false
                    return // Ignoramos el evento automático de Android
                }

                verificarYTraerDatos() // Solo se ejecuta si es un toque real
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        spFiltroTipoAsistencia.onItemSelectedListener = listener
        spActividad.onItemSelectedListener = listener
    }

    // --- PASO 4: Cargar la lista solo cuando se selecciona algo válido ---
    private fun verificarYTraerDatos() {
        val actividadSeleccionada = spActividad.selectedItem as? Actividad
        val posicionFiltro = spFiltroTipoAsistencia.selectedItemPosition

        if (actividadSeleccionada != null) {
            val tipoSocio = if (posicionFiltro > 0) {
                spFiltroTipoAsistencia.selectedItem.toString()
            } else {
                "FILA"
            }
            tvEstadoCarga.text = "Cargando asistencia para $tipoSocio..."

            cargarSoloAsistencias(tipoSocio, actividadSeleccionada.idActividad)
        } else {
            adapter.actualizarLista(emptyList())
            tvEstadoCarga.text = "Seleccione una actividad válida."
        }
    }

    private fun cargarSoloAsistencias(tipoSocio: String, idActividad: Int) {
        RetrofitClient.getApiService().obtenerAsistenciasPorActividad(idActividad).enqueue(object : Callback<List<Asistencia>> {
            override fun onResponse(call: Call<List<Asistencia>>, responseAsistencias: Response<List<Asistencia>>) {
                if (!responseAsistencias.isSuccessful) {
                    tvEstadoCarga.text = "Error al obtener asistencias (Cód: ${responseAsistencias.code()})"
                    return
                }

                val asistenciasPrevias = responseAsistencias.body() ?: emptyList()
                val idTipoBuscado = when (tipoSocio.uppercase(Locale.ROOT)) {
                    "FILA" -> 1
                    "NIÑOS" -> 2
                    "EX INTEGRANTE" -> 3
                    else -> 0
                }

                val asociadosFiltrados = listaTodosLosAsociados.filter { it.idTipo == idTipoBuscado }

                listaCompletaActual = asociadosFiltrados.map { asociado ->
                    val registroEncontrado = asistenciasPrevias.find { it.idAsociado == asociado.idAsociado }
                    val estadoInicial = registroEncontrado?.estado ?: "Seleccionar..."
                    AsistenciaItem(asociado, estadoInicial)
                }

                filtrarPorBusqueda(etBuscarAsistencia.text.toString())
                tvEstadoCarga.text = "Lista de $tipoSocio (${listaCompletaActual.size} personas)"
            }

            override fun onFailure(call: Call<List<Asistencia>>, t: Throwable) {
                tvEstadoCarga.text = "Error al conectar por asistencias: ${t.message}"
            }
        })
    }

    private fun filtrarPorBusqueda(query: String) {
        val q = query.trim().lowercase(Locale.ROOT)
        val listaFiltrada = if (q.isEmpty()) {
            listaCompletaActual
        } else {
            listaCompletaActual.filter { item ->
                val nombreCompleto = "${item.asociado.nombres} ${item.asociado.apellidoPaterno} ${item.asociado.apellidoMaterno}".lowercase(Locale.ROOT)
                val dni = item.asociado.dni.lowercase(Locale.ROOT)
                nombreCompleto.contains(q) || dni.contains(q)
            }
        }
        adapter.actualizarLista(listaFiltrada)
    }

    private fun enviarAsistencia() {
        val actividadSeleccionada = spActividad.selectedItem as? Actividad
        if (actividadSeleccionada == null) {
            Toast.makeText(this, "Debe seleccionar una actividad válida.", Toast.LENGTH_SHORT).show()
            return
        }

        val datosEnPantalla = adapter.obtenerDatosAsistencia()
        val marcados = datosEnPantalla.filter { it.estadoSeleccionado != "Seleccionar..." }

        if (marcados.isEmpty()) {
            Toast.makeText(this, "No ha marcado la asistencia de ningún integrante.", Toast.LENGTH_SHORT).show()
            return
        }

        btnGuardarAsistencia.isEnabled = false
        var completadas = 0
        var errores = 0

        for (item in marcados) {
            val apiService = RetrofitClient.getApiService()
            apiService.registrarAsistencia(
                idActividad = actividadSeleccionada.idActividad,
                idAsociado = item.asociado.idAsociado,
                estado = item.estadoSeleccionado
            ).enqueue(object : Callback<RespuestaBase> {
                override fun onResponse(call: Call<RespuestaBase>, response: Response<RespuestaBase>) {
                    completadas++
                    if (!response.isSuccessful) {
                        errores++
                    }

                    if (completadas == marcados.size) {
                        btnGuardarAsistencia.isEnabled = true
                        if (errores == 0) {
                            Toast.makeText(this@AsistenciaActivity, "¡Asistencias guardadas con éxito!", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this@AsistenciaActivity, "Guardado completado con $errores errores.", Toast.LENGTH_LONG).show()
                        }
                    }
                }

                override fun onFailure(call: Call<RespuestaBase>, t: Throwable) {
                    completadas++
                    errores++
                    if (completadas == marcados.size) {
                        btnGuardarAsistencia.isEnabled = true
                        Toast.makeText(this@AsistenciaActivity, "Error de red al guardar asistencias.", Toast.LENGTH_LONG).show()
                    }
                }
            })
        }
    }

    private fun mostrarDialogoAgregarActividad() {
        val view = layoutInflater.inflate(R.layout.dialog_agregar_actividad, null)
        val etNombre = view.findViewById<EditText>(R.id.etNombreActividad)
        val etFecha = view.findViewById<EditText>(R.id.etFechaActividad)
        val etMultaFalta = view.findViewById<EditText>(R.id.etMultaFalta)
        val etMultaTardanza = view.findViewById<EditText>(R.id.etMultaTardanza)

        // Prellenamos la fecha actual en formato latino (DD/MM/YYYY)
        val fechaHoyUI = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        etFecha.setText(fechaHoyUI)

        // Desplegar selector de calendario al tocar la casilla de fecha
        etFecha.isFocusable = false
        etFecha.isClickable = true
        etFecha.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    val calSelected = Calendar.getInstance()
                    calSelected.set(year, month, day)
                    etFecha.setText(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(calSelected.time))
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("➕ Nueva Actividad")
            .setView(view)
            .setPositiveButton("Guardar", null)
            .setNegativeButton("Cancelar") { d, _ -> d.dismiss() }
            .create()
            .apply {
                window?.setBackgroundDrawableResource(R.drawable.bg_dialog_redondeado)
                show()
                getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    val nombre = etNombre.text.toString().trim()
                    val fechaIngresadaUI = etFecha.text.toString().trim()
                    val multaFalta = etMultaFalta.text.toString().trim().toDoubleOrNull() ?: 0.0
                    val multaTardanza = etMultaTardanza.text.toString().trim().toDoubleOrNull() ?: 0.0

                    if (nombre.isEmpty() || fechaIngresadaUI.isEmpty()) {
                        Toast.makeText(this@AsistenciaActivity, "Error: Nombre y fecha requeridos", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    val fechaParaBD = formatearFechaParaBD(fechaIngresadaUI)
                    guardarNuevaActividad(nombre, fechaParaBD, multaFalta, multaTardanza, this)
                }
            }
    }

    private fun mostrarDialogoEditarActividad(actividad: Actividad) {
        val view = layoutInflater.inflate(R.layout.dialog_agregar_actividad, null)
        val etNombre = view.findViewById<EditText>(R.id.etNombreActividad)
        val etFecha = view.findViewById<EditText>(R.id.etFechaActividad)
        val etMultaFalta = view.findViewById<EditText>(R.id.etMultaFalta)
        val etMultaTardanza = view.findViewById<EditText>(R.id.etMultaTardanza)

        etNombre.setText(actividad.nombreActividad)
        etFecha.setText(formatearFechaParaMostrar(actividad.fechaActividad))
        etMultaFalta.setText(actividad.multaPorFalta.toString())
        etMultaTardanza.setText(actividad.multaPorTardanza.toString())

        etFecha.isFocusable = false
        etFecha.isClickable = true
        etFecha.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    val calSelected = Calendar.getInstance()
                    calSelected.set(year, month, day)
                    etFecha.setText(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(calSelected.time))
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("✏️ Editar Actividad")
            .setView(view)
            .setPositiveButton("Actualizar", null)
            .setNegativeButton("Cancelar") { d, _ -> d.dismiss() }
            .create()

        dialog.show()
        dialog.window?.setBackgroundDrawableResource(R.drawable.bg_dialog_redondeado)

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val nombre = etNombre.text.toString().trim()
            val fechaIngresadaUI = etFecha.text.toString().trim()
            val multaFalta = etMultaFalta.text.toString().trim().toDoubleOrNull() ?: 0.0
            val multaTardanza = etMultaTardanza.text.toString().trim().toDoubleOrNull() ?: 0.0

            if (nombre.isEmpty() || fechaIngresadaUI.isEmpty()) {
                Toast.makeText(this, "Por favor complete nombre y fecha", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val fechaParaBD = formatearFechaParaBD(fechaIngresadaUI)

            RetrofitClient.getApiService().actualizarActividad(
                idActividad = actividad.idActividad,
                nombreActividad = nombre,
                fechaActividad = fechaParaBD,
                multaPorFalta = multaFalta,
                multaPorTardanza = multaTardanza
            ).enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@AsistenciaActivity, "Actividad actualizada con éxito", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        descargarActividadesDespues() // Refrescamos las actividades en el Spinner
                    } else {
                        Toast.makeText(this@AsistenciaActivity, "Error al actualizar la actividad (Cód: ${response.code()})", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<Void>, t: Throwable) {
                    Toast.makeText(this@AsistenciaActivity, "Error de red: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun guardarNuevaActividad(nombreNombres: String, fecha: String, multaFalta: Double, multaTardanza: Double, dialog: AlertDialog) {
        RetrofitClient.getApiService().agregarActividad(nombreNombres, fecha, multaFalta, multaTardanza).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@AsistenciaActivity, "Actividad creada con éxito", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    descargarActividadesDespues()
                } else {
                    Toast.makeText(this@AsistenciaActivity, "Error al guardar la actividad", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Toast.makeText(this@AsistenciaActivity, "Error de red: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun configurarGestoDeslizar() {
        val callback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                rvAsistencia.adapter?.notifyItemChanged(position)

                val actividadSeleccionada = spActividad.selectedItem as? Actividad
                if (actividadSeleccionada == null) {
                    Toast.makeText(this@AsistenciaActivity, "Seleccione una actividad primero", Toast.LENGTH_SHORT).show()
                    return
                }

                val datosActuales = adapter.obtenerDatosAsistencia()
                if (position != RecyclerView.NO_POSITION && position < datosActuales.size) {
                    val item = datosActuales[position]
                    consultarYMostrarMulta(item, actividadSeleccionada.idActividad, actividadSeleccionada.nombreActividad)
                }
            }
        }
        ItemTouchHelper(callback).attachToRecyclerView(rvAsistencia)
    }

    private fun consultarYMostrarMulta(item: AsistenciaItem, idActividad: Int, nombreActividad: String) {
        val idAsociado = item.asociado.idAsociado
        val nombreCompleto = "${item.asociado.nombres} ${item.asociado.apellidoPaterno} ${item.asociado.apellidoMaterno}"

        tvEstadoCarga.text = "Consultando multa de $nombreCompleto..."

        RetrofitClient.getApiService().obtenerCuotasAsociado(idAsociado, 3).enqueue(object : Callback<List<DeudaPendiente>> {
            override fun onResponse(call: Call<List<DeudaPendiente>>, response: Response<List<DeudaPendiente>>) {
                tvEstadoCarga.text = "Lista cargada."
                if (response.isSuccessful && response.body() != null) {
                    val cuotas = response.body()!!
                    val cuotaMulta = cuotas.find { it.idActividad == idActividad }
                    if (cuotaMulta != null) {
                        mostrarDialogoDetalleCuotaMulta(cuotaMulta, item, idActividad, nombreActividad, nombreCompleto)
                    } else {
                        mostrarDialogoSinMulta(nombreCompleto, nombreActividad)
                    }
                } else {
                    mostrarDialogoSinMulta(nombreCompleto, nombreActividad)
                }
            }

            override fun onFailure(call: Call<List<DeudaPendiente>>, t: Throwable) {
                tvEstadoCarga.text = "Lista cargada."
                Toast.makeText(this@AsistenciaActivity, "Error al consultar multa: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun mostrarDialogoSinMulta(nombreSocio: String, nombreActividad: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle("ℹ️ Sin Multa Registrada")
            .setMessage("El integrante:\n$nombreSocio\n\nNo registra ninguna multa en la actividad:\n📌 $nombreActividad")
            .setPositiveButton("Aceptar") { d, _ -> d.dismiss() }
            .show()
    }

    private fun mostrarDialogoDetalleCuotaMulta(cuota: DeudaPendiente, item: AsistenciaItem, idActividad: Int, nombreActividad: String, nombreSocio: String) {
        val mensaje = "Integrante: $nombreSocio\n" +
                "Actividad: $nombreActividad\n" +
                "Concepto: ${cuota.concepto}\n" +
                "Monto Original: S/ ${cuota.montoOriginal ?: 0.0}\n" +
                "Descuento: S/ ${cuota.descuento ?: 0.0}\n" +
                "Monto Total: S/ ${cuota.montoTotal ?: 0.0}\n" +
                "Monto Pagado: S/ ${cuota.montoPagado ?: 0.0}\n" +
                "Saldo Pendiente: S/ ${cuota.monto}\n" +
                "Estado: ${cuota.estado ?: "PENDIENTE"}"

        MaterialAlertDialogBuilder(this)
            .setTitle("🔴 Detalle de Multa")
            .setMessage(mensaje)
            .setPositiveButton("Editar Descuento") { _, _ ->
                mostrarDialogoEditarDescuentoCuota(cuota, item, idActividad, nombreActividad)
            }
            .setNegativeButton("Cerrar") { d, _ -> d.dismiss() }
            .show()
    }

    private fun mostrarDialogoEditarDescuentoCuota(cuota: DeudaPendiente, item: AsistenciaItem, idActividad: Int, nombreActividad: String) {
        val view = layoutInflater.inflate(R.layout.dialog_editar_descuento, null)
        val etDescuento = view.findViewById<TextInputEditText>(R.id.etDescuentoDialog)
        etDescuento.setText(cuota.descuento?.toString() ?: "0.00")

        MaterialAlertDialogBuilder(this)
            .setTitle("✏️ Modificación Especial (Descuento)")
            .setView(view)
            .setPositiveButton("Aplicar") { _, _ ->
                val nuevoDescuentoStr = etDescuento.text.toString().trim()
                val nuevoDescuento = nuevoDescuentoStr.toDoubleOrNull() ?: 0.0

                RetrofitClient.getApiService().registrarAsistencia(
                    idActividad = idActividad,
                    idAsociado = item.asociado.idAsociado,
                    estado = item.estadoSeleccionado,
                    montoMulta = cuota.montoOriginal,
                    descuento = nuevoDescuento
                ).enqueue(object : Callback<RespuestaBase> {
                    override fun onResponse(call: Call<RespuestaBase>, response: Response<RespuestaBase>) {
                        if (response.isSuccessful && (response.body()?.isSuccess == true || response.body()?.success == true || response.body()?.status == "success")) {
                            Toast.makeText(this@AsistenciaActivity, "¡Descuento aplicado con éxito!", Toast.LENGTH_SHORT).show()
                            consultarYMostrarMulta(item, idActividad, nombreActividad)
                        } else {
                            Toast.makeText(this@AsistenciaActivity, "Error al actualizar descuento", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<RespuestaBase>, t: Throwable) {
                        Toast.makeText(this@AsistenciaActivity, "Error de red: ${t.message}", Toast.LENGTH_SHORT).show()
                    }
                })
            }
            .setNegativeButton("Cancelar") { d, _ -> d.dismiss() }
            .show()
    }
}
