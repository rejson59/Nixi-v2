package com.example.service

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.view.KeyEvent

class SpotifyController(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    fun openSpotify() {
        val launchIntent = context.packageManager.getLaunchIntentForPackage("com.spotify.music")
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
        } else {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://open.spotify.com")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
        }
    }

    fun play() {
        dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY)
    }

    fun pause() {
        dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PAUSE)
    }

    fun next() {
        dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT)
    }

    fun previous() {
        dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
    }

    fun searchAndPlay(query: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("spotify:search:${Uri.encode(query)}")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            openSpotify()
        }
    }

    private fun dispatchMediaKey(keyCode: Int) {
        val eventDown = KeyEvent(KeyEvent.ACTION_DOWN, keyCode)
        val eventUp = KeyEvent(KeyEvent.ACTION_UP, keyCode)
        audioManager.dispatchMediaKeyEvent(eventDown)
        audioManager.dispatchMediaKeyEvent(eventUp)
    }
}
