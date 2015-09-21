package co.shunya.gita;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.NotificationCompat;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import co.shunya.gita.db.TrackDao;
import co.shunya.gita.db.model.Track;
import co.shunya.gita.view.activity.MainActivity;

/**
 * Created by nixit on 9/8/14.
 */
public class DownloadService extends IntentService {

    public static final String INTENT_DOWNLOAD_ID_EXTRA = "intent_download_id";

    public static final String ACTION_DOWNLOAD = "co.shunya.gita.action.download";
    public static final String TAG = DownloadService.class.getSimpleName(), ACTION_DELETE = "co.shunya.gita.action.delete";

    private static final int NOTIFICATION_ID = 123;
    private NotificationManager mNotificationManager;
    Track adhyay;
    private TrackDao trackDao;
    private Notification mNotification;
    private NotificationCompat.Builder mBuilder;
    private static final int delete_update = 1;

    public DownloadService() {
        super("MyDownLoadService");
    }

    private static Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message message) {
            if (message.what == delete_update) {
                BusProvider.getInstance().post(new DownloadUpdateClass());
                return true;
            }
            return false;
        }
    });


    @Override
    protected void onHandleIntent(Intent intent) {

        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mBuilder = new NotificationCompat.Builder(getApplicationContext());
        long id = intent.getLongExtra(INTENT_DOWNLOAD_ID_EXTRA, -1);
        if (id < 0) {
            throw new IllegalArgumentException("Must pass INTENT_DOWNLOAD_ID_EXTRA with the intent extra");
        }

        trackDao = new TrackDao(this);
        adhyay = trackDao.select(id);
        String action = intent.getAction();
        if (action.equals(ACTION_DOWNLOAD)) {
            downloadFileHelper();
            handler.sendEmptyMessage(delete_update);
        } /*else if (action.equals(ACTION_DELETE)) {
//            deleteFile();
            if (adhyay.isDownloded()) {
                File file = adhyay.getLocalFile(this);
                boolean b =file.exists();
                boolean a = file.delete();
                Log.i(TAG, "File is deleted from the SD Card " + a);
                adhyay.setDownloded(false);
                trackDao.update(adhyay.getDefaultContentValues(), adhyay.getId());

            }
            else
                Log.i(TAG, "File is already downloaded");
        }*/
        trackDao.close();
    }

    private void downloadFileHelper() {
        if (!adhyay.isDownloded()) {
            try {
                downloadFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
        }
    }


    public void downloadFile() throws IOException {
        File dir = this.getExternalFilesDir(Environment.DIRECTORY_MUSIC);
        HttpClient httpclient = new DefaultHttpClient();
        HttpGet httpget = new HttpGet(adhyay.getUrl());
        HttpResponse response = httpclient.execute(httpget);
        HttpEntity entity = response.getEntity();
        File file = new File(dir, adhyay.getFileNameFromUrl() + ".part");
        BufferedInputStream bis = new BufferedInputStream(entity.getContent());
        FileOutputStream outStream = new FileOutputStream(file);
        byte[] buff = new byte[5 * 1024];
        setUpAsForeground("Downloading started" + adhyay.getTitle());

        // Read bytes (and store them) until there is nothing more to
        // read(-1)
        long total = 0;
        long fileLength = entity.getContentLength();
        int progress = (int) (total * 100 / fileLength);
        int len;
        while ((len = bis.read(buff)) != -1) {
            total += len;
//            Item item = new Item(download.getName(), (total * 100 / fileLength), list.size(), i + 1);
            if ((int) (total * 100 / fileLength) - progress >= 1) {
                updateNotification("Downloading..." + adhyay.getTitle(), (int) (total * 100 / fileLength));
            }
            progress = (int) (total * 100 / fileLength);
//            publishProgress(item);
            outStream.write(buff, 0, len);
        }
        if (total == fileLength) {
            updateNotification("Downloading completed" + adhyay.getTitle(), (int) (total * 100 / fileLength));
            file.renameTo(new File(dir, adhyay.getFileNameFromUrl()));
            adhyay.setDownloded(true);
        }
        outStream.flush();
        outStream.close();
        bis.close();

        trackDao.update(adhyay.getDefaultContentValues(), adhyay.getId());
    }

    void updateNotification(String text, int progress) {
        mBuilder.setContentText(text);
        mBuilder.setProgress(100, progress, false);
        if (progress != 100) {
            mBuilder.setSmallIcon(android.R.drawable.stat_sys_download);
        }

        issueNotification();
    }

    /**
     * Configures service as a foreground service. A foreground service is a service that's doing
     * something the user is actively aware of (such as playing music), and must appear to the
     * user as a notification. That's why we create the notification here.
     */
    void setUpAsForeground(String text) {
        PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0,
                new Intent(getApplicationContext(), MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT);
        Bitmap icon = BitmapFactory.decodeResource(getResources(), R.drawable.gopinathji);
        mBuilder.setAutoCancel(false)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setLargeIcon(icon)
                .setContentTitle(getString(R.string.app_name_reader))
                .setContentText(text)
                .setOngoing(true)
                .setContentIntent(pi);
        issueNotification();
        startForeground(NOTIFICATION_ID, mNotification);
    }

    private void issueNotification() {
        mNotification = mBuilder.build();
        // Including the notification ID allows you to update the notification later on.
        mNotificationManager.notify(NOTIFICATION_ID, mNotification);
    }

    public static class DownloadUpdateClass {
        public boolean flag = true;
    }
}
