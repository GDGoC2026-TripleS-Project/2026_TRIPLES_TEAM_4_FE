package com.project.unimate.ui.profile

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.project.unimate.databinding.ItemSchoolBinding

data class UniversityItem(
    val id: Long,
    val name: String
)

class SchoolAdapter(private val onClick: (UniversityItem) -> Unit) :
    RecyclerView.Adapter<SchoolAdapter.ViewHolder>() {

    private var schools = listOf<UniversityItem>()

    fun submitList(newList: List<UniversityItem>) {
        schools = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSchoolBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = schools[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = schools.size

    inner class ViewHolder(private val binding: ItemSchoolBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: UniversityItem) {
            binding.tvSchoolName.text = item.name
            binding.root.setOnClickListener { onClick(item) }
        }
    }
}
