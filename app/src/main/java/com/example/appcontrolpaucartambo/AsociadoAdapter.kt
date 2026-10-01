package com.example.appcontrolpaucartambo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AsociadoAdapter(private val listaAsociados: List<Asociado>) :
    RecyclerView.Adapter<AsociadoAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNombre: TextView = view.findViewById(R.id.tvNombre)
        val tvDni: TextView = view.findViewById(R.id.tvDni)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_asociado, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val asociado = listaAsociados[position]
        holder.tvNombre.text = "${asociado.nombres} ${asociado.apellidoPaterno} ${asociado.apellidoMaterno}"
        holder.tvDni.text = "DNI: ${asociado.dni}"
    }

    override fun getItemCount(): Int = listaAsociados.size
}