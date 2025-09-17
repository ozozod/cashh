package com.example.vayvene.ui.login

import android.content.Intent
import android.nfc.NfcAdapter
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.vayvene.databinding.ActivityNfcLoginBinding
import com.example.vayvene.ui.admin.AdminMenuActivity
import com.example.vayvene.ui.cashier.CashierQuickOpsActivity
import com.example.vayvene.ui.seller.SellerMenuActivity
import com.example.vayvene.utils.ReaderCallbackImpl
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class NfcLoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNfcLoginBinding
    private var nfcAdapter: NfcAdapter? = null
    private val flags = NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK

    private val vm: NfcLoginViewModel by viewModels {
        NfcLoginViewModel.provideFactory(applicationContext)
    }

    private val reader = ReaderCallbackImpl(
        onUid = { uid -> vm.loginWithCard(uid) },
        onErr = { e -> runOnUiThread { showStatus(e.message ?: "Error NFC") } }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNfcLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        observeVm()
    }

    private fun observeVm() {
        lifecycleScope.launch {
            vm.uiState.collectLatest { st ->
                binding.progress.visibility = if (st.loading) View.VISIBLE else View.GONE
                binding.tvStatus.text = st.message ?: ""
                st.goToRole?.let { goToRoleMenu(it) }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        nfcAdapter?.enableReaderMode(this, reader, flags, null)
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableReaderMode(this)
    }

    private fun goToRoleMenu(role: String) {
        val intent = when (role.uppercase()) {
            "ADMINISTRADOR" -> Intent(this, AdminMenuActivity::class.java)
            "CAJERO" -> Intent(this, CashierQuickOpsActivity::class.java) // existe en tu Manifest
            "VENDEDOR", "ENCARGADO" -> Intent(this, SellerMenuActivity::class.java)
            else -> {
                Toast.makeText(this, "Rol no soportado: $role", Toast.LENGTH_LONG).show()
                return
            }
        }
        startActivity(intent)
        finish()
    }

    private fun showStatus(msg: String) {
        binding.tvStatus.text = msg
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
