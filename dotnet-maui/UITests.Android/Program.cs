using UITests.Android;

// Log environment variables for debugging
var expectedTitle = Environment.GetEnvironmentVariable("EXPECTED_TASK_TITLE");
var buildName = Environment.GetEnvironmentVariable("BUILD_NAME");
if (!string.IsNullOrEmpty(expectedTitle))
    Console.WriteLine($"Expected task: {expectedTitle}");
if (!string.IsNullOrEmpty(buildName))
    Console.WriteLine($"Build: {buildName}");

var testClass = new AndroidTaskSearchTests();

try
{
    testClass.OneTimeSetUp();
    testClass.SetUp();
    testClass.CanFindTaskByTitle();
    Console.WriteLine("✅ Test passed");
    return 0;
}
catch (Exception ex)
{
    Console.WriteLine($"❌ Test failed: {ex.Message}");
    return 1;
}
finally
{
    try
    {
        testClass.TearDown();
    }
    catch
    {
        // Ignore teardown errors
    }
}