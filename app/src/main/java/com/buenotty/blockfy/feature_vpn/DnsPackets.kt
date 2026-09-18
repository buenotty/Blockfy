package com.buenotty.blockfy.feature_vpn

import com.buenotty.blockfy.feature_accessibility.AdultContentDetector
import java.nio.ByteBuffer
import java.nio.ByteOrder

object DnsPackets {

    fun readQueryName(udpPayload: ByteArray): String? {
        if (udpPayload.size < 13) return null
        var index = 12
        val labels = ArrayList<String>()
        var hops = 0
        while (index < udpPayload.size && hops < 20) {
            val length = udpPayload[index].toInt() and 0xFF
            if (length == 0) {
                index++
                break
            }
            if (length and 0xC0 == 0xC0) {
                return null
            }
            if (index + 1 + length > udpPayload.size) return null
            labels += String(udpPayload, index + 1, length, Charsets.US_ASCII)
            index += 1 + length
            hops++
        }
        if (labels.isEmpty()) return null
        return labels.joinToString(".")
    }

    fun readQueryType(udpPayload: ByteArray): Int {
        val nameEnd = skipName(udpPayload) ?: return -1
        if (nameEnd + 2 > udpPayload.size) return -1
        return ((udpPayload[nameEnd].toInt() and 0xFF) shl 8) or (udpPayload[nameEnd + 1].toInt() and 0xFF)
    }

    fun buildBlockedResponse(query: ByteArray): ByteArray? {
        if (query.size < 12) return null
        val nameEnd = skipName(query) ?: return null
        val questionEnd = nameEnd + 4
        if (questionEnd > query.size) return null
        val qtype = ((query[nameEnd].toInt() and 0xFF) shl 8) or (query[nameEnd + 1].toInt() and 0xFF)

        val answerRdata: ByteArray = when (qtype) {
            1 -> byteArrayOf(0, 0, 0, 0)
            28 -> ByteArray(16)
            else -> return query.copyOfRange(0, questionEnd).also { header ->
                header[2] = (0x81).toByte()
                header[3] = (0x83).toByte()
                header[6] = 0
                header[7] = 0
            }
        }

        val response = ByteArray(questionEnd + 12 + answerRdata.size)
        System.arraycopy(query, 0, response, 0, questionEnd)
        response[2] = (0x81).toByte()
        response[3] = (0x80).toByte()
        response[6] = 0
        response[7] = 1
        response[8] = 0
        response[9] = 0
        response[10] = 0
        response[11] = 0

        var pos = questionEnd
        response[pos] = (0xC0).toByte()
        response[pos + 1] = 0x0C
        pos += 2
        response[pos] = ((qtype shr 8) and 0xFF).toByte()
        response[pos + 1] = (qtype and 0xFF).toByte()
        pos += 2
        response[pos] = 0
        response[pos + 1] = 1
        pos += 2
        response[pos] = 0
        response[pos + 1] = 0
        response[pos + 2] = 0
        response[pos + 3] = 60
        pos += 4
        response[pos] = ((answerRdata.size shr 8) and 0xFF).toByte()
        response[pos + 1] = (answerRdata.size and 0xFF).toByte()
        pos += 2
        System.arraycopy(answerRdata, 0, response, pos, answerRdata.size)
        return response
    }

    fun isBlockedHost(host: String?): Boolean = AdultContentDetector.isBlockedHost(host)

    private fun skipName(payload: ByteArray): Int? {
        var index = 12
        var hops = 0
        while (index < payload.size && hops < 20) {
            val length = payload[index].toInt() and 0xFF
            if (length == 0) return index + 1
            if (length and 0xC0 == 0xC0) return null
            index += 1 + length
            hops++
        }
        return null
    }

    fun ipv4Checksum(header: ByteArray, offset: Int = 0, length: Int = 20): Int {
        var sum = 0
        var i = offset
        val end = offset + length
        while (i < end) {
            val word = ((header[i].toInt() and 0xFF) shl 8) or (header[i + 1].toInt() and 0xFF)
            sum += word
            i += 2
        }
        while (sum shr 16 != 0) {
            sum = (sum and 0xFFFF) + (sum shr 16)
        }
        return sum.inv() and 0xFFFF
    }

    fun wrapIpv4Udp(srcIp: ByteArray, dstIp: ByteArray, srcPort: Int, dstPort: Int, payload: ByteArray): ByteArray {
        val packet = ByteArray(20 + 8 + payload.size)
        packet[0] = 0x45
        val total = packet.size
        packet[2] = ((total shr 8) and 0xFF).toByte()
        packet[3] = (total and 0xFF).toByte()
        packet[8] = 64
        packet[9] = 17
        System.arraycopy(srcIp, 0, packet, 12, 4)
        System.arraycopy(dstIp, 0, packet, 16, 4)
        val ipSum = ipv4Checksum(packet)
        packet[10] = ((ipSum shr 8) and 0xFF).toByte()
        packet[11] = (ipSum and 0xFF).toByte()

        packet[20] = ((srcPort shr 8) and 0xFF).toByte()
        packet[21] = (srcPort and 0xFF).toByte()
        packet[22] = ((dstPort shr 8) and 0xFF).toByte()
        packet[23] = (dstPort and 0xFF).toByte()
        val udpLen = 8 + payload.size
        packet[24] = ((udpLen shr 8) and 0xFF).toByte()
        packet[25] = (udpLen and 0xFF).toByte()
        System.arraycopy(payload, 0, packet, 28, payload.size)

        val udpSum = udpChecksum(srcIp, dstIp, packet, 20, udpLen)
        packet[26] = ((udpSum shr 8) and 0xFF).toByte()
        packet[27] = (udpSum and 0xFF).toByte()
        return packet
    }

    fun parseIpv4Udp(packet: ByteArray, length: Int): Ipv4Udp? {
        if (length < 28) return null
        if (packet[0].toInt() and 0xF0 != 0x40) return null
        val headerLen = (packet[0].toInt() and 0x0F) * 4
        if (packet[9].toInt() and 0xFF != 17) return null
        if (length < headerLen + 8) return null
        val srcIp = packet.copyOfRange(12, 16)
        val dstIp = packet.copyOfRange(16, 20)
        val srcPort = ((packet[headerLen].toInt() and 0xFF) shl 8) or (packet[headerLen + 1].toInt() and 0xFF)
        val dstPort = ((packet[headerLen + 2].toInt() and 0xFF) shl 8) or (packet[headerLen + 3].toInt() and 0xFF)
        val payload = packet.copyOfRange(headerLen + 8, length)
        return Ipv4Udp(srcIp, dstIp, srcPort, dstPort, payload)
    }

    private fun udpChecksum(srcIp: ByteArray, dstIp: ByteArray, packet: ByteArray, udpOffset: Int, udpLen: Int): Int {
        val buffer = ByteBuffer.allocate(12 + udpLen).order(ByteOrder.BIG_ENDIAN)
        buffer.put(srcIp)
        buffer.put(dstIp)
        buffer.put(0)
        buffer.put(17)
        buffer.putShort(udpLen.toShort())
        buffer.put(packet, udpOffset, udpLen)
        buffer.rewind()
        var sum = 0
        while (buffer.remaining() > 1) {
            sum += buffer.short.toInt() and 0xFFFF
        }
        if (buffer.remaining() == 1) {
            sum += (buffer.get().toInt() and 0xFF) shl 8
        }
        while (sum shr 16 != 0) {
            sum = (sum and 0xFFFF) + (sum shr 16)
        }
        val result = sum.inv() and 0xFFFF
        return if (result == 0) 0xFFFF else result
    }

    data class Ipv4Udp(
        val srcIp: ByteArray,
        val dstIp: ByteArray,
        val srcPort: Int,
        val dstPort: Int,
        val payload: ByteArray
    )
}
