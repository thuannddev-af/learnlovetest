package com.example.afica.lovetest.component.dialog

import com.example.afica.lovetest.utils.Constant

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.NumberPicker
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.DialogFragment
import com.example.afica.lovetest.R
import com.example.afica.lovetest.databinding.DialogDatePickerBinding
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

class DatePickerDialog : DialogFragment() {
    private var binding: DialogDatePickerBinding? = null
    private var selectedDate = 0L
    private var visibleMonth = 0L
    private var maxDate = 0L
    private val minDate: Long get() = Calendar.getInstance().apply {
        clear()
        set(1900, Calendar.JANUARY, 1)
    }.timeInMillis

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        maxDate = dayStart(requireArguments().getLong(MAX_DATE))
        selectedDate = dayStart(savedInstanceState?.getLong(Constant.SELECTED_DATE)
            ?: requireArguments().getLong(INITIAL_DATE)).coerceIn(minDate, maxDate)
        visibleMonth = savedInstanceState?.getLong(VISIBLE_MONTH) ?: selectedDate
        visibleMonth = calendar(visibleMonth).apply { set(Calendar.DAY_OF_MONTH, 1) }.timeInMillis
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val views = DialogDatePickerBinding.inflate(layoutInflater)
        binding = views
        resources.getStringArray(R.array.date_weekdays).forEachIndexed { index, label ->
            views.weekdayGrid.addView(TextView(requireContext()).apply {
                text = label
                textSize = 12f
                gravity = Gravity.CENTER
                setTextColor(ContextCompat.getColor(context, R.color.splash_notice))
            }, cellLayout(index, 32))
        }
        views.btnPreviousMonth.setOnClickListener { changeMonth(-1) }
        views.btnNextMonth.setOnClickListener { changeMonth(1) }
        views.tvMonth.setOnClickListener { chooseYear() }
        views.tvMonth.contentDescription = getString(R.string.date_choose_year)
        views.btnCancel.setOnClickListener { dismiss() }
        views.btnSave.setOnClickListener {
            parentFragmentManager.setFragmentResult(
                requireArguments().getString(REQUEST_KEY)!!,
                Bundle().apply { putLong(Constant.SELECTED_DATE, selectedDate) }
            )
            dismiss()
        }
        renderMonth()
        return AlertDialog.Builder(requireContext()).setView(views.root).create()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            val width = minOf(resources.displayMetrics.widthPixels * 0.90f, dp(360).toFloat()).roundToInt()
            setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    private fun changeMonth(offset: Int) {
        visibleMonth = calendar(visibleMonth).apply { add(Calendar.MONTH, offset) }.timeInMillis
        renderMonth()
    }

    private fun renderMonth() {
        val views = binding ?: return
        val month = calendar(visibleMonth)
        views.tvMonth.text = SimpleDateFormat("MMMM - yyyy", Locale.ENGLISH).format(month.time)
        val maximum = calendar(maxDate).apply { set(Calendar.DAY_OF_MONTH, 1) }.timeInMillis
        views.btnPreviousMonth.isEnabled = visibleMonth > minDate
        views.btnPreviousMonth.alpha = if (views.btnPreviousMonth.isEnabled) 1f else 0.3f
        views.btnNextMonth.isEnabled = visibleMonth < maximum
        views.btnNextMonth.alpha = if (views.btnNextMonth.isEnabled) 1f else 0.3f
        views.calendarGrid.removeAllViews()
        val offset = month.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY
        val cursor = calendar(visibleMonth).apply { add(Calendar.DAY_OF_MONTH, -offset) }
        repeat(42) { index ->
            val date = cursor.timeInMillis
            val inMonth = cursor.get(Calendar.MONTH) == month.get(Calendar.MONTH)
            val enabled = inMonth && date in minDate..maxDate
            val chosen = date == selectedDate && inMonth
            val cell = TextView(requireContext()).apply {
                text = cursor.get(Calendar.DAY_OF_MONTH).toString()
                textSize = 14f
                gravity = Gravity.CENTER
                typeface = ResourcesCompat.getFont(context, R.font.plusjakartasans_regular)
                isSelected = chosen
                isEnabled = enabled
                contentDescription = DateFormat.getDateInstance(DateFormat.FULL).format(date)
                setTextColor(if (chosen) Color.WHITE else if (enabled)
                    ContextCompat.getColor(context, R.color.splash_title) else Color.LTGRAY)
                if (chosen) setBackgroundResource(R.drawable.background_profile_save)
                if (enabled) {
                    isFocusable = true
                    setOnClickListener {
                        selectedDate = date
                        renderMonth()
                    }
                }
            }
            views.calendarGrid.addView(cell, cellLayout(index, 40).apply { setMargins(dp(2), dp(2), dp(2), dp(2)) })
            cursor.add(Calendar.DAY_OF_MONTH, 1)
        }
    }

    private fun chooseYear() {
        val month = calendar(visibleMonth)
        val max = calendar(maxDate)
        val picker = NumberPicker(requireContext()).apply {
            minValue = 1900
            maxValue = max.get(Calendar.YEAR)
            value = month.get(Calendar.YEAR)
            wrapSelectorWheel = false
        }
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.date_choose_year)
            .setView(picker)
            .setNegativeButton(R.string.date_cancel, null)
            .setPositiveButton(R.string.profile_save) { _, _ ->
                month.set(Calendar.YEAR, picker.value)
                if (month.timeInMillis > maxDate) month.set(Calendar.MONTH, max.get(Calendar.MONTH))
                visibleMonth = month.timeInMillis
                renderMonth()
            }.show()
    }

    private fun cellLayout(index: Int, heightDp: Int) = GridLayout.LayoutParams().apply {
        width = 0
        height = dp(heightDp)
        rowSpec = GridLayout.spec(index / 7)
        columnSpec = GridLayout.spec(index % 7, 1f)
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).roundToInt()

    private fun calendar(date: Long) = Calendar.getInstance().apply { timeInMillis = date }

    private fun dayStart(date: Long) = calendar(date).apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putLong(Constant.SELECTED_DATE, selectedDate)
        outState.putLong(VISIBLE_MONTH, visibleMonth)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    companion object {
        private const val INITIAL_DATE = "initial_date"
        private const val MAX_DATE = "max_date"
        private const val VISIBLE_MONTH = "visible_month"
        private const val REQUEST_KEY = "request_key"

        fun newInstance(initialDate: Long, requestKey: String, maxDate: Long = System.currentTimeMillis()) =
            DatePickerDialog().apply {
                arguments = Bundle().apply {
                    putLong(INITIAL_DATE, initialDate)
                    putLong(MAX_DATE, maxDate)
                    putString(REQUEST_KEY, requestKey)
                }
            }
    }
}
