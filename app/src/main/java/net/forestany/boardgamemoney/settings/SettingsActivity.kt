package net.forestany.boardgamemoney.settings

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import net.forestany.boardgamemoney.R
import net.forestany.boardgamemoney.Util.errorSnackbar
import net.forestany.boardgamemoney.Util.notifySnackbar

class SettingsActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "SettingsActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // default settings
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment(this, findViewById(android.R.id.content), null))
                .commit()
        }

        // deactivate standard back button
        onBackPressedDispatcher.addCallback(
            this,
            object : androidx.activity.OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    /* execute anything, e.g. finish() - if nothing is here, nothing happens pushing main back button */
                    finish()
                }
            }
        )

        Log.v(TAG, "onCreate $TAG")
    }

    class SettingsFragment(private val context: Context, private val view: View, private val anchorView: View?) : PreferenceFragmentCompat(), OnSharedPreferenceChangeListener {
        private val sharedPreferencesHistory = mutableMapOf<String, Any?>()

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            // store all shared preferences key values in a map as history
            PreferenceManager.getDefaultSharedPreferences(context).all.forEach {
                sharedPreferencesHistory[it.key] = it.value
                //Log.v(TAG, "${it.key} -> ${it.value.toString()}")
            }
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
        }

        override fun onResume() {
            super.onResume()
            preferenceScreen.sharedPreferences?.registerOnSharedPreferenceChangeListener(this)
        }

        override fun onPause() {
            super.onPause()
            preferenceScreen.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(this)
        }

        override fun onPreferenceTreeClick(preference: Preference): Boolean {
            return when (preference.key) {
                "tcp_common_passphrase" -> {
                    PasswordDialogFragment.newInstance(
                        prefKey = "tcp_common_passphrase",
                        title = getString(R.string.settings_tcp_common_passphrase)
                    ).show(parentFragmentManager, "password_dialog")
                    true
                }
                else -> super.onPreferenceTreeClick(preference)
            }
        }

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
            if (sharedPreferences == null || key == null) return

            var wrongValue = false
            var exceptionMessage = "Wrong value for settings."
            val value: Any? = sharedPreferences.all[key]

            // do nothing if current value is equal to history value
            if (this.sharedPreferencesHistory[key].toString().contentEquals(value.toString())) return

            //Log.v(TAG, "old $key -> ${this.sharedPreferencesHistory[key].toString()} < - - - > ${value.toString()}")

            // check values
            if (key.contentEquals("general_locale")) {
                // setting is 'de' or 'en'
                if ((value.toString().contentEquals("de")) || (value.toString().contentEquals("en"))) {
                    // restart app with new language setting after 1 second
                    notifySnackbar(message = getString(R.string.settings_language_changed, 5), view = view, anchorView = anchorView)

                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                        intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                        requireActivity().finish()
                        Runtime.getRuntime().exit(0)
                    }, 5000)
                }
            } else if (key.contentEquals("general_currency_text")) {
                // not empty, max. 3 characters long
                if (value.toString().isEmpty()) {
                    exceptionMessage = getString(R.string.settings_value_empty)
                    wrongValue = true
                } else if (value.toString().length > 3) {
                    exceptionMessage = getString(R.string.settings_value_length_to_high, 3)
                    wrongValue = true
                }
            } else if (key.contentEquals("udp_network_interface_name")) {
                // not empty, at least 4 characters long
                if (value.toString().isEmpty()) {
                    exceptionMessage = getString(R.string.settings_value_empty)
                    wrongValue = true
                } else if (value.toString().length < 4) {
                    exceptionMessage = getString(R.string.settings_value_length_to_low, 4)
                    wrongValue = true
                } else if (value.toString().length > 35) {
                    exceptionMessage = getString(R.string.settings_value_length_to_high, 35)
                    wrongValue = true
                }
            } else if (key.contentEquals("udp_multicast_ip")) {
                // not empty, at least 8 characters long
                if (value.toString().isEmpty()) {
                    exceptionMessage = getString(R.string.settings_value_empty)
                    wrongValue = true
                } else if (value.toString().length < 8) {
                    exceptionMessage = getString(R.string.settings_value_length_to_low, 8)
                    wrongValue = true
                }
            } else if (key.contentEquals("udp_multicast_port")) {
                // not empty, isInt, between 1 and 65535
                if (value.toString().isEmpty()) {
                    exceptionMessage = getString(R.string.settings_value_empty)
                    wrongValue = true
                } else if (!net.forestany.forestj.lib.Helper.isInteger(value.toString())) {
                    exceptionMessage = getString(R.string.settings_value_is_not_integer)
                    wrongValue = true
                } else if (Integer.parseInt(value.toString()) < 1) {
                    exceptionMessage = getString(R.string.settings_value_to_low, 1)
                    wrongValue = true
                } else if (Integer.parseInt(value.toString()) > 65535) {
                    exceptionMessage = getString(R.string.settings_value_to_high, 65535)
                    wrongValue = true
                }
            } else if (key.contentEquals("udp_multicast_ttl")) {
                // not empty, isInt, between 1 and 255
                if (value.toString().isEmpty()) {
                    exceptionMessage = getString(R.string.settings_value_empty)
                    wrongValue = true
                } else if (!net.forestany.forestj.lib.Helper.isInteger(value.toString())) {
                    exceptionMessage = getString(R.string.settings_value_is_not_integer)
                    wrongValue = true
                } else if (Integer.parseInt(value.toString()) < 1) {
                    exceptionMessage = getString(R.string.settings_value_to_low, 1)
                    wrongValue = true
                } else if (Integer.parseInt(value.toString()) > 255) {
                    exceptionMessage = getString(R.string.settings_value_to_high, 255)
                    wrongValue = true
                }
            } else if (key.contentEquals("tcp_server_port")) {
                // not empty, isInt, between 1 and 65535
                if (value.toString().isEmpty()) {
                    exceptionMessage = getString(R.string.settings_value_empty)
                    wrongValue = true
                } else if (!net.forestany.forestj.lib.Helper.isInteger(value.toString())) {
                    exceptionMessage = getString(R.string.settings_value_is_not_integer)
                    wrongValue = true
                } else if (Integer.parseInt(value.toString()) < 1) {
                    exceptionMessage = getString(R.string.settings_value_to_low, 1)
                    wrongValue = true
                } else if (Integer.parseInt(value.toString()) > 65535) {
                    exceptionMessage = getString(R.string.settings_value_to_high, 65535)
                    wrongValue = true
                }
            } else if (key.contentEquals("tcp_common_passphrase")) {
                // not empty, at least 36 characters long
                if (value.toString().isEmpty()) {
                    exceptionMessage = getString(R.string.settings_value_empty)
                    wrongValue = true
                } else if (value.toString().length < 36) {
                    exceptionMessage = getString(R.string.settings_value_length_to_low, 36)
                    wrongValue = true
                }
            }

            // entered value is wrong
            if (wrongValue) {
                // show error snackbar
                errorSnackbar(message = exceptionMessage, view = view, anchorView = anchorView)

                // manually update setting UI component
                when (val preference = findPreference<Preference>(key)) {
                    is EditTextPreference -> {
                        preference.text = sharedPreferencesHistory[key].toString()
                    }
                    is ListPreference -> {
                        preference.value = sharedPreferencesHistory[key].toString()
                    }
                    is Preference -> {
                        // nothing to do for password fields
                    }
                }
            } else {
                // update value is shared preferences history
                sharedPreferencesHistory[key] = value
            }
        }
    }

    override fun onStart() {
        super.onStart()

        Log.v(TAG, "onStart $TAG")
    }

    override fun onResume() {
        super.onResume()

        Log.v(TAG, "onResume $TAG")
    }

    override fun onPause() {
        super.onPause()

        Log.v(TAG, "onPause $TAG")
    }

    override fun onStop() {
        super.onStop()

        Log.v(TAG, "onStop $TAG")
    }

    override fun onRestart() {
        super.onRestart()

        Log.v(TAG, "onRestart $TAG")
    }

    override fun onDestroy() {
        super.onDestroy()

        Log.v(TAG, "onDestroy $TAG")
    }
}