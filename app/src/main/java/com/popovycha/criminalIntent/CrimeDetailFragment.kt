package com.popovycha.criminalIntent

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.text.format.DateFormat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.popovycha.criminalIntent.databinding.FragmentCrimeDetailBinding
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID

private const val DATE_FORMAT = "EEE, MMM, dd"

class CrimeDetailFragment : Fragment() {

    private lateinit var crime: Crime

    private val args: CrimeDetailFragmentArgs by navArgs()

    private val crimeDetailViewModel: CrimeDetailViewModel by viewModels {
        CrimeDetailViewModelFactory(args.crimeId)
    }
    private val selectSuspect = registerForActivityResult(
        ActivityResultContracts.PickContact()
    ) { uri: Uri? ->
        uri?.let { parseContactSelection(it) }
    }
    private var _binding: FragmentCrimeDetailBinding? = null
    private val binding
        get() = checkNotNull(_binding) {
            "Cannot access binding because it is null. Is the view visible?"
        }

    //function where you inflate and bind the layout for the fragment’s
    //view and return the inflated View to the hosting activity
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding =
            FragmentCrimeDetailBinding.inflate(layoutInflater, container, false)
        return binding.root
    }
    //wire up the views
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            crimeTitle.doOnTextChanged { text, _, _, _ ->
                crimeDetailViewModel.updateCrime { oldCrime ->
                    oldCrime.copy(title = text.toString())
                }
            }
            crimeSolved.setOnCheckedChangeListener { _, isChecked ->
                crimeDetailViewModel.updateCrime { oldCrime ->
                    oldCrime.copy(isSolved = isChecked)
                }
            }
            //challenge 16
            crimeSuspect.setOnClickListener {
                if (isPermissionGranted()) {
                    selectSuspect.launch(null)
                } else {
                    onClickRequestPermission()
                }
            }
            //challenge 16
            crimeCallSuspect.isEnabled = isPermissionGranted()
            crimeCallSuspect.setOnClickListener {
                val intent = Intent(Intent.ACTION_DIAL)
                intent.data = Uri.parse("tel:${crimeCallSuspect.text}")
                startActivity(intent)
            }

            val selectSuspectIntent = selectSuspect.contract.createIntent(
                requireContext(),
                null
            )
            crimeSuspect.isEnabled = canResolveIntent(selectSuspectIntent)
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                crimeDetailViewModel.crime.collect { crime ->
                    crime?.let { updateUi(it) }
                }
            }
        }
        setFragmentResultListener(
            DatePickerFragment.REQUEST_KEY_DATE
        ) { _ , bundle ->
            //challenge 16
            val newDate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                bundle.getSerializable(DatePickerFragment.BUNDLE_KEY_DATE, Date::class.java) as Date
            } else {
                bundle.getSerializable(DatePickerFragment.BUNDLE_KEY_DATE) as Date
            }
            crimeDetailViewModel.updateCrime { it.copy(date = newDate) }
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    private fun updateUi(crime: Crime) {
        binding.apply {
            if (crimeTitle.text.toString() != crime.title) {
                crimeTitle.setText(crime.title)
            }
            crimeDate.text = crime.date.toString()
            crimeDate.setOnClickListener {
                findNavController().navigate(
                    CrimeDetailFragmentDirections.selectDate(crime.date)
                )
            }
            crimeSolved.isChecked = crime.isSolved

            crimeReport.setOnClickListener {
                val reportIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, getCrimeReport(crime))
                    putExtra(
                        Intent.EXTRA_SUBJECT,
                        getString(R.string.crime_report_subject)
                    )
                }
                val chooserIntent = Intent.createChooser(
                    reportIntent,
                    getString(R.string.send_report)
                )
                startActivity(chooserIntent)
            }
            //challenge 16th
            crimeSuspect.text = crime.suspect.ifEmpty {
                getString(R.string.crime_suspect_text)
            }
            crimeCallSuspect.isEnabled = isPermissionGranted() && crime.phone.isNotEmpty()
            crimeCallSuspect.text = crime.phone.ifEmpty {
                getString(R.string.crime_call_suspect_text)
            }
        }
    }
    private fun getCrimeReport(crime: Crime): String {
        val solvedString = if (crime.isSolved) {
            getString(R.string.crime_report_solved)
        } else {
            getString(R.string.crime_report_unsolved)
        }
        val dateString = DateFormat.format(DATE_FORMAT, crime.date).toString()
        val suspectText = if (crime.suspect.isBlank()) {
            getString(R.string.crime_report_no_suspect)
        } else {
            getString(R.string.crime_report_suspect, crime.suspect)
        }
        return getString(
            R.string.crime_report,
            crime.title, dateString, solvedString, suspectText
        )
    }
//query that asks for all the display names of
//the contacts in the returned data
    @SuppressLint("Range")
    private fun parseContactSelection(contactUri: Uri) {
        val queryFields = arrayOf(ContactsContract.Contacts.DISPLAY_NAME,
            ContactsContract.Contacts._ID)
    //Cursor points to a database table
    //containing a single row and a single column
        val queryCursor = requireActivity().contentResolver
            .query(contactUri,
                queryFields,
                null,
                null,
                null)

        var contactID: String? = null
        queryCursor?.use { cursor ->
            //It moves the cursor to the first row, and it returns a Boolean
            if (cursor.moveToFirst()) {
                val suspect =
                    cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))

                //the suspect information is stored here
                crimeDetailViewModel.updateCrime { oldCrime ->
                    oldCrime.copy(suspect = suspect)
                    //UI will update as it observes the StateFlow’s changes
                }
                contactID = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID))
            }
        }
        if (contactID != null) {
           val phone = getPhoneNumberById(requireActivity(), contactID!!)
           crimeDetailViewModel.updateCrime { oldCrime ->
               oldCrime.copy(phone = phone)
        }
      }
    }
    //challenge 16
    @SuppressLint("Range")
    fun getPhoneNumberById(context: Context, contactId: String): String {

        val queryCursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
            arrayOf(contactId),
            null
        )

        queryCursor?.use { cursor ->
            return if (cursor.moveToFirst()) {
                cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
            } else {
                ""
            }
        }
        return ""
    }

    private fun canResolveIntent(intent: Intent): Boolean {
        val packageManager: PackageManager = requireActivity().packageManager
        val resolvedActivity: ResolveInfo? =
            packageManager.resolveActivity(
                intent,
                PackageManager.MATCH_DEFAULT_ONLY
            )
        return resolvedActivity != null
    }
    //challenge 16 until the end
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.i("Permission: ", "Granted")
            binding.crimeCallSuspect.isEnabled = true
            selectSuspect.launch(null)
        } else {
            Log.i("Permission: ", "Denied")
            binding.crimeCallSuspect.isEnabled = false
        }
    }

    private fun onClickRequestPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(), android.Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED -> {
                Toast.makeText(requireContext(), "Permission Granted", Toast.LENGTH_SHORT).show()
            }

            ActivityCompat.shouldShowRequestPermissionRationale(
                requireActivity(), android.Manifest.permission.READ_CONTACTS
            ) -> {
                Toast.makeText(requireContext(), "Permission Required", Toast.LENGTH_SHORT).show()
                requestPermissionLauncher.launch(
                    android.Manifest.permission.READ_CONTACTS
                )
            }

            else -> {
                requestPermissionLauncher.launch(
                    android.Manifest.permission.READ_CONTACTS
                )
            }
        }
    }

    private fun isPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(), android.Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }
}
