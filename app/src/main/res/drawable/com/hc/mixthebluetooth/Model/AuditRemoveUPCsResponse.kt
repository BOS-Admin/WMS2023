package Model

data class AuditRemoveUPCsResponse (

   val items:List<AuditUPCsToRemove>,
   val lastUPCRemoved : String?

)

data class AuditUPCsToRemove (
    val id : Int,
    val upc : String,
    val qtyToRemove : Int,
    val qtyRemoved:Int,
    val boxLocationScoreId:Int

)
