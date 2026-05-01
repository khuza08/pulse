package app.pulse.android.service

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

private const val TAG = "PulseMessaging"
private const val TOPIC_UPDATES = "pulse-updates"

/**
 * Handles incoming FCM messages for push update notifications.
 *
 * When the CI publishes a new release, it sends a data-only FCM message
 * to the "pulse-updates" topic. This service receives that message and
 * delegates to the existing [VersionCheckWorker] to fetch release info
 * and display the update notification.
 */
class PulseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        Log.d(TAG, "FCM message received: ${message.data}")

        val isUpdateMessage = message.data["type"] == "update_available" || 
                             message.data["key"] == "update_available" ||
                             message.data["action"] == "update_available"

        if (isUpdateMessage) {
            Log.d(TAG, "Triggering VersionCheckWorker from FCM push...")
            VersionCheckWorker.executeOneTime(applicationContext)
        }
    }

    override fun onNewToken(token: String) {
        // No-op: We use topic-based messaging, so individual tokens are not needed.
        Log.d(TAG, "New FCM token generated (unused for topic messaging)")
    }

    companion object {
        /**
         * Subscribe this device to the "pulse-updates" FCM topic.
         * This is idempotent — safe to call on every app launch.
         */
        fun subscribe() {
            FirebaseMessaging.getInstance()
                .subscribeToTopic(TOPIC_UPDATES)
                .addOnSuccessListener {
                    Log.d(TAG, "Subscribed to topic: $TOPIC_UPDATES")
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Failed to subscribe to topic: $TOPIC_UPDATES", e)
                }
        }

        /**
         * Unsubscribe this device from the "pulse-updates" FCM topic.
         * Called when user opts out of update notifications.
         */
        fun unsubscribe() {
            FirebaseMessaging.getInstance()
                .unsubscribeFromTopic(TOPIC_UPDATES)
                .addOnSuccessListener {
                    Log.d(TAG, "Unsubscribed from topic: $TOPIC_UPDATES")
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Failed to unsubscribe from topic: $TOPIC_UPDATES", e)
                }
        }
    }
}
