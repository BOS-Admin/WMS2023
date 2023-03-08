package Model

class BolRecognitionModel {
    var DetectedText: String? = null
    var UserID: Int? = null
    var FileName: String? = null
    lateinit var DetectedBols: Array<String?>

    constructor()
    constructor(
        DetectedText: String?,
        UserID: Int?,
        FileName: String?,
        DetectedBols: Array<String?>
    ) {
        this.DetectedText = DetectedText
        this.UserID = UserID
        this.DetectedBols = DetectedBols
        this.FileName = FileName
    }
}