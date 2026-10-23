package com.example.appcontrolpaucartambo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

class SocioAdapter(
    private var listaSocios: List<Socio>,
    private val onCobrarClick: (Socio) -> Unit
) : RecyclerView.Adapter<SocioAdapter.SocioViewHolder>() {

    class SocioViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNombre: TextView = view.findViewById(R.id.tvNombreSocio)
        val tvDetalle: TextView = view.findViewById(R.id.tvDetalleSocio)
        val tvEstado: TextView = view.findViewById(R.id.tvEstadoSocio)
        val btnCobrar: MaterialButton = view.findViewById(R.id.btnVerCuenta)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SocioViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_socio, parent, false)
        return SocioViewHolder(view)
    }

    override fun onBindViewHolder(holder: SocioViewHolder, position: Int) {
        val socio = listaSocios[position]
        val context = holder.itemView.context

        holder.tvNombre.text = socio.nombre
        holder.tvDetalle.text = "Lote: ${socio.lote}"

        if (socio.estaAlDia) {
            holder.tvEstado.text = "● Al día"
            holder.tvEstado.setTextColor(ContextCompat.getColor(context, R.color.primary_green_light))
        } else {
            holder.tvEstado.text = "● Deuda: S/ ${String.format("%.2f", socio.montoDeuda)}"
            holder.tvEstado.setTextColor(ContextCompat.getColor(context, R.color.danger_red))
        }

        holder.btnCobrar.setOnClickListener {
            onCobrarClick(socio)
        }
    }

    override fun getItemCount(): Int = listaSocios.size

    fun actualizarLista(nuevaLista: List<Socio>) {
        listaSocios = nuevaLista
        notifyDataSetChanged()
    }
}