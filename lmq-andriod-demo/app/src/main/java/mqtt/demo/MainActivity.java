package mqtt.demo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import org.eclipse.paho.android.service.MqttAndroidClient;

public class MainActivity extends AppCompatActivity {

    private MqttAndroidClient mqttAndroidClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mqttAndroidClient = new MqttAndroidClient(getApplicationContext(), Config.serverUri, Config.clientId);
        MqttSimple mqttSimple = new MqttSimple(mqttAndroidClient);
        mqttSimple.test();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mqttAndroidClient.unregisterResources();
    }

}
