#pragma once

#import <Foundation/Foundation.h>
	
void sound_define(NSURL* file, char symbol, double volume);
void sound_start(int speed, NSString* measure);
void sound_stop();
