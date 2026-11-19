package com.sc.player

import com.sc.player.data.LocaleHelper
import android.content.Context
import android.content.res.Configuration
import com.sc.player.ui.player.PipState
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import android.content.Intent
import com.sc.player.data.Channel
import com.sc.player.data.DeepLink
import com.sc.player.data.WatchNext
import com.sc.player.ui.AppNav
import com.sc.player.ui.theme.SCPlayerTheme

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
                .launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        com.sc.player.ui.theme.ThemeState.current = com.sc.player.ui.theme.AppTheme.byId(com.sc.player.data.UserPrefs(this).theme())
        installImageLoader()
        readDeepLink(intent)
        setContent {
            SCPlayerTheme { AppNav() }
        }
    }

    /** تحميل صور أسرع: كاش ذاكرة + قرص كبير، تجاهل no-cache بتاع سيرفرات الصور، اتصالات متوازية، وظهور بنعومة. */
    private fun installImageLoader() {
        val client = okhttp3.OkHttpClient.Builder()
            .dispatcher(okhttp3.Dispatcher().apply { maxRequests = 48; maxRequestsPerHost = 16 })
            .connectTimeout(8, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .build()
        coil.Coil.setImageLoader(
            coil.ImageLoader.Builder(this)
                .okHttpClient(client)
                .memoryCache { coil.memory.MemoryCache.Builder(this).maxSizePercent(0.25).build() }
                .diskCache {
                    coil.disk.DiskCache.Builder()
                        .directory(java.io.File(cacheDir, "image_cache"))
                        .maxSizeBytes(300L * 1024 * 1024)
                        .build()
                }
                .respectCacheHeaders(false)
                .allowRgb565(true)
                .crossfade(220)
                .build(),
        )
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        readDeepLink(intent)
    }

    private fun readDeepLink(i: Intent?) {
        val url = i?.getStringExtra(WatchNext.EXTRA_URL) ?: return
        i.removeExtra(WatchNext.EXTRA_URL)
        DeepLink.item.value = Channel(i.getStringExtra(WatchNext.EXTRA_NAME).orEmpty(), url, "", i.getStringExtra(WatchNext.EXTRA_LOGO), null)
    }

    // خروج المستخدم (زر الهوم) أثناء المشغّل → صورة داخل صورة
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (PipState.playerActive && PipState.autoEnter) PipState.enter(this)
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        PipState.inPip.value = isInPictureInPictureMode
    }
}
