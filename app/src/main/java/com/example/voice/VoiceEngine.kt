package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

sealed interface SpeechRecognitionState {
    data object Idle : SpeechRecognitionState
    data object Listening : SpeechRecognitionState
    data class Partial(val text: String) : SpeechRecognitionState
    data class Error(val message: String) : SpeechRecognitionState
}

class VoiceEngine(private val context: Context) {

    private val appContext = context.applicationContext

    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var pendingSpeakText: String? = null
    private var currentSpeechRate: Float = 1.0f

    private val _recognitionState = MutableStateFlow<SpeechRecognitionState>(SpeechRecognitionState.Idle)
    val recognitionState: StateFlow<SpeechRecognitionState> = _recognitionState.asStateFlow()

    private val _rmsDb = MutableStateFlow(0f)
    val rmsDb: StateFlow<Float> = _rmsDb.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _ttsError = MutableStateFlow<String?>(null)
    val ttsError: StateFlow<String?> = _ttsError.asStateFlow()

    private val _lastSpokenText = MutableStateFlow<String?>(null)
    val lastSpokenText: StateFlow<String?> = _lastSpokenText.asStateFlow()

    val isRecognitionAvailable: Boolean
        get() = SpeechRecognizer.isRecognitionAvailable(appContext)

    init {
        initTts()
    }

    private fun initTts() {
        try {
            tts = TextToSpeech(appContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val result = tts?.setLanguage(Locale.US)
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        val fallback = tts?.setLanguage(Locale.getDefault())
                        ttsReady = fallback != TextToSpeech.LANG_MISSING_DATA && fallback != TextToSpeech.LANG_NOT_SUPPORTED
                        if (!ttsReady) {
                            _ttsError.value = "English Text-to-Speech voice data is not installed on this device."
                        }
                    } else {
                        ttsReady = true
                        _ttsError.value = null
                    }
                    tts?.setSpeechRate(currentSpeechRate)
                    tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            _isSpeaking.value = true
                        }

                        override fun onDone(utteranceId: String?) {
                            _isSpeaking.value = false
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            _isSpeaking.value = false
                        }

                        override fun onError(utteranceId: String?, errorCode: Int) {
                            _isSpeaking.value = false
                            _ttsError.value = "Voice playback error (code $errorCode)."
                        }
                    })
                    pendingSpeakText?.let { queued ->
                        pendingSpeakText = null
                        speak(queued, currentSpeechRate)
                    }
                } else {
                    ttsReady = false
                    _ttsError.value = "Text-to-Speech engine is unavailable on this device."
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize TextToSpeech", e)
            _ttsError.value = "Text-to-Speech unavailable: ${e.localizedMessage}"
        }
    }

    fun startListening(onFinalResult: (String) -> Unit) {
        if (!isRecognitionAvailable) {
            _recognitionState.value = SpeechRecognitionState.Error(
                "On-device Speech Recognition service is unavailable on this device. You can type your response below or use AI Chat."
            )
            return
        }

        stopSpeaking()
        destroyRecognizer()

        try {
            val recognizer = SpeechRecognizer.createSpeechRecognizer(appContext)
            speechRecognizer = recognizer

            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _recognitionState.value = SpeechRecognitionState.Listening
                }

                override fun onBeginningOfSpeech() {
                    _recognitionState.value = SpeechRecognitionState.Listening
                }

                override fun onRmsChanged(rmsdB: Float) {
                    _rmsDb.value = rmsdB.coerceIn(0f, 12f)
                }

                override fun onBufferReceived(buffer: ByteArray?) = Unit

                override fun onEndOfSpeech() {
                    _rmsDb.value = 0f
                }

                override fun onError(error: Int) {
                    _rmsDb.value = 0f
                    val msg = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error. Please check your microphone."
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required."
                        SpeechRecognizer.ERROR_NETWORK,
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network error during speech recognition. Please try again."
                        SpeechRecognizer.ERROR_NO_MATCH -> "Didn't catch that. Tap the mic and speak clearly in English."
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer is busy. Please wait a moment."
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected. Tap the mic when ready to speak."
                        else -> "Speech recognition unavailable (code $error)."
                    }
                    _recognitionState.value = SpeechRecognitionState.Error(msg)
                }

                override fun onResults(results: Bundle?) {
                    _rmsDb.value = 0f
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull()?.trim().orEmpty()
                    if (text.isNotBlank()) {
                        _recognitionState.value = SpeechRecognitionState.Idle
                        onFinalResult(text)
                    } else {
                        _recognitionState.value = SpeechRecognitionState.Error("Didn't catch any English words. Please try again.")
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val partial = partialResults
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        ?.trim()
                    if (!partial.isNullOrBlank()) {
                        _recognitionState.value = SpeechRecognitionState.Partial(partial)
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            _recognitionState.value = SpeechRecognitionState.Listening
            recognizer.startListening(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start SpeechRecognizer", e)
            _recognitionState.value = SpeechRecognitionState.Error(
                e.localizedMessage ?: "Failed to start microphone recognition."
            )
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (_: Exception) {
        }
        _rmsDb.value = 0f
        if (_recognitionState.value is SpeechRecognitionState.Listening ||
            _recognitionState.value is SpeechRecognitionState.Partial
        ) {
            _recognitionState.value = SpeechRecognitionState.Idle
        }
    }

    fun clearRecognitionError() {
        if (_recognitionState.value is SpeechRecognitionState.Error) {
            _recognitionState.value = SpeechRecognitionState.Idle
        }
    }

    fun speak(text: String, speechRate: Float = 1.0f) {
        val cleanText = text.trim()
        if (cleanText.isBlank()) return
        currentSpeechRate = speechRate.coerceIn(0.5f, 1.5f)
        _lastSpokenText.value = cleanText
        if (!ttsReady) {
            pendingSpeakText = cleanText
            return
        }
        try {
            tts?.setSpeechRate(currentSpeechRate)
            val utteranceId = UUID.randomUUID().toString()
            tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        } catch (e: Exception) {
            Log.e(TAG, "TTS speak failed", e)
            _ttsError.value = "Could not play voice response: ${e.localizedMessage}"
        }
    }

    fun replayLast(speechRate: Float = 1.0f) {
        _lastSpokenText.value?.let { speak(it, speechRate) }
    }

    fun stopSpeaking() {
        try {
            tts?.stop()
        } catch (_: Exception) {
        }
        _isSpeaking.value = false
    }

    private fun destroyRecognizer() {
        try {
            speechRecognizer?.destroy()
        } catch (_: Exception) {
        }
        speechRecognizer = null
    }

    fun release() {
        destroyRecognizer()
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {
        }
        tts = null
        ttsReady = false
    }

    companion object {
        private const val TAG = "VoiceEngine"
    }
}
