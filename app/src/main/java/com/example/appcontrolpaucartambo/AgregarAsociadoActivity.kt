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
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AgregarAsociadoActivity : AppCompatActivity() {

    private lateinit var tvTituloFormulario: TextView
    private lateinit var etDni: EditText
    private lateinit var etNombres: EditText
    private lateinit var etApellidoPaterno: EditText
    private lateinit var etApellidoMaterno: EditText
    private lateinit var spSexo: Spinner
    private lateinit var etFechaNacimiento: EditText
    private lateinit var etAnioIngreso: EditText
    private lateinit var spTipoSocioGuardar: Spinner
    private lateinit var cardApoderado: View
    private lateinit var etNombreApoderado: EditText
    private lateinit var etCelularApoderado: EditText
    private lateinit var etCelular: EditText
    private lateinit var etCorreo: EditText
    private lateinit var etDireccion: EditText
    private lateinit var btnGuardarAsociado: Button

    // Lista para guardar los tipos de socio recibidos de la BD
    private var listaTiposSocio: List<TipoSocio> = emptyList()

    // Objeto opcional para el modo de edición
    private var asociadoAEditar: Asociado? = null

    // Variable para almacenar la fecha de nacimiento en formato YYYY-MM-DD para la BD
    private var fechaNacimientoBD: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_agregar_asociado)

        // Enlazar componentes de la vista
        tvTituloFormulario = findViewById(R.id.tvTituloFormulario)
        etDni = findViewById(R.id.etDni)
        etNombres = findViewById(R.id.etNombres)
        etApellidoPaterno = findViewById(R.id.etApellidoPaterno)
        etApellidoMaterno = findViewById(R.id.etApellidoMaterno)
        spSexo = findViewById(R.id.spSexo)
        etFechaNacimiento = findViewById(R.id.etFechaNacimiento)
        etAnioIngreso = findViewById(R.id.etAnioIngreso)
        spTipoSocioGuardar = findViewById(R.id.spTipoSocioGuardar)
        cardApoderado = findViewById(R.id.cardApoderado)
        etNombreApoderado = findViewById(R.id.etNombreApoderado)
        etCelularApoderado = findViewById(R.id.etCelularApoderado)
        etCelular = findViewById(R.id.etCelular)
        etCorreo = findViewById(R.id.etCorreo)
        etDireccion = findViewById(R.id.etDireccion)
        btnGuardarAsociado = findViewById(R.id.btnGuardarAsociado)

        // Configurar selector de fecha para Fecha de Nacimiento
        etFechaNacimiento.setOnClickListener {
            mostrarDatePickerNacimiento()
        }

        // Configurar Spinner de Sexo con opciones "Masculino" y "Femenino"
        val sexos = listOf("Masculino", "Femenino")
        val sexoAdapter = ArrayAdapter(this, R.layout.spinner_item_selected, sexos)
        sexoAdapter.setDropDownViewResource(R.layout.spinner_item_dropdown)
        spSexo.adapter = sexoAdapter

        // Verificar si la pantalla fue abierta para editar un asociado
        asociadoAEditar = intent.getSerializableExtra("ASOCIADO_EDITAR") as? Asociado

        if (asociadoAEditar != null) {
            tvTituloFormulario.text = "Editar Integrante"
            btnGuardarAsociado.text = "ACTUALIZAR INTEGRANTE"
            cargarDatosEnFormulario(asociadoAEditar!!)
        } else {
            tvTituloFormulario.text = "Registrar Nuevo Integrante"
            cardApoderado.visibility = View.GONE
        }

        // Cargar los tipos de socio desde la BD
        cargarTiposSocio()

        // Listener para controlar la visibilidad del Card de Apoderado según el tipo de socio seleccionado
        spTipoSocioGuardar.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position >= 0 && position < listaTiposSocio.size) {
                    val nombreTipo = listaTiposSocio[position].nombre_tipo.uppercase(Locale.ROOT)
                    if (nombreTipo.contains("NIÑO") || nombreTipo.contains("NIÑOS")) {
                        cardApoderado.visibility = View.VISIBLE
                    } else {
                        cardApoderado.visibility = View.GONE
                        etNombreApoderado.setText("")
                        etCelularApoderado.setText("")
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Acción al presionar el botón Guardar / Actualizar
        btnGuardarAsociado.setOnClickListener {
            if (asociadoAEditar != null) {
                actualizarAsociado()
            } else {
                guardarAsociado()
            }
        }
    }

    private fun mostrarDatePickerNacimiento() {
        val calendar = Calendar.getInstance()

        if (!fechaNacimientoBD.isNullOrEmpty() && fechaNacimientoBD!!.length >= 10) {
            try {
                val partes = fechaNacimientoBD!!.take(10).split("-")
                if (partes.size == 3) {
                    val y = partes[0].toInt()
                    val m = partes[1].toInt() - 1
                    val d = partes[2].toInt()
                    calendar.set(y, m, d)
                }
            } catch (_: Exception) {}
        }

        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val calSel = Calendar.getInstance().apply { set(year, month, dayOfMonth) }
                val fmtBD = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val fmtDisplay = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

                fechaNacimientoBD = fmtBD.format(calSel.time)
                etFechaNacimiento.setText(fmtDisplay.format(calSel.time))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun cargarDatosEnFormulario(asociado: Asociado) {
        etDni.setText(asociado.dni)
        etNombres.setText(asociado.nombres)
        etApellidoPaterno.setText(asociado.apellidoPaterno)
        etApellidoMaterno.setText(asociado.apellidoMaterno)

        if (!asociado.fechaNacimiento.isNullOrBlank()) {
            fechaNacimientoBD = asociado.fechaNacimiento.take(10)
            try {
                val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val date = parser.parse(fechaNacimientoBD!!)
                if (date != null) {
                    val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    etFechaNacimiento.setText(formatter.format(date))
                } else {
                    etFechaNacimiento.setText(asociado.fechaNacimiento)
                }
            } catch (_: Exception) {
                etFechaNacimiento.setText(asociado.fechaNacimiento)
            }
        }

        etAnioIngreso.setText(asociado.anioIngreso?.toString() ?: "")
        etNombreApoderado.setText(asociado.nombreApoderado ?: "")
        etCelularApoderado.setText(asociado.celularApoderado ?: "")
        etCelular.setText(asociado.celular ?: "")
        etCorreo.setText(asociado.correo ?: "")
        etDireccion.setText(asociado.direccion ?: "")

        val sexoUpper = asociado.sexo?.uppercase() ?: ""
        if (sexoUpper.startsWith("F") || sexoUpper.contains("FEMENINO") || sexoUpper.contains("MUJER")) {
            spSexo.setSelection(1)
        } else {
            spSexo.setSelection(0)
        }
    }

    private fun cargarTiposSocio() {
        val apiService = RetrofitClient.getApiService()

        apiService.obtenerTiposSocio().enqueue(object : Callback<List<TipoSocio>> {
            override fun onResponse(call: Call<List<TipoSocio>>, response: Response<List<TipoSocio>>) {
                if (response.isSuccessful && response.body() != null) {
                    listaTiposSocio = response.body()!!

                    // Extraer los nombres directos para el Spinner
                    val opciones = listaTiposSocio.map { it.nombre_tipo }

                    // Aplicar el adaptador
                    val adapter = ArrayAdapter(
                        this@AgregarAsociadoActivity,
                        R.layout.spinner_item_selected,
                        opciones
                    )
                    adapter.setDropDownViewResource(R.layout.spinner_item_dropdown)
                    spTipoSocioGuardar.adapter = adapter

                    // Si estamos en modo edición, seleccionar el tipo de socio del asociado
                    if (asociadoAEditar != null) {
                        val pos = listaTiposSocio.indexOfFirst { it.id_tipo == asociadoAEditar!!.idTipo }
                        if (pos != -1) {
                            spTipoSocioGuardar.setSelection(pos)
                            val nombreTipo = listaTiposSocio[pos].nombre_tipo.uppercase(Locale.ROOT)
                            if (nombreTipo.contains("NIÑO") || nombreTipo.contains("NIÑOS")) {
                                cardApoderado.visibility = View.VISIBLE
                            } else {
                                cardApoderado.visibility = View.GONE
                            }
                        }
                    }
                } else {
                    Toast.makeText(this@AgregarAsociadoActivity, "Error al cargar tipos de integrante", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<TipoSocio>>, t: Throwable) {
                Toast.makeText(this@AgregarAsociadoActivity, "Error de red: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun guardarAsociado() {
        procesarGuardado(null, "¡Integrante registrado con éxito!")
    }

    private fun actualizarAsociado() {
        val idAsociado = asociadoAEditar?.idAsociado ?: return
        procesarGuardado(idAsociado, "¡Integrante actualizado con éxito!")
    }

    private fun procesarGuardado(idAsociado: Int?, mensajeExito: String) {
        val dni = etDni.text.toString().trim()
        val nombres = etNombres.text.toString().trim()
        val apellidoPaterno = etApellidoPaterno.text.toString().trim()
        val apellidoMaterno = etApellidoMaterno.text.toString().trim()
        val sexo = if (spSexo.selectedItemPosition == 1) "F" else "M"
        val fechaNacimiento = fechaNacimientoBD
        val anioIngreso = etAnioIngreso.text.toString().trim().toIntOrNull()

        val posicionTipo = spTipoSocioGuardar.selectedItemPosition
        val esNiño = if (posicionTipo >= 0 && posicionTipo < listaTiposSocio.size) {
            val nombreTipo = listaTiposSocio[posicionTipo].nombre_tipo.uppercase(Locale.ROOT)
            nombreTipo.contains("NIÑO") || nombreTipo.contains("NIÑOS")
        } else {
            false
        }

        val nombreApoderado = if (esNiño) etNombreApoderado.text.toString().trim().ifEmpty { null } else null
        val celularApoderado = if (esNiño) etCelularApoderado.text.toString().trim().ifEmpty { null } else null

        val celular = etCelular.text.toString().trim()
        val correo = etCorreo.text.toString().trim()
        val direccion = etDireccion.text.toString().trim()

        if (dni.length != 8) {
            etDni.error = "El DNI debe tener 8 dígitos"
            return
        }
        if (nombres.isEmpty()) {
            etNombres.error = "Ingrese el nombre"
            return
        }
        if (apellidoPaterno.isEmpty()) {
            etApellidoPaterno.error = "Ingrese el apellido paterno"
            return
        }

        val posicionSeleccionada = spTipoSocioGuardar.selectedItemPosition
        if (posicionSeleccionada == -1 || listaTiposSocio.isEmpty()) {
            Toast.makeText(this, "Seleccione un tipo de integrante válido", Toast.LENGTH_SHORT).show()
            return
        }

        val idTipoSeleccionado = listaTiposSocio[posicionSeleccionada].id_tipo

        btnGuardarAsociado.isEnabled = false

        val apiService = RetrofitClient.getApiService()
        apiService.guardarAsociado(
            idAsociado = idAsociado,
            dni = dni,
            nombres = nombres,
            apellidoPaterno = apellidoPaterno,
            apellidoMaterno = apellidoMaterno,
            sexo = sexo,
            fechaNacimiento = fechaNacimiento,
            anioIngreso = anioIngreso,
            idTipo = idTipoSeleccionado,
            celular = celular.ifEmpty { null },
            correo = correo.ifEmpty { null },
            direccion = direccion.ifEmpty { null },
            nombreApoderado = nombreApoderado,
            celularApoderado = celularApoderado
        ).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                btnGuardarAsociado.isEnabled = true
                if (response.isSuccessful) {
                    Toast.makeText(this@AgregarAsociadoActivity, mensajeExito, Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@AgregarAsociadoActivity, "Error al procesar: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                btnGuardarAsociado.isEnabled = true
                Toast.makeText(this@AgregarAsociadoActivity, "Error de red: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
