# Ditto .NET WinForms Quickstart App 🚀


## Prerequisites

1. Install the .NET 8 SDK from <https://dotnet.microsoft.com/en-us/download/dotnet/8.0>
2. Create an application at <https://portal.ditto.live>. Make note of the app ID and online playground token
3. Copy the `.env.sample` file at the top level of the quickstart repo to `.env` and add your app ID and online playground token.


## Documentation

- [Ditto C# .NET SDK Install Guide](https://docs.ditto.live/install-guides/c-sharp)
- [Ditto C# .NET SDK API Reference](https://software.ditto.live/dotnet/Ditto/4.11.4-rc.2/api-reference/)


## .NET Windows Forms Application 

This is a Windows Form Application is targeting [.NET 8.0](https://learn.microsoft.com/en-us/dotnet/desktop/winforms/whats-new/net80?view=netdesktop-9.0) using the new open source version of [Windows Forms](https://learn.microsoft.com/en-us/dotnet/desktop/winforms/overview/).  

This is not compatabile with the Windows version of .NET Framework (4.x) - nor is it compatable with libraries that target .NET Standard 2.0.   Ditto requires libraries target modern .NET Standard 2.1 versions. If you need support for .NET Standard 2.0, please [contact us](https://www.ditto.com/schedule-a-demo).

To run the app open the TasksApp folder in Visual Studio for Windows and select the solution file in it (DittoTaskApp.sln).  

You should be able to also use the modern .NET command line:
```
cd TasksApp 
dotnet build
dotnet run
```
