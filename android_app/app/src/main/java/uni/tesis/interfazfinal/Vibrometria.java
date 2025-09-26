package uni.tesis.interfazfinal;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class Vibrometria extends AppCompatActivity {

    private Button startThread;
    private AudioTrack audioTrack;
    private AudioManager audioManager;

    private String TAG = "TAG";

    short[] freqList={200,300,800,1000,1200,1500,1800};
    int sampleRate = 8000, indexFreq = 0;
    double gain = 1;
    int duration = 10000;
    private int bufferSize;
    private boolean isPressed;
    private short[] left, right, tone;
    private int canal = 0, qtyButton = 0;
    private float curVol;
    private float[][] volumeArray = new float[2][freqList.length]; //ROW 0: LEFT - ROW 1: RIGHT
    private long tiempoInicio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vibrometria);
        tiempoInicio = System.currentTimeMillis();

        startThread = findViewById(R.id.startThread);
        audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
                (int) (audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)/2),
                0);

        Toast.makeText(Vibrometria.this, "Presiona para iniciar LEFT", Toast.LENGTH_SHORT).show();

        startThread.setOnClickListener(v -> {
            qtyButton++;
            isPressed = true;
            Log.d("HOLA","Qty Button: " + qtyButton);
            if (qtyButton <= (2*freqList.length + 2)){
                if (qtyButton == 1 || qtyButton == freqList.length+2){
                    initialize();
                    TonesRunnable tonesRunnable = new TonesRunnable();
                    new Thread(tonesRunnable).start();
                }else {
                    volumeArray[canal-1][indexFreq] = curVol;
                    Log.d("HOLA","volumeArray[" + String.valueOf(canal-1) + "][" + String.valueOf(indexFreq) + "] = " + volumeArray[canal-1][indexFreq]);
                }
            }else {
                qtyButton = 0;
                canal = 0;
                Intent goMenu = new Intent(Vibrometria.this, MainActivity.class);
                startActivity(goMenu);
            }
        });
    }

    class TonesRunnable implements Runnable {
        @Override
        public void run() {
            playingTones();
        }
    }
    private void playingTones (){

        boolean isRunning = true;
        int top, played;

        //Variables Set Volume
        int n = 50;
        int down = (int) (n * 0.2);
        int up = n - down;
        int waitMs = (int) (duration/n);
        int i = 0;
        long startTime = 0, transcurrido = 0;
        float stepVolUp = 0, stepVolDown = 0;

        if (canal == 1) {
            left = setTone(gain, duration, freqList[indexFreq], sampleRate);
            right = new short[left.length];
        } else {
            right = setTone(gain, duration, freqList[indexFreq], sampleRate);
            left = new short[right.length];
        }
        tone = stereoSound(left, right);
        bufferSize = 2 * tone.length;

        top = (int) (bufferSize * 0.25);
        played = top;

        while (isRunning){
            if (indexFreq < freqList.length) {
                if (isPressed == true){
                    audioTrack.pause();
                    audioTrack.flush();
                    isPressed = false;
                    indexFreq++;
                    played = top;
                    if (qtyButton == freqList.length+1){
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(Vibrometria.this, "Presiona para iniciar RIGHT", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }else {
                    if (played == top) {
                        Log.d("START","Inicia " + indexFreq);
                        curVol = 0.0f;
                        if (canal == 1) {
                            left = setTone(gain, duration, freqList[indexFreq], sampleRate);
                            right = new short[left.length];
                        } else {
                            right = setTone(gain, duration, freqList[indexFreq], sampleRate);
                            left = new short[right.length];
                        }
                        tone = stereoSound(left, right);
                        bufferSize = 2 * tone.length;

                        audioTrack = new AudioTrack(
                                AudioManager.STREAM_MUSIC,
                                sampleRate,
                                AudioFormat.CHANNEL_OUT_STEREO,
                                AudioFormat.ENCODING_PCM_16BIT,
                                bufferSize,
                                AudioTrack.MODE_STATIC);

                        stepVolUp = audioTrack.getMaxVolume()/up;
                        stepVolDown = audioTrack.getMaxVolume()/down;
                        i = 0;

                        audioTrack.write(tone, 0, tone.length);
                        waitTime(500);
                        audioTrack.setVolume(curVol);
                        audioTrack.play();
                        transcurrido = waitMs;
                    }
                    played = audioTrack.getPlaybackHeadPosition();

                    // Set Volume
                    if (transcurrido >= waitMs){
                        Log.d(TAG, "i : " + i + "\ncurVol : " + curVol + "\n");
                        audioTrack.setVolume(curVol);
                        if (i < up) {
                            curVol += stepVolUp;
                        } else{
                            curVol -= stepVolDown;
                        }
                        i++;
                        startTime = System.currentTimeMillis();
                    }
                    transcurrido = System.currentTimeMillis()-startTime;
                }
            } else {
                isRunning = false;
            }
        }
    }
    private short[] setTone(double gain, int durationTimeMs, int freq, int sampleRate) {
        int toneSize = (int)(sampleRate * durationTimeMs * 0.001);
        short[] tone = new short[toneSize];
        for (int i = 0; i < toneSize; i++) {
            double t = (double) i / sampleRate;
            tone[i] = (short) (gain * Math.sin(2 * Math.PI * freq * t) * Short.MAX_VALUE);
        }
        return tone;
    }
    private short[] stereoSound(short[] left, short[] right){

        int stereoSoundSize;
        //asignacion tamaño array estereo
        if (left.length > right.length){
            stereoSoundSize = 2*left.length;
        }else {
            stereoSoundSize = 2*right.length;
        }

        short[] stereoSoundArray = new short[stereoSoundSize];

        //inicializar array
        for (int i = 0; i<stereoSoundSize; i++){
            stereoSoundArray[i]=0;
        }
        //left
        for (int i = 0; i<left.length; i++){
            stereoSoundArray[2*i]=left[i];
        }
        //right
        for (int i = 0; i<right.length; i++){
            stereoSoundArray[2*i+1]=right[i];
        }

        return stereoSoundArray;
    }
    private void initialize (){
        indexFreq = 0;
        canal++;
        isPressed = false;
    }
    private void waitTime(int ms){
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    @Override
    protected void onDestroy() {

        super.onDestroy();

    }
    @Override
    protected void onStop(){
        super.onStop();
        Log.d("Vibro", "onDestroy - Llamado");
        long tiempoSesionActual = System.currentTimeMillis() - tiempoInicio;
        TimeT.guardarTiempoAcumulado(this, tiempoSesionActual);
        Log.d("Vibro", "onDestroy - Tiempo acumulado: " + tiempoSesionActual);
    }
}