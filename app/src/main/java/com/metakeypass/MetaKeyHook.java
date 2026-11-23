package com.metakeypass;

import android.view.KeyEvent;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MetaKeyHook implements IXposedHookLoadPackage {

    private static final String TAG = "MetaKeyPass";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!lpparam.packageName.equals("android")) return;

        XposedBridge.log(TAG + ": Hooking PhoneWindowManager");

        try {
            Class<?> pwmClass = XposedHelpers.findClass(
                    "com.android.server.policy.PhoneWindowManager",
                    lpparam.classLoader
            );

            // Hook interceptKeyBeforeDispatching - returns 0 to pass key to app
            XposedHelpers.findAndHookMethod(
                    pwmClass,
                    "interceptKeyBeforeDispatching",
                    "android.view.WindowManagerPolicy$WindowState",
                    KeyEvent.class,
                    int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            KeyEvent event = (KeyEvent) param.args[1];
                            if (event == null) return;

                            int keyCode = event.getKeyCode();

                            // Pass META keys straight through to foreground app
                            if (keyCode == KeyEvent.KEYCODE_META_LEFT ||
                                    keyCode == KeyEvent.KEYCODE_META_RIGHT) {
                                param.setResult(0L); // 0 = do not consume, pass to app
                                XposedBridge.log(TAG + ": Passing META key to foreground app");
                            }
                        }
                    }
            );

            XposedBridge.log(TAG + ": Hook installed successfully");

        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Hook failed: " + t.getMessage());
        }
    }
}
