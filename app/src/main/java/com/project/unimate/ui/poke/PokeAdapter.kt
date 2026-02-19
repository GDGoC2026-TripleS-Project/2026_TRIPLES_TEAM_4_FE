package com.project.unimate.ui.poke

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.project.unimate.R

class PokeAdapter(
    private val onSelectionChanged: (selectedCount: Int) -> Unit
) : ListAdapter<PokeData, RecyclerView.ViewHolder>(DiffCallback) {

    companion object {
        const val TYPE_HEADER = 0
        const val TYPE_MEMBER = 1
        const val TYPE_NO_MEMBERS = 2
        val DiffCallback = object : DiffUtil.ItemCallback<PokeData>() {
            override fun areItemsTheSame(oldItem: PokeData, newItem: PokeData): Boolean = when {
                oldItem is PokeData.Header && newItem is PokeData.Header ->
                    oldItem.teamId == newItem.teamId
                oldItem is PokeData.Member && newItem is PokeData.Member ->
                    oldItem.userId == newItem.userId && oldItem.teamId == newItem.teamId
                oldItem is PokeData.NoMembersMessage && newItem is PokeData.NoMembersMessage ->
                    oldItem.teamName == newItem.teamName
                else -> false
            }
            override fun areContentsTheSame(oldItem: PokeData, newItem: PokeData): Boolean =
                oldItem == newItem
        }
    }

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is PokeData.Header -> TYPE_HEADER
        is PokeData.Member -> TYPE_MEMBER
        is PokeData.NoMembersMessage -> TYPE_NO_MEMBERS
        else -> TYPE_MEMBER
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> HeaderViewHolder(inflater.inflate(R.layout.item_poke_header, parent, false))
            TYPE_MEMBER -> MemberViewHolder(inflater.inflate(R.layout.item_poke_member, parent, false))
            else -> NoMembersMessageViewHolder(inflater.inflate(R.layout.item_poke_no_members, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is PokeData.Header -> (holder as HeaderViewHolder).bind(item)
            is PokeData.Member -> (holder as MemberViewHolder).bind(item)
            is PokeData.NoMembersMessage -> (holder as NoMembersMessageViewHolder).bind(item)
        }
    }

    /** 선택된 Member 목록 반환 (다음 화면 전달 및 버튼 카운트용) */
    fun getSelectedMembers(): List<PokeData.Member> =
        currentList.filterIsInstance<PokeData.Member>().filter { it.isSelected }

    private fun dpToPx(context: Context, dp: Float): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.resources.displayMetrics).toInt()

    // ─── Header ViewHolder ──────────────────────────────────────────────────

    inner class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTeamName: TextView = itemView.findViewById(R.id.tvTeamName)
        private val viewBar: View = itemView.findViewById(R.id.viewBar)
        private val tvSelectAll: TextView = itemView.findViewById(R.id.tvSelectAll)
        private val btnSelectAllArea: View = itemView.findViewById(R.id.btnSelectAllArea)

        fun bind(header: PokeData.Header) {
            tvTeamName.text = header.title

            val drawable = GradientDrawable()
            try { drawable.setColor(Color.parseColor(header.teamColor)) }
            catch (_: Exception) { drawable.setColor(Color.parseColor("#FF4081")) }
            drawable.cornerRadii = floatArrayOf(20f, 20f, 0f, 0f, 0f, 0f, 0f, 0f)
            viewBar.background = drawable

            updateSelectAllText(header.isAllSelected)

            btnSelectAllArea.setOnClickListener {
                if (bindingAdapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener
                val teamMembers = currentList.filter { it is PokeData.Member && it.teamId == header.teamId }
                if (teamMembers.isEmpty()) return@setOnClickListener
                val newAllSelected = !header.isAllSelected
                val newList = currentList.map { item ->
                    when {
                        item is PokeData.Header && item.teamId == header.teamId ->
                            item.copy(isAllSelected = newAllSelected)
                        item is PokeData.Member && item.teamId == header.teamId ->
                            item.copy(isSelected = newAllSelected)
                        else -> item
                    }
                }
                val newCount = newList.filterIsInstance<PokeData.Member>().count { it.isSelected }
                submitList(newList)
                onSelectionChanged(newCount)
            }
        }

        private fun updateSelectAllText(isSelected: Boolean) {
            tvSelectAll.text = if (isSelected) "선택해제" else "전체선택"
            tvSelectAll.setTextColor(if (isSelected) Color.BLACK else Color.parseColor("#888888"))
        }
    }

    // ─── Member ViewHolder ──────────────────────────────────────────────────

    inner class MemberViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvUserName: TextView = itemView.findViewById(R.id.tvUserName)
        private val viewBar: View = itemView.findViewById(R.id.viewBar)
        private val ivCheck: ImageView = itemView.findViewById(R.id.ivCheck)

        fun bind(member: PokeData.Member) {
            tvUserName.text = member.name

            val pos = bindingAdapterPosition
            val isLastTotal = pos == itemCount - 1
            val isLastOfTeam = isLastTotal || (pos < itemCount - 1 && getItem(pos + 1) is PokeData.Header)

            val drawable = GradientDrawable()
            try { drawable.setColor(Color.parseColor(member.teamColor)) }
            catch (_: Exception) { drawable.setColor(Color.parseColor("#FF4081")) }

            val params = itemView.layoutParams as RecyclerView.LayoutParams
            if (isLastOfTeam) {
                drawable.cornerRadii = floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 20f, 20f)
                params.bottomMargin = dpToPx(itemView.context, 20f)
            } else {
                drawable.cornerRadius = 0f
                params.bottomMargin = 0
            }
            viewBar.background = drawable
            itemView.layoutParams = params

            ivCheck.setImageResource(if (member.isSelected) R.drawable.ic_check_on else R.drawable.ic_check_off)

            itemView.setOnClickListener {
                if (bindingAdapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener
                val newSelected = !member.isSelected
                val newList = currentList.map { item ->
                    when {
                        item is PokeData.Member && item.userId == member.userId && item.teamId == member.teamId ->
                            item.copy(isSelected = newSelected)
                        item is PokeData.Header && item.teamId == member.teamId -> {
                            val allSelected = currentList
                                .filterIsInstance<PokeData.Member>()
                                .filter { it.teamId == member.teamId }
                                .all { if (it.userId == member.userId) newSelected else it.isSelected }
                            item.copy(isAllSelected = allSelected)
                        }
                        else -> item
                    }
                }
                val newCount = newList.filterIsInstance<PokeData.Member>().count { it.isSelected }
                submitList(newList)
                onSelectionChanged(newCount)
            }
        }
    }

    inner class NoMembersMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvNoMembers: TextView = itemView.findViewById(R.id.tvNoMembers)

        fun bind(item: PokeData.NoMembersMessage) {
            tvNoMembers.text = "찌르기 가능한 팀원이 없습니다."
        }
    }
}
