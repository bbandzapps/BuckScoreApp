package com.example.buck_score

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

class ScorecardAdapter(
    private var items: List<ScorecardEntity>,
    private val onClick: (ScorecardEntity) -> Unit
) : RecyclerView.Adapter<ScorecardAdapter.ScorecardViewHolder>() {

    private var selectedPosition = -1

    inner class ScorecardViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.cardName)
        val species: TextView = view.findViewById(R.id.cardSpecies)
        //val type: TextView = view.findViewById(R.id.cardType)
        val score: TextView = view.findViewById(R.id.cardScore)
        val date: TextView = view.findViewById(R.id.cardDate)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScorecardViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_scorecard, parent, false)
        return ScorecardViewHolder(view)
    }

    override fun onBindViewHolder(holder: ScorecardViewHolder, position: Int) {
        val item = items[position]

        holder.name.text = item.name
        holder.species.text = item.species
        //holder.type.text = item.buckType ?: "-"
        holder.species.text = buildString {
            append(item.species)
            item.buckType?.let { append(" | $it") }
        }
        holder.score.text = "Score: ${doubleToBC(item.netScore)}"

        holder.date.text = formatDate(item.dateSaved)

        holder.itemView.setOnClickListener {
            val pos = holder.adapterPosition
            if (pos == RecyclerView.NO_POSITION) return@setOnClickListener

            val previous = selectedPosition
            selectedPosition = pos

            notifyItemChanged(previous)
            notifyItemChanged(selectedPosition)

            onClick(items[pos])
        }

        holder.itemView.setBackgroundResource(
            if (position == selectedPosition)
                R.drawable.selected_border
            else
                R.drawable.unselected_scorecard
        )

        // Text color
        val textColor = if (position == selectedPosition) {
            ContextCompat.getColor(holder.itemView.context, R.color.white)
        } else {
            ContextCompat.getColor(holder.itemView.context, R.color.dark_text_soft)
        }

        holder.name.setTextColor(textColor)
        holder.species.setTextColor(textColor)
        holder.score.setTextColor(textColor)
        holder.date.setTextColor(textColor)
    }

    override fun getItemCount(): Int = items.size

    fun updateList(newItems: List<ScorecardEntity>) {
        items = newItems
        notifyDataSetChanged()
    }

    fun formatDate(timestamp: Long): String {
        val formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy")

        return Instant.ofEpochMilli(timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .format(formatter)
    }

    fun getSelectedItem(): ScorecardEntity? {
        return if (selectedPosition != RecyclerView.NO_POSITION) {
            items[selectedPosition]
        } else null
    }

    fun clearSelection() {
        selectedPosition = RecyclerView.NO_POSITION

    }

}
