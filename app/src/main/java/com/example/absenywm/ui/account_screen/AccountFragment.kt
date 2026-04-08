package com.example.absenywm.ui.account_screen

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.absenywm.LoginActivity
import com.example.absenywm.databinding.FragmentAccountBinding

class AccountFragment : Fragment() {

    private var _binding: FragmentAccountBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val accountViewModel =
            ViewModelProvider(this).get(AccountViewModel::class.java)

        _binding = FragmentAccountBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textNotifications
        accountViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }

        val btnLogout = binding.btnLogout
        btnLogout.setOnClickListener {

            val sharedPref = requireActivity()
                .getSharedPreferences("USER_SESSION", Context.MODE_PRIVATE)

            val editor = sharedPref.edit()
            editor.clear()
            editor.apply()

            startActivity(Intent(requireActivity(), LoginActivity::class.java))
            requireActivity().finish()
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}