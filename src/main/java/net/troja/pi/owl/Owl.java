package net.troja.pi.owl;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

public class Owl implements Job, GpioPinListenerDigital {
    private static final int LONG_PRESS_MS = 1000;
    private static final String RADIO_URL = "http://www.wdr.de/wdrlive/media/kiraka.m3u";

    private final GpioController gpio = GpioFactory.getInstance();
    private final DateFormat dateFormat = new SimpleDateFormat("H-m");
    private GpioPinDigitalInput button;
    private GpioPinDigitalOutput leftEye;
    private GpioPinDigitalOutput rightEye;
    private final Random rand = new Random(System.currentTimeMillis());
    private boolean working;
    private long startButtonPress;
    private long lastButtonPress;

    private static boolean playingMp3;
    private static Process mp3Process;

    public void setup() throws SchedulerException {
        button = gpio.provisionDigitalInputPin(RaspiPin.GPIO_11, "Start Button", PinPullResistance.PULL_UP);
        button.addListener(this);
        leftEye = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_13, "Left Eye", PinState.LOW);
        leftEye.setShutdownOptions(true, PinState.LOW, PinPullResistance.OFF);
        rightEye = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_12, "Right Eye", PinState.LOW);
        rightEye.setShutdownOptions(true, PinState.LOW, PinPullResistance.OFF);

        Scheduler scheduler = new StdSchedulerFactory().getScheduler();
        JobDetail job = newJob(Owl.class)
                .withIdentity("myJob", "group1")
                .build();
        Trigger trigger = newTrigger()
                .withIdentity("myTrigger", "group1")
                .withSchedule(cronSchedule("0 45 6 ? * Mon-Fri"))
                .forJob("myJob", "group1")
                .build();
        scheduler.scheduleJob(job, trigger);
        scheduler.start();

        System.out.println("Setup " + scheduler.isStarted());
    }

    public void execute(JobExecutionContext jobExecutionContext) {
        System.out.println("Alarm start");
        Owl.playingMp3 = true;
        playMp3("alarm.mp3");
        try {
            Owl.mp3Process.waitFor();
        } catch (InterruptedException e) {
        }
        Owl.playingMp3 = false;
        System.out.println("Alarm end");
    }

    public void handleGpioPinDigitalStateChangeEvent(final GpioPinDigitalStateChangeEvent event) {
        if (event.getState() == PinState.LOW || working == true) {
            startButtonPress = System.currentTimeMillis();
            return;
        }
        try {
            if(Owl.playingMp3) {
                stopMp3();
                return;
            }
            working = true;

            if (System.currentTimeMillis() - lastButtonPress < 5000) {
                playMp3("ungeduld.mp3");
                bothEyesBlink(1000);
            } else {
                lastButtonPress = System.currentTimeMillis();
                long pressTime = System.currentTimeMillis() - startButtonPress;
                if (pressTime < LONG_PRESS_MS) {
                    tellTime();
                } else {
                    startRadio();
                }
            }
        } catch (final InterruptedException e) {
        }
        working = false;
    }

    private void stopMp3() throws InterruptedException {
        System.out.println("Stop playing mp3");
        Owl.mp3Process.destroy();
        Thread.sleep(20);
        if(Owl.mp3Process.isAlive()) {
            System.out.println("Still alive!");
            Owl.mp3Process.destroyForcibly();
            System.out.println("And now " + Owl.mp3Process.isAlive());

        }
        Owl.playingMp3 = false;
    }

    private void tellTime() throws InterruptedException {
        System.out.println("Tell time");
        final int number = rand.nextInt(5);
        playMp3(dateFormat.format(new Date()) + ".mp3");

        switch (number) {
        case 0:
            bothEyes(1000);
            break;
        case 1:
            bothEyesBlink(1000);
            break;
        case 2:
            leftEyeBlink(1000);
            break;
        case 3:
            rightEyeBlink(1000);
            break;
        case 4:
            leftRightEyeBlink(1000);
            break;
        default:
            System.out.println("Random: " + number);
            break;
        }
    }

    private void startRadio() {
        System.out.println("Start radio");
        Owl.playingMp3 = true;
        playMp3(RADIO_URL);
        new Thread(() -> {
            try {
                eyeMove(500);
            } catch (InterruptedException e) {
            }
        }).start();
    }

    private void playMp3(final String mp3) {
        ProcessBuilder builder;
        if(mp3.endsWith("m3u")) {
            builder = new ProcessBuilder("mpg123", "-@", mp3, ">", "/dev/null");
            builder.inheritIO();
        } else {
            builder = new ProcessBuilder("mpg123", mp3, ">", "/dev/null");
        }
        try {
            Owl.mp3Process = builder.start();
        } catch (final IOException e) {
            System.err.println("Could not play mp3: " + e.getMessage());
        }
    }

    private void bothEyes(final int milis) throws InterruptedException {
        leftEye.high();
        rightEye.high();
        Owl.mp3Process.waitFor();
        leftEye.low();
        rightEye.low();
    }

    private void bothEyesBlink(final int milis) throws InterruptedException {
        while (Owl.mp3Process.isAlive()) {
            leftEye.high();
            rightEye.high();
            Thread.sleep(milis);
            leftEye.low();
            rightEye.low();
            Thread.sleep(milis);
        }
    }

    private void leftEyeBlink(final int milis) throws InterruptedException {
        while (Owl.mp3Process.isAlive()) {
            leftEye.pulse(milis, true);
            Thread.sleep(milis);
        }
    }

    private void rightEyeBlink(final int milis) throws InterruptedException {
        while (Owl.mp3Process.isAlive()) {
            rightEye.pulse(milis, true);
            Thread.sleep(milis);
        }
    }

    private void leftRightEyeBlink(final int milis) {
        while (Owl.mp3Process.isAlive()) {
            leftEye.pulse(milis, true);
            rightEye.pulse(milis, true);
        }
    }

    private void eyeMove(final int milis) throws InterruptedException {
        leftEye.high();
        Thread.sleep(milis);
        while (Owl.mp3Process.isAlive()) {
            rightEye.high();
            Thread.sleep(milis);
            leftEye.low();
            Thread.sleep(milis);
            leftEye.high();
            Thread.sleep(milis);
            rightEye.low();
            Thread.sleep(milis);
        }
        leftEye.low();
    }

    public static void main(final String... args) throws InterruptedException, SchedulerException {
        final Owl owl = new Owl();
        owl.setup();
        while (true) {
            Thread.sleep(500);
        }
    }
}
