package me.xethh.tools.jvault.exceptionhandling;

public class CommonHandle {
    public interface CheckedSupplier<X>{
        public X get() throws Exception;
    }
    public static <X> X tryCatchThrow(CheckedSupplier<X> supplier) {
        try{
            return supplier.get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean throwExceptionIfNotExpected(Boolean checkedResult, String msg) {
        if (Boolean.TRUE.equals(checkedResult)) {
            throw new RuntimeException(msg);
        } else {
            return true;
        }
    }
}
