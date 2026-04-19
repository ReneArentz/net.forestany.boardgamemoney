package net.forestany.boardgamemoney

import net.forestany.boardgamemoney.bank.Message
import net.forestany.boardgamemoney.bank.Player

class Other {
    @Throws(java.lang.Exception::class)
    private fun getCommunicationConfig(
        p_s_currentDirectory: String,
        p_e_comType: net.forestany.forestj.lib.net.sock.com.Type,
        p_e_comCardinality: net.forestany.forestj.lib.net.sock.com.Cardinality,
        p_s_host: String,
        p_i_port: Int,
        p_s_localHost: String,
        p_i_localPort: Int,
        p_b_symmetricSecurity128: Boolean,
        p_b_symmetricSecurity256: Boolean,
        p_b_asymmetricSecurity: Boolean,
        p_b_highSecurity: Boolean,
        p_b_securityTrustAll: Boolean,
        p_b_useMarshalling: Boolean,
        p_b_useMarshallingWholeObject: Boolean,
        p_i_marshallingDataLengthInBytes: Int,
        p_b_marshallingUsePropertyMethods: Boolean,
        p_b_marshallingSystemUsesLittleEndian: Boolean
    ): net.forestany.forestj.lib.net.sock.com.Config {
        val s_resourcesDirectory = p_s_currentDirectory + net.forestany.forestj.lib.io.File.DIR + "certs" + net.forestany.forestj.lib.io.File.DIR

        if ( (p_b_asymmetricSecurity) && (!net.forestany.forestj.lib.io.File.folderExists(s_resourcesDirectory)) ) {
            throw Exception("cannot find directory '$s_resourcesDirectory' where files are needed for asymmetric security communication")
        } else if ((p_b_asymmetricSecurity) && (p_b_securityTrustAll)) {
            System.setProperty("javax.net.ssl.trustStore", s_resourcesDirectory + "all/TrustStore-all.p12")
            System.setProperty("javax.net.ssl.trustStorePassword", "123456")
        }

        val i_comAmount = 1
        val i_comMessageBoxLength = 1500
        val i_comSenderTimeoutMs = 5000
        val i_comReceiverTimeoutMs = 5000
        val i_comSenderIntervalMs = 25
        val i_comQueueTimeoutMs = 25
        val i_comUDPReceiveAckTimeoutMs = 300
        val i_comUDPSendAckTimeoutMs = 125
        val s_comSecretPassphrase = GlobalInstance.get().getPreferences()["tcp_common_passphrase"].toString()

        val o_communicationConfig = net.forestany.forestj.lib.net.sock.com.Config(p_e_comType, p_e_comCardinality)
        o_communicationConfig.socketReceiveType = net.forestany.forestj.lib.net.sock.recv.ReceiveType.SERVER

        if (p_e_comCardinality == net.forestany.forestj.lib.net.sock.com.Cardinality.EqualBidirectional) {
            o_communicationConfig.amountSockets = 1
            o_communicationConfig.amountMessageBoxes = 2
            o_communicationConfig.addMessageBoxLength(i_comMessageBoxLength)
            o_communicationConfig.addMessageBoxLength(i_comMessageBoxLength)
        } else {
            o_communicationConfig.amount = i_comAmount
            o_communicationConfig.addMessageBoxLength(i_comMessageBoxLength)
        }

        o_communicationConfig.addHostAndPort(java.util.AbstractMap.SimpleEntry(p_s_host, p_i_port))
        o_communicationConfig.senderTimeoutMilliseconds = i_comSenderTimeoutMs
        o_communicationConfig.receiverTimeoutMilliseconds = i_comReceiverTimeoutMs
        o_communicationConfig.senderIntervalMilliseconds = i_comSenderIntervalMs
        o_communicationConfig.queueTimeoutMilliseconds = i_comQueueTimeoutMs
        o_communicationConfig.udpReceiveAckTimeoutMilliseconds = i_comUDPReceiveAckTimeoutMs
        o_communicationConfig.udpSendAckTimeoutMilliseconds = i_comUDPSendAckTimeoutMs

        if (!net.forestany.forestj.lib.Helper.isStringEmpty(p_s_localHost)) {
            o_communicationConfig.localAddress = p_s_localHost
        }

        if (p_i_localPort > 0) {
            o_communicationConfig.localPort = p_i_localPort
        }

        if (p_b_symmetricSecurity128) {
            if (p_b_highSecurity) {
                o_communicationConfig.communicationSecurity = net.forestany.forestj.lib.net.sock.com.Security.SYMMETRIC_128_BIT_HIGH
            } else {
                o_communicationConfig.communicationSecurity = net.forestany.forestj.lib.net.sock.com.Security.SYMMETRIC_128_BIT_LOW
            }

            o_communicationConfig.commonSecretPassphrase = s_comSecretPassphrase
        } else if (p_b_symmetricSecurity256) {
            if (p_b_highSecurity) {
                o_communicationConfig.communicationSecurity = net.forestany.forestj.lib.net.sock.com.Security.SYMMETRIC_256_BIT_HIGH
            } else {
                o_communicationConfig.communicationSecurity = net.forestany.forestj.lib.net.sock.com.Security.SYMMETRIC_256_BIT_LOW
            }

            o_communicationConfig.commonSecretPassphrase = s_comSecretPassphrase
        } else if (p_b_asymmetricSecurity) {
            if (p_e_comType == net.forestany.forestj.lib.net.sock.com.Type.UDP_RECEIVE || p_e_comType == net.forestany.forestj.lib.net.sock.com.Type.UDP_RECEIVE_WITH_ACK || p_e_comType == net.forestany.forestj.lib.net.sock.com.Type.TCP_RECEIVE || p_e_comType == net.forestany.forestj.lib.net.sock.com.Type.TCP_RECEIVE_WITH_ANSWER) {
                o_communicationConfig.addSSLContextToList(
                    net.forestany.forestj.lib.Cryptography.createSSLContextWithOneCertificate(
                        s_resourcesDirectory + "server/KeyStore-srv.p12",
                        "123456",
                        "test_server2"
                    )
                )
                o_communicationConfig.communicationSecurity = net.forestany.forestj.lib.net.sock.com.Security.ASYMMETRIC
            } else {
                if (!p_b_securityTrustAll) {
                    o_communicationConfig.setTrustStoreProperties(
                        s_resourcesDirectory + "client/TrustStore-clt.p12",
                        "123456"
                    )
                } else {
                    o_communicationConfig.addSSLContextToList(
                        net.forestany.forestj.lib.Cryptography.createSSLContextWithOneCertificate(
                            s_resourcesDirectory + "client/KeyStore-clt.p12",
                            "123456",
                            "test_client"
                        )
                    )
                }

                o_communicationConfig.communicationSecurity = net.forestany.forestj.lib.net.sock.com.Security.ASYMMETRIC
            }
        }

        o_communicationConfig.useMarshalling = p_b_useMarshalling
        o_communicationConfig.useMarshallingWholeObject = p_b_useMarshallingWholeObject
        o_communicationConfig.marshallingDataLengthInBytes = p_i_marshallingDataLengthInBytes
        o_communicationConfig.marshallingUsePropertyMethods = p_b_marshallingUsePropertyMethods
        o_communicationConfig.marshallingSystemUsesLittleEndian = p_b_marshallingSystemUsesLittleEndian

        o_communicationConfig.debugNetworkTrafficOn = false

        return o_communicationConfig
    }

    @Throws(java.lang.Exception::class)
    fun netLobby(
        udpMulticastIp: String,
        udpMulticastPort: Int,
        udpMulticastTTL: Int,
        gameName: String,
        localIp: String,
        serverPort: Int
    ) {
        if (GlobalInstance.get().o_communicationLobby != null) {
            try {
                GlobalInstance.get().o_communicationLobby?.stop()
            } catch (_: Exception) {

            }
        }

        GlobalInstance.get().o_communicationLobby = null

        val b_symmetricSecurity128 = false
        val b_symmetricSecurity256 = (GlobalInstance.get().getPreferences()["tcp_encryption"] as Boolean)
        val b_asymmetricSecurity = false
        val b_highSecurity = false
        val b_securityTrustAll = false

        val b_useMarshalling = true
        val b_useMarshallingWholeObject = false
        val i_marshallingDataLengthInBytes = 2
        val b_marshallingUsePropertyMethods = false
        val b_marshallingSystemUsesLittleEndian = false

        try {
            /* interrupt and null thread lobby if it is still running */
            if (GlobalInstance.get().o_threadLobby != null) {
                try {
                    GlobalInstance.get().o_threadLobby?.interrupt()
                    GlobalInstance.get().o_threadLobby = null
                } catch (_: Exception) {

                }
            }

            if (GlobalInstance.get().b_isServer) { /* SERVER */
                val e_type = net.forestany.forestj.lib.net.sock.com.Type.UDP_MULTICAST_SENDER
                val o_communicationConfig = getCommunicationConfig(
                    "/",
                    e_type,
                    net.forestany.forestj.lib.net.sock.com.Cardinality.Equal,
                    udpMulticastIp,
                    udpMulticastPort,
                    "",
                    0,
                    b_symmetricSecurity128,
                    b_symmetricSecurity256,
                    b_asymmetricSecurity,
                    b_highSecurity,
                    b_securityTrustAll,
                    b_useMarshalling,
                    b_useMarshallingWholeObject,
                    i_marshallingDataLengthInBytes,
                    b_marshallingUsePropertyMethods,
                    b_marshallingSystemUsesLittleEndian
                )
                o_communicationConfig.udpMulticastSenderTTL = udpMulticastTTL
                GlobalInstance.get().o_communicationLobby = net.forestany.forestj.lib.net.sock.com.Communication(o_communicationConfig)
                GlobalInstance.get().o_communicationLobby?.start()

                GlobalInstance.get().o_threadLobby = object : Thread() {
                    override fun run() {
                        try {
                            while (true) {
                                while (!GlobalInstance.get().o_communicationLobby?.enqueue( ("$gameName|$localIp:$serverPort") )!!) {
                                    net.forestany.forestj.lib.Global.ilog("could not enqueue message")
                                }

                                net.forestany.forestj.lib.Global.ilog("message enqueued: '$gameName|$localIp:$serverPort'")
                                sleep(1000)
                            }
                        } catch (o_exc: java.lang.Exception) {
                            net.forestany.forestj.lib.Global.logException(o_exc)
                        }
                    }
                }

                GlobalInstance.get().o_threadLobby?.start()
            }
            else /* CLIENT */
            {
                val e_type = net.forestany.forestj.lib.net.sock.com.Type.UDP_MULTICAST_RECEIVER
                val o_communicationConfig = getCommunicationConfig(
                    "/",
                    e_type,
                    net.forestany.forestj.lib.net.sock.com.Cardinality.Equal,
                    udpMulticastIp,
                    udpMulticastPort,
                    "",
                    0,
                    b_symmetricSecurity128,
                    b_symmetricSecurity256,
                    b_asymmetricSecurity,
                    b_highSecurity,
                    b_securityTrustAll,
                    b_useMarshalling,
                    b_useMarshallingWholeObject,
                    i_marshallingDataLengthInBytes,
                    b_marshallingUsePropertyMethods,
                    b_marshallingSystemUsesLittleEndian
                )
                o_communicationConfig.udpMulticastReceiverNetworkInterfaceName = GlobalInstance.get().getPreferences()["udp_network_interface_name"].toString()
                GlobalInstance.get().o_communicationLobby = net.forestany.forestj.lib.net.sock.com.Communication(o_communicationConfig)
                GlobalInstance.get().o_communicationLobby?.start()

                GlobalInstance.get().o_threadLobby = object : Thread() {
                    override fun run() {
                        try {
                            while (true) {
                                val a_deleteEntries: MutableList<java.time.LocalDateTime> = ArrayList()

                                for ((o_key) in GlobalInstance.get().getClientLobbyEntries()) {
                                    if (java.time.Duration.between( o_key, java.time.LocalDateTime.now() ).seconds > 30) {
                                        a_deleteEntries.add(o_key)
                                    }
                                }

                                if (a_deleteEntries.isNotEmpty()) {
                                    for (o_key in a_deleteEntries) {
                                        GlobalInstance.get().removeClientLobbyEntry(o_key)
                                    }
                                }

                                var s_connectionInfo: String?

                                do {
                                    s_connectionInfo = GlobalInstance.get().o_communicationLobby?.dequeue() as String?

                                    if (s_connectionInfo != null) {
                                        net.forestany.forestj.lib.Global.ilog("message received: '$s_connectionInfo'")

                                        if (!s_connectionInfo.contains(":")) {
                                            continue
                                        }

                                        val i_readingPort = s_connectionInfo.split(":".toRegex())
                                            .dropLastWhile { it.isEmpty() }
                                            .toTypedArray()[1].toInt()

                                        if (i_readingPort != serverPort) {
                                            continue
                                        }

                                        if (!GlobalInstance.get().getClientLobbyEntries().containsValue(s_connectionInfo)) {
                                            GlobalInstance.get().addClientLobbyEntry( java.time.LocalDateTime.now(), s_connectionInfo )
                                        }
                                    }
                                } while (s_connectionInfo != null)

                                sleep(1000)
                            }
                        } catch (o_exc: java.lang.Exception) {
                            net.forestany.forestj.lib.Global.logException(o_exc)
                        }
                    }
                }

                GlobalInstance.get().o_threadLobby?.start()
            }
        } catch (o_exc: java.lang.Exception) {
            net.forestany.forestj.lib.Global.logException(o_exc)
        }
    }

    @Throws(java.lang.Exception::class)
    fun netBank(
        serverIp: String,
        serverPort: Int
    ) {
        if (GlobalInstance.get().o_communicationBank != null) {
            try {
                GlobalInstance.get().o_communicationBank?.stop()
                GlobalInstance.get().o_communicationBank = null
            } catch (_: java.lang.Exception) {

            }
        }

        val b_symmetricSecurity128 = false
        val b_symmetricSecurity256 = (GlobalInstance.get().getPreferences()["tcp_encryption"] as Boolean)
        val b_asymmetricSecurity = false
        val b_highSecurity = false
        val b_securityTrustAll = false

        val b_useMarshalling = true
        val b_useMarshallingWholeObject = false
        val i_marshallingDataLengthInBytes = 2
        val b_marshallingUsePropertyMethods = false
        val b_marshallingSystemUsesLittleEndian = false

        try {
            if (GlobalInstance.get().b_isServer) { /* SERVER */
                val e_type = net.forestany.forestj.lib.net.sock.com.Type.TCP_RECEIVE_WITH_ANSWER
                val o_communicationConfig = getCommunicationConfig(
                    "/",
                    e_type,
                    net.forestany.forestj.lib.net.sock.com.Cardinality.Equal,
                    serverIp,
                    serverPort,
                    "",
                    0,
                    b_symmetricSecurity128,
                    b_symmetricSecurity256,
                    b_asymmetricSecurity,
                    b_highSecurity,
                    b_securityTrustAll,
                    b_useMarshalling,
                    b_useMarshallingWholeObject,
                    i_marshallingDataLengthInBytes,
                    b_marshallingUsePropertyMethods,
                    b_marshallingSystemUsesLittleEndian
                )

                /* add receive socket task(s) */
                val o_receiveSocketTask: net.forestany.forestj.lib.net.sock.task.Task<*> = object: net.forestany.forestj.lib.net.sock.task.Task<java.net.ServerSocket?>(net.forestany.forestj.lib.net.sock.Type.TCP) {
                        override fun getSocketTaskClassType(): Class<*> {
                            return net.forestany.forestj.lib.net.sock.task.Task::class.java
                        }

                        override fun cloneFromOtherTask(p_o_sourceTask: net.forestany.forestj.lib.net.sock.task.Task<java.net.ServerSocket?>) {
                            this.cloneBasicFields(p_o_sourceTask)
                        }

                        var start = System.currentTimeMillis()

                        @Throws(java.lang.Exception::class)
                        override fun runTask() {
                            try {
                                /* update ping */
                                GlobalInstance.get().setPing(System.currentTimeMillis() - start)

                                /* get request object */
                                val s_request = this.requestObject as String?

                                /* evaluate request */
                                if (s_request != null) {
                                    net.forestany.forestj.lib.Global.ilog("message to server: '$s_request'")

                                    // client message must contain ':'
                                    if (s_request.contains(":")) {
                                        val a_foo = s_request.split(":")

                                        // client message must contain 4 parts divided by ':'
                                        if (a_foo.size == 4) {
                                            if (a_foo[0].contentEquals("0")) { // ping message, using for adding players
                                                if (a_foo[1].contains("#")) {
                                                    val s_player = a_foo[1].substring(0, a_foo[1].indexOf("#"))
                                                    val s_playerColor = a_foo[1].substring(a_foo[1].indexOf("#"))

                                                    GlobalInstance.get().addPlayer(Player(s_player, s_playerColor, false))
                                                    GlobalInstance.get().addActivePlayer(Player(s_player, s_playerColor, false))
                                                }
                                            } else if (a_foo[0].contentEquals("-1")) { // exit message
                                                if (a_foo[1].contains("#")) {
                                                    val s_player = a_foo[1].substring(0, a_foo[1].indexOf("#"))
                                                    val s_playerColor = a_foo[1].substring(a_foo[1].indexOf("#"))

                                                    GlobalInstance.get().removePlayer(Player(s_player, s_playerColor, false))
                                                }
                                            } else { // ledger entry message
                                                if ((a_foo[1].contains("#")) && (a_foo[2].contains("#"))) {
                                                    val i_tid = a_foo[0].toInt()
                                                    val s_from = a_foo[1]
                                                    val s_to = a_foo[2]
                                                    val i_amount = a_foo[3].toInt()

                                                    // add ledger entry only if it not exists, because it can come multiple times
                                                    if (!GlobalInstance.get().checkLedgerExists(Message(i_tid, s_from, s_to, i_amount))) {
                                                        GlobalInstance.get().addLedger(Message(i_tid, s_from, s_to, i_amount))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // prepare answer
                                var s_answer = ""

                                // gather all players
                                if (GlobalInstance.get().getPlayers().size > 1) {
                                    for (player in GlobalInstance.get().getPlayers()) {
                                        s_answer += "${player.name}:${player.color};"
                                    }

                                    // remove last ';'
                                    s_answer = s_answer.substring(0, s_answer.length - 1)
                                }

                                s_answer += "%"

                                // gather all ledger entries
                                if (GlobalInstance.get().getLedger().isNotEmpty()) {
                                    for (ledger in GlobalInstance.get().getLedger()) {
                                        s_answer += "${ledger};"
                                    }

                                    // remove last ';'
                                    s_answer = s_answer.substring(0, s_answer.length - 1)
                                }

                                /* set answer object */
                                this.answerObject = s_answer

                                net.forestany.forestj.lib.Global.ilog("answer set: '$s_answer'")
                            } catch (o_exc: java.lang.Exception) {
                                net.forestany.forestj.lib.Global.logException(o_exc)
                            } finally {
                                start = System.currentTimeMillis()
                            }
                        }
                    }

                o_communicationConfig.addReceiveSocketTask(o_receiveSocketTask)

                GlobalInstance.get().o_communicationBank = net.forestany.forestj.lib.net.sock.com.Communication(o_communicationConfig)
                GlobalInstance.get().o_communicationBank?.start()
            }
            else /* CLIENT */
            {
                val e_type = net.forestany.forestj.lib.net.sock.com.Type.TCP_SEND_WITH_ANSWER
                val o_communicationConfig = getCommunicationConfig(
                    "/",
                    e_type,
                    net.forestany.forestj.lib.net.sock.com.Cardinality.Equal,
                    serverIp,
                    serverPort,
                    "",
                    0,
                    b_symmetricSecurity128,
                    b_symmetricSecurity256,
                    b_asymmetricSecurity,
                    b_highSecurity,
                    b_securityTrustAll,
                    b_useMarshalling,
                    b_useMarshallingWholeObject,
                    i_marshallingDataLengthInBytes,
                    b_marshallingUsePropertyMethods,
                    b_marshallingSystemUsesLittleEndian
                )
                GlobalInstance.get().o_communicationBank = net.forestany.forestj.lib.net.sock.com.Communication(o_communicationConfig)
                GlobalInstance.get().o_communicationBank?.start()
            }

            /* interrupt and null thread bank if it is still running */
            if (GlobalInstance.get().o_threadBank != null) {
                try {
                    GlobalInstance.get().o_threadBank?.interrupt()
                    GlobalInstance.get().o_threadBank = null
                } catch (_: java.lang.Exception) {

                }
            }

            GlobalInstance.get().o_threadBank = object : Thread() {
                override fun run() {
                    try {
                        while (true) {
                            if (GlobalInstance.get().b_isServer) { /* CLIENT only */
                                break
                            }

                            // prepare request
                            var s_request = ""

                            if ( (GlobalInstance.get().getMessageBoxAmount() > 0) ) {
                                val o_message: Any? = GlobalInstance.get().currentMessage()

                                if (o_message != null) {
                                    val s_message = o_message.toString()

                                    // check if message is in ledger
                                    if (s_message.contains(":")) {
                                        val a_foo = s_message.split(":")

                                        // client message must contain 4 parts divided by ':'
                                        if (a_foo.size == 4) {
                                            val tid = a_foo[0].toInt()
                                            val from = a_foo[1]
                                            val to = a_foo[2]
                                            val amount = a_foo[3].toInt()

                                            // check if it exists in ledger we received from server as answer to our previous requests
                                            if (GlobalInstance.get().checkLedgerExists(Message(tid, from, to, amount), 0)) {
                                                // dequeue message, and skip current iteration
                                                GlobalInstance.get().dequeueMessageBox()
                                                continue
                                            }
                                        }
                                    }

                                    // use current message of queue as server request
                                    s_request = s_message
                                }
                            } else {
                                // prepare ping message
                                s_request = Message(0, "${GlobalInstance.get().s_user}${GlobalInstance.get().s_userColor}").toString()
                            }

                            // send request
                            while (!GlobalInstance.get().o_communicationBank?.enqueue(s_request)!!) {
                                net.forestany.forestj.lib.Global.ilogWarning("could not enqueue message")
                            }

                            net.forestany.forestj.lib.Global.ilog("message enqueued: '$s_request'")

                            // wait for answer (communication_wait * 3)
                            val start = System.currentTimeMillis()
                            val o_answer: Any? = GlobalInstance.get().o_communicationBank?.dequeueWithWaitLoop(GlobalInstance.get().getPreferences()["communication_wait"].toString().toInt() * 3)
                            val end = System.currentTimeMillis()
                            GlobalInstance.get().setPing(end - start)

                            if (o_answer != null) {
                                val s_answer = o_answer.toString()
                                net.forestany.forestj.lib.Global.ilog("answer from server: $s_answer")

                                // answer from server are two parts, divided by '%'
                                if (s_answer.contains("%")) {
                                    val players = s_answer.substring(0, s_answer.indexOf("%"))
                                    val ledger = s_answer.substring(s_answer.indexOf("%") + 1)

                                    // iterate all players the server tells us
                                    for (player in players.split(";")) {
                                        if (player.contains(":")) {
                                            val playerName = player.substring(0, player.indexOf(":"))
                                            val playerColor = player.substring(player.indexOf(":") + 1)

                                            // add player if it is not 'bank'
                                            if (!playerName.lowercase().contentEquals("bank")) {
                                                GlobalInstance.get().addPlayer(Player(playerName, playerColor, false))
                                            }
                                        }
                                    }

                                    GlobalInstance.get().clearLedger()

                                    // iterate all ledger entries the server tells us
                                    for (ledgerEntry in ledger.split(";")) {
                                        if (ledgerEntry.contains(":")) {
                                            val a_foo = ledgerEntry.split(":")

                                            if (a_foo.size == 4) {
                                                val tid = a_foo[0].toInt()
                                                val from = a_foo[1]
                                                val to = a_foo[2]
                                                val amount = a_foo[3].toInt()

                                                if (tid == -1) {
                                                    GlobalInstance.get().b_serverClosed = true
                                                }

                                                GlobalInstance.get().addLedger(Message(tid, from, to, amount), false)
                                            }
                                        }
                                    }
                                }
                            } else {
                                net.forestany.forestj.lib.Global.ilogWarning("could not receive any answer data")
                            }

                            sleep(GlobalInstance.get().getPreferences()["communication_wait"].toString().toLong())
                        }
                    } catch (o_exc: java.lang.Exception) {
                        net.forestany.forestj.lib.Global.logException(o_exc)
                    }
                }
            }

            GlobalInstance.get().o_threadBank?.start()
        } catch (o_exc: java.lang.Exception) {
            net.forestany.forestj.lib.Global.logException(o_exc)
        }
    }
}