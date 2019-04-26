#import "ArtisticStyleTransferPlugin.h"
#import <artistic_style_transfer/artistic_style_transfer-Swift.h>

@implementation ArtisticStyleTransferPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftArtisticStyleTransferPlugin registerWithRegistrar:registrar];
}
@end
