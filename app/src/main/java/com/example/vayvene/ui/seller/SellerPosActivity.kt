package com.example.vayvene.ui.seller

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.setPadding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.text.NumberFormat
import java.util.*
import com.example.vayvene.BuildConfig

class SellerPosActivity : AppCompatActivity() {

    // ---------- Modelo ----------
    data class Product(
        val id: String,
        val name: String,
        val price: Long, // unidades mínimas (centavos); si tu API devuelve en enteros, ajusta formatCurrency()
        var qty: Int = 0
    )

    // ---------- UI ----------
    private lateinit var recycler: RecyclerView
    private lateinit var tvTotal: TextView
    private lateinit var btnScanCharge: Button
    private val adapter = ProductsAdapter {
        recalcTotal()
    }

    // ---------- Red ----------
    private val http by lazy { OkHttpClient() }

    // Resultado del escaneo NFC (puede volver con distintas claves; leemos todas)
    private val nfcReader =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
            val data = res.data
            val uid = data?.getStringExtra("extra_uid")
                ?: data?.getStringExtra("EXTRA_UID")
                ?: data?.getStringExtra("uid")

            if (uid.isNullOrBlank()) {
                toast("Sin UID de tarjeta")
                return@registerForActivityResult
            }

            // TODO: acá armá el payload y hacé el POST de cobro a tu backend.
            // val items = adapter.selectedItems()
            toast("Tarjeta $uid detectada. (Implementar POST de cobro)")
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ----------- Construcción de UI por código (sin XML) ----------
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF121212.toInt())
            setPadding(dp(12))
        }

        val title = TextView(this).apply {
            text = "Punto de Venta"
            setTextColor(0xFFE0E0E0.toInt())
            textSize = 20f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, 0, 0, dp(8))
        }
        root.addView(title, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        recycler = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@SellerPosActivity)
            adapter = this@SellerPosActivity.adapter
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
        }
        root.addView(recycler, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        tvTotal = TextView(this).apply {
            text = "Total: $ 0,00"
            setTextColor(0xFFE0E0E0.toInt())
            textSize = 18f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, dp(8), 0, dp(8))
        }
        root.addView(tvTotal, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        btnScanCharge = Button(this).apply {
            text = "ESCANEAR Y COBRAR"
            setAllCaps(true)
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0xFF651FFF.toInt()) // morado
            textSize = 16f
            setPadding(0, dp(14), 0, dp(14))
        }
        root.addView(btnScanCharge, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        setContentView(root)

        btnScanCharge.setOnClickListener {
            openNfcReader(prompt = "Acercá la tarjeta del comprador…")
        }
    }

    override fun onResume() {
        super.onResume()
        loadProductsForCurrentEvent()
    }

    private fun openNfcReader(prompt: String) {
        // Si tenés NfcCaptureActivity, ajustá su package/class si difiere
        val i = Intent().apply {
            setClassName(this@SellerPosActivity, "com.example.vayvene.ui.nfc.NfcCaptureActivity")
            putExtra("extra_prompt", prompt) // también probamos con varias keys dentro de NfcCaptureActivity
        }
        nfcReader.launch(i)
    }

    // ------------- Carga de productos filtrados por EVENTO -------------
    private fun loadProductsForCurrentEvent() {
        val sp = getSharedPreferences("session", MODE_PRIVATE)
        val eventId = sp.getString("event_id", null)
        if (eventId.isNullOrBlank()) {
            toast("Sin event_id en sesión")
            adapter.submitList(emptyList())
            recalcTotal()
            return
        }

        val base = BuildConfig.BASE_URL.trimEnd('/')
        val url = "$base/api/mobile/products?eventId=${URLEncoder.encode(eventId, "UTF-8")}"

        Thread {
            try {
                val req = Request.Builder().url(url).get().build()
                http.newCall(req).execute().use { resp ->
                    val code = resp.code
                    val body = resp.body?.string().orEmpty()

                    if (code in 200..299) {
                        val json = JSONObject(body)
                        val arr: JSONArray = json.optJSONArray("products") ?: JSONArray()
                        val items = ArrayList<Product>(arr.length())
                        for (i in 0 until arr.length()) {
                            val o = arr.getJSONObject(i)
                            items.add(
                                Product(
                                    id = o.getString("id"),
                                    name = o.getString("name"),
                                    price = o.optLong("price", 0L),
                                    qty = 0
                                )
                            )
                        }
                        runOnUiThread {
                            adapter.submitList(items)
                            recalcTotal()
                        }
                    } else {
                        runOnUiThread {
                            toast("Error productos ($code)")
                            adapter.submitList(emptyList())
                            recalcTotal()
                        }
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    toast("Red: ${e.message}")
                    adapter.submitList(emptyList())
                    recalcTotal()
                }
            }
        }.start()
    }

    private fun recalcTotal() {
        val totalCents = adapter.currentItems().sumOf { it.price * it.qty }
        tvTotal.text = "Total: ${formatCurrency(totalCents)}"
    }

    // ---------- Utils ----------
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    private fun formatCurrency(cents: Long): String {
        // Si tu backend ya devuelve en unidades (no centavos), cambia a: amount = cents.toDouble()
        val amount = cents / 100.0
        val nf = NumberFormat.getCurrencyInstance(Locale("es", "AR"))
        return nf.format(amount)
    }

    // ---------- Adapter ----------
    inner class ProductsAdapter(
        private val onQtyChanged: () -> Unit
    ) : RecyclerView.Adapter<ProductsAdapter.VH>() {

        private val items = mutableListOf<Product>()

        inner class VH(val row: View, val tvName: TextView, val btnMinus: Button,
                       val tvQty: TextView, val btnPlus: Button) : RecyclerView.ViewHolder(row)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            // Fila horizontal: [Nombre ($precio)]  [ - ] [ qty ] [ + ]
            val row = LinearLayout(parent.context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(8))
            }

            val tvName = TextView(parent.context).apply {
                setTextColor(0xFFE0E0E0.toInt())
                textSize = 16f
            }
            row.addView(tvName, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

            fun pill(text: String): Button = Button(parent.context).apply {
                this.text = text
                setAllCaps(false)
                setPadding(dp(8), dp(4), dp(8), dp(4))
            }

            val btnMinus = pill("–")
            val tvQty = TextView(parent.context).apply {
                setTextColor(0xFFE0E0E0.toInt())
                text = "0"
                textSize = 16f
                setPadding(dp(12), 0, dp(12), 0)
                gravity = Gravity.CENTER
            }
            val btnPlus = pill("+")

            row.addView(btnMinus)
            row.addView(tvQty)
            row.addView(btnPlus)

            return VH(row, tvName, btnMinus, tvQty, btnPlus)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val p = items[position]
            holder.tvName.text = "${p.name} (${formatCurrency(p.price)})"
            holder.tvQty.text = p.qty.toString()

            holder.btnMinus.setOnClickListener {
                if (p.qty > 0) {
                    p.qty -= 1
                    holder.tvQty.text = p.qty.toString()
                    onQtyChanged()
                }
            }
            holder.btnPlus.setOnClickListener {
                p.qty += 1
                holder.tvQty.text = p.qty.toString()
                onQtyChanged()
            }
        }

        override fun getItemCount(): Int = items.size

        fun submitList(list: List<Product>) {
            items.clear()
            items.addAll(list)
            notifyDataSetChanged()
        }

        fun currentItems(): List<Product> = items

        fun selectedItems(): List<Product> = items.filter { it.qty > 0 }
    }
}
