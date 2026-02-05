package com.project.unimate.ui.team

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.project.unimate.R
import com.project.unimate.databinding.FragmentTeamCreateBinding
import java.util.*

class TeamCreateFragment : Fragment() {

    private var _binding: FragmentTeamCreateBinding? = null
    private val binding get() = _binding!!

    private val colorButtons by lazy {
        with(binding) {
            listOf(
                btnColorYellow, btnColorBeige, btnColorPeriwinkle,
                btnColorLavender, btnColorMagenta, btnColorPinkLight,
                btnColorCoral, btnColorCoralLight, btnColorMint, btnColorAqua
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTeamCreateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 초기화: 모든 버튼의 이미지(체크 아이콘)를 제거
        colorButtons.forEach { it.setImageResource(0) }

        setupDateAndTimeListeners()
        setupColorListeners()
        setupCompleteButton()
    }

    private fun setupDateAndTimeListeners() {
        binding.tvStartDate.setOnClickListener { showDatePicker(binding.tvStartDate) }
        binding.tvStartTime.setOnClickListener { showTimePicker(binding.tvStartTime) }
        binding.tvEndDate.setOnClickListener { showDatePicker(binding.tvEndDate) }
        binding.tvEndTime.setOnClickListener { showTimePicker(binding.tvEndTime) }
    }

    private fun showDatePicker(textView: TextView) {
        val cal = Calendar.getInstance()
        val contextWrapper = ContextThemeWrapper(requireContext(), R.style.MyDatePickerDialogTheme)

        val datePickerDialog = DatePickerDialog(
            contextWrapper,
            { _, year, month, day ->
                textView.text = "${year}. ${month + 1}. ${day}"
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )

        datePickerDialog.setButton(DatePickerDialog.BUTTON_POSITIVE, "확인", datePickerDialog)
        datePickerDialog.setButton(DatePickerDialog.BUTTON_NEGATIVE, "취소", datePickerDialog)

        datePickerDialog.setOnShowListener {
            datePickerDialog.getButton(DatePickerDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
            datePickerDialog.getButton(DatePickerDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
        }

        datePickerDialog.show()
    }

    private fun showTimePicker(textView: TextView) {
        val cal = Calendar.getInstance()
        val contextWrapper = ContextThemeWrapper(requireContext(), R.style.MyDatePickerDialogTheme)

        val timePickerDialog = TimePickerDialog(
            contextWrapper,
            { _, h, m ->
                val amPm = if (h < 12) "오전" else "오후"
                val hour = if (h % 12 == 0) 12 else h % 12
                textView.text = "$amPm ${hour}:${String.format("%02d", m)}"
            },
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE),
            false
        )

        timePickerDialog.setButton(TimePickerDialog.BUTTON_POSITIVE, "확인", timePickerDialog)
        timePickerDialog.setButton(TimePickerDialog.BUTTON_NEGATIVE, "취소", timePickerDialog)

        timePickerDialog.setOnShowListener {
            timePickerDialog.getButton(TimePickerDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
            timePickerDialog.getButton(TimePickerDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
        }

        timePickerDialog.show()
    }

    private fun setupColorListeners() {
        colorButtons.forEach { button ->
            button.setOnClickListener {
                onColorSelected(button)
            }
        }
    }

    private fun onColorSelected(selectedButton: ImageButton) {
        // 모든 버튼에서 이미지(체크 아이콘) 제거
        colorButtons.forEach { it.setImageResource(0) }

        // 클릭된 버튼에만 체크 아이콘 설정 (R.drawable.ic_check 확인 필요)
        selectedButton.setImageResource(R.drawable.ic_check_white)
    }

    private fun setupCompleteButton() {
        binding.btnCompleteCreate.setOnClickListener {
            val teamName = binding.etTeamName.text.toString().trim()

            if (teamName.isEmpty()) {
                Toast.makeText(context, "팀 명을 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 날짜 선택 여부 검사 (초기 텍스트와 비교)
            if (binding.tvStartDate.text == "2026. 2. 23") {
                Toast.makeText(context, "작업 시작 날짜를 선택해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 컬러 선택 여부 검사 (이미지 리소스가 설정된 버튼이 하나라도 있는지 확인)
            val isColorSelected = colorButtons.any { it.drawable != null }
            if (!isColorSelected) {
                Toast.makeText(context, "팀 컬러를 선택해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            findNavController().navigate(R.id.action_teamCreate_to_teamComplete)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}