package com.abtasty.flagshipqa.ui.modifications

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.abtasty.flagshipqa.R
import com.abtasty.flagshipqa.databinding.FragmentModificationsBinding
import org.json.JSONObject


class ModificationFragment : Fragment() {

    private lateinit var modificationViewModel: ModificationViewModel
    private var _binding: FragmentModificationsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        modificationViewModel = ViewModelProvider(this).get(ModificationViewModel::class.java)
        _binding = FragmentModificationsBinding.inflate(inflater, container, false)

        binding.editModifications.keyListener = null
        modificationViewModel.modifications.observe(viewLifecycleOwner, Observer {
            try {
                binding.editModifications.setText(JSONObject(it.toString()).toString(4))
            } catch (e: Exception) {
                binding.editModifications.setText("{\n\n}")
            }
        })

        binding.switchView.setOnClickListener {
            toggleView()
        }

        binding.spinner.adapter = ArrayAdapter(requireContext(), R.layout.spinner_elem, modificationViewModel.types)

        binding.getModificationValue.setOnClickListener {
            modificationViewModel.getModification(
                binding.editTextKey.text.toString(),
                binding.editTextDefault.text.toString(),
                binding.spinner.selectedItem.toString()
            )
        }

        modificationViewModel.value.observe(viewLifecycleOwner, Observer {
            binding.editTextResultValue.setText(it.toString())
        })

        modificationViewModel.info.observe(viewLifecycleOwner, Observer {
//            root.edit_text_result_campaign.setText(it.optString("campaignId", "unknown"))
//            root.edit_text_result_group.setText(it.optString("variationGroupId", "unknown"))
//            root.edit_text_result_variation.setText(it.optString("variationId", "unknown"))
            binding.editTextInfo.setText(it.toString(4))
        })

        binding.activate.setOnClickListener {
            modificationViewModel.activate(binding.editTextKey.text.toString())
        }

        return binding.root
    }

    fun toggleView() {
        binding.getModifications.visibility = if (binding.getModifications.visibility == View.INVISIBLE) View.VISIBLE else View.INVISIBLE
        binding.editModifications.visibility = if (binding.editModifications.visibility == View.INVISIBLE) View.VISIBLE else View.INVISIBLE
        binding.switchView.setText(if (binding.editModifications.visibility == View.VISIBLE) R.string.fragment_modifications_view_compute else R.string.fragment_modifications_view_json)
    }
}