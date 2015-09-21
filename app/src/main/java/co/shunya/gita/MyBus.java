package co.shunya.gita;

import com.squareup.otto.Bus;
import com.squareup.otto.ThreadEnforcer;

/**
 * Created by nixit on 8/25/14.
 */
public class MyBus extends Bus {

    private boolean registed;

    public MyBus() {
        super();
    }

    public MyBus(String identifier) {
        super(identifier);
    }

    public MyBus(ThreadEnforcer enforcer) {
        super(enforcer);
    }

    public MyBus(ThreadEnforcer enforcer, String identifier) {
        super(enforcer, identifier);
    }

    @Override
    public void register(Object object) {
        super.register(object);
        registed = true;
    }

    @Override
    public void unregister(Object object) {
        super.unregister(object);
        registed = false;
    }

    public boolean isRegisted() {
        return registed;
    }
}
