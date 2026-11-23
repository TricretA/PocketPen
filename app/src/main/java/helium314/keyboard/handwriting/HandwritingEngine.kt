package helium314.keyboard.handwriting

import com.google.mlkit.common.MlKitException
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognition
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognitionModel
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognitionModelIdentifier
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognizer
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognizerOptions
import com.google.mlkit.vision.digitalink.recognition.Ink
import com.google.mlkit.vision.digitalink.recognition.RecognitionContext

data class RecognitionOutcome(val candidates: List<String>, val shapeConfident: Boolean)

class HandwritingEngine(languageTag: String) {
    private val manager = RemoteModelManager.getInstance()
    private val textModel = buildModel(languageTag)
    private val shapeModel = buildModel(SHAPES_TAG)
    private var textRecognizer: DigitalInkRecognizer? = null
    private var shapeRecognizer: DigitalInkRecognizer? = null

    fun prepare(onDownloading: () -> Unit, onReady: () -> Unit, onFailed: (String) -> Unit) {
        val model = textModel
        if (model == null) {
            onFailed("Unsupported handwriting language")
            return
        }
        manager.isModelDownloaded(model)
            .addOnSuccessListener { downloaded ->
                if (downloaded) {
                    ready(model, onReady)
                } else {
                    onDownloading()
                    manager.download(model, DownloadConditions.Builder().build())
                        .addOnSuccessListener { ready(model, onReady) }
                        .addOnFailureListener { onFailed(it.message ?: "Download failed") }
                }
            }
            .addOnFailureListener { onFailed(it.message ?: "Model check failed") }
    }

    private fun ready(model: DigitalInkRecognitionModel, onReady: () -> Unit) {
        textRecognizer = DigitalInkRecognition.getClient(DigitalInkRecognizerOptions.builder(model).build())
        prepareShapes()
        onReady()
    }

    private fun prepareShapes() {
        val model = shapeModel ?: return
        manager.isModelDownloaded(model).addOnSuccessListener { downloaded ->
            if (downloaded) {
                shapeRecognizer = DigitalInkRecognition.getClient(DigitalInkRecognizerOptions.builder(model).build())
            } else {
                manager.download(model, DownloadConditions.Builder().build()).addOnSuccessListener {
                    shapeRecognizer = DigitalInkRecognition.getClient(DigitalInkRecognizerOptions.builder(model).build())
                }
            }
        }
    }

    fun recognize(ink: Ink, context: RecognitionContext, callback: (Result<RecognitionOutcome>) -> Unit) {
        val recognizer = textRecognizer
        if (recognizer == null) {
            callback(Result.failure(IllegalStateException("Handwriting model is not ready")))
            return
        }
        recognizer.recognize(ink, context)
            .addOnSuccessListener { result ->
                val texts = result.candidates.map { it.text }.filter { it.isNotBlank() }.distinct()
                val shapes = shapeRecognizer
                if (shapes == null) {
                    callback(Result.success(RecognitionOutcome(texts, false)))
                } else {
                    shapes.recognize(ink)
                        .addOnSuccessListener { shapeResult ->
                            val confident = InkJudge.isConfident(shapeResult.candidates.map { it.score })
                            callback(Result.success(RecognitionOutcome(texts, confident)))
                        }
                        .addOnFailureListener { callback(Result.success(RecognitionOutcome(texts, false))) }
                }
            }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    fun close() {
        textRecognizer?.close()
        shapeRecognizer?.close()
        textRecognizer = null
        shapeRecognizer = null
    }

    private fun buildModel(tag: String): DigitalInkRecognitionModel? = try {
        DigitalInkRecognitionModelIdentifier.fromLanguageTag(tag)?.let { DigitalInkRecognitionModel.builder(it).build() }
    } catch (e: MlKitException) {
        null
    }

    companion object {
        private const val SHAPES_TAG = "zxx-Zsym-x-shapes"
    }
}
