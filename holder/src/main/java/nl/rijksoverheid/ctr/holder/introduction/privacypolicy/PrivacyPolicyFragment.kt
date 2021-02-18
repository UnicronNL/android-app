package nl.rijksoverheid.ctr.holder.introduction.privacypolicy

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.xwray.groupie.GroupieAdapter
import com.xwray.groupie.Section
import nl.rijksoverheid.ctr.holder.HideToolbar
import nl.rijksoverheid.ctr.holder.R
import nl.rijksoverheid.ctr.holder.databinding.FragmentPrivacyPolicyBinding
import nl.rijksoverheid.ctr.holder.introduction.IntroductionViewModel
import nl.rijksoverheid.ctr.holder.introduction.privacypolicy.models.PrivacyPolicyItem
import org.koin.androidx.viewmodel.ext.android.viewModel

/*
 *  Copyright (c) 2021 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */
class PrivacyPolicyFragment : Fragment(), HideToolbar {

    private lateinit var binding: FragmentPrivacyPolicyBinding
    private val introductionViewModel: IntroductionViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPrivacyPolicyBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            val canPop = findNavController().popBackStack()
            if (!canPop) {
                requireActivity().finish()
            }
        }

        val adapterItems = listOf(
            PrivacyPolicyItem(
                R.drawable.shield,
                R.string.privacy_policy_1
            ),
            PrivacyPolicyItem(
                R.drawable.shield,
                R.string.privacy_policy_2
            ),
            PrivacyPolicyItem(
                R.drawable.shield,
                R.string.privacy_policy_1
            ),
            PrivacyPolicyItem(
                R.drawable.shield,
                R.string.privacy_policy_2
            ),
            PrivacyPolicyItem(
                R.drawable.shield,
                R.string.privacy_policy_1
            ),
            PrivacyPolicyItem(
                R.drawable.shield,
                R.string.privacy_policy_2
            ),
            PrivacyPolicyItem(
                R.drawable.shield,
                R.string.privacy_policy_1
            ),
            PrivacyPolicyItem(
                R.drawable.shield,
                R.string.privacy_policy_2
            )
        ).map {
            PrivacyPolicyAdapterItem(
                it
            )
        }

        val adapter = GroupieAdapter()
        val section = Section()
        binding.items.adapter = adapter
        adapter.add(section)
        section.update(adapterItems)

        binding.checkbox.setOnCheckedChangeListener { _, isChecked ->
            binding.button.isEnabled = isChecked
        }

        binding.button.setOnClickListener {
            introductionViewModel.setPrivacyPolicyFinished()
            findNavController().navigate(PrivacyPolicyFragmentDirections.actionHome())
        }

    }
}