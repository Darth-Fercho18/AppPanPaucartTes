package com.example.appcontrolpaucartambo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CuotasExtraordinariasAdapter(
    private var listaItems: List<CuotaExtraordinariaItem>,
    private val onItemCheckedChange: (CuotaExtraordinariaItem, Boolean) -> Unit
) : RecyclerView.Adapter<CuotasExtraordinariasAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cbAsociadoExtra: CheckBox = view.findViewById(R.id.cbAsociadoExtra)
        val tvNombreAsociadoExtra: TextView = view.findViewById(R.id.tvNombreAsociadoExtra)
        val tvDniAsociadoExtra: TextView = view.findViewById(R.id.tvDniAsociadoExtra)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_asociado_extra, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = listaItems[position]

        holder.tvNombreAsociadoExtra.text = "${item.asociado.nombres} ${item.asociado.apellidoPaterno} ${item.asociado.apellidoMaterno}"
        holder.tvDniAsociadoExtra.text = "DNI: ${item.asociado.dni}"

        holder.cbAsociadoExtra.setOnCheckedChangeListener(null)
        holder.cbAsociadoExtra.isChecked = item.isSelected

        holder.cbAsociadoExtra.setOnCheckedChangeListener { _, isChecked ->
            item.isSelected = isChecked
            onItemCheckedChange(item, isChecked)
        }

        holder.itemView.setOnClickListener {
            holder.cbAsociadoExtra.isChecked = !holder.cbAsociadoExtra.isChecked
        }
    }

    override fun getItemCount(): Int = listaItems.size

    fun actualizarLista(nuevaLista: List<CuotaExtraordinariaItem>) {
        listaItems = nuevaLista
        notifyDataSetChanged()
    }
}
