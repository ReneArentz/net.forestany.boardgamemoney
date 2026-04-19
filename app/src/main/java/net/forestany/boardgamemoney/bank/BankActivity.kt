package net.forestany.boardgamemoney.bank

import android.annotation.SuppressLint

import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.DragEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import net.forestany.boardgamemoney.GlobalInstance
import net.forestany.boardgamemoney.MainActivity
import net.forestany.boardgamemoney.Other
import net.forestany.boardgamemoney.R
import net.forestany.boardgamemoney.Util
import net.forestany.boardgamemoney.Util.errorSnackbar

class BankActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "BankActivity"
    }

    private lateinit var ping: TextView
    private lateinit var playerIcon: ImageView
    private lateinit var playerName: TextView
    private lateinit var playerMoney: TextView
    private lateinit var transferAmount: EditText
    private lateinit var arrowLeft: ImageButton
    private lateinit var arrowRight: ImageButton

    private lateinit var bottomPlayerName: TextView
    private lateinit var bottomPlayerBalance: TextView
    private lateinit var bottomArrowLeft: ImageButton
    private lateinit var bottomArrowRight: ImageButton

    private lateinit var money_note_1: ImageView
    private lateinit var money_note_5: ImageView
    private lateinit var money_note_10: ImageView
    private lateinit var money_note_20: ImageView
    private lateinit var money_note_50: ImageView
    private lateinit var money_note_100: ImageView
    private lateinit var money_note_500: ImageView
    private lateinit var money_note_1000: ImageView

    private lateinit var uiThread: Thread

    private var localBank = false
    private var isNetworkBank = false
    private var onlyBank = false
    private var loadGameState = false
    private var userName: String = ""
    private var userNameColor: String = ""
    private var gameName: String = ""
    private var serverIp: String = ""
    private var serverPort: Int = 0

    private var currentPlayerIndex = 0
    private var currentBottomIndex = 0
    private var transferSum = 0

    private val a_observeLedger: MutableList<Message> = mutableListOf()
    private val a_observePlayers: MutableList<Player> = mutableListOf()
    private var q_events: java.util.Queue<String> = java.util.LinkedList()

    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_bank)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.bank_main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // settings toolbar
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar_bank)
        toolbar.overflowIcon = ContextCompat.getDrawable(this, R.drawable.ic_hamburger_menu)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(false) /* standard back/home button */
        supportActionBar?.title = getString(R.string.app_name)

        //region layout elements
        ping = findViewById(R.id.ping)
        playerIcon = findViewById(R.id.playerIcon)
        playerName = findViewById(R.id.playerName)
        playerMoney = findViewById(R.id.playerMoney)
        transferAmount = findViewById(R.id.transferAmount)
        transferAmount.setText("0 ${GlobalInstance.get().getPreferences()["general_currency_text"]}")
        arrowLeft = findViewById(R.id.arrowLeft)
        arrowRight = findViewById(R.id.arrowRight)

        bottomPlayerName = findViewById(R.id.bottomPlayerName)
        bottomPlayerBalance = findViewById(R.id.bottomPlayerBalance)
        bottomArrowLeft = findViewById(R.id.bottomArrowLeft)
        bottomArrowRight = findViewById(R.id.bottomArrowRight)

        money_note_1 = findViewById(R.id.note1)
        money_note_5 = findViewById(R.id.note5)
        money_note_10 = findViewById(R.id.note10)
        money_note_20 = findViewById(R.id.note20)
        money_note_50 = findViewById(R.id.note50)
        money_note_100 = findViewById(R.id.note100)
        money_note_500 = findViewById(R.id.note500)
        money_note_1000 = findViewById(R.id.note1000)

        var inputStream = this.assets.open("1_note.png")
        money_note_1.setImageBitmap(BitmapFactory.decodeStream(inputStream))
        inputStream.close()

        inputStream = this.assets.open("5_note.png")
        money_note_5.setImageBitmap(BitmapFactory.decodeStream(inputStream))
        inputStream.close()

        inputStream = this.assets.open("10_note.png")
        money_note_10.setImageBitmap(BitmapFactory.decodeStream(inputStream))
        inputStream.close()

        inputStream = this.assets.open("20_note.png")
        money_note_20.setImageBitmap(BitmapFactory.decodeStream(inputStream))
        inputStream.close()

        inputStream = this.assets.open("50_note.png")
        money_note_50.setImageBitmap(BitmapFactory.decodeStream(inputStream))
        inputStream.close()

        inputStream = this.assets.open("100_note.png")
        money_note_100.setImageBitmap(BitmapFactory.decodeStream(inputStream))
        inputStream.close()

        inputStream = this.assets.open("500_note.png")
        money_note_500.setImageBitmap(BitmapFactory.decodeStream(inputStream))
        inputStream.close()

        inputStream = this.assets.open("1000_note.png")
        money_note_1000.setImageBitmap(BitmapFactory.decodeStream(inputStream))
        inputStream.close()
        //endregion

        //region intent incoming data
        onlyBank = intent.extras?.getString("ONLY_BANK").contentEquals("1")
        loadGameState = intent.extras?.getString("LOAD_GAME_STATE").contentEquals("1")

        val incomingPlayers = intent.extras?.getString("PLAYERS") ?: "NO_PLAYERS"

        if (!incomingPlayers.contentEquals("NO_PLAYERS")) {
            localBank = true
        }

        if ((!loadGameState) && (localBank) && (!incomingPlayers.contentEquals("LOAD_GAME_STATE"))) {
            GlobalInstance.get().addPlayer(
                Player(
                    "Bank",
                    "#000000",
                    true
                )
            )

            for (playerInfo in incomingPlayers.split(";")) {
                if (playerInfo.contains(":")) {
                    GlobalInstance.get().addPlayer(
                        Player(
                            playerInfo.substring(0, playerInfo.indexOf(":")),
                            playerInfo.substring(playerInfo.indexOf(":") + 1),
                            false
                        )
                    )
                }
            }
        }

        userName = intent.extras?.getString("GAME_USER") ?: "NO_USER_ENTERED"

        if ((!onlyBank) && (!localBank) && ((userName == "NO_USER_ENTERED") || (userName.isBlank()))) {
            setResult(MainActivity.RETURN_CODE_NO_USER)
            finish()
        }

        userNameColor = intent.extras?.getString("GAME_USER_COLOR") ?: "#000000"

        gameName = intent.extras?.getString("GAME_NAME") ?: "NO_GAME_ENTERED"

        if ((!localBank) && ((gameName == "NO_GAME_ENTERED") || (gameName.isBlank()))) {
            setResult(MainActivity.RETURN_CODE_NO_GAME)
            finish()
        }

        val s_networkInterface: String = intent.extras?.getString("NETWORK_INTERFACE") ?: getString(R.string.no_network_interfaces)
        //endregion

        //region initialize activity state
        if (!localBank) {
            // check we have network interface settings
            if ( (s_networkInterface == getString(R.string.no_network_interfaces)) || (s_networkInterface.isBlank()) || (!s_networkInterface.contains(":")) ) {
                setResult(MainActivity.RETURN_CODE_NO_NETWORK_INTERFACE)
                finish()
            } else {
                isNetworkBank = true
                val serverInfo: List<String> = s_networkInterface.split(":")
                serverIp = serverInfo[0]
                serverPort = serverInfo[1].toInt()
            }
        }

        if ((!loadGameState) && (isNetworkBank)) {
            // init player list for network game and not loading game state
            GlobalInstance.get().addPlayer(
                Player(
                    "Bank",
                    "#000000",
                    true
                )
            )

            if (!onlyBank) {
                GlobalInstance.get().addPlayer(
                    Player(
                        userName,
                        userNameColor,
                        false
                    )
                )
            }
        }

        if (loadGameState) {
            // loading game state
            val gameState = MainActivity.loadGameState(cacheDir)

            if (gameState != null) {
                // loading players
                for (player in gameState.players) {
                    GlobalInstance.get().addPlayer(player)
                    a_observePlayers.add(player)
                }

                // loading ledger
                for (ledger in gameState.ledger) {
                    GlobalInstance.get().addLedger(ledger, false)
                    a_observeLedger.add(ledger)
                }
            }
        }

        if (isNetworkBank) {
            // restrict bottom section to own player or bank, if it is a network game
            currentBottomIndex = getOwnPlayerIndex()

            if ((!GlobalInstance.get().b_isServer) || (onlyBank)) {
                bottomArrowLeft.isEnabled = false
                bottomArrowRight.isEnabled = false
                bottomArrowLeft.visibility = View.GONE
                bottomArrowRight.visibility = View.GONE
            }
        }
        //endregion

        // update ui
        updatePlayerUI()
        updateBottomUI()

        //region arrow click listeners
        arrowLeft.setOnClickListener {
            currentPlayerIndex = (currentPlayerIndex - 1 + GlobalInstance.get().getPlayers().size) % GlobalInstance.get().getPlayers().size
            updatePlayerUI()
        }

        arrowRight.setOnClickListener {
            currentPlayerIndex = (currentPlayerIndex + 1) % GlobalInstance.get().getPlayers().size
            updatePlayerUI()
        }

        bottomArrowLeft.setOnClickListener {
            currentBottomIndex = (currentBottomIndex - 1 + GlobalInstance.get().getPlayers().size) % GlobalInstance.get().getPlayers().size

            if ((GlobalInstance.get().b_isServer) || ((isNetworkBank) && (!onlyBank))) {
                var player = GlobalInstance.get().getPlayers()[currentBottomIndex]

                while (
                    (!player.isBank) &&
                    (
                        (!player.name.contentEquals(userName)) &&
                        (!player.color.contentEquals(userNameColor))
                    )
                ) {
                    currentBottomIndex = (currentBottomIndex - 1 + GlobalInstance.get().getPlayers().size) % GlobalInstance.get().getPlayers().size
                    player = GlobalInstance.get().getPlayers()[currentBottomIndex]
                }
            }

            updateBottomUI()
        }

        bottomArrowRight.setOnClickListener {
            currentBottomIndex = (currentBottomIndex + 1) % GlobalInstance.get().getPlayers().size

            if ((GlobalInstance.get().b_isServer) || ((isNetworkBank) && (!onlyBank))) {
                var player = GlobalInstance.get().getPlayers()[currentBottomIndex]

                while (
                    (!player.isBank) &&
                    (
                        (!player.name.contentEquals(userName)) &&
                        (!player.color.contentEquals(userNameColor))
                    )
                ) {
                    currentBottomIndex = (currentBottomIndex + 1) % GlobalInstance.get().getPlayers().size
                    player = GlobalInstance.get().getPlayers()[currentBottomIndex]
                }
            }

            updateBottomUI()
        }
        //endregion

        // touch listeners for notes
        val banknoteIds = listOf(
            R.id.note1, R.id.note5, R.id.note10, R.id.note20,
            R.id.note50, R.id.note100, R.id.note500, R.id.note1000
        )

        for (id in banknoteIds) {
            val note = findViewById<ImageView>(id)

            note.setOnTouchListener { view, event ->
                onTouchOnNote(view, event)
            }
        }

        // drag listener
        val playerSection = findViewById<View>(R.id.playerSection)
        playerSection.setOnDragListener { _, event ->
            onDragOnPlayerSection(event)
        }

        findViewById<Button>(R.id.btnOk).setOnClickListener {
            onTransferButton()
        }

        findViewById<Button>(R.id.btnCancel).setOnClickListener {
            onCancelButton()
        }

        // handle standard back button
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                /* execute anything, e.g. finish() - if nothing is here, nothing happens pushing main back button */
                onExitAction()
            }
        })

        if (GlobalInstance.get().b_isServer) {
            Log.i(TAG, "created game as '$userName' with network interface '$serverIp:$serverPort'")
        } else {
            Log.i(TAG, "joined game as '$userName' with network interface '$serverIp:$serverPort'")
        }

        // start lobby server
        if ((GlobalInstance.get().b_isServer) && (GlobalInstance.get().o_communicationLobby == null)) {
            /*val wifi = getSystemService(WIFI_SERVICE) as WifiManager
            val multicastLock = wifi.createMulticastLock("multicastLockServer")
            multicastLock.setReferenceCounted(true)
            multicastLock.acquire()*/

            Other().netLobby(
                GlobalInstance.get().getPreferences()["udp_multicast_ip"].toString(), //MainActivity.UDP_MULTICAST_IP,
                Integer.parseInt(GlobalInstance.get().getPreferences()["udp_multicast_port"].toString()), //MainActivity.UDP_MULTICAST_PORT,
                Integer.parseInt(GlobalInstance.get().getPreferences()["udp_multicast_ttl"].toString()), //MainActivity.UDP_MULTICAST_TTL,
                gameName,
                serverIp,
                serverPort
            )
        }

        // start network communication for bank
        if ((isNetworkBank) && (GlobalInstance.get().o_communicationBank == null)) {
            Other().netBank(serverIp, serverPort)
        }

        // start ui thread to update ui every second
        uiThread = Thread { uiThreadMethod() }
        uiThread.start()

        Log.v(TAG, "onCreate $TAG")
    }

    private fun onTouchOnNote(view: View, event: android.view.MotionEvent): Boolean {
        if (event.action == android.view.MotionEvent.ACTION_DOWN) {
            val shadow = View.DragShadowBuilder(view)
            view.startDragAndDrop(null, shadow, view, 0)

            return true
        }

        return false
    }

    @SuppressLint("SetTextI18n")
    private fun onDragOnPlayerSection(event: DragEvent): Boolean {
        when (event.action) {
            DragEvent.ACTION_DROP -> {
                try {
                    // get dragged money note and it's value
                    val v = event.localState as ImageView
                    val value = v.contentDescription.toString().toInt()
                    val payer = GlobalInstance.get().getPlayers()[currentBottomIndex]

                    // check if payer has enough money (skip for bank)
                    if (!payer.isBank && calculateBalance("${payer.name}${payer.color}") < transferSum + value) {
                        errorSnackbar(message = getString(R.string.bank_player_not_enough_money, payer.name), view = findViewById(android.R.id.content), length = com.google.android.material.snackbar.Snackbar.LENGTH_SHORT)
                    } else {
                        // add money note value to transfer sum
                        transferSum += value
                        transferAmount.setText("${String.format(java.util.Locale.getDefault(), "%,d", transferSum)} ${GlobalInstance.get().getPreferences()["general_currency_text"]}")
                        arrowLeft.isEnabled = false
                        arrowRight.isEnabled = false
                        arrowLeft.visibility = View.GONE
                        arrowRight.visibility = View.GONE
                        bottomArrowLeft.isEnabled = false
                        bottomArrowRight.isEnabled = false
                        bottomArrowLeft.visibility = View.GONE
                        bottomArrowRight.visibility = View.GONE
                    }
                } catch (_: Exception) {
                    currentPlayerIndex = 0
                    currentBottomIndex = getOwnPlayerIndex()
                    onCancelButton()
                }
            }
        }

        return true
    }

    @SuppressLint("RestrictedApi")
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.bank_menu, menu)

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
            R.id.mI_ledger -> {
                showLedger()

                return true
            }

            R.id.mI_exit -> {
                onExitAction()

                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun uiThreadMethod() {
        var count = 0

        while (true) {
            try {
                runOnUiThread {
                    // update section ui's
                    updatePlayerUI()
                    updateBottomUI()

                    // event message all 6 seconds
                    if ((count != 0) && (count % 6 == 0)) {
                        val eventMessage = q_events.poll()

                        if ((eventMessage != null) && ((GlobalInstance.get().getPreferences()["general_show_events"] as Boolean))) {
                            Util.customSnackbar(
                                message = eventMessage,
                                view = findViewById(android.R.id.content),
                                length = com.google.android.material.snackbar.Snackbar.LENGTH_SHORT,
                                textColor = "#D3D3D3".toColorInt(),
                                backgroundColor = "#272757".toColorInt(),
                                actionButton = false
                            )
                        }
                    }

                    // update ping if activated and network
                    if ((GlobalInstance.get().getPreferences()["general_show_ping"] as Boolean) && (isNetworkBank) && (!localBank)) {
                        val s_foo = "${GlobalInstance.get().getPing()} ms"
                        ping.text = s_foo
                        ping.visibility = View.VISIBLE
                    } else {
                        ping.visibility = View.GONE
                    }
                }

                // save game state all 10 seconds
                if ((count != 0) && (count % 10 == 0)) {
                    saveGameState()
                }

                // remove inactive players
                if (GlobalInstance.get().b_isServer) {
                    for (activePlayer in GlobalInstance.get().getActivePlayers()) {
                        if (activePlayer.value.isBefore(java.time.LocalDateTime.now().withNano(0).minusSeconds(120 + 30))) {
                            GlobalInstance.get().removeActivePlayer(activePlayer.key)
                            GlobalInstance.get().removePlayer(activePlayer.key)
                        }
                    }
                }

                // check if player joined or left
                val a_players = GlobalInstance.get().getPlayers()

                if (a_players.size > a_observePlayers.size) {
                    for (player in a_players) {
                        if (!a_observePlayers.contains(player)) {
                            if (!player.name.lowercase().contentEquals("bank")) {
                                q_events.add(getString(R.string.bank_event_player_joined, player.name))
                            }

                            a_observePlayers.add(player)
                        }
                    }
                }

                if (a_players.size < a_observePlayers.size) {
                    for (player in a_observePlayers) {
                        if (!a_players.contains(player)) {
                            if (!player.name.lowercase().contentEquals("bank")) {
                                q_events.add(getString(R.string.bank_event_player_left, player.name))
                            }

                            a_observePlayers.remove(player)
                        }
                    }
                }

                // check if ledger changed
                val a_ledger = GlobalInstance.get().getLedger()

                if (a_ledger.size > a_observeLedger.size) {
                    val ledgerEntry = a_ledger[a_observeLedger.size]

                    if (a_ledger.size - a_observeLedger.size < 2) {
                        if (ledgerEntry.tid > 0) {
                            q_events.add(
                                getString(
                                    R.string.bank_event_transfer,
                                    ledgerEntry.tid,
                                    ledgerEntry.from.substring(0, ledgerEntry.from.indexOf("#")),
                                    String.format(java.util.Locale.getDefault(), "%,d", ledgerEntry.amount),
                                    GlobalInstance.get().getPreferences()["general_currency_text"],
                                    ledgerEntry.to.substring(0, ledgerEntry.to.indexOf("#"))
                                )
                            )
                        }
                    }

                    a_observeLedger.add(a_ledger[a_observeLedger.size])
                }

                // check for closed server flag
                if (GlobalInstance.get().b_serverClosed) {
                    // delete game state
                    MainActivity.deleteGameState(cacheDir)

                    setResult(MainActivity.RETURN_CODE_OTHER_EXIT)
                    finish()
                    break
                }

                Thread.sleep(GlobalInstance.get().getPreferences()["communication_wait"].toString().toLong())
            } catch (_: InterruptedException) {
                break
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                count++
            }
        }
    }

    private fun saveGameState() {
        val file = java.io.File(cacheDir, MainActivity.GAME_STATE_FILENAME)

        try {
            val fos = java.io.FileOutputStream(file)
            fos.channel.truncate(0)
            fos.close()

            file.printWriter().use { out ->
                out.println( net.forestany.forestj.lib.Helper.toISO8601UTC(java.time.LocalDateTime.now().withNano(0)))

                if (isNetworkBank) {
                    if (GlobalInstance.get().b_isServer) {
                        out.println("server")
                    } else {
                        out.println("client")
                    }
                } else {
                    out.println("local")
                }

                val players = GlobalInstance.get().getPlayers()
                val ledger = GlobalInstance.get().getLedger()

                out.println("${gameName}|${serverIp}|${serverPort}")
                out.println("${userName}|${userNameColor}|${onlyBank}")
                out.println("players|")

                for (player in players) {
                    out.println("${player.name}|${player.color}|${player.isBank}")
                }

                out.println("ledger|")

                for (ledgerEntry in ledger) {
                    if (ledgerEntry.tid > 0) {
                        out.println(ledgerEntry.toString())
                    }
                }
            }
        } catch (_: java.io.IOException) {
            throw Exception(getString(R.string.main_error_access_game_state, MainActivity.GAME_STATE_FILENAME))
        }
    }

    private fun onExitAction() {
        val builder = AlertDialog.Builder(this@BankActivity, R.style.AlertDialogStyle)
            .setTitle(getString(R.string.bank_exit_title))
            .setMessage(getString(R.string.bank_exit_message))
            .setPositiveButton(getString(R.string.text_yes)) { dialog, _ ->
                // only if this is a network game
                if (isNetworkBank) {
                    if (GlobalInstance.get().b_isServer) {
                        // add exit message for clients
                        GlobalInstance.get().addLedger(Message(-1), false)
                        Thread.sleep(5000)
                    } else {
                        // add exit message for server
                        GlobalInstance.get().enqueueMessageBox(Message(-1, "${GlobalInstance.get().s_user}${GlobalInstance.get().s_userColor}").toString())
                    }
                }

                // delete game state
                MainActivity.deleteGameState(cacheDir)

                setResult(MainActivity.RETURN_CODE_OWN_EXIT)
                finish()

                dialog.dismiss()
            }
            .setNegativeButton(
                getString(R.string.text_no)
            ) { dialog, _ ->
                dialog.dismiss()
            }

        val alert = builder.create()
        alert.show()
    }

    @SuppressLint("SetTextI18n")
    private fun onTransferButton() {
        try {
            // get payer and receiver
            val payer = GlobalInstance.get().getPlayers()[currentBottomIndex]
            val receiver = GlobalInstance.get().getPlayers()[currentPlayerIndex]

            // check if payer has enough money for the transfer
            if (!payer.isBank && calculateBalance("${payer.name}${payer.color}") < transferSum) {
                errorSnackbar(message = getString(R.string.bank_player_not_enough_money, payer.name), view = findViewById(android.R.id.content), length = com.google.android.material.snackbar.Snackbar.LENGTH_SHORT)
            } else {
                // check if payer is not transfer money to itself
                if (payer.name.contentEquals(receiver.name)) {
                    errorSnackbar(message = getString(R.string.bank_player_not_transfer_to_yourself), view = findViewById(android.R.id.content), length = com.google.android.material.snackbar.Snackbar.LENGTH_SHORT)
                } else {
                    if ( (GlobalInstance.get().b_isServer) || (!isNetworkBank) ) {
                        // add transfer to ledger and update ui
                        GlobalInstance.get().addLedger(Message(from = "${payer.name}${payer.color}", to = "${receiver.name}${receiver.color}", amount = transferSum))
                        updatePlayerUI()
                        updateBottomUI()
                    } else {
                        // add transfer message for server
                        GlobalInstance.get().enqueueMessageBox(Message(tid = GlobalInstance.get().getLedger().size + 1, from = "${payer.name}${payer.color}", to = "${receiver.name}${receiver.color}", amount = transferSum).toString())
                    }
                }

                transferSum = 0
                transferAmount.setText("0 ${GlobalInstance.get().getPreferences()["general_currency_text"]}")
                arrowLeft.isEnabled = true
                arrowRight.isEnabled = true
                arrowLeft.visibility = View.VISIBLE
                arrowRight.visibility = View.VISIBLE

                if ( ((GlobalInstance.get().b_isServer) && (!onlyBank)) || (!isNetworkBank) ) {
                    bottomArrowLeft.isEnabled = true
                    bottomArrowRight.isEnabled = true
                    bottomArrowLeft.visibility = View.VISIBLE
                    bottomArrowRight.visibility = View.VISIBLE
                }
            }
        } catch (_: Exception) {
            currentPlayerIndex = 0
            currentBottomIndex = getOwnPlayerIndex()
            onCancelButton()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun onCancelButton() {
        transferSum = 0
        transferAmount.setText("0 ${GlobalInstance.get().getPreferences()["general_currency_text"]}")
        arrowLeft.isEnabled = true
        arrowRight.isEnabled = true
        arrowLeft.visibility = View.VISIBLE
        arrowRight.visibility = View.VISIBLE

        if ( ((GlobalInstance.get().b_isServer) && (!onlyBank)) || (!isNetworkBank) ) {
            bottomArrowLeft.isEnabled = true
            bottomArrowRight.isEnabled = true
            bottomArrowLeft.visibility = View.VISIBLE
            bottomArrowRight.visibility = View.VISIBLE
        }
    }

    private fun updatePlayerUI() {
        try {
            val player = GlobalInstance.get().getPlayers()[currentPlayerIndex]

            playerIcon.setColorFilter(player.color.toColorInt())
            playerName.text = player.name
            playerMoney.text = if (player.isBank) "∞" else "${String.format(java.util.Locale.getDefault(), "%,d", calculateBalance("${player.name}${player.color}"))} ${GlobalInstance.get().getPreferences()["general_currency_text"]}"
        } catch (_: Exception) {
            currentPlayerIndex = 0
            currentBottomIndex = getOwnPlayerIndex()
            onCancelButton()
        }
    }

    private fun updateBottomUI() {
        try {
            val player = GlobalInstance.get().getPlayers()[currentBottomIndex]
            bottomPlayerName.text = player.name
            bottomPlayerBalance.text = if (player.isBank) "∞" else "${String.format(java.util.Locale.getDefault(), "%,d", calculateBalance("${player.name}${player.color}"))} ${GlobalInstance.get().getPreferences()["general_currency_text"]}"

            val baseGray = "#DDDDDD".toColorInt()

            if (player.isBank) {
                // bank stays gray
                findViewById<View>(R.id.bottomPlayerSection).setBackgroundColor(baseGray)
            } else {
                // blend player color with gray
                val playerColor = player.color.toColorInt()
                val blended = blendColors(baseGray, playerColor, 0.33f) // 33% tint
                findViewById<View>(R.id.bottomPlayerSection).setBackgroundColor(blended)
            }
        } catch (_: Exception) {
            currentPlayerIndex = 0
            currentBottomIndex = getOwnPlayerIndex()
            onCancelButton()
        }
    }

    private fun calculateBalance(playerWithColor: String): Int {
        var foo = 0

        for (ledgerEntry in GlobalInstance.get().getLedger()) {
            if (ledgerEntry.tid < 0) {
                continue
            }

            if (ledgerEntry.from.contentEquals(playerWithColor)) {
                foo -= ledgerEntry.amount
            } else if (ledgerEntry.to.contentEquals(playerWithColor)) {
                foo += ledgerEntry.amount
            }
        }

        return foo
    }

    private fun getOwnPlayerIndex(): Int {
        var i = 0

        for (player in GlobalInstance.get().getPlayers()) {
            if (onlyBank) {
                if (player.isBank) {
                    break
                }
            } else if ((player.name.contentEquals(userName)) && (player.color.contentEquals(userNameColor)) && (!player.isBank)) {
                break
            }

            i++
        }

        return i
    }

    private fun blendColors(baseColor: Int, overlayColor: Int, ratio: Float): Int {
        val inverseRatio = 1f - ratio
        val r = Color.red(baseColor) * inverseRatio + Color.red(overlayColor) * ratio
        val g = Color.green(baseColor) * inverseRatio + Color.green(overlayColor) * ratio
        val b = Color.blue(baseColor) * inverseRatio + Color.blue(overlayColor) * ratio
        return Color.rgb(r.toInt(), g.toInt(), b.toInt())
    }

    private fun showLedger() {
        val dialog = BottomSheetDialog(this)
        val view = View.inflate(this, R.layout.bottom_sheet_dialog_ledger, null)

        val a_foo = GlobalInstance.get().getLedger()
        a_foo.sortByDescending { it.tid }
        val ledgerEntries = mutableListOf<Message>()

        for (ledgerEntry in a_foo) {
            ledgerEntries.add(Message(ledgerEntry.tid, ledgerEntry.from, ledgerEntry.to, ledgerEntry.amount))
        }

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewFilterItems)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = LedgerEntryAdapter(ledgerEntries)

        dialog.setContentView(view)
        dialog.show()
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

        if ((GlobalInstance.get().b_isServer) && (GlobalInstance.get().o_communicationLobby != null)) {
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
        }

        if (GlobalInstance.get().o_communicationBank != null) {
            if (GlobalInstance.get().b_isServer) {
                Thread.sleep(3000)
            } else {
                Thread.sleep(1500)
            }
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

        GlobalInstance.get().b_serverClosed = false
        GlobalInstance.get().b_isServer = false
        GlobalInstance.get().s_user = "NO_USER_SPECIFIED"
        GlobalInstance.get().s_userColor = "NO_USER_COLOR_SPECIFIED"
        GlobalInstance.get().clearPlayers()
        GlobalInstance.get().clearActivePlayers()
        GlobalInstance.get().clearLedger()
        GlobalInstance.get().clearMessageBox()

        Log.v(TAG, "onDestroy $TAG")
    }
}
