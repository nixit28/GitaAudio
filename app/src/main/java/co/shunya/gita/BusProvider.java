package co.shunya.gita;

import com.squareup.otto.Bus;

/**
 * Created by nixit on 8/24/14.
 */
public class BusProvider {

    private static final Bus bus = new Bus();

    private static final Bus mediabus = new Bus();
    private static boolean started;

    public static Bus getInstance() {
        return bus;
    }

    public static Bus getMediaEventBus() {
        return mediabus;
    }

    private BusProvider() {
    }


    public static boolean isStarted() {
        return started;
    }

    public static void setStarted(boolean started) {
        BusProvider.started = started;
    }
}
