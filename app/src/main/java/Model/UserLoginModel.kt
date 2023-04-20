package Model

class UserLoginModel {
    var UserCode: String? =null
    var Password: String? =null
    var AppName: String? = null
    var AppVersion: String? = null

    constructor()
    constructor(UserCode: String?, Password: String?, AppName: String?, AppVersion: String?) {
        this.UserCode = UserCode
        this.Password = Password
        this.AppVersion = AppVersion
        this.AppName = AppName
    }
}