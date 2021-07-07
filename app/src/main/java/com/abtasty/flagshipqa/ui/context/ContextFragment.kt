package com.abtasty.flagshipqa.ui.context

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.abtasty.flagshipqa.databinding.FragmentContextBinding
import org.json.JSONObject


class ContextFragment : Fragment() {

    private lateinit var contextViewModel: ContextViewModel
    private var _binding: FragmentContextBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        contextViewModel = ViewModelProvider(this).get(ContextViewModel::class.java)
        _binding = FragmentContextBinding.inflate(inflater, container, false)

        contextViewModel.visitorContext.observe(viewLifecycleOwner, Observer {
            try {
                binding.editVisitorContext.setText(JSONObject(it.toString()).toString(4))
            } catch (e: Exception) {
                binding.editVisitorContext.setText("{\n\n}")
            }
        })

        binding.synchronize.setOnClickListener {
            contextViewModel.visitorContext.value =  binding.editVisitorContext.text.toString()
            contextViewModel.synchronize(
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