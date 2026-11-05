package com.example.afica.lovetest.component.main.homefragment

import com.example.afica.lovetest.utils.Constant

import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.afica.lovetest.R
import com.example.afica.lovetest.base.fragment.BaseFragment
import com.example.afica.lovetest.component.main.background.BackgroundSheet
import com.example.afica.lovetest.component.main.background.BackgroundStore
import com.example.afica.lovetest.component.main.profile.ProfileInformationSheet
import com.example.afica.lovetest.component.main.profile.ProfileStore
import com.example.afica.lovetest.component.startdate.StartDateActivity
import com.example.afica.lovetest.component.setting.SettingActivity
import com.example.afica.lovetest.databinding.FragmentHomeBinding
import com.example.afica.lovetest.utils.SharedPreferenceUtils
import com.example.afica.lovetest.utils.setOnClickWithPressEffect
import java.io.File
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class HomeFragment : BaseFragment<FragmentHomeBinding>() {
    private val preferences by lazy { SharedPreferenceUtils(requireContext(), "home") }
    private val views get() = viewBinding

    override fun provideViewBinding(container: ViewGroup?): FragmentHomeBinding =
        FragmentHomeBinding.inflate(layoutInflater, container, false)

    override fun initViews() {
        ViewCompat.setOnApplyWindowInsetsListener(views.root) { _, insets ->
            val actions = views.headerActions
            actions.layoutParams = (actions.layoutParams as ViewGroup.MarginLayoutParams).apply {
                topMargin = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top
            }
            insets
        }
        views.imgBoy.setOnClickListener { showProfile(Constant.PROFILE_BOY) }
        views.btnSettings.setOnClickWithPressEffect {
            startActivity(Intent(requireContext(), SettingActivity::class.java))
        }
        views.imgGirl.setOnClickListener { showProfile(Constant.PROFILE_GIRL) }
        views.btnChangeBackground.setOnClickWithPressEffect {
            if (parentFragmentManager.findFragmentByTag("background_sheet") == null) {
                BackgroundSheet().show(parentFragmentManager, "background_sheet")
            }
        }
        views.btnStartDate.setOnClickListener {
            startActivity(Intent(requireContext(), StartDateActivity::class.java))
        }
    }

    override fun initData() {
        BackgroundStore(requireContext()).selected()?.let { showBackground(it) }
        renderProfiles()
        updateDayCount()
    }

    override fun initObserver() {
        parentFragmentManager.setFragmentResultListener(Constant.RESULT_PROFILE_UPDATED, viewLifecycleOwner) { _, _ ->
            renderProfiles()
        }
        parentFragmentManager.setFragmentResultListener(Constant.RESULT_BACKGROUND_UPDATED, viewLifecycleOwner) { _, result ->
            result.getString(Constant.BACKGROUND_URI)?.let { showBackground(Uri.parse(it)) }
        }
    }

    override fun onResume() {
        super.onResume()
        if (view != null) {
            renderProfiles()
            updateDayCount()
        }
    }

    private fun showProfile(id: String) {
        if (parentFragmentManager.findFragmentByTag("profile_information") == null) {
            ProfileInformationSheet.newInstance(id).show(parentFragmentManager, "profile_information")
        }
    }

    private fun renderProfiles() {
        val store = ProfileStore(requireContext())
        val boy = store.load(Constant.PROFILE_BOY)
        val girl = store.load(Constant.PROFILE_GIRL)
        views.tvBoyName.text = boy.name
        views.tvGirlName.text = girl.name
        views.tvBoyAge.text = boy.age().toString()
        views.tvGirlAge.text = girl.age().toString()
        views.imgBoyGender.setImageResource(if (boy.isMale) R.drawable.boy else R.drawable.girl)
        views.imgGirlGender.setImageResource(if (girl.isMale) R.drawable.boy else R.drawable.girl)
        views.imgBoyGender.contentDescription = getString(if (boy.isMale) R.string.home_male else R.string.home_female)
        views.imgGirlGender.contentDescription = getString(if (girl.isMale) R.string.home_male else R.string.home_female)
        showAvatar(views.imgBoy, boy.photoPath, R.drawable.boy1)
        showAvatar(views.imgGirl, girl.photoPath, R.drawable.girl1)
    }

    private fun showAvatar(image: ImageView, path: String?, defaultImage: Int) {
        image.setImageResource(defaultImage)
        image.scaleType = if (path == null) ImageView.ScaleType.FIT_CENTER else ImageView.ScaleType.CENTER_CROP
        path?.let { if (File(it).isFile) image.setImageURI(Uri.fromFile(File(it))) }
    }

    private fun showBackground(uri: Uri) {
        try {
            views.imgBackground.setImageURI(uri)
            if (views.imgBackground.drawable == null) views.imgBackground.setImageResource(R.drawable.default1)
        } catch (_: SecurityException) {
            views.imgBackground.setImageResource(R.drawable.default1)
        }
    }

    private fun updateDayCount() {
        val days = if (preferences.contains(Constant.KEY_SP_START_DATE)) {
            val start = Calendar.getInstance().apply { timeInMillis = preferences.getLong(Constant.KEY_SP_START_DATE, 0) }
            fun dateInUtc(calendar: Calendar): Long =
                Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                    clear()
                    set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
                }.timeInMillis
            TimeUnit.MILLISECONDS.toDays(dateInUtc(Calendar.getInstance()) - dateInUtc(start)).coerceAtLeast(0)
        } else 0L
        views.tvDayCount.text = days.toString()
    }

    
}
