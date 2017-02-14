package kz.kaznu.bluelock;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by c0rp on 2/13/17.
 */

public class Utils {

    public static final Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss")
            .create();
    private static final Type type = new TypeToken<HashSet<BluePayDevice>>() {
    }.getType();

    public static Set<BluePayDevice> saveDevicesToDB(final Context context,
                                                     final Set<BluePayDevice> devices) {
        final SharedPreferences
                pref = context.getSharedPreferences(Constants.BLUE_LOCK, Context.MODE_PRIVATE);

        final String serializedDevices = gson.toJson(devices);
        final SharedPreferences.Editor editor = pref.edit();
        editor.putString(Constants.DEVICE_LIST, serializedDevices)
                .commit();

        return devices;
    }

    public static Set<BluePayDevice> loadDevicesToDB(final Context context) {
        final SharedPreferences
                pref = context.getSharedPreferences(Constants.BLUE_LOCK, Context.MODE_PRIVATE);

        final String string = pref.getString(Constants.DEVICE_LIST, "");

        final Set<BluePayDevice> devices = gson.fromJson(string, type);

        if (devices == null) {
            return new HashSet<>();
        }
        return devices;
    }

    public static Set<BluePayDevice> addOneDeviceToDB(final Context context,
                                                      final BluePayDevice device) {
        final Set<BluePayDevice> devices = loadDevicesToDB(context);

        if (devices.contains(device)) {
            devices.remove(device);
        }
        devices.add(device);

        return saveDevicesToDB(context, devices);
    }
}
