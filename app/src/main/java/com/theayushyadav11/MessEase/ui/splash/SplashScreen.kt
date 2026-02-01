package com.theayushyadav11.MessEase.ui.splash

import android.content.Intent
import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import com.theayushyadav11.MessEase.MainActivity
import com.theayushyadav11.MessEase.Models.Menu
import com.theayushyadav11.MessEase.R
import com.theayushyadav11.MessEase.RoomDatabase.MenuDataBase.MenuDatabase
import com.theayushyadav11.MessEase.ui.more.ErrorActivity
import com.theayushyadav11.MessEase.ui.more.UpdateActivity
import com.theayushyadav11.MessEase.ui.splash.fragments.LoginAndSignUpActivity
import com.theayushyadav11.MessEase.utils.Constants.Companion.MAIN_MENU
import com.theayushyadav11.MessEase.utils.Constants.Companion.MENU
import com.theayushyadav11.MessEase.utils.Constants.Companion.fireBase
import com.theayushyadav11.MessEase.utils.Constants.Companion.firestoreReference
import com.theayushyadav11.MessEase.utils.Mess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SplashScreen : AppCompatActivity() {

    private lateinit var mess: Mess

    override fun onCreate(savedInstanceState: Bundle?) {
        initialise()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash_screen)


        val imageView = findViewById<ImageView>(R.id.imageViewLogo)
        val fadeAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_out)
        imageView.startAnimation(fadeAnimation)
        lifecycleScope.launch {
            checkIfPresent {
                if (it)
                    runScripts()
            }
            delay(1500)
            if (isFirstTime()) {
                getUpdate {
                    setMainMenu {
                        navigate()
                    }
                }

            } else getUpdate {navigate()}

        }

    }



    fun initialise() {
        mess = Mess(this)
        mess.setTheme()
    }

    private fun getUpdate(onResult: () -> Unit) {
        firestoreReference.collection("Update").document("update")
            .get()
            .addOnSuccessListener { value ->
                val version = value?.getString("version")
                val url = value?.getString("url")
                if (version != null && url != null) {
                    mess.setUpdate(version, url)
                } else {
                    // If document doesn't exist, use current app version as default
                    val versionName = packageManager.getPackageInfo(packageName, 0).versionName ?: "1.2"
                    mess.setUpdate(versionName, "")
                }
                onResult()
            }
            .addOnFailureListener { error ->
                // On failure (no internet, Firebase not configured), use current app version
                val versionName = packageManager.getPackageInfo(packageName, 0).versionName ?: "1.2"
                mess.setUpdate(versionName, "")
                mess.log("Firebase Update fetch failed: ${error.message}")
                onResult()
            }

    }

    private fun navigate() {
        val versionName = packageManager.getPackageInfo(packageName, 0).versionName?:""
        mess.getUpdates { version, _ ->
            mess.log(version)
            mess.log(versionName)
            if (version == "") {

                startActivity(Intent(this, ErrorActivity::class.java))
                finish()
            } else if (version != versionName) {
                mess.log(version)
                mess.log(versionName)
                startActivity(Intent(this, UpdateActivity::class.java))
                finish()

            } else if (mess.isLoggedIn()) {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else {
                startActivity(Intent(this, LoginAndSignUpActivity::class.java))
                finish()
            }


        }

    }

    private fun isFirstTime(): Boolean {
        if (mess.get("firstTime") == "") {
            mess.save("firstTime", "false")
            return true
        } else return false

    }

    private fun setMainMenu(onResult: () -> Unit) {
        firestoreReference.collection("MainMenu").document("menu")
            .get()
            .addOnSuccessListener { value ->
                if (value != null && value.exists()) {
                    try {
                        val menu = value.toObject(Menu::class.java)
                        if (menu != null) {
                            lifecycleScope.launch(Dispatchers.IO) {
                                val menuDatabase = MenuDatabase.getDatabase(this@SplashScreen).menuDao()
                                val newMenu = Menu(
                                    id = 0, creator = menu.creator, menu = menu.menu
                                )
                                menuDatabase.addMenu(newMenu)
                                withContext(Dispatchers.Main) {
                                    onResult()
                                }
                            }
                        } else {
                            mess.log("Menu object is null")
                            onResult()
                        }
                    } catch (e: Exception) {
                        mess.log("Error parsing menu: ${e.message}")
                        onResult()
                    }
                } else {
                    mess.log("Menu document doesn't exist")
                    onResult()
                }
            }
            .addOnFailureListener { error ->
                mess.log("Failed to fetch menu: ${error.message}")
                onResult()
            }

    }

    fun runScripts() {
        fireBase.runScripts(mess.getUser())
    }

    fun checkIfPresent(isPresent: (Boolean) -> Unit) {
        firestoreReference.collection(MAIN_MENU).document(MENU)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists())
                    isPresent(false)
                else isPresent(true)
            }
            .addOnFailureListener {
                isPresent(false)
            }
    }

}
