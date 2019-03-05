
//
//  ViewController.m
//  MQTTChat
//
//  Created by Christoph Krey on 12.07.15.
//  Copyright (c) 2015-2016 Owntracks. All rights reserved.
//

#import "ViewController.h"
#import "ChatCell.h"
#import <CommonCrypto/CommonHMAC.h>
@interface ViewController ()
/*
 * MQTTClient: keep a strong reference to your MQTTSessionManager here
 */
@property (strong, nonatomic) MQTTSessionManager *manager;
@property (strong, nonatomic) NSDictionary *mqttSettings;
@property (strong, nonatomic) NSString *instanceId;
@property (strong, nonatomic) NSString *rootTopic;
@property (strong, nonatomic) NSString *accessKey;
@property (strong, nonatomic) NSString *secretKey;
@property (strong, nonatomic) NSString *groupId;
@property (strong, nonatomic) NSString *clientId;
@property (nonatomic) NSInteger *qos;

@property (strong, nonatomic) NSMutableArray *chat;
@property (weak, nonatomic) IBOutlet UILabel *status;
@property (weak, nonatomic) IBOutlet UITextField *message;
@property (weak, nonatomic) IBOutlet UITableView *tableView;

@property (weak, nonatomic) IBOutlet UIButton *connect;
@property (weak, nonatomic) IBOutlet UIButton *disconnect;

@end

@implementation ViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    //从配置文件导入相关属性
    NSURL *bundleURL = [[NSBundle mainBundle] bundleURL];
    NSURL *mqttPlistUrl = [bundleURL URLByAppendingPathComponent:@"mqtt.plist"];
    self.mqttSettings = [NSDictionary dictionaryWithContentsOfURL:mqttPlistUrl];
    //实例 ID，购买后从控制台获取
    self.instanceId = self.mqttSettings[@"instanceId"];
    self.rootTopic = self.mqttSettings[@"rootTopic"];
    self.accessKey = self.mqttSettings[@"accessKey"];
    self.secretKey = self.mqttSettings[@"secretKey"];
    self.groupId = self.mqttSettings[@"groupId"];
    self.qos =[self.mqttSettings[@"qos"] integerValue];
    //cientId的生成必须遵循GroupID@@@前缀，且需要保证全局唯一
    self.clientId=[NSString stringWithFormat:@"%@@@@%@",self.groupId,@"DEVICE_001"];
    
    self.chat = [[NSMutableArray alloc] init];
    self.tableView.delegate = self;
    self.tableView.dataSource = self;
    self.tableView.estimatedRowHeight = 150;
    self.tableView.rowHeight = UITableViewAutomaticDimension;
    
    self.message.delegate = self;
    
    
    /*
     * MQTTClient: create an instance of MQTTSessionManager once and connect
     * will is set to let the broker indicate to other subscribers if the connection is lost
     */
    if (!self.manager) {
        self.manager = [[MQTTSessionManager alloc] init];
        self.manager.delegate = self;
        self.manager.subscriptions = [NSDictionary dictionaryWithObject:[NSNumber numberWithInt:self.qos]
                                                                 forKey:[NSString stringWithFormat:@"%@/IOS", self.rootTopic]];
        //password的计算方式是，使用secretkey对clientId做hmac签名算法，具体实现参考macSignWithText方法
        NSString *passWord = [[self class] macSignWithText:self.clientId secretKey:self.secretKey];
        NSString *userName = [NSString stringWithFormat:@"Signature|%@|%@",self.accessKey,self.instanceId];;
	    //此处从配置文件导入的Host即为MQTT的接入点，该接入点获取方式请参考资源申请章节文档，在控制台上申请MQTT实例，每个实例会分配一个接入点域名
        [self.manager connectTo:self.mqttSettings[@"host"]
                                  port:[self.mqttSettings[@"port"] intValue]
                                   tls:[self.mqttSettings[@"tls"] boolValue]
                             keepalive:60  //心跳间隔不得大于120s
                                 clean:true
                                  auth:true
                                  user:userName
                                  pass:passWord
                                  will:false
                             willTopic:nil
                               willMsg:nil
                               willQos:0
                        willRetainFlag:FALSE
                          withClientId:self.clientId];
    } else {
        [self.manager connectToLast];
    }
    
    /*
     * MQTTCLient: observe the MQTTSessionManager's state to display the connection status
     */
    
    [self.manager addObserver:self
                   forKeyPath:@"state"
                      options:NSKeyValueObservingOptionInitial | NSKeyValueObservingOptionNew
                      context:nil];

}

+ (NSString *)macSignWithText:(NSString *)text secretKey:(NSString *)secretKey
{
    NSData *saltData = [secretKey dataUsingEncoding:NSUTF8StringEncoding];
    NSData *paramData = [text dataUsingEncoding:NSUTF8StringEncoding];
    NSMutableData* hash = [NSMutableData dataWithLength:CC_SHA1_DIGEST_LENGTH ];
    CCHmac(kCCHmacAlgSHA1, saltData.bytes, saltData.length, paramData.bytes, paramData.length, hash.mutableBytes);
    NSString *base64Hash = [hash base64EncodedStringWithOptions:0];
    
    return base64Hash;
}

- (void)viewWillAppear:(BOOL)animated {
    [super viewWillAppear:animated];
}

- (void)viewWillDisappear:(BOOL)animated {
}

- (void)observeValueForKeyPath:(NSString *)keyPath ofObject:(id)object change:(NSDictionary *)change context:(void *)context {
    switch (self.manager.state) {
        case MQTTSessionManagerStateClosed:
            self.status.text = @"closed";
            self.disconnect.enabled = false;
            self.connect.enabled = false;
            break;
        case MQTTSessionManagerStateClosing:
            self.status.text = @"closing";
            self.disconnect.enabled = false;
            self.connect.enabled = false;
            break;
        case MQTTSessionManagerStateConnected:
            self.status.text = [NSString stringWithFormat:@"connected as %@",
                                self.clientId];
            self.disconnect.enabled = true;
            self.connect.enabled = false;
            break;
        case MQTTSessionManagerStateConnecting:
            self.status.text = @"connecting";
            self.disconnect.enabled = false;
            self.connect.enabled = false;
            break;
        case MQTTSessionManagerStateError:
            self.status.text = @"error";
            self.disconnect.enabled = false;
            self.connect.enabled = false;
            break;
        case MQTTSessionManagerStateStarting:
        default:
            self.status.text = @"not connected";
            self.disconnect.enabled = false;
            self.connect.enabled = true;
            [self.manager connectToLast];
            break;
    }
}

- (BOOL)textFieldShouldReturn:(UITextField *)textField {
    [textField resignFirstResponder];
    return YES;
}

- (IBAction)clear:(id)sender {
    [self.chat removeAllObjects];
    [self.tableView reloadData];
}
- (IBAction)connect:(id)sender {
    /*
     * MQTTClient: connect to same broker again
     */
    
    [self.manager connectToLast];
}

- (IBAction)disconnect:(id)sender {
    /*
     * MQTTClient: gracefully disconnect
     */
    [self.manager disconnect];
}

- (IBAction)send:(id)sender {
    /*
     * MQTTClient: send data to broker
     */
    
    [self.manager sendData:[self.message.text dataUsingEncoding:NSUTF8StringEncoding]
                     topic:[NSString stringWithFormat:@"%@/%@",
                            self.rootTopic,
                            @"IOS"]//此处设置多级子topic
                       qos:self.qos
                    retain:FALSE];
}

/*
 * MQTTSessionManagerDelegate
 */
- (void)handleMessage:(NSData *)data onTopic:(NSString *)topic retained:(BOOL)retained {
    /*
     * MQTTClient: process received message
     */
    
    NSString *dataString = [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];
    
    [self.chat insertObject:[NSString stringWithFormat:@"RecvMsg from Topic: %@\nBody: %@", topic, dataString] atIndex:0];
    [self.tableView reloadData];
}

/*
 * UITableViewDelegate
 */
- (BOOL)tableView:(UITableView *)tableView canEditRowAtIndexPath:(NSIndexPath *)indexPath {
    return NO;
}

- (BOOL)tableView:(UITableView *)tableView canMoveRowAtIndexPath:(NSIndexPath *)indexPath {
    return NO;
}

- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath {
    ChatCell *cell = [self.tableView dequeueReusableCellWithIdentifier:@"line"];
    cell.textView.text = self.chat[indexPath.row];
    return cell;
}

/*
 * UITableViewDataSource
 */

- (NSInteger)numberOfSectionsInTableView:(UITableView *)tableView {
    return 1;
}

- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section {
    return self.chat.count;
}

@end
