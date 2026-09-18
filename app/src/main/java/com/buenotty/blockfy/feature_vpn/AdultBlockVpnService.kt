package com.buenotty.blockfy.feature_vpn

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.buenotty.blockfy.R
import com.buenotty.blockfy.feature_accessibility.AdultContentDetector
import com.buenotty.blockfy.feature_monitor.BankPackages
import com.buenotty.blockfy.feature_monitor.InterruptActivity
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.atomic.AtomicBoolean

class AdultBlockVpnService : VpnService() {

    private val running = AtomicBoolean(false)
    private var tunInterface: ParcelFileDescriptor? = null
    private var worker: Thread? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        showForegroundNotification()
        if (intent?.action == ACTION_STOP) {
            stopShield()
            stopSelf()
            return START_NOT_STICKY
        }
        startShield()
        return START_STICKY
    }

    override fun onDestroy() {
        stopShield()
        super.onDestroy()
    }

    private fun startShield() {
        if (running.getAndSet(true)) return

        val builder = Builder()
            .setSession("Blockfy Shield")
            .setMtu(1500)
            .addAddress(VPN_ADDRESS, 32)
            .addDnsServer(VPN_DNS)
            .addRoute(VPN_DNS, 32)
            .setBlocking(true)

        for (pkg in BankPackages.ALL) {
            try {
                builder.addDisallowedApplication(pkg)
            } catch (_: Exception) {
            }
        }
        try {
            builder.addDisallowedApplication(packageName)
        } catch (_: Exception) {
        }

        tunInterface = builder.establish()
        val pfd = tunInterface
        if (pfd == null) {
            running.set(false)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }

        worker = Thread({ pump(pfd) }, "blockfy-dns").also { it.start() }
    }

    private fun showForegroundNotification() {
        createChannel()
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_policy)
            .setContentTitle(getString(R.string.vpn_notification_title))
            .setContentText(getString(R.string.vpn_notification_text))
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIF_ID, notification)
        }
    }

    private fun stopShield() {
        running.set(false)
        try {
            tunInterface?.close()
        } catch (_: Exception) {
        }
        tunInterface = null
        worker?.interrupt()
        worker = null
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun pump(pfd: ParcelFileDescriptor) {
        val input = FileInputStream(pfd.fileDescriptor)
        val output = FileOutputStream(pfd.fileDescriptor)
        val packet = ByteArray(32767)
        val upstream = DatagramSocket()
        protect(upstream)
        upstream.soTimeout = 1500
        val resolver = InetAddress.getByName(UPSTREAM_DNS)

        while (running.get()) {
            val length = try {
                input.read(packet)
            } catch (_: Exception) {
                break
            }
            if (length <= 0) continue
            val parsed = DnsPackets.parseIpv4Udp(packet, length) ?: continue
            if (parsed.dstPort != 53) continue

            val host = DnsPackets.readQueryName(parsed.payload)
            val responsePayload = if (DnsPackets.isBlockedHost(host)) {
                notifyAdultBlock(host)
                DnsPackets.buildBlockedResponse(parsed.payload)
            } else {
                forward(upstream, resolver, parsed.payload)
            } ?: continue

            val reply = DnsPackets.wrapIpv4Udp(
                srcIp = parsed.dstIp,
                dstIp = parsed.srcIp,
                srcPort = parsed.dstPort,
                dstPort = parsed.srcPort,
                payload = responsePayload
            )
            try {
                output.write(reply)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to write DNS reply", e)
                break
            }
        }
        try {
            upstream.close()
        } catch (_: Exception) {
        }
    }

    private fun forward(socket: DatagramSocket, resolver: InetAddress, query: ByteArray): ByteArray? {
        return try {
            val outgoing = DatagramPacket(query, query.size, resolver, 53)
            socket.send(outgoing)
            val buffer = ByteArray(4096)
            val incoming = DatagramPacket(buffer, buffer.size)
            socket.receive(incoming)
            buffer.copyOf(incoming.length)
        } catch (e: Exception) {
            Log.w(TAG, "DNS forward failed", e)
            null
        }
    }

    private fun notifyAdultBlock(host: String?) {
        val now = System.currentTimeMillis()
        if (now - lastAlertAt < 1500L) return
        lastAlertAt = now
        val quote = AdultContentDetector.getRandomWarning(this)
        val intent = InterruptActivity.intent(
            this,
            InterruptActivity.KIND_ADULT,
            getString(R.string.adult_block_alert_title),
            quote
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Log.w(TAG, "Unable to show adult block screen for $host", e)
        }
        showAlertNotification(intent, quote)
    }

    /**
     * Android 10+ blocks background activity starts, so the startActivity above
     * is silently dropped while the browser is in front. The full-screen intent
     * is what actually brings the warning up.
     */
    private fun showAlertNotification(intent: Intent, message: String) {
        val pending = PendingIntent.getActivity(
            this,
            43,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ALERT_ID)
            .setSmallIcon(R.drawable.ic_policy)
            .setContentTitle(getString(R.string.adult_block_alert_title))
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pending)
            .setFullScreenIntent(pending, true)
            .setAutoCancel(true)
            .build()
        try {
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(ALERT_NOTIF_ID, notification)
        } catch (e: Exception) {
            Log.w(TAG, "Unable to post adult block alert", e)
        }
    }

    private fun createChannel() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, getString(R.string.vpn_channel_name), NotificationManager.IMPORTANCE_LOW)
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ALERT_ID, getString(R.string.vpn_alert_channel_name), NotificationManager.IMPORTANCE_HIGH)
        )
    }

    companion object {
        private const val TAG = "BlockfyVpn"
        private const val ACTION_STOP = "com.buenotty.blockfy.STOP_ADULT_VPN"
        private const val CHANNEL_ID = "blockfy_vpn"
        private const val CHANNEL_ALERT_ID = "blockfy_alerts"
        private const val NOTIF_ID = 1003
        private const val ALERT_NOTIF_ID = 1004
        private const val VPN_ADDRESS = "10.7.0.2"
        private const val VPN_DNS = "10.7.0.1"
        private const val UPSTREAM_DNS = "1.1.1.1"

        @Volatile
        private var lastAlertAt = 0L

        fun start(context: Context) {
            val intent = Intent(context, AdultBlockVpnService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, AdultBlockVpnService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
