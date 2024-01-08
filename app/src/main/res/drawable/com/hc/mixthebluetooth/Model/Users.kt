package Model

class Users {
    var Username: String? =null
    var UserCode: String? =null
    var Fullname: String? =null
    var Password: String? =null
    var LocationID: Int? =null
    var LevelID: Int? =null

    constructor()
    constructor(UserCode: String?, Password: String?) {
        this.UserCode = UserCode
        this.Password = Password
    }

    constructor(
        Username: String?,
        UserCode: String?,
        Fullname: String?,
        Password: String?,
        LocationID: Int?,
        LevelID: Int?
    ) {
        this.Username = Username
        this.UserCode = UserCode
        this.Fullname = Fullname
        this.Password = Password
        this.LocationID = LocationID
        this.LevelID = LevelID
    }

}