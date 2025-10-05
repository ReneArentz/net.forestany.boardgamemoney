package net.forestany.boardgamemoney.bank

data class Player(
    val name: String,
    val color: String,
    val isBank: Boolean = false
)