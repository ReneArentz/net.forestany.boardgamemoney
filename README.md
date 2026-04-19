# Board Game Money Android Project

A digital money manager for board games - handle all transactions easily without paper cash.  

*net.forestany.boardgamemoney*  
*must use JDK 21 for compilation*

---

## Description

This app replaces traditional paper money used in board games with a simple, intuitive digital system. Each player can choose a unique color and manage their own funds directly within the app. One player can act as the bank, or a single device can handle both the bank and all players together - perfect for shared tablets or pass-and-play setups.

You can connect to one another in two ways:
- Automatic discovery: Find available game rooms automatically using UDP multicast over local wifi.
- Manual connection: Enter the known IP address and port of another device in the local wifi to connect directly.

Players can transfer money to each other or to the bank with just a few taps, making it effortless to track balances and transactions throughout the game. The app automatically keeps a full ledger of all transfers, so everyone can see exactly what happened at any time - no confusion, no lost notes, no counting errors.

You can run a complete game locally without any internet or network connection. Whether you're playing with friends around the same table or just want to simplify your next board game night, this app makes managing game money fast, fair, and fun.

Built with Android (Java 21 + Kotlin) and powered by the developer's own lightweight framework, forestJ.

---

## Tech Stack

- Android: Tested Android 36, Minimum Android 27
- Language: Kotlin, Java 21
- Framework: [forestJ](https://github.com/ReneArentz/forestJ)  
- IDE: Android Studio Narwhal for Linux | 2025.1.1 Patch 1
- IDE: Android Studio Panda 2 for Linux | 2025.3.2

---

## License

This project is open source under the GNU GPL v3 license — feel free to host, modify, and improve it while maintaining attribution.