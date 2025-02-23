package org.thoughtcrime.securesms.util;


import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import org.signal.core.util.logging.Log;

public class PlayServicesUtil {

  private static final String TAG = Log.tag(PlayServicesUtil.class);

  public enum PlayServicesStatus {
    SUCCESS,
    DISABLED,
    MISSING,
    NEEDS_UPDATE,
    TRANSIENT_ERROR
  }

  public static @NonNull PlayServicesStatus getPlayServicesStatus(Context context) {
    int gcmStatus = 0;

    try {
      gcmStatus = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context);
    } catch (Throwable t) {
      Log.w(TAG, t);
      return PlayServicesStatus.MISSING;
    }

    Log.i(TAG, "Play Services: " + gcmStatus);

    switch (gcmStatus) {
      case ConnectionResult.SUCCESS:
        return PlayServicesStatus.SUCCESS;
      case ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED:
        if (!isGooglePlayPackageEnabled(context)) {
          return PlayServicesStatus.MISSING;
        }
        return PlayServicesStatus.NEEDS_UPDATE;
      case ConnectionResult.SERVICE_DISABLED:
        return PlayServicesStatus.DISABLED;
      case ConnectionResult.SERVICE_MISSING:
      case ConnectionResult.SERVICE_INVALID:
      case ConnectionResult.API_UNAVAILABLE:
      case ConnectionResult.SERVICE_MISSING_PERMISSION:
        return PlayServicesStatus.MISSING;
      default:
        return PlayServicesStatus.TRANSIENT_ERROR;
    }
  }

  public static boolean isGooglePlayPackageEnabled(Context context) {
    try {
      ApplicationInfo applicationInfo = context.getPackageManager().getApplicationInfo("com.google.android.gms", 0);
      return applicationInfo.enabled;
    } catch (PackageManager.NameNotFoundException e) {
      Log.w(TAG, e);
      return false;
    }
  }
}
