@import XCTest;
@import integration_test;

// This file replaces the one-liner INTEGRATION_TEST_IOS_RUNNER macro so we
// can add a UIInterruptionMonitor.  Without it, native iOS system permission
// dialogs (Bluetooth, Local Network, etc.) block the test and can never be
// dismissed by Flutter's find.text() which only searches Flutter widgets.
//
// The interruption monitor fires whenever a springboard-level alert appears
// during the test and automatically taps the first "accept" button it finds.

@interface RunnerTests : XCTestCase
@end

@implementation RunnerTests

+ (XCTestSuite *)defaultTestSuite {
    return [IntegrationTestIosRunner defaultTestSuiteForIntegrationTestRunner:self];
}

- (void)setUp {
    [super setUp];

    // Automatically accept system permission dialogs so that Ditto's
    // Bluetooth and Local Network transports can initialise during tests.
    // "OK"  – Bluetooth usage alert
    // "Allow" / "Allow While Using App" – Local Network usage alert
    [self addUIInterruptionMonitorWithDescription:@"System Permission Alert"
                                         handler:^BOOL(XCUIElement *alert) {
        NSArray<NSString *> *acceptLabels = @[
            @"OK",
            @"Allow",
            @"Allow While Using App"
        ];
        for (NSString *label in acceptLabels) {
            XCUIElement *button = alert.buttons[label];
            if (button.exists) {
                [button tap];
                return YES;
            }
        }
        return NO;
    }];
}

@end
