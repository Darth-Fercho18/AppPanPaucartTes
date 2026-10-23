package com.example.appcontrolpaucartambo

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale

class ComprobanteMovimientoActivity : AppCompatActivity() {

    private lateinit var cardComprobante: MaterialCardView
    private lateinit var btnExportarComprobante: MaterialButton
    private lateinit var ivCodigoBarras: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comprobante_movimiento)

        val movimiento = intent.getSerializableExtra("MOVIMIENTO_EXTRA") as? Movimiento ?: run {
            finish()
            return
        }

        val tvTipoRecibo = findViewById<TextView>(R.id.tvTipoRecibo)
        val tvComprobanteRecibo = findViewById<TextView>(R.id.tvComprobanteRecibo)
        val tvFechaRecibo = findViewById<TextView>(R.id.tvFechaRecibo)
        val tvCuentaRecibo = findViewById<TextView>(R.id.tvCuentaRecibo)
        val tvCategoriaRecibo = findViewById<TextView>(R.id.tvCategoriaRecibo)
        val tvConceptoRecibo = findViewById<TextView>(R.id.tvConceptoRecibo)
        val layoutAsociadoRecibo = findViewById<LinearLayout>(R.id.layoutAsociadoRecibo)
        val tvAsociadoRecibo = findViewById<TextView>(R.id.tvAsociadoRecibo)
        val tvMontoRecibo = findViewById<TextView>(R.id.tvMontoRecibo)
        val tvCodigoBarrasTexto = findViewById<TextView>(R.id.tvCodigoBarrasTexto)
        ivCodigoBarras = findViewById(R.id.ivCodigoBarras)
        cardComprobante = findViewById(R.id.cardComprobante)
        btnExportarComprobante = findViewById(R.id.btnExportarComprobante)

        val esIngreso = movimiento.tipoMovimiento.uppercase() == "INGRESO"
        tvTipoRecibo.text = if (esIngreso) "COMPROBANTE DE INGRESO" else "COMPROBANTE DE EGRESO"
        tvTipoRecibo.setTextColor(Color.parseColor(if (esIngreso) "#2E7D32" else "#C62828"))

        val numComp = if (!movimiento.numComprobante.isNullOrEmpty()) movimiento.numComprobante else "P-${String.format(Locale.getDefault(), "%04d", movimiento.idMovimiento)}"
        tvComprobanteRecibo.text = numComp

        tvConceptoRecibo.text = movimiento.concepto ?: "MOVIMIENTO"
        tvCategoriaRecibo.text = if (!movimiento.categoria.isNullOrBlank()) movimiento.categoria else "N/A"

        val fechaFormateada = formatearFecha(movimiento.fechaMovimiento)
        tvFechaRecibo.text = fechaFormateada

        val simbolo = if (movimiento.idCuenta == 4) "$" else "S/"
        val montoStr = String.format(Locale.getDefault(), "%.2f", movimiento.monto)
        tvMontoRecibo.text = "$simbolo $montoStr"

        // Obtener el nombre de la cuenta priorizando el valor devuelto por la API (cb.nombre_cuenta)
        val nombreCuenta = when {
            !movimiento.nombreCuenta.isNullOrEmpty() -> movimiento.nombreCuenta
            movimiento.idCuenta == 1 -> "Efectivo"
            movimiento.idCuenta == 2 -> "BCP"
            movimiento.idCuenta == 3 -> "Interbank"
            movimiento.idCuenta == 4 -> "Dólares"
            else -> "Cuenta ${movimiento.idCuenta}"
        }
        tvCuentaRecibo.text = nombreCuenta

        // Asociado o tercero sin mezclar con la cuenta
        val nombreTercero = movimiento.asociadoNombre ?: movimiento.nombre
        if (!nombreTercero.isNullOrEmpty()) {
            layoutAsociadoRecibo.visibility = View.VISIBLE
            tvAsociadoRecibo.text = nombreTercero
        } else {
            layoutAsociadoRecibo.visibility = View.GONE
        }

        val token = movimiento.tokenPublico

        if (!token.isNullOrBlank()) {
            val urlVerificacion = "https://sistemworld.com/ver_comprobante.php?token=$token"

            // Ajustar ImageView a cuadrado de 190dp x 190dp centrado
            val tamanoPx = dpToPx(190)
            val params = ivCodigoBarras.layoutParams as LinearLayout.LayoutParams
            params.width = tamanoPx
            params.height = tamanoPx
            params.gravity = android.view.Gravity.CENTER_HORIZONTAL
            ivCodigoBarras.layoutParams = params

            tvCodigoBarrasTexto.visibility = View.VISIBLE
            tvCodigoBarrasTexto.text = "Escanee para verificar comprobante"

            val qrBitmap = generarBitmapQR(urlVerificacion, 600)
            if (qrBitmap != null) {
                ivCodigoBarras.setImageBitmap(qrBitmap)
            }
        } else {
            // Conservar el código de barras rectangular de 70dp de alto
            val altoPx = dpToPx(70)
            val params = ivCodigoBarras.layoutParams as LinearLayout.LayoutParams
            params.width = LinearLayout.LayoutParams.MATCH_PARENT
            params.height = altoPx
            params.gravity = android.view.Gravity.CENTER_HORIZONTAL
            ivCodigoBarras.layoutParams = params

            val barcodeData = "MOV-${movimiento.idMovimiento}-${numComp.replace(" ", "")}"
            tvCodigoBarrasTexto.visibility = View.VISIBLE
            tvCodigoBarrasTexto.text = "*$barcodeData*"

            val barcodeBitmap = generarBitmapCodigoDeBarras(barcodeData, 800, 200)
            if (barcodeBitmap != null) {
                ivCodigoBarras.setImageBitmap(barcodeBitmap)
            }
        }

        btnExportarComprobante.setOnClickListener {
            exportarComprobanteComoImagen()
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun generarBitmapQR(texto: String, tamano: Int): Bitmap? {
        return try {
            val writer = com.google.zxing.MultiFormatWriter()
            val bitMatrix = writer.encode(texto, com.google.zxing.BarcodeFormat.QR_CODE, tamano, tamano)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bmp.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
                }
            }
            bmp
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun generarBitmapCodigoDeBarras(texto: String, ancho: Int, alto: Int): Bitmap? {
        return try {
            val writer = com.google.zxing.MultiFormatWriter()
            val bitMatrix = writer.encode(texto, com.google.zxing.BarcodeFormat.CODE_128, ancho, alto)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bmp.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
                }
            }
            bmp
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun exportarComprobanteComoImagen() {
        try {
            val bitmap = Bitmap.createBitmap(cardComprobante.width, cardComprobante.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            cardComprobante.draw(canvas)

            val cachePath = File(cacheDir, "comprobante_paucartambo_${System.currentTimeMillis()}.png")
            val stream = FileOutputStream(cachePath)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()

            val imageUri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.fileprovider",
                cachePath
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, imageUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Compartir o Exportar Comprobante"))
        } catch (e: Exception) {
            Toast.makeText(this, "Error al exportar imagen: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
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
}
