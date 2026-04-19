package net.forestany.boardgamemoney.lobby

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Spinner
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import net.forestany.boardgamemoney.GlobalInstance
import net.forestany.boardgamemoney.MainActivity
import net.forestany.boardgamemoney.R
import net.forestany.boardgamemoney.Util.errorSnackbar
import net.forestany.boardgamemoney.Other
import net.forestany.boardgamemoney.bank.BankActivity

class LobbyActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "LobbyActivity"
    }

    private val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        when (it.resultCode) {
            RESULT_OK -> {

            }

            else -> {
                setResult(it.resultCode)
                finish()
            }
        }
    }

    private lateinit var rV_list: RecyclerView
    private lateinit var btn_join: Button
    private lateinit var btn_joinManually: Button
    private lateinit var adapter: LobbyViewAdapter
    private lateinit var uiThread: Thread

    private var lobbyList: List<String> = listOf()
    private var userName: String = ""
    private var userNameColor: String = ""
    private var selectedListItem: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_lobby)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.lobby_activity)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        adapter = LobbyViewAdapter(this, lobbyList, object : LobbyViewAdapter.ListClickListener {
            override fun onListItemClicked(selectedItem: Int) {
                selectedListItem = selectedItem
                Log.i(TAG, "List item clicked: $selectedItem")
            }
        })

        rV_list = findViewById(R.id.rV_list)
        rV_list.adapter = adapter
        rV_list.layoutManager = GridLayoutManager(this, 1)

        btn_join = findViewById(R.id.bt_join)
        btn_join.setOnClickListener {
            joinGame()
        }

        btn_joinManually = findViewById(R.id.bt_join_manually)
        btn_joinManually.setOnClickListener {
            joinGameManually()
        }

        userName = intent.extras?.getString("GAME_USER") ?: "NO_USER_ENTERED"

        if ((userName == "NO_USER_ENTERED") || (userName.isBlank())) {
            setResult(MainActivity.RETURN_CODE_NO_USER)
            finish()
        }

        userNameColor = intent.extras?.getString("GAME_USER_COLOR") ?: "NO_USER_COLOR_ENTERED"

        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                /* execute anything, e.g. finish() - if nothing is here, nothing happens pushing main back button */
                setResult(MainActivity.RETURN_CODE_LOBBY_EXIT)
                finish()
            }
        })

        Log.i(TAG, "find game as '$userName'")

        if (GlobalInstance.get().o_communicationLobby == null) {
            /*val wifi = getSystemService(WIFI_SERVICE) as WifiManager
            val multicastLock = wifi.createMulticastLock("multicastLockClient")
            multicastLock.setReferenceCounted(true)
            multicastLock.acquire()*/

            Other().netLobby(
                GlobalInstance.get().getPreferences()["udp_multicast_ip"].toString(), //MainActivity.UDP_MULTICAST_IP,
                Integer.parseInt(GlobalInstance.get().getPreferences()["udp_multicast_port"].toString()), //MainActivity.UDP_MULTICAST_PORT,
                Integer.parseInt(GlobalInstance.get().getPreferences()["udp_multicast_ttl"].toString()), //MainActivity.UDP_MULTICAST_TTL,
                "",
                "",
                Integer.parseInt(GlobalInstance.get().getPreferences()["tcp_server_port"].toString()) //MainActivity.TCP_SERVER_PORT
            )
        }

        uiThread = object : Thread() {
            override fun run() {
                while (true) {
                    try {
                        runOnUiThread {
                            var l_list: List<String> = listOf()

                            for ((_, s_value) in GlobalInstance.get().getClientLobbyEntries()) {
                                if (!s_value.contains("|")) {
                                    continue
                                }

                                l_list = l_list.plus(s_value.substring(0, s_value.indexOf("|")))
                            }

                            adapter.updateAllData(l_list)
                        }

                        sleep(2500)
                    } catch (_: InterruptedException) {
                        break
                    }
                }
            }
        }

        uiThread.start()

        Log.v(TAG, "onCreate $TAG")
    }

    private fun joinGame() {
        if (selectedListItem < 0) {
            errorSnackbar(message = getString(R.string.lobby_entry_empty), view = findViewById(android.R.id.content), length = com.google.android.material.snackbar.Snackbar.LENGTH_LONG)
        } else {
            try {
                var s_connectionInfo: String? = null
                var i = 0

                for ((_, s_foo) in GlobalInstance.get().getClientLobbyEntries()) {
                    if (i++ != selectedListItem) {
                        continue
                    }

                    s_connectionInfo = s_foo
                }

                if (s_connectionInfo.isNullOrBlank()) {
                    throw Exception(getString(R.string.lobby_entry_not_found))
                }

                val gameName = s_connectionInfo.split("|")[0]
                s_connectionInfo = s_connectionInfo.split("|")[1]
                val s_serverIp = s_connectionInfo.split(":".toRegex())[0]
                val s_serverPort = s_connectionInfo.split(":".toRegex())[1].toInt()

                Log.i(TAG, "connect to game as '$gameName' with '$s_serverIp:$s_serverPort'")

                val intent = Intent(this, BankActivity::class.java)
                intent.putExtra("GAME_NAME", gameName)
                intent.putExtra("GAME_USER", userName)
                intent.putExtra("GAME_USER_COLOR", userNameColor)
                intent.putExtra("ONLY_BANK", "0")
                intent.putExtra("LOAD_GAME_STATE", "0")
                intent.putExtra("NETWORK_INTERFACE", "$s_serverIp:$s_serverPort")

                launcher.launch(intent)
            } catch (e: Exception) {
                e.printStackTrace()
                setResult(MainActivity.RETURN_CODE_INVALID_LOBBY)
                finish()
            }
        }
    }

    private fun showAlertDialog(title: String, view: View?, positiveClickListener: View.OnClickListener) {
        AlertDialog.Builder(this, R.style.SelectionDialogStyle).setTitle(title).setView(view)
            .setNegativeButton(getString(R.string.text_cancel), null)
            .setPositiveButton(getString(R.string.text_ok)) { _, _ ->
                positiveClickListener.onClick(null)
            }.show()
    }

    private fun joinGameManually() {
        try {
            val dialogView = View.inflate(this, R.layout.dialog_create_or_join_game, null)
            val eT_user = dialogView.findViewById<EditText>(R.id.eT_user)
            val eT_game = dialogView.findViewById<EditText>(R.id.eT_game)
            val sp_networkInterface = dialogView.findViewById<Spinner>(R.id.sp_networkInterface)
            val cB_bankOnly = dialogView.findViewById<CheckBox>(R.id.cB_bank_only)
            val btn_colorPicker = dialogView.findViewById<Button>(R.id.btn_pickColor)

            cB_bankOnly.visibility = View.GONE
            btn_colorPicker.visibility = View.GONE
            sp_networkInterface.visibility = View.GONE

            eT_user.hint = getString(R.string.lobby_join_manually_host)
            eT_user.inputType = android.text.InputType.TYPE_CLASS_TEXT
            eT_user.filters = arrayOf(android.text.InputFilter { source, start, end, _, _, _ ->
                for (i in start until end) {
                    val c = source[i]
                    // convert char to String for regex matching
                    if (!c.toString().matches(Regex("[0-9a-fA-F.:]*"))) {
                        return@InputFilter "" // reject input
                    }
                }
                null // accept input
            })

            eT_game.hint = getString(R.string.lobby_join_manually_port)
            eT_game.keyListener = android.text.method.DigitsKeyListener.getInstance("0123456789")

            showAlertDialog(getString(R.string.lobby_join_manually_title), dialogView) {
                val s_serverIp = eT_user.text
                val s_serverPort = eT_game.text

                if (s_serverIp.toString().isEmpty()) {
                    errorSnackbar(message = getString(R.string.lobby_ip_empty), view = findViewById(android.R.id.content))
                    return@showAlertDialog
                } else if (s_serverIp.toString().length < 8) {
                    errorSnackbar(message = getString(R.string.lobby_ip_length_to_low, 8), view = findViewById(android.R.id.content))
                    return@showAlertDialog
                }

                if (s_serverPort.toString().isEmpty()) {
                    errorSnackbar(message = getString(R.string.lobby_port_empty), view = findViewById(android.R.id.content))
                    return@showAlertDialog
                } else if (!net.forestany.forestj.lib.Helper.isInteger(s_serverPort.toString())) {
                    errorSnackbar(message = getString(R.string.lobby_port_is_not_integer), view = findViewById(android.R.id.content))
                    return@showAlertDialog
                } else if (Integer.parseInt(s_serverPort.toString()) < 1) {
                    errorSnackbar(message = getString(R.string.lobby_port_to_low, 1), view = findViewById(android.R.id.content))
                    return@showAlertDialog
                } else if (Integer.parseInt(s_serverPort.toString()) > 65535) {
                    errorSnackbar(message = getString(R.string.lobby_port_to_high, 65535), view = findViewById(android.R.id.content))
                    return@showAlertDialog
                }

                Log.i(TAG, "connect to game with '$s_serverIp:$s_serverPort'")

                val intent = Intent(this, BankActivity::class.java)
                intent.putExtra("GAME_NAME", "game manually")
                intent.putExtra("GAME_USER", userName)
                intent.putExtra("GAME_USER_COLOR", userNameColor)
                intent.putExtra("ONLY_BANK", "0")
                intent.putExtra("LOAD_GAME_STATE", "0")
                intent.putExtra("NETWORK_INTERFACE", "$s_serverIp:$s_serverPort")

                launcher.launch(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            setResult(MainActivity.RETURN_CODE_INVALID_LOBBY)
            finish()
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

        try {
            uiThread.interrupt()
        } catch (_: Exception) {

        }

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

        GlobalInstance.get().clearClientLobbyEntries()

        val l_list: List<String> = listOf()
        adapter.updateAllData(l_list)

        Log.v(TAG, "onDestroy $TAG")
    }
}