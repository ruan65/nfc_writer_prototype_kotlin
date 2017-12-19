package ru.redsys.nfctagger

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.util.Log


/**
 * Created by a on 18/12/17.
 */


object NFCUtil {

    fun createNFCMessage(payload: String, intent: Intent?): Boolean {

        Log.d("nfc_", "createNFCMessage")

//        val pathPrefix = "redsys.com:nfctagger"
        val pathPrefix = ""

        val nfcRecord = NdefRecord(NdefRecord.TNF_EXTERNAL_TYPE,
                pathPrefix.toByteArray(), ByteArray(0),
                payload.toByteArray())

        val nfcMessage = NdefMessage(arrayOf(nfcRecord))

        intent?.let {

            val tag = it.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)

            return writeMessageToTag(nfcMessage, tag)
        }
        return false
    }

    fun retrieveNFCMessage(intent: Intent?): String {
        Log.d("nfc_", "retrieveNFCMessage")

        intent?.let {
            if (NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action) {
                val nDefMessages = getNdefMessages(intent)
                nDefMessages[0].records?.let {

                    it?.forEach {
                        it?.payload.let {
                            it?.let {
                                return String(it)
                            }
                        }
                    }
                }
            } else {
                return "Touch NFC tag to read data"
            }
        }
        return "Touch NFC tag to read data"
    }

    private fun getNdefMessages(intent: Intent): Array<NdefMessage> {
        Log.d("nfc_", "getNdefMessages")

        val rawMessage = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)

        rawMessage?.let {
            return rawMessage.map { it as NdefMessage }.toTypedArray()
        }
        // Unknown tag type
        val empty = byteArrayOf()
        val record = NdefRecord(NdefRecord.TNF_UNKNOWN, empty, empty, empty)
        val msg = NdefMessage(arrayOf(record))
        return arrayOf(msg)
    }

    fun disableNFCInForeground(nfcAdapter: NfcAdapter, activity: Activity) {
        Log.d("nfc_", "disableNFCInForeground")

        nfcAdapter.disableForegroundDispatch(activity)
    }

    fun <T> enableNFCInForeground(nfcAdapter: NfcAdapter, activity: Activity, classType: Class<T>) {
        Log.d("nfc_", "enableNFCInForeground")

        val pendingIntent = PendingIntent.getActivity(activity, 0,
                Intent(activity, classType).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0)

        val nfcIntentFilter = IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED)

        val filters = arrayOf(nfcIntentFilter)

        val techLists = arrayOf(arrayOf(Ndef::class.java.name), arrayOf(NdefFormatable::class.java.name))

        nfcAdapter.enableForegroundDispatch(activity, pendingIntent, filters, techLists)
    }


    private fun writeMessageToTag(nfcMessage: NdefMessage, tag: Tag?): Boolean {
        Log.d("nfc_", "writeMessageToTag")

        try {
            val nDefTag = Ndef.get(tag)

            nDefTag?.let {
                it.connect()
                if (it.maxSize < nfcMessage.toByteArray().size) {
                    return false
                }
                if (it.isWritable) {
                    it.writeNdefMessage(nfcMessage)
                    it.close()
                    return true
                } else {
                    return false
                }
            }

            val ndefFormatableTag = NdefFormatable.get(tag)

            ndefFormatableTag?.let {

                try {
                    it.connect()
                    it.format(nfcMessage)
                    it.close()

                    return true
                } catch (e: Exception) {
                }
            }
        } catch (e: Exception) {
        }

        return false
    }
}