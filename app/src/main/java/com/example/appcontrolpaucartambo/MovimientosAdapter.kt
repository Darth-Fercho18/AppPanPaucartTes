package com.example.appcontrolpaucartambo

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.Locale

class MovimientosAdapter(
    private var listaMovimientos: List<Movimiento>,
    private val onItemLongClick: ((Movimiento) -> Unit)? = null
) : RecyclerView.Adapter<MovimientosAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardMovimiento: MaterialCardView = view.findViewById(R.id.cardMovimiento)
        val tvConceptoMovimiento: TextView = view.findViewById(R.id.tvConceptoMovimiento)
        val tvAsociadoMovimiento: TextView = view.findViewById(R.id.tvAsociadoMovimiento)
        val tvMontoMovimiento: TextView = view.findViewById(R.id.tvMontoMovimiento)
        val tvCuentaMovimiento: TextView = view.findViewById(R.id.tvCuentaMovimiento)
        val tvComprobanteMovimiento: TextView = view.findViewById(R.id.tvComprobanteMovimiento)
        val tvFechaMovimiento: TextView = view.findViewById(R.id.tvFechaMovimiento)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_movimiento, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val movimiento = listaMovimientos[position]

        // Combinar categoría y concepto con espacio necesario
        val cat = movimiento.categoria ?: "GENERAL"
        val con = movimiento.concepto ?: ""
        holder.tvConceptoMovimiento.text = if (con.isNotEmpty()) "$cat - $con" else cat
        holder.tvConceptoMovimiento.setTextColor(Color.parseColor("#1A1A1A")) // Texto oscuro de alta visibilidad

        // Mostrar nombre del asociado si existe (más pequeño que categoría y concepto)
        val nombreTercero = movimiento.asociadoNombre ?: movimiento.nombre
        if (!nombreTercero.isNullOrEmpty()) {
            holder.tvAsociadoMovimiento.visibility = View.VISIBLE
            holder.tvAsociadoMovimiento.text = "Integrante/Tercero: $nombreTercero"
            holder.tvAsociadoMovimiento.setTextColor(Color.parseColor("#555555"))
        } else {
            holder.tvAsociadoMovimiento.visibility = View.GONE
        }

        // Mapear cuenta bancaria por ID (1: Efectivo, 2: BCP, 3: Interbank, 4: Dólares)
        val nombreCuenta = when (movimiento.idCuenta) {
            1 -> "Efectivo"
            2 -> "BCP"
            3 -> "Interbank"
            4 -> "Dólares"
            else -> movimiento.nombreCuenta ?: "Cuenta ${movimiento.idCuenta}"
        }
        holder.tvCuentaMovimiento.text = nombreCuenta
        holder.tvCuentaMovimiento.setTextColor(Color.parseColor("#4A4A4A")) // Gris oscuro legible

        // N° Comprobante
        if (!movimiento.numComprobante.isNullOrEmpty()) {
            holder.tvComprobanteMovimiento.visibility = View.VISIBLE
            holder.tvComprobanteMovimiento.text = "N°: ${movimiento.numComprobante}"
            holder.tvComprobanteMovimiento.setTextColor(Color.parseColor("#4A4A4A"))
        } else {
            holder.tvComprobanteMovimiento.visibility = View.GONE
        }

        // Monto
        val esIngreso = movimiento.tipoMovimiento.uppercase() == "INGRESO"
        val simbolo = if (movimiento.idCuenta == 4) "$" else "S/"
        holder.tvMontoMovimiento.text = "$simbolo ${String.format(Locale.getDefault(), "%.2f", movimiento.monto)}"

        if (esIngreso) {
            holder.tvMontoMovimiento.setTextColor(Color.parseColor("#2E7D32")) // Verde oscuro contrastante
            holder.cardMovimiento.setCardBackgroundColor(Color.parseColor("#E8F5E9")) // Verde claro
        } else {
            holder.tvMontoMovimiento.setTextColor(Color.parseColor("#C62828")) // Rojo oscuro contrastante
            holder.cardMovimiento.setCardBackgroundColor(Color.parseColor("#FFEBEE")) // Rojo claro
        }

        // Fecha sin hora y formateada como dd/MM/yyyy
        holder.tvFechaMovimiento.text = formatearFecha(movimiento.fechaMovimiento)
        holder.tvFechaMovimiento.setTextColor(Color.parseColor("#4A4A4A"))

        // Configurar pulsación larga (mantener presionado)
        holder.itemView.setOnLongClickListener {
            onItemLongClick?.invoke(movimiento)
            true
        }
    }

    private fun formatearFecha(fechaStr: String?): String {
        if (fechaStr.isNullOrEmpty()) return ""
        return try {
            val parser = if (fechaStr.contains(" ")) {
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            } else {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            }
            val date = parser.parse(fechaStr)
            val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            if (date != null) formatter.format(date) else fechaStr
        } catch (_: Exception) {
            fechaStr
        }
    }

    override fun getItemCount(): Int = listaMovimientos.size

    fun actualizarLista(nuevaLista: List<Movimiento>) {
        listaMovimientos = nuevaLista
        notifyDataSetChanged()
    }
}
