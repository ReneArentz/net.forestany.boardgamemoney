package net.forestany.boardgamemoney

// android studio: collapse all methods: ctrl + shift + * and then 1 on numpad
// android studio: expand all with ctrl + shift + numpad + several times

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.os.LocaleListCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.random.Random
import kotlin.system.exitProcess
import net.forestany.boardgamemoney.Util.errorSnackbar
import net.forestany.boardgamemoney.Util.notifySnackbar
import net.forestany.boardgamemoney.bank.BankActivity
import net.forestany.boardgamemoney.bank.GameState
import net.forestany.boardgamemoney.bank.Message
import net.forestany.boardgamemoney.bank.Player
import net.forestany.boardgamemoney.lobby.LobbyActivity
import net.forestany.boardgamemoney.settings.SettingsActivity
import yuku.ambilwarna.AmbilWarnaDialog

class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"

        const val GAME_STATE_FILENAME = "gameState.txt"

        const val RETURN_CODE_NO_USER = 3742
        const val RETURN_CODE_NO_GAME = 4238
        const val RETURN_CODE_NO_NETWORK_INTERFACE = 5748
        const val RETURN_CODE_OTHER_EXIT = 2683
        const val RETURN_CODE_OWN_EXIT = 8948
        const val RETURN_CODE_LOBBY_EXIT = 1356
        const val RETURN_CODE_INVALID_LOBBY = 4727

        const val SETTINGS_GENERAL_CURRENCY_TEXT = "€"
        const val SETTINGS_GENERAL_SHOW_EVENTS = "false"
        const val SETTINGS_UDP_NETWORK_INTERFACE_NAME = "wlan0"
        const val SETTINGS_UDP_MULTICAST_IP = "224.0.0.1" //"FF05:0:0:0:0:0:0:342"
        const val SETTINGS_UDP_MULTICAST_PORT = "12805"
        const val SETTINGS_UDP_MULTICAST_TTL = "5"
        const val SETTINGS_TCP_SERVER_PORT = "12365"
        const val SETTINGS_TCP_COMMON_PASSPHRASE = "1234567890abcdefghijklmnopqrstuvwxyz"
        const val SETTINGS_TCP_ENCRYPTION = "false"

        fun loadGameState(directory: java.io.File): GameState? {
            var gameStateTemp: GameState? = null
            val file = java.io.File(directory, GAME_STATE_FILENAME)

            if (file.exists()) {
                try {
                    val inputStream = java.io.FileInputStream(file)
                    val inputStreamReader = java.io.InputStreamReader(inputStream)
                    val bufferedReader = java.io.BufferedReader(inputStreamReader)
                    var line: String?
                    var i = 0
                    var readPlayers = false
                    var readLedger = false
                    gameStateTemp = GameState()

                    while (bufferedReader.readLine().also { line = it } != null) {
                        if (line.isNullOrEmpty()) {
                            continue
                        }

                        if (i == 0) {
                            gameStateTemp.timestamp = net.forestany.forestj.lib.Helper.fromISO8601UTC(line)
                        } else if (i == 1) {
                            gameStateTemp.userType = line!!
                        } else if (i == 2) {
                            val a_foo = line!!.split("|")

                            if (a_foo.size == 3) {
                                gameStateTemp.gameName = a_foo[0]
                                gameStateTemp.serverIp = a_foo[1]
                                gameStateTemp.serverPort = a_foo[2]
                            }
                        } else if (i == 3) {
                            val a_foo = line!!.split("|")

                            if (a_foo.size == 3) {
                                gameStateTemp.userName = a_foo[0]
                                gameStateTemp.userNameColor = a_foo[1]
                                gameStateTemp.onlyBank = a_foo[2].contentEquals("true")
                            }
                        } else {
                            if (line.contentEquals("players|")) {
                                readPlayers = true
                            } else if (line.contentEquals("ledger|")) {
                                readLedger = true
                                readPlayers = false
                            } else if (readPlayers) {
                                val a_foo = line!!.split("|")

                                if (a_foo.size == 3) {
                                    gameStateTemp.players.add(Player(a_foo[0], a_foo[1], a_foo[2].contentEquals("true")))
                                }
                            } else if (readLedger) {
                                val a_foo = line!!.split(":")

                                if (a_foo.size == 4) {
                                    gameStateTemp.ledger.add(Message(a_foo[0].toInt(), a_foo[1], a_foo[2], a_foo[3].toInt()))
                                }
                            }
                        }

                        i++
                    }

                    inputStream.close()
                } catch (e: java.io.IOException) {
                    gameStateTemp = null
                }
            }

            return gameStateTemp
        }

        fun deleteGameState(directory: java.io.File) {
            val file = java.io.File(directory, GAME_STATE_FILENAME)

            if (file.exists()) {
                file.delete()
            }
        }
    }

    private lateinit var btn_find: Button
    private lateinit var btn_createGame: Button
    private lateinit var btn_localGame: Button

    private var gameState: GameState? = null

    private val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        when (it.resultCode) {
            RESULT_OK -> {

            }

            RETURN_CODE_NO_USER -> {
                errorSnackbar(message = getString(R.string.main_return_message_no_user), view = findViewById(android.R.id.content), length = com.google.android.material.snackbar.Snackbar.LENGTH_LONG)
            }

            RETURN_CODE_NO_GAME -> {
                errorSnackbar(message = getString(R.string.main_return_message_no_game), view = findViewById(android.R.id.content), length = com.google.android.material.snackbar.Snackbar.LENGTH_LONG)
            }

            RETURN_CODE_NO_NETWORK_INTERFACE -> {
                errorSnackbar(message = getString(R.string.main_return_message_no_network_interface), view = findViewById(android.R.id.content), length = com.google.android.material.snackbar.Snackbar.LENGTH_LONG)
            }

            RETURN_CODE_OTHER_EXIT -> {
                errorSnackbar(message = getString(R.string.main_return_message_other_exit), view = findViewById(android.R.id.content))
            }

            RETURN_CODE_OWN_EXIT -> {
                notifySnackbar(message = getString(R.string.main_return_message_own_exit), view = findViewById(android.R.id.content), length = com.google.android.material.snackbar.Snackbar.LENGTH_LONG)
            }

            RETURN_CODE_LOBBY_EXIT -> {
                notifySnackbar(message = getString(R.string.main_return_message_lobby_exit), view = findViewById(android.R.id.content), length = com.google.android.material.snackbar.Snackbar.LENGTH_LONG)
            }

            RETURN_CODE_INVALID_LOBBY -> {
                errorSnackbar(message = getString(R.string.main_return_message_invalid_lobby), view = findViewById(android.R.id.content))
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // init forestj logging
        initLogging()

        // default settings
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_activity)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        try {
            // settings toolbar
            val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar_main)
            toolbar.overflowIcon = ContextCompat.getDrawable(this, R.drawable.ic_hamburger_menu)
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(false) /* standard back/home button */
            supportActionBar?.title = getString(R.string.app_name)

            // deactivate standard back button
            onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    /* execute anything, e.g. finish() - if nothing is here, nothing happens pushing main back button */
                }
            })

            btn_find = findViewById(R.id.bt_findGame)
            btn_find.setOnClickListener {
                // check if we have a saved game state
                if ((gameState != null) && (gameState?.userType.contentEquals("client"))) {
                    AlertDialog.Builder(this@MainActivity, R.style.ConfirmDialogStyle)
                        .setTitle(getString(R.string.main_game_state_title))
                        .setMessage(getString(R.string.main_game_state_question, gameState?.gameName, gameState?.timestamp?.withNano(0).toString()))
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .setPositiveButton(getString(R.string.text_yes)) { _, _ ->
                            showFindGameDialog()
                        }
                        .setNegativeButton(getString(R.string.text_no)) { _, _ ->
                            gameState = null
                            showFindGameDialog()
                        }
                        .show()
                } else {
                    showFindGameDialog()
                }
            }

            btn_createGame = findViewById(R.id.bt_createGame)
            btn_createGame.setOnClickListener {
                // check if we have a saved game state
                if ((gameState != null) && (gameState?.userType.contentEquals("server"))) {
                    AlertDialog.Builder(this@MainActivity, R.style.ConfirmDialogStyle)
                        .setTitle(getString(R.string.main_game_state_title))
                        .setMessage(getString(R.string.main_game_state_question, gameState?.gameName, gameState?.timestamp?.withNano(0).toString()))
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .setPositiveButton(getString(R.string.text_yes)) { _, _ ->
                            showCreateGameDialog()
                        }
                        .setNegativeButton(getString(R.string.text_no)) { _, _ ->
                            gameState = null
                            showCreateGameDialog()
                        }
                        .show()
                } else {
                    showCreateGameDialog()
                }
            }

            btn_localGame = findViewById(R.id.bt_localGame)
            btn_localGame.setOnClickListener {
                // check if we have a saved game state
                if ((gameState != null) && (gameState?.userType.contentEquals("local"))) {
                    AlertDialog.Builder(this@MainActivity, R.style.ConfirmDialogStyle)
                        .setTitle(getString(R.string.main_game_state_title))
                        .setMessage(getString(R.string.main_game_state_question_local, gameState?.timestamp?.withNano(0).toString()))
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .setPositiveButton(getString(R.string.text_yes)) { _, _ ->
                            showLocalGameDialog()
                        }
                        .setNegativeButton(getString(R.string.text_no)) { _, _ ->
                            gameState = null
                            showLocalGameDialog()
                        }
                        .show()
                } else {
                    showLocalGameDialog()
                }
            }

            // restart all settings of app
            //getSharedPreferences("${packageName}_preferences", Context.MODE_PRIVATE).edit(commit = true) { clear() }

            resetAll()
            initSettings()

            // load game state
            gameState = loadGameState(cacheDir)

            // delete loaded game state if it is older than 10 minutes
            if ((gameState != null) && (gameState?.timestamp != null)) {
                if (gameState?.timestamp!!.isBefore(java.time.LocalDateTime.now().minusMinutes(10))) {
                    deleteGameState(cacheDir)
                    gameState = null
                }
            }

//            Log.v(TAG, getAllWifiIpv4Addresses(this).joinToString(", "))
//
//            val cm : ConnectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
//            val networkCapabilities = cm.getNetworkCapabilities(cm.activeNetwork)
//            Log.e(TAG, "Network connection: " + networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
//            Log.e(TAG, "WiFi: " + networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI))
//            Log.e(TAG, "Mobile: " + networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
//
//            cm.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
//                override fun onAvailable(network : android.net.Network) {
//                    Log.e(TAG, "The default network is now: " + network)
//                }
//
//                override fun onLost(network : android.net.Network) {
//                    Log.e(TAG, "The application no longer has a default network. The last default network was " + network)
//                }
//
//                override fun onCapabilitiesChanged(network : android.net.Network, networkCapabilities : NetworkCapabilities) {
//                    Log.e(TAG, "The default network changed capabilities: " + networkCapabilities)
//                }
//
//                override fun onLinkPropertiesChanged(network : android.net.Network, linkProperties : android.net.LinkProperties) {
//                    Log.e(TAG, "The default network changed link properties: " + linkProperties)
//                }
//            })
        } catch (e: Exception) {
            errorSnackbar(message = e.message ?: "Exception in onCreate method.", view = findViewById(R.id.main_activity))
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                finishAffinity()
                exitProcess(0)
            }, 15000)
        }

        Log.v(TAG, "onCreate $TAG")
    }

    private fun initLogging() {
        net.forestany.forestj.lib.Global.get().resetLog()

        val o_loggingConfigAll = net.forestany.forestj.lib.LoggingConfig()
        o_loggingConfigAll.level = java.util.logging.Level.OFF
        //o_loggingConfigAll.level = java.util.logging.Level.SEVERE
        //o_loggingConfigAll.level = java.util.logging.Level.WARNING
        //o_loggingConfigAll.level = java.util.logging.Level.INFO
        //o_loggingConfigAll.level = java.util.logging.Level.CONFIG
        //o_loggingConfigAll.level = java.util.logging.Level.FINE
        //o_loggingConfigAll.level = java.util.logging.Level.FINER
        //o_loggingConfigAll.level = java.util.logging.Level.FINEST
        //o_loggingConfigAll.level = java.util.logging.Level.ALL
        o_loggingConfigAll.useConsole = false

        o_loggingConfigAll.consoleLevel = java.util.logging.Level.OFF
        //o_loggingConfigAll.consoleLevel = java.util.logging.Level.SEVERE
        //o_loggingConfigAll.consoleLevel = java.util.logging.Level.WARNING
        //o_loggingConfigAll.consoleLevel = java.util.logging.Level.INFO
        //o_loggingConfigAll.consoleLevel = java.util.logging.Level.CONFIG
        //o_loggingConfigAll.consoleLevel = java.util.logging.Level.FINE
        //o_loggingConfigAll.consoleLevel = java.util.logging.Level.FINER
        //o_loggingConfigAll.consoleLevel = java.util.logging.Level.FINEST
        //o_loggingConfigAll.consoleLevel = java.util.logging.Level.ALL

        //o_loggingConfigAll.useFile = true
        //o_loggingConfigAll.fileLevel = java.util.logging.Level.SEVERE
        //o_loggingConfigAll.filePath = "C:\\Users\\Public\\Documents\\"
        //o_loggingConfigAll.fileLimit = 1000000 // ~ 1.0 MB
        //o_loggingConfigAll.fileCount = 25
        o_loggingConfigAll.loadConfig()

        net.forestany.forestj.lib.Global.get().by_logControl = net.forestany.forestj.lib.Global.OFF

        //net.forestany.forestj.lib.Global.get().by_logControl = (net.forestany.forestj.lib.Global.SEVERE + net.forestany.forestj.lib.Global.WARNING + net.forestany.forestj.lib.Global.INFO).toByte()
        net.forestany.forestj.lib.Global.get().by_internalLogControl = net.forestany.forestj.lib.Global.OFF
        //net.forestany.forestj.lib.Global.get().by_internalLogControl = net.forestany.forestj.lib.Global.SEVERE
        //net.forestany.forestj.lib.Global.get().by_internalLogControl = (net.forestany.forestj.lib.Global.SEVERE + net.forestany.forestj.lib.Global.WARNING).toByte()
        //net.forestany.forestj.lib.Global.get().by_internalLogControl = (net.forestany.forestj.lib.Global.SEVERE + net.forestany.forestj.lib.Global.WARNING + net.forestany.forestj.lib.Global.INFO).toByte()
        //net.forestany.forestj.lib.Global.get().by_internalLogControl = (net.forestany.forestj.lib.Global.SEVERE + net.forestany.forestj.lib.Global.WARNING + net.forestany.forestj.lib.Global.INFO + net.forestany.forestj.lib.Global.CONFIG).toByte()
        //net.forestany.forestj.lib.Global.get().by_internalLogControl = (net.forestany.forestj.lib.Global.SEVERE + net.forestany.forestj.lib.Global.WARNING + net.forestany.forestj.lib.Global.INFO + net.forestany.forestj.lib.Global.CONFIG + net.forestany.forestj.lib.Global.FINE + net.forestany.forestj.lib.Global.FINER).toByte()
        //net.forestany.forestj.lib.Global.get().by_internalLogControl = (net.forestany.forestj.lib.Global.SEVERE + net.forestany.forestj.lib.Global.WARNING + net.forestany.forestj.lib.Global.INFO + net.forestany.forestj.lib.Global.CONFIG + net.forestany.forestj.lib.Global.FINE + net.forestany.forestj.lib.Global.FINER + net.forestany.forestj.lib.Global.FINEST).toByte()
        //net.forestany.forestj.lib.Global.get().by_internalLogControl = (net.forestany.forestj.lib.Global.SEVERE + net.forestany.forestj.lib.Global.WARNING + net.forestany.forestj.lib.Global.INFO + net.forestany.forestj.lib.Global.CONFIG + net.forestany.forestj.lib.Global.FINE + net.forestany.forestj.lib.Global.FINER + net.forestany.forestj.lib.Global.FINEST + net.forestany.forestj.lib.Global.MASS).toByte()
    }

    private fun getAllWifiIpv4Addresses(context: Context): List<String> {
        val ipv4Addresses = mutableListOf<String>()
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network)

        if (networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) {
            val linkProperties = connectivityManager.getLinkProperties(network)
            linkProperties?.linkAddresses?.forEach { linkAddress ->
                if (linkAddress.address is java.net.Inet4Address) {
                    ipv4Addresses.add(linkAddress.address.hostAddress ?: "ip is null")
                }
            }
        }

        return ipv4Addresses
    }

    @SuppressLint("RestrictedApi")
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)

        // allow showing icons on dropdown toolbar menu
        try {
            if (menu is androidx.appcompat.view.menu.MenuBuilder) {
                val menuBuilder: androidx.appcompat.view.menu.MenuBuilder = menu as androidx.appcompat.view.menu.MenuBuilder
                menuBuilder.setOptionalIconsVisible(true)
            }
            // does not run with release build, so the solution above is enough - @SuppressLint("RestrictedApi") needed
            //val method = menu?.javaClass?.getDeclaredMethod("setOptionalIconsVisible", Boolean::class.javaPrimitiveType)
            //method?.isAccessible = true
            //method?.invoke(menu, true)
        } catch (e: Exception) {
            errorSnackbar(message = e.message ?: "Exception in onCreateOptionsMenu method.", view = findViewById(android.R.id.content))
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.mI_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                launcher.launch(intent)

                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun showAlertDialog(title: String, view: View?, positiveClickListener: View.OnClickListener) {
        AlertDialog.Builder(this, R.style.SelectionDialogStyle).setTitle(title).setView(view)
            .setNegativeButton(getString(R.string.text_cancel), null)
            .setPositiveButton(getString(R.string.text_ok)) { _, _ ->
                positiveClickListener.onClick(null)
            }.show()
    }

    private fun showCreateGameDialog() {
        // load game state if it is available
        if ((gameState != null) && (gameState?.userType.contentEquals("server"))) {
            val onlyBank = if (gameState?.onlyBank == true) {
                "1"
            } else {
                "0"
            }

            val intent = Intent(this, BankActivity::class.java)
            intent.putExtra("GAME_NAME", gameState?.gameName)
            intent.putExtra("GAME_USER", gameState?.userName)
            intent.putExtra("GAME_USER_COLOR", gameState?.userNameColor)
            intent.putExtra("ONLY_BANK", onlyBank)
            intent.putExtra("LOAD_GAME_STATE", "1")
            intent.putExtra("NETWORK_INTERFACE", "${gameState?.serverIp}:${gameState?.serverPort}")
            GlobalInstance.get().b_isServer = true
            GlobalInstance.get().s_user = gameState?.userType!!
            GlobalInstance.get().s_userColor = gameState?.userNameColor!!
            launcher.launch(intent)
        } else {
            /*val intent = Intent(this, BankActivity::class.java)
            intent.putExtra("GAME_NAME", "test game name")
            intent.putExtra("GAME_USER", "creator")
            intent.putExtra("GAME_USER_COLOR", "#FFAAFF")
            intent.putExtra("ONLY_BANK", "0")
            intent.putExtra("LOAD_GAME_STATE", "0")
            intent.putExtra("NETWORK_INTERFACE", "192.168.178.22:${GlobalInstance.get().getPreferences()["tcp_server_port"]}")
            GlobalInstance.get().b_isServer = true
            GlobalInstance.get().s_user = "creator"
            GlobalInstance.get().s_userColor = "#FFAAFF"
            launcher.launch(intent)
            return@showFindGameDialog*/

            val dialogView = View.inflate(this, R.layout.dialog_create_or_join_game, null)
            val cB_bankOnly = dialogView.findViewById<CheckBox>(R.id.cB_bank_only)
            val eT_user = dialogView.findViewById<EditText>(R.id.eT_user)
            val btn_colorPicker = dialogView.findViewById<Button>(R.id.btn_pickColor)
            val eT_game = dialogView.findViewById<EditText>(R.id.eT_game)
            val sp_networkInterface = dialogView.findViewById<Spinner>(R.id.sp_networkInterface)
            eT_game.visibility = View.VISIBLE
            sp_networkInterface.visibility = View.VISIBLE

            cB_bankOnly.setOnCheckedChangeListener { _, _ ->
                if (cB_bankOnly.isChecked) {
                    eT_user.setText("")
                    eT_user.isEnabled = false
                    btn_colorPicker.setBackgroundColor(Color.GRAY)
                    btn_colorPicker.isEnabled = false
                } else {
                    eT_user.isEnabled = true
                    btn_colorPicker.isEnabled = true
                }
            }

            val rnd = Random.Default
            var selectedColor: Int = Color.rgb(rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256))

            btn_colorPicker.setBackgroundColor(selectedColor)
            btn_colorPicker.setOnClickListener {
                val colorPicker = AmbilWarnaDialog(this, selectedColor, object : AmbilWarnaDialog.OnAmbilWarnaListener {
                    override fun onOk(dialog: AmbilWarnaDialog?, color: Int) {
                        selectedColor = color
                        btn_colorPicker.setBackgroundColor(color)
                    }

                    override fun onCancel(dialog: AmbilWarnaDialog?) {
                        // nothing happens if cancelled
                    }
                })
                colorPicker.show()
            }

            var selectedNetworkInterface: String? = null

            sp_networkInterface.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                    selectedNetworkInterface = parent.getItemAtPosition(position).toString()
                    /* Log.i(TAG, "selected network interface: ${selectedNetworkInterface!!}") */
                }

                override fun onNothingSelected(parent: AdapterView<*>) {

                }
            }

            var a_networkInterfaces: List<String> = getAllWifiIpv4Addresses(this)

            if (a_networkInterfaces.isEmpty()) {
                a_networkInterfaces = a_networkInterfaces.plus(getString(R.string.no_network_interfaces))
            }

            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, a_networkInterfaces)
            adapter.setDropDownViewResource(android.R.layout.select_dialog_singlechoice)
            sp_networkInterface.adapter = adapter

            showAlertDialog(getString(R.string.main_create_game_title), dialogView) {
                if (selectedNetworkInterface == getString(R.string.no_network_interfaces)) {
                    errorSnackbar(message = getString(R.string.main_return_message_no_network_interface), view = findViewById(android.R.id.content))
                    return@showAlertDialog
                } else if ((!cB_bankOnly.isChecked) && (eT_user.text.toString().isEmpty())) {
                    errorSnackbar(message = getString(R.string.main_user_empty), view = findViewById(android.R.id.content))
                    return@showAlertDialog
                } else if ((!cB_bankOnly.isChecked) && (eT_user.text.toString().length < 4)) {
                    errorSnackbar(message = getString(R.string.main_user_length_to_low, 4), view = findViewById(android.R.id.content))
                    return@showAlertDialog
                } else if ((!cB_bankOnly.isChecked) && (eT_user.text.toString().trim().lowercase().contentEquals("bank"))) {
                    errorSnackbar(message = getString(R.string.main_bank_as_name_invalid), view = findViewById(android.R.id.content))
                    return@showAlertDialog
                } else if (eT_game.text.toString().isEmpty()) {
                    errorSnackbar(message = getString(R.string.main_game_empty), view = findViewById(android.R.id.content))
                    return@showAlertDialog
                } else if (eT_game.text.toString().length < 4) {
                    errorSnackbar(message = getString(R.string.main_game_length_to_low, 4), view = findViewById(android.R.id.content))
                    return@showAlertDialog
                }

                Log.i(TAG, "create game '" + eT_game.text + "' as '" + eT_user.text + "'")

                assumeSharedPreferencesToGlobal(getSharedPreferences("${packageName}_preferences", Context.MODE_PRIVATE))

                val onlyBank = if (cB_bankOnly.isChecked) {
                    "1"
                } else {
                    "0"
                }

                val intent = Intent(this, BankActivity::class.java)
                intent.putExtra("GAME_NAME", eT_game.text.toString())
                intent.putExtra("GAME_USER", eT_user.text.toString())
                intent.putExtra("GAME_USER_COLOR", String.format("#%06X", 0xFFFFFF and selectedColor))
                intent.putExtra("ONLY_BANK", onlyBank)
                intent.putExtra("LOAD_GAME_STATE", "0")
                intent.putExtra("NETWORK_INTERFACE", "$selectedNetworkInterface:${GlobalInstance.get().getPreferences()["tcp_server_port"]}")
                GlobalInstance.get().b_isServer = true
                GlobalInstance.get().s_user = eT_user.text.toString()
                GlobalInstance.get().s_userColor = String.format("#%06X", 0xFFFFFF and selectedColor)

                launcher.launch(intent)
            }
        }
    }

    private fun showFindGameDialog() {
        // load game state if it is available
        if ((gameState != null) && (gameState?.userType.contentEquals("client"))) {
            val intent = Intent(this, BankActivity::class.java)
            intent.putExtra("GAME_NAME", gameState?.gameName)
            intent.putExtra("GAME_USER", gameState?.userName)
            intent.putExtra("GAME_USER_COLOR", gameState?.userNameColor)
            intent.putExtra("ONLY_BANK", "0")
            intent.putExtra("LOAD_GAME_STATE", "0")
            intent.putExtra("NETWORK_INTERFACE", "${gameState?.serverIp}:${gameState?.serverPort}")
            GlobalInstance.get().s_user = gameState?.userName!!
            GlobalInstance.get().s_userColor = gameState?.userNameColor!!
            launcher.launch(intent)
        } else {
            /*val intent = Intent(this, BankActivity::class.java)
            intent.putExtra("GAME_NAME", "test game name")
            intent.putExtra("GAME_USER", "player")
            intent.putExtra("GAME_USER_COLOR", "#00FF00")
            intent.putExtra("ONLY_BANK", "0")
            intent.putExtra("LOAD_GAME_STATE", "0")
            intent.putExtra("NETWORK_INTERFACE", "192.168.178.22:${GlobalInstance.get().getPreferences()["tcp_server_port"]}")
            GlobalInstance.get().s_user = "player"
            GlobalInstance.get().s_userColor = "#00FF00"
            launcher.launch(intent)
            return@showFindGameDialog*/

            val dialogView = View.inflate(this, R.layout.dialog_create_or_join_game, null)
            val cB_bankOnly = dialogView.findViewById<CheckBox>(R.id.cB_bank_only)
            val eT_user = dialogView.findViewById<EditText>(R.id.eT_user)
            val btn_colorPicker = dialogView.findViewById<Button>(R.id.btn_pickColor)
            val eT_game = dialogView.findViewById<EditText>(R.id.eT_game)
            val sp_networkInterface = dialogView.findViewById<Spinner>(R.id.sp_networkInterface)
            cB_bankOnly.visibility = View.GONE
            eT_game.visibility = View.GONE
            sp_networkInterface.visibility = View.GONE

            val rnd = Random.Default
            var selectedColor: Int = Color.rgb(rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256))

            btn_colorPicker.setBackgroundColor(selectedColor)
            btn_colorPicker.setOnClickListener {
                val colorPicker = AmbilWarnaDialog(this, selectedColor, object : AmbilWarnaDialog.OnAmbilWarnaListener {
                    override fun onOk(dialog: AmbilWarnaDialog?, color: Int) {
                        selectedColor = color
                        btn_colorPicker.setBackgroundColor(color)
                    }

                    override fun onCancel(dialog: AmbilWarnaDialog?) {
                        // nothing happens if cancelled
                    }
                })
                colorPicker.show()
            }

            showAlertDialog(getString(R.string.main_find_game_title), dialogView) {
                if (eT_user.text.toString().isEmpty()) {
                    errorSnackbar(message = getString(R.string.main_user_empty), view = findViewById(android.R.id.content))
                    return@showAlertDialog
                } else if (eT_user.text.toString().length < 4) {
                    errorSnackbar(message = getString(R.string.main_user_length_to_low, 4), view = findViewById(android.R.id.content))
                    return@showAlertDialog
                } else if (eT_user.text.toString().trim().lowercase().contentEquals("bank")) {
                    errorSnackbar(message = getString(R.string.main_bank_as_name_invalid), view = findViewById(android.R.id.content))
                    return@showAlertDialog
                }

                Log.i(TAG, "find game as '" + eT_user.text + "'")

                assumeSharedPreferencesToGlobal(getSharedPreferences("${packageName}_preferences", Context.MODE_PRIVATE))

                val intent = Intent(this, LobbyActivity::class.java)
                intent.putExtra("GAME_USER", eT_user.text.toString())
                intent.putExtra("GAME_USER_COLOR", String.format("#%06X", 0xFFFFFF and selectedColor))
                GlobalInstance.get().s_user = eT_user.text.toString()
                GlobalInstance.get().s_userColor = String.format("#%06X", 0xFFFFFF and selectedColor)

                launcher.launch(intent)
            }
        }
    }

    private fun showLocalGameDialog() {
        // load game state if it is available
        if ((gameState != null) && (gameState?.userType.contentEquals("local"))) {
            val intent = Intent(this, BankActivity::class.java)
            intent.putExtra("LOAD_GAME_STATE", "1")
            intent.putExtra("PLAYERS", "LOAD_GAME_STATE")
            launcher.launch(intent)
        } else {
            val dialogView = View.inflate(this, R.layout.dialog_local_game, null)
            val etUsername = dialogView.findViewById<EditText>(R.id.et_username)
            val btnPickColor = dialogView.findViewById<Button>(R.id.btn_pick_color)
            val btnAdd = dialogView.findViewById<Button>(R.id.btn_add_player)
            val rvPlayers = dialogView.findViewById<RecyclerView>(R.id.rv_players)
            val btnOk = dialogView.findViewById<Button>(R.id.btn_ok)
            val btnCancel = dialogView.findViewById<Button>(R.id.btn_cancel)

            data class Player(val name: String, val color: Int)

            class PlayerAdapter(private val players: MutableList<Player>) : RecyclerView.Adapter<PlayerAdapter.PlayerViewHolder>() {

                inner class PlayerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
                    val tvName: TextView = view.findViewById(R.id.tv_name)
                    val btnDelete: Button = view.findViewById(R.id.btn_delete)
                }

                override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayerViewHolder {
                    val view = View.inflate(parent.context, R.layout.dialog_local_game_item_player, null)
                    return PlayerViewHolder(view)
                }

                override fun onBindViewHolder(holder: PlayerViewHolder, position: Int) {
                    val player = players[position]
                    holder.tvName.text = player.name
                    holder.tvName.setBackgroundColor(player.color)
                    holder.btnDelete.setOnClickListener {
                        val pos = holder.adapterPosition
                        if (pos != RecyclerView.NO_POSITION) {
                            players.removeAt(pos)
                            notifyItemRemoved(pos)
                        }
                    }
                }

                override fun getItemCount() = players.size
            }

            val players = mutableListOf<Player>()
            val adapter = PlayerAdapter(players)
            rvPlayers.layoutManager = LinearLayoutManager(this)
            rvPlayers.adapter = adapter

            val rnd = Random.Default
            var selectedColor: Int = Color.rgb(rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256))
            btnPickColor.setBackgroundColor(selectedColor)

            btnPickColor.setOnClickListener {
                AmbilWarnaDialog(
                    this, selectedColor,
                    object : AmbilWarnaDialog.OnAmbilWarnaListener {
                        override fun onOk(dialog: AmbilWarnaDialog?, color: Int) {
                            selectedColor = color
                            btnPickColor.setBackgroundColor(color)
                        }

                        override fun onCancel(dialog: AmbilWarnaDialog?) {}
                    }).show()
            }

            btnAdd.setOnClickListener {
                val name = etUsername.text.toString().trim()
                if (name.isNotEmpty()) {
                    if (name.lowercase().contentEquals("bank")) {
                        errorSnackbar(message = getString(R.string.main_bank_as_name_invalid), view = dialogView, anchorView = btnAdd)
                    } else if (name.length < 4) {
                        errorSnackbar(message = getString(R.string.main_user_length_to_low, 4), view = dialogView, anchorView = btnAdd)
                    } else {
                        players.add(Player(name, selectedColor))
                        adapter.notifyItemInserted(players.size - 1)
                        etUsername.text.clear()
                        selectedColor = Color.rgb(rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256))
                        btnPickColor.setBackgroundColor(selectedColor)
                    }
                }
            }

            val alert = AlertDialog.Builder(this)
                .setView(dialogView)
                .create()

            btnOk.setOnClickListener {
                var playersString = ""

                for (player in players) {
                    playersString += "${player.name}:${String.format("#%06X", 0xFFFFFF and player.color)};"
                }
                alert.dismiss()

                if (playersString.isNotBlank()) {
                    playersString = playersString.substring(0, playersString.length - 1)

                    val intent = Intent(this, BankActivity::class.java)
                    intent.putExtra("PLAYERS", playersString)
                    launcher.launch(intent)
                }
            }

            btnCancel.setOnClickListener {
                alert.dismiss()
            }

            alert.show()
        }
    }

    private fun resetAll() {
        try {
            GlobalInstance.get().o_threadLobby?.interrupt()
        } catch (_: Exception) {
        } finally {
            GlobalInstance.get().o_threadLobby = null
        }

        try {
            GlobalInstance.get().o_communicationLobby?.stop()
        } catch (_: Exception) {
        } finally {
            GlobalInstance.get().o_communicationLobby = null
        }

        try {
            GlobalInstance.get().o_threadBank?.interrupt()
        } catch (_: Exception) {
        } finally {
            GlobalInstance.get().o_threadBank = null
        }

        try {
            GlobalInstance.get().o_communicationBank?.stop()
        } catch (_: Exception) {
        } finally {
            GlobalInstance.get().o_communicationBank = null
        }

        GlobalInstance.get().b_isServer = false
        GlobalInstance.get().s_user = "NO_USER_SPECIFIED"
        GlobalInstance.get().s_userColor = "NO_USER_COLOR_SPECIFIED"
        GlobalInstance.get().clearClientLobbyEntries()
        GlobalInstance.get().clearPlayers()
        GlobalInstance.get().clearActivePlayers()
        GlobalInstance.get().clearLedger()
        GlobalInstance.get().clearMessageBox()
    }

    private fun initSettings() {
        val sharedPreferences = getSharedPreferences("${packageName}_preferences", Context.MODE_PRIVATE)

        checkForAppUpdate(sharedPreferences)

        //sharedPreferences.all.forEach {
        //    Log.v(TAG, "${it.key} -> ${it.value}")
        //}

        if (
            (sharedPreferences.all.isEmpty()) ||
            (!sharedPreferences.contains("general_locale")) ||
            (!sharedPreferences.contains("general_currency_text")) ||
            (!sharedPreferences.contains("general_show_events")) ||
            (!sharedPreferences.contains("udp_network_interface_name")) ||
            (!sharedPreferences.contains("udp_multicast_ip")) ||
            (!sharedPreferences.contains("udp_multicast_port")) ||
            (!sharedPreferences.contains("udp_multicast_ttl")) ||
            (!sharedPreferences.contains("tcp_server_port")) ||
            (!sharedPreferences.contains("tcp_common_passphrase")) ||
            (!sharedPreferences.contains("tcp_encryption"))
        ) {
            sharedPreferences.edit(commit = true) {
                if (!sharedPreferences.contains("general_locale")) {
                    val s_locale = java.util.Locale.getDefault().toString()

                    if ((s_locale.lowercase().startsWith("de")) || (s_locale.lowercase().startsWith("en"))) {
                        putString("general_locale", java.util.Locale.getDefault().toString().substring(0, 2))
                    } else {
                        putString("general_locale", "en")
                    }
                }

                if (!sharedPreferences.contains("general_currency_text")) putString("general_currency_text", SETTINGS_GENERAL_CURRENCY_TEXT)
                if (!sharedPreferences.contains("general_show_events")) putBoolean("general_show_events", SETTINGS_GENERAL_SHOW_EVENTS.contentEquals("true"))
                if (!sharedPreferences.contains("udp_network_interface_name")) putString("udp_network_interface_name", SETTINGS_UDP_NETWORK_INTERFACE_NAME)
                if (!sharedPreferences.contains("udp_multicast_ip")) putString("udp_multicast_ip", SETTINGS_UDP_MULTICAST_IP)
                if (!sharedPreferences.contains("udp_multicast_port")) putString("udp_multicast_port", SETTINGS_UDP_MULTICAST_PORT)
                if (!sharedPreferences.contains("udp_multicast_ttl")) putString("udp_multicast_ttl", SETTINGS_UDP_MULTICAST_TTL)
                if (!sharedPreferences.contains("tcp_server_port")) putString("tcp_server_port", SETTINGS_TCP_SERVER_PORT)
                if (!sharedPreferences.contains("tcp_common_passphrase")) putString("tcp_common_passphrase", SETTINGS_TCP_COMMON_PASSPHRASE)
                if (!sharedPreferences.contains("tcp_encryption")) putBoolean("tcp_encryption", SETTINGS_TCP_ENCRYPTION.contentEquals("true"))
            }
        }

        assumeSharedPreferencesToGlobal(sharedPreferences)

        if (java.util.Locale.getDefault().toString().substring(0, 2) != sharedPreferences.all["general_locale"].toString()) {
            AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags(
                    sharedPreferences.all["general_locale"].toString()
                )
            )
        }
    }

    private fun assumeSharedPreferencesToGlobal(sharedPreferences: SharedPreferences) {
        GlobalInstance.get().clearPreferences()

        sharedPreferences.all.forEach {
            GlobalInstance.get().addPreference(it.key, it.value)
            //if (it.key!!.contentEquals("option_one")) GlobalInstance.get().optionOne = it.value.toString()
            //if (it.key!!.contentEquals("option_two")) GlobalInstance.get().optionTwo = it.value.toString()
            //if (it.key!!.contentEquals("option_three")) GlobalInstance.get().optionThree = it.value.toString()
        }
    }

    private fun getCurrentAppVersion(): String? {
        return try {
            packageManager.getPackageInfo(packageName, 0).versionName
        } catch (_: PackageManager.NameNotFoundException) {
            "unknown_version"
        }
    }

    private fun checkForAppUpdate(o_sharedPreferences: SharedPreferences) {
        val s_lastVersion: String = o_sharedPreferences.getString("last_version", "") ?: ""

        val s_currentVersion = getCurrentAppVersion()

        if (s_currentVersion.contentEquals("unknown_version")) {
            errorSnackbar(message = getString(R.string.main_app_unknown_version), view = findViewById(android.R.id.content))
        } else if (s_lastVersion.isEmpty()) {
            onFirstLaunchEver()
            o_sharedPreferences.edit { putString("last_version", s_currentVersion) }
        } else if (s_currentVersion != s_lastVersion) {
            onFirstLaunchAfterUpdate()
            o_sharedPreferences.edit { putString("last_version", s_currentVersion) }
        } else {
            Log.v(TAG, "app has not changed")
        }
    }

    private fun onFirstLaunchEver() {
        Log.v(TAG, "first launch ever")
    }

    private fun onFirstLaunchAfterUpdate() {
        Log.v(TAG, "first launch after update")
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

        assumeSharedPreferencesToGlobal(getSharedPreferences("${packageName}_preferences", Context.MODE_PRIVATE))

        Log.v(TAG, "onRestart $TAG")
    }

    override fun onDestroy() {
        super.onDestroy()

        resetAll()

        Log.v(TAG, "onDestroy $TAG")
    }
}