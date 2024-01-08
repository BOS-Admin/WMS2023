package Model

data class RfidAuditResponse (

    val auditResult : String,
    val location : String,
    val result : String,
    val removeUPCs : Boolean,

    )
