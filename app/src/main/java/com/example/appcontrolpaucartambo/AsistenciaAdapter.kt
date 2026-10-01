package com.example.appcontrolpaucartambo

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Spinner
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class AsistenciaAdapter(
    private var listaAsistencia: List<AsistenciaItem>
) : RecyclerView.Adapter<AsistenciaAdapter.AsistenciaViewHolder>() {

    fun actualizarLista(nuevaLista: List<AsistenciaItem>) {
        listaAsistencia = nuevaLista
        notifyDataSetChanged()
    }

    fun obtenerDatosAsistencia(): List<AsistenciaItem> {
        return listaAsistencia
    }

    inner class AsistenciaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNombre: TextView = itemView.findViewById(R.id.tvNombreAsistencia)
        val tvDni: TextView = itemView.findViewById(R.id.tvDniAsistencia)
        val spEstado: Spinner = itemView.findViewById(R.id.spEstadoAsistencia)

        // Guardamos el fondo original de la tarjeta desde el XML
        val fondoOriginal: Drawable? = itemView.background
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AsistenciaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_asistencia, parent, false)
        return AsistenciaViewHolder(view)
    }

    override fun onBindViewHolder(holder: AsistenciaViewHolder, position: Int) {
        val item = listaAsistencia[position]

        holder.tvNombre.text = "${item.asociado.nombres} ${item.asociado.apellidoPaterno} ${item.asociado.apellidoMaterno}"
        holder.tvDni.text = "DNI: ${item.asociado.dni}"

        holder.spEstado.onItemSelectedListener = null

        val opciones = holder.itemView.context.resources.getStringArray(R.array.opciones_asistencia)
        val index = opciones.indexOf(item.estadoSeleccionado)
        if (index >= 0) {
            holder.spEstado.setSelection(index)
        } else {
            holder.spEstado.setSelection(0)
        }

        // Aplicamos los colores
        aplicarColorFondoYTexto(holder, item.estadoSeleccionado)

        holder.spEstado.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                val nuevoEstado = opciones[pos]
                item.estadoSeleccionado = nuevoEstado

                // Aplicamos el color en vivo al cambiar el Spinner
                aplicarColorFondoYTexto(holder, nuevoEstado)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    override fun getItemCount(): Int = listaAsistencia.size

    // --- FUNCIÓN MEJORADA Y ESTANDARIZADA CON TUS COLORES GLOBALES ---
    private fun aplicarColorFondoYTexto(holder: AsistenciaViewHolder, estado: String) {
        val colorFondo = obtenerColorPorEstado(estado)
        val context = holder.itemView.context

        if (colorFondo == null) {
            // 1. Restaurar el fondo original del XML (@color/mi_color_tarjeta)
            holder.itemView.background = holder.fondoOriginal

            // 2. Cargar dinámicamente tus colores globales del tema activo (Día / Noche)
            val colorPrimario = ContextCompat.getColor(context, R.color.mi_color_primario)
            val colorTextoPrincipal = ContextCompat.getColor(context, R.color.mi_color_texto_principal)

            holder.tvNombre.setTextColor(colorPrimario)
            holder.tvDni.setTextColor(colorTextoPrincipal)
        } else {
            // 3. Crear fondo coloreado dinámico con esquinas redondeadas de 16dp
            val fondoRedondeado = GradientDrawable()
            fondoRedondeado.shape = GradientDrawable.RECTANGLE

            val radiusInPx = 16 * context.resources.displayMetrics.density
            fondoRedondeado.cornerRadius = radiusInPx
            fondoRedondeado.setColor(colorFondo)

            holder.itemView.background = fondoRedondeado

            // 4. Texto oscuro fijo cuando hay un color pastel activo para legibilidad
            holder.tvNombre.setTextColor(Color.parseColor("#121212"))
            holder.tvDni.setTextColor(Color.parseColor("#424242"))
        }
    }

    // --- COLORES PASTEL POR ESTADO ---
    private fun obtenerColorPorEstado(estado: String): Int? {
        return when (estado.trim().uppercase()) {
            "ASISTIO" -> Color.parseColor("#C8E6C9")
            "FALTO" -> Color.parseColor("#FFCDD2")
            "JUSTIFICO" -> Color.parseColor("#FFCC80")
            "TARDANZA" -> Color.parseColor("#FFF9C4")
            "DISPENSA MEDICA" -> Color.parseColor("#B3E5FC")
            "DISPENSA EXTRAORDINARIA" -> Color.parseColor("#E1BEE7")
            else -> null
        }
    }
}