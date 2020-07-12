package soot.jimple.infoflow.resourceleak;

import soot.SootMethod;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class AndroidEntryPointConstantsCopy {
    public static final String ACTIVITYCLASS = "android.app.Activity";
    public static final String SERVICECLASS = "android.app.Service";
    public static final String GCMBASEINTENTSERVICECLASS = "com.google.android.gcm.GCMBaseIntentService";
    public static final String GCMLISTENERSERVICECLASS = "com.google.android.gms.gcm.GcmListenerService";
    public static final String BROADCASTRECEIVERCLASS = "android.content.BroadcastReceiver";
    public static final String CONTENTPROVIDERCLASS = "android.content.ContentProvider";
    public static final String APPLICATIONCLASS = "android.app.Application";
    public static final String FRAGMENTCLASS = "android.app.Fragment";
    public static final String SUPPORTFRAGMENTCLASS = "android.support.v4.app.Fragment";
    public static final String SERVICECONNECTIONINTERFACE = "android.content.ServiceConnection";

    public static final String APPCOMPATACTIVITYCLASS_V4 = "android.support.v4.app.AppCompatActivity";
    public static final String APPCOMPATACTIVITYCLASS_V7 = "android.support.v7.app.AppCompatActivity";

    //lifecycle-add 需要加入新版本的Fragment
    public static final String NEWFRAGMENTCLASS = "androidx.fragment.app.Fragment";
    public static final String NEWAPPCOMPATACTIVITYCLASS = "androidx.fragment.app.FragmentActivity";


    public static final String ACTIVITY_ONCREATE = "void onCreate(android.os.Bundle)";
    public static final String ACTIVITY_ONSTART = "void onStart()";
    public static final String ACTIVITY_ONRESTOREINSTANCESTATE = "void onRestoreInstanceState(android.os.Bundle)";
    public static final String ACTIVITY_ONPOSTCREATE = "void onPostCreate(android.os.Bundle)";
    public static final String ACTIVITY_ONRESUME = "void onResume()";
    public static final String ACTIVITY_ONPOSTRESUME = "void onPostResume()";
    public static final String ACTIVITY_ONCREATEDESCRIPTION = "java.lang.CharSequence onCreateDescription()";
    public static final String ACTIVITY_ONSAVEINSTANCESTATE = "void onSaveInstanceState(android.os.Bundle)";
    public static final String ACTIVITY_ONPAUSE = "void onPause()";
    public static final String ACTIVITY_ONSTOP = "void onStop()";
    public static final String ACTIVITY_ONRESTART = "void onRestart()";
    public static final String ACTIVITY_ONDESTROY = "void onDestroy()";
    public static final String ACTIVITY_ONATTACHFRAGMENT = "void onAttachFragment(android.app.Fragment)";

    public static final String SERVICE_ONCREATE = "void onCreate()";
    public static final String SERVICE_ONSTART1 = "void onStart(android.content.Intent,int)";
    public static final String SERVICE_ONSTART2 = "int onStartCommand(android.content.Intent,int,int)";
    public static final String SERVICE_ONBIND = "android.os.IBinder onBind(android.content.Intent)";
    public static final String SERVICE_ONREBIND = "void onRebind(android.content.Intent)";
    public static final String SERVICE_ONUNBIND = "boolean onUnbind(android.content.Intent)";
    public static final String SERVICE_ONDESTROY = "void onDestroy()";

    public static final String GCMINTENTSERVICE_ONDELETEDMESSAGES = "void onDeletedMessages(android.content.Context,int)";
    public static final String GCMINTENTSERVICE_ONERROR = "void onError(android.content.Context,java.lang.String)";
    public static final String GCMINTENTSERVICE_ONMESSAGE = "void onMessage(android.content.Context,android.content.Intent)";
    public static final String GCMINTENTSERVICE_ONRECOVERABLEERROR = "void onRecoverableError(android.content.Context,java.lang.String)";
    public static final String GCMINTENTSERVICE_ONREGISTERED = "void onRegistered(android.content.Context,java.lang.String)";
    public static final String GCMINTENTSERVICE_ONUNREGISTERED = "void onUnregistered(android.content.Context,java.lang.String)";

    public static final String GCMLISTENERSERVICE_ONDELETEDMESSAGES = "void onDeletedMessages()";
    public static final String GCMLISTENERSERVICE_ONMESSAGERECEIVED = "void onMessageReceived(java.lang.String,android.os.Bundle)";
    public static final String GCMLISTENERSERVICE_ONMESSAGESENT = "void onMessageSent(java.lang.String)";
    public static final String GCMLISTENERSERVICE_ONSENDERROR = "void onSendError(java.lang.String,java.lang.String)";

//    public static final String FRAGMENT_ONCREATE = "void onCreate(android.os.Bundle)";
//    public static final String FRAGMENT_ONATTACH = "void onAttach(android.app.Activity)";
//    public static final String FRAGMENT_ONCREATEVIEW = "android.view.View onCreateView(android.view.LayoutInflater,android.view.ViewGroup,android.os.Bundle)";
//    public static final String FRAGMENT_ONVIEWCREATED = "void onViewCreated(android.view.View,android.os.Bundle)";
//    public static final String FRAGMENT_ONSTART = "void onStart()";
//    public static final String FRAGMENT_ONACTIVITYCREATED = "void onActivityCreated(android.os.Bundle)";
//    public static final String FRAGMENT_ONVIEWSTATERESTORED = "void onViewStateRestored(android.app.Activity)";
//    public static final String FRAGMENT_ONRESUME = "void onResume()";
//    public static final String FRAGMENT_ONPAUSE = "void onPause()";
//    public static final String FRAGMENT_ONSTOP = "void onStop()";
//    public static final String FRAGMENT_ONDESTROYVIEW = "void onDestroyView()";
//    public static final String FRAGMENT_ONDESTROY = "void onDestroy()";
//    public static final String FRAGMENT_ONDETACH = "void onDetach()";
//    public static final String FRAGMENT_ONSAVEINSTANCESTATE = "void onSaveInstanceState(android.os.Bundle)";

    public static final String BROADCAST_ONRECEIVE = "void onReceive(android.content.Context,android.content.Intent)";

    public static final String CONTENTPROVIDER_ONCREATE = "boolean onCreate()";

    public static final String APPLICATION_ONCREATE = "void onCreate()";
    public static final String APPLICATION_ONTERMINATE = "void onTerminate()";

    public static final String SERVICECONNECTION_ONSERVICECONNECTED = "void onServiceConnected(android.content.ComponentName,android.os.IBinder)";
    public static final String SERVICECONNECTION_ONSERVICEDISCONNECTED = "void onServiceDisconnected(android.content.ComponentName)";

    public static final String ACTIVITYLIFECYCLECALLBACKSINTERFACE = "android.app.Application$ActivityLifecycleCallbacks";
    public static final String ACTIVITYLIFECYCLECALLBACK_ONACTIVITYSTARTED = "void onActivityStarted(android.app.Activity)";
    public static final String ACTIVITYLIFECYCLECALLBACK_ONACTIVITYSTOPPED = "void onActivityStopped(android.app.Activity)";
    public static final String ACTIVITYLIFECYCLECALLBACK_ONACTIVITYSAVEINSTANCESTATE = "void onActivitySaveInstanceState(android.app.Activity,android.os.Bundle)";
    public static final String ACTIVITYLIFECYCLECALLBACK_ONACTIVITYRESUMED = "void onActivityResumed(android.app.Activity)";
    public static final String ACTIVITYLIFECYCLECALLBACK_ONACTIVITYPAUSED = "void onActivityPaused(android.app.Activity)";
    public static final String ACTIVITYLIFECYCLECALLBACK_ONACTIVITYDESTROYED = "void onActivityDestroyed(android.app.Activity)";
    public static final String ACTIVITYLIFECYCLECALLBACK_ONACTIVITYCREATED = "void onActivityCreated(android.app.Activity,android.os.Bundle)";

    private static final String[] activityMethods = { ACTIVITY_ONCREATE, ACTIVITY_ONDESTROY, ACTIVITY_ONPAUSE,
            ACTIVITY_ONRESTART, ACTIVITY_ONRESUME, ACTIVITY_ONSTART, ACTIVITY_ONSTOP, ACTIVITY_ONSAVEINSTANCESTATE,
            ACTIVITY_ONRESTOREINSTANCESTATE, ACTIVITY_ONCREATEDESCRIPTION, ACTIVITY_ONPOSTCREATE,
            ACTIVITY_ONPOSTRESUME };
    private static final List<String> activityMethodList = Arrays.asList(activityMethods);
    private static final String[] activityLifecycleMethods = { ACTIVITYLIFECYCLECALLBACK_ONACTIVITYSTARTED,
            ACTIVITYLIFECYCLECALLBACK_ONACTIVITYSTOPPED, ACTIVITYLIFECYCLECALLBACK_ONACTIVITYSAVEINSTANCESTATE,
            ACTIVITYLIFECYCLECALLBACK_ONACTIVITYRESUMED, ACTIVITYLIFECYCLECALLBACK_ONACTIVITYPAUSED,
            ACTIVITYLIFECYCLECALLBACK_ONACTIVITYDESTROYED, ACTIVITYLIFECYCLECALLBACK_ONACTIVITYCREATED };
    private static final String[] serviceMethods = { SERVICE_ONCREATE, SERVICE_ONDESTROY, SERVICE_ONSTART1,
            SERVICE_ONSTART2, SERVICE_ONBIND, SERVICE_ONREBIND, SERVICE_ONUNBIND };
    private static final List<String> serviceMethodList = Arrays.asList(serviceMethods);
    private static final List<String> activityLifecycleMethodList = Arrays.asList(activityLifecycleMethods);
    private static final String[] broadcastMethods = { BROADCAST_ONRECEIVE };
    private static final List<String> broadcastMethodList = Arrays.asList(broadcastMethods);
    private static final String[] contentproviderMethods = { CONTENTPROVIDER_ONCREATE };
    private static final List<String> contentProviderMethodList = Arrays.asList(contentproviderMethods);
    private static final String[] serviceConnectionMethods = { SERVICECONNECTION_ONSERVICECONNECTED,
            SERVICECONNECTION_ONSERVICEDISCONNECTED };
    private static final List<String> serviceConnectionMethodList = Arrays.asList(serviceConnectionMethods);


    private static Set<String> allLCMethods = null;
    static {
        allLCMethods = new HashSet<>();
        allLCMethods.addAll(activityMethodList);
        allLCMethods.addAll(serviceMethodList);
        allLCMethods.addAll(activityLifecycleMethodList);
        allLCMethods.addAll(broadcastMethodList);
        allLCMethods.addAll(contentProviderMethodList);
        allLCMethods.addAll(serviceConnectionMethodList);
    }
    public static Set<String> getAllLCMethods() {
        return allLCMethods;
    }

}
