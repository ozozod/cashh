package com.example.vayvene.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.vayvene.R
import com.example.vayvene.data.ApiClient
import com.example.vayvene.data.Product
import com.example.vayvene.data.Repository
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class SellerSummaryActivity : AppCompatActivity() {

    private val repo by lazy { Repository(ApiClient.api) }
    private lateinit var recycler: RecyclerView
    private val adapter = ProductsAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Necesita activity_seller_summary.xml con recyclerProducts
        setContentView(R.layout.activity_seller_summary)

        recycler = findViewById(R.id.recyclerProducts)
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        lifecycleScope.launch {
            val products = repo.products().getOrElse { emptyList() }
            adapter.submit(products)
        }
    }

    private class ProductsAdapter : RecyclerView.Adapter<VH>() {
        private val data = mutableListOf<Product>()
        fun submit(items: List<Product>) {
            data.clear()
            data.addAll(items)
            notifyDataSetChanged()
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_product_summary, parent, false)
            return VH(v)
        }
        override fun getItemCount(): Int = data.size
        override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(data[position])
    }

    private class VH(v: View) : RecyclerView.ViewHolder(v) {
        private val name = v.findViewById<TextView>(R.id.txtName)
        private val price = v.findViewById<TextView>(R.id.txtPrice)
        private val extra = v.findViewById<TextView>(R.id.txtExtra)
        private val money = NumberFormat.getCurrencyInstance(Locale("es", "AR"))

        fun bind(p: Product) {
            name.text = p.name
            price.text = money.format(p.price / 100.0)
            // “extra” podría ser cantidad vendida + total, por ahora placeholder:
            extra.text = "Vendidos: 0 • Total: ${money.format(0)}"
        }
    }
}
