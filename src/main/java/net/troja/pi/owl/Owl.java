package net.troja.pi.owl;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;

public class Owl implements GpioPinListenerDigital {
    private final GpioController gpio = GpioFactory.getInstance();
    private GpioPinDigitalInput button;
    private GpioPinDigitalOutput leftEye;
    private GpioPinDigitalOutput rightEye;

    public void setup() {
        button = gpio.provisionDigitalInputPin(RaspiPin.GPIO_11, "Start Button", PinPullResistance.PULL_DOWN);
        button.addListener(this);
        leftEye = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_12, "Left Eye", PinState.LOW);
        leftEye.setShutdownOptions(true, PinState.LOW, PinPullResistance.OFF);
        rightEye = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_13, "Right Eye", PinState.LOW);
        rightEye.setShutdownOptions(true, PinState.LOW, PinPullResistance.OFF);
    }

    public void handleGpioPinDigitalStateChangeEvent(final GpioPinDigitalStateChangeEvent event) {
        System.out.println("Button pressed");
        leftEye.pulse(500, true);
        rightEye.pulse(500, true);
        leftEye.pulse(500, true);
        rightEye.pulse(500, true);
    }

    public static void main(final String... args) {
        final Owl owl = new Owl();
        owl.setup();
    }
}
