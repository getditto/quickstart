using UITests.iOS;

var testClass = new iOSTaskSearchTests();

try
{
    testClass.OneTimeSetUp();
    testClass.SetUp();
    testClass.CanFindTaskByTitle();
}
catch (Exception ex)
{
    Console.WriteLine($"Test failed: {ex.Message}");
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
    }
}

return 0;