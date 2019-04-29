package com.mth2610.artistic_style_transfer;

import java.util.ArrayList;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

/** ArtisticStyleTransferPlugin */
public class ArtisticStyleTransferPlugin implements MethodCallHandler {
  /** Plugin registration. */
  private Registrar registrar;
  private StylizeActivity stylizeActivity;

  private ArtisticStyleTransferPlugin(Registrar registrar) {
    this.registrar = registrar;
    this.stylizeActivity = new StylizeActivity(registrar.context());
  }

  public static void registerWith(Registrar registrar) {
    final MethodChannel channel = new MethodChannel(registrar.messenger(), "artistic_style_transfer");
    channel.setMethodCallHandler(new ArtisticStyleTransferPlugin(registrar));
  }

  @Override
  public void onMethodCall(MethodCall call, final Result result) {
    if (call.method.equals("getPlatformVersion")) {
      result.success("Android " + android.os.Build.VERSION.RELEASE);
    } else if (call.method.equals("styleTransfer")) {
      final ArrayList<Integer> arrayListStyles = call.argument("styles");
      final Integer[] styles = arrayListStyles.toArray(new Integer[arrayListStyles.size()]);
      final String inputFilePath = call.argument("inputFilePath");
      final String outputFilePath = call.argument("outputFilePath");
      final int quality = call.argument("quality");

      final double styleFactorDouble = call.argument("styleFactor");
      final float styleFactor = (float) styleFactorDouble;
      final boolean convertToGrey = call.argument("convertToGrey");

      new Thread(new Runnable() {
        public void run() {
          try {
            final String output = stylizeActivity.styleTransfer(registrar.context(), styles, inputFilePath, outputFilePath, quality, styleFactor, convertToGrey);
            result.success(output);
          } catch (Exception e) {
            result.error("styleTransfer", "error", e.toString());
          }
        }}).start();
    }
  }
}


