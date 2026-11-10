package com.example.afica.lovetest.component.main.memory

import com.example.afica.lovetest.utils.setOnClickWithPressEffect

import com.example.afica.lovetest.utils.Constant

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast

import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.example.afica.lovetest.R
import com.example.afica.lovetest.ads.AdsConfig
import com.example.afica.lovetest.ads.AppOpenAdsUtils
import com.example.afica.lovetest.ads.BannerAdsUtils
import com.example.afica.lovetest.ads.RemoteConfigManager
import com.example.afica.lovetest.ads.RewardAdsUtils
import com.example.afica.lovetest.base.activity.BaseActivity
import com.example.afica.lovetest.component.dialog.DatePickerDialog
import com.example.afica.lovetest.component.dialog.WatchAndSaveDialog
import com.example.afica.lovetest.data.model.Memory
import com.example.afica.lovetest.data.repositores.LocalRespository
import com.example.afica.lovetest.databinding.ActivityMemoryBinding
import com.example.afica.lovetest.utils.ProfilePhotos
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class MemoryActivity : BaseActivity<ActivityMemoryBinding>() {
    @Inject lateinit var repository: LocalRespository
    private var memoryId = ""
    private var photoPath: String? = null
    private var selectedDate: Long? = null
    private var busy = false
    private val rewardDialogLazy = lazy { WatchAndSaveDialog(this) }
    private val rewardDialog by rewardDialogLazy
    private val remoteConfig by lazy { RemoteConfigManager() }

    private val pickPhoto = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) importPhoto(uri)
    }

    override fun provideViewBinding(): ActivityMemoryBinding = ActivityMemoryBinding.inflate(layoutInflater)

    override fun initViews() {

        WindowInsetsControllerCompat(window, viewBaseActivity.root).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
//        ViewCompat.setOnApplyWindowInsetsListener(viewBaseActivity.root) { view, insets ->
//            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
//            val keyboard = insets.getInsets(WindowInsetsCompat.Type.ime())
//            view.setPadding(bars.left, bars.top, bars.right, maxOf(bars.bottom, keyboard.bottom))
//            insets
//        }

        viewBaseActivity.btnBack.setOnClickWithPressEffect { finish() }
        viewBaseActivity.btnAddPhoto.setOnClickWithPressEffect {
            if (!busy) {
                AppOpenAdsUtils.skipNextResume()
                pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
        }
        viewBaseActivity.btnDate.setOnClickWithPressEffect {
            if (supportFragmentManager.findFragmentByTag(DATE_RESULT) == null) {
                DatePickerDialog.newInstance(selectedDate ?: System.currentTimeMillis(), DATE_RESULT)
                    .show(supportFragmentManager, DATE_RESULT)
            }
        }
        viewBaseActivity.btnSave.setOnClickWithPressEffect { saveMemory() }
        showBanner()
    }

    private fun importPhoto(uri: Uri) {
        setBusy(true)
        lifecycleScope.launch {
            try {
                photoPath = withContext(Dispatchers.IO) { ProfilePhotos.importPhoto(applicationContext, uri) }
                renderPhoto()
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                Toast.makeText(this@MemoryActivity, R.string.profile_photo_error, Toast.LENGTH_SHORT).show()
            } finally {
                setBusy(false)
            }
        }
    }

    private fun renderPhoto() {
        val file = photoPath?.let(::File)?.takeIf { it.isFile }
        viewBaseActivity.tvPlus.visibility = if (file == null) View.VISIBLE else View.GONE
        if (file != null) viewBaseActivity.imgPhoto.setImageURI(Uri.fromFile(file))
    }

    private fun renderDate() {
        viewBaseActivity.btnDate.text = selectedDate?.let {
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it)
        }.orEmpty()
    }

    private fun setBusy(value: Boolean) {
        busy = value
        viewBaseActivity.btnSave.isEnabled = !value
        viewBaseActivity.btnSave.alpha = if (value) 0.5f else 1f
    }

    private fun saveMemory() {
        if (busy || (rewardDialogLazy.isInitialized() && rewardDialog.isShowing)) return
        val title = viewBaseActivity.edtTitle.text?.toString()?.trim().orEmpty()
        if (title.isEmpty()) {
            viewBaseActivity.edtTitle.error = getString(R.string.memory_title_required)
            viewBaseActivity.edtTitle.requestFocus()
            return
        }
        val photo = photoPath?.takeIf { File(it).isFile }
        if (photo == null) {
            Toast.makeText(this, R.string.memory_photo_required, Toast.LENGTH_SHORT).show()
            return
        }
        val date = selectedDate
        if (date == null) {
            Toast.makeText(this, R.string.memory_date_required, Toast.LENGTH_SHORT).show()
            return
        }
        val memory = Memory(memoryId, title, date, photo)
        if (!remoteConfig.canShowAd(RemoteConfigManager.REWARD_ADS)) {
            persistMemory(memory)
            return
        }
        rewardDialog.show { showSaveReward(memory) }
    }

    private fun showSaveReward(memory: Memory) {
        if (busy || isFinishing || isDestroyed) return
        setBusy(true)
        var earned = false
        Toast.makeText(this, R.string.memory_reward_loading, Toast.LENGTH_SHORT).show()
        RewardAdsUtils.loadAndShowRewardAds(
            activity = this,
            lifecycleOwner = this,
            idAds = AdsConfig.REWARD_ID,
            adPlacement = REWARD_SAVE_MEMORY,
            isEnable = remoteConfig.canShowAd(RemoteConfigManager.REWARD_ADS),
            onRewardEarned = { earned = true }
        ) {
            if (!isFinishing && !isDestroyed) {
                setBusy(false)
                if (earned) persistMemory(memory)
                else Toast.makeText(this, R.string.memory_reward_incomplete, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun persistMemory(memory: Memory) {
        if (busy) return
        setBusy(true)
        lifecycleScope.launch {
            try {
                repository.saveMemory(memory)
                finish()
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                Toast.makeText(this@MemoryActivity, R.string.memory_save_error, Toast.LENGTH_SHORT).show()
            } finally {
                setBusy(false)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("memory_id", memoryId)
        outState.putString("photo_path", photoPath)
        selectedDate?.let { outState.putLong("memory_date", it) }
        super.onSaveInstanceState(outState)
    }

    companion object {
        private const val DATE_RESULT = "memory_date_result"
        private const val REWARD_SAVE_MEMORY = "reward_save_memory"
    }

    override fun initData() {
        memoryId = restoredState?.getString("memory_id") ?: UUID.randomUUID().toString()
        photoPath = restoredState?.getString("photo_path")
        selectedDate = restoredState?.takeIf { it.containsKey("memory_date") }?.getLong("memory_date")
        renderPhoto()
        renderDate()
        RewardAdsUtils.loadRewardAds(
            activity = this,
            idAds = AdsConfig.REWARD_ID,
            adPlacement = REWARD_SAVE_MEMORY,
            isEnable = remoteConfig.canShowAd(RemoteConfigManager.REWARD_ADS)
        )
    }

    override fun onDestroy() {
        if (rewardDialogLazy.isInitialized()) rewardDialog.dismiss()
        super.onDestroy()
    }

    override fun initObserver() {
        supportFragmentManager.setFragmentResultListener(DATE_RESULT, this) { _, result ->
            selectedDate = result.getLong(Constant.SELECTED_DATE)
            renderDate()
        }
    }

    private fun showBanner() {
        BannerAdsUtils.showBanner(this, viewBaseActivity.frAdsBanner, BannerAdsUtils.Placement.BANNER_MEMORY)
    }
}

