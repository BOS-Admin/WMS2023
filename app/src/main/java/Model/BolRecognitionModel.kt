package Model

class BolRecognitionModel {
    var DetectedText: String? = null
    var UserID: Int? = null
    var FileName: String? = null
    var DetectedBols: String? = null
    var Error: String? = null
    constructor()
    constructor(
        FileName: String?,
        DetectedText: String?,
        DetectedBols: String?,
        Error: String?,
        UserID: Int?,
    ) {
        this.DetectedText = DetectedText
        this.UserID = UserID
        this.Error = Error
        this.DetectedBols = DetectedBols
        this.FileName = FileName
    }
}