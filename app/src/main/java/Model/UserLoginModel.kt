package Model

class UserLoginModel {
    var UserCode: String? =null
    var Password: String? =null

    constructor()
    constructor(UserCode: String?, Password: String?) {
        this.UserCode = UserCode
        this.Password = Password
    }
}