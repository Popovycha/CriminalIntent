package com.popovycha.criminalIntent

import android.icu.text.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.popovycha.criminalIntent.databinding.ListItemCrimeBinding
import com.popovycha.criminalIntent.databinding.ListItemCrimePoliceBinding
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

class CrimeListAdapter {
    class CrimeHolder (
        private val binding: ListItemCrimeBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(crime: Crime, onCrimeClicked: (crimeId: UUID) -> Unit) {
            binding.crimeTitle.text = crime.title
            //Challenge: Formatting the Date
            binding.crimeDate.text = DateFormat.getPatternInstance(DateFormat.YEAR_ABBR_MONTH_DAY).format(crime.date)
            binding.root.setOnClickListener {
                onCrimeClicked(crime.id)
            }
            binding.crimeSolved.visibility = if (crime.isSolved) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }
    }
    //Challenge: RecyclerView View Types
    class CrimePoliceHolder(
        private val binding: ListItemCrimePoliceBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(crime: Crime) {
            binding.crimeTitle.text = crime.title
            binding.crimeDate.text = crime.date.toString()
            binding.root.setOnClickListener {
                Toast.makeText(
                    binding.root.context,
                    "${crime.title} clicked!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    class CrimeListAdapter(private val crimes: List<Crime>, private val onCrimeClicked: (crimeId: UUID) -> Unit) : RecyclerView.Adapter<CrimeHolder>() {

        //onCreateViewHolder is responsible for creating a
        //binding to display, wrapping the view in a view holder, and returning the result.
        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ) : CrimeHolder {
            val inflater = LayoutInflater.from(parent.context)
                val binding = ListItemCrimeBinding.inflate(inflater, parent, false)
                return CrimeHolder(binding)
        }
        //onBindViewHolder responsible for populating a given holder with the crime from a given position
        override fun onBindViewHolder(holder: CrimeHolder, position: Int) {
            val crime = crimes[position]
            holder.bind(crime, onCrimeClicked)
        }
        //how many items are in the data set backing it
        override fun getItemCount() = crimes.size

    }
}