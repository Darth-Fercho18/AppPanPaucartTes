package com.example.appcontrolpaucartambo

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

sealed class DeudaListItem {
    data class Header(val titulo: String) : DeudaListItem()
    data class Item(val deuda: DeudaPendiente) : DeudaListItem()
}

class DeudasAdapter(
    private val listaItems: List<DeudaListItem>,
    private val onDeudaCheckChanged: (DeudaPendiente) -> Unit,
    private val onItemClick: ((DeudaPendiente) -> Unit)? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_ITEM = 1
    }

    class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvHeaderDeuda: TextView = itemView.findViewById(R.id.tvHeaderDeuda)
    }

    class DeudaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cbSeleccionarDeuda: CheckBox = itemView.findViewById(R.id.cbSeleccionarDeuda)
        val tvConceptoDeuda: TextView = itemView.findViewById(R.id.tvConceptoDeuda)
        val tvDetalleDeuda: TextView = itemView.findViewById(R.id.tvDetalleDeuda)
        val etMontoDeuda: EditText = itemView.findViewById(R.id.etMontoDeuda)
    }

    override fun getItemViewType(position: Int): Int {
        return when (listaItems[position]) {
            is DeudaListItem.Header -> VIEW_TYPE_HEADER
            is DeudaListItem.Item -> VIEW_TYPE_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_HEADER) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_deuda_header, parent, false)
            HeaderViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_deuda, parent, false)
            DeudaViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = listaItems[position]) {
            is DeudaListItem.Header -> {
                (holder as HeaderViewHolder).tvHeaderDeuda.text = item.titulo
            }
            is DeudaListItem.Item -> {
                val deuda = item.deuda
                val h = holder as DeudaViewHolder

                val mesNombres = arrayOf("", "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre")

                when (deuda.idTipoCuota) {
                    1 -> { // Cuota Mensual
                        val nombreMes = if (deuda.mes != null && deuda.mes in 1..12) mesNombres[deuda.mes] else ""
                        val anioStr = deuda.anio?.toString() ?: ""
                        h.tvConceptoDeuda.text = "$nombreMes $anioStr".trim()
                        h.tvDetalleDeuda.visibility = View.GONE
                    }
                    2 -> { // Cuota Extraordinaria
                        h.tvConceptoDeuda.text = deuda.concepto ?: "Cuota Extraordinaria"
                        h.tvDetalleDeuda.visibility = View.GONE
                    }
                    3 -> { // Multa
                        h.tvConceptoDeuda.text = if (!deuda.nombreActividad.isNullOrEmpty()) deuda.nombreActividad else (deuda.concepto ?: "Multa")
                        h.tvDetalleDeuda.text = deuda.fechaEmision ?: ""
                        h.tvDetalleDeuda.visibility = if (!deuda.fechaEmision.isNullOrEmpty()) View.VISIBLE else View.GONE
                    }
                    else -> {
                        h.tvConceptoDeuda.text = deuda.concepto ?: deuda.tipoCuota ?: "Deuda"
                        h.tvDetalleDeuda.visibility = View.GONE
                    }
                }

                // Manejar edición de monto de deuda en tiempo real con TextWatcher
                val existingWatcher = h.etMontoDeuda.tag as? TextWatcher
                if (existingWatcher != null) {
                    h.etMontoDeuda.removeTextChangedListener(existingWatcher)
                }

                h.etMontoDeuda.setText(String.format("%.2f", deuda.monto))

                val textWatcher = object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: Editable?) {
                        val texto = s.toString()
                        val nuevoMonto = texto.toDoubleOrNull() ?: 0.0
                        deuda.monto = nuevoMonto
                        onDeudaCheckChanged(deuda)
                    }
                }

                h.etMontoDeuda.addTextChangedListener(textWatcher)
                h.etMontoDeuda.tag = textWatcher

                h.cbSeleccionarDeuda.setOnCheckedChangeListener(null)
                h.cbSeleccionarDeuda.isChecked = deuda.estaSeleccionada

                h.cbSeleccionarDeuda.setOnCheckedChangeListener { _, isChecked ->
                    deuda.estaSeleccionada = isChecked
                    onDeudaCheckChanged(deuda)
                }

                val clickListener = View.OnClickListener {
                    onItemClick?.invoke(deuda)
                }

                h.tvConceptoDeuda.setOnClickListener(clickListener)
                h.tvDetalleDeuda.setOnClickListener(clickListener)
                h.itemView.setOnClickListener(clickListener)
            }
        }
    }

    override fun getItemCount(): Int = listaItems.size
}
