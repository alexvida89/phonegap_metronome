
#import <Cordova/CDV.h>
#import "Metronome.h"
#import "HapticMetronome.h"
#import "SoundMetronome.h"

@interface CDVEcho : CDVPlugin <MetronomeDelegate>

@property (nonatomic, retain) NSTimer *timer;
@property (nonatomic, retain) Metronome *metronome;
@property (nonatomic, retain) NSArray *callbackParams;
@property (nonatomic) CFAbsoluteTime lastTick;

@property (nonatomic, retain) AVAudioPlayer *player;

@property (nonatomic, retain, readonly) NSURL *soundUrl;

@end

@implementation CDVEcho

- (void)set_haptic_speed:(NSInteger)speed {

/*  
    use haptic_start and haptic_stop to control the metronome
    haptic_start takes speed (bpm) as integer and NSString* to define measure
    
    Supported alphabet for defining measures:
        X -> tripple heavy impact
        H -> double heavy impact
        M -> double medium impact
        L -> double light impact
        h -> heavy impact
        m -> medium impact
        l -> light impact
        E -> error
        S -> success
        W -> warning
        F -> selection
          -> (space) no sound
*/

    // haptic_start(speed, @"XlHlHll"); //balkan!
}

- (void)set_sound_speed:(NSInteger)speed:(NSString*)pattern {
/*
    use sound_start and sound_stop to control the metronome 
    haptic_start takes speed (bpm) as integer and NSString* to define measure
    sound_define is used to define alphabet at runtime, it takes NSUrl* (path to file), char of the alphabet it will map to and volume (sould be between 0 and 1)
    sound_define needs to be executed only once, but it can be executed multiple times (there may be a performance penalty, but it will work)
    All sound samples must be in same format (sample rate, sample depth, stereo/mono, codec). 
*/

    //the following block executes sound_define calls once
    static dispatch_once_t once_token;
    dispatch_once (&once_token, ^{
        NSURL* s = [[NSBundle mainBundle] URLForResource:@"MoreCowbell" withExtension:@"caf"];
        sound_define(s, 'H', 1);
        sound_define(s, 'L', 0.3);
        sound_define(s, 'S', 0.1);
        sound_define(s, 'N', 0);
        sound_define(s, 'M', 0.5);
    });
    
    // sound_start(speed, @"HLMHLHL");
    sound_start(speed, pattern);
}


- (void)stopBeatSpeed{
    [self sound_stop];
}

- (void)setBeatSpeed:(CDVInvokedUrlCommand*)command {
    NSNumber *speed = [command.arguments objectAtIndex:0];
    NSString *pattern = [command.arguments objectAtIndex:1];
    NSLog(@"speed: %@", speed);
    NSLog(@"pattern: %@", pattern);

    CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:@{}];
    [pluginResult setKeepCallbackAsBool:YES];
    
    _callbackParams = @[pluginResult, command.callbackId];
    
    //sound example
    [self set_sound_speed:speed.integerValue:pattern];
    
    //haptic example
    [self set_haptic_speed:speed.integerValue];
}

- (void)fire {
    CDVPluginResult *pluginResult = _callbackParams[0];
    NSString *callbackId = _callbackParams[1];
    
    [self.commandDelegate sendPluginResult:pluginResult callbackId:callbackId];
}

@end
