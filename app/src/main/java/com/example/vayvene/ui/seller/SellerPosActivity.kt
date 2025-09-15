package com.example.vayvene.ui.seller

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.vayvene.BuildConfig
import com.example.vayvene.R
import com.example.vayvene.data.Session
import com.example.vayvene.ui.nfc.NfcCaptureActivity
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.util.Locale
import com.example.vayvene.ui.common.EXTRA_UID
import com.example.vayvene.ui.common.EXTRA_PROMPT
import com.example.vayvene.ui.common.EXTRA_MANAGER_UID

class SellerPosActivity : AppCompatActivity() {

    private val http by lazy { OkHttpClient() }
    private lateinit var container: LinearLayout
    private lateinit var btnScanAndCharge: Button
    private lateinit var tvTotal: TextView
    private lateinit var tvEmpty: TextView
    private val rows = mutableListOf<ProductRow>()
    private var lastBuiltItems: JSONArray = JSONArray()

    data class Product(val id: String, val name: String, val price: Double)

    private data class ProductRow(
        val product: Product,
        val qtyTv: TextView
    ) {
        fun qty(): Int = qtyTv.text.toString().toInt()
        fun setQty(q: Int) { qtyTv.text = q.toString() }
    }

    private val scanLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { res ->
        if (res.resultCode == RESULT_OK) {
            val uid = res.data?.getStringExtra(EXTRA_UID).orEmpty().uppercase()
            if (uid.isNotBlank()) chargeWithItems(uid, lastBuiltItems)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_seller_pos)

        container = findViewById(R.id.productsContainer)
        btnScanAndCharge = findViewById(R.id.btnScanAndCharge)
        tvTotal = findViewById(R.id.tvTotal)
        tvEmpty = findViewById(R.id.tvEmpty)

        btnScanAndCharge.setOnClickListener {
            val items = buildItems()
            if (items.length() == 0) {
                Toast.makeText(this, "Elegí al menos 1 producto", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            lastBuiltItems = items
            showConfirmDialog(items)
        }

        fetchProducts()
    }

    // ---------- UI ----------
    private fun addRow(p: Product) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(12, 16, 12, 16)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val name = TextView(this).apply {
            text = "${p.name}  (${formatMoney(p.price)})"
            textSize = 16f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val minus = Button(this).apply {
            text = "–"
            layoutParams = LinearLayout.LayoutParams(120, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        val qty = TextView(this).apply {
            text = "0"
            gravity = Gravity.CENTER
            textSize = 16f
            layoutParams = LinearLayout.LayoutParams(120, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        val plus = Button(this).apply {
            text = "+"
            layoutParams = LinearLayout.LayoutParams(120, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        val rowModel = ProductRow(p, qty)

        minus.setOnClickListener {
            val q = (rowModel.qty() - 1).coerceAtLeast(0)
            rowModel.setQty(q)
            refreshTotal()
        }
        plus.setOnClickListener {
            val q = (rowModel.qty() + 1).coerceAtMost(999)
            rowModel.setQty(q)
            refreshTotal()
        }

        row.addView(name); row.addView(minus); row.addView(qty); row.addView(plus)
        container.addView(row)
        rows.add(rowModel)
    }

    private fun refreshTotal() { tvTotal.text = "Total: ${formatMoney(currentTotal())}" }

    private fun currentTotal(): Double {
        var total = 0.0
        for (r in rows) total += r.qty() * r.product.price
        return total
    }

    private fun formatMoney(v: Double): String =
        NumberFormat.getCurrencyInstance(Locale("es", "AR")).format(v)

    // ---------- Build items & confirm ----------
    private fun buildItems(): JSONArray {
        val items = JSONArray()
        for (r in rows) {
            val q = r.qty()
            if (q > 0) {
                // Lo que espera tu API para validar: { productId, quantity }
                items.put(JSONObject().apply {
                    put("productId", r.product.id)
                    put("quantity", q)
                })
            }
        }
        return items
    }

    private fun buildDetails(items: JSONArray): String {
        // Genera "2x coquita, 1x fernet"
        val parts = mutableListOf<String>()
        for (i in 0 until items.length()) {
            val it = items.getJSONObject(i)
            val id = it.optString("productId")
            val qty = it.optInt("quantity", 0)
            val name = rows.firstOrNull { r -> r.product.id == id }?.product?.name ?: "item"
            parts.add("${qty}x $name")
        }
        return parts.joinToString(", ")
    }

    private fun showConfirmDialog(items: JSONArray) {
        val sb = StringBuilder()
        var total = 0.0
        for (i in 0 until items.length()) {
            val o = items.getJSONObject(i)
            val id = o.optString("productId")
            val qty = o.optInt("quantity", 0)
            val prod = rows.firstOrNull { it.product.id == id }?.product
            val name = prod?.name ?: "Item"
            val price = prod?.price ?: 0.0
            val sub = price * qty
            total += sub
            sb.append("$name x$qty = ${formatMoney(sub)}\n")
        }
        sb.append("\nTotal: ${formatMoney(total)}")

        AlertDialog.Builder(this)
            .setTitle("Confirmar venta")
            .setMessage(sb.toString())
            .setNegativeButton("Volver") { d, _ -> d.dismiss() }
            .setPositiveButton("Escanear y COBRAR") { d, _ ->
                d.dismiss()
                val i = Intent(this, NfcCaptureActivity::class.java)
                i.putExtra(
                    EXTRA_PROMPT,
                    "Acercá la tarjeta del COMPRADOR para COBRAR"
                )
                scanLauncher.launch(i)
            }
            .show()
    }

    // ---------- NET ----------
    private fun fetchProducts() {
        val base = BuildConfig.BASE_URL.trimEnd('/')
        val url = "$base/api/mobile/products"
        val token = Session.jwt(this)
        if (token.isNullOrBlank()) { Toast.makeText(this, "Sesión expirada", Toast.LENGTH_SHORT).show(); finish(); return }
        val req = Request.Builder().url(url).get().addHeader("Authorization", "Bearer $token").build()

        Thread {
            try {
                http.newCall(req).execute().use { res ->
                    val body = res.body?.string().orEmpty()
                    if (!res.isSuccessful) {
                        runOnUiThread {
                            Toast.makeText(this, "Productos (${res.code}): $body", Toast.LENGTH_LONG).show()
                            showEmpty(true)
                        }
                        return@use
                    }
                    val obj = JSONObject(body)
                    val arr = obj.optJSONArray("products") ?: JSONArray()
                    runOnUiThread {
                        container.removeAllViews(); rows.clear()
                        if (arr.length() == 0) { showEmpty(true) }
                        else {
                            showEmpty(false)
                            for (i in 0 until arr.length()) {
                                val o = arr.getJSONObject(i)
                                val p = Product(
                                    id = o.optString("id"),
                                    name = o.optString("name"),
                                    price = o.optDouble("price", 0.0)
                                )
                                addRow(p)
                            }
                            refreshTotal()
                        }
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Red productos: ${e.message}", Toast.LENGTH_LONG).show()
                    showEmpty(true)
                }
            }
        }.start()
    }

    private fun showEmpty(empty: Boolean) {
        tvEmpty.visibility = if (empty) android.view.View.VISIBLE else android.view.View.GONE
        btnScanAndCharge.isEnabled = !empty
        if (empty) tvTotal.text = "Total: $0,00"
    }

    private fun chargeWithItems(customerUid: String, items: JSONArray) {
        val base = BuildConfig.BASE_URL.trimEnd('/')
        val url = "$base/api/mobile/sale"
        val token = Session.jwt(this) ?: run {
            Toast.makeText(this, "Sesión expirada", Toast.LENGTH_SHORT).show()
            return
        }

        val payload = JSONObject().apply {
            put("customerUid", customerUid)
            put("items", items)                    // para validar/registrar
            put("details", buildDetails(items))    // para que la web muestre “2x coquita, …”
        }

        val req = Request.Builder()
            .url(url)
            .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .addHeader("Authorization", "Bearer $token")
            .build()

        Thread {
            try {
                http.newCall(req).execute().use { res ->
                    val code = res.code
                    val body = res.body?.string().orEmpty()
                    runOnUiThread {
                        if (code == 401) { Toast.makeText(this, "Sesión expirada (401)", Toast.LENGTH_SHORT).show(); finish(); return@runOnUiThread }
                        if (code in 200..299) {
                            rows.forEach { it.setQty(0) }; refreshTotal()
                            val totalCobrado = computeTotalFrom(items)
                            val i = Intent(this, SellerSaleResultActivity::class.java)
                            i.putExtra(SellerSaleResultActivity.EXTRA_TOTAL, totalCobrado)
                            i.putExtra(SellerSaleResultActivity.EXTRA_UID, customerUid)
                            startActivity(i)
                        } else {
                            Toast.makeText(this, "Venta error ($code): $body", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } catch (e: Exception) {
                runOnUiThread { Toast.makeText(this, "Red venta: ${e.message}", Toast.LENGTH_LONG).show() }
            }
        }.start()
    }

    private fun computeTotalFrom(items: JSONArray): Double {
        var t = 0.0
        for (i in 0 until items.length()) {
            val it = items.getJSONObject(i)
            val id = it.optString("productId")
            val qty = it.optInt("quantity", 0)
            val price = rows.firstOrNull { r -> r.product.id == id }?.product?.price ?: 0.0
            t += qty * price
        }
        return t
    }
}
