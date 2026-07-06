package com.cfg.android.ui.payment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.cfg.android.databinding.FragmentPaymentBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PaymentFragment : Fragment() {

    private var _binding: FragmentPaymentBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PaymentViewModel by viewModels()

    private var orderId: String? = null
    private var totalAmount: Double = 0.0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        _binding = FragmentPaymentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        orderId = arguments?.getString("orderId")
        totalAmount = (arguments?.getFloat("totalAmount") ?: 0f).toDouble()

        binding.tvAmountDue.text = "Montant : ${formatAr(totalAmount)}"
        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }

        setupMethodButtons()
        setupPayButton()
        observeState()
    }

    private fun setupMethodButtons() {
        binding.btnCash.setOnClickListener { viewModel.selectMethod("CASH") }
        binding.btnOrangeMoney.setOnClickListener { viewModel.selectMethod("ORANGE_MONEY") }
        binding.btnMvola.setOnClickListener { viewModel.selectMethod("MVOLA") }
        binding.btnAirtel.setOnClickListener { viewModel.selectMethod("AIRTEL_MONEY") }
    }

    private fun setupPayButton() {
        binding.btnPay.setOnClickListener {
            val ref = binding.etReference.text?.toString()?.trim()
            viewModel.setReference(ref)
            orderId?.let { id -> viewModel.pay(id, totalAmount) }
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    // Highlight selected method
                    val methods = listOf(
                        "CASH" to binding.btnCash,
                        "ORANGE_MONEY" to binding.btnOrangeMoney,
                        "MVOLA" to binding.btnMvola,
                        "AIRTEL_MONEY" to binding.btnAirtel
                    )
                    methods.forEach { (method, btn) ->
                        btn.isSelected = method == state.selectedMethod
                    }

                    // Reference field: show only for non-cash methods
                    binding.layoutReference.isVisible = state.selectedMethod != "CASH"

                    binding.progressPayment.isVisible = state.isLoading
                    binding.btnPay.isEnabled = !state.isLoading

                    state.error?.let { err ->
                        binding.tvError.text = err
                        binding.tvError.isVisible = true
                        viewModel.clearError()
                    } ?: run { binding.tvError.isVisible = false }

                    if (state.paymentDone != null) {
                        showSuccess(state.paymentDone!!.amount)
                    }
                }
            }
        }
    }

    private fun showSuccess(amountPaid: Double) {
        binding.layoutPayment.isVisible = false
        binding.layoutSuccess.isVisible = true
        binding.tvSuccessAmount.text = formatAr(amountPaid)
        binding.btnDone.setOnClickListener {
            // Pop back to table list (clear back stack through order + cart)
            findNavController().popBackStack(com.cfg.android.R.id.tableListFragment, false)
        }
    }

    private fun formatAr(amount: Double) =
        "%,.0f Ar".format(amount).replace(",", " ")

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
