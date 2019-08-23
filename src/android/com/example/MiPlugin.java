/**
 */
package com.example;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.widget.Toast;
import android.util.Log;
import com.bxl.BXLConst;
import com.bxl.config.editor.BXLConfigLoader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import jpos.JposConst;
import jpos.JposException;
import jpos.POSPrinter;
import jpos.POSPrinterConst;
import jpos.config.JposEntry;
import jpos.events.ErrorEvent;
import jpos.events.ErrorListener;
import jpos.events.OutputCompleteEvent;
import jpos.events.OutputCompleteListener;
import jpos.events.StatusUpdateEvent;
import jpos.events.StatusUpdateListener;

public class MiPlugin extends CordovaPlugin {
  private static final String TAG = "MiPlugin";
  private BXLConfigLoader bxlConfigLoader;
  private POSPrinter posPrinter;
  private String logicalName;
  Context context;

  private int brightness = 50;
  private int compress = 1;

  public void initialize(CordovaInterface cordova, CordovaWebView webView) {
    super.initialize(cordova, webView);

    Log.d(TAG, "Inicializando MiPlugin");
  }

  public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
    if (action.equals("saludar")) {
      // An example of returning data back to the web layer
      String phrase = args.getString(0);
      // Echo back the first argument
      final PluginResult result = new PluginResult(PluginResult.Status.OK, "Hola todo el... " + phrase);
      callbackContext.sendPluginResult(result);
    } else if (action.equals("listBT")) {
      String name = args.getString(0);
      listBT(callbackContext,name);
      return true;
    }
    return true;
  }

  private void coolMethod(String message, CallbackContext callbackContext) {
    if (message != null && message.length() > 0) {
      callbackContext.success(message);
    } else {
      callbackContext.error("Expected one non-empty string argument.");
    }
  }

  void listBT(CallbackContext callbackContext, String name) {
    BluetoothAdapter mBluetoothAdapter = null;
    String errMsg = null;
    try {
      if (name) {
        callbackContext.success(name);
      } else {
        callbackContext.error("NO BLUETOOTH DEVICE FOUND");
      }
    } catch (Exception e) {
      errMsg = e.getMessage();
      Log.e(LOG_TAG, errMsg);
      e.printStackTrace();
      callbackContext.error(errMsg);
    }
  }

  private boolean start(final Context context) {
    this.context = context;
    // example
    String name = "SICU-151";
    String address = "74:F0:7D:E6:29:F6";

    bxlConfigLoader = new BXLConfigLoader(context);
    try {
      bxlConfigLoader.openFile();
    } catch (Exception e) {
      e.printStackTrace();
      bxlConfigLoader.newFile();
    }
    posPrinter = new POSPrinter(context);

    posPrinter.addErrorListener(new ErrorListener() {
      @Override
      public void errorOccurred(final ErrorEvent errorEvent) {
        Activity activity = (Activity) context;
        activity.runOnUiThread(new Runnable() {

          @Override
          public void run() {

            Toast.makeText(context, "Error status : " + getERMessage(errorEvent.getErrorCodeExtended()),
                Toast.LENGTH_SHORT).show();

            if (getERMessage(errorEvent.getErrorCodeExtended()).equals("Power off")) {
              try {
                posPrinter.close();
              } catch (JposException e) {
                e.printStackTrace();
                Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
              }
              // port-close
            } else if (getERMessage(errorEvent.getErrorCodeExtended()).equals("Cover open")) {
              // re-print
            } else if (getERMessage(errorEvent.getErrorCodeExtended()).equals("Paper empty")) {
              // re-print
            }
          }
        });
      }
    });
    posPrinter.addOutputCompleteListener(new OutputCompleteListener() {
      @Override
      public void outputCompleteOccurred(final OutputCompleteEvent outputCompleteEvent) {
        Activity activity = (Activity) context;
        activity.runOnUiThread(new Runnable() {
          @Override
          public void run() {
            Toast.makeText(context, "complete print", Toast.LENGTH_SHORT).show();
          }
        });
      }
    });
    posPrinter.addStatusUpdateListener(new StatusUpdateListener() {
      @Override
      public void statusUpdateOccurred(final StatusUpdateEvent statusUpdateEvent) {
        Activity activity = (Activity) context;
        activity.runOnUiThread(new Runnable() {

          @Override
          public void run() {
            Toast.makeText(context, "printer status : " + getSUEMessage(statusUpdateEvent.getStatus()),
                Toast.LENGTH_SHORT).show();

            if (getSUEMessage(statusUpdateEvent.getStatus()).equals("Power off")) {
              Toast.makeText(context, "check the printer - Power off", Toast.LENGTH_SHORT).show();
            } else if (getSUEMessage(statusUpdateEvent.getStatus()).equals("Cover Open")) {
              // display message
              Toast.makeText(context, "check the printer - Cover Open", Toast.LENGTH_SHORT).show();
            } else if (getSUEMessage(statusUpdateEvent.getStatus()).equals("Cover OK")) {
              // re-print
            } else if (getSUEMessage(statusUpdateEvent.getStatus()).equals("Receipt Paper Empty")) {
              // display message
              Toast.makeText(context, "check the printer - Receipt Paper Empty", Toast.LENGTH_SHORT).show();
            } else if (getSUEMessage(statusUpdateEvent.getStatus()).equals("Receipt Paper OK")) {
              // re-print
            }
          }
        });
      }
    });

    try {
      for (Object entry : bxlConfigLoader.getEntries()) {
        JposEntry jposEntry = (JposEntry) entry;
        bxlConfigLoader.removeEntry(jposEntry.getLogicalName());
      }
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }

    try {
      logicalName = setProductName("SICU-151");
      bxlConfigLoader.addEntry(logicalName, BXLConfigLoader.DEVICE_CATEGORY_POS_PRINTER, logicalName,
          BXLConfigLoader.DEVICE_BUS_BLUETOOTH, address);

      bxlConfigLoader.saveFile();
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }

    return true;
  }

  public void print(final Context context, String content) {

    this.context = context;
    if (start(context)) {

      try {
        posPrinter.open(logicalName);
        posPrinter.claim(0);
        posPrinter.setDeviceEnabled(true);

        String ESC = new String(new byte[] { 0x1b, 0x7c });
        String LF = "\n";

        posPrinter.setCharacterEncoding(BXLConst.CS_858_EURO);
        posPrinter.printNormal(POSPrinterConst.PTR_S_RECEIPT, content + "\n");

      } catch (JposException e) {
        e.printStackTrace();
      } finally {
        try {
          posPrinter.close();
        } catch (JposException e) {
          e.printStackTrace();
        }
      }
    }
  }

  public void printPair(final Context context, String label, String value) {
    this.context = context;
    if (start(context)) {

      try {
        posPrinter.open(logicalName);
        posPrinter.claim(0);
        posPrinter.setDeviceEnabled(true);

        posPrinter.setCharacterEncoding(BXLConst.CE_UTF8);

        String ESC = new String(new byte[] { 0x1b, 0x7c });
        String LF = "\n";

        posPrinter.printNormal(POSPrinterConst.PTR_S_RECEIPT, label + ":       " + value + LF);

      } catch (JposException e) {
        e.printStackTrace();
        Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
      } finally {
        try {
          posPrinter.close();
        } catch (JposException e) {
          e.printStackTrace();
        }
      }
    }
  }

  public void printImage(final Context context, String path) {
    Log.v("RSULT:", path);
    this.context = context;
    if (start(context)) {
      if (openPrinter()) {
        InputStream is = null;
        try {
          ByteBuffer buffer = ByteBuffer.allocate(4);
          buffer.put((byte) POSPrinterConst.PTR_S_RECEIPT);
          buffer.put((byte) brightness);
          buffer.put((byte) compress);
          buffer.put((byte) 0x00);
          Log.v("inputstream", "buffer");
          posPrinter.printBitmap(buffer.getInt(0), path, posPrinter.getRecLineWidth(), POSPrinterConst.PTR_BM_LEFT);

        } catch (JposException e) {
          e.printStackTrace();
          Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
          if (is != null) {
            Log.v("inputstream", "aqui");
            try {
              is.close();
            } catch (IOException e) {
              e.printStackTrace();
            }
          }
        }
        closePrinter();
      }
    }
  }

  private static String getERMessage(int status) {
    switch (status) {
    case POSPrinterConst.JPOS_EPTR_COVER_OPEN:
      return "Cover open";

    case POSPrinterConst.JPOS_EPTR_REC_EMPTY:
      return "Paper empty";

    case JposConst.JPOS_SUE_POWER_OFF_OFFLINE:
      return "Power off";

    default:
      return "Unknown";
    }
  }

  private static String getSUEMessage(int status) {
    switch (status) {
    case JposConst.JPOS_SUE_POWER_ONLINE:
      return "Power on";

    case JposConst.JPOS_SUE_POWER_OFF_OFFLINE:
      return "Power off";

    case POSPrinterConst.PTR_SUE_COVER_OPEN:
      return "Cover Open";

    case POSPrinterConst.PTR_SUE_COVER_OK:
      return "Cover OK";

    case POSPrinterConst.PTR_SUE_REC_EMPTY:
      return "Receipt Paper Empty";

    case POSPrinterConst.PTR_SUE_REC_NEAREMPTY:
      return "Receipt Paper Near Empty";

    case POSPrinterConst.PTR_SUE_REC_PAPEROK:
      return "Receipt Paper OK";

    case POSPrinterConst.PTR_SUE_IDLE:
      return "Printer Idle";

    default:
      return "Unknown";
    }
  }

  private String setProductName(String name) {

    String productName = BXLConfigLoader.PRODUCT_NAME_SPP_R310;

    return productName;
  }

  private boolean openPrinter() {
    Log.v("inputstream", "apen");
    try {
      posPrinter.open(logicalName);
      posPrinter.claim(0);
      posPrinter.setDeviceEnabled(true);
      return true;
    } catch (JposException e) {
      e.printStackTrace();

      try {
        posPrinter.close();
      } catch (JposException e1) {
        e1.printStackTrace();
      }
    }
    return false;
  }

  private void closePrinter() {
    try {
      posPrinter.close();
    } catch (JposException e) {
      e.printStackTrace();
    }
  }

}
