package cc.gnaixx.detect_debug;

import android.nfc.Tag;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "GNAIXX_JAVA";
    static {
        System.loadLibrary("detect_debug");
    }

    private native void startInotify(int type);
    private native void stopInotify(int type);
    private native void tcpPortMonitor();
    private native void tarcePidMonitor();
    private native void singleStepDetect();


    private Button btnOsDebug;
    private Button btnStartBlock;
    private Button btnStartUnblock;
    private Button btnStopBlock;
    private Button btnStopUnblock;
    private Button btnTCPMonitor;
    private Button btnTarcePidMonitor;
    private Button btnSingleStepDetect;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnOsDebug = (Button) findViewById(R.id.btn_os_debug);
        btnStartBlock = (Button) findViewById(R.id.btn_start_block);
        btnStartUnblock = (Button) findViewById(R.id.btn_start_unblock);
        btnStopBlock = (Button) findViewById(R.id.btn_stop_block);
        btnStopUnblock = (Button) findViewById(R.id.btn_stop_unblock);
        btnTCPMonitor = (Button) findViewById(R.id.btn_tcp_monitor);
        btnTarcePidMonitor = (Button) findViewById(R.id.btn_tarce_pid_monitor);
        btnSingleStepDetect = (Button) findViewById(R.id.btn_single_step);

        btnOsDebug.setOnClickListener(this);
        btnStartBlock.setOnClickListener(this);
        btnStartUnblock.setOnClickListener(this);
        btnStopBlock.setOnClickListener(this);
        btnStopUnblock.setOnClickListener(this);
        btnTCPMonitor.setOnClickListener(this);
        btnTarcePidMonitor.setOnClickListener(this);
        btnSingleStepDetect.setOnClickListener(this);
    }

    void detectOsDebug(){
        boolean connected = android.os.Debug.isDebuggerConnected();
        Log.d(TAG, "debugger connect status:" + connected);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_os_debug:
                detectOsDebug();
                break;
            case R.id.btn_start_block:
                startInotify(1);
                break;
            case R.id.btn_start_unblock:
                startInotify(2);
                break;
            case R.id.btn_stop_block:
                stopInotify(1);
                break;
            case R.id.btn_stop_unblock:
                stopInotify(2);
                break;
            case R.id.btn_tcp_monitor:
                tcpPortMonitor();
                break;
            case R.id.btn_tarce_pid_monitor:
                tarcePidMonitor();
                break;
            case R.id.btn_single_step:
                singleStepDetect();
                break;

        }
    }
}
