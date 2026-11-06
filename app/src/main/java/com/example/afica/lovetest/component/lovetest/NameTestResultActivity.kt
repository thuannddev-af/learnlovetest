package com.example.afica.lovetest.component.lovetest

import com.example.afica.lovetest.utils.setOnClickWithPressEffect

import com.example.afica.lovetest.utils.Constant

import android.content.ClipData
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Animatable
import android.net.Uri
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ImageSpan
import android.widget.ImageView
import android.widget.Toast
import android.view.View
import androidx.activity.viewModels
import androidx.core.content.FileProvider
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.drawToBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.afica.lovetest.R
import com.example.afica.lovetest.base.activity.BaseActivity
import com.example.afica.lovetest.component.main.profile.ProfileStore
import com.example.afica.lovetest.data.model.LoveTestResult
import com.example.afica.lovetest.databinding.ActivityNameTestResultBinding
import com.example.afica.lovetest.domain.usecase.GetZodiacSignsUseCase
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class NameTestResultActivity : BaseActivity<ActivityNameTestResultBinding>() {
    @Inject lateinit var getSigns: GetZodiacSignsUseCase
    private val viewModel: NameTestResultViewModel by viewModels()
    private var yourName = ""
    private var partnerName = ""
    private var animating = false
    private var sharing = false
    private var waitToast: Toast? = null

    override fun onBack() {
        if (viewModel.result.value == null) {
            waitToast?.cancel()
            waitToast = Toast.makeText(this, R.string.result_wait_before_leaving, Toast.LENGTH_SHORT)
            waitToast?.show()
        } else {
            super.onBack()
        }
    }

    override fun onDestroy() {
        waitToast?.cancel()
        waitToast = null
        super.onDestroy()
    }

    override fun provideViewBinding(): ActivityNameTestResultBinding =
        ActivityNameTestResultBinding.inflate(layoutInflater)

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
        viewBaseActivity.btnBack.setOnClickWithPressEffect { onBack() }
        viewBaseActivity.btnRetry.setOnClickWithPressEffect { if (!sharing) viewModel.retry() }
        viewBaseActivity.btnShare.setOnClickWithPressEffect { shareResult() }
    }

    override fun initData() {
        yourName = intent.getStringExtra(Constant.EXTRA_YOUR_NAME)?.trim().orEmpty()
        partnerName = intent.getStringExtra(Constant.EXTRA_PARTNER_NAME)?.trim().orEmpty()
        if (yourName.isEmpty() || partnerName.isEmpty()) {
            finish()
            return
        }
        val profiles = ProfileStore(this)
        val yourProfile = profiles.load(Constant.PROFILE_BOY)
        val partnerProfile = profiles.load(Constant.PROFILE_GIRL)
        val horoscope = intent.getBooleanExtra(Constant.EXTRA_IS_HOROSCOPE, false)
        viewBaseActivity.tvYourName.text = yourName
        viewBaseActivity.tvPartnerName.text = partnerName
        if (horoscope) {
            lifecycleScope.launch {
                val signs = getSigns.execute(GetZodiacSignsUseCase.Param())
                val yourCode = intent.getStringExtra(Constant.EXTRA_YOUR_SIGN_CODE)
                val partnerCode = intent.getStringExtra(Constant.EXTRA_PARTNER_SIGN_CODE)
                val yourSign = signs.firstOrNull { it.code == yourCode }
                val partnerSign = signs.firstOrNull { it.code == partnerCode }
                yourSign?.let {
                    yourName = getString(it.nameRes)
                    viewBaseActivity.tvYourName.text = horoscopeLabel(yourName, it.iconRes )
                }
                partnerSign?.let {
                    partnerName = getString(it.nameRes)
                    viewBaseActivity.tvPartnerName.text = horoscopeLabel(partnerName, it.iconRes)
                }
            }
        }
//        showAvatar(viewBaseActivity.imgYourAvatar, yourProfile.photoPath, R.drawable.boy1)
//        showAvatar(viewBaseActivity.imgPartnerAvatar, partnerProfile.photoPath, R.drawable.girl1)
    }

    private fun horoscopeLabel(sign: String, iconRes: Int): CharSequence {
        val icon = ContextCompat.getDrawable(this, iconRes)?.mutate() ?: return sign
        val size = (20 * resources.displayMetrics.density).toInt()
        val offsetY = 2f * resources.displayMetrics.density
        icon.setBounds(0, 0, size, size)
        val iconSpan = object : ImageSpan(icon, ALIGN_CENTER) {
            override fun draw(
                canvas: Canvas, text: CharSequence, start: Int, end: Int,
                x: Float, top: Int, y: Int, bottom: Int, paint: Paint
            ) {
                val checkpoint = canvas.save()
                canvas.translate(0f, offsetY)
                super.draw(canvas, text, start, end, x, top, y, bottom, paint)
                canvas.restoreToCount(checkpoint)
            }
        }
        return SpannableString("\uFFFC $sign").apply {
            setSpan(iconSpan, 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    override fun initObserver() {
        if (isFinishing) return
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.result.collect { renderResult(it) }
            }
        }
    }

    private fun renderResult(result: LoveTestResult?) {
        val loading = result == null
        val wasLoading = viewBaseActivity.tvPercentage.isLoading
        viewBaseActivity.tvMessage.animate().cancel()
        viewBaseActivity.tvMessage.visibility = if (loading) View.INVISIBLE else View.VISIBLE
        viewBaseActivity.btnShare.isEnabled = !loading && !sharing
        viewBaseActivity.btnRetry.isEnabled = !loading && !sharing
        viewBaseActivity.resultActions.alpha = if (loading) 0.5f else 1f
        if (loading) {
            viewBaseActivity.tvPercentage.startLoading(
                viewModel.remainingLoadingMillis(), viewModel.loadingDurationMillis
            )
            viewBaseActivity.tvMessage.alpha = 0f
            viewBaseActivity.tvMessage.text = ""
            viewBaseActivity.imgInfinity.contentDescription = getString(R.string.result_calculating)
            if (!animating) {
                viewBaseActivity.imgInfinity.setImageResource(R.drawable.animated_vinhcuu)
                (viewBaseActivity.imgInfinity.drawable as? Animatable)?.start()
                animating = true
            }
        } else {
            (viewBaseActivity.imgInfinity.drawable as? Animatable)?.stop()
            animating = false
            viewBaseActivity.imgInfinity.setImageResource(R.drawable.vinhcuu)
            viewBaseActivity.imgInfinity.contentDescription = null
            viewBaseActivity.tvPercentage.showResult(result!!.percentage)
            viewBaseActivity.tvMessage.text = result.message
            if (wasLoading) {
                viewBaseActivity.btnShare.isEnabled = false
                viewBaseActivity.btnRetry.isEnabled = false
                viewBaseActivity.tvMessage.alpha = 0f
                viewBaseActivity.tvMessage.animate().alpha(1f).setDuration(650L)
                    .withEndAction {
                        viewBaseActivity.btnShare.isEnabled = !sharing
                        viewBaseActivity.btnRetry.isEnabled = !sharing
                    }.start()
            } else {
                viewBaseActivity.tvMessage.alpha = 1f
            }
        }
    }

    override fun onStop() {
        viewBaseActivity.tvPercentage.stopAnimations()
        viewBaseActivity.tvMessage.animate().cancel()
        (viewBaseActivity.imgInfinity.drawable as? Animatable)?.stop()
        animating = false
        super.onStop()
    }

//    private fun showAvatar(view: ImageView, path: String?, defaultImage: Int) {
//        view.setImageResource(defaultImage)
//        path?.let { if (File(it).isFile) {
//            view.scaleType = ImageView.ScaleType.CENTER_CROP
//            view.setImageURI(Uri.fromFile(File(it)))
//            if (view.drawable == null) view.setImageResource(defaultImage)
//        } }
//    }

    private fun shareResult() {
        val result = viewModel.result.value ?: return
        if (sharing) return
        sharing = true
        viewBaseActivity.btnShare.isEnabled = false
        viewBaseActivity.btnRetry.isEnabled = false
        lifecycleScope.launch {
            try {
                val bitmap = viewBaseActivity.resultContent.drawToBitmap()
                val file = withContext(Dispatchers.IO) {
                    val directory = File(cacheDir, "name_test_results").apply { mkdirs() }
                    val output = File(directory, UUID.randomUUID().toString() + ".png")
                    try {
                        output.outputStream().use {
                            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) throw IOException("Cannot share result")
                        }
                    } finally {
                        bitmap.recycle()
                    }
                    output
                }
                val uri = FileProvider.getUriForFile(this@NameTestResultActivity, "$packageName.profile.photos", file)
                val share = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_TEXT, getString(R.string.result_share_text,
                        yourName, partnerName, result.percentage, result.message))
                    clipData = ClipData.newUri(contentResolver, getString(R.string.result_title), uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(share, getString(R.string.result_share)))
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                Toast.makeText(this@NameTestResultActivity, R.string.result_share_error, Toast.LENGTH_SHORT).show()
            } finally {
                sharing = false
                val ready = viewModel.result.value != null
                viewBaseActivity.btnShare.isEnabled = ready
                viewBaseActivity.btnRetry.isEnabled = ready
            }
        }
    }

    
}
