
#import <Cordova/CDV.h>
#import "Metronome.h"

@interface CDVEcho : CDVPlugin <MetronomeDelegate>

@property (nonatomic, retain) NSTimer *timer;
@property (nonatomic, retain) Metronome *metronome;
@property (nonatomic, retain) NSArray *callbackParams;
@property (nonatomic) CFAbsoluteTime lastTick;

@property (nonatomic, retain) AVAudioPlayer *player;

@property (nonatomic, retain, readonly) NSURL *soundUrl;

@end

@implementation CDVEcho

- (void)setBeatSpeed:(CDVInvokedUrlCommand*)command {
    NSNumber *speed = [command.arguments objectAtIndex:0];
    NSLog(@"speed: %@", speed);
    
    CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:@{}];
    [pluginResult setKeepCallbackAsBool:YES];
    
    _callbackParams = @[pluginResult, command.callbackId];
    
    // NSTimer metronome
    //    [self handleNSTimerWithSpeed:speed];
       // on scroll the beat stops!!! 
    
    //apple provided metronome with AVAudioEngine and AVAudioPlayerNode
       [self handleMetronomeWithSpeed:speed];
        // works great!

    //absolute time timer
       // [self handleAbsoluteTimeTimerWithSpeed:speed];
}

#pragma mark timer

- (void)handleNSTimerWithSpeed:(NSNumber *)speed {
    [self configPlayer];
    [_timer invalidate];
    _timer = [NSTimer scheduledTimerWithTimeInterval:(60/speed.doubleValue)
                                              target:self
                                            selector:@selector(fireWithSound)
                                            userInfo:nil
                                             repeats:YES];
}

#pragma mark absolute time

- (void)handleAbsoluteTimeTimerWithSpeed:(NSNumber *)speed {
    [self configPlayer];
    [_timer invalidate];
    _timer = [NSTimer scheduledTimerWithTimeInterval:(60/speed.doubleValue)/100
                                              target:self
                                            selector:@selector(absoluteTick:)
                                            userInfo:@{@"speed": speed}
                                             repeats:YES];
    
}

- (void)absoluteTick:(NSTimer *)timer {
    NSNumber *speed = timer.userInfo[@"speed"];
    
    CFAbsoluteTime elapsedTime = CFAbsoluteTimeGetCurrent() - _lastTick;
    double targetTime = 60/speed.doubleValue;
    if (elapsedTime > targetTime || (fabs(elapsedTime - targetTime) < 0.002)) {
        _lastTick = CFAbsoluteTimeGetCurrent();
        [self fireWithSound];
    }
    
}

#pragma mark apple

- (void)handleMetronomeWithSpeed:(NSNumber *)speed {
    if (!_metronome) {
        _metronome = [[Metronome alloc] init:self.soundUrl];
        _metronome.delegate = self;
    }
    
    [_metronome setTempo:speed.floatValue];
    
    if (!_metronome.playing) {
        [_metronome start];
    }
}

- (void)metronomeTicking:(Metronome *)metronome bar:(SInt32)bar beat:(SInt32)beat {
    [self fire];
}

#pragma mark fire

- (void)fire {
    CDVPluginResult *pluginResult = _callbackParams[0];
    NSString *callbackId = _callbackParams[1];
    
    [self.commandDelegate sendPluginResult:pluginResult callbackId:callbackId];
}

- (void)fireWithSound {
    [self fire];
    [self playSound];
}

#pragma mark sound

- (void)configPlayer {
    if (!_player) {
        _player = [[AVAudioPlayer alloc] initWithContentsOfURL:self.soundUrl error:nil];
        [_player setVolume:1];
    }
}

- (void)playSound {
    [_player play];
}

- (NSURL *)soundUrl {
    return [[NSBundle mainBundle] URLForResource:@"MoreCowbell" withExtension:@"caf"];
}

@end
