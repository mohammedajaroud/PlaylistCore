package com.devbrackets.android.playlistcore.helper.audiofocus

import android.content.Context
import android.media.AudioManager
import com.devbrackets.android.playlistcore.api.PlaylistItem
import com.devbrackets.android.playlistcore.helper.playlist.PlaylistHandler
import com.devbrackets.android.playlistcore.util.AudioManagerCompat

open class DefaultAudioFocusProvider<I : PlaylistItem>(context: Context) : AudioFocusProvider<I>, AudioManager.OnAudioFocusChangeListener {
    companion object {
        const val AUDIOFOCUS_NONE = 0
    }

    protected var pausedForFocusLoss = false
    protected var currentAudioFocus = AUDIOFOCUS_NONE
    protected var handler: PlaylistHandler<I>? = null

    protected var audioManager = AudioManagerCompat(context)

    override fun setPlaylistHandler(playlistHandler: PlaylistHandler<I>) {
        handler = playlistHandler
    }

    override fun requestFocus(): Boolean {
        if (handler?.currentMediaPlayer?.handlesOwnAudioFocus ?: true) {
            return false
        }

        if (currentAudioFocus == AudioManager.AUDIOFOCUS_GAIN) {
            return true
        }

        val status = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        return AudioManager.AUDIOFOCUS_REQUEST_GRANTED == status
    }

    override fun abandonFocus(): Boolean {
        if (handler?.currentMediaPlayer?.handlesOwnAudioFocus ?: true) {
            return false
        }

        if (currentAudioFocus == AUDIOFOCUS_NONE) {
            return true
        }

        val status = audioManager.abandonAudioFocus(this)
        if (AudioManager.AUDIOFOCUS_REQUEST_GRANTED == status) {
            currentAudioFocus = AUDIOFOCUS_NONE
        }

        return AudioManager.AUDIOFOCUS_REQUEST_GRANTED == status
    }

    override fun onAudioFocusChange(focusChange: Int) {
        if (currentAudioFocus == focusChange) {
            return
        }

        currentAudioFocus = focusChange
        if (handler?.currentMediaPlayer?.handlesOwnAudioFocus ?: true) {
            return
        }

        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> onFocusGained()
            AudioManager.AUDIOFOCUS_LOSS -> onFocusLoss()
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> onFocusLossTransient()
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> onFocusLossTransientCanDuck()
        }
    }

    open fun onFocusGained() {
        handler?.currentMediaPlayer?.let {
            if (pausedForFocusLoss && !it.isPlaying) {
                pausedForFocusLoss = false
                handler?.play()
            } else {
                it.setVolume(1.0f, 1.0f)
            }
        }
    }

    open fun onFocusLoss() {
        handler?.currentMediaPlayer?.let {
            if (it.isPlaying) {
                pausedForFocusLoss = true
                handler?.pause()
            }
        }
    }

    open fun onFocusLossTransient() {
        onFocusLoss()
    }

    open fun onFocusLossTransientCanDuck() {
        handler?.currentMediaPlayer?.let {
            if (it.isPlaying) {
                it.setVolume(0.1f, 0.1f)
            }
        }
    }
}