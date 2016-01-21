package org.cyanogenmod.theme.util;

import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.provider.CalendarContract;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;

import static android.provider.CalendarContract.Events;

import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class PackagesUtils {

    public static ComponentName getMostUsedBrowser(Context context) {

        String url = "http://www.example.com";
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(url));

        return getMostUsedAppForIntent(context, i);

    }

    public static ComponentName getMostUsedMessenger(Context context) {

        //Choose system default sms application
        String defaultApplication = Settings.Secure.getString(context.getContentResolver(), "sms_default_application");
        PackageManager pm = context.getPackageManager();
        Intent intent = pm.getLaunchIntentForPackage(defaultApplication);

        if (intent != null) {
            return intent.getComponent();
        }

        //Fallback - just in case
        String url = "sms:123456789";
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(url));

        return getMostUsedAppForIntent(context, i);

    }

    public static ComponentName getMostUsedDialer(Context context) {

        String url = "tel:123456789";
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(url));

        return getMostUsedAppForIntent(context, i);
    }

    public static ComponentName getMostUsedCamera(Context context) {

        Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        return getMostUsedAppForIntent(context, i);
    }

    public static ComponentName getMostUsedGallery(Context context) {

        Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

        return getMostUsedAppForIntent(context, i);
    }


    public static ComponentName getMostUsedCalendar(Context context) {

        //Kanged from android dev guide
        Calendar beginTime = Calendar.getInstance();
        beginTime.set(2012, 0, 19, 7, 30);
        Calendar endTime = Calendar.getInstance();
        endTime.set(2012, 0, 19, 8, 30);
        Intent intent = new Intent(Intent.ACTION_INSERT)
                .setData(Events.CONTENT_URI)
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, beginTime.getTimeInMillis())
                .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endTime.getTimeInMillis())
                .putExtra(Events.TITLE, "Yoga")
                .putExtra(Events.DESCRIPTION, "Group class")
                .putExtra(Events.EVENT_LOCATION, "The gym")
                .putExtra(Events.AVAILABILITY, Events.AVAILABILITY_BUSY)
                .putExtra(Intent.EXTRA_EMAIL, "rowan@example.com,trevor@example.com");

        return getMostUsedAppForIntent(context, intent);

    }

    private static ComponentName getMostUsedAppForIntent(Context context, Intent intent) {

        PackageManager packageManager = context.getPackageManager();

        //First, let's check if there is default app for that

        ResolveInfo resolveInfo = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);

        if (resolveInfo != null) {
            Intent launchIntent = packageManager.getLaunchIntentForPackage(resolveInfo.activityInfo.packageName);
            if (launchIntent != null) {
                return launchIntent.getComponent();
            }
        }

        //If not choose the most used one

        List<ResolveInfo> browserPackages = packageManager.queryIntentActivities(intent,
                0);

        UsageStatsManager mUsageStatsManager = (UsageStatsManager)
                context.getSystemService("usagestats");

        Calendar yearAgo = Calendar.getInstance();
        yearAgo.add(Calendar.MONTH, -1);


        final Map<String, UsageStats> mUsageStats = mUsageStatsManager.queryAndAggregateUsageStats(yearAgo.getTimeInMillis(), System.currentTimeMillis());

        Collections.sort(browserPackages, new Comparator<ResolveInfo>() {
            @Override
            public int compare(ResolveInfo lhs, ResolveInfo rhs) {

                long lhsUsage, rhsUsage;

                if (mUsageStats.get(lhs.activityInfo.packageName) != null) {
                    lhsUsage = mUsageStats.get(lhs.activityInfo.packageName).getTotalTimeInForeground();
                } else {
                    lhsUsage = -1;
                }

                if (mUsageStats.get(rhs.activityInfo.packageName) != null) {
                    rhsUsage = mUsageStats.get(rhs.activityInfo.packageName).getTotalTimeInForeground();
                } else {
                    rhsUsage = -1;
                }

                return Long.compare(rhsUsage, lhsUsage);
            }
        });


        if (browserPackages.isEmpty()) {
            return new ComponentName("this.app", "does.not.exist");
        }

        ComponentName app = packageManager.getLaunchIntentForPackage(browserPackages.get(0).activityInfo.packageName).getComponent();

        return app;

    }

}
