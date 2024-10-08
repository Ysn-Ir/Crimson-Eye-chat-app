package com.example.firebase

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.example.firebase.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var usersRef: CollectionReference

    private lateinit var getResult: ActivityResultLauncher<Intent>
    private val MEDIA_IMAGES_REQUEST_CODE = 23432
    private lateinit var uri: Uri

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        usersRef = db.collection("users_collection")

        // Initialize ActivityResultLauncher for image picking
        getResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val data: Intent? = result.data
                val selectedImageUri = data?.data
                if (selectedImageUri != null) {
                    binding.profileImage.setImageURI(selectedImageUri)
                    uri = selectedImageUri
                }
            } else {
                Toast.makeText(this, "Image picking cancelled", Toast.LENGTH_SHORT).show()
            }
        }

        // Check Firebase Auth state
        if (auth.currentUser != null) {
            navigateToChatList()
        }

        binding.btnSignIn.setOnClickListener {
            signIn()
        }

        binding.btnToRegister.setOnClickListener {
            showRegisterView()
        }
        binding.btnRegister.setOnClickListener {
            register()
        }
        binding.btnToSignIn.setOnClickListener {
            showSignInView()
        }

        binding.goToProfile.setOnClickListener {
            showPicturePickerView()
        }

        binding.goToSignUp.setOnClickListener {
            showRegisterView()
        }

        binding.profileImage.setOnClickListener {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_MEDIA_IMAGES
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermission()
            } else {
                getImage()
            }
        }
    }

    private fun showSignInView() {
        binding.viewFlipper.displayedChild = 0
    }

    private fun showRegisterView() {
        binding.viewFlipper.displayedChild = 1
    }

    private fun showPicturePickerView() {
        binding.viewFlipper.displayedChild = 2
    }

    private fun signIn() {
        val email = binding.signInUsername.text.toString()
        val password = binding.signInPassword.text.toString()
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "You should enter email and password", Toast.LENGTH_LONG).show()
            return
        }
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "User signed in", Toast.LENGTH_LONG).show()
                    navigateToChatList()
                } else {
                    Toast.makeText(this, "There is a mistake: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun register() {
        val username = binding.registerUsername.text.toString()
        val email = binding.registerEmail.text.toString()
        val password = binding.registerPassword.text.toString()
        if (email.isEmpty() || password.isEmpty() || username.isEmpty()) {
            Toast.makeText(this, "You should enter username, email, and password", Toast.LENGTH_LONG).show()
            return
        }
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val currentUser = auth.currentUser
                    if (currentUser != null) {
                        saveUserToFirestore(username)
                        Toast.makeText(this, "Account created successfully", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this, "Failed to retrieve current user", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }

    }
    private fun saveUserToFirestore(username: String) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            if (this::uri.isInitialized) {
                val filePath = FirebaseStorage.getInstance().reference.child("profile_images").child(uri.lastPathSegment!!)
                filePath.putFile(uri).addOnSuccessListener { task ->
                    task.metadata?.reference?.downloadUrl?.addOnSuccessListener { downloadUri ->
                        val user = User(currentUser.uid, username, downloadUri.toString())
                        usersRef.document(currentUser.uid).set(user)
                            .addOnSuccessListener {
                                Toast.makeText(this, "User saved to Firestore", Toast.LENGTH_LONG).show()
                            }
                            .addOnFailureListener { exception ->
                                Toast.makeText(this, "Failed to save user: ${exception.message}", Toast.LENGTH_LONG).show()
                            }
                    }
                }.addOnFailureListener { exception ->
                    Toast.makeText(this, "Failed to upload image: ${exception.message}", Toast.LENGTH_LONG).show()
                }
            } else {
                // If no image is selected, create user without profile image
                val user = User(currentUser.uid, username, "@drawable/profile")  // replace "default_image_url" with a default image URL or handle it accordingly
                usersRef.document(currentUser.uid).set(user)
                    .addOnSuccessListener {
                        Toast.makeText(this, "User saved to Firestore", Toast.LENGTH_LONG).show()
                    }
                    .addOnFailureListener { exception ->
                        Toast.makeText(this, "Failed to save user: ${exception.message}", Toast.LENGTH_LONG).show()
                    }
            }
        } else {
            Toast.makeText(this, "Current user is null", Toast.LENGTH_LONG).show()
        }
    }

    private fun navigateToChatList() {
        val intent = Intent(this, SearchActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun getImage() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        getResult.launch(intent)
    }

    private fun requestPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.READ_MEDIA_IMAGES
            )
        ) {
            AlertDialog.Builder(this).setPositiveButton("YES") { _, _ ->
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_MEDIA_IMAGES),
                    MEDIA_IMAGES_REQUEST_CODE
                )
            }.setNegativeButton("NO") { dialog, _ -> dialog.cancel() }.setTitle("Permission Needed").setMessage(
                "This permission is needed to access internal storage and import your profile picture"
            ).show()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_MEDIA_IMAGES),
                MEDIA_IMAGES_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == MEDIA_IMAGES_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getImage()
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_LONG).show()
        }
    }
}
