#import "HapticMetronome.h"

@interface HapticMetronome : NSObject {
	long _speed;
	long _counter;
	UIImpactFeedbackGenerator* _haptic_1;
	UIImpactFeedbackGenerator* _haptic_2;
	UIImpactFeedbackGenerator* _haptic_3;
	UINotificationFeedbackGenerator* _haptic_4;
	UISelectionFeedbackGenerator* _haptic_5;

	dispatch_time_t _start_time;
	NSString* _measure;
}
-(instancetype)init;
-(void)start:(int)speed measure:(NSString*)measure;
-(void)stop;
@end

@implementation HapticMetronome

- (instancetype)init {
	self = [super init];	
	_haptic_1 = [[UIImpactFeedbackGenerator alloc] initWithStyle:UIImpactFeedbackStyleLight];
	_haptic_2 = [[UIImpactFeedbackGenerator alloc] initWithStyle:UIImpactFeedbackStyleMedium];
	_haptic_3 = [[UIImpactFeedbackGenerator alloc] initWithStyle:UIImpactFeedbackStyleHeavy];
	_haptic_4 = [[UINotificationFeedbackGenerator alloc] init];
	_haptic_5 = [[UISelectionFeedbackGenerator alloc] init];
	return self;
}

-(void)fire_current_haptic {
	switch ([_measure characterAtIndex:_counter % [_measure length]]) {
		case 'X': [_haptic_3 impactOccurred]; [_haptic_3 impactOccurred]; [_haptic_3 impactOccurred]; break;
		case 'H': [_haptic_3 impactOccurred]; [_haptic_3 impactOccurred]; break;
		case 'M': [_haptic_2 impactOccurred]; [_haptic_2 impactOccurred]; break;
		case 'L': [_haptic_1 impactOccurred]; [_haptic_1 impactOccurred]; break;
		case 'h': [_haptic_3 impactOccurred]; break;
		case 'm': [_haptic_2 impactOccurred]; break;
		case 'l': [_haptic_1 impactOccurred]; break;
		case 'E': [_haptic_4 notificationOccurred:UINotificationFeedbackTypeError]; break;
		case 'S': [_haptic_4 notificationOccurred:UINotificationFeedbackTypeSuccess]; break;
		case 'W': [_haptic_4 notificationOccurred:UINotificationFeedbackTypeWarning]; break;
		case 'F': [_haptic_5 selectionChanged]; break;
	}
}

-(void)prepare_current_haptic {
	switch ([_measure characterAtIndex:_counter % [_measure length]]) {
		case 'X': [_haptic_3 prepare]; break;
		case 'H': [_haptic_3 prepare]; break;
		case 'M': [_haptic_2 prepare]; break;
		case 'L': [_haptic_1 prepare]; break;
		case 'h': [_haptic_3 prepare]; break;
		case 'm': [_haptic_2 prepare]; break;
		case 'l': [_haptic_1 prepare]; break;
		case 'E': [_haptic_4 prepare]; break;
		case 'S': [_haptic_4 prepare]; break;
		case 'W': [_haptic_4 prepare]; break;
		case 'F': [_haptic_5 prepare]; break;
	}
}


-(void)start:(int)speed measure:(NSString*)measure {
	_measure = measure;
	_speed = 60 * 1000000000. / speed + 0.5;
	_counter = 0;
	_start_time = dispatch_time(DISPATCH_TIME_NOW, 0);

	[self prepare_current_haptic];
	[self beat];
}

-(void)beat {
	[self fire_current_haptic];
	++_counter;
	[self prepare_current_haptic];
	
	if (_speed)
		dispatch_after(dispatch_time(_start_time, _speed * _counter), dispatch_get_main_queue(), ^{ [self beat]; });
}
-(void)stop {
	_speed = 0;
}

@end

static HapticMetronome* m() {
	static HapticMetronome* _ = [[HapticMetronome alloc] init];
    return _;
}

void haptic_start(int speed, NSString* measure) {
	[m() stop];
	[m() start:speed measure:measure];
}

void haptic_stop() {
	[m() stop];
}
