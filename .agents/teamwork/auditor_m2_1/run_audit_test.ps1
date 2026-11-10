$ErrorActionPreference = "Stop"
$env:GRADLE_USER_HOME = "D:\.gradle"
$env:ANDROID_USER_HOME = "D:\.android"
$env:ANDROID_HOME = "D:\Android\sdk"
$env:ANDROID_SDK_ROOT = "D:\Android\sdk"
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot"

Set-Location "D:\projects\hostel_gains"

& .\gradlew.bat --gradle-user-home "D:\.gradle" testDebugUnitTest
