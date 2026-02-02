package com.project.unimate.ui.profile

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.project.unimate.databinding.ItemSchoolBinding

class SchoolAdapter(private val onClick: (String) -> Unit) :
    RecyclerView.Adapter<SchoolAdapter.ViewHolder>() {

    private var schools = listOf<String>()

    fun submitList(newList: List<String>) {
        schools = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSchoolBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val schoolName = schools[position]
        holder.bind(schoolName)
    }

    override fun getItemCount(): Int = schools.size

    inner class ViewHolder(private val binding: ItemSchoolBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(name: String) {
            binding.tvSchoolName.text = name
            binding.root.setOnClickListener { onClick(name) }
        }
    }
}