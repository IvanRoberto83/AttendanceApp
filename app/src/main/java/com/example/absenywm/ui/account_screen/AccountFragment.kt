package com.example.absenywm.ui.account_screen

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.absenywm.LoginActivity
import com.example.absenywm.databinding.FragmentAccountBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AccountFragment : Fragment() {

    private var _binding: FragmentAccountBinding? = null
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

        val sharedPref = requireActivity().getSharedPreferences("USER_SESSION", AppCompatActivity.MODE_PRIVATE)
        val username = sharedPref.getString("USERNAME", "User")
        if (username != null) {
            binding.tvAvatarInitials.text = username.firstOrNull()?.uppercase() ?: "?"
            binding.tvProfileName.text = username
        }

        val jabatan = sharedPref.getString("JABATAN", "Staff Operasional")
        binding.tvProfileRole.text = jabatan
        binding.tvDepartment.text = jabatan

        val id = sharedPref.getString("IDKARYAWAN", "YWM-001")
        binding.tvEmployeeId.text = id

        val phoneNumber = sharedPref.getString("PHONENUM", "08123456789")
        binding.tvPhoneNumber.text = phoneNumber

        val jamKerja = sharedPref.getString("JAMKERJA","08:00 – 15:00")
        binding.tvWorkHours.text = jamKerja

        binding.btnLogout.setOnClickListener {
            showLogoutDialog()
        }

        return root
    }

    private fun showLogoutDialog() {
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Keluar")
            .setMessage("Yakin ingin keluar dari akun?")
            .setPositiveButton("Ya") { _, _ ->
                logout()
            }
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

        val editor = sharedPref.edit()
        editor.clear()
        editor.apply()

        startActivity(Intent(requireActivity(), LoginActivity::class.java))
        requireActivity().finish()
    }

    private fun showDeleteAccountDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Hapus Akun")
            .setMessage("Akun akan dihapus permanen. Lanjutkan?")
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton("Hapus") { _, _ ->
                // TODO hapus akun
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}