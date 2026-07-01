package com.cfg.android.ui.order

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cfg.android.R
import com.cfg.android.databinding.FragmentOrderBinding
import com.cfg.android.databinding.ItemMenuItemBinding
import com.cfg.android.data.remote.dto.MenuItemDto
import com.cfg.android.ui.menu.MenuViewModel
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class OrderFragment : Fragment() {

    private var _binding: FragmentOrderBinding? = null
    private val binding get() = _binding!!

    private val menuViewModel: MenuViewModel by viewModels()
    private val orderViewModel: OrderViewModel by activityViewModels()

    private lateinit var menuAdapter: MenuItemAdapter

    private var tableId: String? = null
    private var tableNumber: Int = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        _binding = FragmentOrderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tableId = arguments?.getString("tableId")
        tableNumber = arguments?.getInt("tableNumber") ?: 0
        val orderId = arguments?.getString("orderId")

        // Reset cart on new order (not for existing order)
        if (orderId == null) orderViewModel.clearCart()
        orderViewModel.setTableId(tableId, tableNumber)

        setupToolbar()
        setupRecyclerView()
        observeMenuState()
        observeCartState()

        menuViewModel.load()

        binding.btnViewCart.setOnClickListener {
            findNavController().navigate(R.id.action_order_to_cart)
        }
    }

    private fun setupToolbar() {
        binding.toolbar.title = "Table $tableNumber"
        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }
    }

    private fun setupRecyclerView() {
        menuAdapter = MenuItemAdapter(
            onAdd = { item ->
                orderViewModel.addToCart(item.id, item.name, item.price)
            },
            onRemove = { item ->
                orderViewModel.removeFromCart(item.id)
            },
            getQuantity = { item ->
                orderViewModel.uiState.value.cart.firstOrNull { it.menuItemId == item.id }?.quantity ?: 0
            }
        )
        binding.recyclerMenu.adapter = menuAdapter
        binding.recyclerMenu.layoutManager = GridLayoutManager(requireContext(), 2)
    }

    private fun observeMenuState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                menuViewModel.uiState.collect { state ->
                    binding.progressMenu.isVisible = state.isLoading
                    binding.tvMenuError.isVisible = state.error != null
                    binding.tvMenuError.text = state.error

                    // Build category chips
                    if (state.categories.isNotEmpty() && binding.chipGroupCategories.childCount == 0) {
                        state.categories.forEach { cat ->
                            val chip = Chip(requireContext()).apply {
                                text = cat.name
                                isCheckable = true
                                tag = cat.id
                                setOnClickListener { menuViewModel.selectCategory(cat.id) }
                            }
                            binding.chipGroupCategories.addView(chip)
                        }
                        // Auto-select first
                        (binding.chipGroupCategories.getChildAt(0) as? Chip)?.isChecked = true
                    }

                    menuAdapter.submitList(state.displayedItems)
                }
            }
        }
    }

    private fun observeCartState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                orderViewModel.uiState.collect { state ->
                    val count = state.cart.sumOf { it.quantity }
                    val total = orderViewModel.totalAmount()
                    binding.tvCartSummary.text = if (count > 0)
                        "Panier ($count) — ${formatAr(total)}"
                    else "Panier vide"
                    binding.btnViewCart.isEnabled = count > 0
                    // Refresh menu adapter to update quantity badges
                    menuAdapter.notifyDataSetChanged()
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

class MenuItemAdapter(
    private val onAdd: (MenuItemDto) -> Unit,
    private val onRemove: (MenuItemDto) -> Unit,
    private val getQuantity: (MenuItemDto) -> Int
) : ListAdapter<MenuItemDto, MenuItemAdapter.VH>(DIFF) {

    inner class VH(val binding: ItemMenuItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemMenuItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        val qty = getQuantity(item)
        with(holder.binding) {
            tvName.text = item.name
            tvDescription.text = item.description ?: ""
            tvDescription.isVisible = !item.description.isNullOrBlank()
            tvPrice.text = "%,.0f Ar".format(item.price).replace(",", " ")
            tvUnavailable.isVisible = !item.isAvailable

            if (qty > 0) {
                btnAdd.isVisible = false
                layoutQty.isVisible = true
                tvQty.text = qty.toString()
            } else {
                btnAdd.isVisible = item.isAvailable
                layoutQty.isVisible = false
            }

            btnAdd.setOnClickListener { onAdd(item) }
            btnPlus.setOnClickListener { onAdd(item) }
            btnMinus.setOnClickListener { onRemove(item) }
            root.alpha = if (item.isAvailable) 1f else 0.5f
        }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<MenuItemDto>() {
            override fun areItemsTheSame(a: MenuItemDto, b: MenuItemDto) = a.id == b.id
            override fun areContentsTheSame(a: MenuItemDto, b: MenuItemDto) = a == b
        }
    }
}
