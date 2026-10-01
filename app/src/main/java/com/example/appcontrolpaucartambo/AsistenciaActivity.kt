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
    private lateinit var rvAsistencia: RecyclerView
    private lateinit var btnGuardarAsistencia: Button
    private lateinit var btnAgregarActividad: Button
    private lateinit var btnEditarActividad: MaterialButton
    private lateinit var tvEstadoCarga: TextView

    private lateinit var adapter: AsistenciaAdapter
    private var listaActividades: List<Actividad> = emptyList()
    private var listaTodosLosAsociados: List<Asociado> = emptyList()

    // Bandera vital para detener el disparo automático de los Spinners de Android
    private var spinnersListos = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_asistencia)

        spActividad = findViewById(R.id.spActividad)
        spFiltroTipoAsistencia = findViewById(R.id.spFiltroTipoAsistencia)
        rvAsistencia = findViewById(R.id.rvAsistencia)
        btnGuardarAsistencia = findViewById(R.id.btnGuardarAsistencia)
        btnAgregarActividad = findViewById(R.id.btnAgregarActividad)
        btnEditarActividad = findViewById(R.id.btnEditarActividad)
        tvEstadoCarga = findViewById(R.id.tvEstadoCarga)

        rvAsistencia.layoutManager = LinearLayoutManager(this)
        adapter = AsistenciaAdapter(emptyList())
        rvAsistencia.adapter = adapter

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
                    tvEstadoCarga.text = "Error HTTP al cargar socios: ${response.code()}"
                }
            }

            override fun onFailure(call: Call<List<Asociado>>, t: Throwable) {
                tvEstadoCarga.text = "Fallo de red en socios: ${t.message}"
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
                    }

                    // SOLO CUANDO TODO ESTÁ DESCARGADO Y DIBUJADO, ACTIVAMOS LOS LECTORES
                    spinnersListos = true
                    configurarFiltroSocio()

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

        if (actividadSeleccionada != null && posicionFiltro > 0) {
            val tipoSocio = spFiltroTipoAsistencia.selectedItem.toString()
            tvEstadoCarga.text = "Buscando registro anterior de $tipoSocio..."

            cargarSoloAsistencias(tipoSocio, actividadSeleccionada.idActividad)
        } else {
            adapter.actualizarLista(emptyList())
            tvEstadoCarga.text = "Seleccione una actividad y un tipo de socio."
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

                val listaParaPantalla = asociadosFiltrados.map { asociado ->
                    val registroEncontrado = asistenciasPrevias.find { it.idAsociado == asociado.idAsociado }
                    val estadoInicial = registroEncontrado?.estado ?: "Seleccionar..."
                    AsistenciaItem(asociado, estadoInicial)
                }

                adapter.actualizarLista(listaParaPantalla)
                tvEstadoCarga.text = "Lista de $tipoSocio (${listaParaPantalla.size} personas)"
            }

            override fun onFailure(call: Call<List<Asistencia>>, t: Throwable) {
                tvEstadoCarga.text = "Error al conectar por asistencias: ${t.message}"
            }
        })
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
            Toast.makeText(this, "No ha marcado la asistencia de ningún socio.", Toast.LENGTH_SHORT).show()
            return
        }

        val payload = marcados.map {
            Asistencia(
                idActividad = actividadSeleccionada.idActividad,
                idAsociado = it.asociado.idAsociado,
                estado = it.estadoSeleccionado
            )
        }

        RetrofitClient.getApiService().guardarAsistencias(payload).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@AsistenciaActivity, "Asistencia guardada con éxito", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this@AsistenciaActivity, "Error al guardar en el servidor", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<Void>, t: Throwable) {
                Toast.makeText(this@AsistenciaActivity, "Error de red: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
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
                { _, year, month, dayOfMonth ->
                    val fechaSeleccionada = String.format(Locale.getDefault(), "%02d/%02d/%04d", dayOfMonth, month + 1, year)
                    etFecha.setText(fechaSeleccionada)
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("✨ Nueva Actividad")
            .setView(view)
            .setPositiveButton("Guardar", null)
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

            // Convertimos la fecha (DD/MM/YYYY) a formato BD (YYYY-MM-DD) antes de enviar
            val fechaParaBD = formatearFechaParaBD(fechaIngresadaUI)

            guardarNuevaActividad(nombre, fechaParaBD, multaFalta, multaTardanza, dialog)
        }
    }

    // --- NUEVO DIÁLOGO PARA EDITAR LA ACTIVIDAD SELECCIONADA ---
    private fun mostrarDialogoEditarActividad(actividad: Actividad) {
        val view = layoutInflater.inflate(R.layout.dialog_agregar_actividad, null)
        val etNombre = view.findViewById<EditText>(R.id.etNombreActividad)
        val etFecha = view.findViewById<EditText>(R.id.etFechaActividad)
        val etMultaFalta = view.findViewById<EditText>(R.id.etMultaFalta)
        val etMultaTardanza = view.findViewById<EditText>(R.id.etMultaTardanza)

        // Prellenamos los campos con los datos actuales
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
                { _, year, month, dayOfMonth ->
                    val fechaSeleccionada = String.format(Locale.getDefault(), "%02d/%02d/%04d", dayOfMonth, month + 1, year)
                    etFecha.setText(fechaSeleccionada)
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

    private fun guardarNuevaActividad(nombre: String, fecha: String, multaFalta: Double, multaTardanza: Double, dialog: AlertDialog) {
        RetrofitClient.getApiService().agregarActividad(nombre, fecha, multaFalta, multaTardanza).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@AsistenciaActivity, "Actividad creada con éxito", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    descargarActividadesDespues() // Refrescamos las actividades
                } else {
                    Toast.makeText(this@AsistenciaActivity, "Error al guardar la actividad", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<Void>, t: Throwable) {
                Toast.makeText(this@AsistenciaActivity, "Error de red: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // --- CONFIGURACIÓN DEL GESTO SWIPE ---
    private fun configurarGestoDeslizar() {
        val swipeHandler = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val posicion = viewHolder.bindingAdapterPosition

                if (posicion != RecyclerView.NO_POSITION) {
                    val item = adapter.obtenerDatosAsistencia()[posicion]
                    val estado = item.estadoSeleccionado.trim().uppercase()

                    val actividadSeleccionada = spActividad.selectedItem as? Actividad
                    val nombreActividad = actividadSeleccionada?.toString() ?: "Actividad actual"
                    val idActividad = actividadSeleccionada?.idActividad ?: 0

                    when (estado) {
                        "ASISTIO", "DISPENSA MEDICA", "DISPENSA EXTRAORDINARIA" -> {
                            mostrarDialogoSinMulta(item, nombreActividad)
                        }
                        "FALTO", "TARDANZA", "JUSTIFICO" -> {
                            consultarYMostrarMulta(item, idActividad, nombreActividad)
                        }
                        else -> {
                            Toast.makeText(this@AsistenciaActivity, "Selecciona primero un estado de asistencia", Toast.LENGTH_SHORT).show()
                        }
                    }

                    // Regresamos suavemente la tarjeta deslizada
                    adapter.notifyItemChanged(posicion)
                }
            }
        }

        val itemTouchHelper = ItemTouchHelper(swipeHandler)
        itemTouchHelper.attachToRecyclerView(rvAsistencia)
    }

    // 1. Diálogo Sin Multa
    private fun mostrarDialogoSinMulta(item: AsistenciaItem, nombreActividad: String) {
        val nombreCompleto = "${item.asociado.nombres} ${item.asociado.apellidoPaterno} ${item.asociado.apellidoMaterno}"

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("🟢 Detalles")
            .setMessage("El asociado:\n$nombreCompleto\n\nNO REGISTRA MULTA en:\n📌 $nombreActividad")
            .setPositiveButton("Aceptar", null)
            .create()

        dialog.show()
        dialog.window?.setBackgroundDrawableResource(R.drawable.bg_dialog_redondeado)
    }

    // 2. Diálogo Detalle de Multa
    private fun consultarYMostrarMulta(item: AsistenciaItem, idActividad: Int, nombreActividad: String) {
        val idAsociado = item.asociado.idAsociado
        val nombreCompleto = "${item.asociado.nombres} ${item.asociado.apellidoPaterno} ${item.asociado.apellidoMaterno}"

        if (idActividad == 0) {
            Toast.makeText(this, "Seleccione una actividad válida primero.", Toast.LENGTH_SHORT).show()
            return
        }

        RetrofitClient.getApiService().obtenerMulta(idAsociado, idActividad).enqueue(object : Callback<RespuestaMulta> {
            override fun onResponse(call: Call<RespuestaMulta>, response: Response<RespuestaMulta>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val multa = response.body()!!.multa!!

                    // Convertimos la fecha devuelta por la BD a formato DD/MM/YYYY
                    val fechaPagoFormateada = formatearFechaParaMostrar(multa.fechaPago)

                    val detalleMensaje = """
                        👤 Asociado: $nombreCompleto
                        📌 Actividad: $nombreActividad
                        🏷️ Estado Asistencia: ${item.estadoSeleccionado}
                        
                        ────────────────────────
                        💰 Detalle de la Multa:
                        • Monto Total: S/ ${multa.montoTotal}
                        • Monto Pagado: S/ ${multa.montoPagado}
                        • Estado de Pago: ${multa.estado}
                        ${if (fechaPagoFormateada.isNotEmpty()) "• Fecha de Pago: $fechaPagoFormateada" else ""}
                    """.trimIndent()

                    val dialog = MaterialAlertDialogBuilder(this@AsistenciaActivity)
                        .setTitle("🔴 Detalle de Multa")
                        .setMessage(detalleMensaje)
                        .setPositiveButton("Cerrar", null)
                        .setNeutralButton("Editar Descuento") { _, _ ->
                            mostrarDialogoEditarDescuento(multa, item, idActividad, nombreActividad)
                        }
                        .create()

                    dialog.show()
                    dialog.window?.setBackgroundDrawableResource(R.drawable.bg_dialog_redondeado)

                } else {
                    val dialog = MaterialAlertDialogBuilder(this@AsistenciaActivity)
                        .setTitle("⚠️ Multa Pendiente")
                        .setMessage("El asociado $nombreCompleto tiene el estado de '${item.estadoSeleccionado}'.\n\nGuarda la asistencia para generar el registro formal de la multa en el sistema.")
                        .setPositiveButton("Entendido", null)
                        .create()

                    dialog.show()
                    dialog.window?.setBackgroundDrawableResource(R.drawable.bg_dialog_redondeado)
                }
            }

            override fun onFailure(call: Call<RespuestaMulta>, t: Throwable) {
                Toast.makeText(this@AsistenciaActivity, "Error al consultar multa: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // 3. Diálogo Editar Descuento
    private fun mostrarDialogoEditarDescuento(multa: Multa, item: AsistenciaItem, idActividad: Int, nombreActividad: String) {
        val view = layoutInflater.inflate(R.layout.dialog_editar_descuento, null)
        val etDescuento = view.findViewById<TextInputEditText>(R.id.etDescuentoDialog)

        etDescuento.setText(multa.descuento.toString())

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("✏️ Editar Descuento")
            .setMessage("Ingrese el descuento para ${item.asociado.nombres}:")
            .setView(view)
            .setPositiveButton("Guardar") { d, _ ->
                val nuevoDescuentoTexto = etDescuento.text.toString().trim()
                val nuevoDescuento = nuevoDescuentoTexto.toDoubleOrNull()

                if (nuevoDescuento == null || nuevoDescuento < 0) {
                    Toast.makeText(this, "Monto de descuento no válido", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                RetrofitClient.getApiService().actualizarDescuentoMulta(multa.idMulta, nuevoDescuento)
                    .enqueue(object : Callback<RespuestaBase> {
                        override fun onResponse(call: Call<RespuestaBase>, response: Response<RespuestaBase>) {
                            if (response.isSuccessful && response.body()?.success == true) {
                                Toast.makeText(this@AsistenciaActivity, "Descuento actualizado con éxito", Toast.LENGTH_SHORT).show()
                                d.dismiss()
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
            .create()

        dialog.show()
        dialog.window?.setBackgroundDrawableResource(R.drawable.bg_dialog_redondeado)
    }
}