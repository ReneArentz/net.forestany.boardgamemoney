package net.forestany.boardgamemoney

import java.util.LinkedList
import java.util.Queue
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import net.forestany.forestj.lib.net.sock.com.Communication
import net.forestany.boardgamemoney.bank.Message
import net.forestany.boardgamemoney.bank.Player

class GlobalInstance {
    companion object {
        @Volatile
        private var instance: GlobalInstance? = null

        fun get(): GlobalInstance {
            if (instance == null) {
                synchronized(this) {
                    if (instance == null) {
                        instance = GlobalInstance()
                    }
                }
            }

            return instance!!
        }
    }

    var o_threadLobby: Thread? = null
    var o_threadBank: Thread? = null

    var o_communicationLobby: Communication? = null
    var o_communicationBank: Communication? = null

    var b_serverClosed: Boolean = false
    var b_isServer: Boolean = false
    var s_user: String = "NO_USER_SPECIFIED"
    var s_userColor: String = "NO_USER_COLOR_SPECIFIED"

    private val o_lockPing = ReentrantLock()
    private var l_ping: Long = 0

    fun getPing(): Long {
        var l_foo: Long

        o_lockPing.withLock {
            l_foo = l_ping
        }

        return l_foo
    }

    fun setPing(p_l_value: Long) {
        o_lockPing.withLock {
            l_ping = p_l_value
        }
    }

    private var o_lockLobbyEntries = ReentrantLock()
    private var m_clientLobbyEntries: MutableMap<java.time.LocalDateTime, String> = HashMap()

    fun getClientLobbyEntries(): MutableMap<java.time.LocalDateTime, String> {
        var m_foo: MutableMap<java.time.LocalDateTime, String>

        o_lockLobbyEntries.withLock {
            m_foo = m_clientLobbyEntries.toMutableMap()
        }

        return m_foo
    }

    fun addClientLobbyEntry(p_o_value: java.time.LocalDateTime, p_s_value: String) {
        o_lockLobbyEntries.withLock {
            m_clientLobbyEntries.put(p_o_value, p_s_value)
        }
    }

    fun removeClientLobbyEntry(p_o_value: java.time.LocalDateTime) {
        o_lockLobbyEntries.withLock {
            m_clientLobbyEntries.remove(p_o_value)
        }
    }

    fun removeClientLobbyEntryByValue(p_s_value: String) {
        o_lockLobbyEntries.withLock {
            if (m_clientLobbyEntries.containsValue(p_s_value)) {
                var o_key: java.time.LocalDateTime? = null

                for ((key, value) in m_clientLobbyEntries) {
                    if (value.contentEquals(p_s_value)) {
                        o_key = key
                    }
                }

                if (o_key != null) {
                    m_clientLobbyEntries.remove(o_key)
                }
            }
        }
    }

    fun clearClientLobbyEntries() {
        o_lockLobbyEntries.withLock {
            m_clientLobbyEntries.clear()
        }
    }

    private val o_lockPreferences = ReentrantLock()
    private val m_preferences: MutableMap<String, Any?> = HashMap()

    fun getPreferences(): MutableMap<String, Any?> {
        var m_foo: MutableMap<String, Any?>

        o_lockPreferences.withLock {
            m_foo = m_preferences.toMutableMap()
        }

        return m_foo
    }

    fun addPreference(p_s_value: String, p_o_value: Any?) {
        o_lockPreferences.withLock {
            m_preferences.put(p_s_value, p_o_value)
        }
    }

    fun clearPreferences() {
        o_lockPreferences.withLock {
            m_preferences.clear()
        }
    }

    private val o_lockLedger = ReentrantLock()
    private val a_ledger: MutableList<Message> = mutableListOf()

    fun getLedger(): MutableList<Message> {
        var a_foo: MutableList<Message>

        o_lockLedger.withLock {
            a_foo = a_ledger.toMutableList()
        }

        return a_foo
    }

    fun addLedger(p_o_value: Message, autoIncrement: Boolean = true) {
        o_lockLedger.withLock {
            if (autoIncrement) {
                p_o_value.tid = a_ledger.size + 1
            }

            if (!a_ledger.contains(p_o_value)) {
                a_ledger.add(p_o_value)
            }
        }
    }

    fun checkLedgerExists(p_o_value: Message, offset: Int = 1): Boolean {
        var b_exists = false

        o_lockLedger.withLock {
            for (ledgerEntry in a_ledger) {
                if (ledgerEntry.tid < p_o_value.tid - offset) {
                    continue
                }

                if ((ledgerEntry.from.contentEquals(p_o_value.from)) && (ledgerEntry.to.contentEquals(p_o_value.to)) && (ledgerEntry.amount == p_o_value.amount)) {
                    b_exists = true
                    break
                }
            }
        }

        return b_exists
    }

    fun clearLedger() {
        o_lockLedger.withLock {
            a_ledger.clear()
        }
    }

    private val o_lockPlayers = ReentrantLock()
    private val a_players: MutableList<Player> = mutableListOf()

    fun getPlayers(): MutableList<Player> {
        var a_foo: MutableList<Player>

        o_lockPlayers.withLock {
            a_foo = a_players.toMutableList()
        }

        return a_foo
    }

    fun addPlayer(p_o_value: Player) {
        o_lockPlayers.withLock {
            if (!a_players.contains(p_o_value)) {
                a_players.add(p_o_value)
            }
        }
    }

    fun removePlayer(p_o_value: Player) {
        o_lockPlayers.withLock {
            a_players.remove(p_o_value)
        }
    }

    fun clearPlayers() {
        o_lockPlayers.withLock {
            a_players.clear()
        }
    }

    private val o_lockActivePlayers = ReentrantLock()
    private val a_activePlayers: MutableMap<Player, java.time.LocalDateTime> = mutableMapOf()

    fun getActivePlayers(): MutableMap<Player, java.time.LocalDateTime> {
        var m_foo: MutableMap<Player, java.time.LocalDateTime>

        o_lockActivePlayers.withLock {
            m_foo = a_activePlayers.toMutableMap()
        }

        return m_foo
    }

    fun addActivePlayer(p_o_value: Player) {
        o_lockActivePlayers.withLock {
            a_activePlayers.put(p_o_value, java.time.LocalDateTime.now().withNano(0))
        }
    }

    fun removeActivePlayer(p_o_value: Player) {
        o_lockActivePlayers.withLock {
            a_activePlayers.remove(p_o_value)
        }
    }

    fun clearActivePlayers() {
        o_lockActivePlayers.withLock {
            a_activePlayers.clear()
        }
    }

    private var o_lockMessageBox = ReentrantLock()
    private var q_messageBox: Queue<String> = LinkedList()

    fun enqueueMessageBox(p_o_foo: String) {
        o_lockMessageBox.withLock {
            q_messageBox.add(p_o_foo)
        }
    }

    fun dequeueMessageBox(): String? {
        var o_foo: String?

        o_lockMessageBox.withLock {
            o_foo = q_messageBox.poll()
        }

        return o_foo
    }

    fun currentMessage(): String? {
        var o_foo: String?

        o_lockMessageBox.withLock {
            o_foo = q_messageBox.peek()
        }

        return o_foo
    }

    fun getMessageBoxAmount(): Int {
        var i_foo: Int

        o_lockMessageBox.withLock {
            i_foo = q_messageBox.size
        }

        return i_foo
    }

    fun clearMessageBox() {
        o_lockMessageBox.withLock {
            do {
                q_messageBox.size
            } while (q_messageBox.poll() != null)
        }
    }
}