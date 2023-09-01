package Remote.UserPermissions;

import android.content.Context;
import android.view.View;

import com.bos.wms.mlkit.app.Logger;
import com.bos.wms.mlkit.storage.Storage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import Remote.APIClient;
import Remote.BasicApi;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import retrofit2.HttpException;

/**
 * Please ensure you edit the AppVersion variable and AppName variable before using this class
 */
public class UserPermissions {

    /**
     * This will be used to validate that the user is using the latest app version
     */
    public static String AppVersion = "2.1.9";

    /**
     * This will let the backend know the app name of the current application
     */
    public static String AppName = "WMSApp";

    /**
     * This Will Be Used In Authorizing APIS Later On
     */
    public static String AuthToken = "EMPTY_TOKEN";

    private static List<UserPermissionListener> listeners = null;

    private static List<UserPermissionErrorListener> errorListeners = null;

    private static List<String> allPermissions = null;

    private static Context applicationContext = null;

    private static boolean permissionsAvailable = false, permissionsReceived = false;

    public static boolean GotPermissionError = false;

    private static String latestPermissionError = "";

    /**
     * Initializes the user permissions and creates a new list for the listeners to know when permissions are received
     * @param userID the users id using the application
     */
    public static void Initialize(Context context, int userID){
        listeners = new ArrayList<>();
        errorListeners = new ArrayList<>();
        allPermissions = new ArrayList<>();
        applicationContext = context;
        RequestUserPermissions(userID);
    }

    /**
     * Requests the user permissions from the GetPermissions Api
     * @param userID
     */
    public static void RequestUserPermissions(int userID){
        try {

            allPermissions.clear();

            Storage mStorage = new Storage(applicationContext);
            String IPAddress = mStorage.getDataString("IPAddress", "192.168.10.82");
            BasicApi api = APIClient.getNewInstanceStatic(IPAddress).create(BasicApi.class);
            CompositeDisposable compositeDisposable = new CompositeDisposable();
            compositeDisposable.addAll(
                    api.GetPermissions(AppName, AppVersion, userID)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe((s) -> {
                                permissionsReceived = true;
                                if(s != null){
                                    try{
                                        permissionsAvailable = true;
                                        allPermissions.addAll(s);
                                        for(UserPermissionListener listener : listeners){
                                            listener.onPermissionsReceived();
                                        }
                                        Logger.Debug("API", "RequestUserPermissions - Got User[" + userID + "] Permissions: " + Arrays.toString(s.toArray()));
                                    }catch(Exception ex){
                                        permissionsAvailable = false;
                                        SendError(ex.getMessage());
                                        Logger.Error("API", "RequestUserPermissions - Returned Error: " + ex.getMessage());
                                    }
                                }
                            }, (throwable) -> {
                                permissionsAvailable = false;
                                if(throwable instanceof HttpException){
                                    HttpException ex = (HttpException) throwable;
                                    String response = ex.response().errorBody().string();
                                    if(response.isEmpty()){
                                        response = "API Error Occurred";
                                    }
                                    SendError(response);
                                    Logger.Debug("API", "RequestUserPermissions - Error In HTTP Response: " + response);
                                }else {
                                    SendError(throwable.getMessage());
                                    Logger.Error("API", "RequestUserPermissions - Error In Response: " + throwable.getMessage());
                                }
                            }));
        } catch (Throwable e) {
            permissionsAvailable = false;
            SendError(e.getMessage());
            Logger.Error("API", "RequestUserPermissions - Error Connecting: " + e.getMessage());
        }
    }

    /**
     * @return true if the permissions are available and received, false if the permissions are not yet received or not available
     */
    public static boolean PermissionsAvailable(){
        return permissionsAvailable;
    }

    /**
     * Sends an error message tro all the error listeners and saves the last error message for GetLatestError()
     * @param message
     */
    public static void SendError(String message){
        GotPermissionError = true;
        latestPermissionError = message;
        if(errorListeners != null) {
            for (UserPermissionErrorListener listener : errorListeners) {
                listener.onPermissionsErrorReceived(message);
            }
        }
    }

    /**
     * Gets the latest error
     * @return
     */
    public static String GetLatestError(){
        GotPermissionError = false;
        return latestPermissionError;
    }

    /**
     * @return true if the permissions are received, false if the permissions are not yet received
     */
    public static boolean PermissionsReceived(){
        return permissionsReceived;
    }

    /**
     * Adds a listener to trigger when the permissions are received and everything was successful
     * @param listener
     */
    public static void AddOnReceiveListener(UserPermissionListener listener){
        listeners.add(listener);
    }

    /**
     * Adds a listener to trigger when the permissions are received with an error
     * @param listener
     */
    public static void AddOnErrorListener(UserPermissionErrorListener listener){
        errorListeners.add(listener);
    }

    /**
     * Checks if the current user has a permission
     * @param permission The permission string to check
     * @return true if the permission is granted, false if the permission in not granted
     */
    public static boolean HasPermission(String permission){
        return allPermissions != null ? allPermissions.contains(permission) || allPermissions.contains("ADMIN.EVERYTHING") : false;
    }

    /**
     * This function returns if the user has the permission and also takes a view to hide or show according to the permission
     * @param permission the permission to check
     * @param view the view to hide/show
     * @return returns if the user has a permission or not
     */
    public static boolean ValidatePermission(String permission, View view){
        try {
            if (HasPermission(permission)) {
                view.post(new Runnable() {
                    public void run() {
                        view.setVisibility(View.VISIBLE);
                    }
                });
                return true;
            } else {
                view.post(new Runnable() {
                    public void run() {
                        view.setVisibility(View.GONE);
                    }
                });
                return false;
            }
        }catch(Exception ex){
            Logger.Error("VIEWError", "ValidatePermission - Returned Error " + ex.getMessage());
            return false;
        }
    }

}
