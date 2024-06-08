package com.msg.msgalaxy.fragments

import android.app.Activity
import android.app.appsearch.AppSearchResult.RESULT_OK
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.airbnb.lottie.LottieAnimationView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.msg.msgalaxy.EntryActivity
import com.msg.msgalaxy.users.LoginActivity
import com.msg.msgalaxy.R
import com.msg.msgalaxy.profileActivities.MVCMyFires.AdapterOfMyFires
import com.msg.msgalaxy.profileActivities.MVCMyFires.ModelOfMyFires
import com.msg.msgalaxy.profileActivities.MVCMyFires.MyFiresActivity
import com.msg.msgalaxy.profileActivities.MVCMyList.MyListActivity
import com.msg.msgalaxy.profileActivities.editProfile.EditProfileActivity
import de.hdodenhof.circleimageview.CircleImageView
import java.util.Collections

class ProfileFragment: Fragment() {

    private lateinit var view: View
    private lateinit var profileView: RelativeLayout
    private lateinit var loadingData: RelativeLayout
    private lateinit var loadingAnimation: LottieAnimationView
    private lateinit var noInternet: RelativeLayout
    private lateinit var retryButton: CardView

    private lateinit var userProfilePicture: String
    private lateinit var userName: String
    companion object {
        private const val EDIT_PROFILE_REQUEST_CODE = 1001
    }

    private var mAuth: FirebaseAuth? = null
    private var usernameTextView: TextView? = null
    private var profilePicImageView: CircleImageView? = null
    private lateinit var alertDialogBuilder : AlertDialog.Builder

    private var logOutBtn: CardView? = null
    private var myListBtn: CardView? = null
    private var myFiresBtn: CardView? = null
    private var suggestionsBtn: CardView? = null
    private var reportIssueBtn: CardView? = null
    private lateinit var editProfileBtn: CardView
    private lateinit var deleteAccount: CardView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            //Clear the Transparent of status bar
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        }

        view = inflater.inflate(R.layout.profile_fragment, container , false)
        profileView = view.findViewById(R.id.profileFragment_viewOfProfileRelativeLayoutId)
        loadingData = view.findViewById(R.id.profileFragment_lottieLoadingAnimation_relativeLayoutId)
        loadingAnimation = view.findViewById(R.id.profileFragment_loadingLottie)

        noInternet = view.findViewById(R.id.profileFragment_noInternet_relativeLayoutId)
        retryButton = view.findViewById(R.id.profileFragment_retryButtonId)

        mAuth = FirebaseAuth.getInstance()

        internetTestProcess()

        retryButton.setOnClickListener {
            onRetryButtonClick()
        }

        // The Total of my list items
        totalOfItems_inMyListProcess()

        logOutProcess()

        myListProcess()

        myFiresProcess()

        editProfileProcess()

        suggestionsForOurAppFunc()

        reportIssueWithOurAppFunc()

        deleteAccountProcess()

        return view
    }

    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        return networkCapabilities != null && (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) )
    }

    private fun internetTestProcess() {
        if (!isNetworkAvailable(requireContext())) {
            profileView.visibility = View.GONE
            loadingData.visibility = View.GONE
            noInternet.visibility = View.VISIBLE
            return
        }
        loadingData.visibility = View.VISIBLE
        noInternet.visibility = View.GONE

        // Attempt to get data again
        //get The Username and The ProfilePic from Firebase
        getUsername_pPic()
    }

    private fun onRetryButtonClick() {
        if (!isNetworkAvailable(requireContext())) {
            // If still no internet
            profileView.visibility = View.GONE
            loadingData.visibility = View.GONE
            noInternet.visibility = View.VISIBLE
            return
        }
        loadingData.visibility = View.VISIBLE
        noInternet.visibility = View.GONE

        // Attempt to get data again
        //get The Username and The ProfilePic from Firebase
        getUsername_pPic()
    }

    private fun totalOfItems_inMyListProcess() {
        var totalItemsTxtView: TextView = view.findViewById(R.id.profileFragment_myListBtn_totalItemsId)
        var totalItems = ""

        var reference: DatabaseReference = FirebaseDatabase.getInstance().reference.child("MyList").child(mAuth!!.currentUser!!.uid)
        reference.addValueEventListener(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                totalItems = snapshot.childrenCount.toString()
                if (totalItems.equals("0")) {
                    totalItemsTxtView.visibility = View.GONE
                }
                else {
                    totalItemsTxtView.text = totalItems
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun logOutProcess() {
        // Initialize the variables
        logOutBtn = view.findViewById(R.id.profileFragment_logOutBtnId)
        logOutBtn!!.setOnClickListener {

            alertDialogBuilder  = AlertDialog.Builder(requireContext())
            .setCancelable(false)
            .setTitle("LOG OUT")
            .setMessage("Are you sure you want to log out ?")
            .setPositiveButton("LOG OUT") { dialogInterface, i ->

                mAuth!!.signOut()

                var intent = Intent(activity, LoginActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(intent)
                activity?.finish()
                Toast.makeText(context, "You have successfully logged out.", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel") { dialogInterface, i ->
                dialogInterface.cancel()
            }

            var alertDialog : AlertDialog = alertDialogBuilder.create()

            //Change The Color of LOG OUT Btn
            alertDialog.setOnShowListener {
                var positiveButton: Button = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                positiveButton.setTextColor(Color.parseColor("#FF002E"))

                var negativeButton: Button = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                negativeButton.setTextColor(Color.parseColor("#F0F3F8"))
            }
            alertDialog.show()

            // for when the user log out he can again signup but he can't enter to mainAc until login
            clearLoginFlag()
        }
    }

    private fun clearLoginFlag() {
        val prefs: SharedPreferences = requireActivity().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val editor: SharedPreferences.Editor = prefs.edit()
        editor.remove("loginCompleted")
        editor.apply()
    }

    private fun getUsername_pPic() {

        //Initialize the variables
        usernameTextView = view.findViewById(R.id.profileFragment_usernameId)
        profilePicImageView = view.findViewById(R.id.profileFragment_profilePicId)

        //Set visibility to loading data Animation
        loadingData.visibility = View.VISIBLE
        profileView.visibility = View.GONE

        var reference: DatabaseReference = FirebaseDatabase.getInstance().reference.child("Users").child(mAuth!!.currentUser!!.uid)

        // Use ValueEventListener to get the value of the "username" child
        reference.child("username").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Get the value from the dataSnapshot
                userName = snapshot.getValue(String::class.java)!!
                usernameTextView!!.text = userName
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })

        reference.child("profilePic").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Get the value from the dataSnapshot
                userProfilePicture = snapshot.getValue(String::class.java)!!

                context?.let {
                    Glide.with(it).load(userProfilePicture).listener(object :
                        RequestListener<Drawable> {
                        override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>, isFirstResource: Boolean): Boolean {
                            return false
                        }
                        override fun onResourceReady(resource: Drawable, model: Any, target: Target<Drawable>?, dataSource: DataSource, isFirstResource: Boolean): Boolean {
                            // Image loaded successfully
                            profileView.visibility = View.VISIBLE
                            loadingData.visibility = View.GONE
                            loadingAnimation.visibility = View.GONE

                            return false
                        }
                    }).into(profilePicImageView!!)
                }

            }
            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        })
    }

    private fun myListProcess() {
        myListBtn = view.findViewById(R.id.profileFragment_myListBtnId)
        myListBtn!!.setOnClickListener {
            var intent = Intent(activity , MyListActivity::class.java)
            startActivity(intent)
        }
    }

    private fun myFiresProcess() {
        myFiresBtn = view.findViewById(R.id.profileFragment_myFiresBtnId)
        myFiresBtn!!.setOnClickListener {
            var intent = Intent(activity , MyFiresActivity::class.java)
            startActivity(intent)
        }
    }

    private fun editProfileProcess() {
        editProfileBtn = view.findViewById(R.id.profileFragment_editProfileBtnId)
        editProfileBtn.setOnClickListener {
            var intent = Intent(requireContext() , EditProfileActivity::class.java)
            intent.putExtra("username" , userName)
            intent.putExtra("profilePic" , userProfilePicture )
            startActivityForResult(intent, EDIT_PROFILE_REQUEST_CODE)
        }
    }

    // this function for the username that it will come from EditProfile
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == EDIT_PROFILE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // Check if the result includes the updated username and profilePic
            try {
                val updatedUsername = data?.getStringExtra("updatedUsername")
                val updatedProfilePic = data?.getStringExtra("updatedProfilePic")

                if (updatedUsername != null) {
                    // Update the UI with the new username
                    userName = updatedUsername
                    usernameTextView!!.text = userName
                    userName = updatedUsername
                }
                if (updatedProfilePic != null) {
                    // Update the UI with the new profilePic
                    loadingAnimation.visibility = View.VISIBLE
                    context?.let {
                        Glide.with(it).load(updatedProfilePic).listener(object : RequestListener<Drawable> {
                            override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>, isFirstResource: Boolean): Boolean {
                                return false
                            }
                            override fun onResourceReady(resource: Drawable, model: Any, target: Target<Drawable>?, dataSource: DataSource, isFirstResource: Boolean): Boolean {
                                // Image loaded successfully
                                loadingAnimation.visibility = View.GONE
                                return false
                            }
                        }).into(profilePicImageView!!)
                    }
                    userProfilePicture = updatedProfilePic
                }
            }
            catch (e: Exception) {}
        }
    }

    private fun reportIssueWithOurAppFunc() {
        reportIssueBtn = view.findViewById(R.id.profileFragment_reportIssueBtnId)
        reportIssueBtn!!.setOnClickListener {
            val recipientEmail = "msg.msgalaxy@gmail.com"
            val emailSubject = "MSG - Reporting Issue"
            val emailMessage = "Hello,\n\nI encountered the following issue with your application:\n\n"

            val intent = Intent(Intent.ACTION_SENDTO)
            intent.data = Uri.parse("mailto:")
            intent.setPackage("com.google.android.gm")
            // Add the email
            intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(recipientEmail))
            // Add the email subject
            intent.putExtra(Intent.EXTRA_SUBJECT, emailSubject)
            // Add the email body text
            intent.putExtra(Intent.EXTRA_TEXT, emailMessage)

            try {
                startActivity(intent)
            } catch (ex: ActivityNotFoundException) {
                // Handle the case where no email app is installed on the device.
            }
        }
    }

    private fun suggestionsForOurAppFunc() {
        // Suggestions For Our Application Function
        suggestionsBtn = view.findViewById(R.id.profileFragment_suggestionsBtnId)
        suggestionsBtn!!.setOnClickListener {

            val recipientEmail = "msg.msgalaxy@gmail.com"
            val emailSubject = "MSG - Suggestions"
            val emailMessage = "Hello,\n\nI have some suggestions for your application:\n\n"

            val intent = Intent(Intent.ACTION_SENDTO)
            intent.data = Uri.parse("mailto:")
            intent.setPackage("com.google.android.gm")
            // Add the email
            intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(recipientEmail))
            // Add the email subject
            intent.putExtra(Intent.EXTRA_SUBJECT, emailSubject)
            // Add the email body text
            intent.putExtra(Intent.EXTRA_TEXT, emailMessage)

            try {
                startActivity(intent)
            } catch (ex: ActivityNotFoundException) {
                // Handle the case where no email app is installed on the device.
            }
        }
    }

    private fun deleteAccountProcess() {
        deleteAccount = view.findViewById(R.id.profileFragment_deleteAccBtnId)
        deleteAccount.setOnClickListener {
            var builder: AlertDialog.Builder = AlertDialog.Builder(requireActivity())
            var view: View = requireActivity().layoutInflater.inflate(R.layout.custom_alert_dialog_of_delete_account , null)

            view.background = ContextCompat.getDrawable(requireContext() , R.drawable.background_of_alert_dialog)

            // Initialize the variables
            var profilePic: CircleImageView = view.findViewById(R.id.customAlertDialogOf_deleteAccount_profilePicId)
            var username: TextView = view.findViewById(R.id.customAlertDialogOf_deleteAccount_usernameId)
            var deleteEditText : TextInputLayout = view.findViewById(R.id.customAlertDialogOf_deleteAccount_textInputLayoutId)
            var checkBox1: CheckBox = view.findViewById(R.id.checkBox_firstId)
            var checkBox2: CheckBox = view.findViewById(R.id.checkBox_secondId)
            var deleteAccBtn: CardView = view.findViewById(R.id.customAlertDialogOf_deleteAccount_deleteAccButtonId)
            var cancelBtn: CardView = view.findViewById(R.id.customAlertDialogOf_deleteAccount_cancelButtonId)
            var deleteAccBtn_deleteTxt: TextView = view.findViewById(R.id.customAlertDialogOf_deleteAccount_deleteAccButtonId_textView)
            var deleteAccBtn_loadingAnimation: LottieAnimationView = view.findViewById(R.id.customAlertDialogOf_deleteAccount_deleteAccButtonId_loadingLottie)

            // Set the data to views
            username.text = userName
            Glide.with(requireActivity()).load(userProfilePicture).apply(RequestOptions.fitCenterTransform()).into(profilePic)

            builder.setView(view)
            var dialog: AlertDialog = builder.create()
            // Set the background of the entire dialog to transparent
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.show()

            // for deleting Account
            deleteAccBtn.setOnClickListener {
                var deleteWord: String = deleteEditText.editText!!.text.toString().trim()

                if(deleteWord.isEmpty() || !deleteWord.equals("delete")) {
                    Toast.makeText(activity, "Please type ' delete ' in the box to continue.", Toast.LENGTH_SHORT).show()
                }
                else if (!checkBox1.isChecked || !checkBox2.isChecked){
                    Toast.makeText(activity, "Please check both checkboxes to continue.", Toast.LENGTH_SHORT).show()
                }
                else {
                    // Here we will delete account
                    // Hide the keyboard
                    var inputMethodManager: InputMethodManager = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    inputMethodManager.hideSoftInputFromWindow(deleteAccBtn.getWindowToken(), 0)

                    deleteAllDataOfUserProcess(dialog , deleteAccBtn_deleteTxt , deleteAccBtn_loadingAnimation)
                }
            }
            // for Canceling
            cancelBtn.setOnClickListener {
                dialog.dismiss()
            }

        }
    }

    private fun deleteAllDataOfUserProcess(dialog: AlertDialog , deleteAccBtn_deleteTxt: TextView , deleteAccBtn_loadingAnimation: LottieAnimationView) {
        // Set visibilities
        deleteAccBtn_deleteTxt.visibility = View.GONE
        deleteAccBtn_loadingAnimation.visibility = View.VISIBLE

        // for deleting email ... from authentication in Firebase
        mAuth!!.currentUser!!.delete().addOnCompleteListener {task->
            if (task.isSuccessful) {
                // Set visibilities
                deleteAccBtn_deleteTxt.visibility = View.VISIBLE
                deleteAccBtn_loadingAnimation.visibility = View.GONE

                Toast.makeText(requireActivity(), "You have successfully deleted your account!", Toast.LENGTH_SHORT).show()
                var intent = Intent(requireActivity() , EntryActivity::class.java)
                requireActivity().startActivity(intent)
                requireActivity().finish()
            }
        }

        // delete all other things
        var userReference: DatabaseReference = FirebaseDatabase.getInstance().reference.child("Users")
        userReference.addListenerForSingleValueEvent(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    snapshot.child(mAuth!!.currentUser!!.uid).ref.removeValue()
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
        deleteFiresDislikesHalvesOfCurrentUser()
        deleteListOfCurrentUser()
        deleteCommentsOfCurrentUser()
        deleteRepliesOfCurrentUser()
        deleteCommentsReactionsOfCurrentUser()
        deleteProfilePicFromStorage()

    }

    private fun deleteFiresDislikesHalvesOfCurrentUser() {
        var referenceFires = FirebaseDatabase.getInstance().reference.child("Fires")
        referenceFires.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (dataSnapshot: DataSnapshot in snapshot.children) {
                        dataSnapshot.child(mAuth!!.currentUser!!.uid).ref.removeValue()
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
        var referenceDislikes = FirebaseDatabase.getInstance().reference.child("Dislikes")
        referenceDislikes.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (dataSnapshot: DataSnapshot in snapshot.children) {
                        dataSnapshot.child(mAuth!!.currentUser!!.uid).ref.removeValue()
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
        var referenceHalves = FirebaseDatabase.getInstance().reference.child("Halves")
        referenceHalves.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (dataSnapshot: DataSnapshot in snapshot.children) {
                        dataSnapshot.child(mAuth!!.currentUser!!.uid).ref.removeValue()
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun deleteListOfCurrentUser() {
        var referenceList = FirebaseDatabase.getInstance().reference.child("MyList")
        referenceList.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    snapshot.child(mAuth!!.currentUser!!.uid).ref.removeValue()
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun deleteCommentsOfCurrentUser() {
        var referenceComments = FirebaseDatabase.getInstance().reference.child("Comments")
        referenceComments.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (dataSnapshot: DataSnapshot in snapshot.children) {
                        dataSnapshot.child(mAuth!!.currentUser!!.uid).ref.removeValue()
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun deleteRepliesOfCurrentUser() {
        var referenceReplies = FirebaseDatabase.getInstance().reference.child("CommentReplies")
        referenceReplies.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                   for (nameSnapshot: DataSnapshot in snapshot.children) { // Names
                       for (commentSnapshot: DataSnapshot in nameSnapshot.children) { // Main CommentId
                           for (replySnapshot: DataSnapshot in commentSnapshot.children) { // Reply CommentId
                               replySnapshot.child(mAuth!!.currentUser!!.uid).ref.removeValue()
                           }
                       }
                   }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun deleteProfilePicFromStorage() {
        // Reference to the Firebase Storage
        var storageReference: StorageReference = FirebaseStorage.getInstance().reference.child("UserProfilePicture").child(mAuth!!.currentUser!!.uid)

        // List all items (files) in the "UserProfilePicture" folder
        storageReference.listAll()
            .addOnSuccessListener { result ->
                // Iterate through each item and delete it
                result.items.forEach { fileReference ->
                    fileReference.delete()
                }
            }
    }

    private fun deleteCommentsReactionsOfCurrentUser() {
        var reference = FirebaseDatabase.getInstance().reference.child("CommentsReactions")
        reference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (dataSnapshot: DataSnapshot in snapshot.children) { // Names
                        for (commentSnapshot: DataSnapshot in dataSnapshot.children) { // Comments
                            for(likesSnapshot: DataSnapshot in commentSnapshot.children) { // Likes or Loves or Laughs or Angry
                                likesSnapshot.child(mAuth!!.currentUser!!.uid).ref.removeValue()
                            }
                        }
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }
}