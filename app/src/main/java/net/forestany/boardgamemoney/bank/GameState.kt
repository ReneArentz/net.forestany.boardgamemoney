package net.forestany.boardgamemoney.bank

class GameState {
    var timestamp: java.time.LocalDateTime? = null
    var userType: String = ""
    var gameName: String = ""
    var serverIp: String = ""
    var serverPort: String = ""
    var userName: String = ""
    var userNameColor: String = ""
    var onlyBank: Boolean = false
    val players: MutableList<Player> = mutableListOf()
    val ledger: MutableList<Message> = mutableListOf()
}