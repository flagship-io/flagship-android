package com.abtasty.flagshipqa.ui.visitor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.abtasty.flagship.main.Flagship
import com.abtasty.flagshipqa.R
import com.abtasty.flagshipqa.databinding.FragmentVisitorBinding


class VisitorFragment : Fragment() {

    private lateinit var visitorViewModel: VisitorViewModel
    private var _binding: FragmentVisitorBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        visitorViewModel = ViewModelProvider(this).get(VisitorViewModel::class.java)
        _binding = FragmentVisitorBinding.inflate(inflater, container, false)

        visitorViewModel.visitorId.observe(viewLifecycleOwner, Observer {
            try {
                binding.editTextVisitorId.setText(it)
            } catch (e: Exception) {
                binding.editTextVisitorId.setText("error")
            }
        })

        visitorViewModel.anonymousId.observe(viewLifecycleOwner, Observer {
            try {
                binding.editTextAnonymousId.setText(it)
            } catch (e: Exception) {
                binding.editTextAnonymousId.setText("error")
            }
        })

        binding.authenticate.setOnClickListener {
            val newId = binding.editTextAuthenticateId.text.toString()
            if (newId.isNotEmpty())
                visitorViewModel.authenticate(newId)
            else {
                activity?.runOnUiThread {
                    Toast.makeText(context, context?.resources?.getString(R.string.fragment_visitor_authenticated_id_empty), Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.chip4.isChecked = Flagship.getVisitor()?.hasConsented() ?: true
        binding.chip4.setOnCheckedChangeListener { v: View, isChecked : Boolean ->
            visitorViewModel.setConsent(isChecked)
        }

        binding.unauthenticate.setOnClickListener {
            visitorViewModel.unauthenticate()
        }

        binding.fragementVisitorSynchronize.setOnClickListener {
            visitorViewModel.synchronize(
                { message ->
                    activity?.runOnUiThread {
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                },
                { error ->
                    activity?.runOnUiThread {
                        Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
        return binding.root
    }
}