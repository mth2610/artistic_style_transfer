import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:artistic_style_transfer/artistic_style_transfer.dart';

void main() {
  const MethodChannel channel = MethodChannel('artistic_style_transfer');

  setUp(() {
    channel.setMockMethodCallHandler((MethodCall methodCall) async {
      return '42';
    });
  });

  tearDown(() {
    channel.setMockMethodCallHandler(null);
  });

  test('getPlatformVersion', () async {
    expect(await ArtisticStyleTransfer.platformVersion, '42');
  });
}
