# VALA

VALA is a static analyser to detect inadequate resource usages in Android applications induced by variant lifecycles. Now VALA support inadequate usages in three variant lifecycles, they are：

SKIP1: When method finish() is called, entrypoint onDestroy will be invoked and the activity will be destroyed immediately after onCreate returns, skipping entrypoints like onStop.

SKIP2: When setRetainInstance(true) is called to retain the Fragment instance, entrypoints onDestroy and onCreate will be skipped whereas the other entrypoints will still be executed in the Fragment lifecycle.

SWAP1: When App is running on Android 9.0 or later, entrypoint onSaveInstanceState will be executed after, instead of before, onStop.

In the two variant lifecycles of type SKIP a few entrypoints will be skipped, in which the releasing operations of resources can be skipped and resource leaks may occur.
For the third variant, i.e., SWAP1, the execution order of entrypoints onSaveInstanceState and onStop is reversed, in which case datas designed to be stored in onSaveInstanceState and next cleared in onStop can may be lost.


## Installation

#### System Requirements

- Java: 1.8
- Android SDK: API 19 or higher (make sure `adb` and `aapt` commands are available)
- Linux: Ubuntu 16.04/Windows/CentOS
- maven: 3.6+

## Usage

VALA is implemented based on Flowdroid---one of the most famous static analysis frameworks for Android applications. VALA is absolutely open-source, As Flowdroid, VALA takes executable APK files of Android applications as its input, and the entry class of VALA is soot.jimple.infoflow.cmd.MainClass.

[Setting]
The following options can be used to specify:
-  -a:    the APK file (or the folder containing a list of APK files) to analyze
-  -p:    the path to the platforms directory from the Android SDK
-  -lpr:   the variant lifecycle ("SKIP1", "SKIP2" and "SWAP1") to detect inadequate resource usages in
-  -s:    the definition file for operations involved in resource leaks and data loss errors
-  -mm:   the definition file for methods that have specific taint rules
-  -eu:   the definition file for error usages in the variant lifecycles
-  -o:    the folder for outputed XML files 
-  -tsf:  whether to taint sub fields and the children of array/list

[Output]

The output XML files are placed in the folder specified by '-o', here is an example reporting a resource leak in lifecycle SKIP1:

```
<ErrorUsages>
  <ErrorUsage violatedPattern="SKIP1" field="<org.microg.gms.ui.AskPushPermission: org.microg.gms.gcm.GcmDatabase database>">
    <entrypoint method="onCreate">
      <oplist optype="android.database.sqlite.SQLiteOpenHelper">
        <REQUIRE stmt="specialinvoke $r0.<android.database.sqlite.SQLiteOpenHelper: void <init>(android.content.Context,java.lang.String,android.database.sqlite.SQLiteDatabase$CursorFactory,int)>($r1, "gcmstatus", null, $i0)"/>
        <EDIT stmt="$r0.<org.microg.gms.ui.AskPushPermission: org.microg.gms.gcm.GcmDatabase database> = $r3"/>
        <FINISH stmt="virtualinvoke $r0.<android.app.Activity: void finish()>()"/>
      </oplist>
    </entrypoint>
    <entrypoint method="onStop">
      <oplist optype="android.database.sqlite.SQLiteOpenHelper">
        <RELEASE stmt="virtualinvoke $r4.<android.database.sqlite.SQLiteOpenHelper: void close()>()"/>
      </oplist>
    </entrypoint>
</ErrorUsage>
```

