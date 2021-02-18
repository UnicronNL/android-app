package nl.rijksoverheid.ctr.holder.myoverview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.doOnPreDraw
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import nl.rijksoverheid.ctr.holder.R
import nl.rijksoverheid.ctr.holder.databinding.FragmentMyOverviewBinding
import nl.rijksoverheid.ctr.holder.digid.DigiDFragment
import nl.rijksoverheid.ctr.holder.models.LocalTestResult
import nl.rijksoverheid.ctr.shared.livedata.EventObserver
import org.koin.androidx.viewmodel.ViewModelOwner
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/*
 *  Copyright (c) 2021 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */
class MyOverviewFragment : DigiDFragment() {

    private lateinit var binding: FragmentMyOverviewBinding
    private val localTestResultViewModel: LocalTestResultViewModel by sharedViewModel(
        owner = {
            ViewModelOwner.from(
                findNavController().getViewModelStoreOwner(R.id.nav_home),
                this
            )
        }
    )
    private val qrCodeViewModel: QrCodeViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMyOverviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.createQrCard.createQrCardButton.setOnClickListener {
            findNavController().navigate(MyOverviewFragmentDirections.actionChooseProvider())
        }

        binding.createQrCard.createQrCardButton.setOnClickListener {
            findNavController().navigate(MyOverviewFragmentDirections.actionChooseProvider())
        }

        binding.qrCard.root.setOnClickListener {
            findNavController().navigate(MyOverviewFragmentDirections.actionQrCode())
        }

        localTestResultViewModel.localTestResultLiveData.observe(viewLifecycleOwner, EventObserver {
            presentLocalTestResult(it)
        })

        qrCodeViewModel.qrCodeLiveData.observe(viewLifecycleOwner, EventObserver {
            binding.qrCard.qrCardQrImage.setImageBitmap(it)
        })

        localTestResultViewModel.getLocalTestResult(OffsetDateTime.now())
    }

    private fun presentLocalTestResult(localTestResult: LocalTestResult) {
        binding.qrCard.cardFooter.text = getString(
            R.string.my_overview_existing_qr_date, localTestResult.expireDate.format(
                DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
            )
        )

        binding.qrCard.root.visibility = View.VISIBLE

        binding.qrCard.qrCardQrImage.doOnPreDraw {
            lifecycleScope.launchWhenResumed {
                localTestResultViewModel.retrievedLocalTestResult?.credentials?.let { credentials ->
                    qrCodeViewModel.generateQrCode(
                        credentials = credentials,
                        qrCodeSize = binding.qrCard.qrCardQrImage.width,
                    )
                }
            }
        }
    }
}