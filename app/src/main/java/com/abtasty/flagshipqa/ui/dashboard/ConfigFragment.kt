package com.abtasty.flagshipqa.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.abtasty.flagshipqa.databinding.FragmentConfigBinding
import org.json.JSONObject


class ConfigFragment : Fragment() {

    private lateinit var dashboardViewModel: ConfigViewModel
    private var _binding: FragmentConfigBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        dashboardViewModel = ViewModelProvider(this).get(ConfigViewModel::class.java)
        _binding = FragmentConfigBinding.inflate(inflater, container, false)

        dashboardViewModel.api_key.observe(viewLifecycleOwner, Observer {
            binding.editTextApiKey.setText(it)
        })
        dashboardViewModel.env_id.observe(viewLifecycleOwner, Observer {
            binding.editTextEnvId.setText(it)
        })
        dashboardViewModel.useBucketing.observe(viewLifecycleOwner, Observer {
            binding.toggleButton.isChecked = it
        })
        dashboardViewModel.useBucketing.observe(viewLifecycleOwner, Observer {
            binding.toggleButton.isChecked = it
        })
        dashboardViewModel.timeout.observe(viewLifecycleOwner, Observer {
            binding.editTextTimeout.setText(it.toString())
        })

        dashboardViewModel.visitorId.observe(viewLifecycleOwner, Observer {
            binding.editTextVisitorId.setText(it.toString())
        })

        dashboardViewModel.hasConsented.observe(viewLifecycleOwner, {
            binding.chip4.isChecked
        })

        dashboardViewModel.pollingIntervalTime.observe(viewLifecycleOwner, {
            if (binding.editTextPolling.text.isNotEmpty())
                binding.editTextPolling.text.toString().toLong()
        })

        dashboardViewModel.pollingIntervalUnit.observe(viewLifecycleOwner, {
            binding.spinnerPolling.selectedItem.toString()
        })

        dashboardViewModel.visitorContext.observe(viewLifecycleOwner, Observer {
            try {
                binding.editVisitorContext.setText(JSONObject(it.toString()).toString(4))
            } catch (e: Exception) {
                binding.editVisitorContext.setText("{\n\n}")
            }
        })

        binding.start.setOnClickListener {
            dashboardViewModel.env_id.value = binding.editTextEnvId.text.toString()
            dashboardViewModel.useBucketing.value = binding.toggleButton.isChecked
            dashboardViewModel.isAuthenticated.value = binding.toggleButton2.isChecked
            dashboardViewModel.timeout.value = if (binding.editTextTimeout.text.toString().isNotEmpty()
            ) binding.editTextTimeout.text.toString().toInt() else 0
            dashboardViewModel.api_key.value = binding.editTextApiKey.text.toString()
            dashboardViewModel.visitorId.value = binding.editTextVisitorId.text.toString()
            dashboardViewModel.visitorContext.value = binding.editVisitorContext.text.toString()
            dashboardViewModel.hasConsented.value = binding.chip4.isChecked
            if (binding.editTextPolling.text.isNotEmpty())
                dashboardViewModel.pollingIntervalTime.value = binding.editTextPolling.text.toString().toLong()
            dashboardViewModel.pollingIntervalUnit.value = binding.spinnerPolling.selectedItem.toString()
            dashboardViewModel.saveLastConf()
            startFlagship()
        }

        binding.add.setOnClickListener { v ->
            showPopupMenu(v)
        }
        return binding.root
    }

    fun showPopupMenu(v: View) {
        val popup = PopupMenu(requireContext(), v)
        var i = 0
        for (e in dashboardViewModel.env_ids) {
            popup.menu.add(0, i, i, e.key + " - " + e.value)
            i++
        }
        popup.setOnMenuItemClickListener { item ->
            val split = item.title.toString().split(" - ")
            if (split.size > 1)
                binding.editTextEnvId.setText(split[1])
            true
        }
        popup.show();
    }

    fun startFlagship() {
        dashboardViewModel.startFlagship(
            { visitor ->
                activity?.runOnUiThread {
                    Toast.makeText(requireContext(), "Started", Toast.LENGTH_SHORT).show()
                }
                println("#DB here 1: " + Thread.currentThread().name)
                visitor.collectEmotionsAIEvents(activity)
                println("#DB here 2: " + Thread.currentThread().name)
            },
            { error ->
                Toast.makeText(requireContext(), "Error : $error", Toast.LENGTH_SHORT).show()
            })
    }
}