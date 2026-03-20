# Ditto .NET 4.8 WinForms Quickstart App 🚀

## Prerequisites
- Microsoft Windows 11, this uses .NET 4.8 for Windows only
- Visual Studio 2019 or higher (tested on Visual Studio 2019/2026)
- x64 version of Windows (does NOT support 32-bit Windows)
- Does not support ARM64 (however can run on Windows on Windows with x64 emulation if supported by OS/hardware).  ARM64 is not officially supported by Ditto for use with the .NET 4.8 framework.  

1. Install the .NET 4.8 SDK from <https://dotnet.microsoft.com/en-us/download/dotnet-framework/net48>
2. Create an application at <https://portal.ditto.live>. Make note of the app ID and online playground token
3. Copy the `.env.sample` file at the top level of the quickstart repo to `.env` and add your app ID and online playground token, and place file in the same folder as the solution and csproj file.


## Documentation

- [Ditto C# .NET SDK Install Guide](https://docs.ditto.live/install-guides/c-sharp)
- [Ditto C# .NET SDK API Reference](https://software.ditto.live/dotnet/Ditto/4.14.1/api-reference/)


## .NET Windows Forms Application 

This is a Windows Form Application is targeting .NET 4.8.  It will NOT run on MacOS or Linux and will not run on modern .NET.  To run the app open the TasksApp folder in Visual Studio for Windows and select the solution file in it (DittoTaskApp.sln). 