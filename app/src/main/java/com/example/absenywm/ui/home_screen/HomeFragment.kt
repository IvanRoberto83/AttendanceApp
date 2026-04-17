package com.example.absenywm.ui.home_screen

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.absenywm.databinding.FragmentHomeBinding
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val sharedPref = requireActivity().getSharedPreferences("USER_SESSION", AppCompatActivity.MODE_PRIVATE)
        val username = sharedPref.getString("USERNAME", "User")
        binding.tvUserName.text = username

        val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID"))
        val currentDate = dateFormat.format(Date())
        binding.tvDate.text = currentDate

        binding.btnCheckIn.setOnClickListener {
            val intent = Intent(requireContext(), AbsensiActivity::class.java)
            intent.putExtra("type", "masuk")
            startActivity(intent)
        }

        binding.btnCheckOut.setOnClickListener {
            val intent = Intent(requireContext(), AbsensiActivity::class.java)
            intent.putExtra("type", "keluar")
            startActivity(intent)
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}