call setJavaHome.bat
echo Using JAVA_HOME=%JAVA_HOME%
"%JAVA_HOME%\bin\java.exe" -cp plugins/org.thanlwinsoft.doccharconvert_1.0.0.jar org.thanlwinsoft.doccharconvert.CommandLine --converters configuration/org.thanlwinsoft.doccharconvert/Converters %1 %2 %3 %4 %5 %6 %7 %8 %9