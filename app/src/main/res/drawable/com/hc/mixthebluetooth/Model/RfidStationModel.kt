package Model

data class RfidStationModel (
    val station : Station,
    val messages : List<Message>
)

data class Station (
    val id : Int,
    val stationCode : String,
    val ipaddress : String,
    val portNb : Int,
    val waitingResponseTimeinMs : Int
)

data class Message (
    val id : Int,
    val stationCode : String,
    val message : String,
    val hexCode : String
)

