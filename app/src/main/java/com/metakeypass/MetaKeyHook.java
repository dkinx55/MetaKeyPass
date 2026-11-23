package com.metakeypass;

import android.view.KeyEvent;

import java.lang.reflect.Method;
import java.util.Set;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MetaKeyHook implements IXposedHookLoadPackage {

    private static final String TAG = "MetaKeyPass";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        XposedBridge.log(TAG + ": handleLoadPackage called for: " + lpparam.packageName);

        if (!lpparam.packageName.equals("android")) return;

        XposedBridge.log(TAG + ": Hooking system package");

        // Hook 1: Oplus StrategyPadKeyBoardShortcut (primary - handles Meta key on OnePlus pads)
        hookOplusStrategy(lpparam.classLoader);

        // Hook 2: PhoneWindowManager fallback (standard Android path)
        hookPhoneWindowManager(lpparam.classLoader);
    }

    private void hookOplusStrategy(ClassLoader classLoader) {
        try {
            Class<?> strategyClass = XposedHelpers.findClass(
                    "com.android.server.policy.StrategyPadKeyBoardShortcut",
                    classLoader
            );
            XposedBridge.log(TAG + ": Found StrategyPadKeyBoardShortcut, hooking all methods named excuteShortCut");

            Set<XC_MethodHook.Unhook> hooks1 = XposedBridge.hookAllMethods(strategyClass, "excuteShortCut", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    for (Object arg : param.args) {
                        if (arg instanceof KeyEvent) {
                            KeyEvent event = (KeyEvent) arg;
                            int keyCode = event.getKeyCode();
                            if (keyCode == KeyEvent.KEYCODE_META_LEFT ||
                                    keyCode == KeyEvent.KEYCODE_META_RIGHT) {
                                XposedBridge.log(TAG + ": Blocking Meta key in StrategyPadKeyBoardShortcut");
                                param.setResult(null);
                                return;
                            }
                        }
                    }
                }
            });

            XposedBridge.log(TAG + ": Hooked " + hooks1.size() + " excuteShortCut overloads");

        } catch (Throwable t) {
            XposedBridge.log(TAG + ": StrategyPadKeyBoardShortcut hook failed: " + t.getMessage());
        }
    }

    private void hookPhoneWindowManager(ClassLoader classLoader) {
        try {
            Class<?> pwmClass = XposedHelpers.findClass(
                    "com.android.server.policy.PhoneWindowManager",
                    classLoader
            );

            // Log available overloads of interceptKeyBeforeDispatching
            int found = 0;
            for (Method m : pwmClass.getDeclaredMethods()) {
                if (m.getName().equals("interceptKeyBeforeDispatching")) {
                    XposedBridge.log(TAG + ": PWM method: " + m);
                    found++;
                }
            }
            XposedBridge.log(TAG + ": Found " + found + " interceptKeyBeforeDispatching overloads");

            Set<XC_MethodHook.Unhook> hooks2 = XposedBridge.hookAllMethods(pwmClass, "interceptKeyBeforeDispatching", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    for (Object arg : param.args) {
                        if (arg instanceof KeyEvent) {
                            KeyEvent event = (KeyEvent) arg;
                            int keyCode = event.getKeyCode();
                            if (keyCode == KeyEvent.KEYCODE_META_LEFT ||
                                    keyCode == KeyEvent.KEYCODE_META_RIGHT) {
                                XposedBridge.log(TAG + ": Passing META key via PWM hook");
                                param.setResult(0L);
                                return;
                            }
                        }
                    }
                }
            });

            XposedBridge.log(TAG + ": Hooked " + hooks2.size() + " interceptKeyBeforeDispatching overloads");

        } catch (Throwable t) {
            XposedBridge.log(TAG + ": PhoneWindowManager hook failed: " + t.getMessage());
        }
    }
}
