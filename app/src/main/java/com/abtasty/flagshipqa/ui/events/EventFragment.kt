package com.abtasty.flagshipqa.ui.events

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.abtasty.flagshipqa.MainActivity2
import com.abtasty.flagshipqa.R
import com.abtasty.flagshipqa.databinding.FragmentEventBinding


class EventFragment : Fragment() {

    private lateinit var eventViewModel: EventViewModel
    private var _binding: FragmentEventBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        eventViewModel = ViewModelProvider(this).get(EventViewModel::class.java)
        _binding = FragmentEventBinding.inflate(inflater, container, false)

        binding.spinnerEvent.adapter = ArrayAdapter(requireContext(), R.layout.spinner_elem, eventViewModel.actions)
        binding.buttonPage.setOnClickListener {
            eventViewModel.sendScreen(binding.editTextInterface.text.toString())
        }
        binding.buttonEvent.setOnClickListener {
            eventViewModel.sendEvent(
                binding.spinnerEvent.selectedItem.toString(),
                binding.editTextEventAction.text.toString()
            )
        }
        binding.buttonTransaction.setOnClickListener {
            eventViewModel.sendTransaction(
                binding.editTextTransactionId.text.toString(),
                binding.editTextAffiliation.text.toString()
            )
        }
        binding.buttonItem.setOnClickListener {
            eventViewModel.sendItem(
                binding.editTextItemTransactionId.text.toString(),
                binding.editTextProductName.text.toString(),
                binding.editTextProductSku.text.toString()
            )
        }
        binding.buttonNextActivity.setOnClickListener {
            val intent = Intent(this.context, MainActivity2::class.java)
            startActivity(intent)
        }
        return binding.root
    }
}