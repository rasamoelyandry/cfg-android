package com.cfg.android.ui.tables

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
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
import com.cfg.android.databinding.FragmentTableListBinding
import com.cfg.android.databinding.ItemTableBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TableListFragment : Fragment() {

    private var _binding: FragmentTableListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TableListViewModel by viewModels()
    private lateinit var adapter: TableAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        _binding = FragmentTableListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupRecyclerView()
        observeState()
        binding.swipeRefresh.setOnRefreshListener { viewModel.loadTables() }
    }

    override fun onResume() {
        super.onResume()
        // Revenir du panier/suivi de commande doit rafraichir l'occupation des tables
        // sans que le serveur ait a tirer manuellement sur l'ecran.
        viewModel.loadTables()
    }

    private fun setupToolbar() {
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_refresh -> { viewModel.loadTables(); true }
                R.id.action_logout  -> { confirmLogout(); true }
                else -> false
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = TableAdapter { tableWithOrder ->
            if (tableWithOrder.isOccupied) {
                // Table occupee : suivi de la commande (statut, marquer servi, encaisser) ou,
                // si deja payee, juste le bouton pour liberer la table.
                val bundle = bundleOf(
                    "tableId"     to tableWithOrder.table.id,
                    "tableNumber" to tableWithOrder.table.number,
                    "orderId"     to tableWithOrder.activeOrder?.id
                )
                findNavController().navigate(R.id.action_tableList_to_orderStatus, bundle)
            } else {
                // Table libre : demarrer une nouvelle commande
                val bundle = bundleOf(
                    "tableId"     to tableWithOrder.table.id,
                    "tableNumber" to tableWithOrder.table.number,
                    "tableLabel"  to tableWithOrder.table.label,
                    "orderId"     to null
                )
                findNavController().navigate(R.id.action_tableList_to_order, bundle)
            }
        }
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = GridLayoutManager(requireContext(), 3)
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.swipeRefresh.isRefreshing = state.isLoading && state.items.isEmpty()
                    binding.progressBar.isVisible = state.isLoading && state.items.isEmpty()
                    binding.tvOffline.isVisible = state.isOffline
                    binding.tvEmpty.isVisible = !state.isLoading && state.items.isEmpty() && state.error == null

                    state.error?.let { err ->
                        binding.tvError.text = err
                        binding.tvError.isVisible = true
                        viewModel.clearError()
                    } ?: run { binding.tvError.isVisible = false }

                    adapter.submitList(state.items)
                }
            }
        }
    }

    private fun confirmLogout() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Déconnexion")
            .setMessage("Voulez-vous vous déconnecter ?")
            .setPositiveButton("Déconnexion") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    viewModel.logout()
                    (activity as? TableListActivity)?.navigateToLogin()
                }
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class TableAdapter(
    private val onClick: (TableWithOrder) -> Unit
) : ListAdapter<TableWithOrder, TableAdapter.VH>(DIFF) {

    inner class VH(val binding: ItemTableBinding) : RecyclerView.ViewHolder(binding.root) {
        init { binding.root.setOnClickListener { onClick(getItem(adapterPosition)) } }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemTableBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        val table = item.table
        val order = item.activeOrder
        with(holder.binding) {
            tvTableNumber.text = "Table ${table.number}"
            tvCapacity.text = "${table.capacity} pers."
            tvLabel.text = table.label ?: ""
            tvLabel.isVisible = !table.label.isNullOrBlank()

            if (item.isOccupied) {
                chipStatus.text = "Occupée"
                chipStatus.setChipBackgroundColorResource(com.cfg.android.R.color.table_occupied_bg)
                chipStatus.setTextColor(root.context.getColor(com.cfg.android.R.color.table_occupied))
                tvCustomer.text = order?.customerName?.let { "Client : $it" } ?: ""
                tvCustomer.isVisible = !order?.customerName.isNullOrBlank()
                root.strokeColor = root.context.getColor(com.cfg.android.R.color.table_occupied)
                root.strokeWidth = 3
            } else {
                chipStatus.text = "Libre"
                chipStatus.setChipBackgroundColorResource(com.cfg.android.R.color.table_free_bg)
                chipStatus.setTextColor(root.context.getColor(com.cfg.android.R.color.table_free))
                tvCustomer.isVisible = false
                root.strokeColor = root.context.getColor(com.cfg.android.R.color.outline)
                root.strokeWidth = 1
            }
        }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<TableWithOrder>() {
            override fun areItemsTheSame(a: TableWithOrder, b: TableWithOrder) = a.table.id == b.table.id
            override fun areContentsTheSame(a: TableWithOrder, b: TableWithOrder) = a == b
        }
    }
}
