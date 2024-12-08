package xyz.fkr.speechtoclipboard

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.ImageViewCompat
import com.google.android.material.snackbar.Snackbar
import xyz.fkr.speechtoclipboard.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var view: View
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        view = binding.root

        setContentView(view)
        enableEdgeToEdge()

        requestPermissionLauncher =
            registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (!isGranted) {
                    checkAppPermission()
                } else {
                    println("app has permission")
                }
            }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainLayout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun onStart() {
        super.onStart()
        checkAppPermission()
    }


    fun recordAudio(view: View) {

        checkAppPermission()

        val colorStateList = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_enabled)),
            intArrayOf(Color.GREEN)
        )
        ImageViewCompat.setImageTintList(binding.buttonRecordAudio, colorStateList)
        binding.buttonRecordAudio.isClickable = false

        try {
            val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this@MainActivity)

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )

            speechRecognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    print("Ready for speech")
                }

                override fun onBeginningOfSpeech() {
                    Toast.makeText(this@MainActivity, "Beginning of speech", Toast.LENGTH_SHORT).show()
                }

                override fun onRmsChanged(rmsdB: Float) {
                    println("Rms changed")
                }

                override fun onBufferReceived(buffer: ByteArray?) {
                    println("Buffer received")
                }

                override fun onEndOfSpeech() {
                    binding.buttonCopySpeechText.visibility = View.VISIBLE
                    cancelRecord()
                    binding.buttonCopySpeechText.visibility = View.VISIBLE
                }

                override fun onError(error: Int) {
                    Snackbar.make(view, "Nothing captured", Snackbar.LENGTH_INDEFINITE).setAction("Again") { recordAudio(view) }.show()
                    cancelRecord()
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val spokenText = matches[0]
                        binding.textViewSpeech.setText(spokenText)
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    println("Partial results")
                }

                override fun onEvent(eventType: Int, params: Bundle?) {
                    println("Event")
                }

            })
            speechRecognizer.startListening(intent)
        } catch (e: Exception){
            e.printStackTrace()
        }

    }

    fun copyText(view: View) {
        binding.buttonCopySpeechText.setColorFilter(Color.GREEN)
        binding.textViewSpeech.setTextColor(Color.GREEN)
        val textToCopy = binding.textViewSpeech.text.toString()
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Label", textToCopy)
        clipboard.setPrimaryClip(clip)
    }

    fun cancelRecord() {
        binding.buttonRecordAudio.isClickable = true

        val colorStateList = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_enabled)),
            intArrayOf(Color.WHITE)
        )

        ImageViewCompat.setImageTintList(binding.buttonRecordAudio, colorStateList)
        Toast.makeText(this, "Recording is over", Toast.LENGTH_SHORT).show()

    }

    private fun checkAppPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this@MainActivity,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {

            }

            ActivityCompat.shouldShowRequestPermissionRationale(
                this, Manifest.permission.RECORD_AUDIO
            ) -> {
                Snackbar.make(view, "Permission needed", Snackbar.LENGTH_INDEFINITE).setAction(
                    "GIVE"
                ) {
                    requestPermissionLauncher.launch(
                        Manifest.permission.RECORD_AUDIO
                    )
                }.show()
            }

            else -> {
                requestPermissionLauncher.launch(
                    Manifest.permission.RECORD_AUDIO
                )
            }
        }
    }
}