package com.example.appcontrolpaucartambo

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class AsistenciaAdapter(
    private var listaAsistencia: List<AsistenciaItem>
) : RecyclerView.Adapter<AsistenciaAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNombre: TextView = view.findViewById(R.id.tvNombreAsociado)
        val tvDni: TextView = view.findViewById(R.id.tvDniAsociado)
        val tvApoderado: TextView = view.findViewById(R.id.tvApoderado)
        val tvCelularApoderado: TextView = view.findViewById(R.id.tvCelularApoderado)
        val spinnerEstadoAsistencia: Spinner = view.findViewById(R.id.spinnerEstadoAsistencia)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_asistencia, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = listaAsistencia[position]
        val context = holder.itemView.context

        // 1. Mostrar Datos
        holder.tvNombre.text = "${item.asociado.nombres} ${item.asociado.apellidoPaterno} ${item.asociado.apellidoMaterno}"
        holder.tvDni.text = "DNI: ${item.asociado.dni ?: "N/A"}"

        // Mostrar u ocultar datos del apoderado (útil para el filtro de niños)
        if (!item.asociado.nombreApoderado.isNullOrEmpty()) {
            holder.tvApoderado.visibility = View.VISIBLE
            holder.tvApoderado.text = "Apoderado: ${item.asociado.nombreApoderado}"
        } else {
            holder.tvApoderado.visibility = View.GONE
        }

        if (!item.asociado.celularApoderado.isNullOrEmpty()) {
            holder.tvCelularApoderado.visibility = View.VISIBLE
            holder.tvCelularApoderado.text = "Cel. Apoderado: ${item.asociado.celularApoderado}"
        } else {
            holder.tvCelularApoderado.visibility = View.GONE
        }

        // 2. Configurar Spinner con las 6 opciones de asistencia y adaptador consciente del tema (Diurno / Nocturno)
        val estadosArray = context.resources.getStringArray(R.array.opciones_asistencia)
        val spinnerAdapter = object : ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, estadosArray) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                (view as? TextView)?.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                return view
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent)
                (view as? TextView)?.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                return view
            }
        }
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        holder.spinnerEstadoAsistencia.adapter = spinnerAdapter

        // Seleccionar estado actual sin disparar listener automáticamente
        holder.spinnerEstadoAsistencia.onItemSelectedListener = null
        val seleccionActual = estadosArray.indexOfFirst { it.uppercase() == item.estadoSeleccionado.uppercase() }
        if (seleccionActual >= 0) {
            holder.spinnerEstadoAsistencia.setSelection(seleccionActual)
        } else {
            holder.spinnerEstadoAsistencia.setSelection(0) // "Seleccionar..."
        }

        // Aplicar color de fondo adaptativo según el modo (Diurno / Nocturno) y el estado
        actualizarColorSpinner(holder.spinnerEstadoAsistencia, item.estadoSeleccionado, context)

        holder.spinnerEstadoAsistencia.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, pos: Int, id: Long) {
                if (pos > 0) {
                    item.estadoSeleccionado = estadosArray[pos]
                } else {
                    item.estadoSeleccionado = "Seleccionar..."
                }
                actualizarColorSpinner(holder.spinnerEstadoAsistencia, item.estadoSeleccionado, context)
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
    }

    private fun actualizarColorSpinner(spinner: Spinner, estado: String, context: Context) {
        val isDarkMode = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        val colorHex = when (estado.uppercase()) {
            "ASISTIO" -> if (isDarkMode) "#1B5E20" else "#E8F5E9" // Verde oscuro (Nocturno) / Verde suave (Diurno)
            "FALTO" -> if (isDarkMode) "#B71C1C" else "#FFEBEE" // Rojo oscuro / Rojo suave
            "TARDANZA" -> if (isDarkMode) "#F57F17" else "#FFFDE7" // Ámbar / Amarillo suave
            "JUSTIFICO" -> if (isDarkMode) "#E65100" else "#FFF3E0" // Naranja oscuro / Naranja suave
            "DISPENSA MEDICA", "DISPENSA EXTRAORDINARIA" -> if (isDarkMode) "#01579B" else "#E1F5FE" // Azul / Celeste suave
            else -> if (isDarkMode) "#2D3748" else "#FFFFFF" // Fondo neutro según modo
        }
        spinner.setBackgroundColor(Color.parseColor(colorHex))
    }

    override fun getItemCount(): Int = listaAsistencia.size

    fun actualizarLista(nuevaLista: List<AsistenciaItem>) {
        listaAsistencia = nuevaLista
        notifyDataSetChanged()
    }

    fun obtenerDatosAsistencia(): List<AsistenciaItem> {
        return listaAsistencia
    }
}
