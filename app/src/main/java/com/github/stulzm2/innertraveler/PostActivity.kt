package com.github.stulzm2.innertraveler

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class PostActivity : BaseActivity() {
    // imports
    private var imageBtn: ImageButton? = null
    private var uri: Uri? = null
    private var textTitle: EditText? = null
    private var textDesc: EditText? = null
    private var postBtn: Button? = null
    private var storage: StorageReference? = null
    private val database: FirebaseDatabase? = null
    //    private var databaseRef: DatabaseReference? = null
    private var mAuth: FirebaseAuth? = null
    private var mDatabaseUsers: DatabaseReference? = null
    private var mCurrentUser: FirebaseUser? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "New Post"
        // initializing objects
        postBtn = findViewById(R.id.postBtn)
        textDesc = findViewById(R.id.textDesc)
        textTitle = findViewById(R.id.textTitle)
        storage = FirebaseStorage.getInstance().reference
//        databaseRef = database!!.getInstance().getReference().child("InnerTraveler")
        val databaseRef = FirebaseDatabase.getInstance().reference.child("InnerTraveler")

        mAuth = FirebaseAuth.getInstance()
        mCurrentUser = mAuth!!.currentUser
        mDatabaseUsers = FirebaseDatabase.getInstance().reference.child("Users").child(mCurrentUser!!.uid)
        imageBtn = findViewById(R.id.imageBtn)
        //picking image from gallery
        imageBtn!!.setOnClickListener {
            val galleryIntent = Intent(Intent.ACTION_GET_CONTENT)
            galleryIntent.type = "image/*"
            startActivityForResult(galleryIntent, GALLERY_REQUEST_CODE)
        }
        // posting to Firebase
        postBtn!!.setOnClickListener {
            Toast.makeText(this@PostActivity, "POSTING...", Toast.LENGTH_LONG).show()
            val PostTitle = textTitle!!.text.toString().trim { it <= ' ' }
            val PostDesc = textDesc!!.text.toString().trim { it <= ' ' }
            // do a check for empty fields
            if (!TextUtils.isEmpty(PostDesc) && !TextUtils.isEmpty(PostTitle)) {
                val filepath = storage!!.child("post_images").child(uri!!.lastPathSegment)
                filepath.putFile(uri!!).addOnSuccessListener { taskSnapshot ->
                    val downloadUrl = taskSnapshot.downloadUrl//getting the post image download url
                    Toast.makeText(applicationContext, "Successfully Uploaded", Toast.LENGTH_SHORT).show()
                    val newPost = databaseRef!!.push()
                    //adding post contents to database reference
                    mDatabaseUsers!!.addValueEventListener(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            newPost.child("title").setValue(PostTitle)
                            newPost.child("desc").setValue(PostDesc)
                            newPost.child("imageUrl").setValue(downloadUrl!!.toString())
                            newPost.child("uid").setValue(mCurrentUser!!.uid)
                            newPost.child("username").setValue(dataSnapshot.child("firstName").value)
                                    .addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            val intent = Intent(this@PostActivity, MainActivity::class.java)
                                            startActivity(intent)
                                        }
                                    }
                        }

                        override fun onCancelled(databaseError: DatabaseError) {

                        }
                    })
                }
            }
        }
    }

    override// image from gallery result
    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == GALLERY_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            uri = data.data
            imageBtn!!.setImageURI(uri)
        }
    }

    companion object {
        private val GALLERY_REQUEST_CODE = 2
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
        return super.onOptionsItemSelected(item)
    }
}
