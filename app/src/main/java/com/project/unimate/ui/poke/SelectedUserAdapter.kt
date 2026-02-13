package com.project.unimate.ui.poke

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.project.unimate.R

class SelectedUserAdapter(
    private val members: List<PokeData.Member>
) : RecyclerView.Adapter<SelectedUserAdapter.UserViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        // 기존 item_poke_member.xml 재사용 (체크박스만 코드에서 숨김)
        return UserViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_poke_member, parent, false))
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(members[position])
    }

    override fun getItemCount(): Int = members.size

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvUserName: TextView = itemView.findViewById(R.id.tvUserName)
        private val ivCheck: ImageView = itemView.findViewById(R.id.ivCheck)
        private val viewBar: View = itemView.findViewById(R.id.viewBar) // 왼쪽 컬러바

        // 상세 화면에서는 구분선이나 불필요한 요소 숨기기
        private val dividerTop: View? = itemView.findViewById(R.id.dividerTop)
        private val dividerBottom: View? = itemView.findViewById(R.id.dividerBottom)

        fun bind(member: PokeData.Member) {
            tvUserName.text = member.name

            // 상세 화면에서는 체크박스 숨김
            ivCheck.visibility = View.GONE

            // 왼쪽 컬러바도 숨길지, 보여줄지 선택 (디자인 시안엔 아바타만 있어서 숨기는 게 맞아 보임)
            viewBar.visibility = View.GONE
            dividerTop?.visibility = View.GONE
            dividerBottom?.visibility = View.GONE
        }
    }
}