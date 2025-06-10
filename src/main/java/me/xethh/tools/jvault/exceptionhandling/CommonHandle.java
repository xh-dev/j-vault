package me.xethh.tools.jvault.exceptionhandling;

import io.vavr.CheckedFunction0;

public class CommonHandle {
    public static <X> X tryCatchThrow(CheckedFunction0<X> runnable) {
        try{
            return runnable.unchecked().get();
        } catch (Exception ex){
            throw new RuntimeException(ex);
        }
    }
}
