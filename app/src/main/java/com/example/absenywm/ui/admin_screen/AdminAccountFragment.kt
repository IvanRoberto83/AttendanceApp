package com.example.absenywm.ui.admin_screen

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.absenywm.EditAdminAccountActivity
import com.example.absenywm.LoginActivity
import com.example.absenywm.databinding.FragmentAdminAccountBinding

class AdminAccountFragment : Fragment() {

    private var _binding: FragmentAdminAccountBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: AdminAccountViewModel

    private val editLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if(result.resultCode == android.app.Activity.RESULT_OK){
            viewModel.loadUserData(requireContext())
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[AdminAccountViewModel::class.java]

        viewModel.username.observe(viewLifecycleOwner) { binding.tvAvatarInitials.text = it.firstOrNull()?.uppercase() }
        viewModel.username.observe(viewLifecycleOwner) { binding.tvProfileName.text = it }
        viewModel.role.observe(viewLifecycleOwner) { binding.tvProfileRole.text = it }
        viewModel.email.observe(viewLifecycleOwner) { binding.tvEmail.text = it }
        viewModel.role.observe(viewLifecycleOwner) { binding.tvRole.text = it }
        viewModel.phoneNumber.observe(viewLifecycleOwner) { binding.tvPhoneNumber.text = it }

        viewModel.loadUserData(requireContext())

        binding.btnLogout.setOnClickListener {
            showLogoutDialog()
        }

        binding.btnDeleteAccount.setOnClickListener {
            showDeleteAccountDialog()
        }

        binding.btnEditProfile.setOnClickListener {
            val intent = Intent(requireContext(), EditAdminAccountActivity::class.java)
            editLauncher.launch(intent)
        }
    }

    private fun showLogoutDialog() {
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Keluar")
            .setMessage("Yakin ingin keluar dari akun?")
            .setPositiveButton("Ya") { _, _ -> logout() }
            .setNegativeButton("Batal", null)
            .create()

        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            .setTextColor(resources.getColor(android.R.color.holo_red_dark))
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            .setTextColor(resources.getColor(android.R.color.black))
    }

    private fun logout() {
        val sharedPref = requireActivity()
            .getSharedPreferences("USER_SESSION", Context.MODE_PRIVATE)
        sharedPref.edit().clear().apply()

        startActivity(Intent(requireActivity(), LoginActivity::class.java))
        requireActivity().finish()
    }

    private fun showDeleteAccountDialog() {
        val alertDialog = AlertDialog.Builder(requireContext())
            .setTitle("Hapus Akun")
            .setMessage("Akun akan dihapus permanen. Lanjutkan?")
            .setPositiveButton("Hapus") { _, _ ->
                viewModel.deleteAccount(requireContext(),
                    onSuccess = {
                        val sharedPref = requireActivity()
                            .getSharedPreferences("USER_SESSION", Context.MODE_PRIVATE)
                        sharedPref.edit().clear().apply()

                        startActivity(Intent(requireActivity(), LoginActivity::class.java))
                        requireActivity().finish()
                    },
                    onError = {
                        Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                    }
                )
            }
            .setNegativeButton("Batal", null)
            .create()

        alertDialog.show()
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
            .setTextColor(resources.getColor(android.R.color.holo_red_dark))
        alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            .setTextColor(resources.getColor(android.R.color.black))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}