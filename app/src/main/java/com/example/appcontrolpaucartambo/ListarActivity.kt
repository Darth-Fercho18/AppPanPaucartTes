package com.example.appcontrolpaucartambo

import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale

class ListarActivity : AppCompatActivity() {

    private lateinit var rvAsociados: RecyclerView
    private lateinit var spFiltroTipo: Spinner
    private lateinit var etBuscarSocio: TextInputEditText

    private var listaCompleta: List<Asociado> = emptyList()
    private var listaTiposSocio: List<TipoSocio> = emptyList()
    private var listaFiltradaActual: List<Asociado> = emptyList()

    private var scrollState: Parcelable? = null
    private var restaurarScrollPendiente = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_listar)

        rvAsociados = findViewById(R.id.rvAsociados)
        spFiltroTipo = findViewById(R.id.spFiltroTipo)
        etBuscarSocio = findViewById(R.id.etBuscarSocio)

        rvAsociados.layoutManager = LinearLayoutManager(this)

        val btnAgregarAsociado = findViewById<ImageButton>(R.id.btnAgregarAsociado)
        btnAgregarAsociado.setOnClickListener {
            val intent = Intent(this, AgregarAsociadoActivity::class.java)
            startActivity(intent)
        }

        etBuscarSocio.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filtrarLista(spFiltroTipo.selectedItemPosition)
            }
        })

        configurarSwipeEditar()
        cargarTiposSocio()
    }

    override fun onPause() {
        super.onPause()
        scrollState = rvAsociados.layoutManager?.onSaveInstanceState()
    }

    override fun onResume() {
        super.onResume()
        restaurarScrollPendiente = true
        cargarDatosDesdeApi()
    }

    private fun configurarSwipeEditar() {
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                rvAsociados.adapter?.notifyItemChanged(position)

                if (position != RecyclerView.NO_POSITION && position < listaFiltradaActual.size) {
                    val asociadoAEditar = listaFiltradaActual[position]
                    val intent = Intent(this@ListarActivity, AgregarAsociadoActivity::class.java)
                    intent.putExtra("ASOCIADO_EDITAR", asociadoAEditar)
                    startActivity(intent)
                }
            }
        }

        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(rvAsociados)
    }

    private fun cargarTiposSocio() {
        val apiService = RetrofitClient.getApiService()

        apiService.obtenerTiposSocio().enqueue(object : Callback<List<TipoSocio>> {
            override fun onResponse(call: Call<List<TipoSocio>>, response: Response<List<TipoSocio>>) {
                if (response.isSuccessful && response.body() != null) {
                    listaTiposSocio = response.body()!!

                    val opcionesSpinner = mutableListOf("TODOS")
                    opcionesSpinner.addAll(listaTiposSocio.map { it.nombre_tipo })

                    val adapter = ArrayAdapter(
                        this@ListarActivity,
                        R.layout.spinner_item_selected,
                        opcionesSpinner
                    )
                    adapter.setDropDownViewResource(R.layout.spinner_item_dropdown)
                    spFiltroTipo.adapter = adapter

                    spFiltroTipo.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                            filtrarLista(position)
                        }

                        override fun onNothingSelected(parent: AdapterView<*>?) {}
                    }
                } else {
                    Toast.makeText(this@ListarActivity, "Error al obtener tipos de integrante", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<TipoSocio>>, t: Throwable) {
                Toast.makeText(this@ListarActivity, "Error de red al cargar tipos: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun cargarDatosDesdeApi() {
        val apiService = RetrofitClient.getApiService()

        apiService.getAsociados().enqueue(object : Callback<List<Asociado>> {
            override fun onResponse(call: Call<List<Asociado>>, response: Response<List<Asociado>>) {
                if (response.isSuccessful) {
                    listaCompleta = response.body()?.sortedBy { it.idAsociado } ?: emptyList()
                    filtrarLista(spFiltroTipo.selectedItemPosition)
                } else {
                    Toast.makeText(this@ListarActivity, "Error: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<Asociado>>, t: Throwable) {
                Toast.makeText(this@ListarActivity, "Error de red: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun filtrarLista(posicion: Int) {
        val query = etBuscarSocio.text.toString().trim().lowercase(Locale.ROOT)

        val filtradosPorTipo = if (posicion == 0) {
            listaCompleta
        } else {
            if (listaTiposSocio.isNotEmpty() && (posicion - 1) < listaTiposSocio.size) {
                val idTipoSeleccionado = listaTiposSocio[posicion - 1].id_tipo
                listaCompleta.filter { it.idTipo == idTipoSeleccionado }
            } else {
                listaCompleta
            }
        }

        listaFiltradaActual = if (query.isEmpty()) {
            filtradosPorTipo
        } else {
            filtradosPorTipo.filter { socio ->
                val nombreCompleto = "${socio.nombres} ${socio.apellidoPaterno} ${socio.apellidoMaterno}".lowercase(Locale.ROOT)
                val dni = socio.dni.lowercase(Locale.ROOT)
                nombreCompleto.contains(query) || dni.contains(query)
            }
        }

        rvAsociados.adapter = AsociadoAdapter(listaFiltradaActual) { socioSeleccionado ->
            Toast.makeText(this@ListarActivity, "Módulo de cobro para: ${socioSeleccionado.nombres}", Toast.LENGTH_SHORT).show()
        }

        if (restaurarScrollPendiente && scrollState != null) {
            rvAsociados.layoutManager?.onRestoreInstanceState(scrollState)
            scrollState = null
            restaurarScrollPendiente = false
        }
    }
}
