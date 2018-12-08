package com.example.blithe.myapplication;

import android.os.Handler;
import android.os.Message;

import java.util.LinkedList;
import java.util.Queue;
import java.util.stream.Stream;

import cn.wch.ch34xuartdriver.CH34xUARTDriver;

public class ReceiveThread extends Thread {

    private CH34xUARTDriver device;
    private boolean run = true;
    private byte[] rxbuffer;
    private Handler mhandler;

    public ReceiveThread(CH34xUARTDriver device, Handler mhandler){
        this.device = device;
        this.mhandler =mhandler;
        rxbuffer = new byte[1024];
    }

    @Override
    public void run() {
        int len;
        CommData rxdata = new CommData();
        Message msg;

        while(isRun()&&device.isConnected())
        {
            len =  device.ReadData(rxbuffer,1);
            if(len == 0 )
            {
                try {
                    sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                continue;
            }

            if(len > 0 && rxbuffer[0] == (byte)0xa5)
            {
                    len =  device.ReadData(rxbuffer,7);
                    if(len > 0 && (byte)(rxbuffer[1] + rxbuffer[2]+ rxbuffer[3]+ rxbuffer[4]+ rxbuffer[5]+ rxbuffer[6] + (byte)0xa5) == rxbuffer[0])
                    {
                        msg = Message.obtain();
                        msg.what = 1;
                        rxdata.temperature = ((int)rxbuffer[1]&0xff)+(((int)rxbuffer[2]<<8)&0xff00);
                        rxdata.pressure  =((int)rxbuffer[3]&0xff)+ (((int)rxbuffer[4]<<8)&0xff00)+ (((int)rxbuffer[5]<<16)&0xff0000)+ (((int)rxbuffer[6]<<24)&0xff000000);
                        msg.obj = rxdata;
                        mhandler.sendMessage(msg);
                    }
            }
        }
    }

    public boolean isRun() {
        return run;
    }

    public void setRun(boolean run) {
        this.run = run;
    }
}
