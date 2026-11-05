package com.example.afica.lovetest.component.lovetest

import com.example.afica.lovetest.utils.Constant

import android.content.Intent
import android.os.Bundle
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.example.afica.lovetest.R
import com.example.afica.lovetest.base.activity.BaseActivity
import com.example.afica.lovetest.data.model.ZodiacSign
import com.example.afica.lovetest.databinding.ActivityHoroscopeTestBinding
import com.example.afica.lovetest.domain.usecase.GetZodiacSignsUseCase
import com.example.afica.lovetest.utils.SharedPreferenceUtils
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HoroscopeTestActivity : BaseActivity<ActivityHoroscopeTestBinding>() {
    @Inject lateinit var getSigns: GetZodiacSignsUseCase
    private val preferences by lazy { SharedPreferenceUtils(this, "horoscope_test") }
    private var signs: List<ZodiacSign> = emptyList()
    private var yourCode = "aries"
    private var partnerCode = "aries"

    override fun provideViewBinding(): ActivityHoroscopeTestBinding =
        ActivityHoroscopeTestBinding.inflate(layoutInflater)

    override fun initViews() {
        WindowInsetsControllerCompat(window, viewBaseActivity.root).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
//        ViewCompat.setOnApplyWindowInsetsListener(viewBaseActivity.root) { view, insets ->
//            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
//            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
//            insets
//        }
        viewBaseActivity.btnBack.setOnClickListener { onBack() }
        viewBaseActivity.btnYourSign.setOnClickListener { chooseSign(yourCode, YOUR_SIGN) }
        viewBaseActivity.btnPartnerSign.setOnClickListener { chooseSign(partnerCode, PARTNER_SIGN) }
        viewBaseActivity.btnLoveTest.setOnClickListener {
            val your = signs.firstOrNull { it.code == yourCode }
            val partner = signs.firstOrNull { it.code == partnerCode }
            if (your != null && partner != null) {
                startActivity(Intent(this, NameTestResultActivity::class.java).apply {
                    putExtra(Constant.EXTRA_YOUR_NAME, getString(your.nameRes))
                    putExtra(Constant.EXTRA_PARTNER_NAME, getString(partner.nameRes))
                    putExtra(Constant.EXTRA_IS_HOROSCOPE, true)
                    putExtra(Constant.EXTRA_YOUR_SIGN_CODE, your.code)
                    putExtra(Constant.EXTRA_PARTNER_SIGN_CODE, partner.code)
                })
            }
        }
    }

    override fun initData() {
        yourCode = restoredState?.getString(YOUR_SIGN) ?: preferences.getString(YOUR_SIGN, "aries")!!
        partnerCode = restoredState?.getString(PARTNER_SIGN) ?: preferences.getString(PARTNER_SIGN, "aries")!!
        lifecycleScope.launch {
            signs = getSigns.execute(GetZodiacSignsUseCase.Param())
            if (signs.none { it.code == yourCode }) yourCode = signs.first().code
            if (signs.none { it.code == partnerCode }) partnerCode = signs.first().code
            renderSigns()
        }
    }

    override fun initObserver() {
        supportFragmentManager.setFragmentResultListener(YOUR_SIGN, this) { _, result ->
            yourCode = result.getString(Constant.SIGN_CODE) ?: yourCode
            preferences.putString(YOUR_SIGN, yourCode)
            renderSigns()
        }
        supportFragmentManager.setFragmentResultListener(PARTNER_SIGN, this) { _, result ->
            partnerCode = result.getString(Constant.SIGN_CODE) ?: partnerCode
            preferences.putString(PARTNER_SIGN, partnerCode)
            renderSigns()
        }
    }

    private fun chooseSign(selectedCode: String, requestKey: String) {
        if (signs.isNotEmpty() && supportFragmentManager.findFragmentByTag(SHEET_TAG) == null) {
            HoroscopeSheet.newInstance(selectedCode, requestKey).show(supportFragmentManager, SHEET_TAG)
        }
    }

    private fun renderSigns() {
        val your = signs.firstOrNull { it.code == yourCode } ?: return
        val partner = signs.firstOrNull { it.code == partnerCode } ?: return
        viewBaseActivity.imgYourSign.setImageResource(your.iconRes)
        viewBaseActivity.imgPartnerSign.setImageResource(partner.iconRes)
        viewBaseActivity.tvYourSign.setText(your.nameRes)
        viewBaseActivity.tvPartnerSign.setText(partner.nameRes)
        viewBaseActivity.btnYourSign.contentDescription = getString(R.string.horoscope_your_sign, getString(your.nameRes))
        viewBaseActivity.btnPartnerSign.contentDescription = getString(R.string.horoscope_partner_sign, getString(partner.nameRes))
        viewBaseActivity.btnLoveTest.isEnabled = true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(YOUR_SIGN, yourCode)
        outState.putString(PARTNER_SIGN, partnerCode)
        super.onSaveInstanceState(outState)
    }

    companion object {
        private const val YOUR_SIGN = "your_sign"
        private const val PARTNER_SIGN = "partner_sign"
        private const val SHEET_TAG = "horoscope_sheet"
    }
}
