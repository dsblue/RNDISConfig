package com.dsblue.rndisconfig.rndis;

import android.content.Context;
import android.util.Log;

import java.io.*;

/**
 * Created by nathan on 8/12/15.
 */
public class NetworkManager {

    private static final String INTERFACE = "rndis0";

    private final File usbTetherStart;
    private final File usbTetherStop;

    private String originalMode = "mtp,adb";

    private final Context appContext;     // Needed to access resources from APK

    public NetworkManager(Context context) {
        appContext = context;
        usbTetherStart = loadScript("usb_tether_start.sh");
        usbTetherStop = loadScript("usb_tether_stop.sh");
    }

    public boolean isRNDISMode() {
        String mode = getPropString("sys.usb.config");
        return (mode.toLowerCase().contains("rndis"));
    }

    public void startRNDIS(String ip, String mask) {
        String mode = getPropString("sys.usb.config");

        if (!mode.toLowerCase().contains("rndis")) {
            originalMode = mode;
        }

        execCommandLine("sh " +
                usbTetherStart.getAbsolutePath() +
                " " + ip + "/" + mask +
                " " + INTERFACE);
    }

    public void stopRNDIS() {
        execCommandLine("sh " +
                usbTetherStop.getAbsolutePath() +
                " " + originalMode +
                " " + INTERFACE);
    }

    public void probeDHCP() {
        execCommandLine("netcfg " + INTERFACE + " dhcp");
    }

    private String getPropString (String prop) {
        Runtime rt = Runtime.getRuntime();
        Process proc;
        String ret = null;
        try {
            proc = rt.exec("getprop " + prop);
            ret = fromStream(proc.getInputStream());

            ret = ret.replaceAll("\\r|\\n", "");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ret;
    }

    private String fromStream(InputStream in) throws IOException
    {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        StringBuilder out = new StringBuilder();
        String newLine = System.getProperty("line.separator");
        String line;
        while ((line = reader.readLine()) != null) {
            out.append(line);
            out.append(newLine);
        }
        return out.toString();
    }

    private void execCommandLine(String command)
    {
        Runtime runtime = Runtime.getRuntime();
        Process proc = null;
        OutputStreamWriter osw = null;

        try
        {
            proc = runtime.exec("su");
            osw = new OutputStreamWriter(proc.getOutputStream());
            osw.write(command);
            osw.flush();
            osw.close();
        }
        catch (IOException ex)
        {
            Log.e("execCommandLine()", "Command resulted in an IO Exception: " + command);
            return;
        }
        finally
        {
            if (osw != null)
            {
                try
                {
                    osw.close();
                }
                catch (IOException e){}
            }
        }

        try
        {
            proc.waitFor();
        }
        catch (InterruptedException e){}

        if (proc.exitValue() != 0)
        {
            Log.e("execCommandLine()", "Command returned error: " + command + "\n  Exit code: " + proc.exitValue());
        }
    }

    private File loadScript(String filename) {

        File f = new File(appContext.getCacheDir() + "/" + filename);
        try {
            InputStream is = appContext.getAssets().open(filename);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();

            FileOutputStream fos = new FileOutputStream(f);
            fos.write(buffer);
            fos.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return f;
    }
}
