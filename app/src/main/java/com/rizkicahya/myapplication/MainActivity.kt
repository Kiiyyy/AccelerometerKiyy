package com.rizkicahya.myapplication

import android.content.Intent
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import com.rizkicahya.myapplication.databinding.ActivityMainBinding
import com.rizkicahya.myapplication.model.InternalFileRepository
import com.rizkicahya.myapplication.model.Note
import com.rizkicahya.myapplication.model.NoteRepository
import org.json.JSONObject
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity(), SensorEventListener {
    private val repo: NoteRepository by lazy { InternalFileRepository(this) }
    private lateinit var sensorManager: SensorManager
    private lateinit var square: TextView
    private lateinit var binding: ActivityMainBinding
    val CITY : String = "Sleman"
    val API: String = "0847c977a92cace81de446eebce654aa"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        class weatherTask() : AsyncTask<String, Void, String>() {
            //untuk menginisialisasi dan mengatur UI dasar dari thread utama.
            override fun onPreExecute() {
                super.onPreExecute()
                /*Menampilkan ProgressBar, Membuat desain utama  */

            }
            //mendeklarasikan jenis variabel nullable dari api open weathermap pada latar belakang
            override fun doInBackground(vararg params: String?): String? {
                var response:String?
                try{
                    response = URL("https://api.openweathermap.org/data/2.5/weather?q=$CITY&units=metric&appid=$API").readText(
                        Charsets.UTF_8
                    )
                }catch (e: Exception){
                    response = null
                }
                return response
            }
            //menambahkan function OnPostExecute
            //metode terakhir dari AsyncTask yang menjalankan setelah penyelesaian tugas dan berjalan di thread UI.
            override fun onPostExecute(result: String?) {
                super.onPostExecute(result)
                try {
                    /* Mengekstrak pengembalian JSON dari API */
                    //membuat variabel yang dibutuhkan pada JSON pada API openweathermaps
                    val jsonObj = JSONObject(result)
                    val main = jsonObj.getJSONObject("main")
                    val weather = jsonObj.getJSONArray("weather").getJSONObject(0)
                    val temp = main.getString("temp")+"°C"
                    val weatherDescription = weather.getString("description")
                    findViewById<TextView>(R.id.weather).text = weatherDescription.capitalize()
                    findViewById<TextView>(R.id.temp).text = temp
                } catch (e: Exception) {
                }
            }
        }
        binding.cuaca.setOnClickListener {
            weatherTask().execute()
        }

        // Menjaga ponsel dalam mode terang
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        square = findViewById(R.id.tv_square)
        setUpSensorStuff()
        //memasukan data sensor gps ke file
        binding.log.setOnClickListener {
            var logDataSensor = binding.editTeksCatatan.text.toString()
            val timeStamp: String = SimpleDateFormat("yy-MM-dd").format(Date())
            binding.editFileName.setText("Kiyy-"+timeStamp+".txt")
            val logData1 = square.text.toString()
            logDataSensor = "$logDataSensor$logData1 ,"
            binding.editTeksCatatan.setText(logDataSensor)
        }

        //menambahkan tombol tulis
        //untuk menyimpan file
        binding.save.setOnClickListener {
            if (binding.editFileName.text.isNotEmpty()) {
                try {
                    repo.addNote(
                        Note(
                            binding.editFileName.text.toString(),
                            binding.editTeksCatatan.text.toString()
                        )
                    )
                } catch (e: Exception) {
                    Toast.makeText(this, "File Write Failed", Toast.LENGTH_LONG).show()
                    e.printStackTrace()
                }
                binding.editFileName.text.clear()
                binding.editTeksCatatan.text.clear()
            } else {
                Toast.makeText(this, "Please provide a Filename", Toast.LENGTH_LONG).show()
            }
        }

        //untuk membuka file
        binding.Read.setOnClickListener {
            if (binding.editFileName.text.isNotEmpty()) {
                try {
                    val note = repo.getNote(binding.editFileName.text.toString())
                    binding.editTeksCatatan.setText(note.noteText)
                } catch (e: Exception) {
                    Toast.makeText(this, "File Read Failed", Toast.LENGTH_LONG).show()
                    e.printStackTrace()
                }
            } else {
                Toast.makeText(this, "Please provide a Filename", Toast.LENGTH_LONG).show()
            }
        }
        //menghapus file beserta isi
        binding.Delete.setOnClickListener {
            if (binding.editFileName.text.isNotEmpty()) {
                try {
                    if (repo.deleteNote(binding.editFileName.text.toString())) {
                        Toast.makeText(this, "File Deleted", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this, "File Could Not Be Deleted", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "File Delete Failed", Toast.LENGTH_LONG).show()
                    e.printStackTrace()
                }
                binding.editFileName.text.clear()
                binding.editTeksCatatan.text.clear()
            } else {
                Toast.makeText(this, "Please provide a Filename", Toast.LENGTH_LONG).show()
            }
        }

        //membagikan data dengan intent
        binding.share.setOnClickListener {
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "text/plain"
            val logData1 = binding.editTeksCatatan.text.toString()
            intent.putExtra(Intent.EXTRA_TEXT, logData1)
            intent.putExtra(Intent.EXTRA_SUBJECT, "Subject Here")
            val chooser = Intent.createChooser(intent, "Bagikan Dengan : ")
            startActivity(chooser)
        }
    }
    private fun setUpSensorStuff() {
        // Membuat sensor manager
        sensorManager = getSystemService(AppCompatActivity.SENSOR_SERVICE) as SensorManager

        // Menentukan sensor yang digunakan sebagai listener
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also { accelerometer ->
            sensorManager.registerListener(
                this,
                accelerometer,
                SensorManager.SENSOR_DELAY_FASTEST,
                SensorManager.SENSOR_DELAY_FASTEST
            )
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        // Cek sensor yang sudah di daftarkan
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            // Sisi = Memiringkan ponsel ke kiri (10) dan kanan (-10)
            val sides = event.values[0]
            // Atas/Bawah = Memiringkan ponsel ke atas (10), datar (0), terbalik (-10)
            val upDown = event.values[1]

            square.apply {
                rotationX = upDown * 3f
                rotationY = sides * 3f
                rotation = -sides
                translationX = sides * -10
                translationY = upDown * 10
            }
            // Mengubah warna persegi jika benar-benar rata menjadi hijau dan jika tidak rata menjadi merah
            val color = if (upDown.toInt() == 0 && sides.toInt() == 0) Color.GREEN else Color.RED
            square.setBackgroundColor(color)

            square.text = "Atas/Bawah ${upDown.toInt()}\nKiri/Kanan ${sides.toInt()}"
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        return
    }

    override fun onDestroy() {
        sensorManager.unregisterListener(this)
        super.onDestroy()
    }
}