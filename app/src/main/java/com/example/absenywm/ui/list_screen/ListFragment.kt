package com.example.absenywm.ui.list_screen

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.absenywm.databinding.FragmentListBinding
import java.text.SimpleDateFormat
import java.util.*

class ListFragment : Fragment() {

    private var _binding: FragmentListBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ListViewModel
    private lateinit var adapter: AbsenAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentListBinding.inflate(inflater, container, false)

        viewModel = ViewModelProvider(this)[ListViewModel::class.java]

        adapter = AbsenAdapter(mutableListOf())
        binding.rvAttendance.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAttendance.adapter = adapter

        setupMonth()
        setupObserver()
        setupFilter()

        viewModel.loadMonthlyHistory()

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadMonthlyHistory()
    }

    private fun setupMonth() {
        val format = SimpleDateFormat("MMMM yyyy", Locale("id", "ID"))
        binding.tvMonthLabel.text = format.format(Date())
    }

    private fun setupObserver() {

        viewModel.absenList.observe(viewLifecycleOwner) {
            adapter.updateData(it)
        }

        viewModel.isEmpty.observe(viewLifecycleOwner) { isEmpty ->
            binding.layoutEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
            binding.rvAttendance.visibility = if (isEmpty) View.GONE else View.VISIBLE
        }
    }

    private fun setupFilter() {
        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->

            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener

            val filter = when (checkedIds[0]) {
                binding.chipHadir.id -> "Hadir"
                binding.chipTelat.id -> "Telat"
                binding.chipSakit.id -> "Sakit"
                binding.chipIzin.id -> "Izin"
                else -> "Semua"
            }

            viewModel.applyFilter(filter)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}