using System;
using System.Reflection;

var assembly = Assembly.LoadFrom("dotnet-tui/DittoDotNetTasksConsole.Tests/bin/Debug/net9.0/DittoDotNetTasksConsole.Tests.dll");
var resources = assembly.GetManifestResourceNames();

Console.WriteLine("Available embedded resources:");
foreach (var resource in resources)
{
    Console.WriteLine($"  {resource}");
}