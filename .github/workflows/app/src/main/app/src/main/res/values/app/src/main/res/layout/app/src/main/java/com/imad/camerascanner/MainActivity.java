package com.imad.camerascanner;

import android.app.Activity;
import android.os.Bundle;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;
import android.widget.Button;
import android.widget.TextView;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    TextView status;
    TextView results;
    Button scanButton;

    ExecutorService pool =
            Executors.newFixedThreadPool(32);

    int[] ports = {
            80,
            443,
            554,
            8000,
            8080,
            37777,
            34567
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        status = findViewById(R.id.status);
        results = findViewById(R.id.results);
        scanButton = findViewById(R.id.scanButton);

        scanButton.setOnClickListener(v -> startScan());
    }

    private void startScan() {

        scanButton.setEnabled(false);
        results.setText("");

        WifiManager wifi =
                (WifiManager)
                getApplicationContext()
                .getSystemService(WIFI_SERVICE);

        int ip =
                wifi.getConnectionInfo()
                .getIpAddress();

        if (ip == 0) {

            status.setText(
                    "اتصل بشبكة Wi-Fi أولاً"
            );

            scanButton.setEnabled(true);
            return;
        }

        String localIp =
                Formatter.formatIpAddress(ip);

        String prefix =
                localIp.substring(
                        0,
                        localIp.lastIndexOf('.') + 1
                );

        status.setText(
                "جاري فحص الشبكة..."
        );

        CountDownLatch latch =
                new CountDownLatch(254);

        for (int i = 1; i <= 254; i++) {

            final int hostNumber = i;

            pool.execute(() -> {

                try {

                    String host =
                            prefix + hostNumber;

                    for (int port : ports) {

                        if (isOpen(host, port)) {

                            String message =
                                    "الجهاز: "
                                    + host
                                    + "   المنفذ: "
                                    + port;

                            if (port == 554) {
                                message +=
                                        "   ← RTSP محتمل";
                            }

                            runOnUiThread(() ->
                                    results.append(
                                            message + "\n"
                                    )
                            );
                        }
                    }

                } finally {

                    latch.countDown();
                }
            });
        }

        pool.execute(() -> {

            try {
                latch.await();
            } catch (InterruptedException ignored) {
            }

            runOnUiThread(() -> {

                status.setText(
                        "انتهى الفحص"
                );

                scanButton.setEnabled(true);
            });
        });
    }

    private boolean isOpen(
            String host,
            int port) {

        try {

            Socket socket =
                    new Socket();

            socket.connect(
                    new InetSocketAddress(
                            host,
                            port
                    ),
                    250
            );

            socket.close();

            return true;

        } catch (Exception e) {

            return false;
        }
    }

    @Override
    protected void onDestroy() {

        pool.shutdownNow();

        super.onDestroy();
    }
}
