package de.uni_hannover.finf.hlapp;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Toast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;

public class MainActivity extends AppCompatActivity {
    private static final int PORT   = 1234;
    private int LENGTH =   42;

    static {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
    }

    private float[] rgbw = new float[4];
    private float[] hsv  = new float[3];

    private SeekBar red;
    private SeekBar green;
    private SeekBar blue;
    private SeekBar white;

    private SeekBar hue;
    private SeekBar sat;
    private SeekBar val;

    private EditText led_length2;

    private void updateLENGTH(){

        led_length2 = findViewById(R.id.led_length);
        LENGTH = Integer.parseInt(led_length2.getText().toString());
    }

    DatagramSocket socket;

    private interface Setter {
        void set(float value);
    }

    private class SeekBarListener implements SeekBar.OnSeekBarChangeListener {
        private Setter setter;

        public SeekBarListener(Setter setter) {
            this.setter = setter;
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int i, boolean fromUser) {
            if (fromUser)
                setter.set((float) i / seekBar.getMax());
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {}

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {}
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        try {
            socket = new DatagramSocket(PORT);
        } catch (SocketException e) {
            Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }

        red   = findViewById(R.id.red);
        green = findViewById(R.id.green);
        blue  = findViewById(R.id.blue);
        white = findViewById(R.id.white);

        red.setOnSeekBarChangeListener(new SeekBarListener(new Setter() {
            @Override
            public void set(float value) {
                rgbw[0] = value;
                onRGB();
            }
        }));

        green.setOnSeekBarChangeListener(new SeekBarListener(new Setter() {
            @Override
            public void set(float value) {
                rgbw[1] = value;
                onRGB();
            }
        }));

        blue.setOnSeekBarChangeListener(new SeekBarListener(new Setter() {
            @Override
            public void set(float value) {
                rgbw[2] = value;
                onRGB();
            }
        }));

        white.setOnSeekBarChangeListener(new SeekBarListener(new Setter() {
            @Override
            public void set(float value) {
                rgbw[3] = value;
                onRGB();
            }
        }));

        hue = findViewById(R.id.hue);
        sat = findViewById(R.id.sat);
        val = findViewById(R.id.val);

        hue.setOnSeekBarChangeListener(new SeekBarListener(new Setter() {
            @Override
            public void set(float value) {
                hsv[0] = value * 360f;
                onHSV();
            }
        }));

        sat.setOnSeekBarChangeListener(new SeekBarListener(new Setter() {
            @Override
            public void set(float value) {
                hsv[1] = value;
                onHSV();
            }
        }));

        val.setOnSeekBarChangeListener(new SeekBarListener(new Setter() {
            @Override
            public void set(float value) {
                hsv[2] = value;
                onHSV();
            }
        }));

        int[] buttons = {
                R.id.esp1, R.id.esp2, R.id.esp3, R.id.esp4,
                R.id.esp5, R.id.esp6, R.id.esp7, R.id.esp8,
                R.id.all

        };

        for (int b: buttons) {
            final Button button = findViewById(b);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    sendData(button);
                }
            });
        }


    }

    private float getRed() {
        return rgbw[0];
    }

    private float getGreen() {
        return rgbw[1];
    }

    private float getBlue() {
        return rgbw[2];
    }

    private float getWhite() {
        return rgbw[3];
    }

    private float getHue() {
        return hsv[0] / 360f;
    }

    private float getSat() {
        return hsv[1];
    }

    private float getVal() {
        return hsv[2];
    }

    private void onRGB() {
        Color.RGBToHSV((int) (rgbw[0] * 255f), (int) (rgbw[1] * 255f), (int) (rgbw[2] * 255f), hsv);

        hue.setProgress((int) (getHue() * hue.getMax()));
        sat.setProgress((int) (getSat() * sat.getMax()));
        val.setProgress((int) (getVal() * val.getMax()));
    }

    private void onHSV() {
        int color = Color.HSVToColor(hsv);
        rgbw[0] = ((float) Color.red  (color)) / 255f;
        rgbw[1] = ((float) Color.green(color)) / 255f;
        rgbw[2] = ((float) Color.blue (color)) / 255f;

        red  .setProgress((int) (  getRed() * red  .getMax()));
        green.setProgress((int) (getGreen() * green.getMax()));
        blue .setProgress((int) ( getBlue() * blue  .getMax()));
    }

    private void sendData(Button button) {
        updateLENGTH();
        byte[] pixel = {
                (byte) (rgbw[1] * 255),
                (byte) (rgbw[0] * 255),
                (byte) (rgbw[2] * 255),
                (byte) (rgbw[3] * 255)
        };
        byte[] data  = repeat(pixel, LENGTH + 1 );

        data[0] = 2;
        data[1] = 0;
        data[2] = 0;
        data[3] = 0;

        System.out.println(Arrays.toString(data));
        try {
            InetAddress address = InetAddress.getByName((String) button.getText());

            for (int i = 0; i < 255; i += 20) {
                data[1] = (byte) i;
                sendData(data, address);
            }
        } catch (UnknownHostException e) {
            Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void sendData(byte[] data, InetAddress address) {
        try {
            DatagramPacket packet = new DatagramPacket(data, LENGTH * 4 + 4, address, 1234);
            socket.send(packet);
        } catch (IOException e) {
            Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    public static byte[] repeat(byte[] arr, int times) {
        int newLength = times * arr.length;
        byte[] dup = Arrays.copyOf(arr, newLength);
        for (int last = arr.length; last != 0 && last < newLength; last <<= 1) {
            System.arraycopy(dup, 0, dup, last, Math.min(last << 1, newLength) - last);
        }
        return dup;
    }
}
