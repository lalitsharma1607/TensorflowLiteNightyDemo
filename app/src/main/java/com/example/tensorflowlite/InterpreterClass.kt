package com.example.tensorflowlite

import android.content.res.AssetManager
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import kotlinx.android.synthetic.main.content_main.*
import org.json.JSONException
import org.json.JSONObject
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.nio.charset.Charset
import java.util.*

class InterpreterClass : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        btnSubmit.setOnClickListener {
            findAnswer()
        }

    }

    private fun findAnswer() {
        val classifier = Classifier(this, "word_dict.json")
        classifier.setMaxLength(1000)
        classifier.setCallback(object : Classifier.DataCallback {
            override fun onDataProcessed(result: HashMap<String, Int>?) {
                val message = editText.text.toString()
                if (!TextUtils.isEmpty(message)) {
                    classifier.setVocab(result)
                    val tokenizedMessage = classifier.tokenize(message)
                    val paddedMessage = classifier.padSequence(tokenizedMessage)
                    val results = classifySequence(paddedMessage)
                    val prediction = results.indexOf(1.0f)
                    if (prediction != -1) {
                        showResultFromJson(prediction)
                    } else {
                        Toast.makeText(this@InterpreterClass, "No result found", Toast.LENGTH_LONG).show()
                    }
                } else
                    showMessage("Please enter something")
            }
        })

        classifier.loadData()
    }

    private fun showResultFromJson(arrayIndex: Int?) {
        try {
            val obj = JSONObject(loadJSONFromAsset())
            var result: String

            obj.keys().forEach {
                val value = obj.get(it)
                if (value == arrayIndex) {
                    result = it
                    Log.d(LOG_TAG, result)
                    showMessage("Result found : $result")
                }
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }

    }

    private fun loadJSONFromAsset(): String? {
        var json: String? = null
        try {
            val inputStream = assets.open("labels.json")
            val size = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            json = String(buffer, Charset.defaultCharset())
        } catch (ex: IOException) {
            ex.printStackTrace()
            return null
        }

        return json
    }

    private fun classifySequence(sequence: IntArray): FloatArray {

        val inputs: Array<FloatArray> = arrayOf(sequence.map { it.toFloat() }.toFloatArray())
        val outputs: Array<FloatArray> =
            arrayOf(floatArrayOf(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f))

        try {
            val interpreter = Interpreter(loadModelFile(assets, "converted_model.tflite"))
            //           read out[0][0] //array index
            interpreter.run(inputs, outputs)
            Log.d(LOG_TAG, outputs.toString())

        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return outputs[0]
    }

    companion object {
        private val LOG_TAG = "TF LOG"


        /**
         * Memory-map the model file in Assets.
         */
        @Throws(IOException::class)
        private fun loadModelFile(assets: AssetManager, modelFilename: String): MappedByteBuffer {
            val fileDescriptor = assets.openFd(modelFilename)
            val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
            val fileChannel = inputStream.channel
            val startOffset = fileDescriptor.startOffset
            val declaredLength = fileDescriptor.declaredLength
            return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
        }
    }

    private fun showMessage(msg: String) {
        Toast.makeText(this@InterpreterClass, msg, Toast.LENGTH_LONG).show()
    }

}
