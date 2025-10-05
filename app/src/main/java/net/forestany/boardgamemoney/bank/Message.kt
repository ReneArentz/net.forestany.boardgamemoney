package net.forestany.boardgamemoney.bank

class Message(
    var tid: Int = 0,
    var from: String = "",
    var to: String = "",
    var amount: Int = 0
) {
    override fun toString(): String {
        return "$tid:$from:$to:$amount"
    }
}