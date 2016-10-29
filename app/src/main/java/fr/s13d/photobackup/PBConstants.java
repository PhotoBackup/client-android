package fr.s13d.photobackup;


/**
 * Delivers all public constants to the application.
 */
public class PBConstants {

    // should correspond to what is in preferences.xml
    public static final String PREF_SERVICE_RUNNING = "PREF_SERVICE_RUNNING";
    public static final String PREF_SERVER = "PREF_SERVER";
    public static final String PREF_WIFI_ONLY = "PREF_WIFI_ONLY";
    public static final String PREF_MEDIAS_TO_BACKUP = "PREF_MEDIAS_TO_BACKUP";
    public static final String PREF_RECENT_UPLOAD_ONLY = "PREF_RECENT_UPLOAD_ONLY";
    public static final String PREF_PICTURE_FOLDER_LIST = "PREF_PICTURE_FOLDER_LIST";
    public static final String PREF_MEDIA_BACKUP_VIDEO = "PREF_MEDIA_BACKUP_VIDEO";
    public static final String PREF_UPLOAD_JOURNAL = "PREF_UPLOAD_JOURNAL";
    public static final String PREF_ABOUT = "PREF_ABOUT";

    // origin to permission asking, to be able to reroute
    public static final int PERM_ORIGIN_SERVICE = 0;
    public static final int PERM_ORIGIN_MEDIAS = 1;


    // Prevent from constructing objects of this class
    private PBConstants() { throw new AssertionError(); }
}
