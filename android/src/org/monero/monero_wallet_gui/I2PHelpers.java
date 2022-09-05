package org.monero.monero_wallet_gui;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.text.DecimalFormat;


public class I2PHelpers {
    static String TAG = "I2P";

    public static void copyResourceToFileIfAbsent(Context ctx, int resID, String myDir, String f) {
        File file = new File(myDir, f);
        if (!file.exists())
            copyResourceToFile(ctx, resID, myDir, f);
    }

    public static void copyResourceToFile(Context ctx, int resID, String myDir, String f) {
        InputStream in = null;
        FileOutputStream out = null;

        Log.i(TAG, "Creating file " + f + " from resource");
        byte buf[] = new byte[4096];
        try {
            // Context methods
            in = ctx.getResources().openRawResource(resID);
            out = new FileOutputStream(new File(myDir, f));

            int read;
            while ( (read = in.read(buf)) != -1)
                out.write(buf, 0, read);

        } catch (IOException ioe) {
            Log.e(TAG, "copyResourceToFile" + "IOE: ", ioe);
        } catch (Resources.NotFoundException nfe) {
            Log.e(TAG, "copyResourceToFile" + "NFE: ", nfe);
        } finally {
            if (in != null) try { in.close(); } catch (IOException ioe) {
                Log.e(TAG, "copyResourceToFile" + "IOE in.close(): ", ioe);
            }
            if (out != null) try { out.close(); } catch (IOException ioe) {
                Log.e(TAG, "copyResourceToFile" + "IOE out.close(): ", ioe);
            }
        }
    }

    public static void unzipResourceToDir(Context ctx, int resID, String myDir, String folder) {
        InputStream in = null;
        FileOutputStream out = null;
        ZipInputStream zis = null;

        Log.i(TAG, "Creating files in '" + myDir + "/" + folder + "/' from resource");
        try {
            // Context methods
            in = ctx.getResources().openRawResource(resID);
            zis = new ZipInputStream((in));
            ZipEntry ze;
            while ((ze = zis.getNextEntry()) != null) {
                out = null;
                Log.i(TAG, "unzipping "+ze);
                try {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024];
                    int count;
                    while ((count = zis.read(buffer)) != -1) {
                        baos.write(buffer, 0, count);
                    }
                    String name = ze.getName();
                    File f = new File(myDir + "/" + folder +"/" + name);
                    String canonicalPath = f.getCanonicalPath().replace("/user/0/", "/data/");
                    // account for canonical path differences when using .aab bundles
                    if (!canonicalPath.startsWith(myDir.replace("/user/0/", "/data/"))) {
                        // If these don't match, there's a path-traversal possibility.
                        // So ignore it.
                        Log.e(TAG, "Path mismatch bug " + canonicalPath.toString() + " " + myDir.toString());
                    } else if (ze.isDirectory()) {
                        Log.i(TAG, "Creating directory " + myDir + "/" + folder +"/" + name + " from resource");
                        f.mkdir();
                    } else {
                        Log.i(TAG, "Creating file " + myDir + "/" + folder +"/" + name + " from resource");
                        //create all the leading directories
                        File newFile = new File(myDir+"/"+folder+"/"+name);
                        newFile.getParentFile().mkdirs();
                        byte[] bytes = baos.toByteArray();
                        out = new FileOutputStream(f);
                        out.write(bytes);
                    }
                } catch (IOException ioe) {
                    Log.e(TAG, "unzipResourceToDir" + "IOE: ", ioe);
                } finally {
                    if (out != null) {
                        try {
                            out.close();
                        } catch (IOException ioe) {
                            Log.e(TAG, "unzipResourceToDir" + "IOE: interior out.close ", ioe);
                        }
                        out = null;
                    }
                }
            }
        } catch (IOException ioe) {
            Log.e(TAG, "unzipResourceToDir" + "IOE: ", ioe);
        } catch (Resources.NotFoundException nfe) {
            Log.e(TAG, "unzipResourceToDir" + "NFE: ", nfe);
        } finally {
            if (in != null) try { in.close(); } catch (IOException ioe) {
                Log.e(TAG, "unzipResourceToDir" + "IOE: in.close() ", ioe);
            }
            if (out != null) try { out.close(); } catch (IOException ioe) {
                Log.e(TAG, "unzipResourceToDir" + "IOE: out.close() ", ioe);
            }
            if (zis != null) try { zis.close(); } catch (IOException ioe) {
                Log.e(TAG, "unzipResourceToDir" + "IOE: zis.close() ", ioe);
            }
        }
    }

    public static String formatSpeed(double size) {
        int baseScale=1;
        int scale;
        for (int i = 0; i < baseScale; i++) {
            size /= 1024.0D;
        }
        for (scale = baseScale; size >= 1024.0D; size /= 1024.0D) {
            ++scale;
        }

        // control total width
        DecimalFormat fmt;
        if (size >= 1000) {
            fmt = new DecimalFormat("#0");
        } else if (size >= 100) {
            fmt = new DecimalFormat("#0.0");
        } else {
            fmt = new DecimalFormat("#0.00");
        }

        String str = fmt.format(size);
        switch (scale) {
            case 1:
                return str + "K";
            case 2:
                return str + "M";
            case 3:
                return str + "G";
            case 4:
                return str + "T";
            case 5:
                return str + "P";
            case 6:
                return str + "E";
            case 7:
                return str + "Z";
            case 8:
                return str + "Y";
            default:
                return str + "";
        }
    }

}
