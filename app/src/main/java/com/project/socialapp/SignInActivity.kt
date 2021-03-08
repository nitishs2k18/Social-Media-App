package com.project.socialapp

import android.content.Intent
import android.nfc.Tag
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthCredential
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.project.socialapp.daos.UserDao
import com.project.socialapp.models.User
import kotlinx.android.synthetic.main.activity_sign_in2.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class SignInActivity : AppCompatActivity() {
    private val RC_SIGN_IN: Int = 123
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var auth: FirebaseAuth
    private val TAG= "SignInActivity Tag"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in2)

        // Configure Google Sign In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        auth = Firebase.auth
        signInButton.setOnClickListener{
            signIn()
        }
    }

    override fun onStart() {
        super.onStart()
        val currentUser =auth.currentUser
        updateUI(currentUser)
    }
    //We can see whole email id's through which we can login into it.
        private fun signIn() {
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>?) {
      try {
          val account =
                  completedTask?.getResult(ApiException::class.java)!!
          Log.d(TAG,"fireBaseAuthWithGoogle:" + account.id)
          firebaseAuthWithGoogle(account.idToken!!)
      } catch(e: ApiException){
          Log.w(TAG,"signInResult:failed code="+ e.statusCode)
      }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
val credential = GoogleAuthProvider.getCredential(idToken,null)

//             Yeh coroutine vala sara kaam background vale thread pe hoga!!
       signInButton.visibility= View.GONE
        progressBar.visibility=View.VISIBLE
        GlobalScope.launch(Dispatchers.IO){
            val auth= auth.signInWithCredential(credential).await()
            val firebaseUser= auth.user
            withContext(Dispatchers.Main){
                updateUI(firebaseUser)
            }
        }
    }

    private fun updateUI(firebaseUser: FirebaseUser?) {
        if(firebaseUser!=null) {
            val user= User(firebaseUser.uid, firebaseUser.displayName, firebaseUser.photoUrl.toString())
            val usersDao= UserDao()
            usersDao.addUser(user)

            val mainActivity = Intent(this, MainActivity::class.java)
            startActivity(mainActivity)
            finish()
        }
        else
        {
                signInButton.visibility=View.VISIBLE
                progressBar.visibility=View.GONE
        }

    }
}