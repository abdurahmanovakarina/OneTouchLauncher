package uz.onetouch.home.data

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface VoiceState {
    data object Idle : VoiceState
    data class Listening(val partial: String, val level: Float) : VoiceState
    data class Result(
        val heard: String,
        val title: String,
        val subtitle: String,
        /** Действие, которое ждёт подтверждения (null — уже выполнено). */
        val confirm: (() -> Unit)? = null,
    ) : VoiceState
    data class Error(val message: String) : VoiceState
}

/** Распознавание речи через системный SpeechRecognizer (Google / встроенный). */
class VoiceController(private val context: Context, private val onText: (String) -> VoiceState) {
    private val _state = MutableStateFlow<VoiceState>(VoiceState.Idle)
    val state: StateFlow<VoiceState> = _state.asStateFlow()
    private var recognizer: SpeechRecognizer? = null

    fun available(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    fun start(language: String = "ru-RU") {
        if (!available()) {
            _state.value = VoiceState.Error("На планшете нет службы распознавания речи. Установите приложение Google.")
            return
        }
        stopRecognizer()
        val r = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer = r
        r.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { _state.value = VoiceState.Listening("", 0f) }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {
                val cur = _state.value
                if (cur is VoiceState.Listening) _state.value = cur.copy(level = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f))
            }
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                _state.value = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                        VoiceState.Error("Не расслышал. Коснитесь кольца и скажите ещё раз.")
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                        VoiceState.Error("Нужно разрешение на микрофон.")
                    SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                        VoiceState.Error("Нет интернета для распознавания речи.")
                    else -> VoiceState.Error("Распознавание недоступно (код $error).")
                }
            }
            override fun onResults(results: Bundle?) {
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                _state.value = if (text.isBlank()) VoiceState.Error("Не расслышал.") else onText(text)
            }
            override fun onPartialResults(partialResults: Bundle?) {
                val text = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                val cur = _state.value
                if (text.isNotBlank() && cur is VoiceState.Listening) _state.value = cur.copy(partial = text)
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
            .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        _state.value = VoiceState.Listening("", 0f)
        r.startListening(intent)
    }

    fun show(state: VoiceState) { _state.value = state }

    fun cancel() {
        stopRecognizer()
        _state.value = VoiceState.Idle
    }

    private fun stopRecognizer() {
        recognizer?.let { runCatching { it.cancel(); it.destroy() } }
        recognizer = null
    }
}
