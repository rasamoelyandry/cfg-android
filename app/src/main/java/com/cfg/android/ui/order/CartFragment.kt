package com.cfg.android.ui.order

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cfg.android.R
import com.cfg.android.databinding.FragmentCartBinding
import com.cfg.android.databinding.ItemCartEntryBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CartFragment : Fragment() {

    private var _binding: FragmentCartBinding? = null
    private val binding get() = _binding!!
    private val orderViewModel: OrderViewModel by activityViewModels()
    private lateinit var cartAdapter: CartAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        _binding = FragmentCartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }

        cartAdapter = CartAdapter(
            onIncrease = { item -> orderViewModel.updateQuantity(item.menuItemId, item.quantity + 1) },
            onDecrease = { item -> orderViewModel.updateQuantity(item.menuItemId, item.quantity - 1) }
        )
        binding.recyclerCart.adapter = cartAdapter
        binding.recyclerCart.layoutManager = LinearLayoutManager(requireContext())

        binding.btnConfirmOrder.setOnClickListener {
            val customerName = binding.etCustomerName.text?.toString()?.trim()
            val notes = binding.etNotes.text?.toString()?.trim()
            val state = orderViewModel.uiState.value
            orderViewModel.submitOrder(state.tableId, customerName?.ifEmpty { null }, notes?.ifEmpty { null })
        }

        observeState()
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                orderViewModel.uiState.collect { state ->
                    cartAdapter.submitList(state.cart)

                    val total = orderViewModel.totalAmount()
                    binding.tvTotal.text = "Total : ${formatAr(total)}"
                    binding.btnConfirmOrder.isEnabled = state.cart.isNotEmpty() && !state.isLoading
                    binding.progressOrder.isVisible = state.isLoading

                    state.error?.let { err ->
                        binding.tvError.text = err
                        binding.tvError.isVisible = true
                        orderViewModel.clearError()
                    } ?: run { binding.tvError.isVisible = false }

                    if (state.orderCreated != null) {
                        orderViewModel.clearOrderCreated()
                        Toast.makeText(
                            requireContext(),
                            "Commande envoyée en cuisine pour préparation",
                            Toast.LENGTH_LONG
                        ).show()
                        // Retour a la liste des tables (la table apparait desormais occupee)
                        findNavController().popBackStack(R.id.tableListFragment, false)
                    }
                }
            }
        }
    }

    private fun formatAr(amount: Double) =
        "%,.0f Ar".format(amount).replace(",", " ")

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class CartAdapter(
    private val onIncrease: (CartItem) -> Unit,
    private val onDecrease: (CartItem) -> Unit
) : ListAdapter<CartItem, CartAdapter.VH>(DIFF) {

    inner class VH(val binding: ItemCartEntryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemCartEntryBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        with(holder.binding) {
            tvName.text = item.name
            tvUnitPrice.text = "%,.0f Ar".format(item.price).replace(",", " ")
            tvSubtotal.text = "%,.0f Ar".format(item.price * item.quantity).replace(",", " ")
            tvQty.text = item.quantity.toString()
            btnMinus.setOnClickListener { onDecrease(item) }
            btnPlus.setOnClickListener { onIncrease(item) }
        }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<CartItem>() {
            override fun areItemsTheSame(a: CartItem, b: CartItem) = a.menuItemId == b.menuItemId
            override fun areContentsTheSame(a: CartItem, b: CartItem) = a == b
        }
    }
}
