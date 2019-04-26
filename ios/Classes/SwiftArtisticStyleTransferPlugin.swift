import Flutter
import UIKit

public class SwiftArtisticStyleTransferPlugin: NSObject, FlutterPlugin {
  public static func register(with registrar: FlutterPluginRegistrar) {
    let channel = FlutterMethodChannel(name: "artistic_style_transfer", binaryMessenger: registrar.messenger())
    let instance = SwiftArtisticStyleTransferPlugin()
    registrar.addMethodCallDelegate(instance, channel: channel)
  }

  public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
    result("iOS " + UIDevice.current.systemVersion)
  }
}
