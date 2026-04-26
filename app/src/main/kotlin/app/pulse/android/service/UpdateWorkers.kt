package app.pulse.android.service

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.pulse.android.BuildConfig
import app.pulse.android.R
import app.pulse.android.utils.hasPermission
import app.pulse.android.utils.pendingIntent
import app.pulse.core.data.utils.Version
import app.pulse.core.data.utils.version
import app.pulse.core.ui.utils.isAtLeastAndroid13
import app.pulse.providers.github.GitHub
import app.pulse.providers.github.models.Release
import app.pulse.providers.github.requests.releases
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import kotlin.time.Duration
import kotlin.time.toJavaDuration

private val VERSION_NAME = BuildConfig.VERSION_NAME.substringBeforeLast("-")
private const val REPO_OWNER = "khuza08"
private const val REPO_NAME = "pulse"
private val permission = android.Manifest.permission.POST_NOTIFICATIONS

suspend fun Version.getNewerVersion(
    repoOwner: String = REPO_OWNER,
    repoName: String = REPO_NAME,
    contentType: String = "application/vnd.android.package-archive"
) = GitHub.releases(
    owner = repoOwner,
    repo = repoName
)?.mapCatching { releases ->
    releases
        .sortedByDescending { it.publishedAt }
        .firstOrNull { release ->
            !release.draft &&
                !release.preRelease &&
                release.tag.version > this &&
                release.assets.any {
                    it.contentType == contentType && it.state == Release.Asset.State.Uploaded
                }
        }
}

class VersionCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_TAG = "version_check_worker"

        fun executeOneTime(context: Context) {
            val request = OneTimeWorkRequestBuilder<VersionCheckWorker>()
                .addTag(WORK_TAG)
                .setConstraints(Constraints(requiredNetworkType = NetworkType.CONNECTED))
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                "${WORK_TAG}_onetime",
                ExistingWorkPolicy.REPLACE,
                request
            )
        }

        fun upsert(context: Context, period: Duration?) = runCatching {
            val workManager = WorkManager.getInstance(context)

            if (period == null) {
                workManager.cancelAllWorkByTag(WORK_TAG)
                return@runCatching
            }

            val request = PeriodicWorkRequestBuilder<VersionCheckWorker>(period.toJavaDuration())
                .addTag(WORK_TAG)
                .setConstraints(
                    Constraints(
                        requiredNetworkType = NetworkType.CONNECTED,
                        requiresBatteryNotLow = true
                    )
                )
                .build()

            workManager.enqueueUniquePeriodicWork(
                /* uniqueWorkName = */ WORK_TAG,
                /* existingPeriodicWorkPolicy = */ ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
                /* periodicWork = */ request
            )
        }.onFailure { it.printStackTrace() }
    }

    override suspend fun doWork(): Result = with(applicationContext) {
        if (isAtLeastAndroid13 && !hasPermission(permission)) return Result.retry()

        val result = withContext(Dispatchers.IO) {
            VERSION_NAME.version.getNewerVersion().also { it?.exceptionOrNull()?.printStackTrace() }
        }

        result?.getOrNull()?.let { release ->
            val asset = release.assets.firstOrNull { it.contentType == "application/vnd.android.package-archive" }
            
            ServiceNotifications.version.sendNotification(applicationContext) {
                this.setSmallIcon(R.drawable.download)
                    .setContentTitle(getString(R.string.new_version_available))
                
                if (asset != null) {
                    val downloadIntent = Intent(applicationContext, DownloadUpdateReceiver::class.java).apply {
                        putExtra("url", asset.downloadUrl.toString())
                    }
                    val pendingDownload = PendingIntent.getBroadcast(
                        applicationContext,
                        0,
                        downloadIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    this.setContentText(getString(R.string.download))
                        .setContentIntent(pendingDownload)
                } else {
                    // Fallback to github url
                    this.setContentText(getString(R.string.redirect_github))
                        .also { builder ->
                            runCatching { release.frontendUrl.toString().toUri() }.getOrNull()?.let { url ->
                                builder.setContentIntent(pendingIntent(Intent(Intent.ACTION_VIEW, url)))
                            }
                        }
                }
                this.setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
            }
        }

        return when {
            result == null || result.isFailure -> Result.retry()
            result.isSuccess -> Result.success()
            else -> Result.failure()
        }
    }
}

class DownloadUpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val url = intent.getStringExtra("url") ?: return
        
        // Dismiss the notification since the user tapped it
        ServiceNotifications.version.cancel(context)
        
        // Start the download worker
        DownloadUpdateWorker.execute(context, url)
    }
}

class DownloadUpdateWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val WORK_TAG = "download_update_worker"

        fun execute(context: Context, downloadUrl: String) {
            val inputData = androidx.work.Data.Builder().putString("url", downloadUrl).build()
            val request = OneTimeWorkRequestBuilder<DownloadUpdateWorker>()
                .addTag(WORK_TAG)
                .setInputData(inputData)
                .setConstraints(Constraints(requiredNetworkType = NetworkType.CONNECTED))
                .build()
            
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_TAG,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }

    override suspend fun doWork(): Result = with(applicationContext) {
        val urlString = inputData.getString("url") ?: return Result.failure()

        ServiceNotifications.version.sendNotification(applicationContext) {
            this.setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle(getString(R.string.downloading_update))
                .setProgress(0, 0, true)
                .setOngoing(true)
        }

        runCatching {
            val file = File(externalCacheDir ?: cacheDir, "updates/update.apk")
            file.parentFile?.mkdirs()
            
            withContext(Dispatchers.IO) {
                URL(urlString).openStream().use { input ->
                    FileOutputStream(file).use { output ->
                        input.copyTo(output)
                    }
                }
            }

            val uri = FileProvider.getUriForFile(applicationContext, "${packageName}.provider", file)
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            ServiceNotifications.version.sendNotification(applicationContext) {
                this.setSmallIcon(R.drawable.download)
                    .setContentTitle(getString(R.string.update_ready))
                    .setContentText(getString(R.string.install_update))
                    .setContentIntent(PendingIntent.getActivity(applicationContext, 0, installIntent, PendingIntent.FLAG_IMMUTABLE))
                    .setAutoCancel(true)
                    .setOngoing(false)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
            }
            Result.success()
        }.onFailure {
            it.printStackTrace()
            ServiceNotifications.version.sendNotification(applicationContext) {
                this.setSmallIcon(android.R.drawable.stat_notify_error)
                    .setContentTitle(getString(R.string.error_message))
                    .setAutoCancel(true)
                    .setOngoing(false)
            }
            return Result.retry()
        }.getOrDefault(Result.failure())
    }
}
