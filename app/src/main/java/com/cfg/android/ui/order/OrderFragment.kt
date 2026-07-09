package com.cfg.android.ui.order

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
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
import com.cfg.android.data.remote.dto.ModifierDto
import com.cfg.android.ui.menu.MenuViewModel
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
        setupSearch()
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
            onAdd = { item -> handleAddTapped(item) },
            onRemove = { item ->
                // Retire la derniere ligne ajoutee pour cet article (les options fines se gerent depuis le panier)
                val match = orderViewModel.uiState.value.cart.lastOrNull { it.menuItemId == item.id }
                if (match != null) orderViewModel.updateQuantity(match.cartItemId, match.quantity - 1)
            },
            getQuantity = { item -> orderViewModel.quantityFor(item.id) }
        )
        binding.recyclerMenu.adapter = menuAdapter
        binding.recyclerMenu.layoutManager = GridLayoutManager(requireContext(), 2)
    }

    private fun handleAddTapped(item: MenuItemDto) {
        if (item.modifiers.isEmpty()) {
            orderViewModel.addToCart(item.id, item.name, item.price)
        } else {
            showOptionsDialog(item)
        }
    }

    private fun showOptionsDialog(item: MenuItemDto) {
        val context = requireContext()
        val density = resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(12), dp(24), 0)
        }

        val checkBoxes = item.modifiers.map { modifier ->
            CheckBox(context).apply {
                text = if (modifier.priceDelta > 0)
                    "${modifier.name} (+${formatAr(modifier.priceDelta)})"
                else modifier.name
                tag = modifier
            }
        }
        checkBoxes.forEach { container.addView(it) }

        val notesInput = EditText(context).apply {
            hint = getString(R.string.hint_item_notes)
            setPadding(0, dp(16), 0, 0)
        }
        container.addView(notesInput)

        val scroll = ScrollView(context).apply { addView(container) }

        MaterialAlertDialogBuilder(context)
            .setTitle(item.name)
            .setView(scroll)
            .setPositiveButton(R.string.btn_add) { _, _ ->
                val selected = checkBoxes.filter { it.isChecked }.map { it.tag as ModifierDto }
                val notes = notesInput.text?.toString()?.trim()?.ifEmpty { null }
                orderViewModel.addToCart(
                    menuItemId = item.id,
                    name = item.name,
                    price = item.price,
                    modifierIds = selected.map { it.id },
                    modifierNames = selected.map { it.name },
                    notes = notes
                )
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }

    private fun setupSearch() {
        binding.etSearchMenu.doAfterTextChanged { text ->
            menuViewModel.setSearchQuery(text?.toString().orEmpty())
        }
    }

    private fun observeMenuState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                menuViewModel.uiState.collect { state ->
                    binding.progressMenu.isVisible = state.isLoading
                    binding.scrollCategories.isVisible = !state.isSearching

                    val noResults = state.error == null && state.isSearching && state.displayedItems.isEmpty()
                    binding.tvMenuError.isVisible = state.error != null || noResults
                    binding.tvMenuError.text = state.error ?: getString(R.string.no_search_results)

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
