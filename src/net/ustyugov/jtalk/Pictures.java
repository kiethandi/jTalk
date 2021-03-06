package net.ustyugov.jtalk;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.preference.PreferenceManager;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ImageSpan;
import android.util.DisplayMetrics;
import net.ustyugov.jtalk.view.MyTextView;

import java.io.*;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Pictures {
    static Pattern linkPattern = Pattern.compile("https?://[a-z0-9\\-\\.]+[a-z]{2,}/.+\\.(png|jpg|jpeg|gif)[^\\s\\n]*", Pattern.CASE_INSENSITIVE);

    public static void loadPicture(final Activity activity, final String jid, final SpannableStringBuilder ssb, final MyTextView tv) {
        Matcher m = linkPattern.matcher(ssb);
        while (m.find()) {
            final String url = ssb.subSequence(m.start(), m.end()).toString();
            String file = url.substring(url.lastIndexOf("/")+1, url.length());
            final String fname = Constants.PATH + "Pictures/" + file;

            if (!new File(fname).exists()) {
                new Thread() {
                    @Override
                    public void run() {
                        try {
                            File folder = new File(Constants.PATH + "Pictures/");
                            if (!folder.exists()) folder.mkdirs();

                            BufferedInputStream in = new BufferedInputStream(new URL(url).openStream());
                            FileOutputStream fout = new FileOutputStream(fname);

                            final byte data[] = new byte[1024];
                            int count;
                            while ((count = in.read(data, 0, 1024)) != -1) {
                                fout.write(data, 0, count);
                            }
                            in.close();
                            fout.close();

                            activity.sendBroadcast(new Intent(Constants.PRESENCE_CHANGED).putExtra("jid", jid));
                        } catch (Exception ignored) { }
                    }
                }.start();
            } else {
                DisplayMetrics metrics = new DisplayMetrics();
                activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
                float scaleWidth = metrics.scaledDensity;
                float scaleHeight = metrics.scaledDensity;

                Matrix matrix = new Matrix();
                matrix.postScale(scaleWidth, scaleHeight);

                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
                int sidebar = prefs.getInt("SideBarSize", 100);

                Bitmap bitmap = BitmapFactory.decodeFile(fname);
                if (bitmap == null) return;
                bitmap.setDensity(metrics.densityDpi);
                int maxWidth = metrics.widthPixels - sidebar;
                int width = bitmap.getWidth();
                if (width > maxWidth)  {
                    double k = (double)width/(double)maxWidth;
                    int h = (int) (bitmap.getHeight()/k);
                    bitmap = Bitmap.createScaledBitmap(bitmap, maxWidth, h, true);
                }

                ssb.insert(m.end(), "\np\n");
                ssb.setSpan(new ImageSpan(activity, bitmap, ImageSpan.ALIGN_BASELINE), m.end()+1, m.end()+2, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                tv.setText(ssb);
            }
        }
    }
}
