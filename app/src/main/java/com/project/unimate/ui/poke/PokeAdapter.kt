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
import androidx.recyclerview.widget.RecyclerView
import com.project.unimate.R

class PokeAdapter(
    private var items: List<PokeData>,
    private val onSelectionChanged: () -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val TYPE_HEADER = 0
        const val TYPE_MEMBER = 1
    }

    fun submitList(newItems: List<PokeData>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is PokeData.Header -> TYPE_HEADER
            is PokeData.Member -> TYPE_MEMBER
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> HeaderViewHolder(inflater.inflate(R.layout.item_poke_header, parent, false))
            else -> MemberViewHolder(inflater.inflate(R.layout.item_poke_member, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is PokeData.Header -> (holder as HeaderViewHolder).bind(item)
            is PokeData.Member -> (holder as MemberViewHolder).bind(item)
        }
    }

    override fun getItemCount(): Int = items.size

    // dp를 픽셀로 변환하는 헬퍼 함수
    private fun dpToPx(context: Context, dp: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            context.resources.displayMetrics
        ).toInt()
    }

    // ===================================================================================
    // [1] 헤더 뷰홀더
    // ===================================================================================
    inner class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTeamName: TextView = itemView.findViewById(R.id.tvTeamName)
        private val viewBar: View = itemView.findViewById(R.id.viewBar)
        private val tvSelectAll: TextView = itemView.findViewById(R.id.tvSelectAll)
        private val btnSelectAllArea: View = itemView.findViewById(R.id.btnSelectAllArea)

        fun bind(header: PokeData.Header) {
            tvTeamName.text = header.title

            // 왼쪽 컬러 바: 왼쪽 위만 둥글게 (Top-Left 20f)
            val drawable = GradientDrawable()
            try {
                drawable.setColor(Color.parseColor(header.teamColor))
            } catch (e: Exception) {
                drawable.setColor(Color.parseColor("#FF4081"))
            }
            drawable.cornerRadii = floatArrayOf(20f, 20f, 0f, 0f, 0f, 0f, 0f, 0f)
            viewBar.background = drawable

            // 전체 선택 텍스트 상태
            updateSelectAllText(header.isAllSelected)

            // 전체 선택 클릭 로직
            btnSelectAllArea.setOnClickListener {
                header.isAllSelected = !header.isAllSelected
                updateSelectAllText(header.isAllSelected) // 즉시 텍스트 변경

                // 리스트 돌면서 멤버들 상태 변경
                items.forEach { item ->
                    if (item is PokeData.Member && item.teamName == header.title) {
                        item.isSelected = header.isAllSelected
                    }
                }
                notifyDataSetChanged()
                onSelectionChanged()
            }
        }

        private fun updateSelectAllText(isSelected: Boolean) {
            if (isSelected) {
                tvSelectAll.text = "선택해제"
                tvSelectAll.setTextColor(Color.BLACK)
            } else {
                tvSelectAll.text = "전체선택"
                tvSelectAll.setTextColor(Color.parseColor("#888888"))
            }
        }
    }

    // ===================================================================================
    // [2] 멤버 뷰홀더 (수정됨: 간격 추가 및 하단 둥글게)
    // ===================================================================================
    inner class MemberViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvUserName: TextView = itemView.findViewById(R.id.tvUserName)
        private val viewBar: View = itemView.findViewById(R.id.viewBar)
        private val ivCheck: ImageView = itemView.findViewById(R.id.ivCheck)

        fun bind(member: PokeData.Member) {
            tvUserName.text = member.name

            // 1. 마지막 멤버인지 확인 로직
            // 현재 아이템이 리스트의 마지막이거나, 바로 다음 아이템이 Header라면 -> 이 팀의 마지막 멤버임
            val currentPos = adapterPosition
            val isLastItemTotal = currentPos == items.lastIndex
            val nextItemIsHeader = if (!isLastItemTotal) items[currentPos + 1] is PokeData.Header else false
            val isLastMemberOfTeam = isLastItemTotal || nextItemIsHeader

            // 2. 배경 및 모서리 설정
            val drawable = GradientDrawable()
            try {
                drawable.setColor(Color.parseColor(member.teamColor))
            } catch (e: Exception) {
                drawable.setColor(Color.parseColor("#FF4081"))
            }

            // 레이아웃 파라미터 가져오기 (마진 조절용)
            val params = itemView.layoutParams as RecyclerView.LayoutParams

            if (isLastMemberOfTeam) {
                // ★ 마지막 멤버: 왼쪽 아래 둥글게 (Bottom-Left 20f)
                // 순서: TL, TR, BR, BL
                drawable.cornerRadii = floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 20f, 20f)

                // ★ 간격 띄우기: 아래쪽 마진 20dp 추가
                params.bottomMargin = dpToPx(itemView.context, 20f)
            } else {
                // 중간 멤버: 직각
                drawable.cornerRadius = 0f

                // 마진 초기화 (재활용 뷰 문제 방지)
                params.bottomMargin = 0
            }

            viewBar.background = drawable
            itemView.layoutParams = params // 변경된 마진 적용

            // 3. 체크박스 UI
            if (member.isSelected) {
                ivCheck.setImageResource(R.drawable.ic_check_on)
            } else {
                ivCheck.setImageResource(R.drawable.ic_check_off)
            }

            // 4. 클릭 리스너 (자동 전체선택 감지 로직 추가)
            itemView.setOnClickListener {
                member.isSelected = !member.isSelected
                notifyItemChanged(adapterPosition) // 체크박스 애니메이션

                // ★ 역방향 로직: 멤버 상태가 변했을 때 헤더 상태 업데이트
                checkAndUpdateHeaderState(member.teamName)

                onSelectionChanged()
            }
        }

        // 해당 팀의 멤버가 모두 선택되었는지 확인하고 헤더 업데이트
        private fun checkAndUpdateHeaderState(teamName: String) {
            // 1. 현재 팀의 모든 멤버 찾기
            val teamMembers = items.filter { it is PokeData.Member && it.teamName == teamName }

            // 2. 모두 선택되었는지 확인
            val isAllSelected = teamMembers.all { (it as PokeData.Member).isSelected }

            // 3. 해당 팀의 헤더 찾기
            val headerIndex = items.indexOfFirst { it is PokeData.Header && it.title == teamName }

            if (headerIndex != -1) {
                val header = items[headerIndex] as PokeData.Header

                // 4. 상태가 다르면 헤더 업데이트
                if (header.isAllSelected != isAllSelected) {
                    header.isAllSelected = isAllSelected
                    // 헤더 뷰만 새로고침 (깜빡임 없이 텍스트만 바뀜)
                    notifyItemChanged(headerIndex)
                }
            }
        }
    }
}