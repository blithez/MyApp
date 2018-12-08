package com.example.blithe.myapplication;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.LinkedList;
import java.util.Queue;

import cn.wch.ch34xuartdriver.CH34xUARTDriver;

public class MainActivity extends AppCompatActivity {

    public static CH34xUARTDriver device;
    private static final String ACTION_USB_PERMISSION = "cn.wch.wchusbdriver.USB_PERMISSION";

    int baudrate = 9600;
    byte databit = 8;
    byte stopbit=1;
    byte parity = 0;
    byte flowcontrol = 0;

    private Button openbt,configbt,startbt;
    private TextView tv;
    private boolean isOpen;
    private int retval;
    private SeekBar angle;
    private TextView rxtv;
    private ReceiveThread rxthread;
    private int temperature,pressure;

    private Handler myhander   = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what)
            {
                case 1:
                    pressure =  ((CommData)msg.obj).pressure;
                    temperature = ((CommData)msg.obj).temperature;
                    double height = 44330-44330*Math.pow((1.0*pressure/101325),1/5.255);
                    rxtv.setText(String.format("%.1f %s\n%d %s\n%.1f %s",0.1*temperature,getString(R.string.degree),pressure,getString(R.string.pa),height,getString(R.string.meter)));break;
                default:;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tv = (TextView) findViewById(R.id.tv);
        openbt = (Button) findViewById(R.id.openbt);
        configbt = (Button)findViewById(R.id.configbt);
        angle =(SeekBar)findViewById(R.id.seekBar);
        rxtv = (TextView)findViewById(R.id.rxtv);
        startbt = (Button)findViewById(R.id.startbt);

        device = new CH34xUARTDriver((UsbManager) getSystemService(Context.USB_SERVICE), this, ACTION_USB_PERMISSION);

        if (!device.UsbFeatureSupported())// 判断系统是否支持USB HOST
        {
            Dialog dialog = new AlertDialog.Builder(MainActivity.this).setTitle("提示").setMessage("您的手机不支持USB HOST，请更换其他手机再试！").setPositiveButton("确认", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {
                            System.exit(0);
                        }
                    }).create();

            dialog.setCanceledOnTouchOutside(false);
            dialog.show();
        }

//        configbt.setOnClickListener(sendbtlistener);
        rxthread = new ReceiveThread(device,myhander);
        startbt.setOnClickListener(startlistener);

        openbt.setOnClickListener(openbtlistener);
        angle.setOnSeekBarChangeListener(anglelistener);
    }

    private View.OnClickListener startlistener = new View.OnClickListener(){

        @Override
        public void onClick(View view) {
            //rxthread.start();
        }
    };

    public SeekBar.OnSeekBarChangeListener anglelistener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
            byte[] pack=new byte[4];
            pack[0]=(byte)0xa5;
            pack[1]=4;
            pack[2]=(byte)i;
            pack[3]=(byte)(0xa5+4+i);
            if(isOpen == true){
            device.WriteData(pack,4);}
            tv.setText(String.valueOf(i));
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    };
    public View.OnClickListener sendbtlistener = new View.OnClickListener(){
        @Override
        public void onClick(View view) {
            byte[] pack=new byte[4];
            pack[0]=(byte)0xa5;
            pack[1]=4;
            pack[2]=90;
            pack[3]=(byte)(0xa5+4+90);
            device.WriteData(pack,4);
        }
    };


    public View.OnClickListener openbtlistener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (!isOpen) {
                retval = device.ResumeUsbList();

                if (retval == -1)// ResumeUsbList方法用于枚举CH34X设备以及打开相关设备
                {
                    Toast.makeText(MainActivity.this, "打开设备失败!", Toast.LENGTH_SHORT).show();
                    device.CloseDevice();
                } else if (retval == 0) {
                    if (!device.UartInit()) {//对串口设备进行初始化操作
                        Toast.makeText(MainActivity.this, "设备初始化失败!", Toast.LENGTH_SHORT).show();
                        Toast.makeText(MainActivity.this, "打开" + "设备失败!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Toast.makeText(MainActivity.this, "打开设备成功!", Toast.LENGTH_SHORT).show();

                    device.SetConfig(baudrate, databit,stopbit,parity,flowcontrol);
                    isOpen = true;
                    rxthread.start();
                    openbt.setText(getString(R.string.close));
                }
            } else {
                isOpen = false;
                rxthread.setRun(false);
                openbt.setText(getString(R.string.close));
                device.CloseDevice();
            }
        }
    };



}
