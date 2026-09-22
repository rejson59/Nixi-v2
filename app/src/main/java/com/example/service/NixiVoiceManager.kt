package com.example.service

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.data.NixiStorageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.sin

class NixiVoiceManager(
    private val context: Context,
    private val storageManager: NixiStorageManager,
    private val scope: CoroutineScope,
    private val onWakeWordDetected: () -> Unit,
    private val onSpeechRecognized: (String) -> Unit
) : TextToSpeech.OnInitListener {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    private var speechRecognizer: SpeechRecognizer? = null
    private var isRecognizerListening = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _currentAmplitude = MutableStateFlow(0f)
    val currentAmplitude: StateFlow<Float> = _currentAmplitude.asStateFlow()

    private val _lastRecognizedText = MutableStateFlow("")
    val lastRecognizedText: StateFlow<String> = _lastRecognizedText.asStateFlow()

    private var ttsAmplitudeJob: Job? = null
    private var audioFocusRequest: AudioFocusRequest? = null

    // Wake word fuzzy match list (acoustically similar Polish phrases)
    private val wakeWordVariants = listOf(
        "hej nixi", "hej niki", "nixi", "hej nixie", "ej nixi", "hej nixi", "hej nexi", "hey nixi", "hej niksi"
    )

    init {
        tts = TextToSpeech(context, this)
        initSpeechRecognizer()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val polish = Locale("pl", "PL")
            val result = tts?.setLanguage(polish)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale.getDefault())
            }
            applyConfigVoiceSettings()
            isTtsReady = true

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                    startSimulatedTtsAmplitude()
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                    stopSimulatedTtsAmplitude()
                    abandonAudioFocus()
                }

                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                    stopSimulatedTtsAmplitude()
                    abandonAudioFocus()
                }
            })
        }
    }

    fun applyConfigVoiceSettings() {
        val config = storageManager.config.value
        tts?.setPitch(config.voicePitch)
        tts?.setSpeechRate(config.voiceSpeed)
    }

    private fun initSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.w("NixiVoiceManager", "Speech recognition not available on device")
            return
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _isListening.value = true
                }

                override fun onBeginningOfSpeech() {}

                override fun onRmsChanged(rmsdB: Float) {
                    // Normalize -2dB..10dB to 0..1 float
                    val norm = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                    _currentAmplitude.value = norm
                }

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    _isListening.value = false
                    _currentAmplitude.value = 0f
                }

                override fun onError(error: Int) {
                    _isListening.value = false
                    _currentAmplitude.value = 0f
                    // If in continuous wake word mode, gently restart after backoff
                    if (storageManager.config.value.backgroundListeningEnabled) {
                        scope.launch {
                            delay(1200)
                            restartListeningIfEnabled()
                        }
                    }
                }

                override fun onResults(results: Bundle?) {
                    _isListening.value = false
                    _currentAmplitude.value = 0f
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val topMatch = matches?.firstOrNull()?.trim() ?: ""
                    if (topMatch.isNotBlank()) {
                        _lastRecognizedText.value = topMatch
                        handleRecognizedUtterance(topMatch)
                    }

                    // Continue listening cycle
                    if (storageManager.config.value.backgroundListeningEnabled) {
                        scope.launch {
                            delay(600)
                            restartListeningIfEnabled()
                        }
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val partials = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val partial = partials?.firstOrNull()?.lowercase(Locale.getDefault()) ?: ""
                    // Quick wake word detection in real-time
                    if (wakeWordVariants.any { partial.contains(it) }) {
                        onWakeWordTriggered()
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
    }

    fun startListeningForWakeWord() {
        if (isRecognizerListening) return
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pl-PL")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            // Enable offline recognition flag where supported by Google/OS
            putExtra("android.speech.extra.PREFER_OFFLINE", true)
        }
        try {
            speechRecognizer?.startListening(intent)
            isRecognizerListening = true
        } catch (e: Exception) {
            Log.e("NixiVoiceManager", "Error starting listening", e)
        }
    }

    fun stopListening() {
        isRecognizerListening = false
        try {
            speechRecognizer?.stopListening()
        } catch (_: Exception) {}
        _isListening.value = false
        _currentAmplitude.value = 0f
    }

    private fun restartListeningIfEnabled() {
        if (!_isSpeaking.value && storageManager.config.value.backgroundListeningEnabled) {
            stopListening()
            startListeningForWakeWord()
        }
    }

    private fun handleRecognizedUtterance(utterance: String) {
        val lower = utterance.lowercase(Locale.getDefault())
        val matchedWake = wakeWordVariants.firstOrNull { lower.contains(it) }
        if (matchedWake != null) {
            onWakeWordTriggered()
            val commandPart = lower.substringAfter(matchedWake).trim()
            if (commandPart.isNotBlank()) {
                onSpeechRecognized(commandPart)
            }
        } else {
            // If already in active conversation turn
            onSpeechRecognized(utterance)
        }
    }

    private fun onWakeWordTriggered() {
        requestAudioFocusAndPauseMedia()
        playWakeChime()
        onWakeWordDetected()
    }

    fun speak(text: String, utteranceId: String = "nixi_response") {
        if (!isTtsReady || text.isBlank()) return
        requestAudioFocusAndPauseMedia()
        applyConfigVoiceSettings()
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stopSpeaking() {
        tts?.stop()
        _isSpeaking.value = false
        stopSimulatedTtsAmplitude()
        abandonAudioFocus()
    }

    private fun startSimulatedTtsAmplitude() {
        ttsAmplitudeJob?.cancel()
        ttsAmplitudeJob = scope.launch(Dispatchers.Default) {
            var step = 0
            while (_isSpeaking.value) {
                // Organic speech modulation wave (0.2 .. 0.95)
                val base = (sin(step * 0.45) * 0.35 + 0.55).toFloat()
                val jitter = ((step % 7) * 0.04f)
                _currentAmplitude.value = (base + jitter).coerceIn(0.1f, 1f)
                step++
                delay(60)
            }
            _currentAmplitude.value = 0f
        }
    }

    private fun stopSimulatedTtsAmplitude() {
        ttsAmplitudeJob?.cancel()
        ttsAmplitudeJob = null
        _currentAmplitude.value = 0f
    }

    // High quality synthetic wake chime (pleasant two-tone ascending chord 587Hz -> 880Hz)
    private fun playWakeChime() {
        scope.launch(Dispatchers.Default) {
            try {
                val sampleRate = 24000
                val durationMs = 180
                val numSamples = (sampleRate * durationMs) / 1000
                val buffer = ShortArray(numSamples)

                for (i in 0 until numSamples) {
                    val progress = i.toDouble() / numSamples
                    val freq = if (progress < 0.45) 587.33 else 880.0 // D5 to A5
                    val envelope = if (progress < 0.1) progress / 0.1 else (1.0 - progress)
                    val sample = (sin(2.0 * Math.PI * i * freq / sampleRate) * envelope * 14000).toInt().toShort()
                    buffer[i] = sample
                }

                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(buffer.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                audioTrack.write(buffer, 0, buffer.size)
                audioTrack.play()
                delay(durationMs.toLong() + 50)
                audioTrack.release()
            } catch (e: Exception) {
                Log.w("NixiVoiceManager", "Error playing wake chime", e)
            }
        }
    }

    private fun requestAudioFocusAndPauseMedia() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val playbackAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
                audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(playbackAttributes)
                    .setAcceptsDelayedFocusGain(true)
                    .build()
                audioFocusRequest?.let { audioManager.requestAudioFocus(it) }
            } else {
                @Suppress("DEPRECATION")
                audioManager.requestAudioFocus(
                    null,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
                )
            }
        } catch (e: Exception) {
            Log.w("NixiVoiceManager", "Audio focus request failed", e)
        }
    }

    private fun abandonAudioFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
            } else {
                @Suppress("DEPRECATION")
                audioManager.abandonAudioFocus(null)
            }
        } catch (_: Exception) {}
    }

    fun release() {
        stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
