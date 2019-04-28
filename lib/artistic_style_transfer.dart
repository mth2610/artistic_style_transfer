import 'dart:async';

import 'package:flutter/services.dart';

class ArtisticStyleTransfer {
  static const MethodChannel _channel =
      const MethodChannel('artistic_style_transfer');

  static Future<String> get platformVersion async {
    final String version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }

  static Future<String> styleTransfer({List<int> styles, String inputFilePath, String outputFilePath, int quality, double styleFactor}) async {
    assert(styles != null);
    assert(styles.length !=0 );
    assert(inputFilePath != null);
    assert(outputFilePath != null);
    assert(quality <= 100&& quality >0);
    assert(styleFactor != null);
    final String outFilePath = await _channel.invokeMethod(
      'styleTransfer',
        {
          'styles': styles,
          'inputFilePath': inputFilePath,
          'outputFilePath': outputFilePath,
          'quality': quality,
          'styleFactor': styleFactor,
        }
    );
    return outFilePath;
  }

}
