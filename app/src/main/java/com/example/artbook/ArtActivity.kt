package com.example.artbook

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.artbook.databinding.ActivityArtBinding
import com.google.android.material.snackbar.Snackbar
import java.io.ByteArrayOutputStream

class ArtActivity : AppCompatActivity() {

    private lateinit var  binding : ActivityArtBinding
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent> // galeriye gitme yapısı
    private lateinit var permissionLauncher : ActivityResultLauncher<String> // izin alma yapısı
    var selectedBitmap : Bitmap?  = null
    private lateinit var database : SQLiteDatabase


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityArtBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        database = this.openOrCreateDatabase("Arts", MODE_PRIVATE,null)

        registerLauncher()

        val intent = intent
        val info = intent.getStringExtra("info")
        if(info.equals("new")){
            // burada yenı kayıt yapıyor options menuden geliyor
        }else{
            binding.button.visibility = View.INVISIBLE
            val selectedId = intent.getIntExtra("id",1)
            val cursor = database.rawQuery("SELECT * FROM arts WHERE id = ?", arrayOf(selectedId.toString()))

            val artNameIx = cursor.getColumnIndex("artname")
            val artistNameIx = cursor.getColumnIndex("artistname")
            val yearIx = cursor.getColumnIndex("year")
            val imageIX = cursor.getColumnIndex("image")

            while(cursor.moveToNext()){
                binding.artNameText.setText(cursor.getString(artNameIx))
                binding.artistNameText.setText(cursor.getString(artistNameIx))
                binding.yearText.setText(cursor.getString(yearIx))

                val byteArray = cursor.getBlob(imageIX)
                val bitmap = BitmapFactory.decodeByteArray(byteArray,0,byteArray.size)
                binding.imageView.setImageBitmap(bitmap)

            }
            cursor.close()
        }



    }

    fun saveButtonClicked(view: View){

        val artName = binding.artNameText.text.toString()
        val artistName = binding.artistNameText.text.toString()
        val year = binding.yearText.text.toString()

        if(selectedBitmap != null){
            val smallBitmap = makeSmallerBitmap(selectedBitmap!!,300)
            val outputStream = ByteArrayOutputStream()
            smallBitmap.compress(Bitmap.CompressFormat.PNG,50,outputStream)
            val byteArray = outputStream.toByteArray() // alınan resmin byte a cevrilmiş hali

            try {

                database.execSQL("CREATE TABLE IF NOT EXISTS arts (id INTEGER PRIMARY KEY, artname VARCHAR, artistname VARCHAR, year VARCHAR, image BLOB)")

                val sqlString = "INSERT INTO arts (artname, artistname, year, image) VALUES (?, ?, ?, ?)"
                val statement = database.compileStatement(sqlString)
                statement.bindString(1,artName)
                statement.bindString(2,artistName)
                statement.bindString(3,year)
                statement.bindBlob(4,byteArray)
                statement.execute()
            }catch (e:Exception){
                e.printStackTrace()
            }
            val intent = Intent(this@ArtActivity,MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
        }


    }


    private fun makeSmallerBitmap(image : Bitmap, maximumSize:Int) : Bitmap{

        var width = image.width
        var height = image.height

        val bitmapRatio : Double = width.toDouble() / height.toDouble()

        if(bitmapRatio > 1){
            // gorsel yatay demek
            width = maximumSize
            val scaleHeight = width / bitmapRatio
            height = scaleHeight.toInt()
        }else{
            // gorsel dikey demek
            height = maximumSize
            val scaleWidth = height * bitmapRatio
            width = scaleWidth.toInt()
        }
        return Bitmap.createScaledBitmap(image,width,height,true)
    }

    fun selectImage(view: View){

        if(ContextCompat.checkSelfPermission(this@ArtActivity,Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if(ActivityCompat.shouldShowRequestPermissionRationale(this@ArtActivity,Manifest.permission.READ_EXTERNAL_STORAGE)){
                Snackbar.make(view,"Galeriye gitmek için izin lazım",Snackbar.LENGTH_INDEFINITE).setAction("İzin Ver",View.OnClickListener {
                    // izin istenicek
                    permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }).show()
            }else{
                // izin istenicek
                permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }

        }else{
            val intentToGallery = Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            activityResultLauncher.launch(intentToGallery)
            // galeriye gidicek.
        }


    }

    private fun registerLauncher(){

        activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){result ->
            if(result.resultCode == RESULT_OK){
                val intentFromResult = result.data
                if(intentFromResult != null){
                    val imageData = intentFromResult.data
                    //binding.imageView.setImageURI(imageData)
                    if(imageData != null){
                        try {
                            if(Build.VERSION.SDK_INT >= 28){
                                val source = ImageDecoder.createSource(this@ArtActivity.contentResolver, imageData)
                                selectedBitmap = ImageDecoder.decodeBitmap(source)
                                binding.imageView.setImageBitmap(selectedBitmap)
                            }else{
                                selectedBitmap = MediaStore.Images.Media.getBitmap(this@ArtActivity.contentResolver,imageData)
                                binding.imageView.setImageBitmap(selectedBitmap)
                            }
                        }catch (e: Exception){
                            e.printStackTrace()
                        }
                    }
                }
            }
        }

        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()){ result ->
            if(result){
                // izin verildi
                val intentToGallery = Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intentToGallery)
            }else{
                // izin verilmedi
                Toast.makeText(this@ArtActivity,"İzin Gerekli!", Toast.LENGTH_SHORT).show()
            }
        }

    }


}