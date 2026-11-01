# R8 shrinks and obfuscates the release build. The default rules plus the libraries' own
# consumer rules cover everything here; this keeps the ViewModel constructor that
# viewModel() creates by reflection.
-keep class com.taskmaster.app.TaskViewModel { <init>(android.app.Application); }
