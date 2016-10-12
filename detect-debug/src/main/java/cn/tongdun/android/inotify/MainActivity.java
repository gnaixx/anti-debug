package cn.tongdun.android.inotify;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "TEST";
    static {
        System.loadLibrary("inotify");
    }

    private native void startInotifyByRead();
    private native void startInotifyBySelect();
    private native void stopInotify(int type);
    private native void netMonitor();


    private Button btnStartRead;
    private Button btnStartSelect;
    private Button btnStopRead;
    private Button btnStopSelect;
    private Button btnTCP;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnStartRead = (Button) findViewById(R.id.btn_start_read);
        btnStartSelect = (Button) findViewById(R.id.btn_start_select);
        btnStopRead = (Button) findViewById(R.id.btn_stop_read);
        btnStopSelect = (Button) findViewById(R.id.btn_stop_select);
        btnTCP = (Button) findViewById(R.id.btn_tcp);

        btnStartRead.setOnClickListener(this);
        btnStartSelect.setOnClickListener(this);
        btnStopRead.setOnClickListener(this);
        btnStopSelect.setOnClickListener(this);
        btnTCP.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_start_read:
                startInotifyByRead();
                break;
            case R.id.btn_start_select:
                startInotifyBySelect();
                break;
            case R.id.btn_stop_read:
                stopInotify(1);
                break;
            case R.id.btn_stop_select:
                stopInotify(2);
                break;
            case R.id.btn_tcp:
                netMonitor();
                break;

        }
    }
}
