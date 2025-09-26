using UITests.iOS;

Console.WriteLine("🚀 Starting BrowserStack Test Runner...");

var testClass = new iOSTaskSearchTests();

try
{
    // Run OneTimeSetUp
    testClass.OneTimeSetUp();
    Console.WriteLine("✅ OneTimeSetUp completed");

    // Run SetUp
    testClass.SetUp();
    Console.WriteLine("✅ SetUp completed");

    // Run the test
    testClass.CanFindTaskByTitle();
    Console.WriteLine("✅ Test passed!");
}
catch (Exception ex)
{
    Console.WriteLine($"❌ Test failed: {ex.Message}");
    Console.WriteLine($"Stack trace: {ex.StackTrace}");
    return 1;
}
finally
{
    try
    {
        testClass.TearDown();
        Console.WriteLine("✅ TearDown completed");
    }
    catch (Exception ex)
    {
        Console.WriteLine($"⚠️ Teardown warning: {ex.Message}");
    }
}

Console.WriteLine("🎉 Test completed successfully!");
return 0;