#import "SoundMetronome.h"
#import <AVFoundation/AVFoundation.h>
#include <map>
#include <cmath>

template<class T>
static void reduce_volume(T*const* data, size_t channels, size_t stride, size_t frame_count, double factor) {
	if (!data)
		return;
	const size_t buffer_len = frame_count * stride;
	for (size_t c = 0; c < channels; ++c)
		for (size_t i = 0; i < buffer_len; ++i)
			data[c][i] *= factor; 
}

static void reduce_buffer_volume(AVAudioPCMBuffer* buffer, double factor) {
	reduce_volume(buffer.int16ChannelData, buffer.format.channelCount, buffer.stride, buffer.frameLength, factor);
	reduce_volume(buffer.int32ChannelData, buffer.format.channelCount, buffer.stride, buffer.frameLength, factor);
	reduce_volume(buffer.floatChannelData, buffer.format.channelCount, buffer.stride, buffer.frameLength, factor);
}

static AVAudioPCMBuffer* load_audio_buffer(NSURL* file_url, double volume = 1.0) {
		AVAudioFile *file = [[AVAudioFile alloc] initForReading: file_url error: NULL];
		AVAudioPCMBuffer* buff = [[AVAudioPCMBuffer alloc] initWithPCMFormat:file.processingFormat frameCapacity: (AVAudioFrameCount)file.length];
		[file readIntoBuffer: buff error:nil];
		
		if (volume != 1.0)
			reduce_buffer_volume(buff, volume);
		return buff;
}

//
// SoundMetronome
//
@interface SoundMetronome : NSObject {
	std::map<char, AVAudioPCMBuffer*> _buffers;
    AVAudioEngine* _engine;
    AVAudioPlayerNode* _player;
    dispatch_queue_t _sync_queue;
    NSString* _measure;
    bool _playing, _player_started;
    int32_t _beat_count, _samples_per_beat; 
}
-(instancetype)init;
-(void)dealloc;
-(void)define_sound:(NSURL*)file_url symbol:(char)symbol volume:(double)volume;
-(void)start:(int)speed measure:(NSString*)measure;
-(void)stop;

-(void)_schedule_beat;
-(AVAudioPCMBuffer*)_current_buffer;

@end

@implementation SoundMetronome

-(instancetype)init {
	self = [super init];	
	_playing = false;
	_player_started = false;
	_engine = [[AVAudioEngine alloc] init];
	_player = [[AVAudioPlayerNode alloc] init];
	[_engine attachNode: _player];
	_sync_queue = dispatch_queue_create("SoundMetronome", DISPATCH_QUEUE_SERIAL);
	return self;
}

- (void)dealloc {
    [self stop];
    [_engine detachNode:_player];
    _player = nil;
    _engine = nil;
    _buffers.clear();
}


-(void)define_sound:(NSURL*)file_url symbol:(char)symbol volume:(double)volume {
	_buffers[symbol] = load_audio_buffer(file_url, volume);
}

-(void)start:(int)speed measure:(NSString*)measure {
	if (_buffers.empty())
		return;

	_measure = measure;

	if (_playing) {
		double new_samples_per_beat = 60. / speed * _buffers.begin()->second.format.sampleRate;
		_beat_count = ceil((_beat_count * _samples_per_beat) / new_samples_per_beat);
		_samples_per_beat = new_samples_per_beat;
		return;
	}
		
	_samples_per_beat = 60. / speed * _buffers.begin()->second.format.sampleRate;

	[_engine disconnectNodeInput: _player];
	[_engine connect:_player to:_engine.outputNode fromBus:0 toBus:0 format:_buffers.begin()->second.format];
	
	if (![_engine startAndReturnError:nil]) 
		return;

	_beat_count = 0;
	_playing = true;
	
	dispatch_sync(_sync_queue, ^{
		[self _schedule_beat];
	});
}

-(void)stop {
	_playing = false;
	[_player stop];
	[_player reset];
	[_engine stop];
	_player_started = false;
}

-(AVAudioPCMBuffer*)_current_buffer {
	while (1) {
		auto it = _buffers.find([_measure characterAtIndex:_beat_count % [_measure length]]);
		if (it != _buffers.end())
			return it->second;
		++_beat_count;
	}
}

-(void)_schedule_beat {
	if (!_playing) 
		return;

	AVAudioPCMBuffer* buffer = [self _current_buffer];
	AVAudioTime* player_beat_time = [AVAudioTime timeWithSampleTime:_samples_per_beat * _beat_count atRate:_buffers.begin()->second.format.sampleRate];
	[_player scheduleBuffer:buffer atTime:player_beat_time options:0 completionHandler:^{
		dispatch_sync(self->_sync_queue, ^{
			++self->_beat_count;
			[self _schedule_beat];
		});
	}];
	
	if (!_player_started) {
		[_player play];
		_player_started = true;
	}		
}

@end

static SoundMetronome* m() {
	static SoundMetronome* _ = [[SoundMetronome alloc] init];
    return _;
}

void sound_define(NSURL* file, char symbol, double volume) {
	[m() define_sound:file symbol:symbol volume:volume];
}

void sound_start(int speed, NSString* measure) {
	[m() stop];
	[m() start:speed measure:measure];
}

void sound_stop() {
	[m() stop];
}
