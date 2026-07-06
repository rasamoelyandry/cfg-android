package com.cfg.android.ui.order

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.cfg.android.R
import com.cfg.android.databinding.FragmentOrderStatusBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class OrderStatusFragment : Fragment() {

    private var _binding: FragmentOrderStatusBinding? = null
    private val binding get() = _binding!!
    private val viewModel: OrderStatusViewModel by viewModels()

    private lateinit var tableId: String
    private var tableNumber: Int = 0
    private var orderId: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        _binding = FragmentOrderStatusBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tableId = requireNotNull(arguments?.getString("tableId")) { "tableId manquant" }
        tableNumber = arguments?.getInt("tableNumber") ?: 0
        orderId = arguments?.getString("orderId")

        binding.toolbar.title = "Table $tableNumber"
        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }

        binding.btnMarkServed.setOnClickListener { viewModel.markAsServed() }
        binding.btnPay.setOnClickListener {
            val total = viewModel.uiState.value.order?.totalAmount ?: 0.0
            val id = orderId ?: return@setOnClickListener
            val bundle = bundleOf("orderId" to id, "totalAmount" to total.toFloat())
            findNavController().navigate(R.id.action_orderStatus_to_payment, bundle)
        }
        binding.btnReleaseTable.setOnClickListener { viewModel.releaseTable(tableId) }

        observeState()
        viewModel.load(orderId)
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    if (state.released) {
                        // Table liberee : retour direct a la liste des tables
                        findNavController().popBackStack(R.id.tableListFragment, false)
                        return@collect
                    }

                    binding.progressOrderStatus.isVisible = state.isLoading
                    binding.tvOrderStatusError.isVisible = state.error != null
                    binding.tvOrderStatusError.text = state.error
                    binding.btnReleaseTable.isEnabled = !state.isReleasing

                    val order = state.order
                    binding.scrollContent.isVisible = order != null
                    binding.tvNoActiveOrder.isVisible = order == null && !state.isLoading

                    if (order != null) {
                        binding.chipStatus.text = statusLabel(order.status)
                        binding.tvCustomerName.text = order.customerName?.let { "Client : $it" } ?: ""
                        binding.tvCustomerName.isVisible = !order.customerName.isNullOrBlank()
                        binding.tvTotal.text = "Total : ${formatAr(order.totalAmount)}"

                        binding.containerItems.removeAllViews()
                        order.items.forEach { item ->
                            val tv = TextView(requireContext())
                            tv.text = "${item.quantity}x ${item.menuItemName} — ${formatAr(item.unitPrice * item.quantity)}"
                            tv.textSize = 14f
                            tv.setPadding(0, 8, 0, 8)
                            binding.containerItems.addView(tv)
                        }

                        binding.btnMarkServed.isVisible = order.status == "READY"
                        binding.btnMarkServed.isEnabled = !state.isUpdating
                        binding.btnPay.isVisible = order.status == "READY" || order.status == "SERVED"
                    }
                }
            }
        }
    }

    private fun statusLabel(status: String) = when (status) {
        "PENDING" -> "En attente"
        "PREPARING" -> "En préparation"
        "READY" -> "Prêt"
        "SERVED" -> "Servi"
        "PAID" -> "Payé"
        "CANCELLED" -> "Annulé"
        else -> status
    }

    private fun formatAr(amount: Double) =
        "%,.0f Ar".format(amount).replace(",", " ")

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
