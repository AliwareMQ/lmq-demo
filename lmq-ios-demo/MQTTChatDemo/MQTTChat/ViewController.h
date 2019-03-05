//
//  ViewController.h
//  MQTTChat
//
//  Created by Christoph Krey on 12.07.15.
// Copyright © 2014-2016  Owntracks. All rights reserved.
//

#import <UIKit/UIKit.h>

/*
 * MQTTClient: imports
 * MQTTSessionManager.h is optional
 */
#import <MQTTClient/MQTTClient.h>
#import <MQTTClient/MQTTSessionManager.h>

/*
 * MQTTClient: using your main view controller as the MQTTSessionManagerDelegate
 */
@interface ViewController : UIViewController <MQTTSessionManagerDelegate, UITableViewDataSource, UITableViewDelegate, UITextFieldDelegate>


@end

