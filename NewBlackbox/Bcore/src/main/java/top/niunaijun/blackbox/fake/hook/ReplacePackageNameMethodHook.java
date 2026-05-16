package top.niunaijun.blackbox.fake.hook;

import java.lang.reflect.Method;
import top.niunaijun.blackbox.BlackBoxCore;
import black.android.content.BRAttributionSource;
import top.niunaijun.blackbox.utils.compat.BuildCompat;
import top.niunaijun.blackbox.utils.compat.ContextCompat;

/**
 * @author virtual_space
 * @function
 **/
public class ReplacePackageNameMethodHook extends MethodHook {
    private int packageNameIndex;

    public ReplacePackageNameMethodHook(int Index) {
        this.packageNameIndex = Index;
    }

    public Object hook(Object proxy, Method method, Object[] args) throws Throwable {
        Class realClass;
        if (args != null) {
            int i = this.packageNameIndex;
            if (i < 0) {
                this.packageNameIndex = i + args.length;
            }
            int i2 = this.packageNameIndex;
            if (i2 >= 0 && i2 < args.length && args[i2] != null) {
                if (args[i2] instanceof String) {
                    args[i2] = BlackBoxCore.getHostPkg();
                } else if (BuildCompat.isS() && (realClass = BRAttributionSource.getRealClass()) != null && realClass.isInstance(args[this.packageNameIndex])) {
                    ContextCompat.fixAttributionSourceState(args[this.packageNameIndex], BlackBoxCore.getHostUid());
                }
            }
        }
        return method.invoke(proxy, args);
    }
}