package Model

class UserLoginResultModel {
    var Id: Int? =null
    var UserCode: String? =null
    var UserName: String? =null
    var FullName: String? =null
    var LevelId: Int? =null
    var LocationId: String? =null


    constructor()
    constructor(Id: Int?, UserCode: String?,UserName: String?, FullName: String?,LevelId: Int?, LocationId: String?) {
        this.Id = Id
        this.UserCode = UserCode
        this.UserName = UserName
        this.FullName = FullName
        this.LevelId = LevelId
        this.LocationId = LocationId
    }
}