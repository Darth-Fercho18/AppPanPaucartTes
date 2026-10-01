package com.example.appcontrolpaucartambo

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AgregarAsociadoActivity : AppCompatActivity() {

    private lateinit var etDni: EditText
    private lateinit var etNombres: EditText
    private lateinit var etApellidoPaterno: EditText
    private lateinit var etApellidoMaterno: EditText
    private lateinit var spTipoSocioGuardar: Spinner
    private lateinit var etCelular: EditText
    private lateinit var etCorreo: EditText
    private lateinit var etDireccion: EditText
    private lateinit var btnGuardarAsociado: Button

    // Lista para guardar los tipos de socio recibidos de la BD
    private var listaTiposSocio: List<TipoSocio> = emptyList()

    // Objeto opcional para el modo de edición
    private var asociadoAEditar: Asociado? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_agregar_asociado)

        // Enlazar componentes de la vista
        etDni = findViewById(R.id.etDni)
        etNombres = findViewById(R.id.etNombres)
        etApellidoPaterno = findViewById(R.id.etApellidoPaterno)
        etApellidoMaterno = findViewById(R.id.etApellidoMaterno)
        spTipoSocioGuardar = findViewById(R.id.spTipoSocioGuardar)
        etCelular = findViewById(R.id.etCelular)
        etCorreo = findViewById(R.id.etCorreo)
        etDireccion = findViewById(R.id.etDireccion)
        btnGuardarAsociado = findViewById(R.id.btnGuardarAsociado)

        // Verificar si la pantalla fue abierta para editar un asociado
        asociadoAEditar = intent.getSerializableExtra("ASOCIADO_EDITAR") as? Asociado

        if (asociadoAEditar != null) {
            btnGuardarAsociado.text = "ACTUALIZAR ASOCIADO"
            cargarDatosEnFormulario(asociadoAEditar!!)
        }

        // Cargar los tipos de socio desde la BD
        cargarTiposSocio()

        // Acción al presionar el botón Guardar / Actualizar
        btnGuardarAsociado.setOnClickListener {
            if (asociadoAEditar != null) {
                actualizarAsociado()
            } else {
                guardarAsociado()
            }
        }
    }

    private fun cargarDatosEnFormulario(asociado: Asociado) {
        etDni.setText(asociado.dni)
        etNombres.setText(asociado.nombres)
        etApellidoPaterno.setText(asociado.apellidoPaterno)
        etApellidoMaterno.setText(asociado.apellidoMaterno)
        etCelular.setText(asociado.celular ?: "")
        etCorreo.setText(asociado.correo ?: "")
        etDireccion.setText(asociado.direccion ?: "")
    }

    private fun cargarTiposSocio() {
        val apiService = RetrofitClient.getApiService()

        apiService.obtenerTiposSocio().enqueue(object : Callback<List<TipoSocio>> {
            override fun onResponse(call: Call<List<TipoSocio>>, response: Response<List<TipoSocio>>) {
                if (response.isSuccessful && response.body() != null) {
                    listaTiposSocio = response.body()!!

                    // Extraer los nombres directos para el Spinner (SIN "TODOS")
                    val opciones = listaTiposSocio.map { it.nombre_tipo }

                    // Aplicar el adaptador usando tus diseños personalizados
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
                        }
                    }
                } else {
                    Toast.makeText(this@AgregarAsociadoActivity, "Error al cargar tipos de socio", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<TipoSocio>>, t: Throwable) {
                Toast.makeText(this@AgregarAsociadoActivity, "Error de red: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun guardarAsociado() {
        val dni = etDni.text.toString().trim()
        val nombres = etNombres.text.toString().trim()
        val apellidoPaterno = etApellidoPaterno.text.toString().trim()
        val apellidoMaterno = etApellidoMaterno.text.toString().trim()
        val celular = etCelular.text.toString().trim()
        val correo = etCorreo.text.toString().trim()
        val direccion = etDireccion.text.toString().trim()

        // Validaciones básicas
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
            Toast.makeText(this, "Seleccione un tipo de socio válido", Toast.LENGTH_SHORT).show()
            return
        }

        val idTipoSeleccionado = listaTiposSocio[posicionSeleccionada].id_tipo

        btnGuardarAsociado.isEnabled = false

        val apiService = RetrofitClient.getApiService()
        apiService.agregarAsociado(
            dni = dni,
            nombres = nombres,
            apellidoPaterno = apellidoPaterno,
            apellidoMaterno = apellidoMaterno,
            idTipo = idTipoSeleccionado,
            celular = if (celular.isNotEmpty()) celular else null,
            correo = if (correo.isNotEmpty()) correo else null,
            direccion = if (direccion.isNotEmpty()) direccion else null
        ).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                btnGuardarAsociado.isEnabled = true
                if (response.isSuccessful) {
                    Toast.makeText(this@AgregarAsociadoActivity, "¡Asociado registrado con éxito!", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@AgregarAsociadoActivity, "Error al guardar: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                btnGuardarAsociado.isEnabled = true
                Toast.makeText(this@AgregarAsociadoActivity, "Error de red: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun actualizarAsociado() {
        val idAsociado = asociadoAEditar?.idAsociado ?: return
        val dni = etDni.text.toString().trim()
        val nombres = etNombres.text.toString().trim()
        val apellidoPaterno = etApellidoPaterno.text.toString().trim()
        val apellidoMaterno = etApellidoMaterno.text.toString().trim()
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
            Toast.makeText(this, "Seleccione un tipo de socio válido", Toast.LENGTH_SHORT).show()
            return
        }

        val idTipoSeleccionado = listaTiposSocio[posicionSeleccionada].id_tipo

        btnGuardarAsociado.isEnabled = false

        val apiService = RetrofitClient.getApiService()
        apiService.actualizarAsociado(
            idAsociado = idAsociado,
            dni = dni,
            nombres = nombres,
            apellidoPaterno = apellidoPaterno,
            apellidoMaterno = apellidoMaterno,
            idTipo = idTipoSeleccionado,
            celular = if (celular.isNotEmpty()) celular else null,
            correo = if (correo.isNotEmpty()) correo else null,
            direccion = if (direccion.isNotEmpty()) direccion else null
        ).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                btnGuardarAsociado.isEnabled = true
                if (response.isSuccessful) {
                    Toast.makeText(this@AgregarAsociadoActivity, "¡Asociado actualizado con éxito!", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@AgregarAsociadoActivity, "Error al actualizar: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                btnGuardarAsociado.isEnabled = true
                Toast.makeText(this@AgregarAsociadoActivity, "Error de red: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}