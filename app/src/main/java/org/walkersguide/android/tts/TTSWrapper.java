package org.walkersguide.android.tts;

import android.annotation.TargetApi;

import android.content.Context;

import android.os.Build;

import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import android.view.accessibility.AccessibilityManager;

import com.google.common.base.Splitter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import android.media.AudioAttributes;
import android.accessibilityservice.AccessibilityServiceInfo;
import timber.log.Timber;
import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.util.SettingsManager;
import android.media.AudioManager;
import android.annotation.SuppressLint;
import android.media.AudioFocusRequest;


public class TTSWrapper extends UtteranceProgressListener {
    private static final String UTTERANCE_ID_SPEAK = "utteranceidspeak";

    private static TTSWrapper managerInstance;
    private AccessibilityManager accessibilityManager;
    private AudioManager mAudioManager;
    private TextToSpeech tts;

    public static TTSWrapper getInstance() {
        if (managerInstance == null){
            managerInstance = getInstanceSynchronized();
        }
        return managerInstance;
    }

    private static synchronized TTSWrapper getInstanceSynchronized() {
        if (managerInstance == null){
            managerInstance = new TTSWrapper();
        }
        return managerInstance;
    }

    private TTSWrapper() {
        accessibilityManager = (AccessibilityManager) GlobalInstance.getContext().getSystemService(Context.ACCESSIBILITY_SERVICE);
        mAudioManager = (AudioManager) GlobalInstance.getContext().getSystemService(Context.AUDIO_SERVICE);

        tts = new TextToSpeech(GlobalInstance.getContext(), new TextToSpeech.OnInitListener() {
            @Override public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    tts.setLanguage(Locale.getDefault());
                    tts.setOnUtteranceProgressListener(TTSWrapper.this);
                } else {
                    tts = null;
                }
            }
        });
    }

    public boolean isInitialized() {
        return tts != null;
    }

    public boolean isSpeaking() {
        if (isInitialized()) {
            return tts.isSpeaking();
        }
        return false;
    }

    public boolean isScreenReaderEnabled() {
        return ! accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_SPOKEN).isEmpty();
    }

    public void screenReader(String message) {
        if (isScreenReaderEnabled()) {
            speak(message);
        }
    }

    public void announce(String message) {
        if (SettingsManager.getInstance().getTtsSettings().getAnnouncementsEnabled()) {
            speak(message);
        }
    }

    private void speak(String message) {
        if (isInitialized()) {
            stop();

            // speak
            tts.setAudioAttributes(buildAudioAttributes());
            if (tryToGetAudioFocus()) {
                for (String chunk : Splitter.fixedLength(tts.getMaxSpeechInputLength()).splitToList(message)) {
                    tts.speak(chunk, TextToSpeech.QUEUE_ADD, null, UTTERANCE_ID_SPEAK);
                }
            }
        }
    }

    public void stop() {
        if (isInitialized() && isSpeaking()) {
            tts.stop();
            giveUpAudioFocus();
        }
    }

    private AudioAttributes buildAudioAttributes() {
        return new AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .setUsage(
                    isScreenReaderEnabled()
                    ? AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY
                    : AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
            .build();
    }


    /**
     * audio focus
     * only supported for android sdk >= 26 (version 8.0)
     */

    private boolean tryToGetAudioFocus() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            return mAudioManager.requestAudioFocus(buildAudioFocusRequest()) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        }
        return true;
    }

    private void giveUpAudioFocus() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            mAudioManager.abandonAudioFocusRequest(buildAudioFocusRequest());
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    private AudioFocusRequest buildAudioFocusRequest() {
        return new AudioFocusRequest.Builder(
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(buildAudioAttributes())
            .setAcceptsDelayedFocusGain(false)
            .build();
    }


    /**
     * UtteranceProgressListener interface inplementation
     */

    @Override public void onStart(String utteranceId) {
    }

    @Override public void onError(String utteranceId) {
        tts.setLanguage(Locale.getDefault());
        giveUpAudioFocus();
    }

    @Override public void onDone(String utteranceId) {
        Timber.d("onDone");
        giveUpAudioFocus();
    }

}
