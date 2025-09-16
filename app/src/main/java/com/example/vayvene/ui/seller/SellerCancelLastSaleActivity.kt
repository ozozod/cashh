package com.example.vayvene.ui.seller

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.vayvene.R
import com.example.vayvene.data.Session

class SellerCancelLastSaleActivity : AppCompatActivity() {

    private lateinit var btnAuthorize: Button
    private lateinit var btnScanAndCancel: Button
    private lateinit var tvInfo: TextView

    companion object {
        const val EXTRA_MANAGER_UID = "extra_manager_uid"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_seller_cancel_last_sale)

        btnAuthorize = findViewById(R.id.btnAuthorize)
        btnScanAndCancel = findViewById(R.id.btnScanAndCancel)
        tvInfo = findViewById(R.id.tvInfo)

        // Si es manager/admin, no pedimos tarjeta de encargado:
        if (Session.isManagerOrAdmin(this)) {
            tvInfo.text = getString(R.string.ready_to_cancel_last_sale)
        } else {
            tvInfo.text = getString(R.string.need_manager_card)
        }

        btnAuthorize.setOnClickListener {
            if (Session.isManagerOrAdmin(this)) {
                Toast.makeText(this, R.string.already_authorized, Toast.LENGTH_SHORT).show()
            } else {
                // Abrí tu captura NFC del ENCARGADO y al volver, marcá autorizado (pendiente de UI)
                Toast.makeText(this, R.string.scan_manager_card, Toast.LENGTH_SHORT).show()
            }
        }

        btnScanAndCancel.setOnClickListener {
            // Abrí tu captura NFC del CLIENTE y luego mandá la anulación (pendiente de UI)
            Toast.makeText(this, R.string.scan_customer_card, Toast.LENGTH_SHORT).show()
        }
    }
}
