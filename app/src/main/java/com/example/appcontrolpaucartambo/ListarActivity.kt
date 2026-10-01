package com.example.appcontrolpaucartambo

import android.content.Intent
import android.os.Bundle
import android.util.Log
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
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ListarActivity : AppCompatActivity() {

    private lateinit var rvAsociados: RecyclerView
    private lateinit var spFiltroTipo: Spinner

    // Listas para manejar los datos completos, los tipos de socio y la lista filtrada actual
    private var listaCompleta: List<Asociado> = emptyList()
    private var listaTiposSocio: List<TipoSocio> = emptyList()

    // Variable para rastrear la lista filtrada que se está mostrando en el RecyclerView
    private var listaFiltradaActual: List<Asociado> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_listar)

        rvAsociados = findViewById(R.id.rvAsociados)
        spFiltroTipo = findViewById(R.id.spFiltroTipo)

        rvAsociados.layoutManager = LinearLayoutManager(this)

        val btnAgregarAsociado = findViewById<ImageButton>(R.id.btnAgregarAsociado)
        btnAgregarAsociado.setOnClickListener {
            val intent = Intent(this, AgregarAsociadoActivity::class.java)
            startActivity(intent)
        }

        // --- GESTOR DE DESLIZAMIENTO PARA EDITAR ---
        configurarSwipeEditar()

        // Cargar primero los tipos de socio y luego los asociados
        cargarTiposSocio()
    }

    override fun onResume() {
        super.onResume()
        // Recarga los datos desde la base de datos para mostrar al nuevo o editado socio
        cargarDatosDesdeApi()
    }

    private fun configurarSwipeEditar() {
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false // No manejamos reordenamiento
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition

                // Notificar al adaptador para regresar el ítem a su lugar visualmente
                rvAsociados.adapter?.notifyItemChanged(position)

                if (position != RecyclerView.NO_POSITION && position < listaFiltradaActual.size) {
                    val asociadoAEditar = listaFiltradaActual[position]

                    // Abrir la pantalla de formulario pasando los datos del asociado
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
                    Toast.makeText(this@ListarActivity, "Error al obtener tipos de socio", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<TipoSocio>>, t: Throwable) {
                Toast.makeText(this@ListarActivity, "Error de red al cargar tipos: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun cargarDatosDesdeApi() {
        // Usamos el cliente global, que ya apunta a "https://sistemworld.com/api/"
        val apiService = RetrofitClient.getApiService()

        apiService.getAsociados().enqueue(object : Callback<List<Asociado>> {
            override fun onResponse(call: Call<List<Asociado>>, response: Response<List<Asociado>>) {
                if (response.isSuccessful) {
                    listaCompleta = response.body()?.sortedBy { it.idAsociado } ?: emptyList()

                    listaCompleta.forEach { asociado ->
                        Log.d("PRUEBA_TIPO", "Nombre: ${asociado.nombres} | idTipo leído: ${asociado.idTipo}")
                    }

                    // Aplicar el filtro actual al recibir los asociados
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
        listaFiltradaActual = if (posicion == 0) {
            listaCompleta
        } else {
            if (listaTiposSocio.isNotEmpty() && (posicion - 1) < listaTiposSocio.size) {
                val idTipoSeleccionado = listaTiposSocio[posicion - 1].id_tipo
                listaCompleta.filter { it.idTipo == idTipoSeleccionado }
            } else {
                listaCompleta
            }
        }

        rvAsociados.adapter = AsociadoAdapter(listaFiltradaActual)
    }
}